package com.example.mojaparafia.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*

@Composable
fun FabPanelOverlay(
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    onFavoritesClick: () -> Unit,
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
                    TextButton(onClick = onRetry) {
                        Text(tryAgainStr, color = Color(0xFF1976D2))
                    }
                }
            }
        }
    }
}