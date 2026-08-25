package com.boikhata.util.printer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfReceiptGenerator {
    fun generatePdf(context: Context, receipt: ReceiptData): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        
        var y = 30f
        paint.textSize = 16f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(receipt.shopName, 150f, y, paint)
        y += 20f
        
        paint.textSize = 10f
        canvas.drawText(receipt.billNumber, 150f, y, paint)
        y += 20f
        
        paint.textAlign = Paint.Align.LEFT
        receipt.items.forEach { item ->
            canvas.drawText("${item.title} - ${item.quantity} x ${item.unitPrice} = ${item.lineTotal}", 10f, y, paint)
            y += 15f
        }
        y += 10f
        
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Total: ${receipt.total}", 290f, y, paint)
        y += 20f
        canvas.drawText("Paid: ${receipt.paid}", 290f, y, paint)
        
        document.finishPage(page)
        
        val file = File(context.cacheDir, "receipt_${receipt.billNumber}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        
        return file
    }
}
