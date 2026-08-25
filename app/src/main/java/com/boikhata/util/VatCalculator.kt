package com.boikhata.util

import com.boikhata.domain.model.BookCategory

object VatCalculator {
    fun calculateVat(category: BookCategory, price: Double, quantity: Int): Double {
        val vatRate = when (category) {
            BookCategory.STATIONERY -> 0.15
            else -> 0.0
        }
        return (price * quantity) * vatRate
    }
}
