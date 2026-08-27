package com.boikhata.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.boikhata.domain.model.KhataCustomerWithBalance
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to send WhatsApp and SMS reminders to customers with due payments.
 * Tailored for Bangladeshi bookstore owners to recover dues efficiently.
 */
@Singleton
class SmsWhatsAppManager @Inject constructor() {

    /**
     * Opens WhatsApp with a pre-filled message for due reminder.
     * Message is in Bengali for better customer communication.
     *
     * @param context Android Context
     * @param customer Customer with balance details
     * @param daysOverdue Number of days the payment is overdue
     */
    fun sendWhatsAppDueReminder(
        context: Context,
        customer: KhataCustomerWithBalance,
        daysOverdue: Int
    ) {
        val message = """
        আসসালামু আলাইকুম ${customer.customer.nameBn},
        
        আপনার বকেয়া বিল: ৳${formatBengaliNumber(customer.balance)}
        বকেয়া দিন: ${formatBengaliNumber(daysOverdue)} দিন
        
        দয়া করে পরিশোধ করুন।
        ধন্যবাদ - বই খাতা
        """.trimIndent()

        val encodedMessage = Uri.encode(message)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/${customer.customer.phone}?text=$encodedMessage")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Verify if WhatsApp is installed before starting activity could be added here
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle case where no app can handle the intent
            e.printStackTrace()
        }
    }

    /**
     * Opens SMS app with a pre-filled message for due reminder.
     *
     * @param context Android Context
     * @param customer Customer with balance details
     * @param daysOverdue Number of days the payment is overdue
     */
    fun sendSmsDueReminder(
        context: Context,
        customer: KhataCustomerWithBalance,
        daysOverdue: Int
    ) {
        val message = "বই খাতা: আপনার বকেয়া ৳${formatBengaliNumber(customer.balance)} (${formatBengaliNumber(daysOverdue)} দিন)। পরিশোধ করুন।"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${customer.customer.phone}")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatBengaliNumber(number: Double): String {
        val bengaliDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return number.toString().map { char ->
            if (char.isDigit()) {
                bengaliDigits[char.digitToInt()]
            } else {
                char
            }
        }.joinToString("")
    }
}
