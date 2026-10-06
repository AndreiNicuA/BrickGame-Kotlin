package com.brickgame.tetris.ui.brand

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.PlayStyle
import com.brickgame.tetris.net.VersusLink

/** The Versus lobby: find a friend's phone nearby, then pick a game and start. */
@Composable
fun VersusScreen(
    myName: String,
    myColor: Color,
    phase: VersusLink.Phase,
    peer: VersusLink.Peer?,
    style: PlayStyle,
    wins: Int,
    losses: Int,
    countdown: Int,
    onSelectStyle: (PlayStyle) -> Unit,
    onSearch: () -> Unit,
    onStart: () -> Unit,
    onLeave: () -> Unit
) {
    BackHandler(onBack = onLeave)
    var permissionDenied by remember { mutableStateOf(false) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) { permissionDenied = false; onSearch() } else permissionDenied = true
    }
    // Start looking as soon as the lobby opens
    LaunchedEffect(Unit) {
        if (phase == VersusLink.Phase.IDLE || phase == VersusLink.Phase.DISCONNECTED || phase == VersusLink.Phase.FAILED) {
            permissions.launch(VersusLink.requiredPermissions())
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Bw.Ground).safeDrawingPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Bw.Surface).border(1.dp, Bw.Line, RoundedCornerShape(14.dp))
                .clickable(onClickLabel = "Back", onClick = onLeave), contentAlignment = Alignment.Center) {
                BwIcon(BwIconKind.CHEVRON_LEFT, Bw.Text)
            }
            Spacer(Modifier.width(14.dp))
            Text("Versus", style = BwType.Title.copy(fontSize = 28.sp))
        }
        Text("Two phones, same room. Clear lines to send garbage to your friend — last one standing wins. No internet needed.",
            style = BwType.Body)

        // Me vs them
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            PlayerBadge(myName, myColor, "YOU")
            Text(if (wins + losses > 0) "$wins – $losses" else "VS", style = BwType.Title.copy(color = Bw.TextMuted))
            if (peer != null) PlayerBadge(peer.name, Bw.Pink, "FRIEND") else SearchingBadge(phase == VersusLink.Phase.SEARCHING)
        }

        // Status card
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Bw.Surface).border(1.dp, Bw.Line, RoundedCornerShape(18.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val (title, body) = when {
                permissionDenied -> "Permission needed" to "Brickwell needs the Nearby devices permission to find your friend's phone. Allow it and try again."
                phase == VersusLink.Phase.SEARCHING -> "Looking for your friend…" to "Open Brickwell → Versus on the other phone and keep both phones close."
                phase == VersusLink.Phase.CONNECTING -> "Connecting…" to "Almost there."
                phase == VersusLink.Phase.CONNECTED -> "Connected" to "Both phones should show the code ${peer?.code ?: ""}. Pick a game — whoever taps START starts it on both phones."
                phase == VersusLink.Phase.DISCONNECTED -> "Connection lost" to "Tap Search again to reconnect."
                phase == VersusLink.Phase.FAILED -> "Couldn't search" to "Check that Bluetooth and Wi-Fi are on (and Location on older phones), then search again."
                else -> "Ready" to "Tap Search to find a friend nearby."
            }
            Text(title, style = BwType.CardTitle)
            Text(body, style = BwType.Small)
        }

        if (phase == VersusLink.Phase.CONNECTED) {
            Text("GAME", style = BwType.Overline)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(PlayStyle.CLASSIC to "Classic", PlayStyle.NEON to "Neon", PlayStyle.THREE_D to "3D").forEach { (st, label) ->
                    val on = st == style
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (on) Bw.Cyan else Bw.Surface).border(1.dp, if (on) Bw.Cyan else Bw.Line, RoundedCornerShape(14.dp))
                        .clickable { onSelectStyle(st) }, contentAlignment = Alignment.Center) {
                        Text(label, style = BwType.Label.copy(color = if (on) Bw.Ground else Bw.Text))
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        when (phase) {
            VersusLink.Phase.CONNECTED -> BwPrimaryButton("START", onStart, Modifier.fillMaxWidth(), height = 60.dp,
                leading = { BwIcon(BwIconKind.PLAY, Bw.Ground, size = 18.dp) })
            VersusLink.Phase.SEARCHING, VersusLink.Phase.CONNECTING -> {}
            else -> BwPrimaryButton("SEARCH", { permissions.launch(VersusLink.requiredPermissions()) }, Modifier.fillMaxWidth(), height = 60.dp)
        }
        BwSecondaryButton("Leave Versus", onLeave, Modifier.fillMaxWidth())
    }
    if (countdown > 0) CountdownOverlay(countdown, style)
    }
}

/** Big 3-2-1 on both phones before a round, so nobody gets a head start. */
@Composable
private fun CountdownOverlay(n: Int, style: PlayStyle) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(n) { pop.snapTo(0f); pop.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
    Box(Modifier.fillMaxSize().background(Bw.Ground.copy(alpha = 0.92f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(when (style) { PlayStyle.CLASSIC -> "CLASSIC"; PlayStyle.NEON -> "NEON"; PlayStyle.THREE_D -> "3D" } + " VERSUS",
                style = BwType.Overline)
            Text("$n", style = BwType.Hero.copy(fontSize = 120.sp, lineHeight = 124.sp, color = Bw.Cyan),
                modifier = Modifier.graphicsLayer {
                    val k = pop.value
                    scaleX = 1.6f - 0.6f * k; scaleY = scaleX; alpha = k
                })
            Text("Get ready", style = BwType.Body)
        }
    }
}

@Composable
private fun PlayerBadge(name: String, color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BwAvatar(name, color, 64.dp, corner = 20.dp)
        Text(name, style = BwType.Label)
        Text(label, style = BwType.Overline.copy(fontSize = 10.sp))
    }
}

@Composable
private fun SearchingBadge(active: Boolean) {
    val t = rememberInfiniteTransition(label = "radar").animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "r")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(64.dp)) {
            drawCircle(Bw.Line, size.minDimension / 2.2f, style = Stroke(2f))
            if (active) {
                val v = t.value
                drawCircle(Bw.Cyan.copy(alpha = 1f - v), size.minDimension / 2.2f * v, style = Stroke(3f))
            }
            drawCircle(Bw.TextMuted, 4f, Offset(size.width / 2, size.height / 2))
        }
        Text("…", style = BwType.Label)
        Text("FRIEND", style = BwType.Overline.copy(fontSize = 10.sp))
    }
}

/** Opponent strip shown on top of the game during a Versus round. */
@Composable
fun VersusHud(opponent: String, score: Int, lines: Int, received: Int, sent: Int, modifier: Modifier = Modifier, incoming: Int = 0) {
    // Flash when garbage arrives
    var lastReceived by remember { mutableIntStateOf(received) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(received) {
        if (received > lastReceived) { flash.snapTo(1f); flash.animateTo(0f, tween(700)) }
        lastReceived = received
    }
    Row(modifier.clip(RoundedCornerShape(999.dp)).background(Bw.Surface.copy(alpha = 0.94f))
        .border(1.dp, Bw.Line, RoundedCornerShape(999.dp))
        .drawBehind { drawRect(Bw.Pink.copy(alpha = flash.value * 0.45f)) }
        .padding(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BwAvatar(opponent, Bw.Pink, 26.dp)
        Text("%,d".format(score), style = BwType.Label)
        Text("$lines L", style = BwType.Small)
        if (sent > 0) Text("↑$sent", style = BwType.Small.copy(color = Bw.Lime))
        if (received > 0) Text("↓$received", style = BwType.Small.copy(color = Bw.Pink))
        if (incoming > 0) Text("⚠ +$incoming", style = BwType.Label.copy(color = Bw.Amber))
    }
}

/**
 * Warning meter on the screen edge: garbage that has arrived and will rise under your stack
 * when the next piece appears. One segment per row/layer, pulsing.
 */
@Composable
fun IncomingGarbageBar(rows: Int, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "incoming")
        .animateFloat(0.5f, 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "p")
    Column(modifier.padding(start = 3.dp).graphicsLayer { alpha = pulse },
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(rows.coerceAtMost(12)) {
            Box(Modifier.size(7.dp, 18.dp).clip(RoundedCornerShape(3.dp)).background(if (rows >= 4) Bw.Pink else Bw.Amber))
        }
    }
}

/** Round result on top of the game: who won, the running score, rematch / back. */
@Composable
fun VersusResultOverlay(won: Boolean, dropped: Boolean, opponent: String, wins: Int, losses: Int, connected: Boolean,
                        onRematch: () -> Unit, onLobby: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Bw.Ground.copy(alpha = 0.94f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 360.dp).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                dropped -> {
                    Text("CONNECTION LOST", style = BwType.Hero.copy(color = Bw.Amber, fontSize = 32.sp, lineHeight = 36.sp))
                    Text("$opponent left or went out of range. This round doesn't count.", style = BwType.Small)
                }
                won -> Text("YOU WIN!", style = BwType.Hero.copy(color = Bw.Lime))
                else -> Text("$opponent WINS", style = BwType.Hero.copy(color = Bw.Pink))
            }
            Text("$wins – $losses", style = BwType.Title.copy(color = Bw.TextMuted))
            Spacer(Modifier.height(8.dp))
            if (connected) BwPrimaryButton("REMATCH", onRematch, Modifier.fillMaxWidth(), height = 60.dp)
            else if (!dropped) Text("Your friend disconnected.", style = BwType.Small)
            BwSecondaryButton("Back to lobby", onLobby, Modifier.fillMaxWidth())
        }
    }
}
