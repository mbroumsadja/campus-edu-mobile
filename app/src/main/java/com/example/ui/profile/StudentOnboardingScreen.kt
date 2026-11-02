package com.example.ui.profile

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Ecole
import com.example.data.model.Filiere
import com.example.data.preferences.StudentProfile

// Établissements et filières réels existants dans Campus Edu (API v1)
internal val EXISTING_ECOLES = listOf(
    Ecole(id = 1, nom = "FS", sigle = "FS", description = "Faculté des Sciences"),
    Ecole(id = 2, nom = "FMSB", sigle = "FMSB", description = "Faculté de Médecine et des Sciences Biomédicales")
)

internal val EXISTING_FILIERES = listOf(
    // Faculté des Sciences (FS - ecole_id = 1)
    Filiere(id = 1, code = "INF", nom = "Informatique", ecole_id = 1, ecole_nom = "FS", ecole = "FS", departement = "Mathematique et Informatique"),
    Filiere(id = 2, code = "MAT", nom = "Mathématiques", ecole_id = 1, ecole_nom = "FS", ecole = "FS", departement = "Mathematique et Informatique"),
    Filiere(id = 3, code = "CHI", nom = "Chimie", ecole_id = 1, ecole_nom = "FS", ecole = "FS", departement = "Chimie"),
    Filiere(id = 4, code = "PHY", nom = "Physique", ecole_id = 1, ecole_nom = "FS", ecole = "FS", departement = "Physique"),
    Filiere(id = 5, code = "ERN", nom = "Energie Renouvelable", ecole_id = 1, ecole_nom = "FS", ecole = "FS", departement = "Energie_renouvelable"),
    Filiere(id = 6, code = "BIO", nom = "Biologie", ecole_id = 1, ecole_nom = "FS", ecole = "FS", departement = "Sciences_Biologie_et_de_la_terre"),
    Filiere(id = 7, code = "STE", nom = "Sciences de la Terre", ecole_id = 1, ecole_nom = "FS", ecole = "FS", departement = "Sciences_Biologie_et_de_la_terre"),

    // Faculté de Médecine et des Sciences Biomédicales (FMSB - ecole_id = 2)
    Filiere(id = 11, code = "MED", nom = "Médecine Générale", ecole_id = 2, ecole_nom = "FMSB", ecole = "FMSB", departement = "Medecine Generale"),
    Filiere(id = 10, code = "RIM", nom = "Radiologie et Imagerie Médicale", ecole_id = 2, ecole_nom = "FMSB", ecole = "FMSB", departement = "Sciences_Biomedicale"),
    Filiere(id = 9, code = "SIN", nom = "Sciences Infirmières", ecole_id = 2, ecole_nom = "FMSB", ecole = "FMSB", departement = "Sciences_Biomedicale"),
    Filiere(id = 8, code = "SBM", nom = "Sciences Biomédicales", ecole_id = 2, ecole_nom = "FMSB", ecole = "FMSB", departement = "Sciences_Biomedicale")
)

val Ecole.displayName: String
    get() = when (nom.uppercase().trim()) {
        "FS" -> "Faculté des Sciences (FS)"
        "FMSB" -> "Faculté de Médecine et des Sciences Biomédicales (FMSB)"
        else -> if (!sigle.isNullOrBlank() && sigle != nom) "$nom ($sigle)" else nom
    }

internal fun filterFilieresForEcole(ecole: Ecole?, allFilieres: List<Filiere>): List<Filiere> {
    if (ecole == null) return allFilieres
    val isFS = ecole.id == 1 || ecole.nom.equals("FS", ignoreCase = true) || ecole.nom.contains("Sciences", ignoreCase = true)
    val isFMSB = ecole.id == 2 || ecole.nom.equals("FMSB", ignoreCase = true) || ecole.nom.contains("Médecine", ignoreCase = true) || ecole.nom.contains("Medecine", ignoreCase = true)

    val fsCodes = setOf("INF", "MAT", "MATH", "CHI", "PHY", "ERN", "BIO", "STE")
    val fmsbCodes = setOf("MED", "RIM", "SIN", "SBM")

    val matches = allFilieres.filter { f ->
        val codeUpper = f.code.uppercase().trim()
        when {
            isFS -> (f.ecole_id != null && f.ecole_id == 1) ||
                    f.ecole_nom.equals("FS", ignoreCase = true) ||
                    f.ecole.equals("FS", ignoreCase = true) ||
                    fsCodes.contains(codeUpper)
            isFMSB -> (f.ecole_id != null && f.ecole_id == 2) ||
                    f.ecole_nom.equals("FMSB", ignoreCase = true) ||
                    f.ecole.equals("FMSB", ignoreCase = true) ||
                    fmsbCodes.contains(codeUpper)
            else -> (ecole.id > 0 && f.ecole_id == ecole.id) ||
                    (!f.ecole_nom.isNullOrBlank() && f.ecole_nom.equals(ecole.nom, ignoreCase = true))
        }
    }

    return if (matches.isNotEmpty()) {
        matches
    } else {
        EXISTING_FILIERES.filter { f ->
            val codeUpper = f.code.uppercase().trim()
            if (isFMSB) fmsbCodes.contains(codeUpper) else fsCodes.contains(codeUpper)
        }
    }
}

/**
 * Formulaire d'accueil et de sélection de profil étudiant.
 * Affiche UNIQUEMENT les écoles et filières existantes sur la plateforme Campus Edu.
 */
@Composable
fun StudentOnboardingScreen(
    ecoles: List<Ecole>,
    filieres: List<Filiere>,
    initialProfile: StudentProfile,
    onSaveProfile: (StudentProfile) -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Affiche uniquement les écoles existantes (FS et FMSB)
    val effectiveEcoles = remember(ecoles) {
        val list = ecoles.filter { ecole ->
            ecole.id == 1 || ecole.id == 2 ||
            ecole.nom.equals("FS", ignoreCase = true) ||
            ecole.nom.equals("FMSB", ignoreCase = true) ||
            ecole.nom.contains("Sciences", ignoreCase = true) ||
            ecole.nom.contains("Médecine", ignoreCase = true) ||
            ecole.nom.contains("Medecine", ignoreCase = true)
        }
        if (list.isNotEmpty()) list else EXISTING_ECOLES
    }

    // Affiche uniquement les filières existantes
    val effectiveFilieres = remember(filieres) {
        val existingCodes = setOf("INF", "MAT", "MATH", "CHI", "PHY", "ERN", "BIO", "STE", "MED", "RIM", "SIN", "SBM")
        val list = filieres.filter { f ->
            existingCodes.contains(f.code.uppercase().trim())
        }
        if (list.isNotEmpty()) {
            list.map { f ->
                val codeUpper = f.code.uppercase().trim()
                val isFmsb = setOf("MED", "RIM", "SIN", "SBM").contains(codeUpper)
                val targetId = if (isFmsb) 2 else 1
                val targetNom = if (isFmsb) "FMSB" else "FS"
                f.copy(
                    ecole_id = f.ecole_id ?: targetId,
                    ecole_nom = if (f.ecole_nom.isNullOrBlank()) targetNom else f.ecole_nom,
                    ecole = if (f.ecole.isNullOrBlank()) targetNom else f.ecole
                )
            }
        } else {
            EXISTING_FILIERES
        }
    }

    // Conservation stable du choix utilisateur sans écrasement par recomposition
    var selectedEcole by remember {
        mutableStateOf(
            effectiveEcoles.find {
                it.id == initialProfile.ecoleId ||
                (!initialProfile.ecoleNom.isNullOrBlank() && it.nom.equals(initialProfile.ecoleNom, ignoreCase = true))
            } ?: effectiveEcoles.firstOrNull()
        )
    }

    LaunchedEffect(effectiveEcoles) {
        if (selectedEcole == null) {
            selectedEcole = effectiveEcoles.find {
                it.id == initialProfile.ecoleId ||
                (!initialProfile.ecoleNom.isNullOrBlank() && it.nom.equals(initialProfile.ecoleNom, ignoreCase = true))
            } ?: effectiveEcoles.firstOrNull()
        }
    }

    val currentFilieres = remember(selectedEcole, effectiveFilieres) {
        filterFilieresForEcole(selectedEcole, effectiveFilieres)
    }

    var selectedFiliere by remember(selectedEcole) {
        mutableStateOf(
            currentFilieres.find {
                it.code.equals(initialProfile.filiereCode, ignoreCase = true) ||
                it.id == initialProfile.filiereId
            } ?: currentFilieres.firstOrNull()
        )
    }

    LaunchedEffect(selectedEcole, currentFilieres) {
        if (selectedFiliere == null || !currentFilieres.any { it.code.equals(selectedFiliere?.code, ignoreCase = true) }) {
            selectedFiliere = currentFilieres.find {
                it.code.equals(initialProfile.filiereCode, ignoreCase = true)
            } ?: currentFilieres.firstOrNull()
        }
    }

    fun onSelectEcole(newEcole: Ecole) {
        selectedEcole = newEcole
        val updatedFilieres = filterFilieresForEcole(newEcole, effectiveFilieres)
        selectedFiliere = updatedFilieres.find {
            it.code.equals(initialProfile.filiereCode, ignoreCase = true)
        } ?: updatedFilieres.firstOrNull()
    }

    var selectedNiveau by remember { mutableStateOf(initialProfile.niveau.ifBlank { "L1" }) }
    var onlyMyCourses by remember { mutableStateOf(initialProfile.onlyMyCourses) }

    var showEcoleDialog by remember { mutableStateOf(false) }
    var showFiliereDialog by remember { mutableStateOf(false) }

    val niveaux = listOf("L1", "L2", "L3", "M1", "M2")

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo chapeau académique blanc
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(86.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_academic_cap_white),
                        contentDescription = "Logo Campus Educatif",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Titre en MAJUSCULE avec police épaisse et compacte
                Text(
                    text = if (initialProfile.isConfigured) "MODIFIER MON PROFIL" else "CAMPUS EDUCATIF",
                    fontSize = 26.sp,
                    fontFamily = com.example.ui.theme.AntonFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

            // Carte du Formulaire
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Champ École
                    Column {
                        Text(
                            text = "Faculté ou École",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            onClick = { showEcoleDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_ecole_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedEcole?.displayName ?: "Sélectionner une école",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedEcole != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Ouvrir la liste des écoles",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Champ Filière
                    Column {
                        Text(
                            text = "Filière",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            onClick = { showFiliereDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_filiere_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedFiliere?.let { "${it.code} - ${it.nom}" } ?: "Sélectionner une filière",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedFiliere != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Ouvrir la liste des filières",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Sélection Niveau
                    Column {

                        Text(
                            text = "Niveau",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            niveaux.forEach { niv ->
                                val isSelected = selectedNiveau == niv
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedNiveau = niv },
                                    label = {
                                        Text(
                                            text = niv,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    // Case à cocher : Uniquement mes cours
                    Surface(
                        onClick = { onlyMyCourses = !onlyMyCourses },
                        shape = RoundedCornerShape(10.dp),
                        color = if (onlyMyCourses) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(
                            1.dp,
                            if (onlyMyCourses) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("only_my_courses_checkbox_row")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = onlyMyCourses,
                                onCheckedChange = { onlyMyCourses = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Uniquement mes cours",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bouton de validation
            Button(
                onClick = {
                    val profile = StudentProfile(
                        ecoleId = selectedEcole?.id,
                        ecoleNom = selectedEcole?.nom,
                        filiereId = selectedFiliere?.id,
                        filiereCode = selectedFiliere?.code,
                        filiereNom = selectedFiliere?.nom,
                        niveau = selectedNiveau,
                        onlyMyCourses = onlyMyCourses,
                        isConfigured = true
                    )
                    onSaveProfile(profile)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_student_profile_button")
            ) {
                Text(
                    text = if (initialProfile.isConfigured) "ENREGISTRER LES MODIFICATIONS" else "COMMENCER",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showEcoleDialog) {
            AlertDialog(
                onDismissRequest = { showEcoleDialog = false },
                title = {
                    Text(
                        text = "Choisir une Faculté ou École",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        effectiveEcoles.forEach { ecole ->
                            val isSelected = selectedEcole?.id == ecole.id ||
                                (selectedEcole?.nom != null && selectedEcole?.nom.equals(ecole.nom, ignoreCase = true))
                            Surface(
                                onClick = {
                                    onSelectEcole(ecole)
                                    showEcoleDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ecole.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sélectionné",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showEcoleDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }

        if (showFiliereDialog) {
            AlertDialog(
                onDismissRequest = { showFiliereDialog = false },
                title = {
                    Column {
                        Text(
                            text = "Choisir une Filière",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        selectedEcole?.let {
                            Text(
                                text = it.sigle ?: it.nom,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentFilieres.forEach { filiere ->
                            val isSelected = selectedFiliere?.code.equals(filiere.code, ignoreCase = true)
                            Surface(
                                onClick = {
                                    selectedFiliere = filiere
                                    showFiliereDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${filiere.code} - ${filiere.nom}",
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sélectionné",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showFiliereDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}
}

