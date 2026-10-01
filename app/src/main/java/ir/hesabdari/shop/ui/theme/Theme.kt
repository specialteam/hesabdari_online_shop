package ir.hesabdari.shop.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.view.WindowCompat
import ir.hesabdari.shop.R
import ir.hesabdari.shop.data.ThemeMode

val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_medium, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0A6C74),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEEF0),
    onPrimaryContainer = Color(0xFF00363B),
    secondary = Color(0xFFB07D1E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8B8),
    onSecondaryContainer = Color(0xFF3A2800),
    tertiary = Color(0xFF5B5FC7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2E1FF),
    onTertiaryContainer = Color(0xFF14137A),
    background = Color(0xFFF3F7F7),
    onBackground = Color(0xFF161D1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF161D1E),
    surfaceVariant = Color(0xFFDFEBEC),
    onSurfaceVariant = Color(0xFF3F4A4C),
    surfaceTint = Color(0xFF0A6C74),
    outline = Color(0xFF6F7B7D),
    outlineVariant = Color(0xFFC3CDCE),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5F5),
    surfaceContainer = Color(0xFFEAF1F1),
    surfaceContainerHigh = Color(0xFFE4ECEC),
    surfaceContainerHighest = Color(0xFFDEE7E8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5CCAD3),
    onPrimary = Color(0xFF00363B),
    primaryContainer = Color(0xFF004F55),
    onPrimaryContainer = Color(0xFFCDEEF0),
    secondary = Color(0xFFEBC06A),
    onSecondary = Color(0xFF402D00),
    secondaryContainer = Color(0xFF5C4200),
    onSecondaryContainer = Color(0xFFFFE8B8),
    tertiary = Color(0xFFBFC1FF),
    onTertiary = Color(0xFF26289B),
    tertiaryContainer = Color(0xFF3D41AD),
    onTertiaryContainer = Color(0xFFE2E1FF),
    background = Color(0xFF0D1415),
    onBackground = Color(0xFFDDE4E5),
    surface = Color(0xFF111A1B),
    onSurface = Color(0xFFDDE4E5),
    surfaceVariant = Color(0xFF3F4A4C),
    onSurfaceVariant = Color(0xFFBFC8CA),
    surfaceTint = Color(0xFF5CCAD3),
    outline = Color(0xFF899294),
    outlineVariant = Color(0xFF3F4A4C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0A1112),
    surfaceContainerLow = Color(0xFF141D1E),
    surfaceContainer = Color(0xFF182223),
    surfaceContainerHigh = Color(0xFF212C2D),
    surfaceContainerHighest = Color(0xFF2B3638),
)

/** Colours with a financial meaning (profit / loss / warning) that the Material scheme doesn't have. */
@Immutable
data class SemanticColors(
    val gain: Color,
    val onGainContainer: Color,
    val gainContainer: Color,
    val loss: Color,
    val onLossContainer: Color,
    val lossContainer: Color,
    val warn: Color,
    val warnContainer: Color,
    val onWarnContainer: Color,
)

private val LightSemantic = SemanticColors(
    gain = Color(0xFF1E8E5A), onGainContainer = Color(0xFF00391F), gainContainer = Color(0xFFD2F3E2),
    loss = Color(0xFFC93B3B), onLossContainer = Color(0xFF410006), lossContainer = Color(0xFFFFDAD6),
    warn = Color(0xFFB07D1E), warnContainer = Color(0xFFFFEFC9), onWarnContainer = Color(0xFF3A2800),
)
private val DarkSemantic = SemanticColors(
    gain = Color(0xFF58D08F), onGainContainer = Color(0xFFD2F3E2), gainContainer = Color(0xFF0F4A2E),
    loss = Color(0xFFFF8A80), onLossContainer = Color(0xFFFFDAD6), lossContainer = Color(0xFF6B1D1D),
    warn = Color(0xFFEBC06A), warnContainer = Color(0xFF4A3600), onWarnContainer = Color(0xFFFFE8B8),
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemantic }

val MaterialTheme.semantic: SemanticColors
    @Composable @ReadOnlyComposable get() = LocalSemanticColors.current

private fun TextStyle.vazir() = copy(fontFamily = Vazir)

private fun appTypography(): Typography {
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge.vazir(),
        displayMedium = b.displayMedium.vazir(),
        displaySmall = b.displaySmall.vazir(),
        headlineLarge = b.headlineLarge.vazir(),
        headlineMedium = b.headlineMedium.vazir(),
        headlineSmall = b.headlineSmall.vazir().copy(fontWeight = FontWeight.Bold),
        titleLarge = b.titleLarge.vazir().copy(fontWeight = FontWeight.Bold, fontSize = 21.sp),
        titleMedium = b.titleMedium.vazir().copy(fontWeight = FontWeight.Medium),
        titleSmall = b.titleSmall.vazir().copy(fontWeight = FontWeight.Medium),
        bodyLarge = b.bodyLarge.vazir(),
        bodyMedium = b.bodyMedium.vazir(),
        bodySmall = b.bodySmall.vazir(),
        labelLarge = b.labelLarge.vazir().copy(fontWeight = FontWeight.Medium),
        labelMedium = b.labelMedium.vazir().copy(fontWeight = FontWeight.Medium),
        labelSmall = b.labelSmall.vazir(),
    )
}

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Composable
fun AppTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme: ColorScheme = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    // The whole UI is right-to-left, independent of the device language.
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalSemanticColors provides if (dark) DarkSemantic else LightSemantic,
    ) {
        MaterialTheme(colorScheme = scheme, typography = appTypography(), shapes = AppShapes, content = content)
    }
}
