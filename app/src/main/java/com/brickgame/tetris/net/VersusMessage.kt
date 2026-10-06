package com.brickgame.tetris.net

/**
 * Messages between the two Versus phones, encoded as one short text line ("KIND|field|field").
 * Pure Kotlin, unit-tested.
 */
sealed class VersusMessage {
    /** Sent right after connecting. */
    data class Hello(val name: String) : VersusMessage()
    /** Start a round now, in this game style (CLASSIC / NEON / THREE_D). */
    data class Go(val style: String) : VersusMessage()
    /** Send [rows] garbage rows (2D) or layers (3D) to the other player. */
    data class Attack(val rows: Int) : VersusMessage()
    /** Live stats for the opponent panel. */
    data class Status(val score: Int, val lines: Int, val level: Int) : VersusMessage()
    /** The sender topped out — the receiver wins the round. */
    object Over : VersusMessage()
    /** The sender left Versus. */
    object Bye : VersusMessage()

    fun encode(): String = when (this) {
        is Hello -> "HI|" + name.replace("|", " ").take(24)
        is Go -> "GO|$style"
        is Attack -> "ATK|$rows"
        is Status -> "ST|$score|$lines|$level"
        Over -> "OVER"
        Bye -> "BYE"
    }

    companion object {
        /** Null for anything unknown or malformed (newer app versions may send more kinds). */
        fun parse(line: String): VersusMessage? {
            val f = line.trim().split('|')
            return try {
                when (f[0]) {
                    "HI" -> Hello(f.getOrElse(1) { "Player" }.ifBlank { "Player" })
                    "GO" -> Go(f[1])
                    "ATK" -> Attack(f[1].toInt().coerceIn(0, 20))
                    "ST" -> Status(f[1].toInt(), f[2].toInt(), f[3].toInt())
                    "OVER" -> Over
                    "BYE" -> Bye
                    else -> null
                }
            } catch (_: Exception) { null }
        }

        /** Rows sent for a 2D line clear: 2→1, 3→2, 4→4 (classic versus rules). */
        fun attackFor2DClear(lines: Int): Int = when (lines) { 2 -> 1; 3 -> 2; 4 -> 4; else -> 0 }

        /** Layers sent for a 3D layer clear: every cleared layer is sent (3D clears are rare). */
        fun attackFor3DClear(layers: Int): Int = layers.coerceAtMost(4)
    }
}
