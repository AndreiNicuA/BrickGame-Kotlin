package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.CustomLayoutData
import com.brickgame.tetris.data.LayoutElements
import com.brickgame.tetris.ui.theme.GameTheme

private val PBG = Color(0xFF0D0D0D)
private val CARD = Color(0xFF1E1E1E)
private val ACC = Color(0xFFF4D03F)
private val TX = Color(0xFFE8E8E8)
private val DIM = Color(0xFF888888)

// ======================================================================
// LAYOUT EDITOR v8 — MENU-BASED (no more drag-and-drop)
// Live preview at top, settings panels below organized by category
// ======================================================================
@Composable
fun LayoutEditorScreen(
    layout: CustomLayoutData,
    theme: GameTheme,
    basePreset: com.brickgame.tetris.ui.layout.LayoutPreset,
    dpadStyle: com.brickgame.tetris.ui.layout.DPadStyle,
    onUpdateLayout: (CustomLayoutData) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var layoutName by remember(layout.id) { mutableStateOf(layout.name) }

    Column(Modifier.fillMaxSize().background(PBG).systemBarsPadding()) {
        // HEADER
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = ACC, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(6.dp))
            BasicTextField(layoutName, { layoutName = it; onUpdateLayout(layout.copy(name = it)) },
                textStyle = TextStyle(color = TX, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(ACC), singleLine = true,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF252525)).padding(horizontal = 10.dp, vertical = 8.dp),
                decorationBox = { inner -> if (layoutName.isEmpty()) Text("Layout name...", color = DIM, fontSize = 14.sp, fontFamily = FontFamily.Monospace); inner() })
            Spacer(Modifier.width(8.dp))
            Text("SAVE", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ACC).clickable { onSave() }.padding(horizontal = 14.dp, vertical = 8.dp))
        }

        // LIVE PREVIEW — scaled down real layout
        Box(Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)).background(theme.backgroundColor).border(1.dp, ACC.copy(0.3f), RoundedCornerShape(12.dp))) {
            LayoutPreviewContent(layout, theme)
        }

        // SETTINGS — scrollable
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

            // BASE LAYOUT
            SectionLabel("Base Layout")
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                listOf("CLASSIC" to "Classic", "MODERN" to "Modern", "FULLSCREEN" to "Fullscreen").forEach { (k, v) ->
                    val sel = layout.baseLayout == k
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (sel) ACC.copy(0.2f) else CARD)
                        .border(1.dp, if (sel) ACC else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { onUpdateLayout(layout.copy(baseLayout = k)) }.padding(vertical = 10.dp), Alignment.Center) {
                        Text(v, color = if (sel) ACC else TX, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            // CONTROLS
            SectionLabel("Controls")
            SettingsCard {
                // Control size
                SettingRow("Control Size") {
                    SizeChips(layout.controlSize) { onUpdateLayout(layout.copy(controlSize = it)) }
                }
                // Swap sides
                SettingToggle("Swap Sides (DPad ↔ Rotate)", layout.controlsSwapped) {
                    onUpdateLayout(layout.copy(controlsSwapped = it))
                }
                // Hide individual controls
                VisibilityToggle("Hold Button", LayoutElements.HOLD_BTN, layout) { onUpdateLayout(it) }
                VisibilityToggle("Pause Button", LayoutElements.PAUSE_BTN, layout) { onUpdateLayout(it) }
            }

            // INFO DISPLAY
            SectionLabel("Information Display")
            SettingsCard {
                // Next queue size
                SettingRow("Next Queue") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..3).forEach { n ->
                            val sel = layout.nextQueueSize == n
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(if (sel) ACC.copy(0.2f) else Color(0xFF333333))
                                .clickable { onUpdateLayout(layout.copy(nextQueueSize = n)) }, Alignment.Center) {
                                Text("$n", color = if (sel) ACC else DIM, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                // Visibility toggles for info elements
                VisibilityToggle("Score", LayoutElements.SCORE, layout) { onUpdateLayout(it) }
                VisibilityToggle("Level", LayoutElements.LEVEL, layout) { onUpdateLayout(it) }
                VisibilityToggle("Lines", LayoutElements.LINES, layout) { onUpdateLayout(it) }
                VisibilityToggle("Hold Preview", LayoutElements.HOLD_PREVIEW, layout) { onUpdateLayout(it) }
                VisibilityToggle("Next Preview", LayoutElements.NEXT_PREVIEW, layout) { onUpdateLayout(it) }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// === LIVE PREVIEW — renders a miniature version of the actual layout ===
@Composable
private fun LayoutPreviewContent(layout: CustomLayoutData, theme: GameTheme) {
    val vis = layout.visibility
    fun isVis(e: String) = vis.getOrDefault(e, true)

    Box(Modifier.fillMaxSize().scale(0.48f), Alignment.Center) {
        // Render a simplified version based on base layout
        when (layout.baseLayout) {
            "MODERN" -> ModernPreview(layout, theme)
            "FULLSCREEN" -> FullscreenPreview(layout, theme)
            else -> ClassicPreview(layout, theme)
        }
    }
}

@Composable
private fun ClassicPreview(layout: CustomLayoutData, theme: GameTheme) {
    fun isVis(e: String) = layout.visibility.getOrDefault(e, true)
    Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)).background(theme.deviceColor).padding(6.dp)) {
            Column(Modifier.width(58.dp).fillMaxHeight(), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                if (isVis(LayoutElements.HOLD_PREVIEW)) { Text("HOLD", fontSize = 8.sp, color = theme.textSecondary); Box(Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(theme.screenBackground)) }
                if (isVis(LayoutElements.SCORE)) Text("001234", fontSize = 9.sp, color = theme.pixelOn, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                if (isVis(LayoutElements.LEVEL)) Text("LV1", fontSize = 8.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
                if (isVis(LayoutElements.LINES)) Text("0L", fontSize = 8.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
            }
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(theme.screenBackground)) { PreviewGrid(theme) }
            Column(Modifier.width(58.dp).fillMaxHeight(), Arrangement.Top, Alignment.CenterHorizontally) {
                if (isVis(LayoutElements.NEXT_PREVIEW)) {
                    Text("NEXT", fontSize = 8.sp, color = theme.textSecondary)
                    repeat(layout.nextQueueSize.coerceIn(1, 3)) { i -> Box(Modifier.size(if (i == 0) 40.dp else 28.dp).padding(1.dp).clip(RoundedCornerShape(4.dp)).background(theme.screenBackground.copy(if (i == 0) 1f else 0.5f))) }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        PreviewControls(layout, theme)
    }
}

@Composable
private fun ModernPreview(layout: CustomLayoutData, theme: GameTheme) {
    fun isVis(e: String) = layout.visibility.getOrDefault(e, true)
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(theme.deviceColor).padding(horizontal = 10.dp, vertical = 6.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically) {
            if (isVis(LayoutElements.HOLD_PREVIEW)) Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("HOLD", fontSize = 7.sp, color = theme.textSecondary); Box(Modifier.size(28.dp).clip(RoundedCornerShape(3.dp)).background(theme.screenBackground)) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isVis(LayoutElements.SCORE)) Text("001234", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isVis(LayoutElements.LEVEL)) Text("LV 1", fontSize = 8.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
                    if (isVis(LayoutElements.LINES)) Text("0 LINES", fontSize = 8.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
                }
            }
            if (isVis(LayoutElements.NEXT_PREVIEW)) Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("NEXT", fontSize = 7.sp, color = theme.textSecondary); Box(Modifier.size(28.dp).clip(RoundedCornerShape(3.dp)).background(theme.screenBackground)) }
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.weight(1f).aspectRatio(0.5f).clip(RoundedCornerShape(6.dp)).background(theme.screenBackground)) { PreviewGrid(theme) }
        Spacer(Modifier.height(4.dp))
        PreviewControls(layout, theme)
    }
}

@Composable
private fun FullscreenPreview(layout: CustomLayoutData, theme: GameTheme) {
    fun isVis(e: String) = layout.visibility.getOrDefault(e, true)
    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(theme.screenBackground)) { PreviewGrid(theme) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            if (isVis(LayoutElements.HOLD_PREVIEW)) Box(Modifier.size(22.dp).clip(RoundedCornerShape(3.dp)).background(theme.screenBackground))
            if (isVis(LayoutElements.SCORE)) Text("1234", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            if (isVis(LayoutElements.LEVEL)) Text("LV1", fontSize = 8.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
            if (isVis(LayoutElements.NEXT_PREVIEW)) Box(Modifier.size(22.dp).clip(RoundedCornerShape(3.dp)).background(theme.screenBackground))
        }
        PreviewControls(layout, theme)
    }
}

@Composable
private fun PreviewGrid(theme: GameTheme) {
    Column(Modifier.fillMaxSize().padding(3.dp), Arrangement.SpaceEvenly) {
        repeat(6) { r -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { repeat(10) { c ->
            val on = (r == 2 && c in 4..5) || (r == 3 && c in 4..5) || (r >= 5)
            Box(Modifier.weight(1f).aspectRatio(1f).padding(0.5.dp).clip(RoundedCornerShape(1.dp)).background(if (on) theme.pixelOn else theme.pixelOff))
        } } }
    }
}

@Composable
private fun PreviewControls(layout: CustomLayoutData, theme: GameTheme) {
    fun isVis(e: String) = layout.visibility.getOrDefault(e, true)
    val btnSz = when (layout.controlSize) { "SMALL" -> 36.dp; "LARGE" -> 50.dp; else -> 44.dp }
    val rotSz = when (layout.controlSize) { "SMALL" -> 40.dp; "LARGE" -> 56.dp; else -> 50.dp }
    val swap = layout.controlsSwapped

    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        // Left side
        if (!swap) {
            // DPad
            Box(Modifier.size(btnSz * 2.5f), Alignment.Center) {
                Box(Modifier.size(btnSz).offset(y = -(btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary))
                Box(Modifier.size(btnSz).offset(y = (btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary))
                Box(Modifier.size(btnSz).offset(x = -(btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary))
                Box(Modifier.size(btnSz).offset(x = (btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary))
            }
        } else {
            Box(Modifier.size(rotSz).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("↻", color = theme.buttonSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        // Center buttons
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (isVis(LayoutElements.HOLD_BTN)) Box(Modifier.clip(RoundedCornerShape(12.dp)).background(theme.accentColor.copy(0.8f)).padding(horizontal = 16.dp, vertical = 4.dp)) { Text("HOLD", fontSize = 9.sp, color = theme.backgroundColor, fontWeight = FontWeight.Bold) }
            if (isVis(LayoutElements.PAUSE_BTN)) Box(Modifier.clip(RoundedCornerShape(12.dp)).background(theme.accentColor.copy(0.8f)).padding(horizontal = 16.dp, vertical = 4.dp)) { Text("PAUSE", fontSize = 9.sp, color = theme.backgroundColor, fontWeight = FontWeight.Bold) }
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(theme.accentColor).padding(horizontal = 10.dp, vertical = 2.dp)) { Text("···", fontSize = 8.sp, color = theme.backgroundColor, fontWeight = FontWeight.Bold) }
        }
        // Right side
        if (!swap) {
            Box(Modifier.size(rotSz).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("↻", color = theme.buttonSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        } else {
            Box(Modifier.size(btnSz * 2.5f), Alignment.Center) {
                Box(Modifier.size(btnSz).offset(y = -(btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary))
                Box(Modifier.size(btnSz).offset(y = (btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary))
                Box(Modifier.size(btnSz).offset(x = -(btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary))
                Box(Modifier.size(btnSz).offset(x = (btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary))
            }
        }
    }
}

// === Reusable settings components ===
@Composable private fun SectionLabel(text: String) { Text(text, color = ACC, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CARD).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }

@Composable private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(label, color = TX, fontSize = 13.sp); content() }
}
@Composable private fun SettingToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(label, color = TX, fontSize = 13.sp); Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = ACC, uncheckedTrackColor = Color(0xFF333333))) }
}
@Composable private fun VisibilityToggle(label: String, elementId: String, layout: CustomLayoutData, onChange: (CustomLayoutData) -> Unit) {
    val visible = layout.visibility.getOrDefault(elementId, true)
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = if (visible) TX else DIM, fontSize = 13.sp)
        Switch(visible, { onChange(layout.copy(visibility = layout.visibility + (elementId to it))) }, colors = SwitchDefaults.colors(checkedTrackColor = ACC, uncheckedTrackColor = Color(0xFF333333)))
    }
}
@Composable private fun SizeChips(current: String, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("SMALL" to "S", "MEDIUM" to "M", "LARGE" to "L").forEach { (k, v) ->
            val sel = current == k
            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (sel) ACC.copy(0.2f) else Color(0xFF333333)).clickable { onChange(k) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(v, color = if (sel) ACC else DIM, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ======================================================================
// THEME EDITOR (unchanged)
// ======================================================================
private data class ThemeColorTarget(val label: String, val getColor: (GameTheme) -> Color, val setColor: (GameTheme, Color) -> GameTheme)
private val themeTargets = listOf(
    ThemeColorTarget("Background", { it.backgroundColor }, { t, c -> t.copy(backgroundColor = c) }),
    ThemeColorTarget("Device Frame", { it.deviceColor }, { t, c -> t.copy(deviceColor = c) }),
    ThemeColorTarget("Screen / LCD", { it.screenBackground }, { t, c -> t.copy(screenBackground = c) }),
    ThemeColorTarget("Blocks (ON)", { it.pixelOn }, { t, c -> t.copy(pixelOn = c) }),
    ThemeColorTarget("Empty Cells", { it.pixelOff }, { t, c -> t.copy(pixelOff = c) }),
    ThemeColorTarget("Text Primary", { it.textPrimary }, { t, c -> t.copy(textPrimary = c) }),
    ThemeColorTarget("Text Secondary", { it.textSecondary }, { t, c -> t.copy(textSecondary = c) }),
    ThemeColorTarget("Button Color", { it.buttonPrimary }, { t, c -> t.copy(buttonPrimary = c, buttonPrimaryPressed = c.copy(alpha = 0.7f)) }),
    ThemeColorTarget("Button Text", { it.buttonSecondary }, { t, c -> t.copy(buttonSecondary = c, buttonSecondaryPressed = c.copy(alpha = 0.7f)) }),
    ThemeColorTarget("Accent / Score", { it.accentColor }, { t, c -> t.copy(accentColor = c) })
)

@Composable fun ThemeEditorScreen(theme: GameTheme, onUpdateTheme: (GameTheme) -> Unit, onSave: () -> Unit, onBack: () -> Unit) {
    var selectedTarget by remember { mutableStateOf<ThemeColorTarget?>(null) }
    var themeName by remember(theme.id) { mutableStateOf(theme.name) }
    Column(Modifier.fillMaxSize().background(PBG).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = ACC, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            BasicTextField(themeName, { themeName = it; onUpdateTheme(theme.copy(name = it)) }, textStyle = TextStyle(color = TX, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(ACC), singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            Text("SAVE", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ACC).clickable { onSave() }.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        Box(Modifier.fillMaxWidth().weight(0.45f).padding(horizontal = 12.dp).clip(RoundedCornerShape(12.dp)).background(theme.backgroundColor).border(2.dp, if (selectedTarget?.label == "Background") Color(0xFF22C55E) else Color.Transparent, RoundedCornerShape(12.dp)).clickable { selectedTarget = themeTargets[0] }) {
            Box(Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(8.dp)).background(theme.deviceColor).border(2.dp, if (selectedTarget?.label == "Device Frame") Color(0xFF22C55E) else Color.Transparent, RoundedCornerShape(8.dp)).clickable { selectedTarget = themeTargets[1] }) {
                Row(Modifier.fillMaxSize().padding(4.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(theme.screenBackground).border(2.dp, if (selectedTarget?.label == "Screen / LCD") Color(0xFF22C55E) else Color.Transparent, RoundedCornerShape(6.dp)).clickable { selectedTarget = themeTargets[2] }) {
                        Column(Modifier.fillMaxSize().padding(6.dp), Arrangement.SpaceEvenly) { repeat(4) { r -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { repeat(6) { c -> val isOn = (r + c) % 3 == 0; Box(Modifier.size(16.dp).clip(RoundedCornerShape(2.dp)).background(if (isOn) theme.pixelOn else theme.pixelOff).border(1.dp, if (selectedTarget?.label == (if (isOn) "Blocks (ON)" else "Empty Cells")) Color(0xFF22C55E) else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = if (isOn) themeTargets[3] else themeTargets[4] }) } } } }
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.width(50.dp).fillMaxHeight(), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                        Text("00123", color = theme.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Accent / Score") Color(0xFF22C55E) else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[9] }.padding(2.dp))
                        Text("LV 3", color = theme.textPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Text Primary") Color(0xFF22C55E) else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[5] }.padding(2.dp))
                        Text("Info", color = theme.textSecondary, fontSize = 9.sp, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Text Secondary") Color(0xFF22C55E) else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[6] }.padding(2.dp))
                    }
                }
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(theme.buttonPrimary).border(2.dp, if (selectedTarget?.label == "Button Color") Color(0xFF22C55E) else Color.Transparent, CircleShape).clickable { selectedTarget = themeTargets[7] })
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(theme.buttonSecondary).border(2.dp, if (selectedTarget?.label == "Button Text") Color(0xFF22C55E) else Color.Transparent, RoundedCornerShape(12.dp)).clickable { selectedTarget = themeTargets[8] })
            }
        }
        Text("Tap element above or select:", color = DIM, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(themeTargets.size) { i -> val t = themeTargets[i]; val sel = selectedTarget == t; Row(Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) ACC.copy(0.2f) else CARD).border(1.dp, if (sel) ACC else DIM.copy(0.2f), RoundedCornerShape(8.dp)).clickable { selectedTarget = t }.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Box(Modifier.size(16.dp).clip(CircleShape).background(t.getColor(theme)).border(1.dp, DIM.copy(0.3f), CircleShape)); Text(t.label, color = if (sel) ACC else TX, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) } } }
        if (selectedTarget != null) { ColorPickerPanel(selectedTarget!!.getColor(theme)) { onUpdateTheme(selectedTarget!!.setColor(theme, it)) } }
        else { Box(Modifier.fillMaxWidth().weight(0.35f), Alignment.Center) { Text("Tap an element to edit its color", color = DIM, fontSize = 13.sp) } }
    }
}

@Composable private fun ColumnScope.ColorPickerPanel(color: Color, onChange: (Color) -> Unit) {
    var hexText by remember(color) { mutableStateOf(colorToHex(color)) }
    Column(Modifier.fillMaxWidth().weight(0.35f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Box(Modifier.size(36.dp).clip(CircleShape).background(color).border(2.dp, DIM.copy(0.4f), CircleShape)); Row(verticalAlignment = Alignment.CenterVertically) { Text("#", color = DIM, fontSize = 14.sp, fontFamily = FontFamily.Monospace); BasicTextField(hexText, { v -> val c = v.filter { it.isLetterOrDigit() }.take(6); hexText = c; hexFromString(c)?.let { onChange(it) } }, textStyle = TextStyle(color = TX, fontSize = 14.sp, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(ACC), singleLine = true, modifier = Modifier.width(80.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF252525)).padding(8.dp)) } }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { val presets = listOf(Color.Black, Color(0xFF1A1A1A), Color(0xFF2A2A2A), Color(0xFF4A4A4A), Color(0xFF6A6A6A), Color(0xFF8A8A8A), Color(0xFFAAAAAA), Color(0xFFCCCCCC), Color.White, Color(0xFFFF0000), Color(0xFFFF4444), Color(0xFFFF8800), Color(0xFFFFAA00), Color(0xFFF4D03F), Color(0xFFFFFF00), Color(0xFF88FF00), Color(0xFF00FF00), Color(0xFF00FF88), Color(0xFF00FFFF), Color(0xFF0088FF), Color(0xFF0000FF), Color(0xFF8800FF), Color(0xFFFF00FF), Color(0xFF8AB4F8), Color(0xFF9B59B6), Color(0xFFE07030), Color(0xFF2ECC71), Color(0xFF3D4A32), Color(0xFFB8C4A8), Color(0xFFD4C896), Color(0xFF201800), Color(0xFF080810), Color(0xFFFAF6F0)); items(presets.size) { i -> val p = presets[i]; Box(Modifier.size(26.dp).clip(CircleShape).background(p).border(if (colorsClose(p, color)) 2.dp else 1.dp, if (colorsClose(p, color)) ACC else DIM.copy(0.2f), CircleShape).clickable { onChange(p); hexText = colorToHex(p) }) } }
        Spacer(Modifier.height(6.dp))
        val r = (color.red * 255).toInt(); val g = (color.green * 255).toInt(); val b = (color.blue * 255).toInt()
        RGBSlider("R", r, Color.Red) { onChange(Color(it / 255f, color.green, color.blue)); hexText = colorToHex(Color(it / 255f, color.green, color.blue)) }
        RGBSlider("G", g, Color.Green) { onChange(Color(color.red, it / 255f, color.blue)); hexText = colorToHex(Color(color.red, it / 255f, color.blue)) }
        RGBSlider("B", b, Color(0xFF4488FF)) { onChange(Color(color.red, color.green, it / 255f)); hexText = colorToHex(Color(color.red, color.green, it / 255f)) }
    }
}
@Composable private fun RGBSlider(label: String, value: Int, trackColor: Color, onChange: (Int) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, color = DIM, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(16.dp)); Slider(value.toFloat(), { onChange(it.toInt()) }, valueRange = 0f..255f, modifier = Modifier.weight(1f).height(24.dp), colors = SliderDefaults.colors(thumbColor = trackColor, activeTrackColor = trackColor.copy(0.7f), inactiveTrackColor = trackColor.copy(0.12f))); Text(value.toString().padStart(3), color = DIM, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(28.dp)) } }
private fun colorToHex(c: Color) = "%02X%02X%02X".format((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
private fun hexFromString(hex: String): Color? = try { if (hex.length == 6) Color(("FF$hex").toLong(16)) else null } catch (_: Exception) { null }
private fun colorsClose(a: Color, b: Color): Boolean { val d = (a.red-b.red)*(a.red-b.red)+(a.green-b.green)*(a.green-b.green)+(a.blue-b.blue)*(a.blue-b.blue); return d < 0.002f }
