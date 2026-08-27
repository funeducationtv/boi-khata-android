package com.boikhata.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_bundles")
data class ProductBundle(
    @PrimaryKey val id: String,
    val tenantId: String,
    val nameBn: String,
    val nameEn: String,
    val description: String,
    val bundlePrice: Double,
    val originalTotalPrice: Double,
    val discountPercent: Double,
    val isActive: Boolean,
    val validFrom: Long,
    val validTo: Long,
    val createdAt: Long
)

@Entity(tableName = "bundle_items")
data class BundleItem(
    @PrimaryKey val id: String,
    val bundleId: String,
    val bookId: String,
    val quantity: Int,
    val isFreeItem: Boolean
)

data class BundleWithItems(
    val bundle: ProductBundle,
    val items: List<BundleItemWithBook>
)

data class BundleItemWithBook(
    val bundleItem: BundleItem,
    val book: Book
)
