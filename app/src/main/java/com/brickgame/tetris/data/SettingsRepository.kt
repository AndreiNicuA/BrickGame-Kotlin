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
        private val ANIMATION_STYLE = stringPreferencesKey("animation_style")
        private val ANIMATION_DURATION = floatPreferencesKey("animation_duration")
        private val HIGH_SCORE = intPreferencesKey("high_score")
        private val GHOST_PIECE_ENABLED = booleanPreferencesKey("ghost_piece_enabled")
        private val DIFFICULTY = stringPreferencesKey("difficulty")
        private val PORTRAIT_LAYOUT = stringPreferencesKey("portrait_layout")
        private val LANDSCAPE_LAYOUT = stringPreferencesKey("landscape_layout")
        private val DPAD_STYLE = stringPreferencesKey("dpad_style")
        private val MULTI_COLOR = booleanPreferencesKey("multi_color_pieces")
        private val PIECE_MATERIAL = stringPreferencesKey("piece_material")
        private val CONTROLLER_ENABLED = booleanPreferencesKey("controller_enabled")
        private val CONTROLLER_DEADZONE = floatPreferencesKey("controller_deadzone")
        // General App Settings
        private val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val ORIENTATION_LOCK = stringPreferencesKey("orientation_lock")
        private val IMMERSIVE_MODE = booleanPreferencesKey("immersive_mode")
        private val FRAME_RATE_TARGET = intPreferencesKey("frame_rate_target")
        private val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        private val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        private val UI_SCALE = floatPreferencesKey("ui_scale")
    }
    
    private fun <T> pref(key: Preferences.Key<T>, default: T): Flow<T> =
        context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[key] ?: default }
    
    private suspend fun <T> set(key: Preferences.Key<T>, value: T) { context.dataStore.edit { it[key] = value } }

    val themeName get() = pref(THEME_NAME, "Classic Green")
    suspend fun setThemeName(name: String) = set(THEME_NAME, name)

    val vibrationEnabled get() = pref(VIBRATION_ENABLED, true)
    suspend fun setVibrationEnabled(v: Boolean) = set(VIBRATION_ENABLED, v)

    val vibrationIntensity get() = pref(VIBRATION_INTENSITY, 0.7f)
    suspend fun setVibrationIntensity(v: Float) = set(VIBRATION_INTENSITY, v.coerceIn(0f, 1f))

    val vibrationStyle get() = pref(VIBRATION_STYLE, "CLASSIC")
    suspend fun setVibrationStyle(v: String) = set(VIBRATION_STYLE, v)

    val soundEnabled get() = pref(SOUND_ENABLED, true)
    suspend fun setSoundEnabled(v: Boolean) = set(SOUND_ENABLED, v)

    val soundVolume get() = pref(SOUND_VOLUME, 0.7f)
    suspend fun setSoundVolume(v: Float) = set(SOUND_VOLUME, v.coerceIn(0f, 1f))

    val soundStyle get() = pref(SOUND_STYLE, "RETRO_BEEP")
    suspend fun setSoundStyle(v: String) = set(SOUND_STYLE, v)

    val animationStyle get() = pref(ANIMATION_STYLE, "MODERN")
    suspend fun setAnimationStyle(v: String) = set(ANIMATION_STYLE, v)

    val animationDuration get() = pref(ANIMATION_DURATION, 0.5f)
    suspend fun setAnimationDuration(v: Float) = set(ANIMATION_DURATION, v.coerceIn(0.1f, 2f))

    val highScore get() = pref(HIGH_SCORE, 0)
    suspend fun setHighScore(v: Int) = set(HIGH_SCORE, v)

    val ghostPieceEnabled get() = pref(GHOST_PIECE_ENABLED, true)
    suspend fun setGhostPieceEnabled(v: Boolean) = set(GHOST_PIECE_ENABLED, v)

    val difficulty get() = pref(DIFFICULTY, "NORMAL")
    suspend fun setDifficulty(v: String) = set(DIFFICULTY, v)

    val portraitLayout get() = pref(PORTRAIT_LAYOUT, "PORTRAIT_CLASSIC")
    suspend fun setPortraitLayout(v: String) = set(PORTRAIT_LAYOUT, v)

    val landscapeLayout get() = pref(LANDSCAPE_LAYOUT, "LANDSCAPE_DEFAULT")
    suspend fun setLandscapeLayout(v: String) = set(LANDSCAPE_LAYOUT, v)

    val dpadStyle get() = pref(DPAD_STYLE, "STANDARD")
    suspend fun setDpadStyle(v: String) = set(DPAD_STYLE, v)

    val multiColorEnabled get() = pref(MULTI_COLOR, false)
    suspend fun setMultiColorEnabled(v: Boolean) = set(MULTI_COLOR, v)

    val pieceMaterial get() = pref(PIECE_MATERIAL, "CLASSIC")
    suspend fun setPieceMaterial(v: String) = set(PIECE_MATERIAL, v)

    // Controller settings
    val controllerEnabled get() = pref(CONTROLLER_ENABLED, true)
    suspend fun setControllerEnabled(v: Boolean) = set(CONTROLLER_ENABLED, v)

    val controllerDeadzone get() = pref(CONTROLLER_DEADZONE, 0.25f)
    suspend fun setControllerDeadzone(v: Float) = set(CONTROLLER_DEADZONE, v.coerceIn(0.05f, 0.8f))

    // General App Settings
    val appThemeMode get() = pref(APP_THEME_MODE, "auto")
    suspend fun setAppThemeMode(v: String) = set(APP_THEME_MODE, v)

    val keepScreenOn get() = pref(KEEP_SCREEN_ON, true)
    suspend fun setKeepScreenOn(v: Boolean) = set(KEEP_SCREEN_ON, v)

    val orientationLock get() = pref(ORIENTATION_LOCK, "auto")
    suspend fun setOrientationLock(v: String) = set(ORIENTATION_LOCK, v)

    val immersiveMode get() = pref(IMMERSIVE_MODE, false)
    suspend fun setImmersiveMode(v: Boolean) = set(IMMERSIVE_MODE, v)

    val frameRateTarget get() = pref(FRAME_RATE_TARGET, 60)
    suspend fun setFrameRateTarget(v: Int) = set(FRAME_RATE_TARGET, v)

    val batterySaver get() = pref(BATTERY_SAVER, false)
    suspend fun setBatterySaver(v: Boolean) = set(BATTERY_SAVER, v)

    val highContrast get() = pref(HIGH_CONTRAST, false)
    suspend fun setHighContrast(v: Boolean) = set(HIGH_CONTRAST, v)

    val uiScale get() = pref(UI_SCALE, 1.0f)
    suspend fun setUiScale(v: Float) = set(UI_SCALE, v.coerceIn(0.8f, 1.5f))
}
