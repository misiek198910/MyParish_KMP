package com.example.mojaparafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mojaparafia.ui.components.AdBanner
import com.example.mojaparafia.viewmodel.ParishListViewModel
import kotlinx.coroutines.launch
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposeChangeScreen(
    parishId: String,
    parishName: String,
    viewModel: ParishListViewModel,
    onBackClick: () -> Unit,
    onGetLocationClick: () -> Unit,
    showToast: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val parishToEdit by viewModel.getParishById(parishId).collectAsState(initial = null)
    val formState = remember { mutableStateMapOf<String, String>() }

    val isPremium by viewModel.isPremium.collectAsState(false)
    val userPoints by viewModel.userPoints.collectAsState(0)
    val effectivePremium = isPremium || userPoints >= 50

    LaunchedEffect(parishToEdit) {
        parishToEdit?.let { p ->
            if (!formState.containsKey("suggestedParishName")) {
                formState["suggestedParishName"] = p.name ?: parishName
                formState["suggestedAddress"] = p.address ?: ""
                formState["suggestedPhotoUrl"] = p.photoUrl ?: ""
                formState["suggestedLatitude"] = p.latitude.toString()
                formState["suggestedLongitude"] = p.longitude.toString()
                formState["suggestedMassSunday"] = p.massHoursSunday ?: ""
                formState["suggestedMassForSunday"] = p.hasMassSundayHour ?: ""
                formState["suggestedMassForChildrenHour"] = p.hasMassForChildrenHour ?: ""
                formState["suggestedMassMonday"] = p.massHoursMonday ?: ""
                formState["suggestedMassTuesday"] = p.massHoursTuesday ?: ""
                formState["suggestedMassWednesday"] = p.massHoursWednesday ?: ""
                formState["suggestedMassThursday"] = p.massHoursThursday ?: ""
                formState["suggestedMassFriday"] = p.massHoursFriday ?: ""
                formState["suggestedMassSaturday"] = p.massHoursSaturday ?: ""
                formState["suggestedConfession"] = p.confessionInfo ?: ""
                formState["suggestedOfficeHoursText"] = p.officeHoursText ?: ""
                formState["suggestedPhoneNum"] = p.phoneNum ?: ""
                formState["suggestedEmail"] = p.email ?: ""
                formState["suggestedWebsiteUrl"] = p.websiteUrl ?: ""
                formState["suggestedAdoration"] = p.adorationInfo ?: ""
                formState["suggestedPastorName"] = p.pastorName ?: ""
                formState["suggestedDiocese"] = p.diocese ?: ""
                formState["suggestedDeanery"] = p.deanery ?: ""
                formState["suggestedFoundingYear"] = p.foundingYear ?: ""
                formState["suggestedSocialMediaFacebook"] = p.socialMediaFacebook ?: ""
                formState["suggestedSocialMediaYouTube"] = p.socialMediaYouTube ?: ""
                formState["suggestedSocialMediaInstagram"] = p.socialMediaInstagram ?: ""
                formState["suggestedFirstSaturdayOfMonth_hour"] = p.firstSaturdayOfMonthHour ?: ""
                formState["suggestedFirstSaturdayOfMonth_info"] = p.firstSaturdayOfMonthInfo ?: ""
            }
        } ?: run {
            if (!formState.containsKey("suggestedParishName")) {
                formState["suggestedParishName"] = parishName
            }
        }
    }

    val customColorScheme = MaterialTheme.colorScheme.copy(
        primary = Color(0xFF1976D2)
    )

    MaterialTheme(colorScheme = customColorScheme) {
        Scaffold(
            topBar = {
                Surface(
                    color = Color.White.copy(alpha = 0.85f),
                    shadowElevation = 2.dp
                ) {
                    TopAppBar(
                        title = { Text(stringResource(Res.string.propose_change_title), fontWeight = FontWeight.Bold, color = Color(0xFF1A252F)) },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć", tint = Color(0xFF1A252F))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            },
            bottomBar = {
                if (!effectivePremium) {
                    Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                        AdBanner(modifier = Modifier.fillMaxWidth(), isPremium = effectivePremium)
                    }
                }
            },
            containerColor = Color(0xFFF5F7FA)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    }
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. INFORMACJE PODSTAWOWE
                GlassFormCard {
                    Text(stringResource(Res.string.propose_change_text1), color = Color(0xFF546E7A), fontSize = 14.sp, fontFamily = FontFamily(Font(Res.font.lora_medium)))
                    Spacer(modifier = Modifier.height(8.dp))

                    FormTextField(
                        value = formState["suggestedParishName"] ?: "",
                        onValueChange = { formState["suggestedParishName"] = it },
                        hint = stringResource(Res.string.propose_change_hint_parish_name),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    Text("ID: #$parishId", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(Res.string.propose_change_address_label), color = Color(0xFF546E7A), fontSize = 14.sp)
                    FormTextField(
                        value = formState["suggestedAddress"] ?: "",
                        onValueChange = { formState["suggestedAddress"] = it },
                        hint = stringResource(Res.string.propose_change_hint_address_example),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(Res.string.propose_change_photo_url_label), color = Color(0xFF546E7A), fontSize = 14.sp)
                    FormTextField(
                        value = formState["suggestedPhotoUrl"] ?: "",
                        onValueChange = { formState["suggestedPhotoUrl"] = it },
                        hint = stringResource(Res.string.propose_change_hint_photo_url_example),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                }

                // 2. LITURGIA I MSZE ŚW.
                GlassFormCard {
                    SectionFormHeader(stringResource(Res.string.propose_change_header_liturgy))

                    FormLabel(stringResource(Res.string.propose_change_label_sunday))
                    FormTextField(formState["suggestedMassSunday"] ?: "", { formState["suggestedMassSunday"] = it }, stringResource(Res.string.propose_change_hint_sunday))

                    FormLabel(stringResource(Res.string.propose_change_label_vigil))
                    FormTextField(formState["suggestedMassForSunday"] ?: "", { formState["suggestedMassForSunday"] = it }, stringResource(Res.string.propose_change_hint_vigil))

                    FormLabel(stringResource(Res.string.propose_change_label_kids))
                    FormTextField(formState["suggestedMassForChildrenHour"] ?: "", { formState["suggestedMassForChildrenHour"] = it }, stringResource(Res.string.propose_change_hint_kids))

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))

                    FormLabel(stringResource(Res.string.propose_change_label_weekdays))
                    FormTextField(formState["suggestedMassMonday"] ?: "", { formState["suggestedMassMonday"] = it }, stringResource(Res.string.propose_change_monday))
                    FormTextField(formState["suggestedMassTuesday"] ?: "", { formState["suggestedMassTuesday"] = it }, stringResource(Res.string.propose_change_tuesday))
                    FormTextField(formState["suggestedMassWednesday"] ?: "", { formState["suggestedMassWednesday"] = it }, stringResource(Res.string.propose_change_wednesday))
                    FormTextField(formState["suggestedMassThursday"] ?: "", { formState["suggestedMassThursday"] = it }, stringResource(Res.string.propose_change_thursday))
                    FormTextField(formState["suggestedMassFriday"] ?: "", { formState["suggestedMassFriday"] = it }, stringResource(Res.string.propose_change_friday))
                    FormTextField(formState["suggestedMassSaturday"] ?: "", { formState["suggestedMassSaturday"] = it }, stringResource(Res.string.propose_change_saturday))

                    FormLabel(stringResource(Res.string.propose_change_label_confession))
                    FormTextField(formState["suggestedConfession"] ?: "", { formState["suggestedConfession"] = it }, stringResource(Res.string.propose_change_hint_confession))

                    FormLabel(stringResource(Res.string.propose_change_label_adoration))
                    FormTextField(formState["suggestedAdoration"] ?: "", { formState["suggestedAdoration"] = it }, stringResource(Res.string.propose_change_hint_adoration), singleLine = false)

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))

                    FormLabel(stringResource(Res.string.propose_change_label_first_saturday))
                    FormTextField(formState["suggestedFirstSaturdayOfMonth_hour"] ?: "", { formState["suggestedFirstSaturdayOfMonth_hour"] = it }, stringResource(Res.string.propose_change_hint_first_saturday_hour))
                    FormTextField(formState["suggestedFirstSaturdayOfMonth_info"] ?: "", { formState["suggestedFirstSaturdayOfMonth_info"] = it }, stringResource(Res.string.propose_change_hint_first_saturday_info), singleLine = false)
                }

                // 3. KANCELARIA
                GlassFormCard {
                    SectionFormHeader(stringResource(Res.string.propose_change_header_office))
                    FormTextField(formState["suggestedOfficeHoursText"] ?: "", { formState["suggestedOfficeHoursText"] = it }, stringResource(Res.string.propose_change_hint_office), singleLine = false)
                }

                // 4. KONTAKT
                GlassFormCard {
                    SectionFormHeader(stringResource(Res.string.propose_change_header_contact))
                    FormTextField(formState["suggestedPhoneNum"] ?: "", { formState["suggestedPhoneNum"] = it }, stringResource(Res.string.propose_change_hint_phone), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    FormTextField(formState["suggestedEmail"] ?: "", { formState["suggestedEmail"] = it }, stringResource(Res.string.propose_change_hint_email), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    FormTextField(formState["suggestedWebsiteUrl"] ?: "", { formState["suggestedWebsiteUrl"] = it }, stringResource(Res.string.propose_change_hint_website), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                }

                // 5. ADMINISTRACJA
                GlassFormCard {
                    SectionFormHeader(stringResource(Res.string.propose_change_header_admin))
                    FormTextField(formState["suggestedPastorName"] ?: "", { formState["suggestedPastorName"] = it }, stringResource(Res.string.propose_change_hint_pastor))
                    FormTextField(formState["suggestedDiocese"] ?: "", { formState["suggestedDiocese"] = it }, stringResource(Res.string.propose_change_hint_diocese))
                    FormTextField(formState["suggestedDeanery"] ?: "", { formState["suggestedDeanery"] = it }, stringResource(Res.string.propose_change_hint_deanery))
                    FormTextField(formState["suggestedFoundingYear"] ?: "", { formState["suggestedFoundingYear"] = it }, stringResource(Res.string.propose_change_hint_founding), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }

                // 6. SOCIAL MEDIA
                GlassFormCard {
                    SectionFormHeader(stringResource(Res.string.propose_change_header_social))
                    FormTextField(formState["suggestedSocialMediaFacebook"] ?: "", { formState["suggestedSocialMediaFacebook"] = it }, stringResource(Res.string.propose_change_hint_fb))
                    FormTextField(formState["suggestedSocialMediaYouTube"] ?: "", { formState["suggestedSocialMediaYouTube"] = it }, stringResource(Res.string.propose_change_hint_yt))
                    FormTextField(formState["suggestedSocialMediaInstagram"] ?: "", { formState["suggestedSocialMediaInstagram"] = it }, stringResource(Res.string.propose_change_hint_ig))
                }

                // 7. LOKALIZACJA
                GlassFormCard {
                    SectionFormHeader(stringResource(Res.string.propose_change_header_location))
                    Text(stringResource(Res.string.propose_change_location_description), color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))

                    FormTextField(formState["suggestedLatitude"] ?: "", { formState["suggestedLatitude"] = it }, stringResource(Res.string.propose_change_hint_latitude), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    FormTextField(formState["suggestedLongitude"] ?: "", { formState["suggestedLongitude"] = it }, stringResource(Res.string.propose_change_hint_longitude), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                    OutlinedButton(
                        onClick = onGetLocationClick,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.propose_change_button_get_location), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val changeMap = mutableMapOf<String, String>()
                        changeMap["parish_id"] = parishId
                        changeMap["parish_name"] = parishName

                        var hasAnyChange = false
                        val keysToCheck = listOf(
                            "suggestedParishName", "suggestedAddress", "suggestedPhotoUrl", "suggestedLatitude", "suggestedLongitude",
                            "suggestedMassSunday", "suggestedMassForSunday", "suggestedMassForChildrenHour",
                            "suggestedMassMonday", "suggestedMassTuesday", "suggestedMassWednesday",
                            "suggestedMassThursday", "suggestedMassFriday", "suggestedMassSaturday",
                            "suggestedConfession", "suggestedOfficeHoursText", "suggestedPhoneNum",
                            "suggestedEmail", "suggestedWebsiteUrl", "suggestedAdoration",
                            "suggestedPastorName", "suggestedDiocese", "suggestedDeanery", "suggestedFoundingYear",
                            "suggestedSocialMediaFacebook", "suggestedSocialMediaYouTube", "suggestedSocialMediaInstagram",
                            "suggestedFirstSaturdayOfMonth_hour", "suggestedFirstSaturdayOfMonth_info"
                        )

                        for (key in keysToCheck) {
                            var value = formState[key]?.trim() ?: ""
                            if (value.isNotEmpty()) {
                                if (key == "suggestedLatitude" || key == "suggestedLongitude") {
                                    value = value.replace(",", ".")
                                }
                                changeMap[key] = value
                                hasAnyChange = true
                            }
                        }

                        if (!hasAnyChange) {
                            scope.launch { showToast(getString(Res.string.propose_change_text17)) }
                            return@Button
                        }

                        viewModel.submitParishProposal(changeMap)

                        scope.launch { showToast(getString(Res.string.propose_change_queued)) }
                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Text(stringResource(Res.string.propose_change_text10).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                // PRZYCISK: RESETUJ DO DANYCH Z BAZY
                OutlinedButton(
                    onClick = {
                        if (parishToEdit != null) {
                            val p = parishToEdit!!
                            formState["suggestedParishName"] = p.name ?: ""
                            formState["suggestedAddress"] = p.address ?: ""
                            formState["suggestedPhotoUrl"] = p.photoUrl ?: ""
                            formState["suggestedLatitude"] = p.latitude.toString()
                            formState["suggestedLongitude"] = p.longitude.toString()
                            formState["suggestedMassSunday"] = p.massHoursSunday ?: ""
                            formState["suggestedMassForSunday"] = p.hasMassSundayHour ?: ""
                            formState["suggestedMassForChildrenHour"] = p.hasMassForChildrenHour ?: ""
                            formState["suggestedMassMonday"] = p.massHoursMonday ?: ""
                            formState["suggestedMassTuesday"] = p.massHoursTuesday ?: ""
                            formState["suggestedMassWednesday"] = p.massHoursWednesday ?: ""
                            formState["suggestedMassThursday"] = p.massHoursThursday ?: ""
                            formState["suggestedMassFriday"] = p.massHoursFriday ?: ""
                            formState["suggestedMassSaturday"] = p.massHoursSaturday ?: ""
                            formState["suggestedConfession"] = p.confessionInfo ?: ""
                            formState["suggestedOfficeHoursText"] = p.officeHoursText ?: ""
                            formState["suggestedPhoneNum"] = p.phoneNum ?: ""
                            formState["suggestedEmail"] = p.email ?: ""
                            formState["suggestedWebsiteUrl"] = p.websiteUrl ?: ""
                            formState["suggestedAdoration"] = p.adorationInfo ?: ""
                            formState["suggestedPastorName"] = p.pastorName ?: ""
                            formState["suggestedDiocese"] = p.diocese ?: ""
                            formState["suggestedDeanery"] = p.deanery ?: ""
                            formState["suggestedFoundingYear"] = p.foundingYear ?: ""
                            formState["suggestedSocialMediaFacebook"] = p.socialMediaFacebook ?: ""
                            formState["suggestedSocialMediaYouTube"] = p.socialMediaYouTube ?: ""
                            formState["suggestedSocialMediaInstagram"] = p.socialMediaInstagram ?: ""
                            formState["suggestedFirstSaturdayOfMonth_hour"] = p.firstSaturdayOfMonthHour ?: ""
                            formState["suggestedFirstSaturdayOfMonth_info"] = p.firstSaturdayOfMonthInfo ?: ""

                            scope.launch { showToast(getString(Res.string.propose_change_fill_current_data_success)) }
                        } else {
                            scope.launch { showToast("Brak danych parafii w pamięci.") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Text(stringResource(Res.string.propose_change_fill_current_data), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun GlassFormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), content = content)
    }
}

@Composable
private fun SectionFormHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily(Font(Res.font.lora_medium)),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun FormLabel(label: String) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF7F8C8D),
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(hint, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.LightGray,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = Color(0xFF1A252F),
            unfocusedTextColor = Color(0xFF1A252F)
        ),
        shape = RoundedCornerShape(8.dp)
    )
}