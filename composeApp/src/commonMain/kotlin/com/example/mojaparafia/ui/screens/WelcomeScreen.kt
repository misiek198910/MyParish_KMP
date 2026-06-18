package com.example.mojaparafia.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*

@Composable
fun WelcomeScreen(
    isProcessing: Boolean,
    isSyncingData: Boolean,
    isIos: Boolean,
    onNextStepClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val alphaAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(50f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }
    LaunchedEffect(Unit) {
        offsetYAnim.animateTo(0f, animationSpec = tween(durationMillis = 800))
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSyncingData) {
                    Text(
                        text = stringResource(Res.string.welcome_sync_database),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(bottom = 16.dp),
                        color = Color(0xFF1976D2),
                        trackColor = Color(0xFFE0E0E0)
                    )
                } else {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(36.dp)
                                .padding(bottom = 8.dp),
                            color = Color(0xFF1976D2)
                        )
                    }

                    Button(
                        onClick = onNextStepClick,
                        enabled = !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(30.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.welcome_text5),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .offset(y = offsetYAnim.value.dp)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(Res.drawable.image_church),
                contentDescription = null,
                modifier = Modifier.size(140.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.welcome_text1),
                fontFamily = FontFamily(Font(Res.font.lora_medium)),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.welcome_text2),
                fontFamily = FontFamily(Font(Res.font.lora_medium)),
                fontSize = 16.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                WelcomeFeatureText(stringResource(Res.string.welcome_text3))
                WelcomeFeatureText(stringResource(Res.string.welcome_text4))
                WelcomeFeatureText(stringResource(Res.string.welcome_text6))
                WelcomeFeatureText(stringResource(Res.string.welcome_text7))
                if (!isIos) {
                    WelcomeFeatureText(stringResource(Res.string.welcome_welcome_text_3))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WelcomeFeatureText(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily(Font(Res.font.lora_medium)),
        fontSize = 14.sp,
        color = Color.Black.copy(alpha = 0.85f),
        lineHeight = 20.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    )
}