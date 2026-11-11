package app.oltnm.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OltnMark(
    size: Dp = 104.dp,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val accent = MaterialTheme.colorScheme.primary
    val progress = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(animated) {
        if (animated) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1100,
                    easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
                )
            )
        }
    }

    Canvas(modifier.size(size)) {
        val scale = this.size.minDimension / 64f
        val centerPoint = center
        drawCircle(
            color = accent,
            radius = 21f * scale,
            style = Stroke(width = 5f * scale)
        )
        val rotation = -300f + 300f * progress.value
        val angle = Math.toRadians(-90.0 + rotation.toDouble())
        val orbit = 21f * scale
        drawCircle(
            color = accent,
            radius = 6.5f * scale,
            center = Offset(
                x = centerPoint.x + (cos(angle) * orbit).toFloat(),
                y = centerPoint.y + (sin(angle) * orbit).toFloat()
            )
        )
    }
}
