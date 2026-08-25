#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

cat << 'INNER_EOF' > $PKG_DIR/di/DatabaseModule.kt
package com.boikhata.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.boikhata.data.local.AppDatabase
import com.boikhata.data.local.TenantDao
import com.boikhata.data.local.UserDao
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.model.Role
import com.boikhata.domain.model.Tenant
import com.boikhata.domain.model.User
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        tenantDaoProvider: Provider<TenantDao>,
        userDaoProvider: Provider<UserDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "boikhata.db"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    tenantDaoProvider.get().insertTenant(
                        Tenant(
                            id = "t_1",
                            name = "Boi Ghor Library",
                            licenseState = LicenseState.GRACE,
                            licenseDaysRemaining = 8
                        )
                    )
                    userDaoProvider.get().insertUser(
                        User(
                            id = "u_1",
                            tenantId = "t_1",
                            name = "Owner",
                            phone = "01711468027",
                            role = Role.OWNER
                        )
                    )
                }
            }
        })
        .build()
    }

    @Provides
    fun provideTenantDao(database: AppDatabase): TenantDao = database.tenantDao()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
}
INNER_EOF

chmod +x generate_di.sh
./generate_di.sh
