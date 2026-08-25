#!/bin/bash
sed -i '/interface KhataDao {/a \    @Query("SELECT * FROM khata_customers WHERE tenantId = :tenantId AND phone = :phone LIMIT 1")\n    suspend fun getCustomerByPhone(tenantId: String, phone: String): KhataCustomer?\n' app/src/main/java/com/boikhata/data/local/Phase4Daos.kt
