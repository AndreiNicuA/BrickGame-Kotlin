package com.brickgame.tetris.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        private val THEME_NAME = stringPreferencesKey("theme_name")
        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val VIBRATION_INTENSITY = floatPreferencesKey("vibration_intensity")
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        private val HIGH_SCORE = intPreferencesKey("high_score")
        private val LAYOUT_MODE = stringPreferencesKey("layout_mode")
    }
    
    // Theme
    val themeName: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[THEME_NAME] ?: "Classic" }
    
    suspend fun setThemeName(name: String) {
        context.dataStore.edit { it[THEME_NAME] = name }
    }
    
    // Vibration enabled
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[VIBRATION_ENABLED] ?: true }
    
    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }
    
    // Vibration intensity (0.0 to 1.0)
    val vibrationIntensity: Flow<Float> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[VIBRATION_INTENSITY] ?: 0.7f }
    
    suspend fun setVibrationIntensity(intensity: Float) {
        context.dataStore.edit { it[VIBRATION_INTENSITY] = intensity.coerceIn(0f, 1f) }
    }
    
    // Sound enabled
    val soundEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SOUND_ENABLED] ?: true }
    
    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SOUND_ENABLED] = enabled }
    }
    
    // Sound volume (0.0 to 1.0)
    val soundVolume: Flow<Float> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SOUND_VOLUME] ?: 0.7f }
    
    suspend fun setSoundVolume(volume: Float) {
        context.dataStore.edit { it[SOUND_VOLUME] = volume.coerceIn(0f, 1f) }
    }
    
    // High Score
    val highScore: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[HIGH_SCORE] ?: 0 }
    
    suspend fun setHighScore(score: Int) {
        context.dataStore.edit { it[HIGH_SCORE] = score }
    }
    
    // Layout mode
    val layoutMode: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[LAYOUT_MODE] ?: "CLASSIC" }
    
    suspend fun setLayoutMode(mode: String) {
        context.dataStore.edit { it[LAYOUT_MODE] = mode }
    }
}
