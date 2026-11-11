package app.oltnm.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

data class CodeBlock(
    val raw: String,
    val code: String,
    val lang: String
)

object CodeImage {
    private val fence = Regex(
        pattern = "^[\\t ]*```[\\t ]*([^\\n`]*)\\r?\\n([\\s\\S]*?)^[\\t ]*```[\\t ]*$",
        options = setOf(RegexOption.MULTILINE)
    )

    fun extractCodeBlocks(text: String): List<CodeBlock> = fence.findAll(text).map { match ->
        CodeBlock(
            raw = match.value,
            code = match.groupValues[2].removeSuffix("\n"),
            lang = match.groupValues[1].trim()
        )
    }.toList()

    fun renderPng(block: CodeBlock): ByteArray? {
        val lines = block.code
            .replace("\t", "  ")
            .replace("\r", "")
            .split("\n")
            .toMutableList()
        while (lines.isNotEmpty() && lines.first().isBlank()) lines.removeAt(0)
        while (lines.isNotEmpty() && lines.last().isBlank()) lines.removeAt(lines.lastIndex)
        if (lines.isEmpty()) lines += ""

        val textSize = 28f
        val padding = 40f
        val lineHeight = 44f
        val maxContent = 1000f
        val maxLines = 160

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE
            this.textSize = textSize
            color = Color.rgb(215, 222, 235)
        }

        val visual = mutableListOf<String>()
        for (sourceLine in lines) {
            if (sourceLine.isEmpty()) {
                visual += ""
                if (visual.size >= maxLines) break
                continue
            }
            var rest = sourceLine
            while (rest.isNotEmpty() && visual.size < maxLines) {
                val count = textPaint.breakText(rest, true, maxContent, null)
                val safe = count.coerceAtLeast(1)
                visual += rest.substring(0, safe)
                rest = rest.substring(safe)
            }
            if (visual.size >= maxLines) {
                visual += "…"
                break
            }
        }
        if (visual.isEmpty()) visual += ""

        val contentWidth = visual.maxOfOrNull { textPaint.measureText(it).toDouble() }?.toFloat() ?: 0f
        val imageWidth = (contentWidth + padding * 2).coerceIn(360f, maxContent + padding * 2)
        val imageHeight = padding * 2 + visual.size * lineHeight

        val bitmap = Bitmap.createBitmap(
            imageWidth.toInt().coerceAtLeast(1),
            imageHeight.toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 34, 43)
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(58, 64, 76)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val rect = RectF(1f, 1f, imageWidth - 1f, imageHeight - 1f)
        val radius = 24f
        canvas.drawRoundRect(rect, radius, radius, background)
        canvas.drawRoundRect(rect, radius, radius, border)

        val baselineOffset = -textPaint.fontMetrics.ascent
        visual.forEachIndexed { index, line ->
            canvas.drawText(line, padding, padding + baselineOffset + index * lineHeight, textPaint)
        }

        val bytes = runCatching {
            ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
        }.getOrNull()
        bitmap.recycle()
        return bytes
    }
}
