package app.oltnm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import app.oltnm.ui.theme.LocalOltnSemantics

@Composable
fun OltnGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val semantics = LocalOltnSemantics.current
    Column(
        modifier = modifier
            .clip(shape)
            .background(semantics.glass)
            .border(1.dp, semantics.glassBorder, shape)
            .padding(contentPadding),
        content = content
    )
}
