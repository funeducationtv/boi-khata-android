#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"
mkdir -p $PKG_DIR/{data/local,data/repository,domain/model,domain/repository,presentation/dashboard,presentation/components,presentation/theme,di}

# generate_code
cat << 'INNER_EOF' > $PKG_DIR/BoiKhataApp.kt
package com.boikhata

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BoiKhataApp : Application()
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/domain/model/Enums.kt
package com.boikhata.domain.model

enum class Role {
    OWNER, MANAGER, SALES, ACCOUNTANT
}

enum class LicenseState {
    ACTIVE, GRACE, SOFT_LOCKED, SUSPENDED
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/domain/model/Entities.kt
package com.boikhata.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tenants")
data class Tenant(
    @PrimaryKey val id: String,
    val name: String,
    val licenseState: LicenseState,
    val licenseDaysRemaining: Int
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val tenantId: String,
    val name: String,
    val phone: String,
    val role: Role
)

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey val id: String,
    val tenantId: String,
    val hardwareSignature: String,
    val name: String
)
INNER_EOF

# generate_db
cat << 'INNER_EOF' > $PKG_DIR/data/local/Converters.kt
package com.boikhata.data.local

import androidx.room.TypeConverter
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.model.Role

class Converters {
    @TypeConverter
    fun fromRole(role: Role): String = role.name

    @TypeConverter
    fun toRole(name: String): Role = Role.valueOf(name)

    @TypeConverter
    fun fromLicenseState(state: LicenseState): String = state.name

    @TypeConverter
    fun toLicenseState(name: String): LicenseState = LicenseState.valueOf(name)
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/local/Daos.kt
package com.boikhata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.domain.model.Tenant
import com.boikhata.domain.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants LIMIT 1")
    fun getTenant(): Flow<Tenant?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: Tenant)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE tenantId = :tenantId")
    fun getUsersByTenant(tenantId: String): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/local/AppDatabase.kt
package com.boikhata.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.boikhata.domain.model.Device
import com.boikhata.domain.model.Tenant
import com.boikhata.domain.model.User

@Database(
    entities = [Tenant::class, User::class, Device::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tenantDao(): TenantDao
    abstract fun userDao(): UserDao
}
INNER_EOF

# generate_di
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

# generate_repo
cat << 'INNER_EOF' > $PKG_DIR/domain/repository/DashboardRepository.kt
package com.boikhata.domain.repository

import com.boikhata.domain.model.LicenseState
import kotlinx.coroutines.flow.Flow

data class DashboardData(
    val tenantName: String,
    val licenseState: LicenseState,
    val licenseDaysRemaining: Int,
    val todaySales: Int,
    val todayCollection: Int,
    val todayProfit: Int,
    val lowStockCount: Int,
    val collectTodayList: List<CollectItem>
)

data class CollectItem(
    val id: String,
    val name: String,
    val amount: Int,
    val daysOverdue: Int,
    val isCritical: Boolean
)

interface DashboardRepository {
    fun getDashboardData(): Flow<DashboardData>
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/repository/MockDashboardRepository.kt
package com.boikhata.data.repository

import com.boikhata.data.local.TenantDao
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.repository.CollectItem
import com.boikhata.domain.repository.DashboardData
import com.boikhata.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MockDashboardRepository @Inject constructor(
    private val tenantDao: TenantDao
) : DashboardRepository {
    override fun getDashboardData(): Flow<DashboardData> {
        return tenantDao.getTenant().map { tenant ->
            DashboardData(
                tenantName = tenant?.name ?: "Boi Ghor Library ▾",
                licenseState = tenant?.licenseState ?: LicenseState.GRACE,
                licenseDaysRemaining = tenant?.licenseDaysRemaining ?: 8,
                todaySales = 4500,
                todayCollection = 3200,
                todayProfit = 1150,
                lowStockCount = 3,
                collectTodayList = listOf(
                    CollectItem("1", "রহিম ভাই", 2400, 30, true),
                    CollectItem("2", "করিম স্টোর", 1200, 15, false)
                )
            )
        }
    }
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/di/RepositoryModule.kt
package com.boikhata.di

import com.boikhata.data.repository.MockDashboardRepository
import com.boikhata.domain.repository.DashboardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindDashboardRepository(
        mockDashboardRepository: MockDashboardRepository
    ): DashboardRepository
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/presentation/dashboard/DashboardViewModel.kt
package com.boikhata.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.repository.DashboardData
import com.boikhata.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val data: DashboardData) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    dashboardRepository: DashboardRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = dashboardRepository.getDashboardData()
        .mapToUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState.Loading
        )

    private fun kotlinx.coroutines.flow.Flow<DashboardData>.mapToUiState(): kotlinx.coroutines.flow.Flow<DashboardUiState> {
        return this.map { DashboardUiState.Success(it) }
    }
}
INNER_EOF

# generate_theme
cat << 'INNER_EOF' > $PKG_DIR/presentation/theme/Color.kt
package com.boikhata.presentation.theme

import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF1976D2)
val Secondary = Color(0xFF03A9F4)
val Background = Color(0xFFF5F5F6)
val Surface = Color(0xFFFFFFFF)
val Error = Color(0xFFD32F2F)
val OnPrimary = Color(0xFFFFFFFF)
val OnSecondary = Color(0xFF000000)
val OnBackground = Color(0xFF1E1E1E)
val OnSurface = Color(0xFF1E1E1E)
val OnError = Color(0xFFFFFFFF)
val Outline = Color(0xFFE0E0E0)
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/presentation/theme/Type.kt
package com.boikhata.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val NotoSansBengali = FontFamily(
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali_bold, FontWeight.Bold)
)

val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/presentation/theme/Theme.kt
package com.boikhata.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    error = Error,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    outline = Outline
)

@Composable
fun BoiKhataTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
INNER_EOF

# generate_ui
cat << 'INNER_EOF' > $PKG_DIR/presentation/dashboard/DashboardScreen.kt
package com.boikhata.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.repository.CollectItem
import com.boikhata.domain.repository.DashboardData

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is DashboardUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DashboardUiState.Success -> {
            DashboardContent(data = state.data)
        }
    }
}

@Composable
private fun DashboardContent(data: DashboardData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = data.tenantName, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.sync_status), style = MaterialTheme.typography.bodyMedium)
                Text(text = stringResource(R.string.backup_status), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Banner
        if (data.licenseState == LicenseState.GRACE || data.licenseDaysRemaining <= 14) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.subscription_alert, data.licenseDaysRemaining),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD32F2F)
                    )
                    TextButton(onClick = { /* Pay */ }) {
                        Text(text = stringResource(R.string.pay_now), color = Color(0xFFD32F2F))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stats Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem(title = stringResource(R.string.today_sales), value = data.todaySales)
            StatItem(title = stringResource(R.string.today_collection), value = data.todayCollection)
            StatItem(title = stringResource(R.string.today_profit), value = data.todayProfit)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ActionButton(text = stringResource(R.string.new_bill))
            ActionButton(text = stringResource(R.string.collect_dues))
            ActionButton(text = stringResource(R.string.add_expense))
            ActionButton(text = stringResource(R.string.reports))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Collect Today Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(R.string.collect_today_title), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.see_all), color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                data.collectTodayList.forEach { item ->
                    CollectTodayItemRow(item)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Low Stock Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.low_stock, data.lowStockCount),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { /* Order */ }) {
                    Text(text = stringResource(R.string.order_now))
                }
            }
        }
    }
}

@Composable
private fun StatItem(title: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(
            text = "${stringResource(R.string.currency_symbol)}$value",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun ActionButton(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable { /* Action */ }
            .padding(4.dp)
    )
}

@Composable
private fun CollectTodayItemRow(item: CollectItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "• ${item.name} — ")
            Text(
                text = "${stringResource(R.string.currency_symbol)}${item.amount}",
                fontWeight = FontWeight.Bold
            )
            Text(text = " ${stringResource(R.string.days_format, item.daysOverdue)}")
            if (item.isCritical) {
                Text(text = " 🔴")
            }
        }
        TextButton(onClick = { /* Collect */ }) {
            Text(text = stringResource(R.string.collect_button))
        }
    }
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/presentation/Navigation.kt
package com.boikhata.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.boikhata.presentation.dashboard.DashboardScreen
import kotlinx.serialization.Serializable

@Serializable data object DashboardRoute
@Serializable data object BillRoute
@Serializable data object KhataRoute
@Serializable data object StockRoute
@Serializable data object AccountsRoute
@Serializable data object MoreRoute

data class BottomNavItem(val titleResId: Int, val route: Any)

@Composable
fun MainApp() {
    val navController = rememberNavController()
    
    val items = listOf(
        BottomNavItem(R.string.dashboard, DashboardRoute),
        BottomNavItem(R.string.bill, BillRoute),
        BottomNavItem(R.string.khata, KhataRoute),
        BottomNavItem(R.string.stock, StockRoute),
        BottomNavItem(R.string.accounts, AccountsRoute),
        BottomNavItem(R.string.more, MoreRoute)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { 
                        it.route?.contains(item.route::class.simpleName ?: "") == true 
                    } == true
                    NavigationBarItem(
                        icon = { },
                        label = { Text(stringResource(item.titleResId)) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<DashboardRoute> { DashboardScreen() }
            composable<BillRoute> { PlaceholderScreen(R.string.bill) }
            composable<KhataRoute> { PlaceholderScreen(R.string.khata) }
            composable<StockRoute> { PlaceholderScreen(R.string.stock) }
            composable<AccountsRoute> { PlaceholderScreen(R.string.accounts) }
            composable<MoreRoute> { PlaceholderScreen(R.string.more) }
        }
    }
}

@Composable
fun PlaceholderScreen(titleResId: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.coming_next_phase),
            style = MaterialTheme.typography.titleLarge
        )
    }
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/MainActivity.kt
package com.boikhata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.boikhata.presentation.MainApp
import com.boikhata.presentation.theme.BoiKhataTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoiKhataTheme {
                MainApp()
            }
        }
    }
}
INNER_EOF
