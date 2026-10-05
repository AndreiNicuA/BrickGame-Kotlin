package com.brickgame.tetris.ui.brand

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.PlayStyle

/**
 * Main menu: "Welcome back, <player>", the three games as cards and one big PLAY.
 */
@Composable
fun MenuScreen(
    playerName: String,
    playerColor: Color,
    bestScore: Int,
    bestLevel: Int,
    style: PlayStyle,
    onSelectStyle: (PlayStyle) -> Unit,
    onPlay: () -> Unit,
    onSwitchPlayer: () -> Unit,
    onSettings: () -> Unit,
    onRecords: () -> Unit,
    onHowToPlay: () -> Unit
) {
    // Gentle entrance: content rises in once
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val enter by animateFloatAsState(if (shown) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "menuIn")

    Box(Modifier.fillMaxSize().background(Bw.Ground)) {
        DecorPiece(Modifier.align(Alignment.TopEnd).padding(top = 132.dp).offset(x = 36.dp), Bw.Violet)

        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .graphicsLayer { alpha = enter; translationY = (1f - enter) * 40f },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Scrolls on short screens; the PLAY buttons below always stay visible
          Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Top bar: player chip + settings
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clip(RoundedCornerShape(999.dp)).background(Bw.Surface)
                        .border(1.dp, Bw.Line, RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button, onClickLabel = "Switch player", onClick = onSwitchPlayer)
                        .heightIn(min = 44.dp).padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BwAvatar(playerName, playerColor, 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(playerName, style = BwType.Label)
                    Spacer(Modifier.width(6.dp))
                    BwIcon(BwIconKind.CHEVRON_DOWN, Bw.TextMuted, size = 14.dp)
                }
                Spacer(Modifier.weight(1f))
                SquareIconButton(BwIconKind.HELP, "How to play", onHowToPlay)
                Spacer(Modifier.width(8.dp))
                SquareIconButton(BwIconKind.SETTINGS, "Settings", onSettings)
            }

            // Welcome
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("BRICKWELL", style = BwType.Wordmark, color = playerColor)
                Text("Welcome back,", style = BwType.Body, color = Bw.TextMuted, modifier = Modifier.padding(top = 8.dp))
                Text(playerName, style = BwType.Hero)
                Text(
                    if (bestScore > 0) "Best ${"%,d".format(bestScore)}  ·  Level $bestLevel reached" else "No score yet — make the first one count",
                    style = BwType.Small, modifier = Modifier.padding(top = 6.dp)
                )
            }

            Text("CHOOSE YOUR WELL", style = BwType.Overline, modifier = Modifier.padding(top = 6.dp))

            ModeCard("Classic", "The pocket handheld, pixel for pixel", style == PlayStyle.CLASSIC, playerColor,
                { onSelectStyle(PlayStyle.CLASSIC) }) { ClassicThumb() }
            ModeCard("Neon", "Glow, combos, explosions", style == PlayStyle.NEON, playerColor,
                { onSelectStyle(PlayStyle.NEON) }) { NeonThumb() }
            ModeCard("3D Well", "Tilt your phone, walk around it", style == PlayStyle.THREE_D, playerColor,
                { onSelectStyle(PlayStyle.THREE_D) }) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Bw.Ground), contentAlignment = Alignment.Center) {
                    BwIcon(BwIconKind.CUBE, Bw.Violet, size = 40.dp)
                }
            }

          }

            BwPrimaryButton(
                "PLAY " + when (style) { PlayStyle.CLASSIC -> "CLASSIC"; PlayStyle.NEON -> "NEON"; PlayStyle.THREE_D -> "3D" },
                onPlay, Modifier.fillMaxWidth(), accent = playerColor, height = 64.dp,
                leading = { BwIcon(BwIconKind.PLAY, Bw.Ground, size = 20.dp) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BwSecondaryButton("Versus", {}, Modifier.weight(1f),
                    leading = { BwIcon(BwIconKind.SWORDS, Bw.Text, size = 18.dp) },
                    trailing = { Text("SOON", style = BwType.Overline.copy(fontSize = 10.sp)) })
                BwSecondaryButton("Records", onRecords, Modifier.weight(1f),
                    leading = { BwIcon(BwIconKind.CHART, Bw.Text, size = 18.dp) })
            }
        }
    }
}


@Composable
private fun SquareIconButton(kind: BwIconKind, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Bw.Surface)
            .border(1.dp, Bw.Line, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { BwIcon(kind, Bw.Text, size = 20.dp) }
}

@Composable
private fun ModeCard(
    title: String, subtitle: String, selected: Boolean, accent: Color,
    onClick: () -> Unit, thumb: @Composable () -> Unit
) {
    BwCard(selected, onClick, Modifier.fillMaxWidth(), accent = accent) {
        thumb()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = BwType.CardTitle)
                if (selected) { Spacer(Modifier.width(8.dp)); BwTag("SELECTED", accent) }
            }
            Text(subtitle, style = BwType.Small)
        }
        BwIcon(BwIconKind.CHEVRON_RIGHT, Bw.TextMuted, size = 18.dp)
    }
}

/** Classic thumbnail: a T piece on the LCD green. */
@Composable
private fun ClassicThumb() {
    Canvas(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Bw.LcdGreen)) {
        val c = size.width / 5.2f; val ox = (size.width - c * 3) / 2; val oy = (size.height - c * 2) / 2
        val cells = listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1)
        for ((x, y) in cells) {
            val o = Offset(ox + x * c + c * 0.08f, oy + y * c + c * 0.08f); val s = Size(c * 0.84f, c * 0.84f)
            drawRect(Bw.LcdInk, o, s)
            drawRect(Bw.LcdGreen, Offset(o.x + s.width * 0.16f, o.y + s.height * 0.16f), Size(s.width * 0.68f, s.height * 0.68f))
            drawRect(Bw.LcdInk, Offset(o.x + s.width * 0.3f, o.y + s.height * 0.3f), Size(s.width * 0.4f, s.height * 0.4f))
        }
    }
}

/** Neon thumbnail: coloured blocks. */
@Composable
private fun NeonThumb() {
    Canvas(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Bw.Ground)) {
        val c = size.width / 5.2f; val ox = (size.width - c * 3) / 2; val oy = (size.height - c * 2) / 2
        val cells = listOf(Triple(0, 0, Bw.Pink), Triple(1, 0, Bw.Pink), Triple(2, 0, Bw.Cyan), Triple(0, 1, Bw.Amber), Triple(1, 1, Bw.Pink), Triple(2, 1, Bw.Cyan))
        for ((x, y, col) in cells) {
            drawRoundRect(col, Offset(ox + x * c + c * 0.08f, oy + y * c + c * 0.08f), Size(c * 0.84f, c * 0.84f), CornerRadius(c * 0.2f))
        }
    }
}

/** Large faint T piece peeking in from the edge. */
@Composable
private fun DecorPiece(modifier: Modifier, color: Color) {
    Canvas(modifier.size(96.dp, 64.dp).graphicsLayer { alpha = 0.18f }) {
        val c = size.width / 3f
        for ((x, y) in listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1)) {
            drawRoundRect(color, Offset(x * c + 2f, y * c + 2f), Size(c - 4f, c - 4f), CornerRadius(c * 0.18f))
        }
    }
}
