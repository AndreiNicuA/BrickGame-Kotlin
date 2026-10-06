package com.brickgame.tetris.net

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Offline two-player link between phones in the same room (Google Nearby Connections: Bluetooth /
 * Wi-Fi, no internet). Both phones advertise and discover at the same time; the phone with the
 * smaller random id asks to connect, so exactly one connection is made. Messages are short text
 * lines (see [VersusMessage]).
 */
class VersusLink(context: Context) {

    enum class Phase { IDLE, SEARCHING, CONNECTING, CONNECTED, DISCONNECTED, FAILED }

    /** The other phone: its player name and the 4-digit code both screens show. */
    data class Peer(val endpointId: String, val name: String, val code: String)

    companion object {
        private const val TAG = "VersusLink"
        private const val SERVICE_ID = "com.andreinicua.brickgame.versus"
        private val STRATEGY = Strategy.P2P_POINT_TO_POINT

        /** Runtime permissions Nearby needs on this Android version. */
        fun requiredPermissions(): Array<String> = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.NEARBY_WIFI_DEVICES
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                // Android 12 only grants fine location when coarse is asked for in the same request
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            )
            else -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private val client = Nearby.getConnectionsClient(context.applicationContext)
    private val myId = UUID.randomUUID().toString().take(8)
    private var myName = "Player"

    private val _phase = MutableStateFlow(Phase.IDLE)
    val phase: StateFlow<Phase> = _phase.asStateFlow()
    private val _peer = MutableStateFlow<Peer?>(null)
    val peer: StateFlow<Peer?> = _peer.asStateFlow()

    /** Called on the main thread for every message from the other phone. */
    var onMessage: (VersusMessage) -> Unit = {}

    private val pendingNames = HashMap<String, String>()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            VersusMessage.parse(String(bytes, Charsets.UTF_8))?.let { onMessage(it) }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            _phase.value = Phase.CONNECTING
            pendingNames[endpointId] = displayName(info.endpointName)
            _peer.value = Peer(endpointId, displayName(info.endpointName), info.authenticationDigits)
            client.acceptConnection(endpointId, payloadCallback)
        }
        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) {
                client.stopAdvertising(); client.stopDiscovery()
                _phase.value = Phase.CONNECTED
                send(VersusMessage.Hello(myName))
            } else {
                _peer.value = null
                if (_phase.value == Phase.CONNECTING) _phase.value = Phase.SEARCHING
            }
        }
        override fun onDisconnected(endpointId: String) {
            _phase.value = Phase.DISCONNECTED
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // Only the phone with the smaller id asks, so the two don't request each other at once
            val theirId = info.endpointName.substringAfterLast('#', "")
            if (_phase.value == Phase.SEARCHING && myId < theirId) {
                client.requestConnection(endpointName(), endpointId, lifecycle)
                    .addOnFailureListener { Log.w(TAG, "requestConnection failed", it) }
            }
        }
        override fun onEndpointLost(endpointId: String) {}
    }

    private fun endpointName() = "${myName.take(16)}#$myId"
    private fun displayName(endpointName: String) = endpointName.substringBeforeLast('#')

    /** Look for a friend: advertise and discover until a connection is made. */
    fun start(name: String) {
        stop()
        myName = name.ifBlank { "Player" }
        _peer.value = null
        _phase.value = Phase.SEARCHING
        client.startAdvertising(endpointName(), SERVICE_ID, lifecycle, AdvertisingOptions.Builder().setStrategy(STRATEGY).build())
            .addOnFailureListener { Log.w(TAG, "advertising failed", it); _phase.value = Phase.FAILED }
        client.startDiscovery(SERVICE_ID, discovery, DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
            .addOnFailureListener { Log.w(TAG, "discovery failed", it); _phase.value = Phase.FAILED }
    }

    fun send(message: VersusMessage) {
        val peer = _peer.value ?: return
        if (_phase.value != Phase.CONNECTED) return
        client.sendPayload(peer.endpointId, Payload.fromBytes(message.encode().toByteArray(Charsets.UTF_8)))
    }

    /** Hang up and stop searching. */
    fun stop() {
        try {
            client.stopAdvertising(); client.stopDiscovery(); client.stopAllEndpoints()
        } catch (_: Exception) {}
        _peer.value = null
        _phase.value = Phase.IDLE
    }
}
