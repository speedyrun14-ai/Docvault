package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfInfo(
    val title: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val filePath: String,
    val lastModified: Long,
    val isEncrypted: Boolean = false,
    val author: String = "DocuVault User",
    val producer: String = "DocuVault PDF Engine 2.0"
)

object PdfToolkitUtil {

    private fun getDocsDir(context: Context): File {
        val dir = File(context.filesDir, "docuvault_documents")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun createPdfFromBitmaps(context: Context, bitmaps: List<Bitmap>, fileName: String): File = withContext(Dispatchers.IO) {
        val cleanName = if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf"
        val outputFile = File(getDocsDir(context), cleanName)

        val document = PdfDocument()
        for ((index, bitmap) in bitmaps.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)
        }

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        outputFile
    }

    suspend fun renderPdfPagesToBitmaps(context: Context, pdfFile: File, maxPages: Int = 20): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext bitmaps

        try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val pagesToRender = minOf(renderer.pageCount, maxPages)
                    for (i in 0 until pagesToRender) {
                        renderer.openPage(i).use { page ->
                            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmaps.add(bitmap)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bitmaps
    }

    suspend fun mergePdfs(context: Context, files: List<File>, outputName: String): File = withContext(Dispatchers.IO) {
        val cleanName = if (outputName.endsWith(".pdf")) outputName else "$outputName.pdf"
        val outputFile = File(getDocsDir(context), cleanName)
        val newDocument = PdfDocument()
        var globalPageIndex = 1

        for (file in files) {
            if (!file.exists() || file.length() == 0L) continue
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        for (i in 0 until renderer.pageCount) {
                            renderer.openPage(i).use { page ->
                                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                Canvas(bitmap).drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, globalPageIndex++).create()
                                val docPage = newDocument.startPage(pageInfo)
                                docPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                                newDocument.finishPage(docPage)
                                bitmap.recycle()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        FileOutputStream(outputFile).use { out ->
            newDocument.writeTo(out)
        }
        newDocument.close()
        outputFile
    }

    suspend fun splitPdf(context: Context, sourcePdf: File, pageRange: List<Int>, outputName: String): File = withContext(Dispatchers.IO) {
        val cleanName = if (outputName.endsWith(".pdf")) outputName else "$outputName.pdf"
        val outputFile = File(getDocsDir(context), cleanName)
        val newDocument = PdfDocument()
        var newPageIndex = 1

        ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (pageIdx in pageRange) {
                    if (pageIdx in 0 until renderer.pageCount) {
                        renderer.openPage(pageIdx).use { page ->
                            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            Canvas(bitmap).drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, newPageIndex++).create()
                            val docPage = newDocument.startPage(pageInfo)
                            docPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                            newDocument.finishPage(docPage)
                            bitmap.recycle()
                        }
                    }
                }
            }
        }

        FileOutputStream(outputFile).use { out ->
            newDocument.writeTo(out)
        }
        newDocument.close()
        outputFile
    }

    suspend fun rotatePdfPages(context: Context, sourcePdf: File, rotationDegrees: Float, outputName: String): File = withContext(Dispatchers.IO) {
        val cleanName = if (outputName.endsWith(".pdf")) outputName else "$outputName.pdf"
        val outputFile = File(getDocsDir(context), cleanName)
        val newDocument = PdfDocument()

        ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val matrix = Matrix().apply { postRotate(rotationDegrees) }
                        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                        val pageInfo = PdfDocument.PageInfo.Builder(rotatedBitmap.width, rotatedBitmap.height, i + 1).create()
                        val docPage = newDocument.startPage(pageInfo)
                        docPage.canvas.drawBitmap(rotatedBitmap, 0f, 0f, null)
                        newDocument.finishPage(docPage)

                        bitmap.recycle()
                        rotatedBitmap.recycle()
                    }
                }
            }
        }

        FileOutputStream(outputFile).use { out ->
            newDocument.writeTo(out)
        }
        newDocument.close()
        outputFile
    }

    suspend fun annotateOrSignPdf(
        context: Context,
        sourcePdf: File,
        signatureBitmap: Bitmap?,
        watermarkText: String?,
        outputName: String
    ): File = withContext(Dispatchers.IO) {
        val cleanName = if (outputName.endsWith(".pdf")) outputName else "$outputName.pdf"
        val outputFile = File(getDocsDir(context), cleanName)
        val newDocument = PdfDocument()

        val textPaint = Paint().apply {
            color = Color.argb(120, 37, 99, 235) // semi-transparent brand blue
            textSize = 42f
            isAntiAlias = true
            isFakeBoldText = true
        }

        ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        // Draw watermark if provided
                        if (!watermarkText.isNullOrBlank()) {
                            canvas.save()
                            canvas.rotate(-30f, page.width / 2f, page.height / 2f)
                            canvas.drawText(watermarkText, (page.width / 4f), page.height / 2f, textPaint)
                            canvas.restore()
                        }

                        // Draw signature on last page bottom-right if provided
                        if (signatureBitmap != null && i == renderer.pageCount - 1) {
                            val sigWidth = minOf(300, page.width / 3)
                            val sigHeight = (sigWidth * (signatureBitmap.height.toFloat() / signatureBitmap.width)).toInt()
                            val destRect = Rect(
                                page.width - sigWidth - 40,
                                page.height - sigHeight - 80,
                                page.width - 40,
                                page.height - 80
                            )
                            canvas.drawBitmap(signatureBitmap, null, destRect, null)
                        }

                        val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                        val docPage = newDocument.startPage(pageInfo)
                        docPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        newDocument.finishPage(docPage)

                        bitmap.recycle()
                    }
                }
            }
        }

        FileOutputStream(outputFile).use { out ->
            newDocument.writeTo(out)
        }
        newDocument.close()
        outputFile
    }

    suspend fun compressPdf(context: Context, sourcePdf: File, compressionRatio: Float = 0.65f, outputName: String): File = withContext(Dispatchers.IO) {
        val cleanName = if (outputName.endsWith(".pdf")) outputName else "$outputName.pdf"
        val outputFile = File(getDocsDir(context), cleanName)
        val newDocument = PdfDocument()

        ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val targetW = (page.width * compressionRatio).toInt()
                        val targetH = (page.height * compressionRatio).toInt()
                        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val pageInfo = PdfDocument.PageInfo.Builder(targetW, targetH, i + 1).create()
                        val docPage = newDocument.startPage(pageInfo)
                        docPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        newDocument.finishPage(docPage)

                        bitmap.recycle()
                    }
                }
            }
        }

        FileOutputStream(outputFile).use { out ->
            newDocument.writeTo(out)
        }
        newDocument.close()
        outputFile
    }

    suspend fun exportPdfPagesAsJpg(context: Context, sourcePdf: File): List<File> = withContext(Dispatchers.IO) {
        val exportedFiles = mutableListOf<File>()
        val baseName = sourcePdf.nameWithoutExtension

        ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val jpgFile = File(getDocsDir(context), "${baseName}_page_${i + 1}.jpg")
                        FileOutputStream(jpgFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        exportedFiles.add(jpgFile)
                        bitmap.recycle()
                    }
                }
            }
        }
        exportedFiles
    }

    fun getPdfInfo(pdfFile: File): PdfInfo {
        if (!pdfFile.exists()) {
            return PdfInfo(
                title = pdfFile.name,
                pageCount = 0,
                fileSizeBytes = 0L,
                filePath = pdfFile.absolutePath,
                lastModified = System.currentTimeMillis()
            )
        }

        var count = 0
        try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    count = renderer.pageCount
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return PdfInfo(
            title = pdfFile.nameWithoutExtension,
            pageCount = count,
            fileSizeBytes = pdfFile.length(),
            filePath = pdfFile.absolutePath,
            lastModified = pdfFile.lastModified()
        )
    }

    fun createSamplePdf(context: Context, title: String, content: String): File {
        val file = File(getDocsDir(context), "$title.pdf")
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72 dpi
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint().apply {
            color = Color.rgb(16, 24, 39) // DocuNavy
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(55, 65, 81)
            textSize = 14f
            isAntiAlias = true
        }

        val datePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 12f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(37, 99, 235) // DocuBlue
            strokeWidth = 3f
        }

        canvas.drawText("DOCUVAULT CERTIFIED DOCUMENT", 40f, 60f, headerPaint)
        canvas.drawLine(40f, 75f, 555f, 75f, linePaint)

        val dateStr = SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $dateStr", 40f, 100f, datePaint)
        canvas.drawText("Document: $title", 40f, 120f, datePaint)

        var y = 160f
        val lines = content.split("\n")
        for (line in lines) {
            if (line.isBlank()) {
                y += 16f
                continue
            }
            // Simple word wrap
            val words = line.split(" ")
            var currentLine = ""
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (bodyPaint.measureText(testLine) > 515f) {
                    canvas.drawText(currentLine, 40f, y, bodyPaint)
                    y += 20f
                    currentLine = word
                } else {
                    currentLine = testLine
                }
            }
            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, 40f, y, bodyPaint)
                y += 20f
            }
        }

        document.finishPage(page)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }
}
