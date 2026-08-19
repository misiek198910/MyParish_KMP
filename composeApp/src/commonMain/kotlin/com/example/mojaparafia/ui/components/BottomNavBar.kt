package com.example.mojaparafia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.nav_home_parish
import myparish.composeapp.generated.resources.nav_map
import myparish.composeapp.generated.resources.nav_more
import org.jetbrains.compose.resources.stringResource

@Composable
fun CustomBottomNavBar(
    currentScreen: String,
    hasNewNews: Boolean = false,
    onNavigate: (String) -> Unit
) {
    // Usunięto sztywną wysokość 80.dp - teraz wysokość dostosowuje się dynamicznie
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Tło paska (Szkło) - na samym dole
        Surface(
            color = Color.White.copy(alpha = 0.65f), // Efekt szkła 65%
            shadowElevation = 0.dp,
            border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Pchnie zawartość (ikony) nad pasek systemowy, ale tło-szkło sięga samej krawędzi ekranu
                    .height(64.dp),          // Tylko obszar użyteczny ma 64.dp
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onNavigate("MAP") }) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = stringResource(Res.string.nav_map),
                        tint = if (currentScreen == "MAP") Color(0xFF1976D2) else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(72.dp))

                IconButton(onClick = { onNavigate("FUNCTIONS") }) {
                    Box {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(Res.string.nav_more),
                            tint = if (currentScreen == "FUNCTIONS") Color(0xFF1976D2) else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )

                        if (hasNewNews) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(10.dp)
                                    .background(Color.Red, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .size(72.dp)
                .background(Color(0xFFF5F7FA), CircleShape)
                .padding(6.dp)
                .background(if (currentScreen == "HOME_PARISH") Color(0xFF1976D2) else Color(0xFF4CAF50), CircleShape)
                .clip(CircleShape)
                .clickable { onNavigate("HOME_PARISH") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = stringResource(Res.string.nav_home_parish),
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}