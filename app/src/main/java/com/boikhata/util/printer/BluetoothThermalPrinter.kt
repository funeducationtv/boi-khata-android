package com.boikhata.util.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise-level Bluetooth thermal printer integration for Bangladeshi bookstores.
 * Supports 58mm and 80mm thermal printers commonly used in Bangladesh.
 */
@Singleton
class BluetoothThermalPrinter @Inject constructor(@ApplicationContext private val context: Context) : ReceiptPrinter {
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var isConnected = false
    
    /**
     * Check if Bluetooth is available and enabled
     */
    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter != null && adapter.isEnabled
    }
    
    /**
     * Connect to Bluetooth printer by MAC address
     */
    suspend fun connect(deviceAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(Exception("Bluetooth not supported"))
            
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            isConnected = true
            Result.success(Unit)
        } catch (e: Exception) {
            isConnected = false
            Result.failure(e)
        }
    }
    
    /**
     * Disconnect from Bluetooth printer
     */
    suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            // Ignore close errors
        } finally {
            outputStream = null
            bluetoothSocket = null
            isConnected = false
        }
    }
    
    /**
     * Print receipt in Bengali with proper formatting
     */
    override suspend fun printReceipt(receipt: ReceiptData): PrinterResult = withContext(Dispatchers.IO) {
        if (!isConnected) {
            return@withContext PrinterResult.Error(400, "Printer not connected")
        }
        
        try {
            val receiptText = buildBengaliReceipt(receipt)
            outputStream?.write(receiptText.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
            
            // Feed paper
            outputStream?.write(byteArrayOf(0x1B, 0x64, 0x03)) // Feed 3 lines
            outputStream?.flush()
            
            PrinterResult.Success("রসিদ প্রিন্ট হয়েছে: ${receipt.billNumber}")
        } catch (e: Exception) {
            PrinterResult.Error(500, "প্রিন্ট ব্যর্থ: ${e.message}")
        }
    }
    
    /**
     * Print test page
     */
    override suspend fun printTestPage(): PrinterResult = withContext(Dispatchers.IO) {
        if (!isConnected) {
            return@withContext PrinterResult.Error(400, "Printer not connected")
        }
        
        try {
            val testText = """
                ═══════════════════════════
                   বই খাতা - টেস্ট প্রিন্ট
                ═══════════════════════════
                
                প্রিন্টার সঠিক কাজ করছে।
                
                তারিখ: ${getCurrentDateTime()}
                ═══════════════════════════
                
                
            """.trimIndent()
            
            outputStream?.write(testText.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
            outputStream?.write(byteArrayOf(0x1B, 0x64, 0x05)) // Feed 5 lines
            outputStream?.flush()
            
            PrinterResult.Success("টেস্ট প্রিন্ট সফল")
        } catch (e: Exception) {
            PrinterResult.Error(500, "টেস্ট ব্যর্থ: ${e.message}")
        }
    }
    
    /**
     * Build formatted Bengali receipt string
     */
    private fun buildBengaliReceipt(receipt: ReceiptData): String {
        return buildString {
            append("══════════════════════════\n")
            append("       বই খাতা\n")
            append("══════════════════════════\n")
            append("বিল নং: ${receipt.billNumber}\n")
            append("তারিখ: ${formatDate(receipt.timestamp)}\n")
            append("──────────────────────────\n")
            
            receipt.items.forEach { item ->
                append("${item.name}\n")
                append("${item.quantity} × ৳${formatBengaliNumber(item.price)} = ৳${formatBengaliNumber(item.quantity * item.price)}\n")
            }
            
            append("──────────────────────────\n")
            append("সাবমোটাল: ৳${formatBengaliNumber(receipt.subtotal)}\n")
            
            if (receipt.discount > 0) {
                append("ডিসকাউন্ট: ৳${formatBengaliNumber(receipt.discount)}\n")
            }
            
            append("ভ্যাট: ৳${formatBengaliNumber(receipt.vat)}\n")
            append("মোট: ৳${formatBengaliNumber(receipt.total)}\n")
            append("পরিশোধ: ৳${formatBengaliNumber(receipt.paid)}\n")
            append("বকেয়া: ৳${formatBengaliNumber(receipt.due)}\n")
            append("══════════════════════════\n")
            append("   ধন্যবাদ আবার আসবেন\n")
            append("══════════════════════════\n")
        }
    }
    
    /**
     * Format number to Bengali digits
     */
    private fun formatBengaliNumber(number: Double): String {
        val bengaliDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return String.format("%.2f", number).map { char ->
            if (char.isDigit()) {
                bengaliDigits[char.digitToInt()]
            } else {
                char
            }
        }.joinToString("")
    }
    
    /**
     * Get current date time in Bengali format
     */
    private fun getCurrentDateTime(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.ENGLISH)
        return sdf.format(java.util.Date())
    }
    
    /**
     * Format timestamp to readable date
     */
    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.ENGLISH)
        return sdf.format(java.util.Date(timestamp))
    }
    
    /**
     * Scan for available Bluetooth printers
     */
    suspend fun scanForPrinters(): List<BluetoothDevice> = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext emptyList()
        
        val bondedDevices = adapter.bondedDevices
        bondedDevices.filter { device ->
            // Filter devices that are likely printers
            device.name.contains("printer", ignoreCase = true) ||
            device.name.contains("thermal", ignoreCase = true) ||
            device.name.contains("ZJ", ignoreCase = true) || // Common printer prefix
            device.name.contains("MPT", ignoreCase = true)   // Common printer prefix
        }
    }
}
