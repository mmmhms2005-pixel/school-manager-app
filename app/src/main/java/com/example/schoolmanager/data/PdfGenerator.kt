package com.example.schoolmanager.data

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
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

    private const val FONT_SCHOOL = 17f
    private const val FONT_YEAR = 10f
    private const val FONT_TITLE = 13f
    private const val FONT_META = 12f
    private const val FONT_TABLE_HEADER = 9f
    private const val FONT_TABLE_BODY_V = 8.5f
    private const val FONT_TABLE_BODY_L = 7.5f
    private const val FONT_SIGNATURE = 10f
    private const val FONT_PAGE_NUM = 9f

    private const val VERTICAL_ROWS_PER_PAGE = 35
    private const val LANDSCAPE_ROWS_PER_PAGE = 30

    data class SchoolInfo(
        val schoolName: String = "",
        val academicYear: String = "",
        val principalName: String = "",
        val teacherName: String = "",
        val homeroomTeacherName: String = "",
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
        val redColumnIndices: Set<Int> = emptySet(),
        val isMultiSubject: Boolean = false
    )

    // ★ بيانات الشهادة الشهرية
    data class SubjectGrade(
        val subjectName: String,
        val score: Int,
        val maxScore: Int,
        val rating: String
    )

    data class MonthlyCertEntry(
        val studentNumber: String,
        val studentName: String,
        val className: String,
        val sectionName: String,
        val grades: List<SubjectGrade>
    )

    // ═══════════════════════════════════════════
    // الدالة الأصلية (التقارير العادية)
    // ═══════════════════════════════════════════

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
            val chunks = if (report.rows.isEmpty()) listOf(emptyList())
                         else report.rows.chunked(rowsPerPage)
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

    // ═══════════════════════════════════════════
    // ★ دالة توليد الشهادات الشهرية (A5، شهادتان لكل A4)
    // ═══════════════════════════════════════════

    fun generateMonthlyCertificates(
        context: Context,
        entries: List<MonthlyCertEntry>,
        school: SchoolInfo,
        month: String,
        fileName: String
    ): File {
        val doc = PdfDocument()
        val pageWidth = A4_WIDTH_PT
        val pageHeight = A4_HEIGHT_PT
        val certWidth = pageWidth - 2 * MARGIN
        val certHeight = (pageHeight - 3 * MARGIN) / 2f

        var pageNumber = 1
        var index = 0

        while (index < entries.size) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            // ★ الشهادة الأولى (في الأعلى)
            val topY = MARGIN
            drawMonthlyCertificate(
                canvas = canvas,
                x = MARGIN,
                y = topY,
                width = certWidth,
                height = certHeight,
                entry = entries[index],
                school = school,
                month = month
            )

            // ★ خط القص في المنتصف (متقطع)
            val cutY = topY + certHeight + MARGIN / 2f
            val dashPaint = Paint().apply {
                color = Color.rgb(120, 120, 120)
                strokeWidth = 0.8f
                pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
            }
            canvas.drawLine(10f, cutY, pageWidth - 10f, cutY, dashPaint)

            // رمز المقص في الطرف
            val scissorPaint = TextPaint().apply {
                color = Color.rgb(120, 120, 120)
                textSize = 10f
            }
            canvas.drawText("✂", 4f, cutY + 3f, scissorPaint)
            canvas.drawText("✂", pageWidth - 14f, cutY + 3f, scissorPaint)

            // ★ الشهادة الثانية (في الأسفل) — إن وُجدت
            if (index + 1 < entries.size) {
                val bottomY = cutY + MARGIN / 2f
                drawMonthlyCertificate(
                    canvas = canvas,
                    x = MARGIN,
                    y = bottomY,
                    width = certWidth,
                    height = certHeight,
                    entry = entries[index + 1],
                    school = school,
                    month = month
                )
            }

            doc.finishPage(page)
            pageNumber++
            index += 2
        }

        val outDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(outDir, fileName)
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    private fun drawMonthlyCertificate(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        entry: MonthlyCertEntry,
        school: SchoolInfo,
        month: String
    ) {
        // ★ إطار الشهادة
        val borderPaint = Paint().apply {
            color = Color.rgb(15, 118, 110)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRect(x, y, x + width, y + height, borderPaint)

        // ★ إطار داخلي خفيف
        val innerBorder = Paint().apply {
            color = Color.rgb(200, 220, 218)
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas.drawRect(x + 4f, y + 4f, x + width - 4f, y + height - 4f, innerBorder)

        var cy = y + 10f

        // ★ الشعار (مصغّر)
        if (school.logoBase64.isNotBlank()) {
            try {
                val bytes = Base64.decode(school.logoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val maxLogo = 26f
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val w = if (bitmap.width > bitmap.height) maxLogo else maxLogo * ratio
                    val h = if (bitmap.width > bitmap.height) maxLogo / ratio else maxLogo
                    val lx = x + (width - w) / 2f
                    canvas.drawBitmap(bitmap, null, RectF(lx, cy, lx + w, cy + h), null)
                    cy += h + 3f
                }
            } catch (e: Exception) { }
        }

        // ★ اسم المدرسة
        val schoolPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }
        drawCenteredText(canvas, school.schoolName.ifBlank { "اسم المدرسة" }, x + width / 2f, cy + 11f, schoolPaint)
        cy += 13f

        // ★ العام الدراسي
        if (school.academicYear.isNotBlank()) {
            val yearPaint = TextPaint().apply {
                color = Color.DKGRAY
                textSize = 8f
            }
            drawCenteredText(canvas, school.academicYear, x + width / 2f, cy + 8f, yearPaint)
            cy += 10f
        }

        // ★ العنوان
        val titlePaint = TextPaint().apply {
            color = Color.rgb(15, 118, 110)
            textSize = 12f
            isFakeBoldText = true
        }
        drawCenteredText(canvas, "شهادة الطالب الشهرية", x + width / 2f, cy + 12f, titlePaint)
        cy += 16f

        // ★ خط فاصل
        canvas.drawLine(x + 15f, cy, x + width - 15f, cy, Paint().apply {
            color = Color.rgb(15, 118, 110); strokeWidth = 1f
        })
        cy += 6f

        // ★ معلومات الطالب
        val infoPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 9f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        val sectionPart = if (entry.sectionName.isNotBlank() && entry.sectionName != "—") {
            "  |  الشعبة: ${entry.sectionName}"
        } else ""

        canvas.drawText("👤 ${entry.studentName}", x + width - 12f, cy + 9f, infoPaint)
        cy += 12f
        canvas.drawText("🏫 ${entry.className}$sectionPart  |  📅 $month", x + width - 12f, cy + 9f, infoPaint)
        cy += 14f

        // ★ الجدول
        val tableLeft = x + 12f
        val tableWidth = width - 24f
        val cols = listOf(0.45f, 0.13f, 0.13f, 0.29f).map { it * tableWidth }
        val headers = listOf("المادة", "الدرجة", "من", "التقدير")

        val rowH = 11f
        val headH = 13f

        // رأس الجدول
        canvas.drawRect(tableLeft, cy, tableLeft + tableWidth, cy + headH, Paint().apply {
            color = Color.rgb(241, 245, 249)
        })

        var cx = tableLeft + tableWidth
        headers.forEachIndexed { i, h ->
            val colW = cols[i]
            val cellX = cx - colW
            val hp = TextPaint().apply {
                color = Color.BLACK; textSize = 8f; isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(h, cellX + colW / 2f, cy + headH / 2f + 3f, hp)
            canvas.drawRect(cellX, cy, cx, cy + headH, borderPaint)
            cx = cellX
        }
        cy += headH

        // صفوف البيانات
        val bodyPaint = TextPaint().apply {
            color = Color.BLACK; textSize = 8f; textAlign = Paint.Align.CENTER
        }
        val bodyBoldPaint = TextPaint().apply {
            color = Color.rgb(15, 118, 110); textSize = 8f
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }

        entry.grades.forEach { g ->
            cx = tableLeft + tableWidth
            val vals = listOf(g.subjectName, "${g.score}", "${g.maxScore}", g.rating)
            vals.forEachIndexed { i, v ->
                val colW = cols[i]
                val cellX = cx - colW
                val p = if (i == 3) bodyBoldPaint else bodyPaint
                canvas.drawText(v, cellX + colW / 2f, cy + rowH / 2f + 3f, p)
                canvas.drawRect(cellX, cy, cx, cy + rowH, borderPaint)
                cx = cellX
            }
            cy += rowH
        }

        cy += 5f

        // ★ الإجماليات
        val totalScore = entry.grades.sumOf { it.score }
        val totalMax = entry.grades.sumOf { it.maxScore }
        val avg = if (entry.grades.isNotEmpty()) totalScore.toDouble() / entry.grades.size else 0.0
        val firstMax = entry.grades.firstOrNull()?.maxScore ?: 20
        val overallRating = ratingForPercent((avg / firstMax * 100).toInt())

        val totalPaint = TextPaint().apply {
            color = Color.BLACK; textSize = 9f; isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("المجموع: $totalScore / $totalMax", x + width - 12f, cy + 9f, totalPaint)
        cy += 11f
        canvas.drawText("المتوسط: ${String.format("%.2f", avg)} / $firstMax", x + width - 12f, cy + 9f, totalPaint)
        cy += 11f

        val ratingPaint = TextPaint().apply {
            color = Color.rgb(15, 118, 110); textSize = 10f; isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("التقدير العام: $overallRating", x + width - 12f, cy + 10f, ratingPaint)
        cy += 14f

        // ★ التوقيعات (إن سمحت المساحة)
        if (cy + 28f < y + height - 5f) {
            val sigY = y + height - 16f
            val third = tableWidth / 3f
            val positions = listOf(
                x + width - 12f - third / 2f,
                x + width / 2f,
                x + 12f + third / 2f
            )
            val labels = listOf("معلم المادة", "مربي الصف", "مدير المدرسة")
            val sigPaint = TextPaint().apply {
                color = Color.DKGRAY; textSize = 7f
                textAlign = Paint.Align.CENTER; isFakeBoldText = true
            }
            val sigLinePaint = Paint().apply {
                color = Color.DKGRAY; strokeWidth = 0.5f
            }
            positions.forEachIndexed { i, px ->
                canvas.drawLine(px - third * 0.38f, sigY, px + third * 0.38f, sigY, sigLinePaint)
                canvas.drawText(labels[i], px, sigY + 8f, sigPaint)
            }
        }
    }

    private fun ratingForPercent(percent: Int): String {
        return when {
            percent >= 90 -> "ممتاز"
            percent >= 80 -> "جيد جداً"
            percent >= 70 -> "جيد"
            percent >= 60 -> "مقبول"
            percent >= 50 -> "ضعيف"
            else -> "راسب"
        }
    }

    // ═══════════════════════════════════════════
    // دوال التقارير العادية
    // ═══════════════════════════════════════════

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

        // الشعار
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
                        logoW = maxLogoSize; logoH = maxLogoSize / ratio
                    } else {
                        logoW = maxLogoSize * ratio; logoH = maxLogoSize
                    }
                    val left = centerX - logoW / 2f
                    canvas.drawBitmap(bitmap, null, RectF(left, y, left + logoW, y + logoH), null)
                    y += logoH + 10f
                }
            } catch (e: Exception) { }
        }

        val schoolPaint = TextPaint().apply {
            color = Color.BLACK; textSize = FONT_SCHOOL; isFakeBoldText = true
        }
        drawCenteredText(canvas, school.schoolName.ifBlank { "اسم المدرسة" }, centerX, y + FONT_SCHOOL, schoolPaint)
        y += FONT_SCHOOL + 5f

        if (school.academicYear.isNotBlank()) {
            val yearPaint = TextPaint().apply {
                color = Color.DKGRAY; textSize = FONT_YEAR; isFakeBoldText = true
            }
            drawCenteredText(canvas, school.academicYear, centerX, y + FONT_YEAR, yearPaint)
            y += FONT_YEAR + 6f
        }

        canvas.drawLine(MARGIN, y, pageWidth - MARGIN, y, Paint().apply {
            color = Color.rgb(15, 118, 110); strokeWidth = 1.5f
        })
        y += 12

        val titlePaint = TextPaint().apply {
            color = Color.BLACK; textSize = FONT_TITLE
            textAlign = Paint.Align.RIGHT; isFakeBoldText = true
        }
        canvas.drawText(report.title, pageWidth - MARGIN, y + FONT_TITLE, titlePaint)

        val metaPaint = TextPaint().apply {
            color = Color.rgb(30, 30, 30); textSize = FONT_META
            textAlign = Paint.Align.LEFT; isFakeBoldText = true
        }
        canvas.drawText(report.meta, MARGIN, y + FONT_META, metaPaint)

        y += FONT_TITLE + 14

        val tableWidth = pageWidth - 2 * MARGIN
        val colWidths = report.columns.map { it.width * tableWidth }
        val headerHeight = 25f

        val reservedBottom = if (isLastPage) 90f else 30f
        val availableForRows = pageHeight - y - headerHeight - MARGIN - reservedBottom
        val rowsCount = rowsChunk.size.coerceAtLeast(1)
        val rowHeight = (availableForRows / rowsCount).coerceIn(9f, 20f)

        val headerPaint = TextPaint().apply {
            color = Color.BLACK; textSize = FONT_TABLE_HEADER
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        val cellBgPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
        val borderPaint = Paint().apply {
            color = Color.rgb(180, 180, 180)
            style = Paint.Style.STROKE; strokeWidth = 0.5f
        }

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

        val bodyFontSize = if (report.isLandscape) FONT_TABLE_BODY_L else FONT_TABLE_BODY_V
        val bodyPaint = TextPaint().apply {
            color = Color.BLACK; textSize = bodyFontSize
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        val redBodyPaint = TextPaint().apply {
            color = Color.rgb(180, 30, 30); textSize = bodyFontSize
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
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

        if (isLastPage) {
            drawSignatures(canvas, pageWidth, pageHeight, school, report.isMultiSubject)
        }

        if (totalPages > 1) {
            val pageNumPaint = TextPaint().apply {
                color = Color.DKGRAY; textSize = FONT_PAGE_NUM
                textAlign = Paint.Align.CENTER; isFakeBoldText = true
            }
            canvas.drawText("صفحة $pageNumber من $totalPages", centerX, pageHeight - 18f, pageNumPaint)
        }
    }

    private fun drawHeaderCell(
        canvas: Canvas, text: String,
        boxLeft: Float, boxTop: Float, boxWidth: Float, boxHeight: Float,
        paint: TextPaint
    ) {
        if (text.isBlank()) return
        val linePaint = TextPaint(paint)
        val lineHeight = linePaint.textSize + 2f
        val startY = boxTop + (boxHeight - lineHeight) / 2f + linePaint.textSize
        linePaint.textAlign = Paint.Align.CENTER
        linePaint.isFakeBoldText = true
        canvas.drawText(text, boxLeft + boxWidth / 2f, startY, linePaint)
    }

    private fun drawSignatures(
        canvas: Canvas, pageWidth: Int, pageHeight: Int,
        school: SchoolInfo, isMultiSubject: Boolean
    ) {
        val signLineY = pageHeight - 75f

        val labelPaint = TextPaint().apply {
            color = Color.BLACK; textSize = FONT_SIGNATURE
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        val signHintPaint = TextPaint().apply {
            color = Color.DKGRAY; textSize = 9f
            textAlign = Paint.Align.RIGHT; isFakeBoldText = true
        }
        val namePaint = TextPaint().apply {
            color = Color.rgb(30, 30, 30); textSize = FONT_SIGNATURE
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        val emptyLinePaint = TextPaint().apply {
            color = Color.rgb(130, 130, 130); textSize = 10f
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        val linePaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 0.7f }

        val third = (pageWidth - 2 * MARGIN) / 3f
        val positions = listOf(
            pageWidth - MARGIN - third / 2f,
            pageWidth / 2f,
            MARGIN + third / 2f
        )

        val firstLabel = if (isMultiSubject) "مربي الصف" else "معلم المادة"
        val firstValue = if (isMultiSubject) school.homeroomTeacherName else school.teacherName

        val labels = listOf(firstLabel, "مدير المدرسة", "الختم الرسمي")
        val values = listOf(firstValue, school.principalName, "")

        positions.forEachIndexed { i, x ->
            canvas.drawLine(x - third * 0.32f, signLineY, x + third * 0.32f, signLineY, linePaint)
            canvas.drawText(labels[i], x, signLineY - 8, labelPaint)

            val signHintX = x + third * 0.18f
            canvas.drawText("التوقيع/", signHintX, signLineY + 16, signHintPaint)

            if (values[i].isNotBlank()) {
                canvas.drawText(values[i], x, signLineY + 32, namePaint)
            } else {
                canvas.drawText("_______________", x, signLineY + 32, emptyLinePaint)
            }
        }
    }

    private fun drawCenteredText(
        canvas: Canvas, text: String,
        centerX: Float, baselineY: Float, paint: TextPaint
    ) {
        val width = paint.measureText(text)
        val oldAlign = paint.textAlign
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(text, centerX - width / 2f, baselineY, paint)
        paint.textAlign = oldAlign
    }

    private fun drawCenteredInBox(
        canvas: Canvas, text: String,
        boxLeft: Float, boxTop: Float, boxWidth: Float, boxHeight: Float,
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
