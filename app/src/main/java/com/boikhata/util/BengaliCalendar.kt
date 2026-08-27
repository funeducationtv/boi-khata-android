package com.boikhata.util

import java.util.Calendar

/**
 * Bengali Calendar utility for dual calendar display
 * Converts Gregorian dates to Bengali calendar dates
 * Supports both Bengali and English date formats throughout the app
 */
object BengaliCalendar {

    private val bengaliMonths = listOf(
        "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
        "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র"
    )

    private val bengaliDays = listOf(
        "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )

    /**
     * Get current Bengali date with Gregorian equivalent
     */
    fun getCurrentBengaliDate(): BengaliDate {
        val calendar = Calendar.getInstance()
        return convertToBengaliDate(calendar)
    }

    /**
     * Convert timestamp to Bengali date
     */
    fun convertToBengaliDate(timestamp: Long): BengaliDate {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return convertToBengaliDate(calendar)
    }

    /**
     * Convert Calendar to Bengali date
     * Note: This is a simplified conversion. For production, use accurate algorithm
     */
    private fun convertToBengaliDate(calendar: Calendar): BengaliDate {
        val gregorianYear = calendar.get(Calendar.YEAR)
        val gregorianMonth = calendar.get(Calendar.MONTH) // 0-indexed
        val gregorianDay = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Simplified conversion (production needs accurate astronomical calculation)
        // Boishakh starts around April 14
        var bengaliYear = gregorianYear - 594
        
        // Adjust year based on month
        if (gregorianMonth < 3 || (gregorianMonth == 3 && gregorianDay < 14)) {
            bengaliYear--
        }

        // Calculate Bengali month and day
        val bengaliMonthIndex: Int
        val bengaliDay: Int

        when {
            // Boishakh (mid-April to mid-May)
            gregorianMonth == 3 && gregorianDay >= 14 -> {
                bengaliMonthIndex = 0
                bengaliDay = gregorianDay - 13
            }
            gregorianMonth == 4 -> {
                bengaliMonthIndex = 0
                bengaliDay = gregorianDay + 17
            }
            // Joishtho (mid-May to mid-June)
            gregorianMonth == 5 && gregorianDay < 15 -> {
                bengaliMonthIndex = 0
                bengaliDay = gregorianDay + 17
            }
            gregorianMonth == 5 && gregorianDay >= 15 -> {
                bengaliMonthIndex = 1
                bengaliDay = gregorianDay - 14
            }
            // Asharh (mid-June to mid-July)
            gregorianMonth == 6 && gregorianDay < 15 -> {
                bengaliMonthIndex = 1
                bengaliDay = gregorianDay + 16
            }
            gregorianMonth == 6 && gregorianDay >= 15 -> {
                bengaliMonthIndex = 2
                bengaliDay = gregorianDay - 14
            }
            // Srabon (mid-July to mid-August)
            gregorianMonth == 7 && gregorianDay < 16 -> {
                bengaliMonthIndex = 2
                bengaliDay = gregorianDay + 17
            }
            gregorianMonth == 7 && gregorianDay >= 16 -> {
                bengaliMonthIndex = 3
                bengaliDay = gregorianDay - 15
            }
            // Bhadro (mid-August to mid-September)
            gregorianMonth == 8 && gregorianDay < 16 -> {
                bengaliMonthIndex = 3
                bengaliDay = gregorianDay + 16
            }
            gregorianMonth == 8 && gregorianDay >= 16 -> {
                bengaliMonthIndex = 4
                bengaliDay = gregorianDay - 15
            }
            // Ashwin (mid-September to mid-October)
            gregorianMonth == 9 && gregorianDay < 17 -> {
                bengaliMonthIndex = 4
                bengaliDay = gregorianDay + 15
            }
            gregorianMonth == 9 && gregorianDay >= 17 -> {
                bengaliMonthIndex = 5
                bengaliDay = gregorianDay - 16
            }
            // Kartik (mid-October to mid-November)
            gregorianMonth == 10 && gregorianDay < 17 -> {
                bengaliMonthIndex = 5
                bengaliDay = gregorianDay + 15
            }
            gregorianMonth == 10 && gregorianDay >= 17 -> {
                bengaliMonthIndex = 6
                bengaliDay = gregorianDay - 16
            }
            // Agrahayan (mid-November to mid-December)
            gregorianMonth == 11 && gregorianDay < 16 -> {
                bengaliMonthIndex = 6
                bengaliDay = gregorianDay + 15
            }
            gregorianMonth == 11 && gregorianDay >= 16 -> {
                bengaliMonthIndex = 7
                bengaliDay = gregorianDay - 15
            }
            // Poush (mid-December to mid-January)
            gregorianMonth == 0 && gregorianDay < 15 -> {
                bengaliMonthIndex = 7
                bengaliDay = gregorianDay + 17
            }
            gregorianMonth == 0 && gregorianDay >= 15 -> {
                bengaliMonthIndex = 8
                bengaliDay = gregorianDay - 14
            }
            // Magh (mid-January to mid-February)
            gregorianMonth == 1 && gregorianDay < 14 -> {
                bengaliMonthIndex = 8
                bengaliDay = gregorianDay + 18
            }
            gregorianMonth == 1 && gregorianDay >= 14 -> {
                bengaliMonthIndex = 9
                bengaliDay = gregorianDay - 13
            }
            // Falgun (mid-February to mid-March)
            gregorianMonth == 2 && gregorianDay < 14 -> {
                bengaliMonthIndex = 9
                bengaliDay = gregorianDay + 18
            }
            gregorianMonth == 2 && gregorianDay >= 14 -> {
                bengaliMonthIndex = 10
                bengaliDay = gregorianDay - 13
            }
            // Chaitra (mid-March to mid-April)
            else -> {
                bengaliMonthIndex = 11
                bengaliDay = if (gregorianMonth == 2) gregorianDay + 15 else gregorianDay - 13
            }
        }

        // Adjust for month overflow (some months have 31 days, some 30)
        val daysInMonth = if (bengaliMonthIndex == 0) 31 else 30
        val adjustedDay = if (bengaliDay > daysInMonth) bengaliDay - daysInMonth else bengaliDay
        val adjustedMonthIndex = if (bengaliDay > daysInMonth) (bengaliMonthIndex + 1) % 12 else bengaliMonthIndex

        val gregorianDateStr = String.format("%02d/%02d/%04d", 
            gregorianDay, gregorianMonth + 1, gregorianYear)

        return BengaliDate(
            year = bengaliYear,
            month = bengaliMonths[adjustedMonthIndex],
            monthIndex = adjustedMonthIndex,
            day = adjustedDay,
            dayOfWeek = bengaliDays[(dayOfWeek - 1) % 7],
            gregorianDate = gregorianDateStr,
            fullBengaliDate = "${adjustedDay} ${bengaliMonths[adjustedMonthIndex]} ${bengaliYear}",
            timestamp = calendar.timeInMillis
        )
    }

    /**
     * Format timestamp with both Bengali and Gregorian dates
     */
    fun formatDualDate(timestamp: Long): String {
        val bengaliDate = convertToBengaliDate(timestamp)
        return "${bengaliDate.fullBengaliDate} (${bengaliDate.gregorianDate})"
    }

    /**
     * Get Bengali month name from index
     */
    fun getBengaliMonthName(index: Int): String {
        return bengaliMonths[index % 12]
    }

    /**
     * Get Bengali day name from Calendar day of week
     */
    fun getBengaliDayName(calendarDayOfWeek: Int): String {
        return bengaliDays[(calendarDayOfWeek - 1) % 7]
    }
}

/**
 * Data class representing a Bengali date with Gregorian equivalent
 */
data class BengaliDate(
    val year: Int,
    val month: String,
    val monthIndex: Int,
    val day: Int,
    val dayOfWeek: String,
    val gregorianDate: String,
    val fullBengaliDate: String,
    val timestamp: Long
) {
    /**
     * Get formatted Bengali date string
     */
    fun toBengaliString(): String = fullBengaliDate

    /**
     * Get formatted dual language string
     */
    fun toDualString(): String = "$fullBengaliDate ($gregorianDate)"

    /**
     * Get short Bengali format (e.g., ১ বৈশাখ ১৪৩১)
     */
    fun toShortBengaliString(): String {
        val bengaliDay = day.toBn()
        val bengaliYear = year.toBn()
        return "$bengaliDay $month $bengaliYear"
    }
}
