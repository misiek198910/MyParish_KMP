package com.example.mojaparafia.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.mojaparafia.db.ParishEntity
import com.example.mojaparafia.util.ReminderScheduler
import com.example.mojaparafia.ui.components.*
import com.example.mojaparafia.ui.map.ParishMap
import com.example.mojaparafia.viewmodel.ParishListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ParishListViewModel,
    reminderScheduler: ReminderScheduler,
    pushAction: String? = null,
    pushParishId: String? = null,
    onPushHandled: () -> Unit = {},
    showToast: (String) -> Unit,
    isLandscape: Boolean,
    onNavigateToAddParish: (Double, Double) -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToIntentions: (String?) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenAmbassador: () -> Unit,
    onOpenHelp: () -> Unit,
    onBuyCoffee: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onRestartAppRequired: (String?) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadReminders()
    }

    val parishes by viewModel.allParishes.collectAsState(emptyList())
    val isPremium by viewModel.isPremium.collectAsState(false)
    val userPoints by viewModel.userPoints.collectAsState(0)
    val effectivePremium = isPremium || userPoints >= 50

    var isMapLoaded by rememberSaveable { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var selectedParishId by rememberSaveable { mutableStateOf<String?>(null) }
    var showParishSheet by rememberSaveable { mutableStateOf(false) }

    var showSupportDialog by remember { mutableStateOf(false) }
    var clickedLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val homeParishId by viewModel.homeParishId.collectAsState(null)
    val userHasCrown by viewModel.hasCrown.collectAsState(false)

    var showFavoritesPanel by remember { mutableStateOf(false) }
    val favoriteParishes by remember(parishes) {
        derivedStateOf { parishes.filter { it.isFavorite } }
    }
    var hasNewNews by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var showMissingParishBanner by rememberSaveable { mutableStateOf(true) }

    var currentFilterState by remember { mutableStateOf(FilterState()) }
    val mapFocusRequest by viewModel.mapFocusRequest.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var displayedParishes by remember { mutableStateOf<List<ParishEntity>>(emptyList()) }

    var currentScreen by rememberSaveable { mutableStateOf("MAP") }

    LaunchedEffect(pushAction, pushParishId) {
        if (pushAction == "open_intentions") {
            currentScreen = "INTENTION_WALL"
            onPushHandled()
        }
        else if (!pushParishId.isNullOrEmpty()) {
            onNavigateToDetails(pushParishId)
            onPushHandled() 
        }
    }

    LaunchedEffect(searchQuery, currentFilterState, parishes) {
        kotlinx.coroutines.delay(250)
        withContext(Dispatchers.Default) {
            val queryLower = searchQuery.lowercase()

            val filtered = parishes.filter { parish ->
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    (parish.name ?: "").lowercase().contains(queryLower) ||
                            (parish.address ?: "").lowercase().contains(queryLower)
                }
                val matchesCathedral = if (currentFilterState.isCathedral) parish.isCathedral else true
                val matchesChurch = if (currentFilterState.isChurch) !parish.isCathedral else true
                val matchesFavorite = if (currentFilterState.isFavorite) parish.isFavorite else true
                val matchesMultimedia = if (currentFilterState.isMultimedia) !parish.photoUrl.isNullOrBlank() else true
                val matchesRegion = if (currentFilterState.regionQuery.isNotBlank()) {
                    (parish.address ?: "").lowercase().contains(currentFilterState.regionQuery.lowercase())
                } else true
                val matchesChildrenMass = if (currentFilterState.isMassForChildren) parish.hasMassForChildren else true
                val matchesVigil = if (currentFilterState.isVigilMass) parish.hasMassSunday else true
                val matchesConfession = if (currentFilterState.isConfession) !parish.confessionInfo.isNullOrBlank() else true
                val matchesAdoration = if (currentFilterState.isAdoration) !parish.adorationInfo.isNullOrBlank() else true

                matchesSearch && matchesCathedral && matchesChurch && matchesFavorite && matchesRegion &&
                        matchesMultimedia && matchesChildrenMass && matchesVigil && matchesConfession && matchesAdoration
            }
            displayedParishes = filtered
        }
    }

    val nearestParishesData by viewModel.nearestParishesState.collectAsState()

    if (clickedLocation != null) {
        AlertDialog(
            onDismissRequest = { clickedLocation = null },
            title = { Text(stringResource(Res.string.main_dialog_new_parish_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.main_dialog_new_parish_desc)) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    onClick = {
                        val lat = clickedLocation!!.first
                        val lng = clickedLocation!!.second
                        clickedLocation = null
                        onNavigateToAddParish(lat, lng)
                    }
                ) { Text(stringResource(Res.string.main_dialog_btn_yes_add)) }
            },
            dismissButton = {
                TextButton(onClick = { clickedLocation = null }) { Text(stringResource(Res.string.btn_cancel).uppercase(), color = Color.Gray) }
            },
            containerColor = Color.White
        )
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text(stringResource(Res.string.support_project_title), fontWeight = FontWeight.Bold, color = Color(0xFF1A252F)) },
            text = { Text(stringResource(Res.string.support_project_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSupportDialog = false
                        onBuyCoffee()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) { Text(stringResource(Res.string.support_project_btn_coffee)) }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) { Text(stringResource(Res.string.support_project_btn_later), color = Color.Gray) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen && currentScreen == "MAP",
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = Color.White.copy(alpha = 0.85f)
            ) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = stringResource(Res.string.app_name),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(Res.font.lora_medium)),
                        color = Color(0xFF1A252F),
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                    DrawerMenuItem(painter = painterResource(Res.drawable.ic_add_vector), text = stringResource(Res.string.main_drawer_add_parish)) {
                        scope.launch { drawerState.close() }; onNavigateToAddParish(0.0, 0.0)
                    }
                    DrawerMenuItem(painter = painterResource(Res.drawable.ic_notification_vector), text = stringResource(Res.string.news), showBadge = hasNewNews) {
                        scope.launch { drawerState.close() }; onOpenNews()
                    }
                    DrawerMenuItem(painter = rememberVectorPainter(Icons.Filled.Star), text = stringResource(Res.string.ambassador_title)) {
                        scope.launch { drawerState.close() }; onOpenAmbassador()
                    }
                    DrawerMenuItem(painter = rememberVectorPainter(Icons.Default.Info), text = stringResource(Res.string.main_help_center)) {
                        scope.launch { drawerState.close() }; onOpenHelp()
                    }
                    DrawerMenuItem(painter = painterResource(Res.drawable.ic_coffe), text = stringResource(Res.string.support_project_title)) {
                        scope.launch { drawerState.close() }; showSupportDialog = true
                    }
                    DrawerMenuItem(painter = painterResource(Res.drawable.ic_settings_vector), text = stringResource(Res.string.ustawienia)) {
                        scope.launch { drawerState.close() }; currentScreen = "SETTINGS"
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            when (currentScreen) {
                "MAP" -> {
                    var hasInitialFocusBeenSet by remember { mutableStateOf(false) }
                    LaunchedEffect(homeParishId, parishes) {
                        if (!hasInitialFocusBeenSet && homeParishId != null && parishes.isNotEmpty()) {
                            val homeParish = parishes.find { it.id == homeParishId }
                            if (homeParish != null) {
                                kotlinx.coroutines.delay(500)
                                viewModel.focusMapOn(homeParish.latitude, homeParish.longitude)
                                hasInitialFocusBeenSet = true
                            }
                        }
                    }
                    ParishMap(
                        modifier = Modifier.fillMaxSize(),
                        parishes = displayedParishes,
                        homeParishId = homeParishId,
                        userHasCrown = userHasCrown,
                        onMapLoaded = { isMapLoaded = true },
                        focusRequest = mapFocusRequest,
                        onMapFocused = { viewModel.onMapFocused() },
                        onMarkerClick = { id ->
                            selectedParishId = id
                            showParishSheet = true
                        },
                        onMapLongClick = { lat, lng ->
                            clickedLocation = Pair(lat, lng)
                        }
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        GlassTopBar(
                            isLandscape = isLandscape,
                            isSearchActive = isSearchActive,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSearchToggle = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) searchQuery = ""
                            },
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onFilterClick = { showFilterSheet = true },
                            onSearchSubmit = { query -> viewModel.logSearchEvent(query) }
                        )

                        AnimatedVisibility(
                            visible = showMissingParishBanner,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            MissingParishBanner(
                                onDismiss = { showMissingParishBanner = false }
                            )
                        }
                    }

                    val bottomPadding = if (isLandscape) {
                        if (!effectivePremium) 64.dp else 16.dp
                    } else {
                        if (!effectivePremium) 92.dp else 16.dp
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        FabPanelOverlay(
                            isLandscape = isLandscape,
                            isPremium = effectivePremium,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = bottomPadding),
                            onIntentionWallClick = { currentScreen = "INTENTION_WALL" },
                            onFavoritesClick = { showFavoritesPanel = !showFavoritesPanel },
                            onLocationClick = { viewModel.requestCurrentLocation() },
                            onNearestClick = { viewModel.findNearestParish() }
                        )
                    }

                    if (!effectivePremium) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.70f))
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .border(
                                    BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                )
                                .windowInsetsPadding(WindowInsets.navigationBars),
                            contentAlignment = Alignment.Center
                        ) {
                            AdBanner(modifier = Modifier.fillMaxWidth(), isPremium = effectivePremium)
                        }
                    }

                    MapLoadingOverlay(
                        isMapLoaded = isMapLoaded,
                        onRetry = { isMapLoaded = true }
                    )
                }

                "INTENTION_WALL" -> {
                    IntentionWallScreen(
                        viewModel = viewModel,
                        onBackClick = { currentScreen = "MAP" },
                        isGooglePremium = effectivePremium
                    )
                }

                "SETTINGS" -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBackClick = { currentScreen = "MAP" },
                        showToast = showToast,
                        onOpenSubscriptions = onNavigateToSubscriptions,
                        onOpenReminders = onOpenReminders,
                        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                        onOpenSystemSettings = onOpenSystemSettings,
                        onClearAllReminders = {
                            viewModel.remindersList.value.forEach { reminder ->
                                viewModel.removeReminder(reminderScheduler, reminder.notificationId)
                            }
                            scope.launch {
                                showToast(getString(Res.string.settings_cancel_all_success))
                            }
                        },
                        onRestartAppRequired = onRestartAppRequired,
                        isPremium = effectivePremium
                    )
                }
            }

            if (currentScreen == "MAP") {
                if (showFavoritesPanel) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showFavoritesPanel = false }
                    )
                }

                AnimatedVisibility(
                    visible = showFavoritesPanel,
                    enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(320.dp)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (dragAmount > 15) showFavoritesPanel = false
                                }
                            },
                        color = Color.White.copy(alpha = 0.95f),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                            Spacer(modifier = Modifier.height(if (isLandscape) 44.dp else 64.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text(
                                    stringResource(Res.string.main_favorites_title),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A252F)
                                )
                                IconButton(onClick = {
                                    showFavoritesPanel = false
                                }) { Icon(Icons.Filled.Close, contentDescription = null) }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            if (favoriteParishes.isEmpty()) {
                                Text(
                                    stringResource(Res.string.main_favorites_empty),
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 16.dp),
                                    fontSize = 14.sp
                                )
                            } else {
                                LazyColumn {
                                    items(favoriteParishes) { parish ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    showFavoritesPanel = false
                                                    viewModel.focusMapOn(
                                                        parish.latitude,
                                                        parish.longitude
                                                    )
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    painterResource(Res.drawable.ic_mappin_vector),
                                                    contentDescription = null,
                                                    tint = Color(0xFF1976D2),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        showFavoritesPanel = false
                                                        onNavigateToDetails(parish.id)
                                                    }
                                                    .padding(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(48.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.LightGray)
                                                ) {
                                                    if (!parish.photoUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = parish.photoUrl,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Image(
                                                            painter = painterResource(Res.drawable.image_church),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column {
                                                    Text(
                                                        parish.name ?: "",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Black
                                                    )
                                                    Text(
                                                        parish.address ?: "",
                                                        fontSize = 12.sp,
                                                        color = Color.DarkGray
                                                    )
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }

                if (showFilterSheet) {
                    FilterBottomSheet(
                        initialState = currentFilterState,
                        onDismiss = { showFilterSheet = false },
                        onApplyFilters = { newState ->
                            currentFilterState = newState
                            showFilterSheet = false
                        }
                    )
                }

                if (showParishSheet && selectedParishId != null) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    val currentParish = parishes.find { it.id == selectedParishId }
                    val isHomeParish = homeParishId == selectedParishId
                    val isFavorite = currentParish?.isFavorite == true

                    if (currentParish != null) {
                        ModalBottomSheet(
                            onDismissRequest = { showParishSheet = false },
                            sheetState = sheetState,
                            containerColor = Color.White.copy(alpha = 0.90f),
                            scrimColor = Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentParish.name ?: "",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily(Font(Res.font.lora_medium)),
                                            color = Color(0xFF1A252F),
                                            letterSpacing = (-0.25).sp
                                        )

                                        if (isHomeParish) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4CAF50),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    stringResource(Res.string.main_home_parish_label),
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF4CAF50),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = currentParish.address ?: "",
                                            fontSize = 15.sp,
                                            color = Color.DarkGray,
                                            lineHeight = 22.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.toggleFavorite(currentParish) }
                                        ) {
                                            Icon(
                                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                                contentDescription = "Ulubione",
                                                tint = if (isFavorite) Color(0xFFFFC107) else Color.Gray,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.toggleHomeParish(currentParish.id) { } }
                                        ) {
                                            Icon(
                                                imageVector = if (isHomeParish) Icons.Filled.Home else Icons.Outlined.Home,
                                                contentDescription = null,
                                                tint = if (isHomeParish) Color(0xFF1976D2) else Color.Gray,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        showParishSheet = false
                                        onNavigateToDetails(currentParish.id)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1976D2)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        stringResource(Res.string.click_for_details).uppercase(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🙏", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                stringResource(Res.string.main_active_intentions),
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                "${currentParish.active_intentions}",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1A252F)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedButton(
                                        onClick = {
                                            showParishSheet = false
                                            currentScreen = "INTENTION_WALL"
                                        },
                                        border = BorderStroke(1.dp, Color(0xFF1976D2)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF1976D2)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(
                                            horizontal = 14.dp,
                                            vertical = 0.dp
                                        ),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(
                                            stringResource(Res.string.main_btn_go),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }

                if (nearestParishesData != null) {
                    val (sortedParishes, userLat, userLng) = nearestParishesData!!

                    NearestParishSheet(
                        parishes = sortedParishes,
                        userLat = userLat,
                        userLng = userLng,
                        onDismiss = { viewModel.clearNearestParishesState() },
                        onParishFocusChange = { lat, lng ->
                            viewModel.focusMapOn(lat, lng)
                        },
                        onAddReminderClick = { parish, massTime, exactMassDateTime, minutes ->
                            val timeZone = TimeZone.currentSystemDefault()
                            val massInstant = exactMassDateTime.toInstant(timeZone)
                            val reminderInstant = massInstant.minus(minutes, DateTimeUnit.MINUTE)
                            val triggerTime = reminderInstant.toLocalDateTime(timeZone)

                            viewModel.addReminder(reminderScheduler, parish, massTime, triggerTime)
                            viewModel.clearNearestParishesState()

                            val reminderHour = triggerTime.hour.toString().padStart(2, '0')
                            val reminderMinute = triggerTime.minute.toString().padStart(2, '0')
                            val przeliczonyCzas = "$reminderHour:$reminderMinute"

                            coroutineScope.launch {
                                try {
                                    val message = getString(Res.string.reminder_set_success, przeliczonyCzas)
                                    showToast(message)
                                } catch (e: Exception) {
                                    showToast("Przypomnienie ustawione: $przeliczonyCzas")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}