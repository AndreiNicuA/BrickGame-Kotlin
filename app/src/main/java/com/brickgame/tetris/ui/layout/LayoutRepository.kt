package com.brickgame.tetris.ui.layout

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.layoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "layout_profiles")

class LayoutRepository(private val context: Context) {

    companion object {
        private val ACTIVE_LANDSCAPE_ID = stringPreferencesKey("active_landscape_id")
        private val ACTIVE_PORTRAIT_ID = stringPreferencesKey("active_portrait_id")
        private val CUSTOM_PROFILES_JSON = stringPreferencesKey("custom_profiles_json")
        private val SNAP_TO_GRID = booleanPreferencesKey("snap_to_grid")
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    val activeLandscapeProfileId: Flow<String> = context.layoutDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ACTIVE_LANDSCAPE_ID] ?: "builtin_landscape_default" }

    val activePortraitProfileId: Flow<String> = context.layoutDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ACTIVE_PORTRAIT_ID] ?: "builtin_portrait_classic" }

    val snapToGrid: Flow<Boolean> = context.layoutDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SNAP_TO_GRID] ?: true }

    val customProfiles: Flow<List<LayoutProfile>> = context.layoutDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val s = prefs[CUSTOM_PROFILES_JSON] ?: "[]"
            try { json.decodeFromString<List<LayoutProfile>>(s) } catch (_: Exception) { emptyList() }
        }

    fun getAllProfiles(): Flow<List<LayoutProfile>> = customProfiles.map { custom ->
        LayoutPresets.getAllPresets() + custom
    }

    suspend fun saveProfile(profile: LayoutProfile) {
        context.layoutDataStore.edit { prefs ->
            val list = decodeCustom(prefs).toMutableList()
            val idx = list.indexOfFirst { it.id == profile.id }
            if (idx >= 0) list[idx] = profile else list.add(profile)
            prefs[CUSTOM_PROFILES_JSON] = json.encodeToString(list)
        }
    }

    suspend fun deleteProfile(profileId: String) {
        context.layoutDataStore.edit { prefs ->
            val list = decodeCustom(prefs).toMutableList()
            list.removeAll { it.id == profileId }
            prefs[CUSTOM_PROFILES_JSON] = json.encodeToString(list)
            // Reset active if deleted
            if (prefs[ACTIVE_LANDSCAPE_ID] == profileId) prefs[ACTIVE_LANDSCAPE_ID] = "builtin_landscape_default"
            if (prefs[ACTIVE_PORTRAIT_ID] == profileId) prefs[ACTIVE_PORTRAIT_ID] = "builtin_portrait_classic"
        }
    }

    suspend fun setActiveLandscapeProfile(id: String) {
        context.layoutDataStore.edit { it[ACTIVE_LANDSCAPE_ID] = id }
    }

    suspend fun setActivePortraitProfile(id: String) {
        context.layoutDataStore.edit { it[ACTIVE_PORTRAIT_ID] = id }
    }

    suspend fun setSnapToGrid(enabled: Boolean) {
        context.layoutDataStore.edit { it[SNAP_TO_GRID] = enabled }
    }

    /** Export a profile as JSON string for sharing across devices */
    fun exportProfile(profile: LayoutProfile): String = json.encodeToString(profile)

    /** Import a profile from JSON string */
    fun importProfile(jsonStr: String): LayoutProfile? = try {
        json.decodeFromString<LayoutProfile>(jsonStr).copy(
            id = "imported_${System.currentTimeMillis()}",
            isBuiltIn = false
        )
    } catch (_: Exception) { null }

    /** Export ALL custom profiles (for full profile backup) */
    fun exportAllCustom(profiles: List<LayoutProfile>): String =
        json.encodeToString(profiles.filter { !it.isBuiltIn })

    private fun decodeCustom(prefs: Preferences): List<LayoutProfile> {
        val s = prefs[CUSTOM_PROFILES_JSON] ?: "[]"
        return try { json.decodeFromString(s) } catch (_: Exception) { emptyList() }
    }
}
