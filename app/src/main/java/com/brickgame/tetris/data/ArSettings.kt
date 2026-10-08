package com.brickgame.tetris.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Player settings for the 3D / AR well. Stored as one JSON value, so new fields only need a
 * default here (old saves keep working).
 */
@Serializable
data class ArSettings(
    /** Size the well starts at: TABLE, BIG, ROOM or INSIDE. */
    val defaultSize: String = "TABLE",
    /** Play area: OFF, SMALL (1.5 m), MEDIUM (2 m), LARGE (3 m), CUSTOM (width × depth) or CORNERS (tap them). */
    val playArea: String = "MEDIUM",
    val customWidthM: Float = 2f,
    val customDepthM: Float = 2f,
    /** Show the scanned floor as a mesh while placing the well / setting up the area. */
    val floorMesh: Boolean = true,
    /** 3D arrow in front of the phone pointing to the falling piece when it's out of view. */
    val arrow: Boolean = true,
    /** Experimental: pinch the falling piece with your hand in front of the camera. */
    val hands: Boolean = false,
    /** Hold a hand flat on a surface for 3 s to place the well there. */
    val handPlace: Boolean = true
) {
    /** Width × depth in metres for the preset areas; null for OFF / CORNERS. */
    fun areaSize(): Pair<Float, Float>? = when (playArea) {
        "SMALL" -> 1.5f to 1.5f
        "MEDIUM" -> 2f to 2f
        "LARGE" -> 3f to 3f
        "CUSTOM" -> customWidthM to customDepthM
        else -> null
    }

    /** Cube edge in metres for [defaultSize]. */
    fun defaultCell(): Float = when (defaultSize) { "BIG" -> 0.08f; "ROOM", "INSIDE" -> 0.2f; else -> 0.03f }

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun decode(raw: String?): ArSettings =
            if (raw.isNullOrBlank()) ArSettings() else try { json.decodeFromString(serializer(), raw) } catch (_: Exception) { ArSettings() }
    }
}
