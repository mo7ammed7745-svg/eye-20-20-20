package com.mohamed.eye2020.ui.stats

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.mohamed.eye2020.domain.WeeklyStats
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale

object WeeklyReportGenerator {

    private const val WIDTH = 1080
    private const val HEIGHT = 1440
    private const val PADDING = 96f

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

    fun create(context: Context, stats: WeeklyStats, allTimeCount: Int): Uri {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        drawReport(bitmap, stats, allTimeCount)

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "eye2020-week-${stats.weekStart}.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        bitmap.recycle()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun shareIntent(context: Context, uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(com.mohamed.eye2020.R.string.share_subject))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun drawReport(bitmap: Bitmap, stats: WeeklyStats, allTimeCount: Int) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#0B1114"))

        val mint = Color.parseColor("#6FE7C8")
        val mist = Color.parseColor("#E6F0EC")
        val slate = Color.parseColor("#A9B8B4")
        val line = Color.parseColor("#2A3B40")

        val labelPaint = paint(size = 30f, color = mint, typeface = mediumTypeface())
        val titlePaint = paint(size = 68f, color = mist, typeface = mediumTypeface())
        val bodyPaint = paint(size = 32f, color = slate)
        val bigNumberPaint = paint(size = 150f, color = mint, typeface = lightTypeface())
        val bigLabelPaint = paint(size = 34f, color = slate)
        val dayPaint = paint(size = 28f, color = slate)
        val countPaint = paint(size = 34f, color = mist, typeface = mediumTypeface())
        val footerPaint = paint(size = 26f, color = slate)

        var y = 130f
        canvas.drawText("EYE 20-20-20", PADDING, y, labelPaint)
        y += 84f
        canvas.drawText("Weekly eye-break report", PADDING, y, titlePaint)
        y += 56f
        canvas.drawText(
            "${dateFormatter.format(stats.weekStart)} – ${dateFormatter.format(stats.weekEnd)}",
            PADDING,
            y,
            bodyPaint,
        )
        y += 46f
        canvas.drawRect(PADDING, y, WIDTH - PADDING, y + 2f, paint(color = line))
        y += 100f

        canvas.drawText(stats.weeklyTotal.toString(), PADDING, y + 70f, bigNumberPaint)
        canvas.drawText("completed breaks this week", PADDING + 250f, y + 42f, bigLabelPaint)
        canvas.drawText("All time: $allTimeCount", PADDING + 250f, y + 84f, dayPaint)
        y += 180f

        drawChart(
            canvas = canvas,
            stats = stats,
            top = y,
            bottom = y + 360f,
            trackColor = line,
            barColor = mint,
            countPaint = countPaint,
            dayPaint = dayPaint,
            todayColor = mist,
        )

        val footerY = HEIGHT - 150f
        canvas.drawRect(PADDING, footerY - 70f, WIDTH - PADDING, footerY - 68f, paint(color = line))
        canvas.drawText(
            "Look 20 feet away for 20 seconds, every 20 minutes.",
            PADDING,
            footerY,
            footerPaint,
        )
        canvas.drawText(
            "Only breaks that reach zero are counted.",
            PADDING,
            footerY + 42f,
            footerPaint,
        )
    }

    private fun drawChart(
        canvas: Canvas,
        stats: WeeklyStats,
        top: Float,
        bottom: Float,
        trackColor: Int,
        barColor: Int,
        countPaint: Paint,
        dayPaint: Paint,
        todayColor: Int,
    ) {
        val available = WIDTH - PADDING * 2
        val slot = available / 7f
        val barWidth = 52f
        val maxCount = maxOf(1, stats.days.maxOfOrNull { it.count } ?: 0)
        val chartHeight = bottom - top - 96f
        val today = java.time.LocalDate.now()

        stats.days.forEachIndexed { index, day ->
            val centerX = PADDING + slot * index + slot / 2f
            val fraction = day.count.toFloat() / maxCount.toFloat()
            val barHeight = if (day.count == 0) 5f else (chartHeight * fraction).coerceAtLeast(14f)
            val left = centerX - barWidth / 2f
            val barTop = bottom - 72f - barHeight

            val track = RectF(left, top, centerX + barWidth / 2f, bottom - 72f)
            canvas.drawRoundRect(track, 26f, 26f, paint(color = trackColor))

            if (day.count > 0) {
                val color = if (day.date == today) todayColor else barColor
                val bar = RectF(left, barTop, centerX + barWidth / 2f, bottom - 72f)
                canvas.drawRoundRect(bar, 26f, 26f, paint(color = color))
            }

            countPaint.textAlign = Paint.Align.CENTER
            dayPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(day.count.toString(), centerX, bottom - 92f, countPaint)
            canvas.drawText(day.label.uppercase(Locale.ENGLISH), centerX, bottom - 24f, dayPaint)
        }
        countPaint.textAlign = Paint.Align.LEFT
        dayPaint.textAlign = Paint.Align.LEFT
    }

    private fun paint(
        size: Float = 30f,
        color: Int = Color.WHITE,
        typeface: Typeface = regularTypeface(),
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        this.typeface = typeface
    }

    private fun regularTypeface(): Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private fun mediumTypeface(): Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private fun lightTypeface(): Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
}
