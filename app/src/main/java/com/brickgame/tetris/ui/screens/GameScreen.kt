package com.brickgame.tetris.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.brickgame.tetris.game.*
import com.brickgame.tetris.data.CustomLayoutData
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
    customLayout: CustomLayoutData? = null,
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
                if (customLayout != null) {
                    CustomLayout(gameState, dpadStyle, ghostEnabled, animationStyle, animationDuration, customLayout, onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame)
                } else when (layoutPreset) {
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
        // Big centered action popup
        ActionPopup(gameState.lastActionLabel, gameState.linesCleared)
    }
}

// === CLASSIC: Device frame — Hold|Board|Next side by side ===
@Composable private fun ClassicLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current
    Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Device frame
        Row(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)).background(theme.deviceColor).padding(6.dp)) {
            // Left: Hold + Score
            Column(Modifier.width(58.dp).fillMaxHeight(), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(48.dp)) }
                ScoreBlock(gs.score, gs.level, gs.lines)
            }
            // Board
            GameBoard(gs.board, Modifier.weight(1f).fillMaxHeight().padding(horizontal = 3.dp), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad)
            // Right: Next
            Column(Modifier.width(58.dp).fillMaxHeight(), Arrangement.Top, Alignment.CenterHorizontally) {
                Tag("NEXT")
                gs.nextPieces.take(3).forEachIndexed { i, p ->
                    NextPiecePreview(p.shape, Modifier.size(when(i){0->46.dp;1->36.dp;else->28.dp}).padding(1.dp), when(i){0->1f;1->0.55f;else->0.3f})
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        FullControls(dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause, onSet, onStart, gs.status)
    }
}

// === MODERN: Compact info bar + big board ===
@Composable private fun ModernLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Compact status bar
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(theme.deviceColor).padding(horizontal = 10.dp, vertical = 6.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(34.dp)) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gs.score.toString().padStart(7, '0'), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Tag("LV ${gs.level}"); Tag("${gs.lines} LINES") }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("NEXT"); NextPiecePreview(gs.nextPieces.firstOrNull()?.shape, Modifier.size(34.dp)) }
        }
        Spacer(Modifier.height(4.dp))
        GameBoard(gs.board, Modifier.weight(1f).aspectRatio(0.5f), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad)
        Spacer(Modifier.height(6.dp))
        FullControls(dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause, onSet, onStart, gs.status)
    }
}

// === FULLSCREEN: Max board, tiny info strip below ===
@Composable private fun FullscreenLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current
    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        GameBoard(gs.board, Modifier.weight(1f).fillMaxWidth(), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad)
        // Tiny info strip
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(28.dp))
            Text(gs.score.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            Tag("LV${gs.level}")
            NextPiecePreview(gs.nextPieces.firstOrNull()?.shape, Modifier.size(28.dp))
        }
        FullControls(dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause, onSet, onStart, gs.status)
    }
}

// === LANDSCAPE ===
@Composable private fun LandscapeLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, lefty: Boolean
) {
    Row(Modifier.fillMaxSize().padding(6.dp)) {
        Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) { if (lefty) LandInfo(gs, onPause, onSet) else LandCtrl(dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause) }
        GameBoard(gs.board, Modifier.fillMaxHeight().aspectRatio(0.5f).padding(horizontal = 6.dp), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad)
        Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) { if (lefty) LandCtrl(dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause) else LandInfo(gs, onPause, onSet) }
    }
}

@Composable private fun LandInfo(gs: GameState, onPause: () -> Unit, onSet: () -> Unit) {
    val theme = LocalGameTheme.current
    Column(Modifier.fillMaxHeight().padding(4.dp), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
        ScoreBlock(gs.score, gs.level, gs.lines)
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(44.dp)) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Tag("NEXT")
            gs.nextPieces.take(3).forEachIndexed { i, p -> NextPiecePreview(p.shape, Modifier.size(when(i){0->40.dp;1->32.dp;else->26.dp}), when(i){0->1f;1->0.6f;else->0.35f}) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { ActionButton("PAUSE", onPause, width = 64.dp, height = 28.dp); ActionButton("...", onSet, width = 36.dp, height = 28.dp) }
    }
}

@Composable private fun LandCtrl(
    dp: DPadStyle, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onRotate: () -> Unit, onPause: () -> Unit
) {
    Column(Modifier.fillMaxHeight().padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        ActionButton("HOLD", onHold, width = 76.dp, height = 32.dp)
        DPad(48.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE, onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR, onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 56.dp)
        ActionButton("PAUSE", onPause, width = 76.dp, height = 32.dp)
    }
}

// === SHARED: Full controls row (ALL buttons at bottom) ===
@Composable private fun FullControls(
    dp: DPadStyle, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onRotate: () -> Unit,
    onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit, status: GameStatus
) {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        // D-Pad
        DPad(54.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
            onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
            onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        // Centre: HOLD + PAUSE/START + SETTINGS
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton("HOLD", onHold, width = 78.dp, height = 34.dp)
            ActionButton(
                if (status == GameStatus.MENU) "START" else "PAUSE",
                { if (status == GameStatus.MENU) onStart() else onPause() },
                width = 78.dp, height = 34.dp
            )
            ActionButton("...", onSet, width = 46.dp, height = 24.dp)
        }
        // Rotate
        if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 66.dp) else Spacer(Modifier.size(66.dp))
    }
}

// === CUSTOM LAYOUT: Dynamically configured by user ===
@Composable private fun CustomLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    cl: CustomLayoutData,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current
    val btnSize = when (cl.controlSize) { "SMALL" -> 44.dp; "LARGE" -> 62.dp; else -> 54.dp }
    val rotSize = when (cl.controlSize) { "SMALL" -> 52.dp; "LARGE" -> 74.dp; else -> 66.dp }

    @Composable fun InfoBar(modifier: Modifier = Modifier, horizontal: Boolean = true) {
        if (horizontal) {
            Row(modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(theme.deviceColor).padding(horizontal = 8.dp, vertical = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically) {
                if (cl.showHold) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(30.dp)) } }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (cl.showScore) Text(gs.score.toString().padStart(7, '0'), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (cl.showLevel) Tag("LV${gs.level}"); if (cl.showLines) Tag("${gs.lines}L") }
                }
                if (cl.showNext) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("NEXT"); gs.nextPieces.take(cl.nextQueueSize).forEachIndexed { i, p -> NextPiecePreview(p.shape, Modifier.size(if (i == 0) 30.dp else 22.dp), if (i == 0) 1f else 0.5f) } } }
            }
        } else {
            Column(modifier.width(56.dp).fillMaxHeight(), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                if (cl.showHold) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(40.dp)) } }
                if (cl.showScore) ScoreBlock(gs.score, gs.level, gs.lines)
                if (cl.showNext) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("NEXT"); gs.nextPieces.take(cl.nextQueueSize).forEachIndexed { i, p -> NextPiecePreview(p.shape, Modifier.size(if (i == 0) 36.dp else 26.dp), if (i == 0) 1f else 0.5f) } } }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Top info
        if (cl.infoPosition == "TOP_BAR") { InfoBar(); Spacer(Modifier.height(4.dp)) }

        // Middle row: optional side info + board
        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (cl.infoPosition == "LEFT_SIDE") { InfoBar(horizontal = false); Spacer(Modifier.width(4.dp)) }

            // Board
            val boardMod = Modifier.weight(1f).then(
                if (cl.boardWidthPercent < 0.99f) Modifier.fillMaxWidth(cl.boardWidthPercent) else Modifier.fillMaxWidth()
            ).then(
                when (cl.boardPosition) {
                    "TOP" -> Modifier.fillMaxHeight()
                    "BOTTOM" -> Modifier.fillMaxHeight()
                    else -> Modifier.fillMaxHeight()
                }
            )
            Box(Modifier.weight(1f).fillMaxHeight(), when (cl.boardPosition) { "TOP" -> Alignment.TopCenter; "BOTTOM" -> Alignment.BottomCenter; else -> Alignment.Center }) {
                GameBoard(gs.board, Modifier.fillMaxHeight().aspectRatio(0.5f), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad)
            }

            if (cl.infoPosition == "RIGHT_SIDE") { Spacer(Modifier.width(4.dp)); InfoBar(horizontal = false) }
        }

        // Bottom info
        if (cl.infoPosition == "BOTTOM_STRIP") { Spacer(Modifier.height(4.dp)); InfoBar() }

        // Controls
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            DPad(btnSize, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
                onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (cl.showHoldButton) ActionButton("HOLD", onHold, width = 78.dp, height = 34.dp)
                if (cl.showPauseButton) ActionButton(if (gs.status == GameStatus.MENU) "START" else "PAUSE", { if (gs.status == GameStatus.MENU) onStart() else onPause() }, width = 78.dp, height = 34.dp)
                ActionButton("...", onSet, width = 46.dp, height = 24.dp)
            }
            if (dp == DPadStyle.STANDARD) RotateButton(onRotate, rotSize) else Spacer(Modifier.size(rotSize))
        }
    }
}

// === Helpers ===
@Composable private fun Tag(t: String) { Text(t, fontSize = 9.sp, color = LocalGameTheme.current.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp) }
@Composable private fun ScoreBlock(score: Int, level: Int, lines: Int) {
    val theme = LocalGameTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(score.toString().padStart(7, '0'), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Tag("LV${level}"); Tag("${lines}L") }
    }
}

// === Overlays ===
@Composable private fun MenuOverlay(hs: Int, onStart: () -> Unit, onSet: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(Modifier.fillMaxSize().background(theme.backgroundColor), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BRICK", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.textPrimary, letterSpacing = 6.sp)
            Text("GAME", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.accentColor, letterSpacing = 6.sp)
            Spacer(Modifier.height(8.dp)); Text("v3.0.0", fontSize = 12.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(32.dp))
            if (hs > 0) { Text("BEST  $hs", fontSize = 18.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor, fontWeight = FontWeight.Bold); Spacer(Modifier.height(24.dp)) }
            ActionButton("PLAY", onStart, width = 160.dp, height = 52.dp, backgroundColor = theme.accentColor)
            Spacer(Modifier.height(14.dp)); ActionButton("SETTINGS", onSet, width = 160.dp, height = 44.dp)
        }
    }
}

@Composable private fun PauseOverlay(onResume: () -> Unit, onSet: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSED", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color.White, letterSpacing = 4.sp)
            Spacer(Modifier.height(28.dp)); ActionButton("RESUME", onResume, width = 160.dp, height = 48.dp)
            Spacer(Modifier.height(12.dp)); ActionButton("SETTINGS", onSet, width = 160.dp, height = 42.dp)
        }
    }
}

@Composable private fun GameOverOverlay(score: Int, level: Int, lines: Int, onRestart: () -> Unit, onMenu: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444), letterSpacing = 4.sp)
            Text("OVER", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444), letterSpacing = 4.sp)
            Spacer(Modifier.height(20.dp))
            Text(score.toString(), fontSize = 28.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor, fontWeight = FontWeight.Bold)
            Text("Level $level · $lines Lines", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(28.dp)); ActionButton("AGAIN", onRestart, width = 160.dp, height = 48.dp, backgroundColor = theme.accentColor)
            Spacer(Modifier.height(12.dp)); ActionButton("MENU", onMenu, width = 160.dp, height = 42.dp)
        }
    }
}

// =============================================================================
// ACTION POPUP — Big, centered, color-coded, auto-fading
// =============================================================================
@Composable
private fun ActionPopup(label: String, linesCleared: Int) {
    // Track when label changes to trigger animation
    var currentLabel by remember { mutableStateOf("") }
    var showPopup by remember { mutableStateOf(false) }
    var popupText by remember { mutableStateOf("") }
    var popupLines by remember { mutableStateOf(0) }

    // Detect label changes
    LaunchedEffect(label) {
        if (label.isNotEmpty()) {
            popupText = label
            popupLines = linesCleared
            showPopup = true
            currentLabel = label
            // Auto-dismiss after duration
            delay(1000L)
            showPopup = false
        }
    }

    if (showPopup && popupText.isNotEmpty()) {
        // Color based on action type
        val (bgColor, textColor, fontSize) = remember(popupText, popupLines) {
            when {
                popupText.contains("Tetris", ignoreCase = true) && popupText.contains("B2B", ignoreCase = true) ->
                    Triple(Color(0xFFFF4400).copy(alpha = 0.55f), Color.White, 38)
                popupText.contains("Tetris", ignoreCase = true) ->
                    Triple(Color(0xFFFF2222).copy(alpha = 0.5f), Color.White, 36)
                popupText.contains("T-Spin", ignoreCase = true) ->
                    Triple(Color(0xFF9B59B6).copy(alpha = 0.5f), Color.White, 32)
                popupText.contains("B2B", ignoreCase = true) ->
                    Triple(Color(0xFFFF8800).copy(alpha = 0.5f), Color.White, 32)
                popupLines >= 3 ->
                    Triple(Color(0xFFFF6600).copy(alpha = 0.45f), Color.White, 30)
                popupLines == 2 ->
                    Triple(Color(0xFFF4D03F).copy(alpha = 0.45f), Color.Black, 28)
                popupLines == 1 ->
                    Triple(Color.White.copy(alpha = 0.4f), Color.Black, 26)
                else -> // Combo, other labels
                    Triple(Color(0xFF3498DB).copy(alpha = 0.45f), Color.White, 28)
            }
        }

        // Animate: scale in then fade out
        val animScale = remember { Animatable(0.3f) }
        val animAlpha = remember { Animatable(0f) }

        LaunchedEffect(popupText) {
            // Scale in
            animScale.snapTo(0.3f)
            animAlpha.snapTo(0f)
            // Quick pop in
            animScale.animateTo(1.1f, tween(100, easing = FastOutSlowInEasing))
            animAlpha.animateTo(1f, tween(80))
            // Settle
            animScale.animateTo(1f, tween(60))
            // Hold
            delay(500L)
            // Fade out
            animAlpha.animateTo(0f, tween(250))
        }

        Box(
            Modifier.fillMaxSize(),
            Alignment.Center
        ) {
            Text(
                popupText,
                modifier = Modifier
                    .scale(animScale.value)
                    .alpha(animAlpha.value)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .padding(horizontal = 36.dp, vertical = 16.dp),
                fontSize = fontSize.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = textColor,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }
    }
}
