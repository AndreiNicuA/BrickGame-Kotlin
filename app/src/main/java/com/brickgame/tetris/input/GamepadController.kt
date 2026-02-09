package com.brickgame.tetris.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Gamepad / controller input handler for Brick Game.
 *
 * Maps physical controller inputs to game actions.
 * Supports: Xbox, PlayStation, Switch Pro, and generic Bluetooth/USB controllers.
 *
 * Default mapping:
 *   D-Pad / Left Stick → Move piece (left/right/down/up in 3D)
 *   A / Cross         → Rotate (spin XZ)
 *   B / Circle        → Rotate (tilt XY in 3D, or rotate in 2D)
 *   X / Square        → Hold piece
 *   Y / Triangle      → Hard drop
 *   L1 / LB           → Move Z- (3D) / unused (2D)
 *   R1 / RB           → Move Z+ (3D) / unused (2D)
 *   Start             → Pause / Start
 *   Select            → Settings
 *   L Stick click     → Toggle gravity (3D only)
 */
class GamepadController {

    /** Game actions the controller can trigger. */
    enum class Action {
        MOVE_LEFT, MOVE_RIGHT, SOFT_DROP, MOVE_UP,
        MOVE_Z_FORWARD, MOVE_Z_BACKWARD,
        ROTATE_XZ, ROTATE_XY,
        HARD_DROP, HOLD,
        PAUSE, SETTINGS,
        TOGGLE_GRAVITY
    }

    data class ControllerInfo(
        val name: String,
        val vendorId: Int,
        val productId: Int,
        val isConnected: Boolean
    )

    var enabled: Boolean = true
    var deadzone: Float = 0.25f

    // Callbacks
    var onAction: ((Action) -> Unit)? = null

    /** Check if a KeyEvent comes from a game controller. */
    fun isGamepad(event: KeyEvent): Boolean {
        val source = event.source
        return (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
               (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
               (source and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
    }

    /** Handle key down event. Returns true if consumed. */
    fun handleKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!enabled || !isGamepad(event)) return false

        val action = mapKeyToAction(keyCode) ?: return false
        onAction?.invoke(action)
        return true
    }

    /** Handle key up — used for repeat prevention. Returns true if consumed. */
    fun handleKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!enabled || !isGamepad(event)) return false
        // Consume the key up for any mapped key to prevent default behavior
        return mapKeyToAction(keyCode) != null
    }

    /** Handle analog stick / trigger motion events. */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        if (!enabled) return false
        if (event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK) return false

        // Left stick
        val axisX = event.getAxisValue(MotionEvent.AXIS_X)
        val axisY = event.getAxisValue(MotionEvent.AXIS_Y)

        // Hat (D-Pad on some controllers)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        val x = if (kotlin.math.abs(hatX) > 0.5f) hatX else if (kotlin.math.abs(axisX) > deadzone) axisX else 0f
        val y = if (kotlin.math.abs(hatY) > 0.5f) hatY else if (kotlin.math.abs(axisY) > deadzone) axisY else 0f

        // Determine dominant direction
        if (kotlin.math.abs(x) > kotlin.math.abs(y)) {
            if (x > deadzone) onAction?.invoke(Action.MOVE_RIGHT)
            else if (x < -deadzone) onAction?.invoke(Action.MOVE_LEFT)
        } else {
            if (y > deadzone) onAction?.invoke(Action.SOFT_DROP)
            else if (y < -deadzone) onAction?.invoke(Action.MOVE_UP)
        }

        return true
    }

    /** Map a keycode to a game action. */
    private fun mapKeyToAction(keyCode: Int): Action? = when (keyCode) {
        // D-Pad
        KeyEvent.KEYCODE_DPAD_LEFT -> Action.MOVE_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> Action.MOVE_RIGHT
        KeyEvent.KEYCODE_DPAD_DOWN -> Action.SOFT_DROP
        KeyEvent.KEYCODE_DPAD_UP -> Action.MOVE_UP

        // Face buttons (A/B/X/Y — Xbox naming)
        KeyEvent.KEYCODE_BUTTON_A -> Action.ROTATE_XZ       // A / Cross → Spin
        KeyEvent.KEYCODE_BUTTON_B -> Action.ROTATE_XY       // B / Circle → Tilt
        KeyEvent.KEYCODE_BUTTON_X -> Action.HOLD             // X / Square → Hold
        KeyEvent.KEYCODE_BUTTON_Y -> Action.HARD_DROP        // Y / Triangle → Hard Drop

        // Shoulder buttons
        KeyEvent.KEYCODE_BUTTON_L1 -> Action.MOVE_Z_BACKWARD
        KeyEvent.KEYCODE_BUTTON_R1 -> Action.MOVE_Z_FORWARD

        // Menu buttons
        KeyEvent.KEYCODE_BUTTON_START -> Action.PAUSE
        KeyEvent.KEYCODE_BUTTON_SELECT -> Action.SETTINGS
        KeyEvent.KEYCODE_MENU -> Action.PAUSE

        // Stick click
        KeyEvent.KEYCODE_BUTTON_THUMBL -> Action.TOGGLE_GRAVITY

        else -> null
    }

    companion object {
        /** Get list of connected game controllers. */
        fun getConnectedControllers(): List<ControllerInfo> {
            val result = mutableListOf<ControllerInfo>()
            val deviceIds = InputDevice.getDeviceIds()
            for (id in deviceIds) {
                val device = InputDevice.getDevice(id) ?: continue
                val sources = device.sources
                val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                val isJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                if (isGamepad || isJoystick) {
                    result.add(ControllerInfo(
                        name = device.name,
                        vendorId = device.vendorId,
                        productId = device.productId,
                        isConnected = true
                    ))
                }
            }
            return result
        }
    }
}
