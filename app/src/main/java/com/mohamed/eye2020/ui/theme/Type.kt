package com.mohamed.eye2020.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val base = Typography()

val EyeTypography = base.copy(
    displayLarge = base.displayLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 76.sp,
        lineHeight = 80.sp,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "tnum",
    ),
    displayMedium = base.displayMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum",
    ),
    headlineLarge = base.headlineLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = base.headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = base.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = base.titleMedium.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = base.bodyLarge.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = base.bodyMedium.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = base.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = base.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
    ),
)

/**
 * The styles above tune negative letter-spacing for Latin headlines, and small section
 * headers add generous tracking for all-caps labels. Arabic joins its letters, so any
 * tracking visually breaks the joined words: in Arabic all tracking is dropped.
 */
@Composable
fun typographyForLocale(): Typography {
    if (currentLocaleLanguage() != "ar") return EyeTypography
    return ArabicTypography
}

/** Same as [typographyForLocale] but for one explicit letter-spacing value. */
@Composable
fun letterSpacingForLocale(default: TextUnit): TextUnit {
    if (currentLocaleLanguage() != "ar") return default
    return 0.sp
}

private @Composable fun currentLocaleLanguage(): String =
    LocalConfiguration.current.locales[0].language

private val ArabicTypography = base.copy(
    displayLarge = EyeTypography.displayLarge.copy(letterSpacing = 0.sp),
    displayMedium = EyeTypography.displayMedium.copy(letterSpacing = 0.sp),
    headlineLarge = EyeTypography.headlineLarge.copy(letterSpacing = 0.sp),
    headlineMedium = EyeTypography.headlineMedium.copy(letterSpacing = 0.sp),
    labelLarge = EyeTypography.labelLarge.copy(letterSpacing = 0.sp),
    labelMedium = EyeTypography.labelMedium.copy(letterSpacing = 0.sp),
)
