package com.hitbosss.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import kotlin.math.abs

/**
 * Rueda de selección estilo iOS (UIPickerView). Muestra [visibleCount] ítems, el central
 * resaltado y el resto difuminados; hace snap al soltar y reporta el índice seleccionado.
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: androidx.compose.ui.unit.Dp = 48.dp,
    highlight: Boolean = true,
) {
    if (items.isEmpty()) return
    val half = visibleCount / 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex))
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    // Índice central = primer visible (con padding = half ítems arriba y abajo).
    val centerIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    // Al detenerse, reporta el ítem central.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                val i = listState.firstVisibleItemIndex.coerceIn(0, items.lastIndex)
                if (i != selectedIndex) onSelected(i)
            }
        }
    }
    // Si cambia el seleccionado desde fuera, reposiciona.
    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != selectedIndex) {
            listState.scrollToItem(selectedIndex.coerceIn(0, items.lastIndex))
        }
    }

    Box(modifier.height(itemHeight * visibleCount), contentAlignment = Alignment.Center) {
        if (highlight) {
            Box(Modifier.fillMaxWidth().height(itemHeight).clip(RoundedCornerShape(8.dp)).background(Gray200))
        }
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = itemHeight * half),
        ) {
            items(items.size) { i ->
                val distance = abs(i - centerIndex)
                val isCenter = distance == 0
                Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                    Text(
                        items[i],
                        style = if (isCenter) HitbosssType.titleSubsection else HitbosssType.bodyLargeRegular,
                        color = Gray800,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(
                            when (distance) {
                                0 -> 1f
                                1 -> 0.5f
                                2 -> 0.25f
                                else -> 0.12f
                            },
                        ),
                    )
                }
            }
        }
    }
}
