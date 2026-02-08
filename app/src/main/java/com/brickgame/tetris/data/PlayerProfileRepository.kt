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

/**
 * Repository for the unified PlayerProfile.
 * 
 * On first launch (no profile JSON found), migrates existing values from
 * SettingsRepository and PlayerRepository into a single PlayerProfile.
 * After that, PlayerProfile is the single source of truth and syncs back
 * to the legacy repos on save so existing code keeps working.
 */
class PlayerProfileRepository(private val context: Context) {

    companion object {
        private val PROFILE_JSON = stringPreferencesKey("profile_json")
        private val MIGRATED = booleanPreferencesKey("migrated_from_legacy")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * Flow of the current player profile.
     */
    val profile: Flow<PlayerProfile> = context.profileDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val raw = prefs[PROFILE_JSON]
            if (raw != null) {
                try { json.decodeFromString<PlayerProfile>(raw) }
                catch (_: Exception) { PlayerProfile() }
            } else {
                PlayerProfile()
            }
        }

    /**
     * Get the current profile snapshot (suspend).
     */
    suspend fun getProfile(): PlayerProfile {
        return profile.first()
    }

    /**
     * Save the entire profile.
     */
    suspend fun saveProfile(playerProfile: PlayerProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[PROFILE_JSON] = json.encodeToString(playerProfile)
        }
    }

    /**
     * Update a single field of the profile using a transform lambda.
     */
    suspend fun updateProfile(transform: (PlayerProfile) -> PlayerProfile) {
        context.profileDataStore.edit { prefs ->
            val current = prefs[PROFILE_JSON]?.let {
                try { json.decodeFromString<PlayerProfile>(it) }
                catch (_: Exception) { PlayerProfile() }
            } ?: PlayerProfile()
            val updated = transform(current)
            prefs[PROFILE_JSON] = json.encodeToString(updated)
        }
    }

    /**
     * Update a single freeform control position.
     */
    suspend fun updateFreeformPosition(elementKey: String, position: FreeformPosition) {
        updateProfile { profile ->
            profile.copy(
                freeformPositions = profile.freeformPositions + (elementKey to position)
            )
        }
    }

    /**
     * Update a single freeform info position.
     */
    suspend fun updateFreeformInfoPosition(elementKey: String, position: FreeformPosition) {
        updateProfile { profile ->
            profile.copy(
                freeformInfoPositions = profile.freeformInfoPositions + (elementKey to position)
            )
        }
    }

    /**
     * Reset all freeform positions to defaults.
     */
    suspend fun resetFreeformPositions() {
        updateProfile { profile ->
            profile.copy(
                freeformPositions = PlayerProfile.defaultFreeformPositions(),
                freeformInfoPositions = PlayerProfile.defaultFreeformInfoPositions()
            )
        }
    }

    /**
     * Increment game stats after a game ends.
     */
    suspend fun recordGamePlayed(score: Int, lines: Int, playTimeSeconds: Long) {
        updateProfile { profile ->
            profile.copy(
                totalGamesPlayed = profile.totalGamesPlayed + 1,
                totalLinesCleared = profile.totalLinesCleared + lines,
                totalPlayTimeSeconds = profile.totalPlayTimeSeconds + playTimeSeconds,
                highScore = maxOf(profile.highScore, score)
            )
        }
    }

    /**
     * Check if migration has been done, if not, migrate from legacy repos.
     */
    suspend fun migrateIfNeeded(settingsRepo: SettingsRepository, playerRepo: PlayerRepository) {
        val prefs = context.profileDataStore.data.first()
        if (prefs[MIGRATED] == true) return

        // Read current values from legacy stores
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
            name = name,
            themeName = themeName,
            difficulty = difficulty,
            ghostPieceEnabled = ghostPiece,
            multiColorPieces = multiColor,
            portraitLayout = portraitLayout,
            landscapeLayout = landscapeLayout,
            dpadStyle = dpadStyle,
            soundEnabled = soundEnabled,
            soundVolume = soundVolume,
            soundStyle = soundStyle,
            vibrationEnabled = vibEnabled,
            vibrationIntensity = vibIntensity,
            vibrationStyle = vibStyle,
            animationStyle = animStyle,
            animationDuration = animDuration,
            highScore = highScore
        )

        context.profileDataStore.edit { p ->
            p[PROFILE_JSON] = json.encodeToString(migrated)
            p[MIGRATED] = true
        }
    }

    /**
     * Sync profile changes back to legacy SettingsRepository.
     * Call after saving profile so existing code that reads from SettingsRepo stays in sync.
     */
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
