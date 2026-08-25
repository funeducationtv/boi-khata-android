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
