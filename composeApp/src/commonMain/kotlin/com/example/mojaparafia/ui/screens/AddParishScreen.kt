package com.example.mojaparafia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mojaparafia.ui.components.AdBanner
import kotlinx.coroutines.launch
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddParishScreen(
    initialLat: String,
    initialLng: String,
    isPremium: Boolean,
    onBackClick: () -> Unit,
    viewModel: com.example.mojaparafia.viewmodel.ParishListViewModel,
    onSubmitClick: (Map<String, String>) -> Unit,
    showToast: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val customColorScheme = MaterialTheme.colorScheme.copy(
        primary = Color(0xFF1976D2)
    )

    MaterialTheme(colorScheme = customColorScheme) {

        var parishName by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var photoUrl by remember { mutableStateOf("") }
        var latitude by remember { mutableStateOf(initialLat) }
        var longitude by remember { mutableStateOf(initialLng) }
        val mapFocusRequest by viewModel.mapFocusRequest.collectAsState()

        LaunchedEffect(mapFocusRequest) {
            mapFocusRequest?.let { (lat, lon) ->
                latitude = lat.toString()
                longitude = lon.toString()
                viewModel.onMapFocused()
            }
        }

        var massSunday by remember { mutableStateOf("") }
        var massVigil by remember { mutableStateOf("") }
        var massKids by remember { mutableStateOf("") }
        var massMonday by remember { mutableStateOf("") }
        var massTuesday by remember { mutableStateOf("") }
        var massWednesday by remember { mutableStateOf("") }
        var massThursday by remember { mutableStateOf("") }
        var massFriday by remember { mutableStateOf("") }
        var massSaturday by remember { mutableStateOf("") }
        var confession by remember { mutableStateOf("") }
        var adoration by remember { mutableStateOf("") }
        var firstSatHour by remember { mutableStateOf("") }
        var firstSatInfo by remember { mutableStateOf("") }
        var officeHours by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var website by remember { mutableStateOf("") }
        var pastorName by remember { mutableStateOf("") }
        var diocese by remember { mutableStateOf("") }
        var deanery by remember { mutableStateOf("") }
        var foundingYear by remember { mutableStateOf("") }
        var facebook by remember { mutableStateOf("") }
        var youtube by remember { mutableStateOf("") }
        var instagram by remember { mutableStateOf("") }

        val isLocationPreFilled = initialLat.isNotBlank() && initialLng.isNotBlank()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.add_parish_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.add_parish_back_desc))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                if (!isPremium) {
                    Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                        AdBanner(modifier = Modifier.fillMaxWidth(), isPremium = isPremium)
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

                GlassCard {
                    Text(stringResource(Res.string.add_parish_header_basic), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = parishName, onValueChange = { parishName = it }, label = { Text(stringResource(Res.string.add_parish_hint_name)) }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(stringResource(Res.string.add_parish_hint_address)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = photoUrl, onValueChange = { photoUrl = it }, label = { Text(stringResource(Res.string.add_parish_hint_photo)) }, placeholder = { Text(stringResource(Res.string.add_parish_placeholder_photo)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
                }

                GlassCard {
                    Text(stringResource(Res.string.add_parish_header_location), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(Res.string.add_parish_location_desc), fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = latitude, onValueChange = { latitude = it.replace(",", ".") }, label = { Text(stringResource(Res.string.add_parish_hint_lat)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = longitude, onValueChange = { longitude = it.replace(",", ".") }, label = { Text(stringResource(Res.string.add_parish_hint_lng)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.requestCurrentLocation() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLocationPreFilled,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, disabledContainerColor = Color.LightGray, disabledContentColor = Color.DarkGray)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isLocationPreFilled) stringResource(Res.string.add_parish_location_prefilled) else stringResource(Res.string.add_parish_btn_get_location))
                    }
                }

                GlassCard {
                    Text(stringResource(Res.string.add_parish_header_liturgy), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(value = massSunday, onValueChange = { massSunday = it }, label = { Text(stringResource(Res.string.add_parish_hint_mass_sunday)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = massVigil, onValueChange = { massVigil = it }, label = { Text(stringResource(Res.string.add_parish_hint_mass_vigil)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = massKids, onValueChange = { massKids = it }, label = { Text(stringResource(Res.string.add_parish_hint_mass_kids)) }, modifier = Modifier.fillMaxWidth())

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(stringResource(Res.string.add_parish_header_weekdays), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(value = massMonday, onValueChange = { massMonday = it }, label = { Text(stringResource(Res.string.add_parish_hint_mass_mon)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = massTuesday, onValueChange = { massTuesday = it }, label = { Text(stringResource(Res.string.add_parish_hint_mass_tue)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = massWednesday, onValueChange = { massWednesday = it }, label = { Text(stringResource(Res.string.add_parish_hint_mass_wed)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = massThursday, onValueChange = { massThursday = it }, label = { Text(stringResource(Res.string.add_parish_hint_mass_thu)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = massFriday, onValueChange = { massFriday = it }, label = { Text(stringResource(Res.string.add_parish_hint_mass_fri)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = massSaturday, onValueChange = { massSaturday = it }, label = { Text(stringResource(Res.string.add_parish_hint_mass_sat)) }, modifier = Modifier.fillMaxWidth())

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    OutlinedTextField(value = confession, onValueChange = { confession = it }, label = { Text(stringResource(Res.string.add_parish_hint_confession)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = adoration, onValueChange = { adoration = it }, label = { Text(stringResource(Res.string.add_parish_hint_adoration)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(stringResource(Res.string.add_parish_header_first_sat), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = firstSatHour, onValueChange = { firstSatHour = it }, label = { Text(stringResource(Res.string.add_parish_hint_first_sat_hour)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = firstSatInfo, onValueChange = { firstSatInfo = it }, label = { Text(stringResource(Res.string.add_parish_hint_first_sat_info)) }, modifier = Modifier.fillMaxWidth())
                }

                GlassCard {
                    Text(stringResource(Res.string.add_parish_header_office), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(value = officeHours, onValueChange = { officeHours = it }, label = { Text(stringResource(Res.string.add_parish_hint_office_hours)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(Res.string.add_parish_hint_phone)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource(Res.string.add_parish_hint_email)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text(stringResource(Res.string.add_parish_hint_website)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
                }

                GlassCard {
                    Text(stringResource(Res.string.add_parish_header_admin), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(value = pastorName, onValueChange = { pastorName = it }, label = { Text(stringResource(Res.string.add_parish_hint_pastor)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = diocese, onValueChange = { diocese = it }, label = { Text(stringResource(Res.string.add_parish_hint_diocese)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = deanery, onValueChange = { deanery = it }, label = { Text(stringResource(Res.string.add_parish_hint_deanery)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = foundingYear, onValueChange = { foundingYear = it }, label = { Text(stringResource(Res.string.add_parish_hint_founding_year)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }

                GlassCard {
                    Text(stringResource(Res.string.add_parish_header_social), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(value = facebook, onValueChange = { facebook = it }, label = { Text(stringResource(Res.string.add_parish_hint_fb)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = youtube, onValueChange = { youtube = it }, label = { Text(stringResource(Res.string.add_parish_hint_yt)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = instagram, onValueChange = { instagram = it }, label = { Text(stringResource(Res.string.add_parish_hint_ig)) }, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (parishName.isBlank() || latitude.isBlank() || longitude.isBlank()) {
                            scope.launch { showToast(getString(Res.string.add_parish_error_required_fields)) }
                            return@Button
                        }
                        val map = mapOf(
                            "name" to parishName,
                            "address" to address,
                            "latitude" to latitude,
                            "longitude" to longitude,
                            "photoUrl" to photoUrl,
                            "massHoursSunday" to massSunday,
                            "hasMassSundayHour" to massVigil,
                            "hasMassForChildrenHour" to massKids,
                            "massHoursMonday" to massMonday,
                            "massHoursTuesday" to massTuesday,
                            "massHoursWednesday" to massWednesday,
                            "massHoursThursday" to massThursday,
                            "massHoursFriday" to massFriday,
                            "massHoursSaturday" to massSaturday,
                            "confessionInfo" to confession,
                            "adorationInfo" to adoration,
                            "firstSaturdayOfMonth_hour" to firstSatHour,
                            "firstSaturdayOfMonth_info" to firstSatInfo,
                            "officeHoursText" to officeHours,
                            "phoneNum" to phone,
                            "email" to email,
                            "websiteUrl" to website,
                            "pastorName" to pastorName,
                            "diocese" to diocese,
                            "deanery" to deanery,
                            "foundingYear" to foundingYear,
                            "socialMediaFacebook" to facebook,
                            "socialMediaYouTube" to youtube,
                            "socialMediaInstagram" to instagram,
                            "is_new_parish" to "true"
                        )
                        onSubmitClick(map)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(Res.string.add_parish_btn_submit), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
@Composable
fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}