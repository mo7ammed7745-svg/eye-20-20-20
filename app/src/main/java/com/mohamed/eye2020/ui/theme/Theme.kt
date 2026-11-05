package com.mohamed.eye2020.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = MintDeep,
    primaryContainer = SeaContainer,
    onPrimaryContainer = MintSoft,
    secondary = Sky,
    onSecondary = Color(0xFF0B2B36),
    secondaryContainer = SkyContainer,
    onSecondaryContainer = Color(0xFFD0E8F3),
    tertiary = Sand,
    onTertiary = Color(0xFF2C2415),
    background = Ink,
    onBackground = Mist,
    surface = Ink,
    onSurface = Mist,
    surfaceVariant = InkVariant,
    onSurfaceVariant = Slate,
    surfaceDim = Ink,
    surfaceBright = InkSurfaceHigh,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = InkSurface,
    surfaceContainer = InkSurface,
    surfaceContainerHigh = InkSurfaceHigh,
    surfaceContainerHighest = InkVariant,
    outline = OutlineDark,
    outlineVariant = OutlineDarkSoft,
    error = ErrorDark,
    onError = Ink,
    inverseSurface = Mist,
    inverseOnSurface = Ink,
    inversePrimary = Forest,
    surfaceTint = Mint,
)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = PaperSurface,
    primaryContainer = ForestContainer,
    onPrimaryContainer = ForestOnContainer,
    secondary = Sea,
    onSecondary = PaperSurface,
    secondaryContainer = SeaSoft,
    onSecondaryContainer = Color(0xFF0A2C38),
    tertiary = Color(0xFF6F5633),
    onTertiary = PaperSurface,
    background = Paper,
    onBackground = Color(0xFF161D1B),
    surface = PaperSurface,
    onSurface = Color(0xFF161D1B),
    surfaceVariant = Color(0xFFDCE8E3),
    onSurfaceVariant = Stone,
    surfaceDim = Color(0xFFDFE9E4),
    surfaceBright = PaperSurface,
    surfaceContainerLowest = PaperSurface,
    surfaceContainerLow = Paper,
    surfaceContainer = Color(0xFFEDF4F0),
    surfaceContainerHigh = Color(0xFFE7F0EB),
    surfaceContainerHighest = Color(0xFFE1EBE6),
    outline = OutlineLight,
    outlineVariant = OutlineLightSoft,
    error = ErrorLight,
    onError = PaperSurface,
    inverseSurface = Ink,
    inverseOnSurface = Mist,
    inversePrimary = Mint,
    surfaceTint = Forest,
)

private val EyeShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun EyeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = EyeTypography,
        shapes = EyeShapes,
        content = content,
    )
}
