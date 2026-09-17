package com.example.schoolmanager.data

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    private const val A4_WIDTH_PT = 595
    private const val A4_HEIGHT_PT = 842
    private const val A4_WIDTH_LS_PT = 842
    private const val A4_HEIGHT_LS_PT = 595

    private const val MARGIN = 25f

    // الخطوط
    private const val FONT_SCHOOL = 17f
    private const val FONT_YEAR = 10f
    private const val FONT_TITLE = 13f
    private const val FONT_META = 12f
    private const val FONT_TABLE_HEADER = 9f
    private const val FONT_TABLE_BODY_V = 8.5f  // عمودي
    private const val FONT_TABLE_BODY_L = 7.5f  // أفقي
    private const val FONT_SIGNATURE = 10f
    private const val FONT_PAGE_NUM = 9f

    // حدود الصفوف لكل صفحة
    private const val VERTICAL_ROWS_PER_PAGE = 35
    private const val LANDSCAPE_ROWS_PER_PAGE = 30

    data class SchoolInfo(
        val schoolName: String = "",
        val academicYear: String = "",
        val principalName: String = "",
        val teacherName: String = "",
        val logoBase64: String = ""
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
        val isLandscape: Boolean = false,
        val redColumnIndices: Set<Int> = emptySet()
    )

    fun generate(
        context: Context,
        reports: List<ReportData>,
        school: SchoolInfo,
        fileName: String
    ): File {
        val doc = PdfDocument()
        var absolutePageNumber = 1

        reports.forEach { report ->
            val rowsPerPage = if (report.isLandscape) LANDSCAPE_ROWS_PER_PAGE else VERTICAL_ROWS_PER_PAGE
            val chunks = if (report.rows.isEmpty()) {
                listOf(emptyList())
            } else {
                report.rows.chunked(rowsPerPage)
            }
            val totalPages = chunks.size

            chunks.forEachIndexed { chunkIndex, chunk ->
                val width = if (report.isLandscape) A4_WIDTH_LS_PT else A4_WIDTH_PT
                val height = if (report.isLandscape) A4_HEIGHT_LS_PT else A4_HEIGHT_PT

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, absolutePageNumber).create()
                val page = doc.startPage(pageInfo)

                val isLastPage = chunkIndex == totalPages - 1
                drawReportPage(
                    canvas = page.canvas,
                    pageWidth = width,
                    pageHeight = height,
                    report = report,
                    school = school,
                    rowsChunk = chunk,
                    pageNumber = chunkIndex + 1,
                    totalPages = totalPages,
                    isLastPage = isLastPage
                )

                doc.finishPage(page)
                absolutePageNumber++
            }
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
        school: SchoolInfo,
        rowsChunk: List<List<String>>,
        pageNumber: Int,
        totalPages: Int,
        isLastPage: Boolean
    ) {
        canvas.drawColor(Color.WHITE)
        var y = MARGIN + 5f
        val centerX = pageWidth / 2f

        // ===== الشعار =====
        if (school.logoBase64.isNotBlank()) {
            try {
                val bytes = Base64.decode(school.logoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val maxLogoSize = 55f
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val logoW: Float
                    val logoH: Float
                    if (bitmap.width > bitmap.height) {
                        logoW = maxLogoSize
                        logoH = maxLogoSize / ratio
                    } else {
                        logoW = maxLogoSize * ratio
                        logoH = maxLogoSize
                    }
                    val left = centerX - logoW / 2f
                    val destRect = RectF(left, y, left + logoW, y + logoH)
                    canvas.drawBitmap(bitmap, null, destRect, null)
                    y += logoH + 10f
                }
            } catch (e: Exception) {
                // تجاهل
            }
        }

        // ===== اسم المدرسة =====
        val schoolPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = FONT_SCHOOL
            isFakeBoldText = true
        }
        drawCenteredText(canvas, school.schoolName.ifBlank { "اسم المدرسة" }, centerX, y + FONT_SCHOOL, schoolPaint)
        y += FONT_SCHOOL + 5f

        // ===== العام الدراسي =====
        if (school.academicYear.isNotBlank()) {
            val yearPaint = TextPaint().apply {
                color = Color.DKGRAY
                textSize = FONT_YEAR
            }
            drawCenteredText(canvas, school.academicYear, centerX, y + FONT_YEAR, yearPaint)
            y += FONT_YEAR + 6f
        }

        // خط فاصل
        val linePaint = Paint().apply {
            color = Color.rgb(15, 118, 110)
            strokeWidth = 1.5f
        }
        canvas.drawLine(MARGIN, y, pageWidth - MARGIN, y, linePaint)
        y += 12

        // ===== عنوان التقرير + المعلومات =====
        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = FONT_TITLE
            textAlign = Paint.Align.RIGHT
            isFakeBoldText = true
        }
        canvas.drawText(report.title, pageWidth - MARGIN, y + FONT_TITLE, titlePaint)

        val metaPaint = TextPaint().apply {
            color = Color.rgb(50, 50, 50)
            textSize = FONT_META
            textAlign = Paint.Align.LEFT
            isFakeBoldText = true
        }
        canvas.drawText(report.meta, MARGIN, y + FONT_META, metaPaint)

        y += FONT_TITLE + 14

        // ===== الجدول =====
        val tableWidth = pageWidth - 2 * MARGIN
        val colWidths = report.columns.map { it.width * tableWidth }

        val headerHeight = 25f

        // المساحة المتبقية لحساب ارتفاع الصف
        val reservedBottom = if (isLastPage) 90f else 30f  // توقيعات + رقم الصفحة
        val availableForRows = pageHeight - y - headerHeight - MARGIN - reservedBottom
        val rowsCount = rowsChunk.size.coerceAtLeast(1)
        val rowHeight = (availableForRows / rowsCount).coerceIn(9f, 20f)

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

        // رأس الجدول
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

        // صفوف البيانات
        val bodyFontSize = if (report.isLandscape) FONT_TABLE_BODY_L else FONT_TABLE_BODY_V
        val bodyPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = bodyFontSize
            textAlign = Paint.Align.CENTER
        }
        val redBodyPaint = TextPaint().apply {
            color = Color.rgb(180, 30, 30)
            textSize = bodyFontSize
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        rowsChunk.forEach { row ->
            x = pageWidth - MARGIN
            row.forEachIndexed { i, cell ->
                val colW = colWidths.getOrElse(i) { 0f }
                val cellX = x - colW
                val paint = if (i in report.redColumnIndices) redBodyPaint else bodyPaint
                drawCenteredInBox(canvas, cell, cellX, y, colW, rowHeight, paint)
                canvas.drawRect(cellX, y, x, y + rowHeight, borderPaint)
                x = cellX
            }
            y += rowHeight
        }

        // ===== التوقيعات (فقط في الصفحة الأخيرة) =====
        if (isLastPage) {
            drawSignatures(canvas, pageWidth, pageHeight, school)
        }

        // ===== رقم الصفحة (فقط إذا كان هناك أكثر من صفحة) =====
        if (totalPages > 1) {
            val pageNumPaint = TextPaint().apply {
                color = Color.DKGRAY
                textSize = FONT_PAGE_NUM
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawText(
                "صفحة $pageNumber من $totalPages",
                centerX,
                pageHeight - 18f,
                pageNumPaint
            )
        }
    }

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
        val linePaint = TextPaint(paint)
        val lineHeight = linePaint.textSize + 2f
        val startY = boxTop + (boxHeight - lineHeight) / 2f + linePaint.textSize
        linePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, boxLeft + boxWidth / 2f, startY, linePaint)
    }

    private fun drawSignatures(canvas: Canvas, pageWidth: Int, pageHeight: Int, school: SchoolInfo) {
        val signLineY = pageHeight - 75f

        val labelPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = FONT_SIGNATURE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val signHintPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 9f
            textAlign = Paint.Align.RIGHT
            isFakeBoldText = true
        }
        val namePaint = TextPaint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = FONT_SIGNATURE
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 0.7f
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
            canvas.drawLine(x - third * 0.32f, signLineY, x + third * 0.32f, signLineY, linePaint)
            canvas.drawText(labels[i], x, signLineY - 8, labelPaint)

            val signHintX = x + third * 0.18f
            canvas.drawText("التوقيع/", signHintX, signLineY + 16, signHintPaint)

            if (values[i].isNotBlank()) {
                canvas.drawText(values[i], x, signLineY + 32, namePaint)
            }
        }
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        paint: TextPaint
    ) {
        val width = paint.measureText(text)
        val oldAlign = paint.textAlign
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(text, centerX - width / 2f, baselineY, paint)
        paint.textAlign = oldAlign
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
        val oldAlign = paint.textAlign
        paint.textAlign = Paint.Align.CENTER
        val baselineY = boxTop + boxHeight / 2f + paint.textSize / 3f
        canvas.drawText(text, boxLeft + boxWidth / 2f, baselineY, paint)
        paint.textAlign = oldAlign
    }
}
