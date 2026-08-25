package com.boikhata.di

import com.boikhata.data.repository.*
import com.boikhata.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class Phase5RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFirebaseAuthRepository(
        impl: FirebaseAuthRepositoryImpl
    ): FirebaseAuthRepository

    @Binds
    @Singleton
    abstract fun bindCloudLicenseRepository(
        impl: CloudLicenseRepositoryImpl
    ): CloudLicenseRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        impl: SubscriptionRepositoryImpl
    ): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: BackupRepositoryImpl
    ): BackupRepository

    @Binds
    @Singleton
    abstract fun bindRestoreRepository(
        impl: RestoreRepositoryImpl
    ): RestoreRepository

    @Binds
    @Singleton
    abstract fun bindRemoteMasterCatalogRepository(
        impl: RemoteMasterCatalogRepositoryImpl
    ): RemoteMasterCatalogRepository
}
