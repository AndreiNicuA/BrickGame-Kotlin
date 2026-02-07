package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.*
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.layout.DPadStyle
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.theme.LocalGameTheme

@Composable
fun GameScreen(
    gameState: GameState,
    layoutPreset: LayoutPreset,
    dpadStyle: DPadStyle,
    ghostEnabled: Boolean,
    animationStyle: AnimationStyle,
    animationDuration: Float,
    onStartGame: () -> Unit, onPause: () -> Unit, onResume: () -> Unit,
    onRotate: () -> Unit, onRotateCCW: () -> Unit,
    onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onOpenSettings: () -> Unit, onToggleSound: () -> Unit
) {
    val theme = LocalGameTheme.current

    Box(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
        when {
            gameState.status == GameStatus.MENU -> MenuOverlay(gameState.highScore, onStartGame, onOpenSettings)
            else -> {
                val gs = gameState
                val cbs = Controls(onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause, onOpenSettings, onToggleSound, onStartGame)

                if (layoutPreset.isLandscape) {
                    LandscapeGame(gs, layoutPreset, dpadStyle, ghostEnabled, animationStyle, animationDuration, cbs)
                } else {
                    when (layoutPreset) {
                        LayoutPreset.PORTRAIT_CLASSIC -> ClassicLayout(gs, dpadStyle, ghostEnabled, animationStyle, animationDuration, cbs)
                        LayoutPreset.PORTRAIT_MODERN -> ModernLayout(gs, dpadStyle, ghostEnabled, animationStyle, animationDuration, cbs)
                        LayoutPreset.PORTRAIT_FULLSCREEN -> FullscreenLayout(gs, dpadStyle, ghostEnabled, animationStyle, animationDuration, cbs)
                        else -> {}
                    }
                }

                if (gs.status == GameStatus.PAUSED) PauseOverlay(onResume, onOpenSettings)
                if (gs.status == GameStatus.GAME_OVER) GameOverOverlay(gs.score, gs.level, gs.lines, onStartGame, onOpenSettings)
            }
        }

        // Floating action label
        if (gameState.lastActionLabel.isNotEmpty() && gameState.status == GameStatus.PLAYING) {
            Text(
                text = gameState.lastActionLabel,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.accentColor)
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = Color.Black
            )
        }
    }
}

// Callback bundle to reduce parameter passing
private data class Controls(
    val hardDrop: () -> Unit, val hold: () -> Unit,
    val leftPress: () -> Unit, val leftRelease: () -> Unit,
    val rightPress: () -> Unit, val rightRelease: () -> Unit,
    val downPress: () -> Unit, val downRelease: () -> Unit,
    val rotate: () -> Unit, val pause: () -> Unit,
    val settings: () -> Unit, val sound: () -> Unit,
    val start: () -> Unit
)

// ======================================================================
//  PORTRAIT CLASSIC — Handheld device look
//  Info bar on top, board in centre, controls at bottom
// ======================================================================
@Composable
private fun ClassicLayout(
    gs: GameState, dpadStyle: DPadStyle, ghost: Boolean,
    anim: AnimationStyle, animDur: Float, cb: Controls
) {
    val theme = LocalGameTheme.current

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // === Top info bar ===
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.deviceColor)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hold
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Lbl("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(42.dp))
            }
            // Score block
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScoreLCD(gs.score)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatChip("LV ${gs.level}")
                    StatChip("${gs.lines} L")
                }
            }
            // Next
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Lbl("NEXT"); NextPiecePreview(gs.nextPieces.firstOrNull()?.shape, Modifier.size(42.dp))
            }
        }

        Spacer(Modifier.height(4.dp))

        // === Board ===
        GameBoard(
            board = gs.board, currentPiece = gs.currentPiece, ghostY = gs.ghostY,
            showGhost = ghost, clearingLines = gs.clearedLineRows,
            animationStyle = anim, animationDuration = animDur,
            modifier = Modifier.weight(1f).aspectRatio(0.5f)
        )

        Spacer(Modifier.height(4.dp))

        // === Next queue (small, under board) ===
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            gs.nextPieces.drop(1).take(2).forEachIndexed { i, p ->
                NextPiecePreview(p.shape, Modifier.size(32.dp), alpha = if (i == 0) 0.5f else 0.3f)
            }
        }

        Spacer(Modifier.height(6.dp))

        // === Controls ===
        ControlsBlock(dpadStyle, gs.status, cb)
    }
}

// ======================================================================
//  PORTRAIT MODERN — Score integrated into minimal top strip, max board
// ======================================================================
@Composable
private fun ModernLayout(
    gs: GameState, dpadStyle: DPadStyle, ghost: Boolean,
    anim: AnimationStyle, animDur: Float, cb: Controls
) {
    val theme = LocalGameTheme.current

    Column(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // === Minimal top strip ===
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(theme.deviceColor)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(34.dp))

            // Score inline
            Text(
                gs.score.toString().padStart(7, '0'),
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 2.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("LV${gs.level}")
                StatChip("${gs.lines}L")
            }

            // Next queue compact
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                gs.nextPieces.take(3).forEachIndexed { i, p ->
                    NextPiecePreview(p.shape, Modifier.size(when(i){ 0->34.dp; 1->28.dp; else->24.dp }),
                        alpha = when(i){ 0->1f; 1->0.5f; else->0.3f })
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // === Board — maximum space ===
        GameBoard(
            board = gs.board, currentPiece = gs.currentPiece, ghostY = gs.ghostY,
            showGhost = ghost, clearingLines = gs.clearedLineRows,
            animationStyle = anim, animationDuration = animDur,
            modifier = Modifier.weight(1f).aspectRatio(0.5f)
        )

        Spacer(Modifier.height(4.dp))

        // === Controls ===
        ControlsBlock(dpadStyle, gs.status, cb)
    }
}

// ======================================================================
//  PORTRAIT FULLSCREEN — Board fills everything, info outside board area
// ======================================================================
@Composable
private fun FullscreenLayout(
    gs: GameState, dpadStyle: DPadStyle, ghost: Boolean,
    anim: AnimationStyle, animDur: Float, cb: Controls
) {
    val theme = LocalGameTheme.current

    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // === Tiny info row — NO overlap with board ===
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hold (tiny)
            HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(28.dp))
            // Score minimal
            Text(
                gs.score.toString().padStart(7, '0'),
                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = theme.pixelOn.copy(alpha = 0.7f)
            )
            Text("LV${gs.level}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = theme.textSecondary)
            // Next (tiny, just first)
            NextPiecePreview(gs.nextPieces.firstOrNull()?.shape, Modifier.size(28.dp))
            // Settings button
            ActionButton("☰", cb.settings, width = 36.dp, height = 28.dp)
        }

        // === Board fills remaining space ===
        GameBoard(
            board = gs.board, currentPiece = gs.currentPiece, ghostY = gs.ghostY,
            showGhost = ghost, clearingLines = gs.clearedLineRows,
            animationStyle = anim, animationDuration = animDur,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        Spacer(Modifier.height(2.dp))

        // === Controls ===
        ControlsBlock(dpadStyle, gs.status, cb)
    }
}

// ======================================================================
//  SHARED CONTROLS BLOCK — DPad | HOLD+PAUSE | Rotate
//  Consistent across all layouts. Clean, centered, accessible.
// ======================================================================
@Composable
private fun ControlsBlock(dpadStyle: DPadStyle, status: GameStatus, cb: Controls) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // D-Pad (left side)
        DPad(
            buttonSize = 56.dp,
            rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
            onUpPress = cb.hardDrop, onDownPress = cb.downPress, onDownRelease = cb.downRelease,
            onLeftPress = cb.leftPress, onLeftRelease = cb.leftRelease,
            onRightPress = cb.rightPress, onRightRelease = cb.rightRelease,
            onRotate = cb.rotate
        )

        // Middle column: HOLD + action buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActionButton("HOLD", cb.hold, width = 82.dp, height = 36.dp)
            ActionButton(
                if (status == GameStatus.MENU) "START" else "PAUSE",
                { if (status == GameStatus.MENU) cb.start() else cb.pause() },
                width = 82.dp, height = 36.dp
            )
        }

        // Rotate (right side)
        if (dpadStyle == DPadStyle.STANDARD)
            RotateButton(onClick = cb.rotate, size = 68.dp)
        else
            Spacer(Modifier.size(68.dp))
    }
}

// ======================================================================
//  LANDSCAPE
// ======================================================================
@Composable
private fun LandscapeGame(
    gs: GameState, layout: LayoutPreset, dpadStyle: DPadStyle, ghost: Boolean,
    anim: AnimationStyle, animDur: Float, cb: Controls
) {
    val isLefty = layout == LayoutPreset.LANDSCAPE_LEFTY

    Row(Modifier.fillMaxSize().padding(6.dp)) {
        Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) {
            if (isLefty) LandInfo(gs, cb) else LandControls(dpadStyle, gs.status, cb)
        }
        GameBoard(
            board = gs.board, currentPiece = gs.currentPiece, ghostY = gs.ghostY,
            showGhost = ghost, clearingLines = gs.clearedLineRows,
            animationStyle = anim, animationDuration = animDur,
            modifier = Modifier.fillMaxHeight().aspectRatio(0.5f).padding(horizontal = 6.dp)
        )
        Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) {
            if (isLefty) LandControls(dpadStyle, gs.status, cb) else LandInfo(gs, cb)
        }
    }
}

@Composable
private fun LandInfo(gs: GameState, cb: Controls) {
    val theme = LocalGameTheme.current
    Column(Modifier.fillMaxHeight().padding(4.dp), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(theme.deviceColor).padding(10.dp, 6.dp)) {
            ScoreLCD(gs.score)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatChip("LV${gs.level}"); StatChip("${gs.lines}L") }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Lbl("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(40.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Lbl("NEXT")
            gs.nextPieces.take(3).forEachIndexed { i, p ->
                NextPiecePreview(p.shape, Modifier.size(when(i){0->36.dp;1->28.dp;else->24.dp}).padding(1.dp),
                    alpha = when(i){0->1f;1->0.5f;else->0.3f})
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton("⏸", cb.pause, width = 36.dp, height = 30.dp)
            ActionButton("☰", cb.settings, width = 36.dp, height = 30.dp)
        }
    }
}

@Composable
private fun LandControls(dpadStyle: DPadStyle, status: GameStatus, cb: Controls) {
    Column(modifier = Modifier.fillMaxHeight().padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        ActionButton("HOLD", cb.hold, width = 80.dp, height = 34.dp)
        DPad(50.dp, rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
            onUpPress = cb.hardDrop, onDownPress = cb.downPress, onDownRelease = cb.downRelease,
            onLeftPress = cb.leftPress, onLeftRelease = cb.leftRelease,
            onRightPress = cb.rightPress, onRightRelease = cb.rightRelease, onRotate = cb.rotate)
        if (dpadStyle == DPadStyle.STANDARD) RotateButton(onClick = cb.rotate, size = 60.dp)
        ActionButton(if (status == GameStatus.MENU) "START" else "PAUSE",
            { if (status == GameStatus.MENU) cb.start() else cb.pause() }, width = 80.dp, height = 34.dp)
    }
}

// ======================================================================
//  REUSABLE UI ELEMENTS
// ======================================================================
@Composable
private fun Lbl(t: String) {
    val theme = LocalGameTheme.current
    Text(t, fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 2.dp), letterSpacing = 1.sp)
}

@Composable
private fun ScoreLCD(score: Int) {
    val theme = LocalGameTheme.current
    Text(score.toString().padStart(7, '0'),
        fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        color = theme.pixelOn, letterSpacing = 2.sp)
}

@Composable
private fun StatChip(text: String) {
    val theme = LocalGameTheme.current
    Text(
        text, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        color = theme.textSecondary, letterSpacing = 1.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(theme.screenBackground.copy(alpha = 0.3f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// ======================================================================
//  OVERLAYS
// ======================================================================
@Composable
private fun MenuOverlay(hs: Int, onStart: () -> Unit, onSettings: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(Modifier.fillMaxSize().background(theme.backgroundColor), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BRICK", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                color = theme.textPrimary, letterSpacing = 8.sp)
            Text("GAME", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                color = theme.accentColor, letterSpacing = 8.sp)
            Spacer(Modifier.height(8.dp))
            Text("v3.0.0", fontSize = 12.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(32.dp))
            if (hs > 0) {
                Text("BEST  $hs", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
            }
            ActionButton("PLAY", onStart, width = 160.dp, height = 52.dp, backgroundColor = theme.accentColor)
            Spacer(Modifier.height(14.dp))
            ActionButton("SETTINGS", onSettings, width = 160.dp, height = 44.dp)
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSED", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                color = Color.White, letterSpacing = 4.sp)
            Spacer(Modifier.height(28.dp))
            ActionButton("RESUME", onResume, width = 160.dp, height = 48.dp)
            Spacer(Modifier.height(12.dp))
            ActionButton("SETTINGS", onSettings, width = 160.dp, height = 42.dp)
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, level: Int, lines: Int, onRestart: () -> Unit, onMenu: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                color = Color(0xFFFF4444), letterSpacing = 4.sp)
            Text("OVER", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                color = Color(0xFFFF4444), letterSpacing = 4.sp)
            Spacer(Modifier.height(20.dp))
            Text(score.toString(), fontSize = 28.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor, fontWeight = FontWeight.Bold)
            Text("LV$level   ${lines}L", fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(28.dp))
            ActionButton("AGAIN", onRestart, width = 160.dp, height = 48.dp, backgroundColor = theme.accentColor)
            Spacer(Modifier.height(12.dp))
            ActionButton("MENU", onMenu, width = 160.dp, height = 42.dp)
        }
    }
}
