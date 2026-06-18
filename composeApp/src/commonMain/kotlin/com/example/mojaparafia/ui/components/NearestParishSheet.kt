package com.example.mojaparafia.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mojaparafia.db.ParishEntity
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.bs_distance_label
import myparish.composeapp.generated.resources.bs_next_mass_info
import myparish.composeapp.generated.resources.bs_no_upcoming_masses
import myparish.composeapp.generated.resources.bs_tomorrow_suffix
import myparish.composeapp.generated.resources.btn_cancel
import myparish.composeapp.generated.resources.btn_navigate
import myparish.composeapp.generated.resources.btn_remind
import myparish.composeapp.generated.resources.duration_hours_minutes
import myparish.composeapp.generated.resources.duration_hours_only
import myparish.composeapp.generated.resources.duration_just_now
import myparish.composeapp.generated.resources.duration_minutes_only
import myparish.composeapp.generated.resources.transport_bike
import myparish.composeapp.generated.resources.transport_bus
import myparish.composeapp.generated.resources.transport_car
import myparish.composeapp.generated.resources.transport_title
import myparish.composeapp.generated.resources.transport_walk
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

data class UpcomingMassInfo(
    val massTime: LocalTime,
    val isTomorrow: Boolean,
    val hoursLeft: Long,
    val minutesLeft: Long,
    val fullDateTime: LocalDateTime
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearestParishSheet(
    parishes: List<ParishEntity>,
    userLat: Double,
    userLng: Double,
    onDismiss: () -> Unit,
    onParishFocusChange: (Double, Double) -> Unit,
    onAddReminderClick: (ParishEntity, String, LocalDateTime, Int) -> Unit // Obsługa przypomnienia przekazana wyżej
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentIndex by remember { mutableStateOf(0) }
    var showTransportDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val currentParish = parishes[currentIndex]

    val distanceMeters = remember(currentIndex) {
        calculateDistance(userLat, userLng, currentParish.latitude, currentParish.longitude)
    }
    val distanceKmValue = remember(currentIndex) {
        (distanceMeters / 1000.0)
    }

    val distanceKmString = remember(distanceKmValue) {
        val rounded = (distanceKmValue * 10).roundToInt() / 10.0
        rounded.toString().replace('.', ',') // Opcjonalnie zamień kropkę na przecinek
    }

    // Logika obliczania czasu do mszy przy użyciu kotlinx-datetime
    val upcomingMassInfo = remember(currentIndex) { calculateUpcomingMass(currentParish) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }

    var selectedOption by remember { mutableStateOf(30) }
    val options = listOf(5, 10, 15, 30, 60)

    // Autozamykanie panelu po 10 sekundach bezczynności
    LaunchedEffect(currentIndex, showTransportDialog) {
        if (!showTransportDialog) {
            delay(10000)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (currentIndex > 0) {
                            currentIndex--
                            onParishFocusChange(parishes[currentIndex].latitude, parishes[currentIndex].longitude)
                        }
                    },
                    enabled = currentIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = if (currentIndex > 0) Color(0xFF1976D2) else Color.Transparent
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentParish.name ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A252F),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "$distanceKmString ${stringResource(Res.string.bs_distance_label)}",
                        fontSize = 13.sp,
                        color = Color(0xFF546E7A)
                    )
                }

                IconButton(
                    onClick = {
                        if (currentIndex < parishes.size - 1) {
                            currentIndex++
                            onParishFocusChange(parishes[currentIndex].latitude, parishes[currentIndex].longitude)
                        }
                    },
                    enabled = currentIndex < parishes.size - 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (currentIndex < parishes.size - 1) Color(0xFF1976D2) else Color.Transparent
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tekst informujący o najbliższej mszy świętej
            val massInfoText = if (upcomingMassInfo != null) {
                val timeStr = upcomingMassInfo.massTime.toString().take(5) // Format HH:mm
                val daySuffix = if (upcomingMassInfo.isTomorrow) stringResource(Res.string.bs_tomorrow_suffix) else ""
                val remainingStr = formatRemainingTime(upcomingMassInfo.hoursLeft, upcomingMassInfo.minutesLeft)

                stringResource(Res.string.bs_next_mass_info, timeStr, daySuffix, remainingStr)
            } else {
                stringResource(Res.string.bs_no_upcoming_masses)
            }

            Text(
                text = massInfoText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A252F),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (upcomingMassInfo != null) {
                    OutlinedButton(
                        onClick = { showReminderTimeDialog = true }, // Otwiera dialog wyboru czasu
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2))
                    ) {
                        Text(text = stringResource(Res.string.btn_remind), color = Color(0xFF1976D2), maxLines = 1)
                    }
                }

                Button(
                    onClick = { showTransportDialog = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text(
                        text = stringResource(Res.string.btn_navigate),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showTransportDialog) {
        AlertDialog(
            onDismissRequest = { showTransportDialog = false },
            title = { Text(stringResource(Res.string.transport_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransportOption(stringResource(Res.string.transport_car), "driving") { mode ->
                        openMapNavigation(uriHandler, currentParish.latitude, currentParish.longitude, mode)
                        showTransportDialog = false
                        onDismiss()
                    }
                    TransportOption(stringResource(Res.string.transport_walk), "walking") { mode ->
                        openMapNavigation(uriHandler, currentParish.latitude, currentParish.longitude, mode)
                        showTransportDialog = false
                        onDismiss()
                    }
                    TransportOption(stringResource(Res.string.transport_bike), "bicycling") { mode ->
                        openMapNavigation(uriHandler, currentParish.latitude, currentParish.longitude, mode)
                        showTransportDialog = false
                        onDismiss()
                    }
                    TransportOption(stringResource(Res.string.transport_bus), "transit") { mode ->
                        openMapNavigation(uriHandler, currentParish.latitude, currentParish.longitude, mode)
                        showTransportDialog = false
                        onDismiss()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTransportDialog = false }) { Text(stringResource(Res.string.btn_cancel), color = Color.Gray) }
            },
            containerColor = Color.White
        )
    }

    if (showReminderTimeDialog) {
        AlertDialog(
            onDismissRequest = { showReminderTimeDialog = false },
            title = {
                Text(
                    text = "Kiedy przypomnieć?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF1A252F)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = minutes }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (minutes == selectedOption),
                                onClick = { selectedOption = minutes },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF1976D2), // Twój niebieski
                                    unselectedColor = Color.Gray
                                )
                            )
                            Text(
                                text = "$minutes minut przed mszą",
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formattedTime = upcomingMassInfo!!.massTime.toString().take(5)
                        // Przekazujemy wybrany czas do callbacka
                        onAddReminderClick(currentParish, formattedTime, upcomingMassInfo.fullDateTime, selectedOption)
                        showReminderTimeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("USTAW", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimeDialog = false }) {
                    Text("ANULUJ", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
private fun TransportOption(title: String, travelMode: String, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(travelMode) }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 16.sp, color = Color.Black)
    }
}

@Composable
private fun formatRemainingTime(hours: Long, minutes: Long): String {
    return when {
        hours > 0 && minutes > 0 -> stringResource(Res.string.duration_hours_minutes, hours.toString(), minutes.toString())
        hours > 0 -> stringResource(Res.string.duration_hours_only, hours.toString())
        minutes > 0 -> stringResource(Res.string.duration_minutes_only, minutes.toString())
        else -> stringResource(Res.string.duration_just_now)
    }
}

private fun openMapNavigation(uriHandler: androidx.compose.ui.platform.UriHandler, destLat: Double, destLng: Double, travelMode: String) {
    // Uniwersalny link, który na Androidzie i iOS otwiera natywną aplikację map lub przeglądarkę
    val url = "https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng&travelmode=$travelMode"
    uriHandler.openUri(url)
}

private fun calculateUpcomingMass(parish: ParishEntity): UpcomingMassInfo? {
    val nowInstant = Clock.System.now()
    val timeZone = TimeZone.currentSystemDefault()
    val nowDateTime = nowInstant.toLocalDateTime(timeZone)

    val today = nowDateTime.date
    val currentTime = nowDateTime.time

    // 1. Sprawdzamy msze z dnia dzisiejszego
    val todayHours = getMassHoursForDate(parish, today)
    val nextTodayTime = todayHours
        .mapNotNull { try { LocalTime.parse(it) } catch (e: Exception) { null } }
        .filter { it > currentTime }
        .minOrNull()

    if (nextTodayTime != null) {
        val targetDateTime = LocalDateTime(today, nextTodayTime) // <-- OBLICZONE
        val targetInstant = targetDateTime.toInstant(timeZone)
        val diff = targetInstant - nowInstant
        val totalMinutes = diff.inWholeMinutes
        return UpcomingMassInfo(nextTodayTime, false, totalMinutes / 60, totalMinutes % 60, targetDateTime) // <-- PRZEKAZANE
    }

    // 2. Jeśli dzisiaj już nie ma mszy, sprawdzamy dzień jutrzejszy
    val tomorrow = today.plus(1, DateTimeUnit.DAY)
    val tomorrowHours = getMassHoursForDate(parish, tomorrow)
    val nextTomorrowTime = tomorrowHours
        .mapNotNull { try { LocalTime.parse(it) } catch (e: Exception) { null } }
        .minOrNull()

    if (nextTomorrowTime != null) {
        val targetDateTime = LocalDateTime(tomorrow, nextTomorrowTime) // <-- OBLICZONE
        val targetInstant = targetDateTime.toInstant(timeZone)
        val diff = targetInstant - nowInstant
        val totalMinutes = diff.inWholeMinutes
        return UpcomingMassInfo(nextTomorrowTime, true, totalMinutes / 60, totalMinutes % 60, targetDateTime) // <-- PRZEKAZANE
    }

    return null
}

private fun getMassHoursForDate(parish: ParishEntity, date: LocalDate): List<String> {
    val massHoursString = when (date.dayOfWeek) {
        DayOfWeek.SUNDAY -> parish.massHoursSunday
        DayOfWeek.MONDAY -> parish.massHoursMonday
        DayOfWeek.TUESDAY -> parish.massHoursTuesday
        DayOfWeek.WEDNESDAY -> parish.massHoursWednesday
        DayOfWeek.THURSDAY -> parish.massHoursThursday
        DayOfWeek.FRIDAY -> parish.massHoursFriday
        DayOfWeek.SATURDAY -> parish.massHoursSaturday
        else -> null
    }
    if (massHoursString.isNullOrBlank()) return emptyList()
    return massHoursString.replace('.', ':')
        .split(Regex("[\\s,]+"))
        .filter { it.isNotBlank() }
        .map { hour ->
            val trimmed = hour.trim()
            if (trimmed.indexOf(':') == 1) "0$trimmed" else trimmed
        }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371e3
    val phi1 = lat1 * kotlin.math.PI / 180
    val phi2 = lat2 * kotlin.math.PI / 180
    val deltaPhi = (lat2 - lat1) * kotlin.math.PI / 180
    val deltaLambda = (lon2 - lon1) * kotlin.math.PI / 180
    val a = kotlin.math.sin(deltaPhi / 2) * kotlin.math.sin(deltaPhi / 2) +
            kotlin.math.cos(phi1) * kotlin.math.cos(phi2) *
            kotlin.math.sin(deltaLambda / 2) * kotlin.math.sin(deltaLambda / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}