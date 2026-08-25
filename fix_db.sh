#!/bin/bash
sed -i 's/abstract fun catalogDao(): CatalogDao/abstract fun catalogDao(): CatalogDao\n    abstract fun khataDao(): KhataDao\n    abstract fun accountingDao(): AccountingDao\n    abstract fun masterCatalogDao(): MasterCatalogDao/g' app/src/main/java/com/boikhata/data/local/AppDatabase.kt
