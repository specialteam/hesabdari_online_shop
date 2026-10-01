package ir.hesabdari.shop.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import ir.hesabdari.shop.R
import java.io.File
import java.io.FileOutputStream

/** Renders a [ReportDoc] to an A4 right-to-left PDF using the bundled Persian font. */
object PdfExporter {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f
    private const val FOOTER_H = 30f

    private const val INK = 0xFF1B2425.toInt()
    private const val MUTED = 0xFF6B7678.toInt()
    private const val ACCENT = 0xFF0A6C74.toInt()
    private const val GAIN = 0xFF1E8E5A.toInt()
    private const val LOSS = 0xFFC93B3B.toInt()
    private const val LINE = 0xFFD5DDDE.toInt()
    private const val HEAD_BG = 0xFFE3F1F2.toInt()
    private const val ZEBRA = 0xFFF6F9F9.toInt()

    fun render(context: Context, doc: ReportDoc, out: File) {
        val regular = ResourcesCompat.getFont(context, R.font.vazirmatn_regular) ?: Typeface.DEFAULT
        val bold = ResourcesCompat.getFont(context, R.font.vazirmatn_bold) ?: Typeface.DEFAULT_BOLD
        val pdf = PdfDocument()
        try {
            Writer(pdf, regular, bold).write(doc)
            out.parentFile?.mkdirs()
            FileOutputStream(out).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }

    private class Writer(val pdf: PdfDocument, val regular: Typeface, val bold: Typeface) {
        private var pageNo = 0
        private lateinit var page: PdfDocument.Page
        private lateinit var canvas: Canvas
        private var y = MARGIN
        private val contentW = PAGE_W - 2 * MARGIN
        private val right = PAGE_W - MARGIN
        private val bottomLimit = PAGE_H - MARGIN - FOOTER_H

        private fun paint(size: Float, color: Int = INK, isBold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (isBold) bold else regular
        }

        private fun layout(text: String, p: TextPaint, width: Float, align: Layout.Alignment): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, p, width.toInt().coerceAtLeast(1))
                .setAlignment(align)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()

        private fun draw(l: StaticLayout, x: Float, top: Float) {
            canvas.save()
            canvas.translate(x, top)
            l.draw(canvas)
            canvas.restore()
        }

        private fun startPage() {
            pageNo++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page.canvas
            y = MARGIN
        }

        private fun finishPage() {
            val p = paint(8f, MUTED)
            val footer = layout("حسابدار شاپ  •  صفحه ${ir.hesabdari.shop.util.Fa.digits(pageNo.toString())}", p, contentW, Layout.Alignment.ALIGN_CENTER)
            canvas.drawLine(MARGIN, PAGE_H - MARGIN - FOOTER_H + 8, right, PAGE_H - MARGIN - FOOTER_H + 8, Paint().apply { color = LINE; strokeWidth = 0.6f })
            draw(footer, MARGIN, PAGE_H - MARGIN - FOOTER_H + 14)
            pdf.finishPage(page)
        }

        private fun ensure(h: Float) {
            if (y + h > bottomLimit) {
                finishPage()
                startPage()
            }
        }

        fun write(doc: ReportDoc) {
            startPage()
            // Title
            val tl = layout(doc.title, paint(18f, ACCENT, true), contentW, Layout.Alignment.ALIGN_NORMAL)
            draw(tl, MARGIN, y)
            y += tl.height + 4
            doc.subtitle?.takeIf { it.isNotBlank() }?.let {
                val sl = layout(it, paint(9.5f, MUTED), contentW, Layout.Alignment.ALIGN_NORMAL)
                draw(sl, MARGIN, y)
                y += sl.height + 4
            }
            canvas.drawRect(MARGIN, y, right, y + 2f, Paint().apply { color = ACCENT })
            y += 12
            for (b in doc.blocks) block(b)
            finishPage()
        }

        private fun block(b: Block) {
            when (b) {
                is Block.Heading -> {
                    ensure(40f)
                    y += 6
                    val l = layout(b.text, paint(12f, ACCENT, true), contentW, Layout.Alignment.ALIGN_NORMAL)
                    draw(l, MARGIN, y)
                    y += l.height + 3
                    canvas.drawLine(MARGIN, y, right, y, Paint().apply { color = LINE; strokeWidth = 0.8f })
                    y += 6
                }
                is Block.Paragraph -> {
                    val p = paint(if (b.small) 8.5f else 10f, if (b.small) MUTED else INK)
                    val l = layout(b.text, p, contentW, if (b.center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
                    ensure(l.height + 6f)
                    draw(l, MARGIN, y)
                    y += l.height + 6
                }
                is Block.KeyValues -> keyValues(b)
                is Block.Table -> table(b)
                Block.Spacer -> y += 10
            }
        }

        private fun toneColor(t: Tone) = when (t) {
            Tone.NEUTRAL -> INK
            Tone.GAIN -> GAIN
            Tone.LOSS -> LOSS
            Tone.MUTED -> MUTED
        }

        private fun keyValues(b: Block.KeyValues) {
            val labelW = contentW * 0.5f
            val valueW = contentW * 0.5f - 8f
            for (kv in b.rows) {
                val l1 = layout(kv.label, paint(10f, MUTED), labelW, Layout.Alignment.ALIGN_NORMAL)
                val l2 = layout(kv.value, paint(10.5f, toneColor(kv.tone), kv.bold), valueW, Layout.Alignment.ALIGN_OPPOSITE)
                val h = maxOf(l1.height, l2.height) + 7f
                ensure(h)
                draw(l1, right - labelW, y + 3)
                draw(l2, MARGIN, y + 3)
                canvas.drawLine(MARGIN, y + h, right, y + h, Paint().apply { color = 0xFFEAEFEF.toInt(); strokeWidth = 0.5f })
                y += h
            }
            y += 4
        }

        private fun table(t: Block.Table) {
            val total = t.weights.sum()
            val widths = t.weights.map { it / total * contentW }
            val pad = 4f
            val headP = paint(8.5f, ACCENT, true)
            val cellP = paint(8.5f)

            fun rowHeight(cells: List<String>, p: TextPaint): Float =
                cells.mapIndexed { i, c -> layout(c, p, widths[i] - 2 * pad, Layout.Alignment.ALIGN_CENTER).height }.maxOrNull()?.plus(2 * pad) ?: 0f

            fun drawRow(cells: List<String>, p: TextPaint, h: Float, bg: Int?) {
                if (bg != null) canvas.drawRect(MARGIN, y, right, y + h, Paint().apply { color = bg })
                var x = right
                cells.forEachIndexed { i, c ->
                    x -= widths[i]
                    val l = layout(c, p, widths[i] - 2 * pad, Layout.Alignment.ALIGN_CENTER)
                    draw(l, x + pad, y + (h - l.height) / 2)
                }
                canvas.drawLine(MARGIN, y + h, right, y + h, Paint().apply { color = LINE; strokeWidth = 0.5f })
                y += h
            }

            fun header() {
                val h = rowHeight(t.headers, headP)
                ensure(h + 24f)
                drawRow(t.headers, headP, h, HEAD_BG)
            }
            header()
            t.rows.forEachIndexed { idx, row ->
                val h = rowHeight(row, cellP)
                if (y + h > bottomLimit) {
                    finishPage(); startPage(); header()
                }
                drawRow(row, cellP, h, if (idx % 2 == 1) ZEBRA else null)
            }
            t.footer?.let {
                val fp = paint(8.5f, ACCENT, true)
                val h = rowHeight(it, fp)
                ensure(h)
                drawRow(it, fp, h, HEAD_BG)
            }
            y += 8
        }
    }
}
