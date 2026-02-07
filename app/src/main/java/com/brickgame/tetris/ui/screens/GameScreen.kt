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
                when (layoutPreset) {
                    LayoutPreset.PORTRAIT_CLASSIC -> ClassicLayout(gameState, dpadStyle, ghostEnabled, animationStyle, animationDuration, onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame)
                    LayoutPreset.PORTRAIT_MODERN -> ModernLayout(gameState, dpadStyle, ghostEnabled, animationStyle, animationDuration, onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame)
                    LayoutPreset.PORTRAIT_FULLSCREEN -> FullscreenLayout(gameState, dpadStyle, ghostEnabled, animationStyle, animationDuration, onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame)
                    LayoutPreset.LANDSCAPE_DEFAULT -> LandscapeLayout(gameState, dpadStyle, ghostEnabled, animationStyle, animationDuration, onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, false)
                    LayoutPreset.LANDSCAPE_LEFTY -> LandscapeLayout(gameState, dpadStyle, ghostEnabled, animationStyle, animationDuration, onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, true)
                }

                if (gameState.status == GameStatus.PAUSED) PauseOverlay(onResume, onOpenSettings)
                if (gameState.status == GameStatus.GAME_OVER) GameOverOverlay(gameState.score, gameState.level, gameState.lines, onStartGame, onOpenSettings)
            }
        }

        // Floating action label
        if (gameState.lastActionLabel.isNotEmpty() && gameState.status == GameStatus.PLAYING) {
            Text(
                gameState.lastActionLabel,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.accentColor)
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = Color.Black
            )
        }
    }
}

// =============================================================================
// CLASSIC — Traditional handheld device look
// Score/Hold on left, Board in centre, Next on right, all in a "device" frame
// =============================================================================
@Composable
private fun ClassicLayout(
    gs: GameState, dpadStyle: DPadStyle, ghost: Boolean, anim: AnimationStyle, animDur: Float,
    onRotate: () -> Unit, onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onPause: () -> Unit, onSettings: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current

    Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Device frame containing board + sidebars
        Row(
            Modifier.fillMaxWidth().weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(theme.deviceColor)
                .padding(8.dp)
        ) {
            // Left: Hold + Score info
            Column(
                Modifier.width(60.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hold
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Tag("HOLD")
                    HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(50.dp))
                }
                // Score
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ScoreDisplay(gs.score, gs.level, gs.lines)
                }
                // Utility
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ActionButton("II", onPause, width = 42.dp, height = 26.dp)
                    ActionButton("...", onSettings, width = 42.dp, height = 26.dp)
                }
            }

            // Centre: Board
            GameBoard(
                gs.board, Modifier.weight(1f).fillMaxHeight().padding(horizontal = 4.dp),
                gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, animDur
            )

            // Right: Next queue
            Column(
                Modifier.width(60.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Tag("NEXT")
                gs.nextPieces.take(3).forEachIndexed { i, p ->
                    NextPiecePreview(
                        p.shape,
                        Modifier.size(when(i) { 0 -> 48.dp; 1 -> 38.dp; else -> 30.dp }).padding(2.dp),
                        when(i) { 0 -> 1f; 1 -> 0.55f; else -> 0.3f }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Controls
        ControlsRow(dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause, onStart, gs.status)
    }
}

// =============================================================================
// MODERN — Clean status bar on top, big board, compact controls
// =============================================================================
@Composable
private fun ModernLayout(
    gs: GameState, dpadStyle: DPadStyle, ghost: Boolean, anim: AnimationStyle, animDur: Float,
    onRotate: () -> Unit, onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onPause: () -> Unit, onSettings: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Status bar: compact, all info in one line
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(theme.deviceColor)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hold (small)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Tag("HOLD")
                HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(36.dp))
            }
            // Score (centred, prominent)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    gs.score.toString().padStart(7, '0'),
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 2.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tag("LV ${gs.level}")
                    Tag("${gs.lines} LINES")
                }
            }
            // Next (small, just first piece)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Tag("NEXT")
                NextPiecePreview(gs.nextPieces.firstOrNull()?.shape, Modifier.size(36.dp))
            }
            // Utility
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ActionButton("II", onPause, width = 36.dp, height = 22.dp)
                ActionButton("...", onSettings, width = 36.dp, height = 22.dp)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Large board
        GameBoard(
            gs.board, Modifier.weight(1f).aspectRatio(0.5f),
            gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, animDur
        )

        Spacer(Modifier.height(6.dp))

        // Controls
        ControlsRow(dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause, onStart, gs.status)
    }
}

// =============================================================================
// FULLSCREEN — Board fills screen, minimal floating HUD below board
// =============================================================================
@Composable
private fun FullscreenLayout(
    gs: GameState, dpadStyle: DPadStyle, ghost: Boolean, anim: AnimationStyle, animDur: Float,
    onRotate: () -> Unit, onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onPause: () -> Unit, onSettings: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current

    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Board fills all available space — NO overlapping elements
        GameBoard(
            gs.board, Modifier.weight(1f).fillMaxWidth(),
            gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, animDur
        )

        // Compact info strip between board and controls
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hold piece (tiny)
            HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(30.dp))
            // Score + Level
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(gs.score.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = theme.accentColor)
                Tag("LV${gs.level}")
            }
            // Next piece (tiny)
            NextPiecePreview(gs.nextPieces.firstOrNull()?.shape, Modifier.size(30.dp))
            // Settings
            ActionButton("...", onSettings, width = 32.dp, height = 22.dp)
        }

        // Controls
        ControlsRow(dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause, onStart, gs.status)
    }
}

// =============================================================================
// LANDSCAPE (Default + Lefty)
// =============================================================================
@Composable
private fun LandscapeLayout(
    gs: GameState, dpadStyle: DPadStyle, ghost: Boolean, anim: AnimationStyle, animDur: Float,
    onRotate: () -> Unit, onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onPause: () -> Unit, onSettings: () -> Unit, isLefty: Boolean
) {
    Row(Modifier.fillMaxSize().padding(6.dp)) {
        // Left panel
        Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) {
            if (isLefty) LandInfo(gs, onPause, onSettings)
            else LandControls(dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause)
        }

        // Board
        GameBoard(
            gs.board, Modifier.fillMaxHeight().aspectRatio(0.5f).padding(horizontal = 6.dp),
            gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, animDur
        )

        // Right panel
        Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) {
            if (isLefty) LandControls(dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause)
            else LandInfo(gs, onPause, onSettings)
        }
    }
}

@Composable
private fun LandInfo(gs: GameState, onPause: () -> Unit, onSettings: () -> Unit) {
    val theme = LocalGameTheme.current
    Column(Modifier.fillMaxHeight().padding(4.dp), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
        ScoreDisplay(gs.score, gs.level, gs.lines)
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(44.dp)) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Tag("NEXT")
            gs.nextPieces.take(3).forEachIndexed { i, p ->
                NextPiecePreview(p.shape, Modifier.size(when(i){0->40.dp;1->32.dp;else->26.dp}), when(i){0->1f;1->0.6f;else->0.35f})
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionButton("II", onPause, width = 48.dp, height = 28.dp)
            ActionButton("...", onSettings, width = 48.dp, height = 28.dp)
        }
    }
}

@Composable
private fun LandControls(
    dpadStyle: DPadStyle, onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onRotate: () -> Unit, onPause: () -> Unit
) {
    Column(Modifier.fillMaxHeight().padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        ActionButton("HOLD", onClick = onHold, width = 76.dp, height = 32.dp)
        DPad(48.dp, rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
            onUpPress = onHardDrop, onDownPress = onDownPress, onDownRelease = onDownRelease,
            onLeftPress = onLeftPress, onLeftRelease = onLeftRelease, onRightPress = onRightPress, onRightRelease = onRightRelease, onRotate = onRotate)
        if (dpadStyle == DPadStyle.STANDARD) RotateButton(onClick = onRotate, size = 56.dp)
        ActionButton("PAUSE", onClick = onPause, width = 76.dp, height = 32.dp)
    }
}

// =============================================================================
// SHARED Controls Row — D-Pad | HOLD+PAUSE | Rotate
// =============================================================================
@Composable
private fun ControlsRow(
    dpadStyle: DPadStyle,
    onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onRotate: () -> Unit, onPause: () -> Unit, onStart: () -> Unit, status: GameStatus
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        DPad(
            54.dp, rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
            onUpPress = onHardDrop, onDownPress = onDownPress, onDownRelease = onDownRelease,
            onLeftPress = onLeftPress, onLeftRelease = onLeftRelease,
            onRightPress = onRightPress, onRightRelease = onRightRelease, onRotate = onRotate
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("HOLD", onClick = onHold, width = 80.dp, height = 36.dp)
            ActionButton(
                if (status == GameStatus.MENU) "START" else "PAUSE",
                onClick = { if (status == GameStatus.MENU) onStart() else onPause() },
                width = 80.dp, height = 36.dp
            )
        }

        if (dpadStyle == DPadStyle.STANDARD)
            RotateButton(onClick = onRotate, size = 66.dp)
        else
            Spacer(Modifier.size(66.dp))
    }
}

// =============================================================================
// Shared Components
// =============================================================================
@Composable
private fun Tag(text: String) {
    val theme = LocalGameTheme.current
    Text(text, fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
}

@Composable
private fun ScoreDisplay(score: Int, level: Int, lines: Int) {
    val theme = LocalGameTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            score.toString().padStart(7, '0'),
            fontSize = 16.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 1.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("LV ${level}")
            Tag("${lines} L")
        }
    }
}

// =============================================================================
// Overlays
// =============================================================================
@Composable
private fun MenuOverlay(hs: Int, onStart: () -> Unit, onSettings: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(Modifier.fillMaxSize().background(theme.backgroundColor), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BRICK", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = theme.textPrimary, letterSpacing = 6.sp)
            Text("GAME", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = theme.accentColor, letterSpacing = 6.sp)
            Spacer(Modifier.height(8.dp))
            Text("v3.0.0", fontSize = 12.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(32.dp))
            if (hs > 0) {
                Text("BEST  $hs", fontSize = 18.sp, fontFamily = FontFamily.Monospace,
                    color = theme.accentColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
            }
            ActionButton("PLAY", onClick = onStart, width = 160.dp, height = 52.dp, backgroundColor = theme.accentColor)
            Spacer(Modifier.height(14.dp))
            ActionButton("SETTINGS", onClick = onSettings, width = 160.dp, height = 44.dp)
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSED", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = Color.White, letterSpacing = 4.sp)
            Spacer(Modifier.height(28.dp))
            ActionButton("RESUME", onClick = onResume, width = 160.dp, height = 48.dp)
            Spacer(Modifier.height(12.dp))
            ActionButton("SETTINGS", onClick = onSettings, width = 160.dp, height = 42.dp)
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, level: Int, lines: Int, onRestart: () -> Unit, onMenu: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444), letterSpacing = 4.sp)
            Text("OVER", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444), letterSpacing = 4.sp)
            Spacer(Modifier.height(20.dp))
            Text(score.toString(), fontSize = 28.sp, fontFamily = FontFamily.Monospace,
                color = theme.accentColor, fontWeight = FontWeight.Bold)
            Text("Level $level  ·  $lines Lines", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(28.dp))
            ActionButton("AGAIN", onClick = onRestart, width = 160.dp, height = 48.dp, backgroundColor = theme.accentColor)
            Spacer(Modifier.height(12.dp))
            ActionButton("MENU", onClick = onMenu, width = 160.dp, height = 42.dp)
        }
    }
}
