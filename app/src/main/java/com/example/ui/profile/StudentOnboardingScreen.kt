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

// Établissements et filières de référence garantissant un choix complet immédiat
private val DEFAULT_ECOLES = listOf(
    Ecole(1, "Faculté des Sciences", "FS"),
    Ecole(2, "Institut Universitaire de Technologie", "IUT"),
    Ecole(3, "Faculté de Médecine et des Sciences Biomédicales", "FMSB"),
    Ecole(4, "Faculté des Sciences Économiques et de Gestion", "FSEG"),
    Ecole(5, "Faculté des Arts, Lettres et Sciences Humaines", "FALSH"),
    Ecole(6, "Faculté des Sciences Juridiques et Politiques", "FSJP")
)

private val DEFAULT_FILIERES = listOf(
    // FS
    Filiere(101, "INF", "Informatique", ecole_id = 1, ecole_nom = "Faculté des Sciences"),
    Filiere(102, "MATH", "Mathématiques", ecole_id = 1, ecole_nom = "Faculté des Sciences"),
    Filiere(103, "PHY", "Physique", ecole_id = 1, ecole_nom = "Faculté des Sciences"),
    Filiere(104, "CHIM", "Chimie", ecole_id = 1, ecole_nom = "Faculté des Sciences"),
    Filiere(105, "BIO", "Biologie", ecole_id = 1, ecole_nom = "Faculté des Sciences"),
    // IUT
    Filiere(201, "GL", "Génie Logiciel", ecole_id = 2, ecole_nom = "Institut Universitaire de Technologie"),
    Filiere(202, "RIM", "Réseaux et Télécoms", ecole_id = 2, ecole_nom = "Institut Universitaire de Technologie"),
    Filiere(203, "GEII", "Génie Électrique", ecole_id = 2, ecole_nom = "Institut Universitaire de Technologie"),
    // FMSB
    Filiere(301, "MED", "Médecine Générale", ecole_id = 3, ecole_nom = "Faculté de Médecine et des Sciences Biomédicales"),
    Filiere(302, "PHARM", "Pharmacie", ecole_id = 3, ecole_nom = "Faculté de Médecine et des Sciences Biomédicales"),
    // FSEG
    Filiere(401, "ECO", "Sciences Économiques", ecole_id = 4, ecole_nom = "Faculté des Sciences Économiques et de Gestion"),
    Filiere(402, "GEST", "Gestion & Finance", ecole_id = 4, ecole_nom = "Faculté des Sciences Économiques et de Gestion"),
    // FSJP
    Filiere(601, "DROIT", "Droit Privé & Public", ecole_id = 6, ecole_nom = "Faculté des Sciences Juridiques et Politiques")
)

private fun filterFilieresForEcole(ecole: Ecole?, allFilieres: List<Filiere>): List<Filiere> {
    if (ecole == null) return allFilieres
    val filtered = allFilieres.filter { f ->
        (ecole.id > 0 && f.ecole_id == ecole.id) ||
        (!f.ecole_nom.isNullOrBlank() && f.ecole_nom.equals(ecole.nom, ignoreCase = true)) ||
        (ecole.nom.isNotBlank() && !f.ecole_nom.isNullOrBlank() && (ecole.nom.contains(f.ecole_nom, ignoreCase = true) || f.ecole_nom.contains(ecole.nom, ignoreCase = true))) ||
        (!ecole.sigle.isNullOrBlank() && !f.ecole_nom.isNullOrBlank() && (ecole.sigle.equals(f.ecole_nom, ignoreCase = true) || f.ecole_nom.contains(ecole.sigle, ignoreCase = true)))
    }
    return if (filtered.isNotEmpty()) filtered else allFilieres
}

/**
 * Formulaire d'accueil et de sélection de profil étudiant.
 * Fiabilisé pour permettre une sélection fluide et sans blocage sous toute condition réseau.
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
    // Fusion avec les listes de repli pour ne jamais avoir de sélection vide
    val effectiveEcoles = remember(ecoles) {
        if (ecoles.isNotEmpty()) {
            val list = ecoles.toMutableList()
            DEFAULT_ECOLES.forEach { def ->
                if (!list.any { it.nom.equals(def.nom, ignoreCase = true) || it.id == def.id }) {
                    list.add(def)
                }
            }
            list
        } else {
            DEFAULT_ECOLES
        }
    }

    val effectiveFilieres = remember(filieres) {
        if (filieres.isNotEmpty()) {
            val list = filieres.toMutableList()
            DEFAULT_FILIERES.forEach { def ->
                if (!list.any { it.code.equals(def.code, ignoreCase = true) }) {
                    list.add(def)
                }
            }
            list
        } else {
            DEFAULT_FILIERES
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

    val currentFilieres = remember(selectedEcole, effectiveFilieres) {
        filterFilieresForEcole(selectedEcole, effectiveFilieres)
    }

    var selectedFiliere by remember {
        mutableStateOf(
            currentFilieres.find {
                it.code.equals(initialProfile.filiereCode, ignoreCase = true) ||
                it.id == initialProfile.filiereId
            } ?: currentFilieres.firstOrNull()
        )
    }

    fun onSelectEcole(newEcole: Ecole) {
        selectedEcole = newEcole
        val updatedFilieres = filterFilieresForEcole(newEcole, effectiveFilieres)
        if (selectedFiliere == null || !updatedFilieres.any { it.code.equals(selectedFiliere?.code, ignoreCase = true) }) {
            selectedFiliere = updatedFilieres.firstOrNull()
        }
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
                Image(
                    painter = painterResource(id = com.example.ui.search.components.campusEduMarkRes),
                    contentDescription = "Logo Campus Edu",
                    modifier = Modifier.size(88.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Titre : nom de la marque, ou « Modifier mon profil » si le profil existe déjà
                if (initialProfile.isConfigured) {
                    Text(
                        text = "Modifier mon profil",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                } else {
                    com.example.ui.search.components.CampusEduWordmark(
                        style = MaterialTheme.typography.headlineLarge
                    )
                }

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
                                        text = selectedEcole?.nom ?: "Sélectionner une école",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedEcole != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!selectedEcole?.sigle.isNullOrBlank()) {
                                        Text(
                                            text = selectedEcole?.sigle.orEmpty(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
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
                                            text = ecole.nom,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!ecole.sigle.isNullOrBlank()) {
                                            Text(
                                                text = ecole.sigle.orEmpty(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        }
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

