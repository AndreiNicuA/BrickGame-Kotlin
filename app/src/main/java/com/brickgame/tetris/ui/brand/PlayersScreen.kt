package com.brickgame.tetris.ui.brand

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.LocalPlayer
import com.brickgame.tetris.data.PlayStyle
import com.brickgame.tetris.data.ProfilesRepository

/** One row of the player list: who, their best, and their favourite game. */
data class PlayerSummary(val player: LocalPlayer, val best: Int, val lastPlayed: Long?)

/** "Who's playing?" — pick a player or add a new one. */
@Composable
fun PlayersScreen(
    players: List<PlayerSummary>,
    activeId: String?,
    onPick: (String) -> Unit,
    onAdd: (String) -> Unit,
    onClose: () -> Unit,
    onRemove: (String) -> Unit = {}
) {
    BackHandler(onBack = onClose)
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var confirmId by remember { mutableStateOf<String?>(null) }
    val active = players.firstOrNull { it.player.id == activeId } ?: players.firstOrNull()
    val accent = active?.let { Bw.playerColor(it.player.colorIndex) } ?: Bw.Cyan

    Column(Modifier.fillMaxSize().background(Bw.Ground).safeDrawingPadding().imePadding().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("BRICKWELL", style = BwType.Wordmark, color = accent, modifier = Modifier.weight(1f))
                if (players.size > 1) {
                    Text(if (editing) "Done" else "Edit", style = BwType.Label.copy(color = if (editing) accent else Bw.TextMuted),
                        modifier = Modifier.clip(RoundedCornerShape(999.dp))
                            .clickable(role = Role.Button) { editing = !editing; confirmId = null }
                            .padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }
            Text("Who's playing?", style = BwType.Title.copy(fontSize = 34.sp), modifier = Modifier.padding(top = 12.dp))
            Text("Every player keeps their own scores, controls and style.", style = BwType.Body, modifier = Modifier.padding(bottom = 10.dp))

            players.forEach { s ->
                val p = s.player
                val color = Bw.playerColor(p.colorIndex)
                BwCard(p.id == active?.player?.id, { onPick(p.id) }, Modifier.fillMaxWidth().heightIn(min = 76.dp),
                    accent = color, contentPadding = PaddingValues(14.dp)) {
                    BwAvatar(p.name, color, 48.dp, corner = 14.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(p.name, style = BwType.Label.copy(fontSize = 17.sp))
                        Text(if (s.best > 0) "Best ${"%,d".format(s.best)}${lastPlayedText(s.lastPlayed)}" else "No games yet", style = BwType.Small)
                    }
                    if (editing && players.size > 1) {
                        // Two taps to remove, so a slip doesn't delete anyone
                        val sure = confirmId == p.id
                        Box(Modifier.clip(RoundedCornerShape(999.dp))
                            .background(if (sure) Bw.Pink else Bw.Surface)
                            .border(1.dp, if (sure) Bw.Pink else Bw.Line, RoundedCornerShape(999.dp))
                            .clickable(role = Role.Button, onClickLabel = "Remove ${p.name}") {
                                if (sure) { onRemove(p.id); confirmId = null } else confirmId = p.id
                            }
                            .heightIn(min = 40.dp).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                            Text(if (sure) "REMOVE?" else "Remove", style = BwType.Label.copy(fontSize = 13.sp, color = if (sure) Bw.Ground else Bw.Text))
                        }
                    } else {
                        Text(when (p.style) { PlayStyle.CLASSIC -> "CLASSIC"; PlayStyle.NEON -> "NEON"; PlayStyle.THREE_D -> "3D" },
                            style = BwType.Overline.copy(color = color))
                    }
                }
            }

            if (adding) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BasicTextField(
                        value = newName, onValueChange = { newName = it.take(16) }, singleLine = true,
                        textStyle = BwType.Label.copy(fontSize = 17.sp), cursorBrush = SolidColor(Bw.Cyan),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (newName.isNotBlank()) { onAdd(newName); adding = false; newName = "" } }),
                        modifier = Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).background(Bw.Surface)
                            .border(2.dp, Bw.Cyan, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp)
                            .semantics { contentDescription = "New player name" },
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                if (newName.isEmpty()) Text("Name", style = BwType.Label.copy(fontSize = 17.sp, color = Bw.TextMuted))
                                inner()
                            }
                        }
                    )
                    BwPrimaryButton("ADD", { if (newName.isNotBlank()) { onAdd(newName); adding = false; newName = "" } }, height = 56.dp)
                }
            } else if (players.size < ProfilesRepository.MAX_PLAYERS) {
                Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(RoundedCornerShape(18.dp))
                    .border(1.5.dp, Bw.LineStrong, RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button) { adding = true }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).border(1.5.dp, Bw.LineStrong, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center) { BwIcon(BwIconKind.PLUS, Bw.Text, size = 22.dp) }
                    Spacer(Modifier.width(14.dp))
                    Text("New player", style = BwType.Label.copy(fontSize = 16.sp))
                }
            }
        }

        BwPrimaryButton("CONTINUE AS ${(active?.player?.name ?: "PLAYER").uppercase()}", onClose,
            Modifier.fillMaxWidth(), accent = accent, height = 56.dp)
    }
}

private fun lastPlayedText(ts: Long?): String {
    if (ts == null) return ""
    val days = ((System.currentTimeMillis() - ts) / 86_400_000L).toInt()
    return when {
        days <= 0 -> " · played today"
        days == 1 -> " · yesterday"
        days < 7 -> " · $days days ago"
        else -> " · ${days / 7} wk ago"
    }
}
