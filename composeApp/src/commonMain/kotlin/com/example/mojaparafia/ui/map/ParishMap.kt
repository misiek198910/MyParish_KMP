package com.example.mojaparafia.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mojaparafia.db.ParishEntity

@Composable
expect fun ParishMap(
    modifier: Modifier = Modifier,
    parishes: List<ParishEntity>,
    focusRequest: Pair<Double, Double>?,
    onMapFocused: () -> Unit,
    homeParishId: String?,
    userHasCrown: Boolean,
    onMapLoaded: () -> Unit,
    onMarkerClick: (parishId: String) -> Unit,
    onMapLongClick: (lat: Double, lng: Double) -> Unit
)