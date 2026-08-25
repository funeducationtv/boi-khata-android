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
