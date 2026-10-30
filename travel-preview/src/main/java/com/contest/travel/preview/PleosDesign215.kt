package com.contest.travel.preview

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Connect-S values from the two supplied 2.1.5 .fig files. See docs/design/pleos-215.md. */
internal object Pleos215 {
    val WindowPadding = 20.dp // display guide 2003:5556
    val ButtonLarge = 100.dp // design assets 67:21955
    val ButtonMedium = 80.dp // 67:22114
    val ButtonSmall = 64.dp // 67:22550
    val RadiusLarge = 32.dp // 67:21956
    val RadiusMedium = 24.dp // 67:22115
    val RadiusSmall = 16.dp // 67:22122
    val RadiusPopup = 48.dp // popup/system/radius_top_left 67:28787
    val PageNavigation = 80.dp // 67:27337
    val Icon = 48.dp // Iconography 67:14839
    val IconMedium = 40.dp // 67:22148
    val Positive = Color(0xFF0064FF) // informative/positive 67:22170
    val PositiveDark = Color(0xFF0082FF) // same token, Dark mode
    val Neutral = Color(0xFF131417) // text/dark/primary 67:5031
    val Filled = Color(0xFF44464E) // button/filled/enabled 67:22125
    val Gray400 = Color(0xFFA4A8AE) // Gray/400 67:5085
}

@OptIn(ExperimentalTextApi::class)
private val AstaSans = FontFamily(
    Font(R.font.asta_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.asta_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.asta_sans, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800)))
)

private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = AstaSans, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

internal val PleosTypography = Typography(
    displayLarge = type(60, 80, FontWeight.ExtraBold),
    displayMedium = type(56, 72, FontWeight.ExtraBold),
    displaySmall = type(48, 64, FontWeight.Bold),
    headlineLarge = type(40, 52, FontWeight.Bold),
    headlineMedium = type(36, 44, FontWeight.Bold),
    headlineSmall = type(32, 40, FontWeight.Bold),
    titleLarge = type(32, 40, FontWeight.Bold),
    titleMedium = type(30, 38, FontWeight.Bold),
    titleSmall = type(28, 36, FontWeight.Bold),
    bodyLarge = type(30, 38),
    bodyMedium = type(26, 34),
    bodySmall = type(24, 28),
    labelLarge = type(26, 34, FontWeight.Bold),
    labelMedium = type(24, 28),
    labelSmall = type(20, 24)
)

internal val PleosLight = lightColorScheme(
    primary = Pleos215.Filled, onPrimary = Color.White,
    background = Color(0xFFF7F8FA), surface = Color.White,
    onBackground = Pleos215.Neutral, onSurface = Pleos215.Neutral,
    surfaceVariant = Color(0xFFEDEEF2), onSurfaceVariant = Color(0xD6131417),
    primaryContainer = Color(0x1A131417), onPrimaryContainer = Pleos215.Neutral,
    secondaryContainer = Color(0x0D131417), onSecondaryContainer = Pleos215.Neutral,
    outline = Color(0x33131417), error = Color(0xFFFE3D16)
)

internal val PleosDark = darkColorScheme(
    primary = Color(0xFFE0E1E6), onPrimary = Pleos215.Neutral,
    background = Color(0xFF1D1E21), surface = Pleos215.Neutral,
    onBackground = Color.White, onSurface = Color.White,
    surfaceVariant = Color(0xFF313236), onSurfaceVariant = Color(0xD6FFFFFF),
    primaryContainer = Color(0x1AFFFFFF), onPrimaryContainer = Color.White,
    secondaryContainer = Color(0x0DFFFFFF), onSecondaryContainer = Color.White,
    outline = Color(0x33FFFFFF), error = Color(0xFFFF4C28)
)
