package dev.andikune.launcherhpio.ui.components

import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.andikune.launcherhpio.R
import dev.andikune.launcherhpio.domain.model.BuiltInWallpaper
import dev.andikune.launcherhpio.domain.model.InstalledApp
import dev.andikune.launcherhpio.domain.model.LauncherPreferences
import dev.andikune.launcherhpio.domain.model.ThemeMode
import dev.andikune.launcherhpio.ui.theme.GlassBorderDark
import dev.andikune.launcherhpio.ui.theme.GlassBorderLight
import dev.andikune.launcherhpio.ui.theme.GlassHighlightDark
import dev.andikune.launcherhpio.ui.theme.GlassHighlightLight
import java.io.File

@Composable
fun isDarkModeActive(preferences: LauncherPreferences): Boolean {
    return when (preferences.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

@Composable
fun WallpaperBackdrop(
    preferences: LauncherPreferences,
    extraBlurDp: Dp = 0.dp,
    scrimAlpha: Float = 0.12f
) {
    val isDark = isDarkModeActive(preferences)
    val totalBlur = if (preferences.safeModeEnabled) 0.dp else (preferences.blurIntensityDp.dp * 0.35f + extraBlurDp)
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && totalBlur > 0.5.dp) {
        Modifier.blur(totalBlur)
    } else {
        Modifier
    }

    val customBitmap: ImageBitmap? = remember(preferences.customWallpaperPath, preferences.selectedWallpaper) {
        if (preferences.selectedWallpaper == BuiltInWallpaper.CUSTOM_IMAGE &&
            !preferences.customWallpaperPath.isNullOrBlank()
        ) {
            runCatching {
                val f = File(preferences.customWallpaperPath)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
            }.getOrNull()
        } else {
            null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
            )
        } else {
            val resId = when (preferences.selectedWallpaper) {
                BuiltInWallpaper.PRISM_SILK -> R.drawable.img_wallpaper_prism_silk
                BuiltInWallpaper.OBSIDIAN_WAVE -> R.drawable.img_wallpaper_obsidian_wave
                else -> R.drawable.img_wallpaper_aurora_glass
            }
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
            )
        }

        // Controlled atmospheric vignette & readability scrim
        val overlayBrush = if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF060A14).copy(alpha = (scrimAlpha + 0.18f).coerceAtMost(0.75f)),
                    Color(0xFF091022).copy(alpha = (scrimAlpha + 0.08f).coerceAtMost(0.65f)),
                    Color(0xFF050811).copy(alpha = (scrimAlpha + 0.25f).coerceAtMost(0.80f))
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFF6F9FF).copy(alpha = scrimAlpha.coerceAtMost(0.55f)),
                    Color(0xFFEFF4FC).copy(alpha = (scrimAlpha * 0.7f).coerceAtMost(0.45f)),
                    Color(0xFFE6EEFA).copy(alpha = (scrimAlpha + 0.08f).coerceAtMost(0.60f))
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBrush)
        )
    }
}

@Composable
fun liquidGlassPanelModifier(
    preferences: LauncherPreferences? = null,
    cornerRadius: Dp = 24.dp,
    alphaBoost: Float = 0f
): Modifier {
    val dark = if (preferences != null) isDarkModeActive(preferences) else isSystemInDarkTheme()
    val alpha = ((preferences?.glassTransparency ?: 0.22f) + alphaBoost).coerceIn(0.14f, 0.90f)
    val shape = RoundedCornerShape(cornerRadius)

    // Multi-stop tint recreates depth inside the glass instead of flattening it
    // into a plain translucent card.
    val fill = if (dark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF52617D).copy(alpha = (alpha * 0.54f).coerceAtMost(0.48f)),
                Color(0xFF18243B).copy(alpha = (alpha + 0.10f).coerceAtMost(0.72f)),
                Color(0xFF0A1325).copy(alpha = (alpha + 0.16f).coerceAtMost(0.82f))
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (alpha + 0.18f).coerceAtMost(0.76f)),
                Color(0xFFF5F9FF).copy(alpha = (alpha + 0.12f).coerceAtMost(0.72f)),
                Color(0xFFDCE8F8).copy(alpha = (alpha + 0.08f).coerceAtMost(0.66f))
            )
        )
    }
    val edge = Brush.linearGradient(
        colors = if (dark) {
            listOf(
                Color.White.copy(alpha = 0.42f),
                Color(0xFFB7D5FF).copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.07f),
                Color(0xFF9CBFFF).copy(alpha = 0.18f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.94f),
                Color(0xFFBBD0EB).copy(alpha = 0.48f),
                Color.White.copy(alpha = 0.34f),
                Color(0xFF8FAED8).copy(alpha = 0.34f)
            )
        }
    )

    return Modifier
        .shadow(if (dark) 12.dp else 10.dp, shape, clip = false)
        .clip(shape)
        .background(fill, shape)
        .border(1.dp, edge, shape)
}
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    preferences: LauncherPreferences,
    cornerRadius: Dp = 26.dp,
    alphaBoost: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = isDarkModeActive(preferences)
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier.then(liquidGlassPanelModifier(preferences, cornerRadius, alphaBoost)),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box {
            // A faint top reflection gives the surface a glass edge without
            // turning every component into a bright, raised card.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (dark) {
                                listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color(0xFFB7D5FF).copy(alpha = 0.055f),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.34f),
                                    Color(0xFFBBD0EB).copy(alpha = 0.11f),
                                    Color.Transparent
                                )
                            }
                        ),
                        shape
                    )
            )
            content()
        }
    }
}
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    preferences: LauncherPreferences,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    LiquidGlassSurface(
        modifier = modifier.then(clickableMod),
        preferences = preferences,
        cornerRadius = 999.dp,
        alphaBoost = 0.06f,
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassAppIconTile(
    label: String,
    appEntry: InstalledApp?,
    iconSize: Dp,
    showLabel: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.16f))
                .border(
                    BorderStroke(0.7.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (appEntry?.icon != null) {
                Image(
                    bitmap = appEntry.icon,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(13.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(iconSize * 0.55f)
                )
            }
        }
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 5.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}
