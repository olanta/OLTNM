package app.oltnm.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object OltnPalette {
    val Bg = Color(0xFF0A0E13)
    val BgArena = Color(0xFF070A0E)
    val Surface1 = Color(0xFF10161D)
    val Surface2 = Color(0xFF161E27)
    val Surface3 = Color(0xFF1D2732)

    val Text = Color(0xFFE7EDF3)
    val Text2 = Color(0xA3E7EDF3)
    val Text3 = Color(0x8CE7EDF3)
    val TextInverse = Color(0xFF06120C)

    val Border = Color(0x14FFFFFF)
    val BorderStrong = Color(0x24FFFFFF)
    val Divider = Color(0x0FFFFFFF)
    val Glass = Color(0xB810161D)
    val GlassStrong = Color(0xE010161D)
    val GlassBorder = Color(0x12FFFFFF)

    val Boost = Color(0xFFC084FC)
    val BoostSoft = Color(0x24C084FC)
    val Fav = Color(0xFFFB7185)
    val FavSoft = Color(0x24FB7185)
    val Reply = Color(0xFFFBBF24)
    val ReplySoft = Color(0x21FBBF24)
    val Tag = Color(0xFF5EEAD4)
    val Link = Color(0xFF7DD3FC)
    val Danger = Color(0xFFF87171)
    val DangerSoft = Color(0x24F87171)
    val MediaBg = Color(0xFF05070A)
    val BlurhashBg = Color(0xFF131A22)

    val AccentGreen = Color(0xFF3DDC97)
    val AccentOrange = Color(0xFFF59E0B)
    val AccentPurple = Color(0xFFA78BFA)
    val AccentBlue = Color(0xFF60A5FA)
    val AccentPink = Color(0xFFF472B6)

    val LightBg = Color(0xFFF4F6F8)
    val LightBgArena = Color(0xFFEAEEF2)
    val LightSurface1 = Color(0xFFFFFFFF)
    val LightSurface2 = Color(0xFFF0F3F6)
    val LightSurface3 = Color(0xFFE6EBF0)
    val LightText = Color(0xFF101720)
    val LightText2 = Color(0xA8101720)
    val LightText3 = Color(0x73101720)
    val LightBorder = Color(0x1A000000)
    val LightBorderStrong = Color(0x2E000000)
    val LightDivider = Color(0x12000000)
    val LightGlass = Color(0xC2FFFFFF)
    val LightGlassStrong = Color(0xEBFFFFFF)
    val LightGlassBorder = Color(0x14000000)
    val BoostLight = Color(0xFF8B5CF6)
    val BoostLightSoft = Color(0x1F8B5CF6)
    val FavLight = Color(0xFFE11D48)
    val FavLightSoft = Color(0x1AE11D48)
    val ReplyLight = Color(0xFFB45309)
    val ReplyLightSoft = Color(0x1AB45309)
    val TagLight = Color(0xFF0D9488)
    val LinkLight = Color(0xFF0369A1)
    val DangerLight = Color(0xFFDC2626)
    val DangerLightSoft = Color(0x1ADC2626)
    val MediaBgLight = Color(0xFF0D1117)
    val BlurhashBgLight = Color(0xFFE2E8F0)
}

data class OltnSemantics(
    val boost: Color,
    val boostSoft: Color,
    val fav: Color,
    val favSoft: Color,
    val reply: Color,
    val replySoft: Color,
    val tag: Color,
    val link: Color,
    val danger: Color,
    val dangerSoft: Color,
    val mediaBg: Color,
    val blurhashBg: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassBorder: Color
)

val DarkSemantics = OltnSemantics(
    boost = OltnPalette.Boost,
    boostSoft = OltnPalette.BoostSoft,
    fav = OltnPalette.Fav,
    favSoft = OltnPalette.FavSoft,
    reply = OltnPalette.Reply,
    replySoft = OltnPalette.ReplySoft,
    tag = OltnPalette.Tag,
    link = OltnPalette.Link,
    danger = OltnPalette.Danger,
    dangerSoft = OltnPalette.DangerSoft,
    mediaBg = OltnPalette.MediaBg,
    blurhashBg = OltnPalette.BlurhashBg,
    glass = OltnPalette.Glass,
    glassStrong = OltnPalette.GlassStrong,
    glassBorder = OltnPalette.GlassBorder
)

val LightSemantics = OltnSemantics(
    boost = OltnPalette.BoostLight,
    boostSoft = OltnPalette.BoostLightSoft,
    fav = OltnPalette.FavLight,
    favSoft = OltnPalette.FavLightSoft,
    reply = OltnPalette.ReplyLight,
    replySoft = OltnPalette.ReplyLightSoft,
    tag = OltnPalette.TagLight,
    link = OltnPalette.LinkLight,
    danger = OltnPalette.DangerLight,
    dangerSoft = OltnPalette.DangerLightSoft,
    mediaBg = OltnPalette.MediaBgLight,
    blurhashBg = OltnPalette.BlurhashBgLight,
    glass = OltnPalette.LightGlass,
    glassStrong = OltnPalette.LightGlassStrong,
    glassBorder = OltnPalette.LightGlassBorder
)

val LocalOltnSemantics = staticCompositionLocalOf { DarkSemantics }
