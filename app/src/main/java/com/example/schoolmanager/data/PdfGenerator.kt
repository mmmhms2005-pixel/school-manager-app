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

    // ★★★ عدد الشهادات في كل صفحة (يمكن تغييره إلى 4)
    private const val CERTS_PER_PAGE = 4

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
    // التقارير العادية
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

                val pageInfo = PdfDocument.PageInfo.Builder(width.toInt(), height.toInt(), absolutePageNumber).create()
                val page = doc.startPage(pageInfo)

                val isLastPage = chunkIndex == totalPages - 1
                drawReportPage(
                    canvas = page.canvas,
                    pageWidth = width.toInt(),
                    pageHeight = height.toInt(),
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

    val outputStream = FileOutputStream(outFile)
    doc.writeTo(outputStream)
    outputStream.flush()
    outputStream.close()
    doc.close()
    return outFile
}

    // ═══════════════════════════════════════════
    // ★★★ الشهادات الشهرية (جدول أفقي + 3 في الصفحة)
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
        val topMargin = 12f
        val bottomMargin = 12f
        val gap = 6f

        val availableHeight = pageHeight - topMargin - bottomMargin - (CERTS_PER_PAGE - 1) * gap
        val certHeight = availableHeight / CERTS_PER_PAGE
        val certWidth = pageWidth - 2 * MARGIN

        var pageNumber = 1
        var index = 0

        while (index < entries.size) {
            val pageInfo = PdfDocument.PageInfo.Builder(
                pageWidth.toInt(), pageHeight.toInt(), pageNumber
            ).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            var currentY = topMargin

            for (i in 0 until CERTS_PER_PAGE) {
                if (index + i >= entries.size) break

                drawMonthlyCertificate(
                    canvas = canvas,
                    x = MARGIN,
                    y = currentY,
                    width = certWidth,
                    height = certHeight,
                    entry = entries[index + i],
                    school = school,
                    month = month
                )

                currentY += certHeight

                // خط القص المتقطع
                if (i < CERTS_PER_PAGE - 1 && index + i + 1 < entries.size) {
                    val cutY = currentY + gap / 2f
                    val dashPaint = Paint().apply {
                        color = Color.rgb(120, 120, 120)
                        strokeWidth = 0.8f
                        pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
                    }
                    canvas.drawLine(8f, cutY, pageWidth - 8f, cutY, dashPaint)

                    val scissorPaint = TextPaint().apply {
                        color = Color.rgb(120, 120, 120)
                        textSize = 9f
                    }
                    canvas.drawText("✂", 3f, cutY + 3f, scissorPaint)
                    canvas.drawText("✂", pageWidth - 13f, cutY + 3f, scissorPaint)
                }

                currentY += gap
            }

            doc.finishPage(page)
            pageNumber++
            index += CERTS_PER_PAGE
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
            strokeWidth = 1.2f
        }
        canvas.drawRect(x, y, x + width, y + height, borderPaint)

        // إطار داخلي
        val innerBorder = Paint().apply {
            color = Color.rgb(200, 220, 218)
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas.drawRect(x + 3f, y + 3f, x + width - 3f, y + height - 3f, innerBorder)

        var cy = y + 8f

        // ★ الشعار
        if (school.logoBase64.isNotBlank()) {
            try {
                val bytes = Base64.decode(school.logoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val maxLogo = 22f
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val w = if (bitmap.width > bitmap.height) maxLogo else maxLogo * ratio
                    val h = if (bitmap.width > bitmap.height) maxLogo / ratio else maxLogo
                    val lx = x + (width - w) / 2f
                    canvas.drawBitmap(bitmap, null, RectF(lx, cy, lx + w, cy + h), null)
                    cy += h + 2f
                }
            } catch (e: Exception) { }
        }

        // ★ اسم المدرسة
        val schoolPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 10f
            isFakeBoldText = true
        }
        drawCenteredText(canvas, school.schoolName.ifBlank { "اسم المدرسة" }, x + width / 2f, cy + 10f, schoolPaint)
        cy += 12f

        // ★ العنوان + العام (سطر واحد)
        val titleWithYear = if (school.academicYear.isNotBlank()) {
            "شهادة الطالب الشهرية — ${school.academicYear}"
        } else {
            "شهادة الطالب الشهرية"
        }
        val titlePaint = TextPaint().apply {
            color = Color.rgb(15, 118, 110)
            textSize = 10f
            isFakeBoldText = true
        }
        drawCenteredText(canvas, titleWithYear, x + width / 2f, cy + 10f, titlePaint)
        cy += 13f

        // خط فاصل
        canvas.drawLine(x + 12f, cy, x + width - 12f, cy, Paint().apply {
            color = Color.rgb(15, 118, 110)
            strokeWidth = 0.8f
        })
        cy += 4f

        // ★ معلومات الطالب (سطر واحد)
        val sectionPart = if (entry.sectionName.isNotBlank() && entry.sectionName != "—") {
            " / ${entry.sectionName}"
        } else ""

        val infoLine = "👤 ${entry.studentName}   |   🏫 ${entry.className}$sectionPart   |   📅 $month"
        val studentPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 8f
            isFakeBoldText = true
        }
        drawCenteredText(canvas, infoLine, x + width / 2f, cy + 8f, studentPaint)
        cy += 11f

        // ═══ الجدول الأفقي ═══
        val tableLeft = x + 10f
        val tableWidth = width - 20f
        val numCols = entry.grades.size.coerceAtLeast(1)
        val colWidth = tableWidth / numCols

        val rowH = 15f
        val headH = 13f

        // ★ صف العناوين (أسماء المواد)
        var cx = tableLeft + tableWidth
        entry.grades.forEach { g ->
            val cellX = cx - colWidth

            canvas.drawRect(cellX, cy, cx, cy + headH, Paint().apply {
                color = Color.rgb(241, 245, 249)
            })

            val hp = TextPaint().apply {
                color = Color.BLACK
                textSize = 7.5f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val subjectText = if (g.subjectName.length > 10) {
                g.subjectName.substring(0, 9) + "…"
            } else g.subjectName

            canvas.drawText(subjectText, cellX + colWidth / 2f, cy + headH / 2f + 3f, hp)
            canvas.drawRect(cellX, cy, cx, cy + headH, borderPaint)
            cx = cellX
        }
        cy += headH

        // ★ صف الدرجات
        cx = tableLeft + tableWidth
        entry.grades.forEach { g ->
            val cellX = cx - colWidth

            val dp = TextPaint().apply {
                color = Color.rgb(15, 118, 110)
                textSize = 8.5f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val scoreText = "${g.score} من ${g.maxScore}"

            canvas.drawText(scoreText, cellX + colWidth / 2f, cy + rowH / 2f + 3f, dp)
            canvas.drawRect(cellX, cy, cx, cy + rowH, borderPaint)
            cx = cellX
        }
        cy += rowH + 3f

        // ★ الإجماليات (سطر واحد)
        val totalScore = entry.grades.sumOf { it.score }
        val totalMax = entry.grades.sumOf { it.maxScore }
        val avg = if (entry.grades.isNotEmpty()) totalScore.toDouble() / entry.grades.size else 0.0
        val firstMax = entry.grades.firstOrNull()?.maxScore ?: 20
        val overallRating = ratingForPercent((avg / firstMax * 100).toInt())

        val totalsPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 8f
            isFakeBoldText = true
        }

        val totalsText = "المجموع: $totalScore / $totalMax   |   المتوسط: ${String.format("%.2f", avg)} / $firstMax   |   التقدير: $overallRating"
        drawCenteredText(canvas, totalsText, x + width / 2f, cy + 8f, totalsPaint)
        cy += 14f

        // ═══ ★ التوقيعات مرفوعة (فوراً بعد المحتوى) ═══
        val sigY = cy + 6f
        val third = tableWidth / 3f
        val positions = listOf(
            x + width - 10f - third / 2f,
            x + width / 2f,
            x + 10f + third / 2f
        )

        val labels = listOf("معلم المادة", "مربي الصف", "مدير المدرسة")
        val sigPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 7f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val sigLinePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 0.5f
        }

        positions.forEachIndexed { i, px ->
            canvas.drawLine(px - third * 0.35f, sigY, px + third * 0.35f, sigY, sigLinePaint)
            canvas.drawText(labels[i], px, sigY + 8f, sigPaint)
        }
        // ★ المساحة المتبقية أسفل الشهادة فارغة (للسماح بـ 3 أو 4 شهادات)
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
    // دالة التقارير العادية (بقيت كما هي)
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
    school: SchoolInfo, isMultiSubject: Boolean // يمكنك إبقاء هذا المتغير حتى لو لم نستخدمه
) {
    val signLineY = pageHeight - 75f

    val labelPaint = TextPaint().apply {
        color = Color.BLACK; textSize = 10f
        textAlign = Paint.Align.CENTER
    }
    val signHintPaint = TextPaint().apply {
        color = Color.DKGRAY; textSize = 8f
        textAlign = Paint.Align.RIGHT
    }
    val namePaint = TextPaint().apply {
        color = Color.rgb(30, 30, 30); textSize = 9f
        textAlign = Paint.Align.CENTER
    }
    val linePaint = Paint().apply {
        color = Color.BLACK; strokeWidth = 1f
    }

    val third = (pageWidth - 2 * MARGIN) / 3f
    val positions = listOf(
        pageWidth - MARGIN - third / 2f, // يمين (مدير المدرسة)
        MARGIN + third / 2f              // يسار (مربي الصف)
    )

    // ★★★ التعديل: تثبيت التسميات لطلبك ★★★
    val labels = listOf("مدير المدرسة", "مربي الصف")
    
    // ★★★ التعديل: جلب الأسماء من SchoolInfo ★★★
    val values = listOf(
        school.principalName,  // اسم المدير
        school.homeroomTeacherName // اسم المربي
    )

    positions.forEachIndexed { i, x ->
        canvas.drawLine(x - third * 0.35f, signLineY, x + third * 0.35f, signLineY, linePaint)
        canvas.drawText(labels[i], x, signLineY + 12f, labelPaint)

        val signHintX = x + third * 0.35f
        canvas.drawText("التوقيع/", signHintX, signLineY + 12f, signHintPaint)

        if (values[i].isNotBlank()) {
            canvas.drawText(values[i], x, signLineY + 25f, namePaint)
        } else {
            canvas.drawText("____________________", x, signLineY + 25f, emptyLinePaint)
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
