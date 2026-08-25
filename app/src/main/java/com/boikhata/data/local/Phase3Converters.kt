package com.boikhata.data.local

import androidx.room.TypeConverter
import com.boikhata.domain.model.*

class Phase3Converters {
    @TypeConverter fun fromBookCategory(v: BookCategory) = v.name
    @TypeConverter fun toBookCategory(n: String) = BookCategory.valueOf(n)
    
    @TypeConverter fun fromStockChangeReason(v: StockChangeReason) = v.name
    @TypeConverter fun toStockChangeReason(n: String) = StockChangeReason.valueOf(n)
    
    @TypeConverter fun fromPaymentMethod(v: PaymentMethod) = v.name
    @TypeConverter fun toPaymentMethod(n: String) = PaymentMethod.valueOf(n)
    
    @TypeConverter fun fromDiscountType(v: DiscountType) = v.name
    @TypeConverter fun toDiscountType(n: String) = DiscountType.valueOf(n)
    
    @TypeConverter fun fromSyncStatus(v: SyncStatus) = v.name
    @TypeConverter fun toSyncStatus(n: String) = SyncStatus.valueOf(n)
    
    @TypeConverter fun fromKhataEntryType(v: KhataEntryType) = v.name
    @TypeConverter fun toKhataEntryType(n: String) = KhataEntryType.valueOf(n)
}
