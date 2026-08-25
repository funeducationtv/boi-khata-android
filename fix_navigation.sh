#!/bin/bash
sed -i 's/composable<BillRoute> { PlaceholderScreen(R.string.bill) }/composable<BillRoute> { com.boikhata.presentation.billing.BillingScreen(sessionManager) }/g' app/src/main/java/com/boikhata/presentation/Navigation.kt
sed -i 's/composable<KhataRoute> { PlaceholderScreen(R.string.khata) }/composable<KhataRoute> { com.boikhata.presentation.khata.KhataScreen(sessionManager) }/g' app/src/main/java/com/boikhata/presentation/Navigation.kt
sed -i 's/composable<StockRoute> { PlaceholderScreen(R.string.stock) }/composable<StockRoute> { com.boikhata.presentation.catalog.CatalogScreen(sessionManager) }/g' app/src/main/java/com/boikhata/presentation/Navigation.kt
sed -i 's/composable<AccountsRoute> { PlaceholderScreen(R.string.accounts) }/composable<AccountsRoute> { com.boikhata.presentation.accounting.AccountingScreen(sessionManager) }/g' app/src/main/java/com/boikhata/presentation/Navigation.kt
