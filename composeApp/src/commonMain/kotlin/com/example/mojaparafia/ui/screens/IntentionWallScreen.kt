package com.example.mojaparafia.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import com.example.mojaparafia.model.Candle
import com.example.mojaparafia.model.Intention
import com.example.mojaparafia.model.PrayingParish
import com.example.mojaparafia.viewmodel.ParishListViewModel
import myparish.composeapp.generated.resources.*
import myparish.composeapp.generated.resources.Res
import kotlin.math.abs

// Importy Platformowe KMP i zasobów
import com.example.mojaparafia.showPlatformToast
import com.example.mojaparafia.generateAndShareIntentionImage
import com.example.mojaparafia.navigateToMap
import com.example.mojaparafia.AdBannerView
import com.example.mojaparafia.db.ParishEntity
import com.example.mojaparafia.isLandscapeOrientation
import com.example.mojaparafia.ui.components.AdBanner
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.collections.find

private val StickyNotePalettes = listOf(
    Color(0xFFFFEFA1), Color(0xFFFFC0CB), Color(0xFFAEEEEE),
    Color(0xFFF0FFF0), Color(0xFFFFDAB9), Color(0xFFE6E6FA)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionWallScreen(
    viewModel: ParishListViewModel,
    onBackClick: () -> Unit,
    isGooglePremium: Boolean
) {
    val intentions by viewModel.intentions.collectAsState(emptyList())
    val isLoading by viewModel.isLoading.collectAsState(false)
    val allParishes by viewModel.allParishes.collectAsState(emptyList())
    val homeParishId by viewModel.homeParishId.collectAsState(null)
    val userPoints by viewModel.userPoints.collectAsState(0)

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showFilterSheet by remember { mutableStateOf(false) }
    var showParishPrompt by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var intentionToShare by remember { mutableStateOf<Intention?>(null) }
    var intentionToEdit by remember { mutableStateOf<Intention?>(null) }
    var intentionToDelete by remember { mutableStateOf<Intention?>(null) }
    var candleForPreview by remember { mutableStateOf<Candle?>(null) }
    var intentionToRenew by remember { mutableStateOf<Intention?>(null) }

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredIntentions = remember(intentions, selectedFilter, homeParishId, viewModel.deviceId) {
        when (selectedFilter) {
            "ALL" -> intentions
            "MINE" -> intentions.filter { it.creatorId == viewModel.deviceId.value }
            "HOME_PARISH" -> intentions.filter { it.authorParishId == homeParishId }
            else -> intentions.filter { it.category == selectedFilter }
        }
    }

    val isLandscape = isLandscapeOrientation()
    val effectivePremium = isGooglePremium || userPoints >= 50

    val toastCandleLit = stringResource(Res.string.intention_wall_toast_candle_lit)
    val toastCandleError = stringResource(Res.string.intention_wall_toast_candle_error)
    val toastAlreadyLit = stringResource(Res.string.intention_wall_toast_already_lit)
    val toastDeleted = stringResource(Res.string.intention_wall_toast_deleted)
    val toastAdded = stringResource(Res.string.intention_wall_toast_added)
    val toastUpdated = stringResource(Res.string.intention_wall_toast_updated)
    val toastUpdateError = stringResource(Res.string.intention_wall_toast_update_error)
    val toastCandleExtinguished = stringResource(Res.string.intention_wall_toast_candle_extinguished)
    val toastCandleExtinguishError = stringResource(Res.string.intention_wall_toast_candle_extinguish_error)
    val toastRenewError = stringResource(Res.string.intention_wall_toast_renew_error)

    LaunchedEffect(homeParishId) {
        showParishPrompt = (homeParishId == null)
    }

    Scaffold(
        containerColor = Color(0xFFDCDCDC),
        bottomBar = {
            if (!effectivePremium) {
                Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                    AdBanner(modifier = Modifier.fillMaxWidth(), isPremium = effectivePremium)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (homeParishId == null) showParishPrompt = true else showAddDialog = true },
                containerColor = if (homeParishId == null) Color.Gray else MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.intention_wall_cd_add), tint = Color.White)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && intentions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (isLandscape) 85.dp else 16.dp,
                        end = 16.dp,
                        top = paddingValues.calculateTopPadding() + 80.dp,
                        bottom = paddingValues.calculateBottomPadding() + 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(
                        items = filteredIntentions,
                        key = { it.id },
                        contentType = { "intention_card" }
                    ) { intention ->
                        val stickyNoteColor = remember(intention.id) {
                            val index = (intention.id * 31).hashCode()
                            StickyNotePalettes[abs(index) % StickyNotePalettes.size]
                        }
                        IntentionCard(
                            intention = intention,
                            backgroundColor = stickyNoteColor,
                            myDeviceId = viewModel.deviceId.value,
                            allParishes = allParishes,
                            onPrayClick = { if (homeParishId == null) showParishPrompt = true else viewModel.prayForIntention(intention.id) },
                            onDeleteClick = { intentionToDelete = intention },
                            onEditClick = { intentionToEdit = intention },
                            onShareClick = { intentionToShare = intention },
                            onCandleClick = {
                                if (homeParishId == null) {
                                    showParishPrompt = true
                                } else {
                                    val hasAlreadyLit = intention.candles?.any { it.deviceId == viewModel.deviceId.value } == true
                                    if (hasAlreadyLit) {
                                        showPlatformToast(toastAlreadyLit)
                                    } else {
                                        val isForDeceased = intention.category == "Za zmarłych"
                                        val type = if (isForDeceased) "znicz" else "candle"

                                        viewModel.lightCandle(intention.id, type, 24) { success ->
                                            if(success) {
                                                showPlatformToast(toastCandleLit)
                                                viewModel.fetchIntentions()
                                                viewModel.fetchUserStats()
                                            } else {
                                                showPlatformToast(toastCandleError)
                                            }
                                        }
                                    }
                                }
                            },
                            onSingleCandleClick = { candle -> candleForPreview = candle },
                            onRenewClick = { intentionToRenew = intention },
                            onPinClick = { viewModel.togglePin(intention.id) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.8f), CircleShape).size(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.intention_wall_cd_back), tint = Color.Black)
                }

                IconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.8f), CircleShape).size(44.dp)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = stringResource(Res.string.intention_wall_filter_menu_cd), tint = Color.Black)
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(Res.string.intention_wall_filter_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A252F),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                val baseFilters = listOf("ALL", "MINE")
                val homeParishFilter = if (homeParishId != null) listOf("HOME_PARISH") else emptyList()
                val categoryFilters = listOf("O zdrowie", "Za zmarłych", "Dziękczynna", "Ogólna")
                val filters = baseFilters + homeParishFilter + categoryFilters

                filters.forEach { filterKey ->
                    val isSelected = selectedFilter == filterKey
                    val filterName = when (filterKey) {
                        "ALL" -> stringResource(Res.string.filter_all)
                        "MINE" -> stringResource(Res.string.filter_mine)
                        "HOME_PARISH" -> stringResource(Res.string.filter_home_parish)
                        else -> stringResource(getCategoryResId(filterKey))
                    }
                    val emoji = when (filterKey) {
                        "ALL" -> "🌍"
                        "MINE" -> "👤"
                        "HOME_PARISH" -> "⛪"
                        else -> getCategoryEmoji(filterKey)
                    }

                    Surface(
                        onClick = {
                            selectedFilter = filterKey
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) { showFilterSheet = false }
                            }
                        },
                        color = if (isSelected) Color(0xFF1976D2).copy(alpha = 0.1f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = filterName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 16.sp,
                                color = if (isSelected) Color(0xFF1976D2) else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }

    if (candleForPreview != null) {
        BigCandlePreviewDialog(
            candle = candleForPreview!!,
            myDeviceId = viewModel.deviceId.value,
            onDismiss = { candleForPreview = null },
            onExtinguish = {
                viewModel.extinguishCandle(candleForPreview!!.id) { success ->
                    if (success) {
                        showPlatformToast(toastCandleExtinguished)
                        viewModel.fetchIntentions()
                    } else {
                        showPlatformToast(toastCandleExtinguishError)
                    }
                }
                candleForPreview = null
            }
        )
    }

    if (intentionToDelete != null) {
        AlertDialog(
            onDismissRequest = { intentionToDelete = null }, containerColor = Color.White, shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(Res.string.intention_wall_delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.intention_wall_delete_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        intentionToDelete?.id?.let {
                            viewModel.deleteIntention(it)
                            viewModel.fetchIntentions()
                        }
                        showPlatformToast(toastDeleted)
                        intentionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text(stringResource(Res.string.intention_wall_btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { intentionToDelete = null }) {
                    Text(stringResource(Res.string.intention_wall_btn_cancel), color = Color.Gray)
                }
            }
        )
    }

    if (showParishPrompt) {
        AlertDialog(
            onDismissRequest = { showParishPrompt = false }, shape = RoundedCornerShape(24.dp), containerColor = Color.White,
            title = { Text(stringResource(Res.string.intention_wall_prompt_parish_title), fontFamily = FontFamily(Font(Res.font.lora_medium)), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.intention_wall_prompt_parish_desc)) },
            confirmButton = { Button(onClick = { showParishPrompt = false; onBackClick() }) { Text(stringResource(Res.string.intention_wall_prompt_parish_btn_map)) } },
            dismissButton = { TextButton(onClick = { showParishPrompt = false }) { Text(stringResource(Res.string.intention_wall_prompt_parish_btn_later), color = Color.Gray) } }
        )
    }

    if (showAddDialog) {
        AddIntentionDialog(
            isLandscape = isLandscape,
            onDismiss = { showAddDialog = false },
            onConfirm = { text, category, isAnonymous ->

                viewModel.addIntention(text, category, isAnonymous, "PL") { success ->
                    if (success) {
                        showPlatformToast(toastAdded)
                        viewModel.fetchIntentions()
                    }
                }
            }
        )
    }

    if (intentionToEdit != null) {
        EditIntentionDialog(
            intention = intentionToEdit!!,
            isLandscape = isLandscape,
            onDismiss = { intentionToEdit = null },
            onConfirm = { text, category, isAnonymous ->
                viewModel.updateIntention(intentionToEdit!!.id, text, category, isAnonymous) { success ->
                    if (success) {
                        showPlatformToast(toastUpdated)
                        viewModel.fetchIntentions()
                        intentionToEdit = null
                    } else {
                        showPlatformToast(toastUpdateError)
                    }
                }
            }
        )
    }

    if (intentionToShare != null) {
        ShareIntentionDialog(intention = intentionToShare!!, onDismiss = { intentionToShare = null })
    }

    val toastRenewedTemplate = stringResource(Res.string.intention_wall_toast_renewed)
    if (intentionToRenew != null) {
        RenewIntentionDialog(
            isLandscape = isLandscape,
            onDismiss = { intentionToRenew = null },
            onConfirm = { days ->
                viewModel.renewIntention(intentionToRenew!!.id, days) { success ->
                    if (success) {
                        showPlatformToast(toastRenewedTemplate.replace("%1\$d", days.toString()))
                        viewModel.fetchIntentions()
                    } else {
                        showPlatformToast(toastRenewError)
                    }
                }
                intentionToRenew = null
            }
        )
    }
}

@Composable
fun IntentionCard(
    intention: Intention,
    backgroundColor: Color,
    myDeviceId: String,
    allParishes: List<ParishEntity>,
    onPrayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onRenewClick: () -> Unit,
    onPinClick: () -> Unit,
    onShareClick: () -> Unit,
    onCandleClick: () -> Unit,
    onSingleCandleClick: (Candle) -> Unit
) {
    val loraMediumFont = FontFamily(Font(Res.font.lora_medium))
    val isMyIntention = intention.creatorId == myDeviceId
    var expandedCommunity by remember { mutableStateOf(false) }
    var isTextExpanded by remember { mutableStateOf(false) }

    val categoryName = intention.category ?: "Ogólna"
    val categoryLabel = stringResource(getCategoryResId(categoryName))
    val categoryEmoji = getCategoryEmoji(categoryName)
    val flagEmoji = getFlagEmoji(intention.country)

    val authorParishName = remember(intention.isAnonymous, intention.authorParishId, allParishes) {
        if (intention.isAnonymous) null else allParishes.find { it.id == intention.authorParishId }?.name
    }

    val ADMIN_ID = "ca31b6b0a75f655b"
    val finalBackgroundColor = if (intention.isPinned) Color(0xFFFFD700) else backgroundColor
    val finalElevation = if (intention.isPinned) 12.dp else 6.dp

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = finalBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = finalElevation)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {

                    if (intention.isPinned) {
                        Text(
                            text = stringResource(Res.string.intention_wall_pinned_admin),
                            color = Color.Black.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Surface(color = Color.Transparent, modifier = Modifier.padding(top = if (intention.isPinned) 0.dp else 8.dp)) {
                        val anonymousStr = stringResource(Res.string.cat_anonymous)
                        val headerText = if (intention.isAnonymous) {
                            "$flagEmoji $categoryEmoji ${categoryLabel.uppercase()} • $anonymousStr"
                        } else {
                            "$flagEmoji $categoryEmoji ${categoryLabel.uppercase()}"
                        }

                        Text(
                            text = headerText,
                            color = Color.Black.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold
                        )
                    }

                    val timeInfo = getTimeInfo(intention.createdAt)
                    if (timeInfo.isNotEmpty()) {
                        Text(text = timeInfo, fontSize = 10.sp, color = Color.Black.copy(alpha = 0.6f), fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
                    }

                    if (!authorParishName.isNullOrBlank()) {
                        Text(
                            text = stringResource(Res.string.intention_wall_author_from, authorParishName),
                            fontSize = 11.sp,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(modifier = Modifier.padding(start = 8.dp)) {
                    IconButton(onClick = onShareClick, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                    }

                    if (myDeviceId == ADMIN_ID) {
                        IconButton(onClick = onPinClick, modifier = Modifier.size(36.dp)) {
                            if (intention.isPinned) {
                                Text(text = "📌", fontSize = 18.sp)
                            } else {
                                Text(text = "📍", fontSize = 18.sp, modifier = Modifier.graphicsLayer { alpha = 0.4f })
                            }
                        }
                    }

                    if (isMyIntention) {
                        IconButton(onClick = onRenewClick, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = intention.content,
                modifier = Modifier.fillMaxWidth().clickable { isTextExpanded = !isTextExpanded },
                fontSize = 17.sp, fontFamily = loraMediumFont, color = Color.Black.copy(alpha = 0.8f),
                lineHeight = 26.sp, maxLines = if (isTextExpanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis
            )

            if (!intention.candles.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.4f)).padding(8.dp)
                ) {
                    items(intention.candles) { candle ->
                        Box(modifier = Modifier.clickable { onSingleCandleClick(candle) }) {
                            StageCandle(expiresAt = candle.expiresAt)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                val activeCandlesCount = intention.candles?.size ?: 0

                Row(modifier = Modifier.clickable { expandedCommunity = !expandedCommunity }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(Res.drawable.ic_pray_vector), contentDescription = null, tint = if (intention.prayedByMe) Color(0xFF1976D2) else Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(text = "${intention.prayerCount}", fontWeight = FontWeight.Bold, color = if (intention.prayedByMe) Color(0xFF1976D2) else Color.Black.copy(alpha = 0.5f))

                    Spacer(Modifier.width(14.dp))
                    Text(text = "🕯️", fontSize = 16.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$activeCandlesCount",
                        fontWeight = FontWeight.Bold,
                        color = if (activeCandlesCount > 0) Color(0xFFFF9800) else Color.Black.copy(alpha = 0.5f)
                    )

                    Spacer(Modifier.width(6.dp))
                    Icon(imageVector = if (expandedCommunity) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Black.copy(alpha = 0.4f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCandleClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)), shape = RoundedCornerShape(12.dp)) {
                        Text(stringResource(Res.string.intention_wall_btn_light).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Button(onClick = onPrayClick, colors = ButtonDefaults.buttonColors(containerColor = if (intention.prayedByMe) Color.Gray else MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(12.dp)) {
                        val prayStr = if (intention.prayedByMe) stringResource(Res.string.intention_wall_btn_pray_stop) else stringResource(Res.string.intention_wall_btn_pray_start)
                        Text(prayStr.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            AnimatedVisibility(visible = expandedCommunity) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(thickness = 0.5.dp, color = Color.Black.copy(alpha = 0.1f))
                    Text(text = stringResource(Res.string.intention_wall_praying_from), fontSize = 11.sp, color = Color.Black.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    val groupedParishes = remember(intention.prayingParishes) { intention.prayingParishes?.groupBy { it.id }?.map { (_, list) -> list.first() to list.size }?.sortedByDescending { it.second } }
                    if (!groupedParishes.isNullOrEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            groupedParishes.forEach { (parish, count) -> ParishMiniChip(parish, count, allParishes) { navigateToMap(parish.id, parish.lat, parish.lon) } }
                        }
                    } else {
                        Text(stringResource(Res.string.intention_wall_be_first), fontSize = 11.sp, color = Color.Black.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun StageCandle(expiresAt: String) {
    val expireTime = remember(expiresAt) { parseServerTime(expiresAt).toEpochMilliseconds() }
    val currentTime = Clock.System.now().toEpochMilliseconds()
    val totalTimeMillis = 8 * 60 * 60 * 1000L
    val timeRemaining = expireTime - currentTime
    val percentRemaining = (timeRemaining.toFloat() / totalTimeMillis.toFloat()).coerceIn(0f, 1f)
    val isExpired = currentTime >= expireTime

    val resId = when {
        isExpired -> Res.drawable.candle_stage_25
        percentRemaining > 0.75f -> Res.drawable.candle_stage_100
        percentRemaining > 0.50f -> Res.drawable.candle_stage_75
        percentRemaining > 0.25f -> Res.drawable.candle_stage_50
        else -> Res.drawable.candle_stage_25
    }

    val glowOffsetY = when {
        percentRemaining > 0.75f -> (-22).dp
        percentRemaining > 0.50f -> (-8).dp
        percentRemaining > 0.25f -> 4.dp
        else -> 18.dp
    }

    val expiredColorFilter = remember(isExpired) {
        if (isExpired) {
            val matrix = ColorMatrix().apply { setToSaturation(0f) }
            ColorFilter.colorMatrix(matrix)
        } else null
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fireFlicker")
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flickerAlpha"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val glowBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFFF9800).copy(alpha = 0.8f),
                Color(0xFFFFC107).copy(alpha = 0.3f),
                Color.Transparent
            )
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            if (!isExpired) {
                Box(
                    modifier = Modifier
                        .offset(y = glowOffsetY)
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = glowScale
                            scaleY = glowScale
                            alpha = flickerAlpha
                        }
                        .background(brush = glowBrush, shape = CircleShape)
                )
            }

            Image(
                painter = painterResource(resId),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .graphicsLayer { alpha = if (isExpired) 0.5f else 1f },
                colorFilter = expiredColorFilter,
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun AddIntentionDialog(onDismiss: () -> Unit, isLandscape: Boolean, onConfirm: (String, String, Boolean) -> Unit) {
    var text by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    val categories = listOf("O zdrowie", "Za zmarłych", "Dziękczynna", "Ogólna")
    var selectedCategory by remember { mutableStateOf(categories.last()) }
    val loraMediumFont = FontFamily(Font(Res.font.lora_medium))

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        modifier = Modifier.widthIn(max = if (isLandscape) 600.dp else 400.dp),
        title = { Text(stringResource(Res.string.intention_dialog_add_title), fontFamily = loraMediumFont, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        Surface(shape = RoundedCornerShape(16.dp), color = if (isSelected) Color(0xFF1976D2) else Color.Transparent, border = BorderStroke(1.dp, if (isSelected) Color.Transparent else Color.LightGray), modifier = Modifier.clickable { selectedCategory = cat }) {
                            Text(text = "${getCategoryEmoji(cat)} ${stringResource(getCategoryResId(cat))}", color = if (isSelected) Color.White else Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 1000) text = it },
                    placeholder = { Text(stringResource(Res.string.intention_wall_dialog_hint)) },
                    modifier = Modifier.fillMaxWidth().height(if (isLandscape) 100.dp else 200.dp),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 3,
                    maxLines = 10,
                    supportingText = { Text("${text.length}/1000") }
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { isAnonymous = !isAnonymous }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAnonymous, onCheckedChange = { isAnonymous = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1976D2)))
                    Text(stringResource(Res.string.intention_dialog_anonymous), fontSize = 14.sp, color = if (isAnonymous) Color(0xFF1976D2) else Color.Gray, fontWeight = if (isAnonymous) FontWeight.Bold else FontWeight.Normal)
                }
            }
        },
        confirmButton = { Button(onClick = { if (text.isNotBlank()) { onConfirm(text, selectedCategory, isAnonymous); onDismiss() } }, enabled = text.isNotBlank()) { Text(stringResource(Res.string.intention_wall_dialog_btn_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.btn_cancel), color = Color.Gray) } }
    )
}

@Composable
fun EditIntentionDialog(intention: Intention, isLandscape: Boolean, onDismiss: () -> Unit, onConfirm: (String, String, Boolean) -> Unit) {
    var text by remember { mutableStateOf(intention.content) }
    // 🔥 POPRAWKA: is_anonymous -> isAnonymous
    var isAnonymous by remember { mutableStateOf(intention.isAnonymous) }
    val categories = listOf("O zdrowie", "Za zmarłych", "Dziękczynna", "Ogólna")
    var selectedCategory by remember { mutableStateOf(intention.category ?: "Ogólna") }
    val loraMediumFont = FontFamily(Font(Res.font.lora_medium))

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        modifier = Modifier.widthIn(max = if (isLandscape) 600.dp else 400.dp),
        title = { Text(stringResource(Res.string.intention_dialog_edit_title), fontFamily = loraMediumFont, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        Surface(shape = RoundedCornerShape(16.dp), color = if (isSelected) Color(0xFF1976D2) else Color.Transparent, border = BorderStroke(1.dp, if (isSelected) Color.Transparent else Color.LightGray), modifier = Modifier.clickable { selectedCategory = cat }) {
                            Text(text = "${getCategoryEmoji(cat)} ${stringResource(getCategoryResId(cat))}", color = if (isSelected) Color.White else Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 1000) text = it },
                    placeholder = { Text(stringResource(Res.string.intention_wall_dialog_hint)) },
                    modifier = Modifier.fillMaxWidth().height(if (isLandscape) 100.dp else 200.dp),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 3,
                    maxLines = 10,
                    supportingText = { Text("${text.length}/1000") }
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { isAnonymous = !isAnonymous }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAnonymous, onCheckedChange = { isAnonymous = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1976D2)))
                    Text(stringResource(Res.string.intention_dialog_anonymous), fontSize = 14.sp, color = if (isAnonymous) Color(0xFF1976D2) else Color.Gray, fontWeight = if (isAnonymous) FontWeight.Bold else FontWeight.Normal)
                }
            }
        },
        confirmButton = { Button(onClick = { if (text.isNotBlank()) onConfirm(text, selectedCategory, isAnonymous) }, enabled = text.isNotBlank()) { Text(stringResource(Res.string.intention_dialog_btn_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.btn_cancel), color = Color.Gray) } }
    )
}

@Composable
fun ShareIntentionDialog(intention: Intention, onDismiss: () -> Unit) {
    val loraMediumFont = FontFamily(Font(Res.font.lora_medium))
    val backgrounds = listOf(
        Res.drawable.intention_1,
        Res.drawable.intention_2,
        Res.drawable.intention_3,
        Res.drawable.intention_4,
        Res.drawable.intention_5,
        Res.drawable.intention_6,
        Res.drawable.intention_7,
        Res.drawable.intention_8
    )

    // ZAMKNIĘCIE BLOKU REMEMBER BYŁO PROBLEMEM
    var selectedBg by remember { mutableStateOf(backgrounds[0]) }
    val currentImageBitmap = imageResource(selectedBg)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.intention_share_title),
                    fontFamily = loraMediumFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF1A252F),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(selectedBg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = intention.content,
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(backgrounds) { bg ->
                        Image(
                            painter = painterResource(bg),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (bg == selectedBg) 3.dp else 0.dp,
                                    color = if (bg == selectedBg) Color(0xFF1976D2) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ).clickable { selectedBg = bg }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    generateAndShareIntentionImage(
                        intention.content,
                        currentImageBitmap
                    ); onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text(
                    stringResource(Res.string.intention_share_btn_image),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        },
        dismissButton = null
    )
}

@Composable
fun RenewIntentionDialog(isLandscape: Boolean, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {

    var selectedDays by remember { mutableIntStateOf(7) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.widthIn(max = if (isLandscape) 500.dp else 400.dp),
        title = {
            Text(
                stringResource(Res.string.intention_wall_renew_title),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A252F)
            )
        },
        text = {
            Column {
                Text(
                    stringResource(Res.string.intention_wall_renew_desc),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedDays == 7) Color(0xFFE8F5E9) else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (selectedDays == 7) Color(0xFF4CAF50) else Color.LightGray
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { selectedDays = 7 }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (selectedDays == 7) Color(0xFF4CAF50) else Color.LightGray
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(Res.string.intention_wall_renew_7_days),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedDays == 30) Color(0xFFE8F5E9) else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (selectedDays == 30) Color(0xFF4CAF50) else Color.LightGray
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { selectedDays = 30 }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (selectedDays == 30) Color(0xFF4CAF50) else Color.LightGray
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(Res.string.intention_wall_renew_30_days),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDays) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    stringResource(Res.string.intention_wall_btn_renew),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(Res.string.btn_cancel),
                    color = Color.Gray
                )
            }
        }
    )
}

@Composable
fun ParishMiniChip(parish: PrayingParish, count: Int, allParishes: List<ParishEntity>, onClick: () -> Unit) {

    val photoUrl = allParishes.find { it.id == parish.id }?.photoUrl
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        if (count > 1) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp).background(Color(0xFF1976D2), CircleShape)
            ) {
                Text(
                    text = count.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            painter = painterResource(Res.drawable.ic_mappin_vector),
            contentDescription = null,
            tint = Color(0xFF1976D2),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Surface(
            modifier = Modifier.size(30.dp),
            shape = CircleShape,
            color = Color.LightGray.copy(alpha = 0.2f)
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(Res.drawable.image_church),
                error = painterResource(Res.drawable.image_church)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = parish.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black.copy(alpha = 0.8f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BigCandlePreviewDialog(candle: Candle, myDeviceId: String, onDismiss: () -> Unit, onExtinguish: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier.padding(16.dp).wrapContentHeight()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                // POPRAWKA: deviceId
                val isMine = candle.deviceId == myDeviceId

                // POPRAWKA: lighterParishName
                val lighterName = if (!candle.lighterParishName.isNullOrBlank()) {
                    "Parafianin z: ${candle.lighterParishName}"
                } else {
                    stringResource(Res.string.candle_lighter_anonymous)
                }

                Text(
                    text = stringResource(Res.string.candle_lit_by),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                Text(
                    text = lighterName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))

                // POPRAWKA: expiresAt
                val expireTime =
                    remember(candle.expiresAt) { parseServerTime(candle.expiresAt).toEpochMilliseconds() }
                val currentTime = Clock.System.now().toEpochMilliseconds()
                val totalTimeMillis = 8 * 60 * 60 * 1000L
                val timeRemaining = expireTime - currentTime
                val percentRemaining =
                    (timeRemaining.toFloat() / totalTimeMillis.toFloat()).coerceIn(0f, 1f)
                val isExpired = currentTime >= expireTime

                val resId = when {
                    isExpired -> Res.drawable.candle_stage_25
                    percentRemaining > 0.75f -> Res.drawable.candle_stage_100
                    percentRemaining > 0.50f -> Res.drawable.candle_stage_75
                    percentRemaining > 0.25f -> Res.drawable.candle_stage_50
                    else -> Res.drawable.candle_stage_25
                }

                val glowOffsetY = when {
                    percentRemaining > 0.75f -> (-88).dp
                    percentRemaining > 0.50f -> (-32).dp
                    percentRemaining > 0.25f -> 16.dp
                    else -> 72.dp
                }

                val infiniteTransition = rememberInfiniteTransition(label = "bigFire")
                val flickerAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f, targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 150),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                val glowScale by infiniteTransition.animateFloat(
                    initialValue = 0.8f, targetValue = 1.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 1000,
                            easing = FastOutSlowInEasing
                        ), repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(contentAlignment = Alignment.Center) {
                    if (!isExpired) {
                        Box(
                            modifier = Modifier.offset(y = glowOffsetY).size(120.dp)
                                .graphicsLayer {
                                    scaleX = glowScale; scaleY = glowScale; alpha = flickerAlpha
                                }.background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF9800).copy(alpha = 0.8f),
                                            Color(0xFFFFC107).copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    ), shape = CircleShape
                                )
                        )
                    }
                    Image(
                        painter = painterResource(resId),
                        contentDescription = null,
                        modifier = Modifier.size(200.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                val expireStr = if (timeRemaining > 0) {
                    val hours = timeRemaining / (1000 * 60 * 60)
                    val minutes = (timeRemaining / (1000 * 60)) % 60
                    if (hours > 0) {
                        stringResource(
                            Res.string.candle_burns_h_m,
                            hours.toInt(),
                            minutes.toInt()
                        )
                    } else {
                        stringResource(Res.string.candle_burns_m, minutes.toInt())
                    }
                } else {
                    stringResource(Res.string.candle_expired)
                }

                Text(
                    text = expireStr,
                    color = Color(0xFFFF9800),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                if (isMine && !isExpired) {
                    Button(
                        onClick = onExtinguish,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.7f).padding(bottom = 8.dp)
                    ) {
                        Text(
                            stringResource(Res.string.intention_wall_btn_extinguish),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(
                        stringResource(Res.string.btn_close),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun getCategoryResId(categoryName: String) = when (categoryName) {
    "O zdrowie" -> Res.string.cat_health
    "Za zmarłych" -> Res.string.cat_deceased
    "Dziękczynna" -> Res.string.cat_thanks
    else -> Res.string.cat_general
}

fun getCategoryEmoji(categoryName: String) = when (categoryName) {
    "O zdrowie" -> "❤️"
    "Za zmarłych" -> "🕊️"
    "Dziękczynna" -> "🙏"
    else -> "💬"
}

// Konwersja czasu serwera "yyyy-MM-dd HH:mm:ss" na Instant (KMP)
fun parseServerTime(timeStr: String): Instant {
    return try {
        Instant.parse(timeStr.replace(" ", "T") + "Z")
    } catch (e: Exception) {
        Clock.System.now()
    }
}

@Composable
fun getTimeInfo(createdAt: String?): String {
    if (createdAt.isNullOrEmpty()) return ""

    // POPRAWKA: Bezpieczne parsowanie, wyjęte przed użycie composables
    val createdTime = try {
        parseServerTime(createdAt).toEpochMilliseconds()
    } catch (e: Exception) {
        return ""
    }

    val currentTime = Clock.System.now().toEpochMilliseconds()

    val diffAdded = currentTime - createdTime
    val daysAdded = diffAdded / (1000 * 60 * 60 * 24)
    val hoursAdded = diffAdded / (1000 * 60 * 60)
    val minutesAdded = diffAdded / (1000 * 60)

    val addedStr = when {
        daysAdded > 0 -> stringResource(Res.string.time_days_ago, daysAdded.toInt())
        hoursAdded > 0 -> stringResource(Res.string.time_hours_ago, hoursAdded.toInt())
        minutesAdded > 0 -> stringResource(Res.string.time_minutes_ago, minutesAdded.toInt())
        else -> stringResource(Res.string.time_just_now)
    }

    val expireTime = createdTime + (7L * 24 * 60 * 60 * 1000)
    val diffExpire = expireTime - currentTime
    val daysExpire = diffExpire / (1000 * 60 * 60 * 24)
    val hoursExpire = diffExpire / (1000 * 60 * 60)

    val expireStr = when {
        daysExpire > 0 -> stringResource(Res.string.time_expires_in_days, daysExpire.toInt())
        hoursExpire > 0 -> stringResource(Res.string.time_expires_in_hours, hoursExpire.toInt())
        else -> stringResource(Res.string.time_expires_soon)
    }

    return stringResource(Res.string.time_added_and_expires, addedStr, expireStr)
}

fun getFlagEmoji(countryCode: String?): String {
    if (countryCode.isNullOrEmpty() || countryCode.length != 2) return "🇵🇱"

    val offset = 0x1F1E6 - 'A'.code
    fun charToRegionalIndicator(c: Char): String {
        val codePoint = c.uppercaseChar().code + offset
        val highSurrogate = ((codePoint - 0x10000) / 0x400 + 0xD800).toChar()
        val lowSurrogate = ((codePoint - 0x10000) % 0x400 + 0xDC00).toChar()
        return charArrayOf(highSurrogate, lowSurrogate).concatToString()
    }

    return charToRegionalIndicator(countryCode[0]) + charToRegionalIndicator(countryCode[1])
}