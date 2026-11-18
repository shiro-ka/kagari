package dev.dimension.flare.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R

@Composable
internal fun KagariSearchSortChips(
    top: Boolean,
    onSelected: (Boolean) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = top,
            onClick = { onSelected(true) },
            label = { Text(text = stringResource(R.string.kagari_search_top)) },
        )
        FilterChip(
            selected = !top,
            onClick = { onSelected(false) },
            label = { Text(text = stringResource(R.string.kagari_search_latest)) },
        )
    }
}
