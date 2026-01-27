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
        private val VIBRATION_STYLE = stringPreferencesKey("vibration_style")
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        private val SOUND_STYLE = stringPreferencesKey("sound_style")
        private val ANIMATION_ENABLED = booleanPreferencesKey("animation_enabled")
        private val ANIMATION_STYLE = stringPreferencesKey("animation_style")
        private val ANIMATION_DURATION = floatPreferencesKey("animation_duration")
        private val STYLE_PRESET = stringPreferencesKey("style_preset")
        private val HIGH_SCORE = intPreferencesKey("high_score")
        private val LAYOUT_MODE = stringPreferencesKey("layout_mode")
        private val GHOST_PIECE_ENABLED = booleanPreferencesKey("ghost_piece_enabled")
        private val DIFFICULTY = stringPreferencesKey("difficulty")
    }
    
    val themeName: Flow<String> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[THEME_NAME] ?: "Classic" }
    suspend fun setThemeName(name: String) { context.dataStore.edit { it[THEME_NAME] = name } }
    
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[VIBRATION_ENABLED] ?: true }
    suspend fun setVibrationEnabled(enabled: Boolean) { context.dataStore.edit { it[VIBRATION_ENABLED] = enabled } }
    
    val vibrationIntensity: Flow<Float> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[VIBRATION_INTENSITY] ?: 0.7f }
    suspend fun setVibrationIntensity(intensity: Float) { context.dataStore.edit { it[VIBRATION_INTENSITY] = intensity.coerceIn(0f, 1f) } }
    
    val vibrationStyle: Flow<String> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[VIBRATION_STYLE] ?: "CLASSIC" }
    suspend fun setVibrationStyle(style: String) { context.dataStore.edit { it[VIBRATION_STYLE] = style } }
    
    val soundEnabled: Flow<Boolean> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[SOUND_ENABLED] ?: true }
    suspend fun setSoundEnabled(enabled: Boolean) { context.dataStore.edit { it[SOUND_ENABLED] = enabled } }
    
    val soundVolume: Flow<Float> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[SOUND_VOLUME] ?: 0.7f }
    suspend fun setSoundVolume(volume: Float) { context.dataStore.edit { it[SOUND_VOLUME] = volume.coerceIn(0f, 1f) } }
    
    val soundStyle: Flow<String> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[SOUND_STYLE] ?: "RETRO_BEEP" }
    suspend fun setSoundStyle(style: String) { context.dataStore.edit { it[SOUND_STYLE] = style } }
    
    val animationEnabled: Flow<Boolean> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[ANIMATION_ENABLED] ?: true }
    suspend fun setAnimationEnabled(enabled: Boolean) { context.dataStore.edit { it[ANIMATION_ENABLED] = enabled } }
    
    val animationStyle: Flow<String> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[ANIMATION_STYLE] ?: "MODERN" }
    suspend fun setAnimationStyle(style: String) { context.dataStore.edit { it[ANIMATION_STYLE] = style } }
    
    val animationDuration: Flow<Float> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[ANIMATION_DURATION] ?: 0.5f }
    suspend fun setAnimationDuration(duration: Float) { context.dataStore.edit { it[ANIMATION_DURATION] = duration.coerceIn(0.1f, 2f) } }
    
    val stylePreset: Flow<String> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[STYLE_PRESET] ?: "CUSTOM" }
    suspend fun setStylePreset(preset: String) { context.dataStore.edit { it[STYLE_PRESET] = preset } }
    
    val highScore: Flow<Int> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[HIGH_SCORE] ?: 0 }
    suspend fun setHighScore(score: Int) { context.dataStore.edit { it[HIGH_SCORE] = score } }
    
    val layoutMode: Flow<String> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[LAYOUT_MODE] ?: "CLASSIC" }
    suspend fun setLayoutMode(mode: String) { context.dataStore.edit { it[LAYOUT_MODE] = mode } }
    
    val ghostPieceEnabled: Flow<Boolean> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[GHOST_PIECE_ENABLED] ?: true }
    suspend fun setGhostPieceEnabled(enabled: Boolean) { context.dataStore.edit { it[GHOST_PIECE_ENABLED] = enabled } }
    
    val difficulty: Flow<String> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[DIFFICULTY] ?: "NORMAL" }
    suspend fun setDifficulty(diff: String) { context.dataStore.edit { it[DIFFICULTY] = diff } }
}
