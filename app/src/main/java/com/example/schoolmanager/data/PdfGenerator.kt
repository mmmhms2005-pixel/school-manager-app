package com.example.schoolmanager.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    private const val A4_WIDTH_PT = 595
    private const val A4_HEIGHT_PT = 842
    private const val A4_WIDTH_LS_PT = 842
    private const val A4_HEIGHT_LS_PT = 595

    private const val MARGIN = 25f

    private const val FONT_SCHOOL = 16f
    private const val FONT_YEAR = 10f
    private const val FONT_TITLE = 13f
    private const val FONT_META = 9f
    private const val FONT_TABLE_HEADER = 9f
    private const val FONT_TABLE_BODY = 9f
    private const val FONT_SIGNATURE = 9f

    data class SchoolInfo(
        val schoolName: String = "",
        val academicYear: String = "",
        val principalName: String = "",
        val teacherName: String = "",
        val logoPath: String = ""
    )

    data class Column(
        val title: String,
        val width: Float
    )

    data class ReportData(
        val title: String,
        val meta: String,
        val columns: List<Column>,
        val rows: List<List<String>>,
        val isLandscape: Boolean = false
    )

    fun generate(
        context: Context,
        reports: List<ReportData>,
        school: SchoolInfo,
        fileName: String
    ): File {
        val doc = PdfDocument()
        var pageNumber = 1

        reports.forEach { report ->
            val width = if (report.isLandscape) A4_WIDTH_LS_PT else A4_WIDTH_PT
            val height = if (report.isLandscape) A4_HEIGHT_LS_PT else A4_HEIGHT_PT

            val pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
            val page = doc.startPage(pageInfo)
            drawReportPage(page.canvas, width, height, report, school)
            doc.finishPage(page)
            pageNumber++
        }

        val outDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(outDir, fileName)
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    private fun drawReportPage(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        report: ReportData,
        school: SchoolInfo
    ) {
        canvas.drawColor(Color.WHITE)
        var y = MARGIN

        // ===== الترويسة =====
        val schoolPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = FONT_SCHOOL
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        drawCenteredText(canvas, school.schoolName.ifBlank { "اسم المدرسة" }, pageWidth / 2f, y + FONT_SCHOOL, schoolPaint)
        y += FONT_SCHOOL + 4

        val yearPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = FONT_YEAR
            textAlign = Paint.Align.CENTER
        }
        if (school.academicYear.isNotBlank()) {
            drawCenteredText(canvas, school.academicYear, pageWidth / 2f, y + FONT_YEAR, yearPaint)
            y += FONT_YEAR + 4
        }

        // خط فاصل
        val linePaint = Paint().apply {
            color = Color.rgb(15, 118, 110)
            strokeWidth = 1.5f
        }
        canvas.drawLine(MARGIN, y, pageWidth - MARGIN, y, linePaint)
        y += 6

        // عنوان التقرير (يمين) + المعلومات (يسار)
        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = FONT_TITLE
            textAlign = Paint.Align.RIGHT
            isFakeBoldText = true
        }
        canvas.drawText(report.title, pageWidth - MARGIN, y + FONT_TITLE, titlePaint)

        val metaPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = FONT_META
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(report.meta, MARGIN, y + FONT_META, metaPaint)

        y += FONT_TITLE + 10

        // ===== الجدول =====
        val tableWidth = pageWidth - 2 * MARGIN
        val colWidths = report.columns.map { it.width * tableWidth }

        // ★ الإصلاح 1: زيادة ارتفاع صف الرأس بشكل كبير (سطران)
        val rowHeight = (FONT_TABLE_BODY + 6f)
        val headerHeight = (FONT_TABLE_HEADER * 2.8f) + 4f  // ~29pt — يستوعب سطرين كاملين

        val headerPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = FONT_TABLE_HEADER
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val cellBgPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
        val borderPaint = Paint().apply {
            color = Color.rgb(200, 200, 200)
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }

        // خلفية الرأس
        canvas.drawRect(MARGIN, y, pageWidth - MARGIN, y + headerHeight, cellBgPaint)

        var x = pageWidth - MARGIN
        report.columns.forEachIndexed { i, col ->
            val colW = colWidths[i]
            val cellX = x - colW
            drawHeaderCell(canvas, col.title, cellX, y, colW, headerHeight, headerPaint)
            canvas.drawRect(cellX, y, x, y + headerHeight, borderPaint)
            x = cellX
        }
        y += headerHeight

        // صفوف الجدول
        val bodyPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = FONT_TABLE_BODY
            textAlign = Paint.Align.CENTER
        }
        report.rows.forEach { row ->
            if (y + rowHeight > pageHeight - MARGIN - 60) return@forEach
            x = pageWidth - MARGIN
            row.forEachIndexed { i, cell ->
                val colW = colWidths.getOrElse(i) { 0f }
                val cellX = x - colW
                drawCenteredInBox(canvas, cell, cellX, y, colW, rowHeight, bodyPaint)
                canvas.drawRect(cellX, y, x, y + rowHeight, borderPaint)
                x = cellX
            }
            y += rowHeight
        }

        // ===== التوقيعات =====
        drawSignatures(canvas, pageWidth, pageHeight, school)
    }

    /**
     * رسم رأس الخلية مع دعم سطرين بشكل صحيح — بدون تداخل
     */
    private fun drawHeaderCell(
        canvas: Canvas,
        text: String,
        boxLeft: Float,
        boxTop: Float,
        boxWidth: Float,
        boxHeight: Float,
        paint: TextPaint
    ) {
        if (text.isBlank()) return

        // إذا كان النص يحتوي على "/" نفصله إلى سطرين (مثال: "واجب /20" → "واجب" + "/20")
        val parts = text.split("/")
        val lines = if (parts.size == 2) {
            listOf(parts[0].trim(), "/" + parts[1].trim())
        } else {
            listOf(text)
        }

        val linePaint = TextPaint(paint)
        val lineHeight = linePaint.textSize + 2f
        val totalHeight = lineHeight * lines.size
        val startY = boxTop + (boxHeight - totalHeight) / 2f + linePaint.textSize

        lines.forEachIndexed { idx, line ->
            linePaint.textAlign = Paint.Align.CENTER
            canvas.drawText(line, boxLeft + boxWidth / 2f, startY + idx * lineHeight, linePaint)
        }
    }

    private fun drawSignatures(canvas: Canvas, pageWidth: Int, pageHeight: Int, school: SchoolInfo) {
        val sigY = pageHeight - 50f
        val sigPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = FONT_SIGNATURE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val namePaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = FONT_SIGNATURE
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 0.5f
        }

        val third = (pageWidth - 2 * MARGIN) / 3f
        val positions = listOf(
            pageWidth - MARGIN - third / 2f,
            pageWidth / 2f,
            MARGIN + third / 2f
        )

        val labels = listOf("معلم المادة", "مدير المدرسة", "الختم الرسمي")
        val values = listOf(school.teacherName, school.principalName, "")

        positions.forEachIndexed { i, x ->
            canvas.drawLine(x - third * 0.3f, sigY, x + third * 0.3f, sigY, linePaint)
            canvas.drawText(labels[i], x, sigY + 12, sigPaint)
            if (values[i].isNotBlank()) {
                canvas.drawText(values[i], x, sigY + 24, namePaint)
            }
        }
    }

    private fun drawCenteredText(canvas: Canvas, text: String, centerX: Float, baselineY: Float, paint: TextPaint) {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, (centerX * 2).toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setMaxLines(1)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(centerX - layout.width / 2f, baselineY - layout.height)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawCenteredInBox(
        canvas: Canvas,
        text: String,
        boxLeft: Float,
        boxTop: Float,
        boxWidth: Float,
        boxHeight: Float,
        paint: TextPaint
    ) {
        if (text.isBlank()) return
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, boxWidth.toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setMaxLines(1)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        val x = boxLeft + (boxWidth - layout.width) / 2f
        val y = boxTop + (boxHeight - layout.height) / 2f
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}
