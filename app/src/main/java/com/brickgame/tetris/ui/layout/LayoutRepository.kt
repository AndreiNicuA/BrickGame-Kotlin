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
        private val ACTIVE_LANDSCAPE_PROFILE_ID = stringPreferencesKey("active_landscape_profile_id")
        private val ACTIVE_PORTRAIT_PROFILE_ID = stringPreferencesKey("active_portrait_profile_id")
        private val CUSTOM_PROFILES_JSON = stringPreferencesKey("custom_profiles_json")
        private val SNAP_TO_GRID = booleanPreferencesKey("snap_to_grid")
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    val activeLandscapeProfileId: Flow<String> = context.layoutDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ACTIVE_LANDSCAPE_PROFILE_ID] ?: "default_landscape" }
    
    val activePortraitProfileId: Flow<String> = context.layoutDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ACTIVE_PORTRAIT_PROFILE_ID] ?: "default_portrait" }
    
    val snapToGrid: Flow<Boolean> = context.layoutDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SNAP_TO_GRID] ?: true }
    
    val customProfiles: Flow<List<LayoutProfile>> = context.layoutDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val jsonStr = prefs[CUSTOM_PROFILES_JSON] ?: "[]"
            try {
                json.decodeFromString<List<LayoutProfile>>(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }
    
    fun getAllProfiles(): Flow<List<LayoutProfile>> = customProfiles.map { custom ->
        LayoutPresets.getAllPresets() + custom
    }
    
    suspend fun saveProfile(profile: LayoutProfile) {
        context.layoutDataStore.edit { prefs ->
            val jsonStr = prefs[CUSTOM_PROFILES_JSON] ?: "[]"
            val existing = try {
                json.decodeFromString<List<LayoutProfile>>(jsonStr).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            val index = existing.indexOfFirst { it.id == profile.id }
            if (index >= 0) {
                existing[index] = profile
            } else {
                existing.add(profile)
            }
            
            prefs[CUSTOM_PROFILES_JSON] = json.encodeToString(existing)
        }
    }
    
    suspend fun deleteProfile(profileId: String) {
        context.layoutDataStore.edit { prefs ->
            val jsonStr = prefs[CUSTOM_PROFILES_JSON] ?: "[]"
            val existing = try {
                json.decodeFromString<List<LayoutProfile>>(jsonStr).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            existing.removeAll { it.id == profileId }
            prefs[CUSTOM_PROFILES_JSON] = json.encodeToString(existing)
        }
    }
    
    suspend fun setActiveLandscapeProfile(profileId: String) {
        context.layoutDataStore.edit { it[ACTIVE_LANDSCAPE_PROFILE_ID] = profileId }
    }
    
    suspend fun setActivePortraitProfile(profileId: String) {
        context.layoutDataStore.edit { it[ACTIVE_PORTRAIT_PROFILE_ID] = profileId }
    }
    
    suspend fun setSnapToGrid(enabled: Boolean) {
        context.layoutDataStore.edit { it[SNAP_TO_GRID] = enabled }
    }
    
    /**
     * Export a profile as JSON string (for sharing)
     */
    fun exportProfile(profile: LayoutProfile): String {
        return json.encodeToString(profile)
    }
    
    /**
     * Import a profile from JSON string
     */
    fun importProfile(jsonStr: String): LayoutProfile? {
        return try {
            json.decodeFromString<LayoutProfile>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }
}
