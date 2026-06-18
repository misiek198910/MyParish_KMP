package com.example.mojaparafia.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
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
fun HelpScreen(
    isPremium: Boolean,
    onBackClick: () -> Unit,
    showToast: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    val baseUrl = "https://mivs-myparish.mivs.dev/static/flame"
    var expandedCardId by remember { mutableStateOf<Int?>(null) }
    val isAnyExpanded = expandedCardId != null

    val flameUrl = if (isAnyExpanded) "$baseUrl/flame_1.png" else "$baseUrl/flame_0.png"
    val localPlaceholder = if (isAnyExpanded) Res.drawable.flame_1 else Res.drawable.flame_0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.help_center_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .padding(bottom = 150.dp)
            ) {
                Text(
                    text = stringResource(Res.string.help_center_faq_header),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ExpandableHelpCard(
                    id = 1,
                    title = stringResource(Res.string.help_q1_title),
                    description = stringResource(Res.string.help_q1_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 1) null else 1 }
                )
                ExpandableHelpCard(
                    id = 5,
                    title = stringResource(Res.string.help_q5_title),
                    description = stringResource(Res.string.help_q5_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 5) null else 5 }
                )
                ExpandableHelpCard(
                    id = 6,
                    title = stringResource(Res.string.help_q6_title),
                    description = stringResource(Res.string.help_q6_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 6) null else 6 }
                )
                ExpandableHelpCard(
                    id = 7,
                    title = stringResource(Res.string.help_q7_title),
                    description = stringResource(Res.string.help_q7_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 7) null else 7 }
                )
                ExpandableHelpCard(
                    id = 8,
                    title = stringResource(Res.string.help_q8_title),
                    description = stringResource(Res.string.help_q8_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 8) null else 8 }
                )
                ExpandableHelpCard(
                    id = 9,
                    title = stringResource(Res.string.help_q9_title),
                    description = stringResource(Res.string.help_q9_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 9) null else 9 }
                )
                ExpandableHelpCard(
                    id = 10,
                    title = stringResource(Res.string.help_q10_title),
                    description = stringResource(Res.string.help_q10_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 10) null else 10 }
                )
                ExpandableHelpCard(
                    id = 11,
                    title = stringResource(Res.string.help_q11_title),
                    description = stringResource(Res.string.help_q11_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 11) null else 11 }
                )
                ExpandableHelpCard(
                    id = 12,
                    title = stringResource(Res.string.help_q12_title),
                    description = stringResource(Res.string.help_q12_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 12) null else 12 }
                )
                ExpandableHelpCard(
                    id = 13,
                    title = stringResource(Res.string.help_q13_title),
                    description = stringResource(Res.string.help_q13_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 13) null else 13 }
                )
                ExpandableHelpCard(
                    id = 14,
                    title = stringResource(Res.string.help_q14_title),
                    description = stringResource(Res.string.help_q14_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 14) null else 14 }
                )
                ExpandableHelpCard(
                    id = 15,
                    title = stringResource(Res.string.help_q15_title),
                    description = stringResource(Res.string.help_q15_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 15) null else 15 }
                )
                ExpandableHelpCard(
                    id = 16,
                    title = stringResource(Res.string.help_q16_title),
                    description = stringResource(Res.string.help_q16_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 16) null else 16 }
                )
                ExpandableHelpCard(
                    id = 17,
                    title = stringResource(Res.string.help_q17_title),
                    description = stringResource(Res.string.help_q17_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 17) null else 17 }
                )
                ExpandableHelpCard(
                    id = 18,
                    title = stringResource(Res.string.help_q18_title),
                    description = stringResource(Res.string.help_q18_desc),
                    expandedCardId = expandedCardId,
                    onCardClick = { expandedCardId = if (expandedCardId == 18) null else 18 }
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                val bubbleText = if (isAnyExpanded) {
                    stringResource(Res.string.help_flame_bubble_expanded)
                } else {
                    stringResource(Res.string.help_flame_bubble_collapsed)
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
                    modifier = Modifier
                        .padding(bottom = 8.dp, end = 8.dp)
                        .widthIn(max = 250.dp)
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                ) {
                    AnimatedContent(
                        targetState = bubbleText,
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

                val noEmailToast = stringResource(Res.string.help_no_email_app)
                val emailSubject = stringResource(Res.string.help_email_subject)

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            try {
                                // Wieloplatformowe wywołanie klienta poczty
                                uriHandler.openUri("mailto:biuro.mojaparafia@gmail.com?subject=${emailSubject}")
                            } catch (e: Exception) {
                                showToast(noEmailToast)
                            }
                        }
                ) {
                    AsyncImage(
                        model = flameUrl,
                        contentDescription = stringResource(Res.string.help_flame_cd),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableHelpCard(
    id: Int,
    title: String,
    description: String,
    expandedCardId: Int?,
    onCardClick: () -> Unit
) {
    val isExpanded = expandedCardId == id
    val iconId = "loc_icon"

    val annotatedDescription = buildAnnotatedString {
        if (description.contains("[icon]")) {
            val parts = description.split("[icon]")
            if (parts.size == 2) {
                append(parts[0])
                appendInlineContent(iconId, "[icon]")
                append(parts[1])
            } else {
                append(description)
            }
        } else {
            append(description)
        }
    }

    val inlineContent = mapOf(
        iconId to InlineTextContent(
            Placeholder(
                width = 18.sp,
                height = 18.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            val painter = when (id) {
                1 -> painterResource(Res.drawable.ic_localization_1)
                6 -> painterResource(Res.drawable.ic_localization_2)
                16 -> rememberVectorPainter(image = Icons.Default.Star)
                else -> painterResource(Res.drawable.ic_localization_1)
            }

            Icon(
                painter = painter,
                contentDescription = null,
                tint = if (id == 16) Color(0xFFFFC107) else Color.Unspecified,
                modifier = Modifier.size(18.dp)
            )
        }
    )

    Card(
        onClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .animateContentSize(animationSpec = tween(300)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpanded) Color(0xFF1976D2) else Color(0xFF1A252F),
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = annotatedDescription,
                    inlineContent = inlineContent,
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}