package com.example.mojaparafia.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.mojaparafia.db.ParishEntity
import com.example.mojaparafia.db.ParishEventEntity
import com.example.mojaparafia.util.parseHtmlToAnnotatedString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ParishDetailScreen(
    parish: ParishEntity,
    isHomeParish: Boolean,
    isLandscape: Boolean,
    isParishActive: Boolean,
    events: List<ParishEventEntity> = emptyList(),
    isSyncing: Boolean = false,
    onRefresh: () -> Unit = {},
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

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isSyncing,
        onRefresh = { onRefresh() }
    )

    // Stan zakładek
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabTitles = listOf("Informacje", "Ogłoszenia", "Wydarzenia", "Intencje")

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA)).navigationBarsPadding()) {
            Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    HeaderImage(photoUrl = parish.photoUrl, scrollOffset = 0f)
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .pullRefresh(pullRefreshState)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { BasicInfoCard(parish, isHomeParish, onToggleFavorite, onToggleHomeParish) }
                    item { QuickActionRow(parish, onCallClick, onWebsiteClick, onEmailClick, onCopyAccountClick) }

                    item {
                        ParishTabs(selectedTab = selectedTab, tabTitles = tabTitles, onTabSelected = { selectedTab = it })
                    }

                    item {
                        Crossfade(targetState = selectedTab, label = "TabContent") { tab ->
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                when (tab) {
                                    0 -> { // Informacje
                                        LiturgicalCard(parish)
                                        OfficeCard(parish)
                                        OrganizationCard(parish)
                                        DonationCard(parish, onCopyAccountClick)

                                        Button(
                                            onClick = onProposeChangeClick,
                                            modifier = Modifier.fillMaxWidth().height(64.dp).padding(top = 8.dp, bottom = 12.dp),
                                            shape = RoundedCornerShape(32.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(Res.string.parish_details_button_propose_change), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    1 -> { // Ogłoszenia
                                        AnnouncementsCard(parish, isParishActive, onSubmitPriestRequest)
                                    }
                                    2 -> { // Wydarzenia
                                        EventsCard(events)
                                    }
                                    3 -> { // Intencje
                                        IntentionsCard(parish)
                                    }
                                }
                            }
                        }
                    }
                }

                PullRefreshIndicator(
                    refreshing = isSyncing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    contentColor = Color(0xFF1976D2),
                    backgroundColor = Color.White
                )
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color(0xFFF5F7FA),
            topBar = {
                LargeTopAppBar(
                    title = { Text("") },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 80.dp)
                ) {
                    item { HeaderImage(photoUrl = parish.photoUrl, scrollOffset = scrollBehavior.state.collapsedFraction) }

                    item {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            BasicInfoCard(parish, isHomeParish, onToggleFavorite, onToggleHomeParish)
                            QuickActionRow(parish, onCallClick, onWebsiteClick, onEmailClick, onCopyAccountClick)

                            ParishTabs(selectedTab = selectedTab, tabTitles = tabTitles, onTabSelected = { selectedTab = it })

                            Crossfade(targetState = selectedTab, label = "TabContentPortrait") { tab ->
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    when (tab) {
                                        0 -> { // Informacje
                                            LiturgicalCard(parish)
                                            OfficeCard(parish)
                                            OrganizationCard(parish)
                                            DonationCard(parish, onCopyAccountClick)

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
                                        1 -> { // Ogłoszenia
                                            AnnouncementsCard(parish, isParishActive, onSubmitPriestRequest)
                                        }
                                        2 -> { // Wydarzenia
                                            EventsCard(events)
                                        }
                                        3 -> { // Intencje
                                            IntentionsCard(parish)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                PullRefreshIndicator(
                    refreshing = isSyncing,
                    state = pullRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = innerPadding.calculateTopPadding()),
                    contentColor = Color(0xFF1976D2),
                    backgroundColor = Color.White
                )
            }
        }
    }
}

@Composable
fun ParishTabs(selectedTab: Int, tabTitles: List<String>, onTabSelected: (Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = Color(0xFF1976D2),
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            if (selectedTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF1976D2),
                    height = 3.dp
                )
            }
        },
        divider = {
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
        }
    ) {
        tabTitles.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                },
                selectedContentColor = Color(0xFF1976D2),
                unselectedContentColor = Color.Gray
            )
        }
    }
}

@Composable
fun QuickActionRow(parish: ParishEntity, onCallClick: (String) -> Unit, onWebsiteClick: (String) -> Unit, onEmailClick: (String) -> Unit, onCopyAccountClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!parish.phoneNum.isNullOrBlank()) {
            QuickActionButton(Icons.Filled.Call, "Zadzwoń") { onCallClick(parish.phoneNum) }
        }
        if (!parish.websiteUrl.isNullOrBlank()) {
            QuickActionButton(Icons.Filled.Info, "Strona") { onWebsiteClick(parish.websiteUrl) }
        }
        if (!parish.email.isNullOrBlank()) {
            QuickActionButton(Icons.Filled.Email, "E-mail") { onEmailClick(parish.email) }
        }
        if (!parish.bankAccountNumber.isNullOrBlank()) {
            QuickActionButton(Icons.Filled.Star, "Wsparcie") { onCopyAccountClick(parish.bankAccountNumber) }
        }
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF1976D2),
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, color = Color(0xFF1A252F), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HeaderImage(photoUrl: String?, scrollOffset: Float) {
    val parallaxOffset = scrollOffset * 300f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFFF5F7FA)),
                        startY = 400f
                    )
                )
        )
    }
}

@Composable
fun GlassCardDetail(modifier: Modifier = Modifier, containerColor: Color = Color.White.copy(alpha = 0.95f), content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun BasicInfoCard(parish: ParishEntity, isHomeParish: Boolean, onToggleFavorite: () -> Unit, onToggleHomeParish: () -> Unit) {
    val noInfo = stringResource(Res.string.parish_details_no_info)

    GlassCardDetail(modifier = Modifier.offset(y = (-30).dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                if (isHomeParish) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Parafia Domowa", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text("ID: ${parish.id}", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))

                Text(parish.name ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A252F), lineHeight = 30.sp, letterSpacing = (-0.5).sp)
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

        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(parish.address ?: noInfo, fontSize = 15.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun AnnouncementsCard(parish: ParishEntity, isParishActive: Boolean, onSubmitPriestRequest: (String) -> Unit) {
    val placeholder = stringResource(Res.string.parish_details_no_announcements_placeholder)
    var showPriestDialog by remember { mutableStateOf(false) }

    GlassCardDetail(containerColor = Color.White) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFFFF3E0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("📢", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            CardMainTitle(stringResource(Res.string.details_announcement))
        }

        Spacer(Modifier.height(12.dp))

        if (isParishActive) {
            val textRaw = parish.announcements.takeIf { !it.isNullOrBlank() } ?: placeholder
            val formattedText = remember(textRaw) { parseHtmlToAnnotatedString(textRaw) }

            Text(
                text = formattedText,
                fontSize = 15.sp,
                color = Color(0xFF333333),
                lineHeight = 22.sp
            )
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
                shape = RoundedCornerShape(16.dp),
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
fun IntentionsCard(parish: ParishEntity) {
    val placeholder = "Brak intencji mszalnych na najbliższe dni."

    GlassCardDetail(containerColor = Color.White) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFF3E5F5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🙏", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            CardMainTitle("Intencje Mszalne")
        }

        Spacer(Modifier.height(12.dp))

        val textRaw = parish.intentions.takeIf { !it.isNullOrBlank() } ?: placeholder
        val formattedText = remember(textRaw) { parseHtmlToAnnotatedString(textRaw) }

        Text(
            text = formattedText,
            fontSize = 15.sp,
            color = Color(0xFF333333),
            lineHeight = 22.sp
        )
    }
}

@Composable
fun DonationCard(parish: ParishEntity, onCopyAccountClick: (String) -> Unit) {
    val noInfo = stringResource(Res.string.parish_details_no_info)

    GlassCardDetail(containerColor = Color(0xFFE3F2FD)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFBBDEFB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF1976D2))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.parish_details_donation_title),
                color = Color(0xFF1565C0),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(parish.donationInfo.takeIf { !it.isNullOrBlank() } ?: noInfo,
            fontSize = 14.sp,
            color = Color(0xFF1A252F))

        Spacer(Modifier.height(12.dp))

        val isBankAvailable = !parish.bankAccountNumber.isNullOrBlank()
        val bankBorderColor = if (isBankAvailable) Color(0xFF4CAF50) else Color(0xFFE53935)
        val bankTextColor = if (isBankAvailable) Color(0xFF2E7D32) else Color(0xFFC62828)
        val bankText = if (isBankAvailable) parish.bankAccountNumber!! else "Brak numeru konta"

        Surface(
            onClick = { if (isBankAvailable) onCopyAccountClick(parish.bankAccountNumber!!) },
            enabled = isBankAvailable,
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, bankBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("💳", fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = bankText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = bankTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isBankAvailable) {
                    Text(
                        "Kopiuj",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val isBlikAvailable = !parish.blikNumber.isNullOrBlank()
        val blikBorderColor = if (isBlikAvailable) Color(0xFF4CAF50) else Color(0xFFE53935)
        val blikTextColor = if (isBlikAvailable) Color(0xFF2E7D32) else Color(0xFFC62828)
        val blikTextDisplay = if (isBlikAvailable) parish.blikNumber!! else "Brak numeru telefonu"

        Surface(
            onClick = { if (isBlikAvailable) onCopyAccountClick(parish.blikNumber!!) },
            enabled = isBlikAvailable,
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, blikBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.blik_logo),
                        contentDescription = "Logo BLIK",
                        modifier = Modifier
                            .height(22.dp)
                            .widthIn(max = 50.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = blikTextDisplay,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = blikTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isBlikAvailable) {
                    Text(
                        "Kopiuj",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
@Composable
fun EventsCard(events: List<ParishEventEntity>) {
    GlassCardDetail(containerColor = Color.White) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("📅", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            CardMainTitle(stringResource(Res.string.events_card_title))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (events.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.events_empty_title),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.events_empty_desc),
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                events.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(Color(0xFF1976D2).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = parseEventDate(event.eventDate),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1976D2)
                            )
                            Text(
                                text = parseEventTime(event.eventDate),
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1A252F)
                            )
                            if (!event.description.isNullOrBlank()) {
                                Text(
                                    text = event.description,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
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
fun OfficeCard(parish: ParishEntity) {
    val noInfo = stringResource(Res.string.parish_details_no_info)
    GlassCardDetail {
        CardMainTitle(stringResource(Res.string.parish_details_office_hours_title))
        Spacer(Modifier.height(8.dp))
        Text(parish.officeHoursText.takeIf { !it.isNullOrBlank() } ?: noInfo, fontSize = 15.sp, color = Color(0xFF1A252F))
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
fun CardMainTitle(title: String) {
    Text(text = title, color = Color(0xFF1976D2), fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

private fun parseEventDate(rawDateTime: String): String {
    return try {
        val datePart = rawDateTime.split(" ")[0]
        val parts = datePart.split("-")
        val day = parts[2]
        val monthNum = parts[1].toInt()
        val months = listOf("STY", "LUT", "MAR", "KWI", "MAJ", "CZE", "LIP", "SIE", "WRZ", "PAŹ", "LIS", "GRU")
        "$day ${months.getOrElse(monthNum - 1) { "" }}"
    } catch (e: Exception) {
        "DZIŚ"
    }
}

private fun parseEventTime(rawDateTime: String): String {
    return try {
        val timePart = rawDateTime.split(" ")[1]
        timePart.substring(0, 5)
    } catch (e: Exception) {
        ""
    }
}