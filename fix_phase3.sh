#!/bin/bash

# Fix Enums
sed -i 's/enum class SyncStatus { PENDING, SYNCED, CONFLICT }/enum class SyncStatus { PENDING, SYNCED, CONFLICT, VOIDED }/g' app/src/main/java/com/boikhata/domain/model/Phase3Enums.kt
sed -i 's/enum class KhataEntryType { DUE, PAYMENT }/enum class KhataEntryType { CREDIT, PAYMENT, ADJUSTMENT, OPENING }/g' app/src/main/java/com/boikhata/domain/model/Phase3Enums.kt

cat << 'INNER_EOF' >> app/src/main/java/com/boikhata/domain/model/Phase3Enums.kt

enum class CashbookAccount { CASH, BKASH, BANK }
enum class CashbookEntryType { INCOME, EXPENSE, TRANSFER }
INNER_EOF

# Fix Billing Repo & Screen
sed -i 's/SyncStatus.CONFLICT/SyncStatus.VOIDED/g' app/src/main/java/com/boikhata/data/repository/BillingRepositoryImpl.kt
sed -i 's/SyncStatus.CONFLICT/SyncStatus.VOIDED/g' app/src/main/java/com/boikhata/presentation/billing/BillingScreen.kt
sed -i 's/KhataEntryType.DUE/KhataEntryType.CREDIT/g' app/src/main/java/com/boikhata/data/repository/BillingRepositoryImpl.kt

# Update KhataEntry instantiation in BillingRepositoryImpl
sed -i 's/id = khataId, tenantId = tenantId, customerId = null,/id = khataId, tenantId = tenantId, customerId = "WALK-IN",/g' app/src/main/java/com/boikhata/data/repository/BillingRepositoryImpl.kt
sed -i 's/date = now, referenceId = billId/description = "Due from Bill", referenceBillId = billId, collectedByUserId = userId, date = now, idempotencyKey = UUID.randomUUID().toString()/g' app/src/main/java/com/boikhata/data/repository/BillingRepositoryImpl.kt

