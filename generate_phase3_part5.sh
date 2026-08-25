#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

# 12. PRINTER AND PDF UTILS
mkdir -p $PKG_DIR/util/printer
cat << 'INNER_EOF' > $PKG_DIR/util/printer/ReceiptData.kt
package com.boikhata.util.printer

data class ReceiptData(
    val shopName: String,
    val shopAddress: String,
    val shopPhone: String,
    val billNumber: String,
    val billDate: String,
    val customerName: String,
    val items: List<ReceiptLineItem>,
    val subtotal: String,
    val discount: String,
    val vat: String,
    val total: String,
    val paid: String,
    val due: String,
    val paymentMethod: String,
    val footerMessage: String
)

data class ReceiptLineItem(
    val title: String,
    val quantity: Int,
    val unitPrice: String,
    val lineTotal: String
)
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/util/printer/ReceiptPrinter.kt
package com.boikhata.util.printer

import kotlinx.coroutines.delay

interface ReceiptPrinter {
    suspend fun isAvailable(): Boolean
    suspend fun printReceipt(receipt: ReceiptData): PrinterResult
    suspend fun printTestPage(): PrinterResult
}

sealed class PrinterResult {
    data class Success(val message: String) : PrinterResult()
    data class Error(val code: Int, val message: String) : PrinterResult()
    data class PaperOut(val message: String) : PrinterResult()
}

class MockReceiptPrinter : ReceiptPrinter {
    override suspend fun isAvailable(): Boolean = true
    override suspend fun printReceipt(receipt: ReceiptData): PrinterResult {
        delay(2000)
        return PrinterResult.Success("Printed mock receipt for ${receipt.billNumber}")
    }
    override suspend fun printTestPage(): PrinterResult {
        delay(1000)
        return PrinterResult.Success("Test page printed")
    }
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/util/printer/PdfReceiptGenerator.kt
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
INNER_EOF

cat << 'INNER_EOF' >> $PKG_DIR/di/RepositoryModule.kt

@Module
@InstallIn(SingletonComponent::class)
object PrinterModule {
    @Provides
    @Singleton
    fun provideReceiptPrinter(): com.boikhata.util.printer.ReceiptPrinter = com.boikhata.util.printer.MockReceiptPrinter()
}
INNER_EOF

