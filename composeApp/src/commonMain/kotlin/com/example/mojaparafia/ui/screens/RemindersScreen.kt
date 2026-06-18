package com.example.mojaparafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mojaparafia.ui.components.AdBanner
import com.example.mojaparafia.util.Reminder
import com.example.mojaparafia.util.ReminderScheduler
import com.example.mojaparafia.viewmodel.ParishListViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: ParishListViewModel,
    reminderScheduler: ReminderScheduler,
    onBackClick: () -> Unit,
    isPremium: Boolean
) {
    val reminders by viewModel.remindersList.collectAsState(emptyList())
    val userPoints by viewModel.userPoints.collectAsState(0)
    val effectivePremium = isPremium || userPoints >= 50

    // Filtrowanie tylko przyszłych przypomnień za pomocą kotlinx-datetime
    val futureReminders = remember(reminders) {
        val currentMoment = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()

        reminders.filter { reminder ->
            val reminderInstant = reminder.reminderDateTime.toInstant(timeZone)
            reminderInstant > currentMoment
        }.sortedBy { it.reminderDateTime }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White.copy(alpha = 0.85f),
                shadowElevation = 2.dp
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.notification1),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A252F)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Wróć",
                                tint = Color(0xFF1A252F)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        bottomBar = {
            // Współdzielony AdBanner
            if (!effectivePremium) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.65f))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    AdBanner(modifier = Modifier.fillMaxWidth(), isPremium = effectivePremium)
                }
            }
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (futureReminders.isEmpty()) {
                Text(
                    text = stringResource(Res.string.notification2),
                    fontSize = 16.sp,
                    color = Color(0xFF546E7A),
                    fontFamily = FontFamily(Font(Res.font.lora_medium)),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = futureReminders,
                        key = { it.notificationId } // Optymalizacja list w Compose
                    ) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onDeleteClick = {
                                viewModel.removeReminder(reminderScheduler, reminder.notificationId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onDeleteClick: () -> Unit
) {
    // Ręczne formatowanie daty kompatybilne z KMP (kotlinx-datetime)
    val dt = reminder.reminderDateTime
    val day = dt.dayOfMonth.toString().padStart(2, '0')
    val month = dt.monthNumber.toString().padStart(2, '0')
    val hour = dt.hour.toString().padStart(2, '0')
    val minute = dt.minute.toString().padStart(2, '0')
    val formattedTime = "$day.$month.${dt.year} $hour:$minute"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFE3F2FD),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.parishName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A252F),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Msza Święta: ${reminder.massTime}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Przypomnienie: $formattedTime",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Usuń",
                    tint = Color(0xFFE74C3C)
                )
            }
        }
    }
}