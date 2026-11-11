package app.oltnm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.oltnm.data.model.Accent
import app.oltnm.data.model.FontFamilyChoice
import app.oltnm.data.model.TextColorChoice
import app.oltnm.data.model.TextSizeChoice
import app.oltnm.data.model.ThemeMode

val OltnShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

fun Accent.color(): Color = when (this) {
    Accent.Green -> OltnPalette.AccentGreen
    Accent.Orange -> OltnPalette.AccentOrange
    Accent.Purple -> OltnPalette.AccentPurple
    Accent.Blue -> OltnPalette.AccentBlue
    Accent.Pink -> OltnPalette.AccentPink
}

fun TextColorChoice.color(isDark: Boolean): Color? = when (this) {
    TextColorChoice.Default -> null
    TextColorChoice.HighContrast -> if (isDark) Color.White else Color.Black
    TextColorChoice.Cream -> if (isDark) Color(0xFFF5E9D0) else Color(0xFF5B4A2A)
    TextColorChoice.Yellow -> if (isDark) Color(0xFFFDE047) else Color(0xFFA16207)
    TextColorChoice.Mint -> if (isDark) Color(0xFFB8F5D8) else Color(0xFF0F766E)
    TextColorChoice.Blue -> if (isDark) Color(0xFFBFDBFE) else Color(0xFF1D4ED8)
    TextColorChoice.Pink -> if (isDark) Color(0xFFFBCFE8) else Color(0xFFBE185D)
}

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = OltnPalette.TextInverse,
    primaryContainer = accent.copy(alpha = 0.16f).compositeOver(OltnPalette.Surface1),
    onPrimaryContainer = accent,
    secondary = OltnPalette.Boost,
    onSecondary = Color(0xFF100720),
    secondaryContainer = OltnPalette.BoostSoft.compositeOver(OltnPalette.Surface1),
    onSecondaryContainer = OltnPalette.Boost,
    tertiary = OltnPalette.Reply,
    onTertiary = Color(0xFF211500),
    background = OltnPalette.Bg,
    onBackground = OltnPalette.Text,
    surface = OltnPalette.Surface1,
    onSurface = OltnPalette.Text,
    surfaceVariant = OltnPalette.Surface2,
    onSurfaceVariant = OltnPalette.Text2,
    surfaceContainer = OltnPalette.Surface2,
    surfaceContainerHigh = OltnPalette.Surface3,
    surfaceContainerHighest = OltnPalette.Surface3,
    outline = OltnPalette.BorderStrong,
    outlineVariant = OltnPalette.Divider,
    error = OltnPalette.Danger,
    onError = Color(0xFF2A0808),
    errorContainer = OltnPalette.DangerSoft.compositeOver(OltnPalette.Surface1),
    onErrorContainer = OltnPalette.Danger,
    inverseSurface = OltnPalette.Text,
    inverseOnSurface = OltnPalette.Bg
)

private fun lightScheme(accent: Color): androidx.compose.material3.ColorScheme {
    val accentDark = lerp(accent, Color.Black, 0.38f)
    return lightColorScheme(
        primary = accentDark,
        onPrimary = Color.White,
        primaryContainer = accent.copy(alpha = 0.16f).compositeOver(Color.White),
        onPrimaryContainer = accentDark,
        secondary = OltnPalette.BoostLight,
        onSecondary = Color.White,
        secondaryContainer = OltnPalette.BoostLightSoft.compositeOver(Color.White),
        onSecondaryContainer = OltnPalette.BoostLight,
        tertiary = OltnPalette.ReplyLight,
        onTertiary = Color.White,
        background = OltnPalette.LightBg,
        onBackground = OltnPalette.LightText,
        surface = OltnPalette.LightSurface1,
        onSurface = OltnPalette.LightText,
        surfaceVariant = OltnPalette.LightSurface2,
        onSurfaceVariant = OltnPalette.LightText2,
        surfaceContainer = OltnPalette.LightSurface2,
        surfaceContainerHigh = OltnPalette.LightSurface3,
        surfaceContainerHighest = OltnPalette.LightSurface3,
        outline = OltnPalette.LightBorderStrong,
        outlineVariant = OltnPalette.LightDivider,
        error = OltnPalette.DangerLight,
        onError = Color.White,
        errorContainer = OltnPalette.DangerLightSoft.compositeOver(Color.White),
        onErrorContainer = OltnPalette.DangerLight,
        inverseSurface = OltnPalette.LightText,
        inverseOnSurface = Color.White
    )
}

@Composable
fun OltnTheme(
    themeMode: ThemeMode = ThemeMode.Dark,
    accent: Accent = Accent.Green,
    fontFamily: FontFamilyChoice = FontFamilyChoice.SpaceGrotesk,
    textSize: TextSizeChoice = TextSizeChoice.Default,
    textColor: TextColorChoice = TextColorChoice.Default,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    val accentColor = accent.color()
    val baseScheme = if (dark) darkScheme(accentColor) else lightScheme(accentColor)
    val fontColor = textColor.color(dark)
    val scheme = if (fontColor == null) {
        baseScheme
    } else {
        baseScheme.copy(
            onBackground = fontColor,
            onSurface = fontColor,
            onSurfaceVariant = fontColor.copy(alpha = 0.66f)
        )
    }
    val semantics = if (dark) DarkSemantics else LightSemantics
    val density = LocalDensity.current
    val scaledDensity = remember(density, textSize) {
        Density(density.density, density.fontScale * textSize.scale)
    }

    CompositionLocalProvider(
        LocalOltnSemantics provides semantics,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = oltnTypography(fontFamily.fontFamily()),
            shapes = OltnShapes,
            content = content
        )
    }
}
