package com.boikhata.domain.model

enum class BookCategory { NCTB, GENERAL, STATIONERY, OTHER }
enum class StockChangeReason { PURCHASE, SALE, RETURN, DAMAGE, ADJUSTMENT }
enum class PaymentMethod { CASH, BKASH, NAGAD, ROCKET, CARD, CREDIT }
enum class DiscountType { NONE, PERCENT, FIXED }
enum class SyncStatus { PENDING, SYNCED, CONFLICT, VOIDED }
enum class KhataEntryType { CREDIT, PAYMENT, ADJUSTMENT, OPENING }

enum class CashbookAccount { CASH, BKASH, BANK }
enum class CashbookEntryType { INCOME, EXPENSE, TRANSFER }
