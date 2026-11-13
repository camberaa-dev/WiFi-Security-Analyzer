package com.wifisecurity.analyzer.network

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WifiScanner(private val context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun hasScanPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    suspend fun scan(): Result<List<WifiNetwork>> {
        if (!hasScanPermission()) return Result.failure(SecurityException("Se necesita permiso de ubicación precisa para consultar resultados de escaneo WiFi."))
        if (!wifiManager.isWifiEnabled) return Result.failure(IllegalStateException("Activa el WiFi para buscar redes."))

        return try {
            val results = suspendCancellableCoroutine { continuation ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) { }
                        if (!continuation.isActive) return
                        val networks = wifiManager.scanResults.orEmpty()
                            .filter { it.SSID.isNotBlank() }
                            .map { result ->
                                val caps = result.capabilities.orEmpty()
                                WifiNetwork(
                                    ssid = result.SSID,
                                    bssid = result.BSSID.orEmpty(),
                                    security = classifySecurity(caps),
                                    frequencyMhz = result.frequency,
                                    levelDbm = result.level,
                                    capabilities = caps
                                )
                            }
                            .distinctBy { it.bssid }
                            .sortedByDescending { it.levelDbm }
                        continuation.resume(networks)
                    }
                }

                val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                if (Build.VERSION.SDK_INT >= 33) {
                    context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    @Suppress("DEPRECATION")
                    context.registerReceiver(receiver, filter)
                }
                val started = wifiManager.startScan()
                if (!started) {
                    try { context.unregisterReceiver(receiver) } catch (_: Exception) { }
                    if (continuation.isActive) continuation.resume(
                        wifiManager.scanResults.orEmpty().map { r ->
                            val caps = r.capabilities.orEmpty()
                            WifiNetwork(r.SSID.ifBlank { "Red oculta" }, r.BSSID.orEmpty(),
                                classifySecurity(caps), r.frequency, r.level, caps)
                        }.distinctBy { it.bssid }.sortedByDescending { it.levelDbm }
                    )
                }
                continuation.invokeOnCancellation {
                    try { context.unregisterReceiver(receiver) } catch (_: Exception) { }
                }
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun classifySecurity(capabilities: String): String = when {
        capabilities.contains("WPA3", true) || capabilities.contains("SAE", true) -> "WPA3"
        capabilities.contains("WPA2", true) || capabilities.contains("PSK", true) -> "WPA2/WPA"
        capabilities.contains("WEP", true) -> "WEP"
        capabilities.contains("EAP", true) -> "WPA-Enterprise"
        else -> "Abierta / no identificada"
    }
}
