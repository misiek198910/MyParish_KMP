package com.example.mojaparafia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import myparish.composeapp.generated.resources.Res
import myparish.composeapp.generated.resources.*

// Przejrzysty model stanu zamiast kilkunastu zmiennych
data class FilterState(
    val isCathedral: Boolean = false,
    val isChurch: Boolean = false,
    val isMassForChildren: Boolean = false,
    val isVigilMass: Boolean = false,
    val isConfession: Boolean = false,
    val isAdoration: Boolean = false,
    val isFavorite: Boolean = false,
    val isMultimedia: Boolean = false,
    val regionQuery: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    initialState: FilterState,
    onDismiss: () -> Unit,
    onApplyFilters: (FilterState) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF5F7FA),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            // Nasz własny drag handle, żeby zachować Twój stary styl
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFAAB8C2))
            )
        }
    ) {
        FilterContent(
            initialState = initialState,
            onApply = onApplyFilters
        )
    }
}

@Composable
private fun FilterContent(
    initialState: FilterState,
    onApply: (FilterState) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Lokalne stany dla modyfikowania filtrów przed zapisem
    var isCathedral by remember { mutableStateOf(initialState.isCathedral) }
    var isChurch by remember { mutableStateOf(initialState.isChurch) }
    var isMassForChildren by remember { mutableStateOf(initialState.isMassForChildren) }
    var isVigilMass by remember { mutableStateOf(initialState.isVigilMass) }
    var isConfession by remember { mutableStateOf(initialState.isConfession) }
    var isAdoration by remember { mutableStateOf(initialState.isAdoration) }
    var isFavorite by remember { mutableStateOf(initialState.isFavorite) }
    var isMultimedia by remember { mutableStateOf(initialState.isMultimedia) }
    var regionQuery by remember { mutableStateOf(initialState.regionQuery) }

    val applyFilters = {
        onApply(
            FilterState(
                isCathedral = isCathedral,
                isChurch = isChurch,
                isMassForChildren = isMassForChildren,
                isVigilMass = isVigilMass,
                isConfession = isConfession,
                isAdoration = isAdoration,
                isFavorite = isFavorite,
                isMultimedia = isMultimedia,
                regionQuery = regionQuery.trim()
            )
        )
    }

    // Obliczamy bezpieczny margines dolny dla klawiatury i paska nawigacji
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding)
            .imePadding()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.filter_object),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A252F),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        FilterCard(title = stringResource(Res.string.filter_header_type)) {
            FilterSwitchRow(
                label = stringResource(Res.string.filter_cathedrals),
                isChecked = isCathedral,
                onCheckedChange = {
                    isCathedral = it
                    if (it) isChurch = false
                }
            )
            FilterSwitchRow(
                label = stringResource(Res.string.filter_churches),
                isChecked = isChurch,
                onCheckedChange = {
                    isChurch = it
                    if (it) isCathedral = false
                }
            )
        }

        FilterCard(title = stringResource(Res.string.filter_header_services)) {
            FilterSwitchRow(label = stringResource(Res.string.filter_text2), isChecked = isMassForChildren, onCheckedChange = { isMassForChildren = it })
            FilterSwitchRow(label = stringResource(Res.string.filter_vigil_mass_label), isChecked = isVigilMass, onCheckedChange = { isVigilMass = it })
            FilterSwitchRow(label = stringResource(Res.string.filter_confession_label), isChecked = isConfession, onCheckedChange = { isConfession = it })
            FilterSwitchRow(label = stringResource(Res.string.filter_adoration), isChecked = isAdoration, onCheckedChange = { isAdoration = it })
        }

        FilterCard(title = stringResource(Res.string.filter_header_area)) {
            OutlinedTextField(
                value = regionQuery,
                onValueChange = { regionQuery = it },
                placeholder = { Text(stringResource(Res.string.filter_region_hint), color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2C3E50),
                    unfocusedBorderColor = Color.LightGray,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    applyFilters()
                })
            )
        }

        FilterCard(title = "") {
            FilterSwitchRow(label = stringResource(Res.string.filter_favorite), isChecked = isFavorite, onCheckedChange = { isFavorite = it })
            FilterSwitchRow(label = stringResource(Res.string.filter_multimedia), isChecked = isMultimedia, onCheckedChange = { isMultimedia = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.horizontalGradient(colors = listOf(Color(0xFF1976D2), Color(0xFF0D47A1))))
                .clickable { applyFilters() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.filter_use),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FilterCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (title.isNotEmpty()) {
                Text(
                    text = title.uppercase(),
                    color = Color(0xFF1976D2),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            content()
        }
    }
}

@Composable
private fun FilterSwitchRow(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF1A252F),
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1976D2)
            )
        )
    }
}