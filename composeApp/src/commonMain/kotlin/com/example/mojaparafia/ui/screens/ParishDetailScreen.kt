package com.example.mojaparafia.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.mojaparafia.db.ParishEntity
import com.example.mojaparafia.ui.components.AdBanner
import com.example.mojaparafia.ui.components.InlineAdBanner
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParishDetailScreen(
    parish: ParishEntity,
    isHomeParish: Boolean,
    isLandscape: Boolean,
    effectivePremium: Boolean,
    isParishActive: Boolean,
    onBackClick: () -> Unit,
    onProposeChangeClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHomeParish: () -> Unit,
    onCallClick: (String) -> Unit,
    onEmailClick: (String) -> Unit,
    onWebsiteClick: (String) -> Unit,
    onCopyAccountClick: (String) -> Unit,
    onSubmitPriestRequest: (String) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA)).navigationBarsPadding()) {
            Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    HeaderImage(photoUrl = parish.photoUrl, scrollOffset = 0f)
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(16.dp).statusBarsPadding().background(Color.White.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }

                if (!effectivePremium) {
                    Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                        AdBanner(modifier = Modifier.fillMaxWidth(), isPremium = effectivePremium)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(0.6f).fillMaxHeight(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { BasicInfoCard(parish, isHomeParish, onToggleFavorite, onToggleHomeParish) }
                item { LiturgicalCard(parish) }
                if (!effectivePremium) {
                    item { InlineAdCard(isPremium = effectivePremium) }
                }

                item { AnnouncementsCard(parish, isParishActive, onSubmitPriestRequest) }

                item { OfficeCard(parish) }
                item { ContactCard(parish, onCallClick, onEmailClick, onWebsiteClick) }
                item { DonationCard(parish, onCopyAccountClick) }
                item { OrganizationCard(parish) }
                item {
                    Button(
                        onClick = onProposeChangeClick,
                        modifier = Modifier.fillMaxWidth().height(64.dp).padding(bottom = 16.dp),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.parish_details_button_propose_change), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color(0xFFF5F7FA),
            topBar = {
                LargeTopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.padding(8.dp).background(Color.White.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                if (!effectivePremium) {
                    Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                        AdBanner(modifier = Modifier.fillMaxWidth(), isPremium = effectivePremium)
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 80.dp)
            ) {
                item { HeaderImage(photoUrl = parish.photoUrl, scrollOffset = scrollBehavior.state.collapsedFraction) }
                item {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BasicInfoCard(parish, isHomeParish, onToggleFavorite, onToggleHomeParish)
                        LiturgicalCard(parish)
                        if (!effectivePremium) {
                            InlineAdCard(isPremium = effectivePremium)
                        }

                        AnnouncementsCard(parish, isParishActive, onSubmitPriestRequest)

                        OfficeCard(parish)
                        ContactCard(parish, onCallClick, onEmailClick, onWebsiteClick)
                        DonationCard(parish, onCopyAccountClick)
                        OrganizationCard(parish)

                        Button(
                            onClick = onProposeChangeClick,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 48.dp).height(64.dp),
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.parish_details_button_propose_change), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InlineAdCard(isPremium: Boolean) {
    if (isPremium) return

    GlassCardDetail {
        Text(
            text = stringResource(Res.string.adconteiner),
            fontSize = 9.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(8.dp))
        InlineAdBanner(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            isPremium = isPremium
        )
    }
}

@Composable
fun HeaderImage(photoUrl: String?, scrollOffset: Float) {
    val parallaxOffset = scrollOffset * 300f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .graphicsLayer {
                translationY = parallaxOffset
                alpha = 1f - scrollOffset
            }
    ) {
        if (!photoUrl.isNullOrBlank()) {

            Box(modifier = Modifier.fillMaxSize()) {

                Image(
                    painter = painterResource(Res.drawable.image_church),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {

            Image(
                painter = painterResource(Res.drawable.image_church),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun GlassCardDetail(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun BasicInfoCard(parish: ParishEntity, isHomeParish: Boolean, onToggleFavorite: () -> Unit, onToggleHomeParish: () -> Unit) {
    val loraMediumFont = FontFamily(Font(Res.font.lora_medium))
    val noInfo = stringResource(Res.string.parish_details_no_info)

    GlassCardDetail {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ID: ${parish.id}", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(parish.name ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A252F), fontFamily = loraMediumFont)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (parish.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Ulubione",
                        tint = if (parish.isFavorite) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = onToggleHomeParish) {
                    Icon(
                        imageVector = if (isHomeParish) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Parafia domowa",
                        tint = if (isHomeParish) Color(0xFF1976D2) else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        if (isHomeParish) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("To Twoja parafia domowa", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
        }

        if (!parish.foundingYear.isNullOrBlank()) {
            Text(stringResource(Res.string.parish_details_founding_year_format, parish.foundingYear ?: ""), fontSize = 14.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF2C3E50), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(parish.address ?: noInfo, fontSize = 16.sp, color = Color(0xFF2C3E50))
        }
    }
}

@Composable
fun LiturgicalCard(parish: ParishEntity) {
    val noInfo = stringResource(Res.string.parish_details_no_info)

    GlassCardDetail {
        CardMainTitle(stringResource(Res.string.parish_details_liturgical))

        SectionTitle(stringResource(Res.string.parish_details_label_sunday))
        Text(parish.massHoursSunday.takeIf { !it.isNullOrBlank() } ?: noInfo, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A252F))

        AnimatedVisibility(visible = !parish.hasMassSundayHour.isNullOrBlank()) {
            Column {
                SectionTitle(stringResource(Res.string.parish_details_label_vigil))
                Text(parish.hasMassSundayHour ?: "", fontSize = 16.sp, color = Color(0xFF1A252F))
            }
        }

        AnimatedVisibility(visible = !parish.hasMassForChildrenHour.isNullOrBlank()) {
            Column {
                SectionTitle(stringResource(Res.string.parish_details_label_kids))
                Text(parish.hasMassForChildrenHour ?: "", fontSize = 16.sp, color = Color(0xFF1A252F))
            }
        }

        SectionTitle(stringResource(Res.string.parish_details_label_weekdays))
        WeekdayRow("${stringResource(Res.string.propose_change_monday)}:", parish.massHoursMonday.takeIf { !it.isNullOrBlank() } ?: noInfo)
        WeekdayRow("${stringResource(Res.string.propose_change_tuesday)}:", parish.massHoursTuesday.takeIf { !it.isNullOrBlank() } ?: noInfo)
        WeekdayRow("${stringResource(Res.string.propose_change_wednesday)}:", parish.massHoursWednesday.takeIf { !it.isNullOrBlank() } ?: noInfo)
        WeekdayRow("${stringResource(Res.string.propose_change_thursday)}:", parish.massHoursThursday.takeIf { !it.isNullOrBlank() } ?: noInfo)
        WeekdayRow("${stringResource(Res.string.propose_change_friday)}:", parish.massHoursFriday.takeIf { !it.isNullOrBlank() } ?: noInfo)
        WeekdayRow("${stringResource(Res.string.propose_change_saturday)}:", parish.massHoursSaturday.takeIf { !it.isNullOrBlank() } ?: noInfo)

        SectionTitle(stringResource(Res.string.parish_details_first_sat_label))
        Text(parish.firstSaturdayOfMonthHour.takeIf { !it.isNullOrBlank() } ?: noInfo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A252F))
        Text(parish.firstSaturdayOfMonthInfo.takeIf { !it.isNullOrBlank() } ?: noInfo, fontSize = 14.sp, color = Color(0xFF1A252F))

        SectionTitle(stringResource(Res.string.parish_details_label_confession))
        Text(parish.confessionInfo.takeIf { !it.isNullOrBlank() } ?: noInfo, fontSize = 16.sp, color = Color(0xFF1A252F))

        SectionTitle(stringResource(Res.string.parish_details_adoration_info))
        Text(parish.adorationInfo.takeIf { !it.isNullOrBlank() } ?: noInfo, fontSize = 16.sp, color = Color(0xFF1A252F))
    }
}

@Composable
fun AnnouncementsCard(parish: ParishEntity, isParishActive: Boolean, onSubmitPriestRequest: (String) -> Unit) {
    val placeholder = stringResource(Res.string.parish_details_no_announcements_placeholder)
    var showPriestDialog by remember { mutableStateOf(false) }

    GlassCardDetail {
        CardMainTitle(stringResource(Res.string.parish_details_announcements))
        Spacer(Modifier.height(8.dp))

        if (isParishActive) {
            val textRaw = parish.announcements.takeIf { !it.isNullOrBlank() } ?: placeholder
            Text(text = textRaw, fontSize = 15.sp, color = Color.Black)
        } else {
            Text(
                text = stringResource(Res.string.parish_details_no_announcements_yet),
                fontSize = 15.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                onClick = { showPriestDialog = true },
                color = Color(0xFF1976D2).copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(Res.string.parish_details_priest_prompt_title), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A252F))
                        Text(stringResource(Res.string.parish_details_priest_prompt_desc), fontSize = 12.sp, color = Color.DarkGray, lineHeight = 16.sp)
                    }
                }
            }
        }
    }

    if (showPriestDialog) {
        PriestSubscriptionDialog(
            onDismiss = { showPriestDialog = false },
            onSubmit = { email ->
                onSubmitPriestRequest(email)
                showPriestDialog = false
            }
        )
    }
}

@Composable
fun PriestSubscriptionDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { Text(stringResource(Res.string.priest_dialog_title), fontWeight = FontWeight.Bold, color = Color(0xFF1A252F)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.priest_dialog_free_info), fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(Res.string.priest_dialog_email_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(email) },
                enabled = email.isNotBlank() && email.contains("@"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text(stringResource(Res.string.priest_dialog_btn_submit), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.priest_dialog_btn_cancel), color = Color.Gray)
            }
        }
    )
}

@Composable
fun OfficeCard(parish: ParishEntity) {
    val noInfo = stringResource(Res.string.parish_details_no_info)
    GlassCardDetail {
        CardMainTitle(stringResource(Res.string.parish_details_office_hours_title))
        Spacer(Modifier.height(8.dp))
        Text(parish.officeHoursText.takeIf { !it.isNullOrBlank() } ?: noInfo, fontSize = 15.sp, color = Color(0xFF1A252F))
    }
}

@Composable
fun ContactCard(parish: ParishEntity, onCallClick: (String) -> Unit, onEmailClick: (String) -> Unit, onWebsiteClick: (String) -> Unit) {
    val noInfo = stringResource(Res.string.parish_details_no_info)
    GlassCardDetail {
        CardMainTitle(stringResource(Res.string.parish_details_contact_title))
        Spacer(Modifier.height(12.dp))

        ContactRow(Icons.Filled.Call, parish.phoneNum ?: noInfo) {
            parish.phoneNum?.let { onCallClick(it) }
        }

        ContactRow(Icons.Filled.Email, parish.email ?: noInfo) {
            parish.email?.let { onEmailClick(it) }
        }

        ContactRow(Icons.Filled.Info, parish.websiteUrl ?: noInfo) {
            parish.websiteUrl?.let { onWebsiteClick(it) }
        }

        if (!parish.socialMediaFacebook.isNullOrBlank() || !parish.socialMediaYouTube.isNullOrBlank() || !parish.socialMediaInstagram.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            SectionTitle(stringResource(Res.string.parish_details_social_media_title))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SocialIcon(Res.drawable.ic_facebook, parish.socialMediaFacebook, onWebsiteClick)
                SocialIcon(Res.drawable.ic_youtube, parish.socialMediaYouTube, onWebsiteClick)
                SocialIcon(Res.drawable.ic_instagram, parish.socialMediaInstagram, onWebsiteClick)
            }
        }
    }
}

@Composable
fun DonationCard(parish: ParishEntity, onCopyAccountClick: (String) -> Unit) {
    val noInfo = stringResource(Res.string.parish_details_no_info)
    GlassCardDetail {
        CardMainTitle(stringResource(Res.string.parish_details_donation_title))
        Spacer(Modifier.height(8.dp))
        Text(parish.donationInfo.takeIf { !it.isNullOrBlank() } ?: noInfo, fontSize = 14.sp, color = Color(0xFF1A252F))

        Spacer(Modifier.height(6.dp))
        val accNum = parish.bankAccountNumber
        if (!accNum.isNullOrBlank()) {
            Text(
                text = accNum,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                modifier = Modifier.clickable { onCopyAccountClick(accNum) }
            )
        } else {
            Text(noInfo, fontSize = 15.sp, color = Color(0xFF2C3E50))
        }
    }
}

@Composable
fun OrganizationCard(parish: ParishEntity) {
    val hasContent = !parish.pastorName.isNullOrBlank() || !parish.diocese.isNullOrBlank() || !parish.deanery.isNullOrBlank()
    if (hasContent) {
        GlassCardDetail {
            CardMainTitle(stringResource(Res.string.parish_details_organization_title))
            Spacer(Modifier.height(10.dp))
            if (!parish.pastorName.isNullOrBlank()) Text(stringResource(Res.string.parish_details_pastor_name_format, parish.pastorName ?: ""), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A252F))
            if (!parish.diocese.isNullOrBlank()) Text(stringResource(Res.string.parish_details_diocese_format, parish.diocese ?: ""), fontSize = 14.sp, color = Color.Gray)
            if (!parish.deanery.isNullOrBlank()) Text(stringResource(Res.string.parish_details_deanery_format, parish.deanery ?: ""), fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(text = title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
fun WeekdayRow(day: String, hours: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "$day ", fontSize = 15.sp, color = Color(0xFF1A252F), fontWeight = FontWeight.SemiBold)
        Text(text = hours, fontSize = 15.sp, color = Color(0xFF1A252F))
    }
}

@Composable
fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1A252F), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A252F))
    }
}

@Composable
fun SocialIcon(drawableRes: org.jetbrains.compose.resources.DrawableResource, url: String?, onUrlClick: (String) -> Unit) {
    if (!url.isNullOrBlank()) {
        IconButton(onClick = { onUrlClick(url) }, modifier = Modifier.size(48.dp)) {
            Image(painterResource(drawableRes), contentDescription = null)
        }
    }
}

@Composable
fun CardMainTitle(title: String) {
    val loraMediumFont = FontFamily(Font(Res.font.lora_medium))
    Text(text = title, color = Color(0xFF1976D2), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = loraMediumFont, letterSpacing = 1.sp)
}