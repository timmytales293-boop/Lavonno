package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NoorColorScheme = darkColorScheme(
    primary = NoorGold,
    onPrimary = NoorDeepestGreen,
    primaryContainer = NoorGoldSurface,
    onPrimaryContainer = NoorGoldSoft,
    secondary = NoorMintAccent,
    onSecondary = NoorDeepestGreen,
    secondaryContainer = NoorElevatedGreen,
    onSecondaryContainer = NoorIvory,
    tertiary = NoorGoldSoft,
    onTertiary = NoorDeepestGreen,
    background = NoorDarkGreen,
    onBackground = NoorIvory,
    surface = NoorCardGreen,
    onSurface = NoorIvory,
    surfaceVariant = NoorElevatedGreen,
    onSurfaceVariant = NoorSageText,
    outline = NoorBorderGreen,
    outlineVariant = NoorGoldMuted,
    error = NoorCrimson,
    onError = NoorIvory,
    errorContainer = NoorCrimsonSurface,
    onErrorContainer = NoorIvory
)

private val NoorShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun NoorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NoorColorScheme,
        typography = NoorTypography,
        shapes = NoorShapes,
        content = content
    )
}
