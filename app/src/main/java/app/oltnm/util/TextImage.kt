package app.oltnm.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import app.oltnm.R
import app.oltnm.data.model.FontFamilyChoice
import java.io.ByteArrayOutputStream

data class TextImageStyle(
    val font: FontFamilyChoice,
    val textScale: Float,
    val textColor: Int,
    val backgroundColor: Int,
    val borderColor: Int,
    val accentColor: Int
)

object TextImage {
    private const val WIDTH = 1080
    private const val PADDING = 64
    private const val MAX_HEIGHT = 4096
    private const val BASE_TEXT_SIZE = 40f

    fun render(context: Context, text: String, style: TextImageStyle): ByteArray? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val limited = if (trimmed.length > 4000) {
            trimmed.take(4000).trimEnd() + "…"
        } else {
            trimmed
        }

        val typeface = typefaceFor(context, style.font)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = BASE_TEXT_SIZE * style.textScale
            color = style.textColor
            isSubpixelText = true
        }
        val contentWidth = WIDTH - PADDING * 2
        val lineHeight = textPaint.fontMetrics.bottom - textPaint.fontMetrics.top
        val maxLines = ((MAX_HEIGHT - PADDING * 2) / lineHeight).toInt().coerceAtLeast(1)
        val layout = StaticLayout.Builder
            .obtain(limited, 0, limited.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.25f)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        if (layout.height <= 0) return null

        val height = (layout.height + PADDING * 2).coerceAtMost(MAX_HEIGHT)
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.backgroundColor
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.borderColor
            this.style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.accentColor
        }
        val radius = 48f
        val rect = RectF(1f, 1f, WIDTH - 1f, height - 1f)
        canvas.drawRoundRect(rect, radius, radius, background)
        canvas.drawRoundRect(rect, radius, radius, border)
        canvas.drawRoundRect(
            RectF(
                PADDING / 2f,
                PADDING.toFloat(),
                PADDING / 2f + 6f,
                (height - PADDING).toFloat()
            ),
            3f,
            3f,
            accent
        )

        canvas.save()
        canvas.translate(PADDING.toFloat(), PADDING.toFloat())
        layout.draw(canvas)
        canvas.restore()

        val bytes = runCatching {
            ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
        }.getOrNull()
        bitmap.recycle()
        return bytes
    }

    private fun typefaceFor(context: Context, choice: FontFamilyChoice): Typeface = when (choice) {
        FontFamilyChoice.SpaceGrotesk -> ResourcesCompat.getFont(context, R.font.space_grotesk)
            ?: Typeface.SANS_SERIF
        FontFamilyChoice.Atkinson -> ResourcesCompat.getFont(context, R.font.atkinson_regular)
            ?: Typeface.SANS_SERIF
        FontFamilyChoice.System -> Typeface.SANS_SERIF
        FontFamilyChoice.Serif -> Typeface.SERIF
        FontFamilyChoice.Monospace -> Typeface.MONOSPACE
    }
}
