package com.example.mojaparafia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mojaparafia.db.ParishEntity
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.lora_medium
import myparish.composeapp.generated.resources.sheet_btn_details
import myparish.composeapp.generated.resources.sheet_fav_active
import myparish.composeapp.generated.resources.sheet_fav_inactive
import myparish.composeapp.generated.resources.sheet_home_active
import myparish.composeapp.generated.resources.sheet_home_inactive
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParishSheet(
    parish: ParishEntity,
    isHomeParish: Boolean,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHomeParish: () -> Unit,
    onNavigateToDetails: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White.copy(alpha = 0.98f),
        scrimColor = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Text(
                text = parish.name ?: "",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A252F),
                letterSpacing = (-0.5).sp,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ID: ${parish.id} • ",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = parish.address ?: "",
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = onToggleHomeParish,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isHomeParish) Color(0xFFE8F5E9) else Color(0xFFF5F7FA),
                    border = BorderStroke(1.dp, if (isHomeParish) Color(0xFF81C784) else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .height(86.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isHomeParish) Icons.Filled.CheckCircle else Icons.Outlined.Home,
                            contentDescription = null,
                            tint = if (isHomeParish) Color(0xFF2E7D32) else Color(0xFF1976D2),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHomeParish) stringResource(Res.string.sheet_home_active) else stringResource(Res.string.sheet_home_inactive),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHomeParish) Color(0xFF2E7D32) else Color(0xFF1A252F),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Surface(
                    onClick = onToggleFavorite,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isFavorite) Color(0xFFFFF8E1) else Color(0xFFF5F7FA),
                    border = BorderStroke(1.dp, if (isFavorite) Color(0xFFFFD54F) else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .height(86.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (isFavorite) Color(0xFFFFA000) else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isFavorite) stringResource(Res.string.sheet_fav_active) else stringResource(Res.string.sheet_fav_inactive),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFavorite) Color(0xFFFFA000) else Color(0xFF1A252F),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onNavigateToDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1A252F)
                )
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF1976D2))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.sheet_btn_details),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}