package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography as TypographyTv

val Typography = Typography(
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
)

val TypographyTv = TypographyTv(
    displayMedium = Typography.displayMedium,
    headlineMedium = Typography.headlineMedium,
    titleMedium = Typography.titleMedium,
    titleSmall = Typography.titleSmall,
    bodyMedium = Typography.bodyMedium,
    labelMedium = Typography.labelMedium,
)
