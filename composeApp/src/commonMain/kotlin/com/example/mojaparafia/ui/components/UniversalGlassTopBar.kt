package com.example.mojaparafia.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UniversalGlassTopBar(
    currentScreen: String,
    isLandscape: Boolean,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onFilterClick: () -> Unit,
    onSearchSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val barHeight = if (isLandscape) 44.dp else 64.dp
    val titleSize = if (isLandscape) 18.sp else 22.sp
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchHintStr = stringResource(Res.string.main_search_hint)
    val cdBackStr = stringResource(Res.string.main_cd_back)
    val cdClearStr = stringResource(Res.string.main_cd_clear)
    val cdSearchStr = stringResource(Res.string.main_cd_search)
    val cdFiltersStr = stringResource(Res.string.main_cd_filters)
    val appNameStr = stringResource(Res.string.app_name)

    // Logika nazwy ekranu
    val displayTitle = when (currentScreen) {
        "PLACEHOLDER" -> "Wszyscy Święci"
        else -> appNameStr
    }

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
                if (searchMode && currentScreen == "MAP") { // Szukanie działa tylko na mapie (na razie)
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
                        modifier = Modifier.fillMaxWidth().height(barHeight).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title always left-aligned now (zamiast szuflady menu)
                        Text(
                            text = displayTitle,
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(Res.font.lora_medium)),
                            color = Color(0xFF1A252F)
                        )

                        // Opcje z prawej strony pojawiają się tylko w trybie mapy
                        if (currentScreen == "MAP") {
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
}