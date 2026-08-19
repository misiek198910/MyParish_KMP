package com.example.mojaparafia.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.mojaparafia.db.ParishEntity
import com.example.mojaparafia.ui.components.*
import com.example.mojaparafia.ui.map.ParishMap
import com.example.mojaparafia.util.ReminderScheduler
import com.example.mojaparafia.viewmodel.ParishListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: ParishListViewModel,
    reminderScheduler: ReminderScheduler,
    isLandscape: Boolean,
    parishes: List<ParishEntity>,
    homeParishId: String?,
    userHasCrown: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigateToAddParish: (Double, Double) -> Unit,
    onNavigateToDetails: (String) -> Unit,
    showToast: (String) -> Unit,
    isSearchActive: Boolean,
    searchQuery: String,
    showFilterSheet: Boolean,
    onFilterDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var isMapLoaded by rememberSaveable { mutableStateOf(false) }
    var selectedParishId by rememberSaveable { mutableStateOf<String?>(null) }
    var showParishSheet by rememberSaveable { mutableStateOf(false) }
    var clickedLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var showFavoritesPanel by remember { mutableStateOf(false) }
    var showMissingParishBanner by rememberSaveable { mutableStateOf(true) }

    var currentFilterState by remember { mutableStateOf(FilterState()) }
    var displayedParishes by remember { mutableStateOf<List<ParishEntity>>(emptyList()) }

    val mapFocusRequest by viewModel.mapFocusRequest.collectAsState()
    val nearestParishesData by viewModel.nearestParishesState.collectAsState()
    val favoriteParishes by remember(parishes) { derivedStateOf { parishes.filter { it.isFavorite } } }

    LaunchedEffect(searchQuery, currentFilterState, parishes) {
        kotlinx.coroutines.delay(250.milliseconds)
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

    var hasInitialFocusBeenSet by remember { mutableStateOf(false) }
    LaunchedEffect(homeParishId, parishes) {
        if (!hasInitialFocusBeenSet) {
            val homeParish = if (homeParishId != null && parishes.isNotEmpty()) {
                parishes.find { it.id == homeParishId }
            } else null

            kotlinx.coroutines.delay(500.milliseconds)

            if (homeParish != null) {
                viewModel.focusMapOn(homeParish.latitude, homeParish.longitude)
            } else {
                val centerOfPolandLat = 52.0693
                val centerOfPolandLng = 19.4803
                viewModel.focusMapOn(centerOfPolandLat, centerOfPolandLng)
            }

            hasInitialFocusBeenSet = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ParishMap(
            modifier = Modifier.fillMaxSize(),
            parishes = displayedParishes,
            homeParishId = homeParishId,
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

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + if (isLandscape) 44.dp else 64.dp)
        ) {
            AnimatedVisibility(
                visible = showMissingParishBanner,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                MissingParishBanner(onDismiss = { showMissingParishBanner = false })
            }
        }

        // ZMIANA: Dynamiczny padding dolny.
        // Bierzemy 110.dp (aby przeskoczyć nasz nowy dolny pasek i zielony przycisk)
        // i dodajemy do tego wysokość systemowego paska Samsunga / Pixela!
        val bottomSystemInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomMapPadding = 110.dp + bottomSystemInsets

        Box(modifier = Modifier.fillMaxSize()) {
            FabPanelOverlay(
                isLandscape = isLandscape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = bottomMapPadding),
                onFavoritesClick = { showFavoritesPanel = !showFavoritesPanel },
                onLocationClick = { viewModel.requestCurrentLocation() },
                onNearestClick = { viewModel.findNearestParish() }
            )
        }

        MapLoadingOverlay(
            isMapLoaded = isMapLoaded,
            onRetry = { isMapLoaded = true }
        )

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
                        IconButton(onClick = { showFavoritesPanel = false }) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
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
                                            viewModel.focusMapOn(parish.latitude, parish.longitude)
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
                onDismiss = onFilterDismiss,
                onApplyFilters = { newState ->
                    currentFilterState = newState
                    onFilterDismiss()
                }
            )
        }

        if (showParishSheet && selectedParishId != null) {
            val currentParish = parishes.find { it.id == selectedParishId }
            if (currentParish != null) {
                ParishSheet(
                    parish = currentParish,
                    isHomeParish = homeParishId == selectedParishId,
                    isFavorite = currentParish.isFavorite,
                    onDismiss = { showParishSheet = false },
                    onToggleFavorite = { viewModel.toggleFavorite(currentParish) },
                    onToggleHomeParish = { viewModel.toggleHomeParish(currentParish.id) { } },
                    onNavigateToDetails = {
                        showParishSheet = false
                        onNavigateToDetails(currentParish.id)
                    }
                )
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
                            showToast(getString(Res.string.reminder_set_success, przeliczonyCzas))
                        } catch (e: Exception) {
                            showToast("Przypomnienie ustawione: $przeliczonyCzas")
                        }
                    }
                }
            )
        }

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
    }
}