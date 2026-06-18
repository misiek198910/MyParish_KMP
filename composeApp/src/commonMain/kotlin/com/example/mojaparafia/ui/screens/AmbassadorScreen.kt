package com.example.mojaparafia.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.mojaparafia.ui.components.AdBanner
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AmbassadorScreen(
    points: Int,
    isPremium: Boolean,
    onBackClick: () -> Unit,
    showToast: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val ambassadorTitle = stringResource(Res.string.ambassador_title)
    val pointsSubtitle = stringResource(Res.string.ambassador_points_subtitle)
    val pointsThanks = stringResource(Res.string.ambassador_points_thanks)
    val progressTitle = stringResource(Res.string.ambassador_progress_title)

    val crownTitle = stringResource(Res.string.ambassador_reward_crown_title)
    val crownDesc = stringResource(Res.string.ambassador_reward_crown_desc)
    val crownToast = stringResource(Res.string.ambassador_reward_crown_toast)

    val vipTitle = stringResource(Res.string.ambassador_reward_vip_title)
    val vipDesc = stringResource(Res.string.ambassador_reward_vip_desc)
    val vipToast = stringResource(Res.string.ambassador_reward_vip_toast)

    val baseUrl = "https://mivs-myparish.mivs.dev/static/flame"
    val flameUrl = if (points > 0) "$baseUrl/flame_1.png" else "$baseUrl/flame_0.png"
    val localPlaceholder = if (points > 0) Res.drawable.flame_1 else Res.drawable.flame_0

    val flameMessage = when {
        points == 0 -> stringResource(Res.string.flame_msg_empty)
        points in 1..8 -> stringResource(Res.string.flame_msg_keep_going)
        points == 9 -> stringResource(Res.string.flame_msg_almost_crown)
        points in 10..44 -> stringResource(Res.string.flame_msg_crown_earned)
        points in 45..49 -> stringResource(Res.string.flame_msg_almost_premium)
        points >= 50 -> stringResource(Res.string.flame_msg_ambassador)
        else -> stringResource(Res.string.flame_msg_default)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ambassadorTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (!isPremium) {
                Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                    AdBanner(modifier = Modifier.fillMaxWidth(), isPremium = isPremium)
                }
            }
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp) // Zapobiega nadmiernemu rozciąganiu w trybie Landscape
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 240.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(), color = Color(0xFF1976D2),
                    shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(pointsSubtitle, color = Color.White.copy(alpha = 0.8f))
                        Text("$points", fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(pointsThanks, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text(progressTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(16.dp))

                RewardItem(
                    title = crownTitle,
                    desc = crownDesc,
                    requiredPoints = 10,
                    currentPoints = points,
                    icon = "👑"
                ) {
                    if (points >= 10) showToast(crownToast)
                }

                RewardItem(
                    title = vipTitle,
                    desc = vipDesc,
                    requiredPoints = 50,
                    currentPoints = points,
                    icon = "💎"
                ) {
                    if (points >= 50) showToast(vipToast)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
                    modifier = Modifier
                        .padding(bottom = 8.dp, end = 8.dp)
                        .widthIn(max = 250.dp)
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                ) {
                    AnimatedContent(
                        targetState = flameMessage,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "BubbleText"
                    ) { text ->
                        Text(
                            text = text,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1A252F)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            try {
                                uriHandler.openUri("mailto:kontakt@mojaparafia.pl?subject=Płomyk - Program Ambasador")
                            } catch (e: Exception) {
                                showToast("Brak aplikacji e-mail")
                            }
                        }
                ) {
                    AsyncImage(
                        model = flameUrl,
                        contentDescription = "Płomyk Asystent",
                        placeholder = painterResource(localPlaceholder),
                        error = painterResource(localPlaceholder),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun RewardItem(title: String, desc: String, requiredPoints: Int, currentPoints: Int, icon: String, onClick: () -> Unit) {
    val progress = (currentPoints.toFloat() / requiredPoints.toFloat()).coerceAtMost(1f)
    val isUnlocked = currentPoints >= requiredPoints

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(50.dp).background(if (isUnlocked) Color(0xFFFFD700) else Color.LightGray, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 24.sp) }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (isUnlocked) Color.Black else Color.Gray)
            Text(desc, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = if (isUnlocked) Color(0xFF4CAF50) else Color(0xFF1976D2), trackColor = Color.LightGray.copy(alpha = 0.3f))
        }

        if (isUnlocked) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.padding(start = 8.dp))
    }
}