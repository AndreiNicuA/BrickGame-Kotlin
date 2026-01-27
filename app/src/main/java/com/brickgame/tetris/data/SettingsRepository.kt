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
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
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
    
    // Vibration - default TRUE
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[VIBRATION_ENABLED] ?: true }
    
    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }
    
    // Sound - default TRUE
    val soundEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SOUND_ENABLED] ?: true }
    
    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SOUND_ENABLED] = enabled }
    }
    
    // High Score
    val highScore: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[HIGH_SCORE] ?: 0 }
    
    suspend fun setHighScore(score: Int) {
        context.dataStore.edit { it[HIGH_SCORE] = score }
    }
    
    // Layout mode: CLASSIC, MODERN, FULLSCREEN
    val layoutMode: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[LAYOUT_MODE] ?: "CLASSIC" }
    
    suspend fun setLayoutMode(mode: String) {
        context.dataStore.edit { it[LAYOUT_MODE] = mode }
    }
}
