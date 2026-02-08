package com.brickgame.tetris.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "player_profile")

class PlayerProfileRepository(private val context: Context) {

    companion object {
        private val PROFILE_JSON = stringPreferencesKey("profile_json")
        private val MIGRATED = booleanPreferencesKey("migrated_from_legacy")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val profile: Flow<PlayerProfile> = context.profileDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val raw = prefs[PROFILE_JSON]
            if (raw != null) {
                try { json.decodeFromString<PlayerProfile>(raw) }
                catch (_: Exception) { PlayerProfile() }
            } else PlayerProfile()
        }

    suspend fun getProfile(): PlayerProfile = profile.first()

    suspend fun saveProfile(playerProfile: PlayerProfile) {
        context.profileDataStore.edit { it[PROFILE_JSON] = json.encodeToString(playerProfile) }
    }

    suspend fun updateProfile(transform: (PlayerProfile) -> PlayerProfile) {
        context.profileDataStore.edit { prefs ->
            val current = prefs[PROFILE_JSON]?.let {
                try { json.decodeFromString<PlayerProfile>(it) } catch (_: Exception) { PlayerProfile() }
            } ?: PlayerProfile()
            prefs[PROFILE_JSON] = json.encodeToString(transform(current))
        }
    }

    /** Update a single freeform element (position, size, alpha, visibility) */
    suspend fun updateFreeformElement(element: FreeformElement) {
        updateProfile { p ->
            p.copy(freeformElements = p.freeformElements + (element.key to element))
        }
    }

    /** Add a new element to the freeform layout */
    suspend fun addFreeformElement(element: FreeformElement) = updateFreeformElement(element)

    /** Remove an element from the freeform layout */
    suspend fun removeFreeformElement(key: String) {
        updateProfile { p ->
            p.copy(freeformElements = p.freeformElements - key)
        }
    }

    /** Reset all freeform elements to defaults */
    suspend fun resetFreeformElements() {
        updateProfile { it.copy(freeformElements = PlayerProfile.defaultFreeformElements()) }
    }

    suspend fun recordGamePlayed(score: Int, lines: Int, playTimeSeconds: Long) {
        updateProfile { p ->
            p.copy(
                totalGamesPlayed = p.totalGamesPlayed + 1,
                totalLinesCleared = p.totalLinesCleared + lines,
                totalPlayTimeSeconds = p.totalPlayTimeSeconds + playTimeSeconds,
                highScore = maxOf(p.highScore, score)
            )
        }
    }

    suspend fun migrateIfNeeded(settingsRepo: SettingsRepository, playerRepo: PlayerRepository) {
        val prefs = context.profileDataStore.data.first()
        if (prefs[MIGRATED] == true) return

        val name = playerRepo.playerName.first()
        val themeName = settingsRepo.themeName.first()
        val difficulty = settingsRepo.difficulty.first()
        val ghostPiece = settingsRepo.ghostPieceEnabled.first()
        val multiColor = settingsRepo.multiColorEnabled.first()
        val portraitLayout = settingsRepo.portraitLayout.first()
        val landscapeLayout = settingsRepo.landscapeLayout.first()
        val dpadStyle = settingsRepo.dpadStyle.first()
        val soundEnabled = settingsRepo.soundEnabled.first()
        val soundVolume = settingsRepo.soundVolume.first()
        val soundStyle = settingsRepo.soundStyle.first()
        val vibEnabled = settingsRepo.vibrationEnabled.first()
        val vibIntensity = settingsRepo.vibrationIntensity.first()
        val vibStyle = settingsRepo.vibrationStyle.first()
        val animStyle = settingsRepo.animationStyle.first()
        val animDuration = settingsRepo.animationDuration.first()
        val highScore = settingsRepo.highScore.first()

        val migrated = PlayerProfile(
            name = name, themeName = themeName, difficulty = difficulty,
            ghostPieceEnabled = ghostPiece, multiColorPieces = multiColor,
            portraitLayout = portraitLayout, landscapeLayout = landscapeLayout,
            dpadStyle = dpadStyle, soundEnabled = soundEnabled, soundVolume = soundVolume,
            soundStyle = soundStyle, vibrationEnabled = vibEnabled,
            vibrationIntensity = vibIntensity, vibrationStyle = vibStyle,
            animationStyle = animStyle, animationDuration = animDuration, highScore = highScore
        )

        context.profileDataStore.edit { p ->
            p[PROFILE_JSON] = json.encodeToString(migrated)
            p[MIGRATED] = true
        }
    }

    suspend fun syncToLegacy(profile: PlayerProfile, settingsRepo: SettingsRepository, playerRepo: PlayerRepository) {
        playerRepo.setPlayerName(profile.name)
        settingsRepo.setThemeName(profile.themeName)
        settingsRepo.setDifficulty(profile.difficulty)
        settingsRepo.setGhostPieceEnabled(profile.ghostPieceEnabled)
        settingsRepo.setMultiColorEnabled(profile.multiColorPieces)
        settingsRepo.setPortraitLayout(profile.portraitLayout)
        settingsRepo.setLandscapeLayout(profile.landscapeLayout)
        settingsRepo.setDpadStyle(profile.dpadStyle)
        settingsRepo.setSoundEnabled(profile.soundEnabled)
        settingsRepo.setSoundVolume(profile.soundVolume)
        settingsRepo.setSoundStyle(profile.soundStyle)
        settingsRepo.setVibrationEnabled(profile.vibrationEnabled)
        settingsRepo.setVibrationIntensity(profile.vibrationIntensity)
        settingsRepo.setVibrationStyle(profile.vibrationStyle)
        settingsRepo.setAnimationStyle(profile.animationStyle)
        settingsRepo.setAnimationDuration(profile.animationDuration)
        settingsRepo.setHighScore(profile.highScore)
    }
}
