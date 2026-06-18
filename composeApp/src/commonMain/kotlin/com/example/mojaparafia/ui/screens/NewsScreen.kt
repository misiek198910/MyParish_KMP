package com.example.mojaparafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.mojaparafia.model.NewsResponse
import com.example.mojaparafia.ui.components.AdBanner
import com.example.mojaparafia.viewmodel.ParishListViewModel
import com.russhwolf.settings.Settings
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: ParishListViewModel,
    isPremium: Boolean,
    onBackClick: () -> Unit
) {
    val newsList by viewModel.newsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val settings = remember { Settings() }

    LaunchedEffect(newsList) {
        if (newsList.isNotEmpty()) {
            val latestId = newsList.maxOf { it.id }
            settings.putInt("last_read_news_id", latestId)
        }
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
                            text = stringResource(Res.string.news),
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
            if (!isPremium) {
                Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                    AdBanner(modifier = Modifier.fillMaxWidth(), isPremium = isPremium)
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
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF1976D2),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (newsList.isEmpty()) {
                Text(
                    text = stringResource(Res.string.no_news),
                    fontSize = 16.sp,
                    color = Color(0xFF546E7A),
                    fontFamily = FontFamily(Font(Res.font.lora_medium)),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(newsList) { news ->
                        NewsCard(news = news)
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(news: NewsResponse) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Obrazek (jeśli istnieje)
            if (!news.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = news.imageUrl,
                    contentDescription = news.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Data publikacji
                if (!news.publishDate.isNullOrEmpty()) {
                    Text(
                        text = news.publishDate,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Tytuł
                Text(
                    text = news.title ?: "",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A252F)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Treść
                if (!news.content.isNullOrEmpty()) {
                    Text(
                        text = news.content,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 22.sp
                    )
                }

                // Przycisk akcji (np. zbiórka, zewnętrzny link)
                if (!news.actionLink.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            try {
                                val link = news.actionLink!!
                                val safeUrl = if (!link.startsWith("http")) "https://$link" else link
                                uriHandler.openUri(safeUrl)
                            } catch (e: Exception) {
                                // Opcjonalnie: logowanie błędu, jeśli link jest niepoprawny
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "ZOBACZ WIĘCEJ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}