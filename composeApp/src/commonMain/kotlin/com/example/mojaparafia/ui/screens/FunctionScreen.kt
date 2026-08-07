package com.example.mojaparafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*

@Composable
fun FunctionScreen(
    onAddParishClick: () -> Unit,
    onNewsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSupportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    hasNewNews: Boolean,
    isIos: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
            .padding(top = 72.dp, bottom = 100.dp)
    ) {
        FunctionOptionCard(
            icon = painterResource(Res.drawable.ic_add_vector),
            title = stringResource(Res.string.main_drawer_add_parish),
            showBadge = false,
            onClick = onAddParishClick
        )
        FunctionOptionCard(
            icon = painterResource(Res.drawable.ic_notification_vector),
            title = stringResource(Res.string.news),
            showBadge = hasNewNews,
            onClick = onNewsClick
        )
        FunctionOptionCard(
            icon = rememberVectorPainter(Icons.Default.Info),
            title = stringResource(Res.string.main_help_center),
            showBadge = false,
            onClick = onHelpClick
        )

        if (!isIos) {
            FunctionOptionCard(
                icon = painterResource(Res.drawable.ic_coffe),
                title = stringResource(Res.string.support_project_title),
                showBadge = false,
                onClick = onSupportClick
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        FunctionOptionCard(
            icon = painterResource(Res.drawable.ic_settings_vector),
            title = stringResource(Res.string.ustawienia),
            showBadge = false,
            onClick = onSettingsClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionOptionCard(
    icon: androidx.compose.ui.graphics.painter.Painter,
    title: String,
    showBadge: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A252F),
                modifier = Modifier.weight(1f)
            )
            if (showBadge) {
                Box(modifier = Modifier.size(10.dp).background(Color.Red, CircleShape))
            }
        }
    }
}