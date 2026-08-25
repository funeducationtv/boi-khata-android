#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata/presentation/theme"

cat << 'INNER_EOF' > $PKG_DIR/Color.kt
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

cat << 'INNER_EOF' > $PKG_DIR/Type.kt
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

cat << 'INNER_EOF' > $PKG_DIR/Theme.kt
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

chmod +x generate_theme.sh
./generate_theme.sh
