package com.zaziapps.mtg_scanner.ui.themes

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Enum class representing thematic Magic: The Gathering color profiles for background gradients and text contrast layouts.
 * Pairs distinct mana identity archetypes with their respective typography shading markers and core palette tokens.
 *
 * @property textColor The balanced text layout contrast color token assigned to guarantee typeface accessibility.
 * @property mainColor The centralized base theme color used for solid asset blending and multi-gradient calculations.
 * @property gradient The preconfigured structural brush asset rendering localized field shading effects.
 */
enum class CardDetailBackground(
    val textColor: Color,
    val mainColor: Color,
    val gradient: Brush
) {
    UNCOLORED(
        textColor = Color.White,
        mainColor = Grey,
        gradient = Brush.verticalGradient(colors = listOf(LightBrown, Grey, DarkBrown))
    ),
    WHITE(
        textColor = Black,
        mainColor = Beige,
        gradient = Brush.verticalGradient(colors = listOf(MtgWhiteLight, Beige, MtgWhiteDark))
    ),
    BLUE(
        textColor = Color.White,
        mainColor = MtgBlueDark,
        gradient = Brush.radialGradient(colors = listOf(MtgBlueLight, MtgBlueMedium, MtgBlueDark))
    ),
    BLACK(
        textColor = Beige,
        mainColor = DarkBrown,
        gradient = Brush.verticalGradient(colors = listOf(MtgBlackLight, DarkBrown, Black))
    ),
    RED(
        textColor = Color.White,
        mainColor = DarkRed,
        gradient = Brush.linearGradient(colors = listOf(Red, DarkRed, MtgRedDarkest))
    ),
    GREEN(
        textColor = Color.White,
        mainColor = MtgGreenDark,
        gradient = Brush.verticalGradient(colors = listOf(MtgGreenLight, MtgGreenMedium, MtgGreenDark))
    )
}
