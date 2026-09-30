package com.zaziapps.mtg_scanner.ui.themes

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.zaziapps.mtg_scanner.R

/**
 * Enum class representing thematic MTG color faction profiles for user interface customization.
 * Binds specific color space constraints with typography, structural layout shapes, and layout canvas textures.
 *
 * @property id The standard mechanical one-letter shorthand tracking code assigned by Scryfall rules.
 * @property displayName The user-facing textual name identifying the target faction landscape.
 * @property minHue The lower threshold boundary float mapping the faction color signature inside the HSV wheel.
 * @property maxHue The upper threshold boundary float mapping the faction color signature inside the HSV wheel.
 * @property titleColor The typography color configuration profile dedicated to primary title elements.
 * @property textColor The balanced text layout contrast color token assigned to guarantee typeface readability.
 * @property fontFamily The corporate structural font style configuration applied to interactive controls.
 * @property componentShape The design clipping shape applied to bounding boxes and component containers.
 * @property surfaceColor The internal backdrop window coloring binding nested components together.
 * @property borderColor The layout outline stroke color token applied to container boundary tracks.
 * @property accentColor The highlight illumination tone used for secondary visual signals.
 * @property backgroundPattern The raw image layout resource reference ID driving texture overlays.
 */
enum class ManaFaction(
    val id: String,
    val displayName: String,
    val minHue: Float,
    val maxHue: Float,
    val titleColor: Color,
    val textColor: Color,
    val fontFamily: FontFamily = FontFamily.Serif,
    val componentShape: Shape,
    val surfaceColor: Color,
    val borderColor: Color,
    val accentColor: Color,
    val backgroundPattern: Int,
) {
    WHITE(
        id = "W", displayName = "Plains",
        minHue = -1f, maxHue = -1f,
        titleColor = Color(0xFFF2E6D0), textColor = Color(0xFF1A1A1A),
        componentShape = RoundedCornerShape(4.dp),
        surfaceColor = Color(0xEEF5F2EB), borderColor = Color(0xFFD4AF37), accentColor = Color(0xFFD4AF37),
        backgroundPattern = R.drawable.white_bg
    ),
    BLUE(
        id = "U", displayName = "Island",
        minHue = 165f, maxHue = 255f,
        titleColor = Color(0xFF81D4FA), textColor = Color(0xFFFFFFFF),
        componentShape = RoundedCornerShape(24.dp),
        surfaceColor = Color(0xF40A192F), borderColor = Color(0xFF40E0D0), accentColor = Color(0xFF00B0FF),
        backgroundPattern = R.drawable.blue_bg
    ),
    BLACK(
        id = "B", displayName = "Swamp",
        minHue = -2f, maxHue = -2f,
        titleColor = Color(0xFFB388FF), textColor = Color(0xFFE0E0E0),
        componentShape = CutCornerShape(12.dp),
        surfaceColor = Color(0xFA0D0714), borderColor = Color(0xFF4A148C), accentColor = Color(0xFF7B1FA2),
        backgroundPattern = R.drawable.black_bg
    ),
    RED(
        id = "R", displayName = "Mountain",
        minHue = 0f, maxHue = 49f,
        titleColor = Color(0xFFFF5252), textColor = Color(0xFFFFFFFF),
        componentShape = CutCornerShape(0.dp),
        surfaceColor = Color(0xEE1A0505), borderColor = Color(0xFFFF3D00), accentColor = Color(0xFFFF9100),
        backgroundPattern = R.drawable.red_bg
    ),
    GREEN(
        id = "G", displayName = "Forest",
        minHue = 50f, maxHue = 165f,
        titleColor = Color(0xFFA5D6A7), textColor = Color(0xFFFFFFFF),
        componentShape = RoundedCornerShape(16.dp),
        surfaceColor = Color(0xF40C1A10), borderColor = Color(0xFF2E7D32), accentColor = Color(0xFF4CAF50),
        backgroundPattern = R.drawable.green_bg
    ),
    MULTICOLOR(
        id = "M", displayName = "Multicolor",
        minHue = -3f, maxHue = -3f,
        titleColor = Color(0xFFFFD700), textColor = Color(0xFFFFFFFF),
        componentShape = RoundedCornerShape(12.dp),
        surfaceColor = Color(0xF42A2115), borderColor = Color(0xFFB8860B), accentColor = Color(0xFFFFD700),
        backgroundPattern = R.drawable.main_background
    ),
    RED_HIGH(
        id = "R", displayName = "Mountain",
        minHue = 315f, maxHue = 360f,
        titleColor = Color(0xFFFF5252), textColor = Color(0xFFFFFFFF),
        componentShape = CutCornerShape(0.dp),
        surfaceColor = Color(0xEE1A0505), borderColor = Color(0xFFFF3D00), accentColor = Color(0xFFFF9100),
        backgroundPattern = R.drawable.red_bg
    )
}
