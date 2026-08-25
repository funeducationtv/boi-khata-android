package com.boikhata.data.local

import androidx.room.TypeConverter
import com.boikhata.domain.model.CashbookAccount
import com.boikhata.domain.model.CashbookEntryType

class Phase4Converters {
    @TypeConverter
    fun fromCashbookAccount(value: CashbookAccount) = value.name
    @TypeConverter
    fun toCashbookAccount(value: String) = CashbookAccount.valueOf(value)

    @TypeConverter
    fun fromCashbookEntryType(value: CashbookEntryType) = value.name
    @TypeConverter
    fun toCashbookEntryType(value: String) = CashbookEntryType.valueOf(value)
}
