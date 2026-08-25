package com.boikhata.util

object BengaliUtils {
    private val engToBnMap = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )
    
    fun Int.toBn(): String = this.toString().map { engToBnMap[it] ?: it }.joinToString("")
    fun Double.toBn(): String = String.format("%.2f", this).map { engToBnMap[it] ?: it }.joinToString("")
    fun Long.toBn(): String = this.toString().map { engToBnMap[it] ?: it }.joinToString("")
    fun String.toBn(): String = this.map { engToBnMap[it] ?: it }.joinToString("")
    
    fun formatCurrency(amount: Double): String = "৳" + amount.toBn()
    fun formatCurrency(amount: Int): String = "৳" + amount.toBn()
}
