package com.example.schoolmanager.data

import android.content.Context
import android.graphics.Bitmap
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
import kotlin.math.ceil

object PdfGenerator {

    // ═══════════════════════════════════════════
    // أبعاد A4
    // ═══════════════════════════════════════════

    private const val A4_WIDTH_PT = 595
    private const val A4_HEIGHT_PT = 842

    private const val A4_WIDTH_LS_PT = 842
    private const val A4_HEIGHT_LS_PT = 595

    // أبعاد A5
private const val A5_WIDTH_PT = 595
private const val A5_HEIGHT_PT = 420
    private const val MARGIN = 25f

    // ═══════════════════════════════════════════
    // إعدادات الطباعة الآمنة
    // ═══════════════════════════════════════════

    /**
     * عند true:
     * يتم رسم الصفحة كاملة على Bitmap ثم إدخالها
     * كصورة واحدة داخل ملف PDF.
     *
     * هذا يحسن توافق الملف مع الطابعات التي لا
     * تتعامل جيداً مع PDF الذي يحتوي على عدد كبير
     * من عناصر Canvas المتجهة.
     */
    private const val PRINT_SAFE_MODE = true

    /**
     * الدقة الأساسية.
     */
    private const val PRINT_DPI = 300

    /**
     * الدقة الاحتياطية في حالة نقص الذاكرة.
     */
    private const val FALLBACK_PRINT_DPI = 200

    /**
     * PDF يستخدم 72 نقطة لكل بوصة.
     */
    private const val PDF_BASE_DPI = 72f

    // ═══════════════════════════════════════════
    // أحجام الخطوط
    // ═══════════════════════════════════════════

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

    // عدد الشهادات في الصفحة
    private const val CERTS_PER_PAGE = 4

    // ═══════════════════════════════════════════
    // البيانات
    // ═══════════════════════════════════════════

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
    val isA5: Boolean = false,
    val redColumnIndices: Set<Int> = emptySet(),
    val isMultiSubject: Boolean = false,
    val photoBase64: String = ""
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
    val grades: List<SubjectGrade>,
    val photoBase64: String = ""
)

    // ═══════════════════════════════════════════
    // أدوات الطباعة الآمنة
    // ═══════════════════════════════════════════

    /**
     * إنشاء Bitmap للصفحة بالحجم الصحيح.
     *
     * يعمل مع:
     *
     * Portrait:
     * 595 × 842
     *
     * Landscape:
     * 842 × 595
     *
     * ولا يهم اتجاه الصفحة، لأن الدالة تستقبل
     * العرض والارتفاع الفعليين.
     */
    private fun createRasterPage(
        pageWidthPt: Int,
        pageHeightPt: Int,
        dpi: Int
    ): Pair<Bitmap, Canvas> {

        val scale = dpi / PDF_BASE_DPI

        val bitmapWidth = ceil(
            pageWidthPt * scale
        ).toInt()

        val bitmapHeight = ceil(
            pageHeightPt * scale
        ).toInt()

        val bitmap = Bitmap.createBitmap(
            bitmapWidth,
            bitmapHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        // خلفية بيضاء
        canvas.drawColor(Color.WHITE)

        /*
         * جميع دوال الرسم الحالية تعمل بالنقاط
         * الخاصة بصفحة PDF.
         *
         * لذلك نقوم بتكبير Canvas فقط.
         *
         * مثال:
         * 595 نقطة × 300/72 ≈ 2479 بكسل
         *
         * وهذا يحافظ على نفس التصميم.
         */
        canvas.scale(scale, scale)

        return Pair(bitmap, canvas)
    }

    /**
     * رسم الصفحة داخل Bitmap.
     *
     * يبدأ بـ300 DPI.
     *
     * إذا حدث OutOfMemoryError:
     * يعيد المحاولة بـ200 DPI.
     */
    private fun renderPageSafely(
        pageWidthPt: Int,
        pageHeightPt: Int,
        drawContent: (Canvas) -> Unit
    ): Bitmap {

        try {

            val (bitmap, canvas) = createRasterPage(
                pageWidthPt = pageWidthPt,
                pageHeightPt = pageHeightPt,
                dpi = PRINT_DPI
            )

            try {

                drawContent(canvas)

                return bitmap

            } catch (error: Throwable) {

                bitmap.recycle()
                throw error
            }

        } catch (error: OutOfMemoryError) {

            /*
             * إذا لم تكفِ الذاكرة لـ300 DPI،
             * نستخدم 200 DPI تلقائياً.
             */
            System.gc()

            val (bitmap, canvas) = createRasterPage(
                pageWidthPt = pageWidthPt,
                pageHeightPt = pageHeightPt,
                dpi = FALLBACK_PRINT_DPI
            )

            try {

                drawContent(canvas)

                return bitmap

            } catch (secondError: Throwable) {

                bitmap.recycle()
                throw secondError
            }
        }
    }

    /**
     * وضع الـBitmap داخل صفحة PDF.
     *
     * يتم تمديد الصورة إلى كامل أبعاد الصفحة
     * دون تغيير الاتجاه.
     */
    private fun drawRasterOnPdfPage(
        pageCanvas: Canvas,
        bitmap: Bitmap,
        pageWidthPt: Int,
        pageHeightPt: Int
    ) {

        val paint = Paint(
            Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG or
                    Paint.DITHER_FLAG
        )

        val destination = RectF(
            0f,
            0f,
            pageWidthPt.toFloat(),
            pageHeightPt.toFloat()
        )

        pageCanvas.drawBitmap(
            bitmap,
            null,
            destination,
            paint
        )
    }

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
        try {

        
        var absolutePageNumber = 1



            reports.forEach { report ->

                val rowsPerPage =
                    if (report.isLandscape) {
                        LANDSCAPE_ROWS_PER_PAGE
                    } else {
                        VERTICAL_ROWS_PER_PAGE
                    }

                val chunks =
                    if (report.rows.isEmpty()) {
                        listOf(emptyList())
                    } else {
                        report.rows.chunked(rowsPerPage)
                    }

                val totalPages = chunks.size

                chunks.forEachIndexed { chunkIndex, chunk ->

                    /*
                     * تحديد أبعاد الصفحة بناءً على
                     * اتجاه التقرير.
                     *
                     * العمودي:
                     * 595 × 842
                     *
                     * الأفقي:
                     * 842 × 595
                     */
                    val pageWidth =
    if (report.isA5) {
        A5_WIDTH_PT
    } else if (report.isLandscape) {
        A4_WIDTH_LS_PT
    } else {
        A4_WIDTH_PT
    }

                    val pageHeight =
    if (report.isA5) {
        A5_HEIGHT_PT
    } else if (report.isLandscape) {
        A4_HEIGHT_LS_PT
    } else {
        A4_HEIGHT_PT
    }

                    val pageInfo =
                        PdfDocument.PageInfo.Builder(
                            pageWidth,
                            pageHeight,
                            absolutePageNumber
                        ).create()

                    val page = doc.startPage(pageInfo)

                    val isLastPage =
                        chunkIndex == totalPages - 1

                    if (PRINT_SAFE_MODE) {

                        /*
                         * إنشاء صورة كاملة للصفحة.
                         *
                         * لاحظ أن pageWidth/pageHeight
                         * هما نفس أبعاد الصفحة الفعلية،
                         * سواء عمودية أو أفقية.
                         */
                        val bitmap = renderPageSafely(
                            pageWidthPt = pageWidth,
                            pageHeightPt = pageHeight
                        ) { rasterCanvas ->

                            drawReportPage(
                                canvas = rasterCanvas,
                                pageWidth = pageWidth,
                                pageHeight = pageHeight,
                                report = report,
                                school = school,
                                rowsChunk = chunk,
                                pageNumber = chunkIndex + 1,
                                totalPages = totalPages,
                                isLastPage = isLastPage
                            )
                        }

                        try {

                            /*
                             * إدخال الصورة كعنصر واحد داخل PDF.
                             */
                            drawRasterOnPdfPage(
                                pageCanvas = page.canvas,
                                bitmap = bitmap,
                                pageWidthPt = pageWidth,
                                pageHeightPt = pageHeight
                            )

                            doc.finishPage(page)

                        } finally {

                            /*
                             * تحرير الذاكرة بعد إنهاء الصفحة.
                             */
                            bitmap.recycle()
                        }

                    } else {

                        /*
                         * الوضع القديم في حال تم تعطيل
                         * PRINT_SAFE_MODE.
                         */
                        drawReportPage(
                            canvas = page.canvas,
                            pageWidth = pageWidth,
                            pageHeight = pageHeight,
                            report = report,
                            school = school,
                            rowsChunk = chunk,
                            pageNumber = chunkIndex + 1,
                            totalPages = totalPages,
                            isLastPage = isLastPage
                        )

                        doc.finishPage(page)
                    }

                    absolutePageNumber++
                }
            }

            val outDir =
                File(context.cacheDir, "reports").apply {
                    mkdirs()
                }

            val outFile = File(outDir, fileName)

            FileOutputStream(outFile).use { outputStream ->
                doc.writeTo(outputStream)
                outputStream.flush()
            }

            return outFile

     } catch (e: Exception) {
    // حفظ الخطأ في ملف نصي
    try {
        val errorFile = java.io.File(context.getExternalFilesDir(null), "error_log.txt")
        errorFile.appendText("\n--- خطأ في التوليد: ---\n")
        errorFile.appendText(e.message ?: "خطأ غير معروف")
        errorFile.appendText("\n${e.stackTraceToString()}\n")
    } catch (ignored: Exception) {}
    throw e
} finally {
    doc.close()
}
    }

    // ═══════════════════════════════════════════
    // الشهادات الشهرية
    // ═══════════════════════════════════════════

    fun generateMonthlyCertificates(
        context: Context,
        entries: List<MonthlyCertEntry>,
        school: SchoolInfo,
        month: String,
        fileName: String
    ): File {

        val doc = PdfDocument()

        /*
         * الشهادات الشهرية عمودية A4.
         */
        val pageWidth = A4_WIDTH_PT
        val pageHeight = A4_HEIGHT_PT

        val topMargin = 12f
        val bottomMargin = 12f
        val gap = 6f

        val availableHeight =
            pageHeight -
                    topMargin -
                    bottomMargin -
                    (CERTS_PER_PAGE - 1) * gap

        val certHeight =
            availableHeight / CERTS_PER_PAGE

        val certWidth =
            pageWidth - 2 * MARGIN

        var pageNumber = 1
        var index = 0

        try {

            while (index < entries.size) {

                val pageInfo =
                    PdfDocument.PageInfo.Builder(
                        pageWidth,
                        pageHeight,
                        pageNumber
                    ).create()

                val page = doc.startPage(pageInfo)

                if (PRINT_SAFE_MODE) {

                    /*
                     * إنشاء الصفحة كاملة كصورة.
                     */
                    val bitmap = renderPageSafely(
                        pageWidthPt = pageWidth,
                        pageHeightPt = pageHeight
                    ) { rasterCanvas ->

                        var currentY = topMargin

                        for (i in 0 until CERTS_PER_PAGE) {

                            if (index + i >= entries.size) {
                                break
                            }

                            drawMonthlyCertificate(
                                canvas = rasterCanvas,
                                x = MARGIN,
                                y = currentY,
                                width = certWidth,
                                height = certHeight,
                                entry = entries[index + i],
                                school = school,
                                month = month
                            )

                            currentY += certHeight

                            /*
                             * خط القص المتقطع.
                             */
                            if (
                                i < CERTS_PER_PAGE - 1 &&
                                index + i + 1 < entries.size
                            ) {

                                val cutY =
                                    currentY + gap / 2f

                                val dashPaint =
                                    Paint().apply {
                                        color = Color.rgb(
                                            120,
                                            120,
                                            120
                                        )
                                        strokeWidth = 0.8f
                                        pathEffect =
                                            DashPathEffect(
                                                floatArrayOf(
                                                    5f,
                                                    5f
                                                ),
                                                0f
                                            )
                                    }

                                rasterCanvas.drawLine(
                                    8f,
                                    cutY,
                                    pageWidth - 8f,
                                    cutY,
                                    dashPaint
                                )

                                val scissorPaint =
                                    TextPaint().apply {
                                        color = Color.rgb(
                                            120,
                                            120,
                                            120
                                        )
                                        textSize = 9f
                                    }

                                rasterCanvas.drawText(
                                    "✂",
                                    3f,
                                    cutY + 3f,
                                    scissorPaint
                                )

                                rasterCanvas.drawText(
                                    "✂",
                                    pageWidth - 13f,
                                    cutY + 3f,
                                    scissorPaint
                                )
                            }

                            currentY += gap
                        }
                    }

                    try {

                        drawRasterOnPdfPage(
                            pageCanvas = page.canvas,
                            bitmap = bitmap,
                            pageWidthPt = pageWidth,
                            pageHeightPt = pageHeight
                        )

                        doc.finishPage(page)

                    } finally {

                        bitmap.recycle()
                    }

                } else {

                    /*
                     * الوضع القديم.
                     */
                    val canvas = page.canvas

                    canvas.drawColor(Color.WHITE)

                    var currentY = topMargin

                    for (i in 0 until CERTS_PER_PAGE) {

                        if (index + i >= entries.size) {
                            break
                        }

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

                        if (
                            i < CERTS_PER_PAGE - 1 &&
                            index + i + 1 < entries.size
                        ) {

                            val cutY =
                                currentY + gap / 2f

                            val dashPaint =
                                Paint().apply {
                                    color = Color.rgb(
                                        120,
                                        120,
                                        120
                                    )
                                    strokeWidth = 0.8f
                                    pathEffect =
                                        DashPathEffect(
                                            floatArrayOf(
                                                5f,
                                                5f
                                            ),
                                            0f
                                        )
                                }

                            canvas.drawLine(
                                8f,
                                cutY,
                                pageWidth - 8f,
                                cutY,
                                dashPaint
                            )

                            val scissorPaint =
                                TextPaint().apply {
                                    color = Color.rgb(
                                        120,
                                        120,
                                        120
                                    )
                                    textSize = 9f
                                }

                            canvas.drawText(
                                "✂",
                                3f,
                                cutY + 3f,
                                scissorPaint
                            )

                            canvas.drawText(
                                "✂",
                                pageWidth - 13f,
                                cutY + 3f,
                                scissorPaint
                            )
                        }

                        currentY += gap
                    }

                    doc.finishPage(page)
                }

                pageNumber++
                index += CERTS_PER_PAGE
            }

            val outDir =
                File(context.cacheDir, "reports").apply {
                    mkdirs()
                }

            val outFile = File(outDir, fileName)

            FileOutputStream(outFile).use {
                doc.writeTo(it)
            }

            return outFile

        } finally {

            doc.close()
        }
    }

    // ═══════════════════════════════════════════
    // الشهادة الشهرية
    // ═══════════════════════════════════════════

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

        // إطار الشهادة
        val borderPaint = Paint().apply {
            color = Color.rgb(15, 118, 110)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

        canvas.drawRect(
            x,
            y,
            x + width,
            y + height,
            borderPaint
        )

        // إطار داخلي
        val innerBorder = Paint().apply {
            color = Color.rgb(200, 220, 218)
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }

        canvas.drawRect(
            x + 3f,
            y + 3f,
            x + width - 3f,
            y + height - 3f,
            innerBorder
        )

        var cy = y + 8f

        // الشعار
        if (school.logoBase64.isNotBlank()) {

            try {

                val bytes =
                    Base64.decode(
                        school.logoBase64,
                        Base64.DEFAULT
                    )

                val bitmap =
                    BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size
                    )

                if (bitmap != null) {

                    val maxLogo = 22f

                    val ratio =
                        bitmap.width.toFloat() /
                                bitmap.height.toFloat()

                    val w =
                        if (bitmap.width > bitmap.height) {
                            maxLogo
                        } else {
                            maxLogo * ratio
                        }

                    val h =
                        if (bitmap.width > bitmap.height) {
                            maxLogo / ratio
                        } else {
                            maxLogo
                        }

                    val lx =
                        x + (width - w) / 2f

                    canvas.drawBitmap(
                        bitmap,
                        null,
                        RectF(
                            lx,
                            cy,
                            lx + w,
                            cy + h
                        ),
                        null
                    )

                    cy += h + 2f
                }

            } catch (e: Exception) {
            }
        }
// ★★★ صورة الطالب (أعلى يمين) ★★★
if (entry.photoBase64.isNotBlank()) {
    try {
        val photoBytes = Base64.decode(entry.photoBase64, Base64.DEFAULT)
        val photoBitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
        if (photoBitmap != null) {
            val photoSize = 32f
            val photoX = x + width - photoSize - 8f
            val photoY = y + 8f

            val photoBorder = Paint().apply {
                color = Color.rgb(180, 180, 180)
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }
            canvas.drawRect(
                photoX - 1f, photoY - 1f,
                photoX + photoSize + 1f, photoY + photoSize + 1f,
                photoBorder
            )

            canvas.drawBitmap(
                photoBitmap,
                null,
                RectF(photoX, photoY, photoX + photoSize, photoY + photoSize),
                null
            )
        }
    } catch (e: Exception) {}
}

        // اسم المدرسة
        val schoolPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 10f
            isFakeBoldText = true
        }

        drawCenteredText(
            canvas,
            school.schoolName.ifBlank {
                "اسم المدرسة"
            },
            x + width / 2f,
            cy + 10f,
            schoolPaint
        )

        cy += 12f

        // العنوان + العام
        val titleWithYear =
            if (school.academicYear.isNotBlank()) {
                "شهادة الطالب الشهرية — ${school.academicYear}"
            } else {
                "شهادة الطالب الشهرية"
            }

        val titlePaint = TextPaint().apply {
            color = Color.rgb(15, 118, 110)
            textSize = 10f
            isFakeBoldText = true
        }

        drawCenteredText(
            canvas,
            titleWithYear,
            x + width / 2f,
            cy + 10f,
            titlePaint
        )

        cy += 13f

        // خط فاصل
        canvas.drawLine(
            x + 12f,
            cy,
            x + width - 12f,
            cy,
            Paint().apply {
                color = Color.rgb(15, 118, 110)
                strokeWidth = 0.8f
            }
        )

        cy += 4f

        // معلومات الطالب
        val sectionPart =
            if (
                entry.sectionName.isNotBlank() &&
                entry.sectionName != "—"
            ) {
                " / ${entry.sectionName}"
            } else {
                ""
            }

        val infoLine =
            "👤 ${entry.studentName}   |   🏫 ${entry.className}$sectionPart   |   📅 $month"

        val studentPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 8f
            isFakeBoldText = true
        }

        drawCenteredText(
            canvas,
            infoLine,
            x + width / 2f,
            cy + 8f,
            studentPaint
        )

        cy += 11f

        // الجدول الأفقي
        val tableLeft = x + 10f
        val tableWidth = width - 20f

        val numCols =
            entry.grades.size.coerceAtLeast(1)

        val colWidth =
            tableWidth / numCols

        val rowH = 15f
        val headH = 13f

        // صف أسماء المواد
        var cx = tableLeft + tableWidth

        entry.grades.forEach { g ->

            val cellX = cx - colWidth

            canvas.drawRect(
                cellX,
                cy,
                cx,
                cy + headH,
                Paint().apply {
                    color = Color.rgb(
                        241,
                        245,
                        249
                    )
                }
            )

            val hp = TextPaint().apply {
                color = Color.BLACK
                textSize = 7.5f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val subjectText =
                if (g.subjectName.length > 10) {
                    g.subjectName.substring(
                        0,
                        9
                    ) + "…"
                } else {
                    g.subjectName
                }

            canvas.drawText(
                subjectText,
                cellX + colWidth / 2f,
                cy + headH / 2f + 3f,
                hp
            )

            canvas.drawRect(
                cellX,
                cy,
                cx,
                cy + headH,
                borderPaint
            )

            cx = cellX
        }

        cy += headH

        // صف الدرجات
        cx = tableLeft + tableWidth

        entry.grades.forEach { g ->

            val cellX = cx - colWidth

            val dp = TextPaint().apply {
                color = Color.rgb(
                    15,
                    118,
                    110
                )
                textSize = 8.5f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val scoreText =
                "${g.score} من ${g.maxScore}"

            canvas.drawText(
                scoreText,
                cellX + colWidth / 2f,
                cy + rowH / 2f + 3f,
                dp
            )

            canvas.drawRect(
                cellX,
                cy,
                cx,
                cy + rowH,
                borderPaint
            )

            cx = cellX
        }

        cy += rowH + 3f

        // الإجماليات
        val totalScore =
            entry.grades.sumOf {
                it.score
            }

        val totalMax =
            entry.grades.sumOf {
                it.maxScore
            }

        val avg =
            if (entry.grades.isNotEmpty()) {
                totalScore.toDouble() /
                        entry.grades.size
            } else {
                0.0
            }

        val firstMax =
            entry.grades.firstOrNull()?.maxScore
                ?: 20

        val overallRating =
            ratingForPercent(
                (avg / firstMax * 100).toInt()
            )

        val totalsPaint =
            TextPaint().apply {
                color = Color.BLACK
                textSize = 8f
                isFakeBoldText = true
            }

        val totalsText =
            "المجموع: $totalScore / $totalMax   |   المتوسط: ${
                String.format("%.2f", avg)
            } / $firstMax   |   التقدير: $overallRating"

        // ★★★ التعديل: جعل التقدير أحمر إذا كان راسب ★★★
val isFail = overallRating == "راسب"
val ratingPaint = if (isFail) {
    TextPaint(totalsPaint).apply { color = Color.RED }
} else {
    totalsPaint
}
drawCenteredText(
    canvas,
    totalsText,
    x + width / 2f,
    cy + 8f,
    ratingPaint
)
// ★★★ نهاية التعديل ★★★nt

        cy += 14f

        // التوقيعات
        val sigY = cy + 6f

        val third =
            tableWidth / 3f

        val positions = listOf(
            x + width - 10f - third / 2f,
            x + 10f + third / 2f
        )

        val labels =
            listOf(
                "مدير المدرسة",
                "مربي الصف"
            )

        val values =
            listOf(
                school.principalName,
                school.homeroomTeacherName
            )

        val sigPaint =
    TextPaint().apply {
        color = Color.BLACK
        textSize = 9f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

val namePaint =
    TextPaint().apply {
        color = Color.BLACK
        textSize = 8f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

        val sigLinePaint =
            Paint().apply {
                color = Color.DKGRAY
                strokeWidth = 0.5f
            }

        positions.forEachIndexed { i, px ->

            canvas.drawLine(
                px - third * 0.35f,
                sigY,
                px + third * 0.35f,
                sigY,
                sigLinePaint
            )

            canvas.drawText(
                labels[i],
                px,
                sigY + 8f,
                sigPaint
            )

            if (values[i].isNotBlank()) {

                canvas.drawText(
                    values[i],
                    px,
                    sigY + 16f,
                    namePaint
                )
            }
        }

        // الختم الرسمي
        val stampX =
            x + width / 2f

        val stampRadius = 15f

        val stampPaint =
            Paint().apply {
                color = Color.argb(100, 150, 150, 150)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

        canvas.drawCircle(
            stampX,
            sigY + 30f,
            stampRadius,
            stampPaint
        )

        val stampTextPaint =
            TextPaint().apply {
                color = Color.argb(100, 150, 150, 150)
                textSize = 5f
                textAlign = Paint.Align.CENTER
            }

        canvas.drawText(
            "الختم",
            stampX,
            sigY + 28f,
            stampTextPaint
        )

        canvas.drawText(
            "الرسمي",
            stampX,
            sigY + 28f,
            stampTextPaint
        )
    }

    // ═══════════════════════════════════════════
    // التقدير
    // ═══════════════════════════════════════════

    private fun ratingForPercent(
        percent: Int
    ): String {

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
    // رسم التقارير العادية
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

        val centerX =
            pageWidth / 2f

        // الشعار
        if (school.logoBase64.isNotBlank()) {

            try {

                val bytes =
                    Base64.decode(
                        school.logoBase64,
                        Base64.DEFAULT
                    )

                val bitmap =
                    BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size
                    )

                if (bitmap != null) {

                    val maxLogoSize = 55f

                    val ratio =
                        bitmap.width.toFloat() /
                                bitmap.height.toFloat()

                    val logoW: Float
                    val logoH: Float

                    if (bitmap.width > bitmap.height) {

                        logoW = maxLogoSize
                        logoH =
                            maxLogoSize / ratio

                    } else {

                        logoW =
                            maxLogoSize * ratio

                        logoH =
                            maxLogoSize
                    }

                    val left =
                        centerX - logoW / 2f

                    canvas.drawBitmap(
                        bitmap,
                        null,
                        RectF(
                            left,
                            y,
                            left + logoW,
                            y + logoH
                        ),
                        null
                    )

                    y += logoH + 10f
                }

            } catch (e: Exception) {
            }
        }

        // اسم المدرسة
        val schoolPaint =
            TextPaint().apply {
                color = Color.BLACK
                textSize = FONT_SCHOOL
                isFakeBoldText = true
            }

        drawCenteredText(
            canvas,
            school.schoolName.ifBlank {
                "اسم المدرسة"
            },
            centerX,
            y + FONT_SCHOOL,
            schoolPaint
        )

        y += FONT_SCHOOL + 5f

        // العام الدراسي
        if (school.academicYear.isNotBlank()) {

            val yearPaint =
                TextPaint().apply {
                    color = Color.DKGRAY
                    textSize = FONT_YEAR
                    isFakeBoldText = true
                }

            drawCenteredText(
                canvas,
                school.academicYear,
                centerX,
                y + FONT_YEAR,
                yearPaint
            )

            y += FONT_YEAR + 6f
        }

        // الخط الفاصل
        canvas.drawLine(
            MARGIN,
            y,
            pageWidth - MARGIN,
            y,
            Paint().apply {
                color = Color.rgb(
                    15,
                    118,
                    110
                )
                strokeWidth = 1.5f
            }
        )

        y += 12f
// ★★★ صورة الطالب (أعلى يمين) ★★★
if (report.photoBase64.isNotBlank()) {
    try {
        val photoBytes = Base64.decode(report.photoBase64, Base64.DEFAULT)
        val photoBitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
        if (photoBitmap != null) {
            val photoSize = 45f
            val photoX = pageWidth - MARGIN - photoSize
            val photoY = MARGIN + 5f

            // إطار رمادي للصورة
            val photoBorder = Paint().apply {
                color = Color.rgb(180, 180, 180)
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            canvas.drawRect(
                photoX - 1f, photoY - 1f,
                photoX + photoSize + 1f, photoY + photoSize + 1f,
                photoBorder
            )

            canvas.drawBitmap(
                photoBitmap,
                null,
                RectF(photoX, photoY, photoX + photoSize, photoY + photoSize),
                null
            )
        }
    } catch (e: Exception) {}
}
        // العنوان
        val titlePaint =
            TextPaint().apply {
                color = Color.BLACK
                textSize = FONT_TITLE
                textAlign = Paint.Align.RIGHT
                isFakeBoldText = true
            }

        canvas.drawText(
            report.title,
            pageWidth - MARGIN,
            y + FONT_TITLE,
            titlePaint
        )

        // المعلومات
        val metaPaint =
            TextPaint().apply {
                color = Color.rgb(
                    30,
                    30,
                    30
                )
                textSize = FONT_META
                textAlign = Paint.Align.LEFT
                isFakeBoldText = true
            }

        canvas.drawText(
            report.meta,
            MARGIN,
            y + FONT_META,
            metaPaint
        )

        y += FONT_TITLE + 14f

        // الجدول
        val tableWidth =
            pageWidth - 2 * MARGIN

        val colWidths =
            report.columns.map {
                it.width * tableWidth
            }

        val headerHeight = 25f

        val reservedBottom =
            if (isLastPage) {
                90f
            } else {
                30f
            }

        val availableForRows =
            pageHeight -
                    y -
                    headerHeight -
                    MARGIN -
                    reservedBottom

        val rowsCount =
            rowsChunk.size.coerceAtLeast(1)

        val rowHeight =
            (
                    availableForRows /
                            rowsCount
                    ).coerceIn(
                    9f,
                    20f
                )

        val headerPaint =
            TextPaint().apply {
                color = Color.BLACK
                textSize = FONT_TABLE_HEADER
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }

        val cellBgPaint =
            Paint().apply {
                color = Color.rgb(
                    241,
                    245,
                    249
                )
            }

        val borderPaint =
            Paint().apply {
                color = Color.rgb(
                    180,
                    180,
                    180
                )
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }

        canvas.drawRect(
            MARGIN,
            y,
            pageWidth - MARGIN,
            y + headerHeight,
            cellBgPaint
        )

        var x =
            pageWidth - MARGIN

        report.columns.forEachIndexed { i, col ->

            val colW =
                colWidths[i]

            val cellX =
                x - colW

            drawHeaderCell(
                canvas,
                col.title,
                cellX,
                y,
                colW,
                headerHeight,
                headerPaint
            )

            canvas.drawRect(
                cellX,
                y,
                x,
                y + headerHeight,
                borderPaint
            )

            x = cellX
        }

        y += headerHeight

        // حجم خط جسم الجدول حسب الاتجاه
        val bodyFontSize =
            if (report.isLandscape) {
                FONT_TABLE_BODY_L
            } else {
                FONT_TABLE_BODY_V
            }

        val bodyPaint =
            TextPaint().apply {
                color = Color.BLACK
                textSize = bodyFontSize
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }

        val redBodyPaint =
            TextPaint().apply {
                color = Color.rgb(
                    180,
                    30,
                    30
                )
                textSize = bodyFontSize
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }

        rowsChunk.forEach { row ->

            x =
                pageWidth - MARGIN

            row.forEachIndexed { i, cell ->

                val colW =
                    colWidths.getOrElse(i) {
                        0f
                    }

                val cellX =
                    x - colW

                val paint =
                    if (
                        i in report.redColumnIndices
                    ) {
                        redBodyPaint
                    } else {
                        bodyPaint
                    }

                drawCenteredInBox(
                    canvas,
                    cell,
                    cellX,
                    y,
                    colW,
                    rowHeight,
                    paint
                )

                canvas.drawRect(
                    cellX,
                    y,
                    x,
                    y + rowHeight,
                    borderPaint
                )

                x = cellX
            }

            y += rowHeight
        }

        // التوقيعات
        if (isLastPage) {

            drawSignatures(
                canvas,
                pageWidth,
                pageHeight,
                school,
                report.isMultiSubject
            )
        }

        // رقم الصفحة
        if (totalPages > 1) {

            val pageNumPaint =
                TextPaint().apply {
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

    // ═══════════════════════════════════════════
    // رأس الجدول
    // ═══════════════════════════════════════════

    private fun drawHeaderCell(
        canvas: Canvas,
        text: String,
        boxLeft: Float,
        boxTop: Float,
        boxWidth: Float,
        boxHeight: Float,
        paint: TextPaint
    ) {

        if (text.isBlank()) {
            return
        }

        val linePaint =
            TextPaint(paint)

        val lineHeight =
            linePaint.textSize + 2f

        val startY =
            boxTop +
                    (boxHeight - lineHeight) / 2f +
                    linePaint.textSize

        linePaint.textAlign =
            Paint.Align.CENTER

        linePaint.isFakeBoldText = true

        canvas.drawText(
            text,
            boxLeft + boxWidth / 2f,
            startY,
            linePaint
        )
    }

    // ═══════════════════════════════════════════
    // التوقيعات
    // ═══════════════════════════════════════════

    private fun drawSignatures(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        school: SchoolInfo,
        isMultiSubject: Boolean
    ) {

        val signLineY =
            pageHeight - 75f

        val labelPaint =
            TextPaint().apply {
                color = Color.BLACK
                textSize = 10f
                textAlign = Paint.Align.CENTER
            }

        val signHintPaint =
            TextPaint().apply {
                color = Color.DKGRAY
                textSize = 8f
                textAlign = Paint.Align.RIGHT
            }

        val namePaint =
            TextPaint().apply {
                color = Color.rgb(
                    30,
                    30,
                    30
                )
                textSize = 9f
                textAlign = Paint.Align.CENTER
            }

        val emptyLinePaint =
            TextPaint().apply {
                color = Color.rgb(
                    130,
                    130,
                    130
                )
                textSize = 9f
                textAlign = Paint.Align.CENTER
            }

        val linePaint =
            Paint().apply {
                color = Color.BLACK
                strokeWidth = 1f
            }

        // حسب نوع الكشف
        val labels: List<String>
        val values: List<String>

        if (isMultiSubject) {

            // كشف متعدد المواد
            labels =
                listOf(
                    "مدير المدرسة",
                    "مربي الصف"
                )

            values =
                listOf(
                    school.principalName,
                    school.homeroomTeacherName
                )

        } else {

            // كشف مادة واحدة
            labels =
                listOf(
                    "مدير المدرسة",
                    "معلم المادة"
                )

            values =
                listOf(
                    school.principalName,
                    school.teacherName
                )
        }

        val third =
            (pageWidth - 2 * MARGIN) / 3f

        val positions =
            listOf(
                pageWidth -
                        MARGIN -
                        third / 2f,

                pageWidth / 2f,

                MARGIN +
                        third / 2f
            )

        // مدير المدرسة
        var x = positions[0]

        canvas.drawLine(
            x - third * 0.35f,
            signLineY,
            x + third * 0.35f,
            signLineY,
            linePaint
        )

        canvas.drawText(
            labels[0],
            x,
            signLineY + 12f,
            labelPaint
        )

        val signHintX1 =
            x + third * 0.35f

        canvas.drawText(
            "التوقيع/",
            signHintX1,
            signLineY + 12f,
            signHintPaint
        )

        if (values[0].isNotBlank()) {

            canvas.drawText(
                values[0],
                x,
                signLineY + 25f,
                namePaint
            )

        } else {

            canvas.drawText(
                "____________________",
                x,
                signLineY + 25f,
                emptyLinePaint
            )
        }

        // المعلم / مربي الصف
        x = positions[2]

        canvas.drawLine(
            x - third * 0.35f,
            signLineY,
            x + third * 0.35f,
            signLineY,
            linePaint
        )

        canvas.drawText(
            labels[1],
            x,
            signLineY + 12f,
            labelPaint
        )

        val signHintX2 =
            x + third * 0.35f

        canvas.drawText(
            "التوقيع/",
            signHintX2,
            signLineY + 12f,
            signHintPaint
        )

        if (values[1].isNotBlank()) {

            canvas.drawText(
                values[1],
                x,
                signLineY + 25f,
                namePaint
            )

        } else {

            canvas.drawText(
                "____________________",
                x,
                signLineY + 25f,
                emptyLinePaint
            )
        }

        // الختم الرسمي
        x = positions[1]

        val stampRadius = 25f

        val stampPaint =
            Paint().apply {
                color = Color.argb(100, 150, 150, 150)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }

        canvas.drawCircle(
            x,
            signLineY - 5f,
            stampRadius,
            stampPaint
        )

        val stampTextPaint =
            TextPaint().apply {
                color = Color.argb(100, 150, 150, 150)
                textSize = 7f
                textAlign = Paint.Align.CENTER
            }

        canvas.drawText(
            "الختم",
            x,
            signLineY - 5f,
            stampTextPaint
        )

        canvas.drawText(
            "الرسمي",
            x,
            signLineY - 12f,
            stampTextPaint
        )
    }

    // ═══════════════════════════════════════════
    // نص في المنتصف
    // ═══════════════════════════════════════════

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        paint: TextPaint
    ) {

        val width =
            paint.measureText(text)

        val oldAlign =
            paint.textAlign

        paint.textAlign =
            Paint.Align.LEFT

        canvas.drawText(
            text,
            centerX - width / 2f,
            baselineY,
            paint
        )

        paint.textAlign =
            oldAlign
    }

    // ═══════════════════════════════════════════
    // نص داخل خلية
    // ═══════════════════════════════════════════

    private fun drawCenteredInBox(
        canvas: Canvas,
        text: String,
        boxLeft: Float,
        boxTop: Float,
        boxWidth: Float,
        boxHeight: Float,
        paint: TextPaint
    ) {

        if (text.isBlank()) {
            return
        }

        val oldAlign =
            paint.textAlign

        paint.textAlign =
            Paint.Align.CENTER

        val baselineY =
            boxTop +
                    boxHeight / 2f +
                    paint.textSize / 3f

        canvas.drawText(
            text,
            boxLeft + boxWidth / 2f,
            baselineY,
            paint
        )

        paint.textAlign =
            oldAlign
    }
    fun savePdfToDownloads(context: android.content.Context, file: java.io.File) {
    try {
        val fileName = file.name
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = java.io.File(downloadsDir, fileName)
            file.copyTo(destFile, overwrite = true)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
}
