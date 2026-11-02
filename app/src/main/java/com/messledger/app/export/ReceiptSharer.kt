package com.messledger.app.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.messledger.app.data.Config
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Everything needed to print or share one cycle's bill. Kept as plain data so
 * the receipt can be rendered identically on screen, as text, and as a PNG.
 */
data class ReceiptData(
    val cycleStart: LocalDate,
    val cycleEnd: LocalDate,
    val daysElapsed: Int,
    val mealsBilled: Double,
    val guestPortions: Int,
    val rebatesUsed: Double,
    val allowance: Int,
    val subtotal: Float,
    val tax: Float,
    val total: Float
)

object ReceiptSharer {

    private val rangeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    private val shortFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

    fun subject(): String = "Mess Ledger bill receipt"

    fun receiptText(data: ReceiptData): String = buildString {
        appendLine("MESS LEDGER")
        appendLine("Bill receipt")
        appendLine("${data.cycleStart.format(rangeFormatter)} — ${data.cycleEnd.format(rangeFormatter)}")
        appendLine()
        appendLine("Days so far        ${data.daysElapsed}")
        appendLine("Meal portions      ${formatUnits(data.mealsBilled)}")
        appendLine("Guest portions     ${data.guestPortions}")
        appendLine("Rebates used       ${formatUnits(data.rebatesUsed)} of ${data.allowance}")
        appendLine()
        appendLine("Subtotal           ₹${"%.2f".format(data.subtotal)}")
        appendLine("GST                ₹${"%.2f".format(data.tax)}")
        appendLine("TOTAL              ₹${"%.2f".format(data.total)}")
        appendLine()
        appendLine("Mess Ledger · warden's copy")
    }

    fun shareText(context: Context, data: ReceiptData) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject())
            putExtra(Intent.EXTRA_TEXT, receiptText(data))
        }
        context.startActivity(
            Intent.createChooser(intent, "Share receipt").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun shareImage(context: Context, data: ReceiptData, config: Config) {
        val bitmap = renderBitmap(data, config)
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "mess-receipt-${LocalDate.now()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject())
            putExtra(Intent.EXTRA_TEXT, receiptText(data))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share receipt").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** A clean, print-like receipt. Deliberately theme-independent. */
    fun renderBitmap(data: ReceiptData, config: Config): Bitmap {
        val width = 900
        val margin = 72f
        val height = 990

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val ink = Color.rgb(28, 28, 28)
        val soft = Color.rgb(110, 108, 102)
        val gold = Color.rgb(150, 112, 40)
        val rule = Color.rgb(214, 208, 194)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 54f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = soft
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.18f
        }
        val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = soft
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 30f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 46f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = soft
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rule
            strokeWidth = 2f
        }

        var y = 90f
        canvas.drawText("MESS LEDGER", margin, y, titlePaint)
        y += 38f
        canvas.drawText("BILL RECEIPT", margin, y, subtitlePaint)
        y += 54f
        canvas.drawText(
            "${data.cycleStart.format(rangeFormatter)}  —  ${data.cycleEnd.format(rangeFormatter)}",
            margin,
            y,
            rangePaint
        )

        y += 36f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += 50f

        fun row(label: String, value: String) {
            canvas.drawText(label, margin, y, labelPaint)
            canvas.drawText(value, width - margin, y, valuePaint)
            y += 58f
        }

        row("Days so far", data.daysElapsed.toString())
        row("Meal portions billed", formatUnits(data.mealsBilled))
        row("Guest portions", data.guestPortions.toString())
        row("Rebates used", "${formatUnits(data.rebatesUsed)} / ${data.allowance}")
        row("Rate per lunch", "₹${"%.2f".format(config.priceLunch)}")

        y += 6f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += 52f

        row("Subtotal", "₹${"%.2f".format(data.subtotal)}")
        row("GST ${"%.1f".format(config.taxPercent)}%", "₹${"%.2f".format(data.tax)}")

        y += 2f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        canvas.drawLine(margin, y + 5f, width - margin, y + 5f, linePaint)
        y += 62f

        canvas.drawText("TOTAL DUE", margin, y, totalLabelPaint)
        canvas.drawText("₹${"%.2f".format(data.total)}", width - margin, y, totalPaint)

        y += 52f
        canvas.drawText(
            "Generated ${LocalDate.now().format(shortFormatter)} · warden's copy",
            margin,
            y,
            footerPaint
        )

        return bitmap
    }

    private fun formatUnits(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
}
