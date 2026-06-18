package com.example.mojaparafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mojaparafia.ui.components.AdBanner
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    isPremium: Boolean,
    userPoints: Int,
    monthlyPriceStr: String?,
    yearlyPriceStr: String?,
    onBackClick: () -> Unit,
    onBuyMonthlyClick: () -> Unit,
    onBuyYearlyClick: () -> Unit,
    onManageClick: () -> Unit,
    onRestoreClick: () -> Unit,
    isIos: Boolean = false // Wymusza zaślepkę
) {
    val isAmbassadorPremium = userPoints >= 50
    val effectivePremium = isPremium || isAmbassadorPremium

    val statusText = when {
        isPremium -> stringResource(Res.string.subs_active)
        isAmbassadorPremium -> stringResource(Res.string.subs_active_ambassador)
        else -> stringResource(Res.string.subs_deactive)
    }
    val statusColor = if (effectivePremium) Color(0xFF4CAF50) else Color(0xFFE74C3C)

    Scaffold(
        topBar = {
            Surface(
                color = Color.White.copy(alpha = 0.85f),
                shadowElevation = 2.dp
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.subscription_toolbar_title),
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
            if (!effectivePremium && !isIos) {
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
            // STATUS
            SubscriptionSection(title = stringResource(Res.string.subscription_header_status)) {
                Text(
                    text = statusText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    textAlign = TextAlign.Center
                )
            }

            // KORZYŚCI
            SubscriptionSection(title = stringResource(Res.string.subscription_header_benefits)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BenefitRow(text = stringResource(Res.string.subscription_benefit_ads))
                    BenefitRow(text = stringResource(Res.string.subscription_benefit_support))
                }
            }

            // ZAŚLEPKA iOS / SKLEP ANDROID
            Column {
                Text(
                    text = stringResource(Res.string.subscription_header_plans).uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(Res.font.lora_medium)),
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )

                if (isIos) {
                    // Widok zastępczy na system Apple
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE3F2FD),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.ios_subs_coming_soon),
                            fontSize = 14.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    if (effectivePremium) {
                        if (isPremium) {
                            Button(
                                onClick = onManageClick,
                                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                shape = RoundedCornerShape(32.dp)
                            ) {
                                Text(stringResource(Res.string.settings_subs), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.subs_ambassador_congrats),
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onBuyMonthlyClick,
                            enabled = monthlyPriceStr != null,
                            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            shape = RoundedCornerShape(32.dp)
                        ) {
                            Text(
                                text = monthlyPriceStr?.let { stringResource(Res.string.subs_monthly_price, it) } ?: stringResource(Res.string.subs_loading),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onBuyYearlyClick,
                            enabled = yearlyPriceStr != null,
                            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            shape = RoundedCornerShape(32.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Text(
                                text = yearlyPriceStr?.let { stringResource(Res.string.subs_yearly_price, it) } ?: stringResource(Res.string.subs_loading),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = onRestoreClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(Res.string.subscription_restore_button),
                                color = Color(0xFF546E7A),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SubscriptionSection(title: String, content: @Composable ColumnScope.() -> Unit) {
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
fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, fontSize = 16.sp, color = Color(0xFF2C3E50), lineHeight = 22.sp)
    }
}