package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .systemBarsPadding() // FIX: respect status bar and nav bar
    ) {
        when {
            gameState.status == GameStatus.MENU -> MenuOverlay(gameState.highScore, onStartGame, onOpenSettings)
            else -> {
                if (layoutPreset.isLandscape) {
                    LandscapeLayout(
                        gameState, layoutPreset, dpadStyle, ghostEnabled, animationStyle, animationDuration,
                        onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease,
                        onRightPress, onRightRelease, onDownPress, onDownRelease,
                        onPause, onToggleSound, onOpenSettings
                    )
                } else {
                    // THREE DISTINCT portrait layouts
                    when (layoutPreset) {
                        LayoutPreset.PORTRAIT_CLASSIC -> PortraitClassic(
                            gameState, dpadStyle, ghostEnabled, animationStyle, animationDuration,
                            onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease,
                            onRightPress, onRightRelease, onDownPress, onDownRelease,
                            onPause, onToggleSound, onOpenSettings, onStartGame
                        )
                        LayoutPreset.PORTRAIT_MODERN -> PortraitModern(
                            gameState, dpadStyle, ghostEnabled, animationStyle, animationDuration,
                            onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease,
                            onRightPress, onRightRelease, onDownPress, onDownRelease,
                            onPause, onToggleSound, onOpenSettings, onStartGame
                        )
                        LayoutPreset.PORTRAIT_FULLSCREEN -> PortraitFullscreen(
                            gameState, dpadStyle, ghostEnabled, animationStyle, animationDuration,
                            onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease,
                            onRightPress, onRightRelease, onDownPress, onDownRelease,
                            onPause, onToggleSound, onOpenSettings, onStartGame
                        )
                        else -> {} // landscape handled above
                    }
                }

                if (gameState.status == GameStatus.PAUSED) PauseOverlay(onResume, onOpenSettings)
                if (gameState.status == GameStatus.GAME_OVER) GameOverOverlay(gameState.score, gameState.level, gameState.lines, onStartGame, onOpenSettings)
            }
        }

        // Action label
        if (gameState.lastActionLabel.isNotEmpty() && gameState.status == GameStatus.PLAYING) {
            Text(
                text = gameState.lastActionLabel,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.accentColor.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = Color.Black
            )
        }
    }
}

// =====================================================================
// PORTRAIT CLASSIC — device frame feel, info panels on sides of board
// =====================================================================
@Composable
private fun PortraitClassic(
    gs: GameState, dpadStyle: DPadStyle, ghost: Boolean,
    anim: AnimationStyle, animDur: Float,
    onRotate: () -> Unit, onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onPause: () -> Unit, onSound: () -> Unit, onSettings: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // === Device frame style: Hold | Board | Next in a single row ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // take all available space
                .clip(RoundedCornerShape(8.dp))
                .background(theme.deviceColor)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left sidebar: Hold + Score
            Column(
                modifier = Modifier.width(56.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Lbl("HOLD")
                    HoldPiecePreview(shape = gs.holdPiece?.shape, isUsed = gs.holdUsed, modifier = Modifier.size(48.dp))
                }
                // Score column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ScoreText(gs.score)
                    Spacer(Modifier.height(4.dp))
                    Lbl("LV ${gs.level}")
                    Lbl("${gs.lines} L")
                }
                // Small buttons
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SmBtn("⏸", onPause)
                    SmBtn("♪", onSound)
                    SmBtn("☰", onSettings)
                }
            }

            // Centre: Board
            GameBoard(
                board = gs.board, currentPiece = gs.currentPiece, ghostY = gs.ghostY,
                showGhost = ghost, clearingLines = gs.clearedLineRows,
                animationStyle = anim, animationDuration = animDur,
                modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 4.dp)
            )

            // Right sidebar: Next queue
            Column(
                modifier = Modifier.width(56.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Lbl("NEXT")
                gs.nextPieces.take(3).forEachIndexed { i, p ->
                    val sz = when (i) { 0 -> 46.dp; 1 -> 38.dp; else -> 32.dp }
                    val a = when (i) { 0 -> 1f; 1 -> 0.6f; else -> 0.35f }
                    NextPiecePreview(shape = p.shape, modifier = Modifier.size(sz).padding(2.dp), alpha = a)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // === Controls: DPad | HOLD+PAUSE | Rotate ===
        ControlsRow(dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause, onStart, gs.status)
    }
}

// =====================================================================
// PORTRAIT MODERN — compact status bar on top, maximized board
// =====================================================================
@Composable
private fun PortraitModern(
    gs: GameState, dpadStyle: DPadStyle, ghost: Boolean,
    anim: AnimationStyle, animDur: Float,
    onRotate: () -> Unit, onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onPause: () -> Unit, onSound: () -> Unit, onSettings: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // === Compact single-line status ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(theme.deviceColor)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hold (tiny)
            HoldPiecePreview(shape = gs.holdPiece?.shape, isUsed = gs.holdUsed, modifier = Modifier.size(36.dp))
            // Score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gs.score.toString().padStart(7, '0'), fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn)
                Text("LV${gs.level}  ${gs.lines}L", fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, color = theme.textSecondary, fontWeight = FontWeight.Bold)
            }
            // Next (just the first one, small)
            NextPiecePreview(shape = gs.nextPieces.firstOrNull()?.shape, modifier = Modifier.size(36.dp))
            // Utility
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                SmBtn("⏸", onPause)
                SmBtn("☰", onSettings)
            }
        }

        Spacer(Modifier.height(4.dp))

        // === Large board ===
        GameBoard(
            board = gs.board, currentPiece = gs.currentPiece, ghostY = gs.ghostY,
            showGhost = ghost, clearingLines = gs.clearedLineRows,
            animationStyle = anim, animationDuration = animDur,
            modifier = Modifier.weight(1f).aspectRatio(0.5f)
        )

        Spacer(Modifier.height(4.dp))

        // === Controls ===
        ControlsRow(dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause, onStart, gs.status)
    }
}

// =====================================================================
// PORTRAIT FULLSCREEN — minimal HUD, board as large as possible
// =====================================================================
@Composable
private fun PortraitFullscreen(
    gs: GameState, dpadStyle: DPadStyle, ghost: Boolean,
    anim: AnimationStyle, animDur: Float,
    onRotate: () -> Unit, onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onPause: () -> Unit, onSound: () -> Unit, onSettings: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // === Minimal floating HUD on top of board ===
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Board fills entire area
            GameBoard(
                board = gs.board, currentPiece = gs.currentPiece, ghostY = gs.ghostY,
                showGhost = ghost, clearingLines = gs.clearedLineRows,
                animationStyle = anim, animationDuration = animDur,
                modifier = Modifier.fillMaxSize()
            )

            // Floating score overlay (top centre)
            Text(
                text = "${gs.score}",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = theme.accentColor
            )

            // Floating level (top left)
            Text(
                text = "LV${gs.level}",
                modifier = Modifier.align(Alignment.TopStart).padding(top = 4.dp, start = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                color = theme.textSecondary, fontWeight = FontWeight.Bold
            )

            // Floating next piece (top right)
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                NextPiecePreview(shape = gs.nextPieces.firstOrNull()?.shape, modifier = Modifier.size(32.dp))
            }

            // Floating hold piece (bottom left)
            if (gs.holdPiece != null) {
                Box(
                    modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 4.dp, start = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    HoldPiecePreview(shape = gs.holdPiece?.shape, isUsed = gs.holdUsed, modifier = Modifier.size(32.dp))
                }
            }

            // Floating settings button (bottom right)
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 4.dp, end = 4.dp)) {
                SmBtn("☰", onSettings)
            }
        }

        Spacer(Modifier.height(2.dp))

        // === Controls (same row structure) ===
        ControlsRow(dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause, onStart, gs.status)
    }
}

// =====================================================================
// SHARED Controls Row — DPad | HOLD+PAUSE column | Rotate
// =====================================================================
@Composable
private fun ControlsRow(
    dpadStyle: DPadStyle,
    onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onRotate: () -> Unit, onPause: () -> Unit, onStart: () -> Unit,
    status: GameStatus
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // D-Pad
        DPad(
            buttonSize = 56.dp,
            rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
            onUpPress = onHardDrop, onDownPress = onDownPress, onDownRelease = onDownRelease,
            onLeftPress = onLeftPress, onLeftRelease = onLeftRelease,
            onRightPress = onRightPress, onRightRelease = onRightRelease,
            onRotate = onRotate
        )

        // Middle: HOLD + PAUSE stacked between DPad and Rotate
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WideActionButton("HOLD", onClick = onHold, width = 76.dp, height = 34.dp)
            WideActionButton(
                text = if (status == GameStatus.MENU) "START" else "PAUSE",
                onClick = { if (status == GameStatus.MENU) onStart() else onPause() },
                width = 76.dp, height = 34.dp
            )
        }

        // Rotate
        if (dpadStyle == DPadStyle.STANDARD)
            RotateButton(onClick = onRotate, size = 68.dp)
        else
            Spacer(Modifier.size(68.dp))
    }
}

// =====================================================================
// LANDSCAPE LAYOUTS (Default + Lefty)
// =====================================================================
@Composable
private fun LandscapeLayout(
    gs: GameState, layout: LayoutPreset, dpadStyle: DPadStyle, ghost: Boolean,
    anim: AnimationStyle, animDur: Float,
    onRotate: () -> Unit, onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onPause: () -> Unit, onSound: () -> Unit, onSettings: () -> Unit
) {
    val isLefty = layout == LayoutPreset.LANDSCAPE_LEFTY

    Row(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            if (isLefty) LandInfo(gs, onPause, onSound, onSettings)
            else LandControls(gs, dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause)
        }

        GameBoard(
            board = gs.board, currentPiece = gs.currentPiece, ghostY = gs.ghostY,
            showGhost = ghost, clearingLines = gs.clearedLineRows,
            animationStyle = anim, animationDuration = animDur,
            modifier = Modifier.fillMaxHeight().aspectRatio(0.5f).padding(horizontal = 6.dp)
        )

        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            if (isLefty) LandControls(gs, dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause)
            else LandInfo(gs, onPause, onSound, onSettings)
        }
    }
}

@Composable
private fun LandInfo(gs: GameState, onPause: () -> Unit, onSound: () -> Unit, onSettings: () -> Unit) {
    val theme = LocalGameTheme.current
    Column(
        Modifier.fillMaxHeight().padding(4.dp),
        Arrangement.SpaceEvenly, Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(theme.screenBackground.copy(alpha = 0.25f)).padding(8.dp, 4.dp)
        ) {
            Text(gs.score.toString().padStart(7, '0'), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 2.sp)
            Text("LV${gs.level}  ${gs.lines}L", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = theme.textSecondary, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Lbl("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(44.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Lbl("NEXT")
            gs.nextPieces.take(3).forEachIndexed { i, p ->
                NextPiecePreview(p.shape, Modifier.size(when(i){0->40.dp;1->32.dp;else->26.dp}), when(i){0->1f;1->0.6f;else->0.35f})
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { SmBtn("⏸", onPause); SmBtn("♪", onSound); SmBtn("☰", onSettings) }
    }
}

@Composable
private fun LandControls(
    gs: GameState, dpadStyle: DPadStyle,
    onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onRotate: () -> Unit, onPause: () -> Unit
) {
    Column(Alignment.CenterHorizontally, Arrangement.SpaceEvenly, Modifier.fillMaxHeight().padding(4.dp)) {
        WideActionButton("HOLD", onClick = onHold, width = 80.dp, height = 34.dp)
        DPad(50.dp, rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
            onUpPress = onHardDrop, onDownPress = onDownPress, onDownRelease = onDownRelease,
            onLeftPress = onLeftPress, onLeftRelease = onLeftRelease, onRightPress = onRightPress, onRightRelease = onRightRelease, onRotate = onRotate)
        if (dpadStyle == DPadStyle.STANDARD) RotateButton(onClick = onRotate, size = 60.dp)
        WideActionButton("PAUSE", onClick = onPause, width = 80.dp, height = 34.dp)
    }
}

// =====================================================================
// Helpers
// =====================================================================
@Composable private fun Lbl(t: String) {
    val theme = LocalGameTheme.current
    Text(t, fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 1.dp))
}

@Composable private fun ScoreText(score: Int) {
    val theme = LocalGameTheme.current
    Text(score.toString().padStart(7, '0'), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 1.sp)
}

@Composable private fun SmBtn(t: String, onClick: () -> Unit) {
    WideActionButton(t, onClick = onClick, width = 38.dp, height = 24.dp)
}

// =====================================================================
// Overlays
// =====================================================================
@Composable private fun MenuOverlay(hs: Int, onStart: () -> Unit, onSettings: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(Modifier.fillMaxSize().background(theme.backgroundColor), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BRICK GAME", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.textPrimary)
            Text("v3.0.0", fontSize = 14.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(24.dp))
            if (hs > 0) { Text("HIGH SCORE: $hs", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor); Spacer(Modifier.height(20.dp)) }
            WideActionButton("START GAME", onClick = onStart, width = 180.dp, height = 50.dp, backgroundColor = theme.accentColor)
            Spacer(Modifier.height(12.dp))
            WideActionButton("SETTINGS", onClick = onSettings, width = 180.dp, height = 44.dp)
        }
    }
}

@Composable private fun PauseOverlay(onResume: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSED", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color.White)
            Spacer(Modifier.height(24.dp))
            WideActionButton("RESUME", onClick = onResume, width = 160.dp, height = 46.dp)
            Spacer(Modifier.height(10.dp))
            WideActionButton("SETTINGS", onClick = onSettings, width = 160.dp, height = 40.dp)
        }
    }
}

@Composable private fun GameOverOverlay(score: Int, level: Int, lines: Int, onRestart: () -> Unit, onMenu: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444))
            Spacer(Modifier.height(16.dp))
            Text("Score: $score", fontSize = 20.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            Text("Level: $level  Lines: $lines", fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = Color.White)
            Spacer(Modifier.height(24.dp))
            WideActionButton("PLAY AGAIN", onClick = onRestart, width = 160.dp, height = 46.dp, backgroundColor = theme.accentColor)
            Spacer(Modifier.height(10.dp))
            WideActionButton("MENU", onClick = onMenu, width = 160.dp, height = 40.dp)
        }
    }
}
