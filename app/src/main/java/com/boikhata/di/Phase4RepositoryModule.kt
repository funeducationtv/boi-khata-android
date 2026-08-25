package com.boikhata.di

import com.boikhata.data.repository.AccountingRepositoryImpl
import com.boikhata.data.repository.KhataRepositoryImpl
import com.boikhata.data.repository.MasterCatalogRepositoryImpl
import com.boikhata.domain.repository.AccountingRepository
import com.boikhata.domain.repository.KhataRepository
import com.boikhata.domain.repository.MasterCatalogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class Phase4RepositoryModule {
    @Binds
    abstract fun bindKhataRepository(impl: KhataRepositoryImpl): KhataRepository
    
    @Binds
    abstract fun bindAccountingRepository(impl: AccountingRepositoryImpl): AccountingRepository
    
    @Binds
    abstract fun bindMasterCatalogRepository(impl: MasterCatalogRepositoryImpl): MasterCatalogRepository
}
