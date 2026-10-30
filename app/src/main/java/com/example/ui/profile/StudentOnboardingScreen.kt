package com.example.ui.profile

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

/**
 * Formulaire d'accueil et de sélection de profil étudiant.
 * Épuré au maximum selon les exigences :
 *  - Uniquement le logo du chapeau académique blanc
 *  - Le titre "CAMPUS EDUCATIF" (aucun sous-texte)
 *  - Le formulaire sans icônes dans les champs
 *  - Filtrage strict des filières selon l'école choisie
 *  - Bouton intitulé uniquement "COMMENCER"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentOnboardingScreen(
    ecoles: List<Ecole>,
    filieres: List<Filiere>,
    initialProfile: StudentProfile,
    onSaveProfile: (StudentProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEcole by remember { mutableStateOf(ecoles.find { it.id == initialProfile.ecoleId } ?: ecoles.firstOrNull()) }

    // Uniquement les filières de l'école choisie
    val filieresDeLecole = remember(selectedEcole, filieres) {
        val ecole = selectedEcole
        if (ecole == null) emptyList()
        else filieres.filter { f ->
            (ecole.id > 0 && f.ecole_id == ecole.id) ||
            (!f.ecole_nom.isNullOrBlank() && f.ecole_nom.equals(ecole.nom, ignoreCase = true)) ||
            (ecole.nom.isNotBlank() && !f.ecole_nom.isNullOrBlank() && ecole.nom.contains(f.ecole_nom, ignoreCase = true))
        }
    }

    var selectedFiliere by remember {
        mutableStateOf(filieresDeLecole.find { it.code == initialProfile.filiereCode } ?: filieresDeLecole.firstOrNull())
    }

    // Réinitialiser la filière choisie dès que l'école change pour garantir l'appartenance stricte
    LaunchedEffect(selectedEcole) {
        selectedFiliere = filieresDeLecole.firstOrNull()
    }

    var selectedNiveau by remember { mutableStateOf(initialProfile.niveau.ifBlank { "L1" }) }

    var ecoleExpanded by remember { mutableStateOf(false) }
    var filiereExpanded by remember { mutableStateOf(false) }

    val niveaux = listOf("L1", "L2", "L3", "M1", "M2")

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                modifier = Modifier.size(92.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_academic_cap_white),
                    contentDescription = "Logo Campus Educatif",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Titre en MAJUSCULE uniquement (aucun sous-texte)
            Text(
                text = "CAMPUS EDUCATIF",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

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
                    // Champ École (sans icône interne)
                    Column {
                        Text(
                            text = "Faculté ou École",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = ecoleExpanded,
                            onExpandedChange = { ecoleExpanded = !ecoleExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedEcole?.nom ?: "Sélectionner une école",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = ecoleExpanded)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = ecoleExpanded,
                                onDismissRequest = { ecoleExpanded = false }
                            ) {
                                ecoles.forEach { ecole ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = ecole.nom,
                                                fontWeight = if (selectedEcole?.id == ecole.id) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedEcole = ecole
                                            ecoleExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Champ Filière (uniquement les filières de l'école choisie, sans icône interne)
                    Column {
                        Text(
                            text = "Filière",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = filiereExpanded,
                            onExpandedChange = { filiereExpanded = !filiereExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedFiliere?.let { "${it.code} - ${it.nom}" } ?: "Sélectionner une filière",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = filiereExpanded)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = filiereExpanded,
                                onDismissRequest = { filiereExpanded = false }
                            ) {
                                if (filieresDeLecole.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Aucune filière répertoriée pour cette école") },
                                        onClick = { filiereExpanded = false }
                                    )
                                } else {
                                    filieresDeLecole.forEach { filiere ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${filiere.code} - ${filiere.nom}",
                                                    fontWeight = if (selectedFiliere?.id == filiere.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedFiliere = filiere
                                                filiereExpanded = false
                                            }
                                        )
                                    }
                                }
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
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bouton COMMENCER uniquement
            Button(
                onClick = {
                    val profile = StudentProfile(
                        ecoleId = selectedEcole?.id,
                        ecoleNom = selectedEcole?.nom,
                        filiereId = selectedFiliere?.id,
                        filiereCode = selectedFiliere?.code,
                        filiereNom = selectedFiliere?.nom,
                        niveau = selectedNiveau,
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
                    text = "COMMENCER",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
