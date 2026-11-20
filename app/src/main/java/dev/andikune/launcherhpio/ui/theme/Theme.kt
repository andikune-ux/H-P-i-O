package dev.andikune.launcherhpio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.andikune.launcherhpio.domain.model.ThemeMode

private val HpioDarkColorScheme = darkColorScheme(
    primary = CyanPrism,
    onPrimary = Color(0xFF002631),
    primaryContainer = Color(0xFF0C3A4F),
    onPrimaryContainer = Color(0xFFC0F4FF),
    secondary = IndigoAura,
    onSecondary = Color(0xFF0E1542),
    secondaryContainer = Color(0xFF232C5C),
    onSecondaryContainer = Color(0xFFE0E5FF),
    tertiary = EmeraldPulse,
    onTertiary = Color(0xFF002B20),
    background = ObsidianBg,
    onBackground = ObsidianOnSurface,
    surface = ObsidianSurface,
    onSurface = ObsidianOnSurface,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianOnSurfaceMuted,
    error = CoralAlert,
    outline = GlassBorderDark
)

private val HpioLightColorScheme = lightColorScheme(
    primary = CyanDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F7FF),
    onPrimaryContainer = Color(0xFF002A36),
    secondary = Color(0xFF4B5AD9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E6FF),
    onSecondaryContainer = Color(0xFF101954),
    tertiary = Color(0xFF0D9473),
    onTertiary = Color.White,
    background = AlabasterBg,
    onBackground = AlabasterOnSurface,
    surface = AlabasterSurface,
    onSurface = AlabasterOnSurface,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = AlabasterOnSurfaceMuted,
    error = Color(0xFFD32F46),
    outline = GlassBorderLight
)

val HpioShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun HpioTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> HpioDarkColorScheme
        else -> HpioLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HpioTypography,
        shapes = HpioShapes,
        content = content
    )
}
