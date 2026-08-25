package com.boikhata.di

import com.boikhata.data.repository.AuditRepositoryImpl
import com.boikhata.data.repository.AuthRepositoryImpl
import com.boikhata.data.repository.UserRepositoryImpl
import com.boikhata.domain.biometric.BiometricAuth
import com.boikhata.domain.biometric.BiometricAuthStub
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.AuthRepository
import com.boikhata.domain.repository.DashboardRepository
import com.boikhata.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindDashboardRepository(impl: com.boikhata.data.repository.DashboardRepositoryImpl): DashboardRepository

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindAuditRepository(impl: AuditRepositoryImpl): AuditRepository
}

@Module
@InstallIn(SingletonComponent::class)
object BiometricModule {
    @Provides
    @Singleton
    fun provideBiometricAuth(): BiometricAuth = BiometricAuthStub()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class Phase3RepositoryModule {
    @Binds
    abstract fun bindCatalogRepository(impl: com.boikhata.data.repository.CatalogRepositoryImpl): com.boikhata.domain.repository.CatalogRepository
    @Binds
    abstract fun bindBillingRepository(impl: com.boikhata.data.repository.BillingRepositoryImpl): com.boikhata.domain.repository.BillingRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PrinterModule {
    @Provides
    @Singleton
    fun provideReceiptPrinter(): com.boikhata.util.printer.ReceiptPrinter = com.boikhata.util.printer.MockReceiptPrinter()
}
