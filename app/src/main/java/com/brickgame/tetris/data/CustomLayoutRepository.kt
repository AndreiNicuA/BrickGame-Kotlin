package com.brickgame.tetris.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.customLayoutStore: DataStore<Preferences> by preferencesDataStore(name = "custom_layouts")

@Serializable
data class CustomLayoutData(
    val id: String,
    val name: String,
    // Board
    val boardWidthPercent: Float = 0.85f,        // 0.5..1.0 — width of board relative to screen
    val boardPosition: String = "CENTER",         // TOP, CENTER, BOTTOM
    // Info placement
    val infoPosition: String = "TOP_BAR",         // TOP_BAR, LEFT_SIDE, RIGHT_SIDE, BOTTOM_STRIP, HIDDEN
    // What info to show
    val showScore: Boolean = true,
    val showLevel: Boolean = true,
    val showLines: Boolean = true,
    val showHold: Boolean = true,
    val showNext: Boolean = true,
    val nextQueueSize: Int = 1,                   // 1..3
    // Controls
    val controlSize: String = "MEDIUM",           // SMALL, MEDIUM, LARGE
    val showHoldButton: Boolean = true,
    val showPauseButton: Boolean = true
)

class CustomLayoutRepository(private val context: Context) {
    companion object {
        private val LAYOUTS_JSON = stringPreferencesKey("custom_layouts_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val customLayouts: Flow<List<CustomLayoutData>> =
        context.customLayoutStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { prefs ->
                val raw = prefs[LAYOUTS_JSON] ?: "[]"
                try { json.decodeFromString<List<CustomLayoutData>>(raw) } catch (_: Exception) { emptyList() }
            }

    suspend fun saveLayout(layout: CustomLayoutData) {
        context.customLayoutStore.edit { prefs ->
            val raw = prefs[LAYOUTS_JSON] ?: "[]"
            val list = try { json.decodeFromString<List<CustomLayoutData>>(raw).toMutableList() } catch (_: Exception) { mutableListOf() }
            val idx = list.indexOfFirst { it.id == layout.id }
            if (idx >= 0) list[idx] = layout else list.add(layout)
            prefs[LAYOUTS_JSON] = json.encodeToString(list)
        }
    }

    suspend fun deleteLayout(id: String) {
        context.customLayoutStore.edit { prefs ->
            val raw = prefs[LAYOUTS_JSON] ?: "[]"
            val list = try { json.decodeFromString<List<CustomLayoutData>>(raw).toMutableList() } catch (_: Exception) { mutableListOf() }
            list.removeAll { it.id == id }
            prefs[LAYOUTS_JSON] = json.encodeToString(list)
        }
    }
}
