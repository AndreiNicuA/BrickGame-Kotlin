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

/**
 * Normalized position (0..1 range) like RoadTrip's ElementPosition
 */
@Serializable
data class ElementPosition(val x: Float, val y: Float)

/**
 * Identifiers for every movable/toggleable UI element
 */
object LayoutElements {
    const val BOARD = "BOARD"
    const val SCORE = "SCORE"
    const val LEVEL = "LEVEL"
    const val LINES = "LINES"
    const val HOLD_PREVIEW = "HOLD_PREVIEW"
    const val NEXT_PREVIEW = "NEXT_PREVIEW"
    const val DPAD = "DPAD"
    const val ROTATE_BTN = "ROTATE_BTN"
    const val HOLD_BTN = "HOLD_BTN"
    const val PAUSE_BTN = "PAUSE_BTN"
    const val MENU_BTN = "MENU_BTN"     // Cannot be hidden

    val allElements = listOf(BOARD, SCORE, LEVEL, LINES, HOLD_PREVIEW, NEXT_PREVIEW, DPAD, ROTATE_BTN, HOLD_BTN, PAUSE_BTN, MENU_BTN)
    val hideable = allElements - MENU_BTN  // Everything except menu can be hidden
}

/**
 * Custom layout config: base layout + per-element visibility/size/style options
 * No more free-form positioning — uses real Compose layouts as base.
 */
@Serializable
data class CustomLayoutData(
    val id: String,
    val name: String,
    val baseLayout: String = "CLASSIC",                    // CLASSIC, MODERN, FULLSCREEN
    val positions: Map<String, ElementPosition> = emptyMap(), // Legacy — kept for compatibility
    val visibility: Map<String, Boolean> = LayoutElements.allElements.associateWith { true },
    val nextQueueSize: Int = 3,
    val controlSize: String = "MEDIUM",                    // Global control size: SMALL/MEDIUM/LARGE
    val elementSizes: Map<String, String> = emptyMap(),    // Per-element override: SMALL/MEDIUM/LARGE
    val elementStyles: Map<String, String> = emptyMap(),   // Per-element style options
    val infoMode: String = "DEFAULT",                      // DEFAULT (use base layout's info), COMPACT, HIDDEN
    val controlsSwapped: Boolean = false,                  // Swap DPad ↔ Rotate sides
    val showGhostGrid: Boolean = true                      // Show grid lines in board
) {
    fun sizeFor(elem: String): String = elementSizes[elem] ?: controlSize
    fun styleFor(elem: String): String = elementStyles[elem] ?: "DEFAULT"

    companion object {
        fun defaultPositions() = emptyMap<String, ElementPosition>()
    }
}

class CustomLayoutRepository(private val context: Context) {
    companion object {
        private val LAYOUTS_JSON = stringPreferencesKey("custom_layouts_json_v2")
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
