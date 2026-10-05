package com.brickgame.tetris.ui.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.R

/**
 * Brickwell's design system — one look for the whole app (menu, intro, profiles, settings, game
 * chrome). Deep navy ground, one neon accent per player, Chakra Petch for display text and
 * Manrope for everything else. Fonts are bundled (SIL Open Font License).
 */
object Bw {
    // Ground and surfaces
    val Ground = Color(0xFF0B0E1A)
    val Surface = Color(0xFF141A2E)
    val SurfaceDeep = Color(0xFF0F1426)
    val Raised = Color(0xFF1B2340)
    val Line = Color(0xFF26304D)
    val LineStrong = Color(0xFF33406A)

    // Text
    val Text = Color(0xFFEEF2FF)
    val TextSoft = Color(0xFFC7CEE6)
    val TextMuted = Color(0xFFA5B0CF)

    // Accents (also the player colours)
    val Cyan = Color(0xFF22D3EE)
    val Pink = Color(0xFFF472B6)
    val Lime = Color(0xFFA3E635)
    val Amber = Color(0xFFFBBF24)
    val Violet = Color(0xFFA78BFA)
    val Orange = Color(0xFFFB923C)
    val playerColors = listOf(Cyan, Pink, Lime, Amber, Violet, Orange)

    // Classic LCD (used where the menu previews the Classic mode)
    val LcdGreen = Color(0xFF9EAD86)
    val LcdInk = Color(0xFF1E2416)

    val Display = FontFamily(Font(R.font.chakra_petch_bold, FontWeight.Bold))
    val Body = FontFamily(
        Font(R.font.manrope_medium, FontWeight.Medium),
        Font(R.font.manrope_bold, FontWeight.Bold),
        Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
    )

    fun playerColor(index: Int): Color = playerColors[index.mod(playerColors.size)]
}

/** Text styles of the design system. */
object BwType {
    val Hero = TextStyle(fontFamily = Bw.Display, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp, color = Bw.Text)
    val Title = TextStyle(fontFamily = Bw.Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 33.sp, color = Bw.Text)
    val CardTitle = TextStyle(fontFamily = Bw.Display, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Bw.Text)
    val Wordmark = TextStyle(fontFamily = Bw.Display, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 6.sp)
    val Button = TextStyle(fontFamily = Bw.Display, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 3.sp)
    val Overline = TextStyle(fontFamily = Bw.Body, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 2.sp, color = Bw.TextMuted)
    val Body = TextStyle(fontFamily = Bw.Body, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, color = Bw.TextSoft)
    val Small = TextStyle(fontFamily = Bw.Body, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Bw.TextMuted)
    val Label = TextStyle(fontFamily = Bw.Body, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Bw.Text)
}

/** Press feedback shared by all design-system buttons: a quick scale-down. */
@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    return if (pressed) 0.96f else 1f
}

/** The one big call-to-action per screen. */
@Composable
fun BwPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Bw.Cyan,
    height: Dp = 60.dp,
    leading: (@Composable () -> Unit)? = null
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(accent)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(10.dp)) }
        Text(text, style = BwType.Button, color = Bw.Ground)
    }
}

/** Secondary action: surface fill with a hairline border. */
@Composable
fun BwSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(Bw.Surface)
            .border(1.dp, Bw.Line, RoundedCornerShape(16.dp))
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(8.dp)) }
        Text(text, style = BwType.Label)
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
    }
}

/** A selectable card (mode cards, profile rows, control choices). */
@Composable
fun BwCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Bw.Cyan,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(Bw.Surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) accent else Bw.Line, RoundedCornerShape(18.dp))
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Small filled tag, e.g. "LAST PLAYED". */
@Composable
fun BwTag(text: String, color: Color) {
    Text(text, Modifier.clip(RoundedCornerShape(6.dp)).background(color).padding(horizontal = 6.dp, vertical = 2.dp),
        style = TextStyle(fontFamily = Bw.Body, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 1.sp, color = Bw.Ground))
}

/** Segmented progress bar for multi-step flows. */
@Composable
fun BwSteps(count: Int, current: Int, modifier: Modifier = Modifier, accent: Color = Bw.Cyan) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= current) accent else Bw.Line))
        }
    }
}

/** Round avatar with the player's initial on their colour. */
@Composable
fun BwAvatar(name: String, color: Color, size: Dp = 32.dp, corner: Dp = size / 2) {
    Box(Modifier.size(size).clip(RoundedCornerShape(corner)).background(color), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase().ifEmpty { "?" },
            style = TextStyle(fontFamily = Bw.Display, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.45f).sp, color = Bw.Ground))
    }
}

/** The brand screens are always dark: light status/navigation icons while one is shown. */
@Composable
fun BwDarkSystemBars() {
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        val activity = view.context as? android.app.Activity
        val window = activity?.window
        if (window == null) onDispose {} else {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            val lightStatus = controller.isAppearanceLightStatusBars
            val lightNav = controller.isAppearanceLightNavigationBars
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
            onDispose {
                controller.isAppearanceLightStatusBars = lightStatus
                controller.isAppearanceLightNavigationBars = lightNav
            }
        }
    }
}
