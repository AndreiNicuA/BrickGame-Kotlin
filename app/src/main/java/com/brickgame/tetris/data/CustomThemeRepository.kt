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

private val Context.customThemeStore: DataStore<Preferences> by preferencesDataStore(name = "custom_themes")

/**
 * Serializable theme data for storage. Colors stored as ARGB hex longs.
 */
@Serializable
data class CustomThemeData(
    val id: String,
    val name: String,
    val backgroundColor: Long,
    val deviceColor: Long,
    val screenBackground: Long,
    val pixelOn: Long,
    val pixelOff: Long,
    val textPrimary: Long,
    val textSecondary: Long,
    val buttonPrimary: Long,
    val buttonPrimaryPressed: Long,
    val buttonSecondary: Long,
    val buttonSecondaryPressed: Long,
    val accentColor: Long
)

class CustomThemeRepository(private val context: Context) {
    companion object {
        private val THEMES_JSON = stringPreferencesKey("custom_themes_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val customThemes: Flow<List<CustomThemeData>> =
        context.customThemeStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { prefs ->
                val raw = prefs[THEMES_JSON] ?: "[]"
                try { json.decodeFromString<List<CustomThemeData>>(raw) } catch (_: Exception) { emptyList() }
            }

    suspend fun saveTheme(theme: CustomThemeData) {
        context.customThemeStore.edit { prefs ->
            val raw = prefs[THEMES_JSON] ?: "[]"
            val list = try { json.decodeFromString<List<CustomThemeData>>(raw).toMutableList() } catch (_: Exception) { mutableListOf() }
            val idx = list.indexOfFirst { it.id == theme.id }
            if (idx >= 0) list[idx] = theme else list.add(theme)
            prefs[THEMES_JSON] = json.encodeToString(list)
        }
    }

    suspend fun deleteTheme(id: String) {
        context.customThemeStore.edit { prefs ->
            val raw = prefs[THEMES_JSON] ?: "[]"
            val list = try { json.decodeFromString<List<CustomThemeData>>(raw).toMutableList() } catch (_: Exception) { mutableListOf() }
            list.removeAll { it.id == id }
            prefs[THEMES_JSON] = json.encodeToString(list)
        }
    }
}
