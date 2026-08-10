package com.example.mojaparafia.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mojaparafia.ui.components.CustomBottomNavBar
import com.example.mojaparafia.ui.components.UniversalGlassTopBar
import com.example.mojaparafia.util.ReminderScheduler
import com.example.mojaparafia.viewmodel.ParishListViewModel
import kotlinx.coroutines.launch
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.home_parish_go_to_map
import myparish.composeapp.generated.resources.home_parish_not_selected_desc
import myparish.composeapp.generated.resources.home_parish_not_selected_title
import myparish.composeapp.generated.resources.settings_cancel_all_success
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

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
    onProposeChangeClick: (String, String) -> Unit = { _, _ -> },
    onCallClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onWebsiteClick: (String) -> Unit = {},
    onCopyAccountClick: (String) -> Unit = {},
    onSubmitPriestRequest: (String) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onRestartAppRequired: (String?) -> Unit
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadReminders()
    }

    val parishes by viewModel.allParishes.collectAsState(emptyList())
    val homeParishId by viewModel.homeParishId.collectAsState(viewModel.homeParishId.value)

    var hasNewNews by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    
    var detailParishId by rememberSaveable { mutableStateOf<String?>(null) }

    var currentScreen by rememberSaveable {
        mutableStateOf(if (viewModel.homeParishId.value != null) "HOME_PARISH" else "MAP")
    }

    var initialRouteHandled by rememberSaveable {
        mutableStateOf(viewModel.homeParishId.value != null)
    }

    LaunchedEffect(homeParishId) {
        if (!initialRouteHandled && homeParishId != null) {
            currentScreen = "HOME_PARISH"
            initialRouteHandled = true
        }
    }

    LaunchedEffect(pushAction, pushParishId) {
        if (!pushParishId.isNullOrEmpty()) {
            detailParishId = pushParishId
            currentScreen = "DETAILS"
            onPushHandled()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                "MAP" -> {
                    MapScreen(
                        viewModel = viewModel,
                        reminderScheduler = reminderScheduler,
                        isLandscape = isLandscape,
                        parishes = parishes,
                        homeParishId = homeParishId,
                        userHasCrown = false,
                        onOpenDrawer = { },
                        onNavigateToAddParish = onNavigateToAddParish,
                        onNavigateToDetails = { id ->
                            detailParishId = id
                            currentScreen = "DETAILS"
                        },
                        showToast = showToast,
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        showFilterSheet = showFilterSheet,
                        onFilterDismiss = { showFilterSheet = false }
                    )
                }
                "HOME_PARISH" -> {
                    val homeParish = parishes.find { it.id == homeParishId }
                    if (homeParish != null) {
                        val homeEvents by viewModel.getParishEvents(homeParish.id).collectAsState(initial = emptyList())
                        ParishDetailScreen(
                            parish = homeParish,
                            isHomeParish = true,
                            isLandscape = isLandscape,
                            isParishActive = true,
                            events = homeEvents,
                            onProposeChangeClick = { onProposeChangeClick(homeParish.id, homeParish.name ?: "Nieznana") },
                            onToggleFavorite = { viewModel.toggleFavorite(homeParish) },
                            onToggleHomeParish = { viewModel.toggleHomeParish(homeParish.id) {} },
                            onCallClick = onCallClick,
                            onEmailClick = onEmailClick,
                            onWebsiteClick = onWebsiteClick,
                            onCopyAccountClick = onCopyAccountClick,
                            onSubmitPriestRequest = onSubmitPriestRequest
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(stringResource(Res.string.home_parish_not_selected_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A252F))
                                Text(stringResource(Res.string.home_parish_not_selected_desc), color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                                Button(
                                    onClick = { currentScreen = "MAP" },
                                    modifier = Modifier.padding(top = 24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                                ) {
                                    Text(stringResource(Res.string.home_parish_go_to_map), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                "DETAILS" -> {
                    val parish = parishes.find { it.id == detailParishId }
                    if (parish != null) {
                        val detailsEvents by viewModel.getParishEvents(parish.id).collectAsState(initial = emptyList())
                        ParishDetailScreen(
                            parish = parish,
                            isHomeParish = homeParishId == parish.id,
                            isLandscape = isLandscape,
                            isParishActive = true,
                            events = detailsEvents,
                            onProposeChangeClick = { onProposeChangeClick(parish.id, parish.name ?: "Nieznana") },
                            onToggleFavorite = { viewModel.toggleFavorite(parish) },
                            onToggleHomeParish = { viewModel.toggleHomeParish(parish.id) {} },
                            onCallClick = onCallClick,
                            onEmailClick = onEmailClick,
                            onWebsiteClick = onWebsiteClick,
                            onCopyAccountClick = onCopyAccountClick,
                            onSubmitPriestRequest = onSubmitPriestRequest
                        )
                    } else {
                        currentScreen = "MAP"
                    }
                }
                "FUNCTIONS" -> {
                    FunctionScreen(
                        onAddParishClick = { onNavigateToAddParish(0.0, 0.0) },
                        onNewsClick = onOpenNews,
                        onHelpClick = onOpenHelp,
                        onSupportClick = { showSupportDialog = true },
                        onSettingsClick = { currentScreen = "SETTINGS" },
                        hasNewNews = hasNewNews
                    )
                }
                "SETTINGS" -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBackClick = { currentScreen = "FUNCTIONS" },
                        showToast = showToast,
                        onOpenReminders = onOpenReminders,
                        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                        onOpenSystemSettings = onOpenSystemSettings,
                        onClearAllReminders = {
                            viewModel.remindersList.value.forEach { reminder ->
                                viewModel.removeReminder(reminderScheduler, reminder.notificationId)
                            }
                            scope.launch { showToast(getString(Res.string.settings_cancel_all_success)) }
                        },
                        onRestartAppRequired = onRestartAppRequired
                    )
                }
            }
        }

        if (currentScreen in listOf("MAP", "HOME_PARISH", "FUNCTIONS", "DETAILS")) {
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                UniversalGlassTopBar(
                    currentScreen = currentScreen,
                    isLandscape = isLandscape,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSearchToggle = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    },
                    onFilterClick = { showFilterSheet = true },
                    onSearchSubmit = { query -> viewModel.logSearchEvent(query) }
                )
            }
        }

        if (currentScreen in listOf("MAP", "HOME_PARISH", "FUNCTIONS", "DETAILS")) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                CustomBottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it }
                )
            }
        }
    }
}