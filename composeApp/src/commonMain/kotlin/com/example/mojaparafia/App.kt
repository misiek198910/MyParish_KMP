package com.example.mojaparafia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import com.example.mojaparafia.util.ReminderScheduler
import com.example.mojaparafia.ui.screens.MainScreen
import com.example.mojaparafia.ui.screens.WelcomeScreen
import com.example.mojaparafia.ui.screens.ParishDetailScreen
import com.example.mojaparafia.ui.screens.RemindersScreen
import com.example.mojaparafia.ui.screens.AddParishScreen
import com.example.mojaparafia.ui.screens.HelpScreen
import com.example.mojaparafia.ui.screens.NewsScreen
import com.example.mojaparafia.ui.screens.ProposeChangeScreen
import com.example.mojaparafia.viewmodel.ParishListViewModel
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade

sealed class Screen {
    object Main : Screen()
    data class ParishDetails(val parishId: String) : Screen()
    object Reminders : Screen()
    data class AddParish(val lat: Double, val lng: Double) : Screen()
    object Help : Screen()
    object News : Screen()
    data class ProposeChange(val parishId: String, val parishName: String) : Screen()
}

@Composable
fun App(
    viewModel: ParishListViewModel,
    pushAction: String? = null,
    pushParishId: String? = null,
    onPushHandled: () -> Unit = {},
    reminderScheduler: ReminderScheduler,
    onRequestPlatformPermissions: () -> Unit,
    showToast: (String) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onRestartAppRequired: (String?) -> Unit,
    isIos: Boolean = false,
    onSubmitNewParish: (Map<String, String>) -> Unit = {}

) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }

    MaterialTheme {

        val isFirstRun by viewModel.isFirstRun.collectAsState()
        val isInitialSyncing by viewModel.isInitialSyncing.collectAsState()

        var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
        val uriHandler = LocalUriHandler.current
        val clipboardManager = LocalClipboardManager.current

        if (isFirstRun) {

            var isProcessingPermissions by remember { mutableStateOf(false) }

            WelcomeScreen(
                isProcessing = isProcessingPermissions,
                isSyncingData = isInitialSyncing,
                isIos = isIos,
                onNextStepClick = {
                    isProcessingPermissions = true
                    onRequestPlatformPermissions()
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                MainScreen(
                    viewModel = viewModel,
                    pushAction = pushAction,
                    pushParishId = pushParishId,
                    onPushHandled = onPushHandled,
                    reminderScheduler = reminderScheduler,
                    showToast = showToast,
                    isLandscape = false,
                    onNavigateToAddParish = { lat, lng -> currentScreen = Screen.AddParish(lat, lng) },
                    onNavigateToDetails = { id -> currentScreen = Screen.ParishDetails(id) },
                    onOpenSettings = {},
                    onOpenNews = { currentScreen = Screen.News },
                    onOpenHelp = { currentScreen = Screen.Help },
                    onBuyCoffee = { uriHandler.openUri("https://buycoffee.to/mivs/MojaParafia") },
                    onOpenReminders = { currentScreen = Screen.Reminders },
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    onOpenSystemSettings = onOpenSystemSettings,
                    onRestartAppRequired = onRestartAppRequired
                )

                if (currentScreen is Screen.ParishDetails) {
                    val detailsScreen = currentScreen as Screen.ParishDetails
                    val parish by viewModel.getParishById(detailsScreen.parishId).collectAsState(initial = null)

                    val homeParishId by viewModel.homeParishId.collectAsState(initial = null)

                    if (parish != null) {
                        ParishDetailScreen(
                            parish = parish!!,
                            isHomeParish = homeParishId == parish!!.id,
                            isLandscape = false,
                            isParishActive = true,
                            onProposeChangeClick = { currentScreen = Screen.ProposeChange(parish!!.id, parish!!.name ?: "Nieznana") },
                            onToggleFavorite = { viewModel.toggleFavorite(parish!!) },
                            onToggleHomeParish = { viewModel.toggleHomeParish(parish!!.id) { } },
                            onCallClick = { uriHandler.openUri("tel:$it") },
                            onEmailClick = { uriHandler.openUri("mailto:$it") },
                            onWebsiteClick = {
                                val safeUrl = if (!it.startsWith("http")) "https://$it" else it
                                uriHandler.openUri(safeUrl)
                            },
                            onCopyAccountClick = { clipboardManager.setText(AnnotatedString(it)) },
                            onSubmitPriestRequest = { viewModel.submitPriestRequest(parish!!.id, it) }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF1976D2))
                        }
                    }
                }

                if (currentScreen is Screen.Reminders) {

                    RemindersScreen(
                        viewModel = viewModel,
                        reminderScheduler = reminderScheduler,
                        onBackClick = { currentScreen = Screen.Main }
                    )
                }

                if (currentScreen is Screen.AddParish) {
                    val addParishScreen = currentScreen as Screen.AddParish

                    AddParishScreen(
                        initialLat = if (addParishScreen.lat != 0.0) addParishScreen.lat.toString() else "",
                        initialLng = if (addParishScreen.lng != 0.0) addParishScreen.lng.toString() else "",
                        onBackClick = { currentScreen = Screen.Main },
                        viewModel = viewModel,
                        onSubmitClick = { data ->
                            viewModel.submitParishProposal(data)

                            showToast("Dziękujemy! Propozycja została wysłana do weryfikacji.")
                            currentScreen = Screen.Main
                        },
                        showToast = showToast
                    )
                }

                if (currentScreen is Screen.Help) {
                    HelpScreen(
                        onBackClick = { currentScreen = Screen.Main },
                        showToast = showToast
                    )
                }

                if (currentScreen is Screen.News) {

                    NewsScreen(
                        viewModel = viewModel,
                        onBackClick = { currentScreen = Screen.Main }
                    )
                }

                if (currentScreen is Screen.ProposeChange) {
                    val changeScreen = currentScreen as Screen.ProposeChange

                    ProposeChangeScreen(
                        parishId = changeScreen.parishId,
                        parishName = changeScreen.parishName,
                        viewModel = viewModel,
                        onBackClick = { currentScreen = Screen.ParishDetails(changeScreen.parishId) },
                        onGetLocationClick = {
                            showToast("Aby zaktualizować dokładną lokalizację, zamknij to okno, przytrzymaj palec na mapie i wciśnij 'Dodaj'.")
                        },
                        showToast = showToast
                    )
                }
            }
        }
    }
}