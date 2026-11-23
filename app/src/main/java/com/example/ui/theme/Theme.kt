package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val PandaDarkColorScheme = darkColorScheme(
  primary = JadePrimary,
  onPrimary = Color(0xFF042619),
  primaryContainer = JadeDark,
  onPrimaryContainer = Color(0xFFC2FFE6),
  secondary = BambooGold,
  onSecondary = Color(0xFF2B1E00),
  secondaryContainer = Color(0xFF473300),
  onSecondaryContainer = Color(0xFFFFE08A),
  tertiary = SunsetAmber,
  onTertiary = Color(0xFF2B0D00),
  background = ObsidianBg,
  onBackground = WarmCreamText,
  surface = DeepSlateSurface,
  onSurface = WarmCreamText,
  surfaceVariant = ElevatedCardSurface,
  onSurfaceVariant = MutedSageText,
  outline = BorderGlow,
  error = CrimsonAccent,
  onError = Color.White,
)

private val PandaLightColorScheme = lightColorScheme(
  primary = JadeDark,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFBDF7DD),
  onPrimaryContainer = Color(0xFF05291A),
  secondary = Color(0xFFD49200),
  onSecondary = Color.White,
  tertiary = SunsetAmber,
  onTertiary = Color.White,
  background = LightBg,
  onBackground = DarkInkText,
  surface = LightSurface,
  onSurface = DarkInkText,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = Color(0xFF42594C),
  outline = Color(0xFFB5C9BC),
  error = CrimsonAccent,
  onError = Color.White,
)

val PandaShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(26.dp),
  extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PandaClimbTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) PandaDarkColorScheme else PandaLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = PandaClimbTypography,
    shapes = PandaShapes,
    content = content,
  )
}
