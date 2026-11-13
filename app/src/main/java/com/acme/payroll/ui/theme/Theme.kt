package com.acme.payroll.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Teal40 = Color(0xFF00696E)
private val Teal80 = Color(0xFF4FD8DF)
private val Slate40 = Color(0xFF4A6365)
private val Slate80 = Color(0xFFB1CBCD)
private val Amber40 = Color(0xFF7B5800)
private val Amber80 = Color(0xFFF7BD48)

private val LightColors = lightColorScheme(
    primary = Teal40,
    secondary = Slate40,
    tertiary = Amber40,
    background = Color(0xFFF8FAFA),
    surface = Color(0xFFF8FAFA),
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    secondary = Slate80,
    tertiary = Amber80,
)

object StatusColors {
    val Draft = Color(0xFF6B7280)
    val Pending = Color(0xFFB45309)
    val Approved = Color(0xFF15803D)
    val Rejected = Color(0xFFB91C1C)
    val Calculated = Color(0xFF1D4ED8)
    val Paid = Color(0xFF0F766E)
}

private val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

@Composable
fun PayrollTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
