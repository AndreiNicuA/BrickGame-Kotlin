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

private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(name = "player_data")

/**
 * Player profile and score history repository
 */
class PlayerRepository(private val context: Context) {
    
    companion object {
        private val PLAYER_NAME = stringPreferencesKey("player_name")
        private val SCORE_HISTORY = stringPreferencesKey("score_history")
        private val CUSTOM_THEME = stringPreferencesKey("custom_theme")
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // Player name
    val playerName: Flow<String> = context.playerDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PLAYER_NAME] ?: "Player" }
    
    suspend fun setPlayerName(name: String) {
        context.playerDataStore.edit { it[PLAYER_NAME] = name }
    }
    
    // Score history
    val scoreHistory: Flow<List<ScoreEntry>> = context.playerDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val jsonString = prefs[SCORE_HISTORY] ?: "[]"
            try {
                json.decodeFromString<List<ScoreEntry>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }
    
    suspend fun addScore(score: Int, level: Int, lines: Int) {
        context.playerDataStore.edit { prefs ->
            val currentJson = prefs[SCORE_HISTORY] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<ScoreEntry>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            currentList.add(0, ScoreEntry(
                score = score,
                level = level,
                lines = lines,
                timestamp = System.currentTimeMillis()
            ))
            
            // Keep only last 50 scores
            val trimmedList = currentList.take(50)
            prefs[SCORE_HISTORY] = json.encodeToString(trimmedList)
        }
    }
    
    suspend fun clearHistory() {
        context.playerDataStore.edit { it[SCORE_HISTORY] = "[]" }
    }
    
    // Custom theme colors
    val customTheme: Flow<CustomThemeColors?> = context.playerDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val jsonString = prefs[CUSTOM_THEME]
            if (jsonString != null) {
                try {
                    json.decodeFromString<CustomThemeColors>(jsonString)
                } catch (e: Exception) {
                    null
                }
            } else null
        }
    
    suspend fun saveCustomTheme(colors: CustomThemeColors) {
        context.playerDataStore.edit { 
            it[CUSTOM_THEME] = json.encodeToString(colors)
        }
    }
    
    suspend fun clearCustomTheme() {
        context.playerDataStore.edit { it.remove(CUSTOM_THEME) }
    }
}

@Serializable
data class ScoreEntry(
    val score: Int,
    val level: Int,
    val lines: Int,
    val timestamp: Long
)

@Serializable
data class CustomThemeColors(
    val name: String = "Custom",
    val deviceColor: Long,
    val screenBackground: Long,
    val pixelOn: Long,
    val pixelOff: Long,
    val buttonPrimary: Long,
    val accentColor: Long,
    val decoColor1: Long,
    val decoColor2: Long
)
