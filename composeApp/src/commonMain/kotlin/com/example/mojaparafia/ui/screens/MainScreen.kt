package com.example.mojaparafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mojaparafia.util.ReminderScheduler
import com.example.mojaparafia.ui.components.CustomBottomNavBar
import com.example.mojaparafia.ui.components.UniversalGlassTopBar
import com.example.mojaparafia.viewmodel.ParishListViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import androidx.compose.runtime.saveable.rememberSaveable

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
    onProposeChangeClick: () -> Unit = {},
    onCallClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onWebsiteClick: (String) -> Unit = {},
    onCopyAccountClick: (String) -> Unit = {},
    onSubmitPriestRequest: (String) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenHelp: () -> Unit,
    onBuyCoffee: () -> Unit,
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

    // Stany dla mapy i paska nawigacji (wyciągnięte wyżej, żeby pasek mógł reagować)
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }

    var currentScreen by rememberSaveable {
        mutableStateOf(if (viewModel.homeParishId.value != null) "HOME_PARISH" else "MAP")
    }

    LaunchedEffect(pushAction, pushParishId) {
        if (!pushParishId.isNullOrEmpty()) {
            onNavigateToDetails(pushParishId)
            onPushHandled()
        }
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text(stringResource(Res.string.support_project_title), fontWeight = FontWeight.Bold, color = Color(0xFF1A252F)) },
            text = { Text(stringResource(Res.string.support_project_desc)) },
            confirmButton = {
                Button(
                    onClick = { showSupportDialog = false; onBuyCoffee() },
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

    // GŁÓWNY KONTENER EKRANU
    Box(modifier = Modifier.fillMaxSize()) {

        // ZARZĄDCA WIDOKÓW
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
                        onNavigateToDetails = onNavigateToDetails,
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
                        ParishDetailScreen(
                            parish = homeParish,
                            isHomeParish = true,
                            isLandscape = isLandscape,
                            isParishActive = true,
                            onProposeChangeClick = onProposeChangeClick,
                            onToggleFavorite = { viewModel.toggleFavorite(homeParish) },
                            onToggleHomeParish = { viewModel.toggleHomeParish(homeParish.id) {} },
                            onCallClick = onCallClick,
                            onEmailClick = onEmailClick,
                            onWebsiteClick = onWebsiteClick,
                            onCopyAccountClick = onCopyAccountClick,
                            onSubmitPriestRequest = onSubmitPriestRequest
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
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
                            scope.launch {
                                showToast(getString(Res.string.settings_cancel_all_success))
                            }
                        },
                        onRestartAppRequired = onRestartAppRequired
                    )
                }
            }
        }

        // UNIWERSALNY PASEK GÓRNY (zawsze widoczny nad treścia dla odpowiednich ekranów)
        if (currentScreen in listOf("MAP", "HOME_PARISH", "FUNCTIONS")) {
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

        // DOLNY PASEK NAWIGACJI
        if (currentScreen in listOf("MAP", "HOME_PARISH", "FUNCTIONS")) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                CustomBottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it }
                )
            }
        }
    }
}