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
data class ElementPosition(val x: Float, val y: Float)

object LayoutElements {
    const val SCORE = "SCORE"
    const val LEVEL = "LEVEL"
    const val LINES = "LINES"
    const val HOLD_PREVIEW = "HOLD_PREVIEW"
    const val NEXT_PREVIEW = "NEXT_PREVIEW"
    const val BOARD = "BOARD"
    const val DPAD = "DPAD"
    const val ROTATE_BTN = "ROTATE_BTN"
    const val HOLD_BTN = "HOLD_BTN"
    const val PAUSE_BTN = "PAUSE_BTN"
    const val MENU_BTN = "MENU_BTN"

    val topBarElements = listOf(HOLD_PREVIEW, SCORE, LEVEL, LINES, NEXT_PREVIEW)
    val controlElements = listOf(DPAD, ROTATE_BTN, HOLD_BTN, PAUSE_BTN, MENU_BTN)
    val allElements = topBarElements + BOARD + controlElements
    val hideable = listOf(SCORE, LEVEL, LINES, HOLD_PREVIEW, NEXT_PREVIEW, HOLD_BTN, PAUSE_BTN)
}

/**
 * 3-zone layout:
 *   TOP — optional info bar with draggable element order
 *   MIDDLE — LCD board with alignment and size
 *   BOTTOM — individually draggable controls
 */
@Serializable
data class CustomLayoutData(
    val id: String,
    val name: String,
    // TOP BAR
    val topBarVisible: Boolean = true,
    val topBarStyle: String = "COMPACT",            // DEVICE_FRAME, COMPACT, MINIMAL
    val topBarElementOrder: List<String> = LayoutElements.topBarElements,
    // BOARD
    val boardAlignment: String = "CENTER",          // LEFT, CENTER, RIGHT
    val boardSize: String = "STANDARD",             // COMPACT, STANDARD, FULLSCREEN
    val boardInfoOverlay: String = "TOP",           // TOP, SIDE, HIDDEN (when topBar hidden)
    val boardGridLines: Boolean = false,
    // CONTROLS
    val controlPositions: Map<String, ElementPosition> = defaultControlPositions(),
    val controlSize: String = "MEDIUM",
    val elementSizes: Map<String, String> = emptyMap(),
    val elementStyles: Map<String, String> = emptyMap(),
    val dpadStyle: String = "STANDARD",             // STANDARD, ROTATE_CENTER
    // VISIBILITY
    val visibility: Map<String, Boolean> = LayoutElements.allElements.associateWith { true },
    val nextQueueSize: Int = 3,
    // GENERAL
    val autoRotate: Boolean = false,
    // Legacy compat
    val positions: Map<String, ElementPosition> = emptyMap(),
    val baseLayout: String = "CLASSIC",
    val infoMode: String = "DEFAULT"
) {
    fun sizeFor(elem: String): String = elementSizes[elem] ?: controlSize
    fun styleFor(elem: String): String = elementStyles[elem] ?: "DEFAULT"
    fun isVisible(elem: String): Boolean = visibility.getOrDefault(elem, true)

    companion object {
        fun defaultControlPositions() = mapOf(
            LayoutElements.DPAD to ElementPosition(0.15f, 0.5f),
            LayoutElements.ROTATE_BTN to ElementPosition(0.85f, 0.5f),
            LayoutElements.HOLD_BTN to ElementPosition(0.5f, 0.2f),
            LayoutElements.PAUSE_BTN to ElementPosition(0.5f, 0.55f),
            LayoutElements.MENU_BTN to ElementPosition(0.5f, 0.88f)
        )
        fun defaultPositions() = emptyMap<String, ElementPosition>()
    }
}

class CustomLayoutRepository(private val context: Context) {
    companion object { private val LAYOUTS_JSON = stringPreferencesKey("custom_layouts_json_v2") }
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
