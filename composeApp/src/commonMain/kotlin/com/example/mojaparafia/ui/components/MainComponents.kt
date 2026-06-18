package com.example.mojaparafia.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*
@Composable
fun DrawerMenuItem(painter: Painter, text: String, showBadge: Boolean = false, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = {
            BadgedBox(
                badge = {
                    if (showBadge) {
                        Badge(containerColor = Color.Red, modifier = Modifier.size(8.dp))
                    }
                }
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = Color(0xFF546E7A),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        label = { Text(text, fontSize = 16.sp, color = Color(0xFF333333)) },
        selected = false,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GlassTopBar(isLandscape: Boolean, isSearchActive: Boolean, searchQuery: String, onSearchQueryChange: (String) -> Unit, onSearchToggle: () -> Unit, onMenuClick: () -> Unit, onFilterClick: () -> Unit, onSearchSubmit: (String) -> Unit, modifier: Modifier = Modifier) {
    val barHeight = if (isLandscape) 44.dp else 64.dp
    val titleSize = if (isLandscape) 18.sp else 22.sp
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchHintStr = stringResource(Res.string.main_search_hint)
    val cdBackStr = stringResource(Res.string.main_cd_back)
    val cdClearStr = stringResource(Res.string.main_cd_clear)
    val cdMenuStr = stringResource(Res.string.main_cd_menu)
    val cdSearchStr = stringResource(Res.string.main_cd_search)
    val cdFiltersStr = stringResource(Res.string.main_cd_filters)
    val appNameStr = stringResource(Res.string.app_name)

    Surface(
        color = Color.White.copy(alpha = 0.60f),
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 2.dp
    ) {
        Column {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "TopBarAnimation"
            ) { searchMode ->
                if (searchMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(barHeight).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onSearchToggle) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = cdBackStr, tint = Color(0xFF1A252F))
                        }
                        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            if (searchQuery.isEmpty()) {
                                Text(searchHintStr, color = Color.DarkGray, fontSize = 16.sp)
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                                cursorBrush = SolidColor(Color(0xFF1976D2)),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        onSearchSubmit(searchQuery)
                                        keyboardController?.hide()
                                    }
                                )
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = cdClearStr, tint = Color.DarkGray)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(barHeight).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Filled.Menu, contentDescription = cdMenuStr, tint = Color(0xFF1A252F))
                        }
                        Text(
                            text = appNameStr,
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(Res.font.lora_medium)),
                            color = Color(0xFF1A252F)
                        )
                        Row {
                            IconButton(onClick = onSearchToggle) {
                                Icon(Icons.Filled.Search, contentDescription = cdSearchStr, tint = Color(0xFF1A252F))
                            }
                            IconButton(onClick = onFilterClick) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = cdFiltersStr, tint = Color(0xFF1A252F))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FabPanelOverlay(
    isLandscape: Boolean,
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    onFavoritesClick: () -> Unit,
    onIntentionWallClick: () -> Unit,
    onLocationClick: () -> Unit,
    onNearestClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val hiddenOffset = if (isLandscape) 56.dp else 76.dp
    val slideOffset by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else hiddenOffset,
        animationSpec = tween(300),
        label = "fabSlide"
    )

    val fabSize = if (isLandscape) 40.dp else 48.dp
    val handleHeight = if (isLandscape) 70.dp else 80.dp
    val handleWidth = if (isLandscape) 24.dp else 28.dp

    val cdToggle = stringResource(Res.string.main_cd_toggle_fabs)
    val cdIntention = stringResource(Res.string.main_drawer_intention_wall)
    val cdFav = stringResource(Res.string.main_cd_favorite)
    val cdLoc = stringResource(Res.string.main_cd_location)
    val cdNear = stringResource(Res.string.main_cd_nearest)

    Row(
        modifier = modifier
            .offset { IntOffset(slideOffset.roundToPx(), 0) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            color = Color.White.copy(alpha = 0.60f),
            shadowElevation = 2.dp,
            border = BorderStroke(0.5.dp, Color.White),
            modifier = Modifier
                .height(handleHeight)
                .width(handleWidth)
                .clickable { isExpanded = !isExpanded }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isExpanded) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = cdToggle,
                    tint = Color(0xFF1A252F)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
            color = Color.White.copy(alpha = 0.60f),
            shadowElevation = 2.dp,
            border = BorderStroke(0.5.dp, Color.White),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(
                    onClick = onIntentionWallClick,
                    containerColor = Color.White.copy(alpha = 0.85f),
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 2.dp),
                    modifier = Modifier.size(fabSize)
                ) {
                    Icon(painterResource(Res.drawable.ic_pray_wall), contentDescription = cdIntention, tint = Color.Unspecified)
                }

                FloatingActionButton(
                    onClick = onFavoritesClick,
                    containerColor = Color.White.copy(alpha = 0.85f),
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 2.dp),
                    modifier = Modifier.size(fabSize)
                ) {
                    Icon(painterResource(Res.drawable.ic_favorite), contentDescription = cdFav, tint = Color.Unspecified)
                }

                FloatingActionButton(
                    onClick = onLocationClick,
                    containerColor = Color.White.copy(alpha = 0.85f),
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 2.dp),
                    modifier = Modifier.size(fabSize)
                ) {
                    Icon(painterResource(Res.drawable.ic_localization_1), contentDescription = cdLoc, tint = Color.Unspecified)
                }

                FloatingActionButton(
                    onClick = onNearestClick,
                    containerColor = Color.White.copy(alpha = 0.85f),
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 2.dp),
                    modifier = Modifier.size(fabSize)
                ) {
                    Icon(painterResource(Res.drawable.ic_localization_2), contentDescription = cdNear, tint = Color.Unspecified)
                }
            }
        }
    }
}

@Composable
fun MapLoadingOverlay(isMapLoaded: Boolean, onRetry: () -> Unit) {
    var showRetryButton by remember { mutableStateOf(false) }

    val loadingStr = stringResource(Res.string.main_map_loading)
    val loadingFailedStr = stringResource(Res.string.main_map_loading_failed)
    val tryAgainStr = stringResource(Res.string.main_btn_try_again)

    LaunchedEffect(isMapLoaded) {
        if (!isMapLoaded) {
            delay(12000)
            showRetryButton = true
        }
    }

    AnimatedVisibility(
        visible = !isMapLoaded,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(500))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE3E9F2))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!showRetryButton) {
                    CircularProgressIndicator(color = Color(0xFF1976D2), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = loadingStr,
                        color = Color(0xFF546E7A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = loadingFailedStr,
                        color = Color(0xFF546E7A),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onRetry) { // Używamy lambdy onRetry zamiast Androidowego Intent
                        Text(tryAgainStr, color = Color(0xFF1976D2))
                    }
                }
            }
        }
    }
}