package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = RoyalMaroon,
    onPrimary = Color(0xFFFFFDF9),
    primaryContainer = SoftMaroonContainer,
    onPrimaryContainer = OnSoftMaroonContainer,
    secondary = AntiqueGold,
    onSecondary = Color(0xFFFFFDF9),
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = TerracottaAccent,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = EspressoInk,
    background = HandloomIvory,
    onBackground = EspressoInk,
    surface = WarmCardSurface,
    onSurface = EspressoInk,
    surfaceVariant = ParchmentCream,
    onSurfaceVariant = RosewoodMuted,
    outline = WarmBorderOutline,
    error = AlertCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkMaroonPrimary,
    onPrimary = DarkOnMaroon,
    primaryContainer = DarkMaroonContainer,
    onPrimaryContainer = SoftMaroonContainer,
    secondary = DarkGoldSecondary,
    onSecondary = OnGoldContainer,
    secondaryContainer = Color(0xFF4F3806),
    onSecondaryContainer = GoldContainer,
    tertiary = Color(0xFFFFB4A1),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF352528),
    onSurfaceVariant = Color(0xFFD8C1C4),
    outline = Color(0xFF6B5357)
)

val StoreShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StoreShapes,
        content = content
    )
}
