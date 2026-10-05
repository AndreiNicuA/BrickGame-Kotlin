package com.brickgame.tetris.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.profilesDataStore: DataStore<Preferences> by preferencesDataStore(name = "players")

/** Which of the three games a player starts in. */
enum class PlayStyle { CLASSIC, NEON, THREE_D }

/**
 * One local player on this phone. The per-player choices that matter most (name, colour,
 * controls, style, speed) live here; switching player applies them to the app.
 */
@Serializable
data class LocalPlayer(
    val id: String,
    val name: String,
    val colorIndex: Int = 0,
    val swipeControls: Boolean = false,
    val style: PlayStyle = PlayStyle.NEON,
    val difficulty: String = "NORMAL",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class PlayersState(val players: List<LocalPlayer> = emptyList(), val activeId: String? = null) {
    val active: LocalPlayer? get() = players.firstOrNull { it.id == activeId } ?: players.firstOrNull()
}

/** Local multi-player profiles ("Who's playing?"). */
class ProfilesRepository(private val context: Context) {

    companion object {
        private val STATE_JSON = stringPreferencesKey("players_json")
        const val MAX_PLAYERS = 8
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val state: Flow<PlayersState> = context.profilesDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> decode(prefs[STATE_JSON]) }

    private fun decode(raw: String?): PlayersState =
        raw?.let { try { json.decodeFromString<PlayersState>(it) } catch (_: Exception) { null } } ?: PlayersState()

    private suspend fun update(transform: (PlayersState) -> PlayersState): PlayersState {
        var result = PlayersState()
        context.profilesDataStore.edit { prefs ->
            result = transform(decode(prefs[STATE_JSON]))
            prefs[STATE_JSON] = json.encodeToString(result)
        }
        return result
    }

    /** First run of this version: turn the existing single player into player #1. */
    suspend fun ensureFirstPlayer(name: String, swipe: Boolean, style: PlayStyle, difficulty: String) {
        if (state.first().players.isNotEmpty()) return
        update { s ->
            if (s.players.isNotEmpty()) s
            else {
                val p = LocalPlayer(id = newId(), name = name.ifBlank { "Player" }, swipeControls = swipe, style = style, difficulty = difficulty)
                PlayersState(listOf(p), p.id)
            }
        }
    }

    suspend fun addPlayer(name: String): LocalPlayer? {
        var created: LocalPlayer? = null
        update { s ->
            if (s.players.size >= MAX_PLAYERS) s
            else {
                val p = LocalPlayer(id = newId(), name = name.ifBlank { "Player ${s.players.size + 1}" }, colorIndex = nextColor(s))
                created = p
                s.copy(players = s.players + p, activeId = p.id)
            }
        }
        return created
    }

    suspend fun setActive(id: String) { update { s -> if (s.players.any { it.id == id }) s.copy(activeId = id) else s } }

    /** Apply a change to the active player. */
    suspend fun updateActive(transform: (LocalPlayer) -> LocalPlayer) {
        update { s ->
            val active = s.active ?: return@update s
            s.copy(players = s.players.map { if (it.id == active.id) transform(it) else it })
        }
    }

    suspend fun removePlayer(id: String) {
        update { s ->
            if (s.players.size <= 1) s
            else {
                val rest = s.players.filterNot { it.id == id }
                s.copy(players = rest, activeId = if (s.activeId == id) rest.first().id else s.activeId)
            }
        }
    }

    private fun nextColor(s: PlayersState): Int {
        val used = s.players.map { it.colorIndex }.toSet()
        return (0 until 6).firstOrNull { it !in used } ?: s.players.size
    }

    private fun newId() = "p_" + System.currentTimeMillis().toString(36) + "_" + (0..9999).random()
}
