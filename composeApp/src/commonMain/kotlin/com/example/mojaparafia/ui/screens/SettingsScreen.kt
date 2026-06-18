package com.example.mojaparafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mojaparafia.Screen
import com.example.mojaparafia.ui.components.AdBanner
import com.example.mojaparafia.viewmodel.ParishListViewModel
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString

// Stałe dla preferencji
const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
const val KEY_APP_THEME = "app_theme" // 0 = System, 1 = Light, 2 = Dark
const val KEY_APP_LANGUAGE = "app_language"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ParishListViewModel,
    onBackClick: () -> Unit,
    showToast: (String) -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onClearAllReminders: () -> Unit,
    onRestartAppRequired: (String?) -> Unit,
    isPremium: Boolean
) {
    val settings = remember { Settings() }
    val scope = rememberCoroutineScope()

    val isSyncing by viewModel.isSyncing.collectAsState(false)
    val userPoints by viewModel.userPoints.collectAsState(0)
    val effectivePremium = isPremium || userPoints >= 50

    var isNotificationsEnabled by remember {
        mutableStateOf(settings.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
    }

    // Motyw: 0 = System, 1 = Jasny, 2 = Ciemny
    var currentTheme by remember {
        mutableStateOf(settings.getInt(KEY_APP_THEME, 0))
    }

    var currentLanguage by remember {
        mutableStateOf(settings.getString(KEY_APP_LANGUAGE, "pl"))
    }

    var showCancelDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showRestartPrompt by remember { mutableStateOf(false) }

    var pendingLanguageChange by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White.copy(alpha = 0.85f),
                shadowElevation = 2.dp
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.ustawienia),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A252F)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć", tint = Color(0xFF1A252F))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        bottomBar = {
            if (!effectivePremium) {
                Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.65f)).windowInsetsPadding(WindowInsets.navigationBars)) {
                    AdBanner(modifier = Modifier.fillMaxWidth(), isPremium = effectivePremium)
                }
            }
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // SEKCJA 1: APLIKACJA
            SettingsSection(title = stringResource(Res.string.settings_header_app)) {
                SettingsRow(
                    icon = Icons.Filled.Refresh,
                    title = if (isSyncing) stringResource(Res.string.settings_refresh_syncing) else stringResource(Res.string.settings_text1),
                    summary = stringResource(Res.string.settings_text2),
                    titleColor = if (!isSyncing) Color(0xFF4CAF50) else Color.Gray,
                    isEnabled = !isSyncing,
                    onClick = {
                        viewModel.syncParishes()
                        scope.launch {
                            showToast(getString(Res.string.settings_refresh_start))
                        }
                    }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Filled.Language,
                    title = stringResource(Res.string.settings_language_title),
                    summary = stringResource(Res.string.settings_language_summary),
                    onClick = { showLanguageDialog = true }
                )
                SettingsDivider()

                SettingsRow(
                    icon = Icons.Filled.Settings,
                    title = stringResource(Res.string.settings_theme_title),
                    summary = when (currentTheme) {
                        1 -> stringResource(Res.string.settings_theme_light)
                        2 -> stringResource(Res.string.settings_theme_dark)
                        else -> stringResource(Res.string.settings_theme_system)
                    },
                    onClick = { showThemeDialog = true }
                )
                SettingsDivider()

                SettingsRow(
                    icon = Icons.Filled.Star,
                    title = stringResource(Res.string.settings_text3),
                    summary = stringResource(Res.string.settings_text4),
                    onClick = onOpenSubscriptions
                )
            }

            // SEKCJA 2: POWIADOMIENIA
            SettingsSection(title = stringResource(Res.string.settings_header_notifications)) {
                SettingsSwitchRow(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(Res.string.settings_notif_enable_title),
                    summary = stringResource(Res.string.settings_notif_enable_summary),
                    isChecked = isNotificationsEnabled,
                    onCheckedChange = { checked ->
                        isNotificationsEnabled = checked
                        settings.putBoolean(KEY_NOTIFICATIONS_ENABLED, checked)

                        scope.launch {
                            val msgRes = if (checked) Res.string.notifications_enabled else Res.string.notifications_disabled
                            showToast(getString(msgRes))
                        }
                    }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(Res.string.settings_notif_manage_title),
                    summary = stringResource(Res.string.settings_notif_manage_summary),
                    onClick = onOpenReminders
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Filled.Delete,
                    title = stringResource(Res.string.settings_notif_cancel_all_title),
                    summary = stringResource(Res.string.settings_notif_cancel_all_summary),
                    titleColor = Color(0xFFE74C3C),
                    onClick = { showCancelDialog = true }
                )
            }

            // SEKCJA 3: BEZPIECZEŃSTWO
            SettingsSection(title = stringResource(Res.string.settings_header_security)) {
                SettingsRow(
                    icon = Icons.Filled.Security,
                    title = stringResource(Res.string.privacy_policy_title),
                    summary = stringResource(Res.string.privacy_policy_description),
                    onClick = onOpenPrivacyPolicy
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Filled.Settings,
                    title = stringResource(Res.string.settings_permission),
                    summary = stringResource(Res.string.open_app_settings),
                    onClick = onOpenSystemSettings
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // DIALOG: Zmiana Motywu
    if (showThemeDialog) {
        val themeOptions = listOf(
            0 to stringResource(Res.string.settings_theme_system),
            1 to stringResource(Res.string.settings_theme_light),
            2 to stringResource(Res.string.settings_theme_dark)
        )
        var tempSelectedTheme by remember { mutableStateOf(currentTheme) }

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(Res.string.settings_theme_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.selectableGroup()) {
                    themeOptions.forEach { (mode, name) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (mode == tempSelectedTheme),
                                    onClick = { tempSelectedTheme = mode },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (mode == tempSelectedTheme),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1976D2))
                            )
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 16.dp),
                                color = Color(0xFF1A252F)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if(currentTheme != tempSelectedTheme) {
                        currentTheme = tempSelectedTheme
                        settings.putInt(KEY_APP_THEME, currentTheme)
                        showThemeDialog = false
                        onRestartAppRequired(null)
                    } else {
                        showThemeDialog = false
                    }
                }) {
                    Text(stringResource(Res.string.settings_theme_btn_choose), color = Color(0xFF1976D2))
                }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(Res.string.btn_cancel), color = Color.Gray)
                }
            },
            containerColor = Color.White
        )
    }

    if (showLanguageDialog) {
        val options = listOf(
            "pl" to "Polski",
            "en" to "English",
            "de" to "Deutsch",
            "fr" to "Français",
            "no" to "Norsk (Bokmål)",
            "es" to "Español",
            "it" to "Italiano",
            "uk" to "Українська"
        )
        var tempSelectedLang by remember { mutableStateOf(currentLanguage) }

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(Res.string.settings_language_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                    options.forEach { (tag, name) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (tag == tempSelectedLang),
                                    onClick = { tempSelectedLang = tag },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (tag == tempSelectedLang),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1976D2))
                            )
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 16.dp),
                                color = Color(0xFF1A252F)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if(currentLanguage != tempSelectedLang) {
                        currentLanguage = tempSelectedLang
                        settings.putString(KEY_APP_LANGUAGE, currentLanguage)
                        showLanguageDialog = false
                        pendingLanguageChange = currentLanguage
                        showRestartPrompt = true
                    } else {
                        showLanguageDialog = false
                    }
                }) {
                    Text(stringResource(Res.string.settings_theme_btn_choose), color = Color(0xFF1976D2))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(Res.string.btn_cancel), color = Color.Gray)
                }
            },
            containerColor = Color.White
        )
    }

    if (showRestartPrompt) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(Res.string.settings_restart_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.settings_restart_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartPrompt = false
                        onRestartAppRequired(pendingLanguageChange)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text(stringResource(Res.string.settings_restart_btn_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartPrompt = false }) {
                    Text(stringResource(Res.string.settings_restart_btn_later), color = Color.Gray)
                }
            },
            containerColor = Color.White
        )
    }
    
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(Res.string.settings_cancel_all_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.settings_cancel_all_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllReminders()
                        scope.launch { showToast(getString(Res.string.settings_cancel_all_success)) }
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
                ) {
                    Text(stringResource(Res.string.settings_cancel_all_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(Res.string.btn_cancel), color = Color.Gray)
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(Res.font.lora_medium)),
            color = Color(0xFF1976D2),
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    summary: String,
    titleColor: Color = Color(0xFF1A252F),
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick)
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF546E7A),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = summary, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    summary: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF546E7A),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A252F))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = summary, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1976D2))
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = Color.LightGray.copy(alpha = 0.5f)
    )
}