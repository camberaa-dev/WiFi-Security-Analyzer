package com.wifisecurity.analyzer.network

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val security: String,
    val frequencyMhz: Int,
    val levelDbm: Int,
    val capabilities: String
) {
    val band: String
        get() = when {
            frequencyMhz in 2400..2500 -> "2.4 GHz"
            frequencyMhz in 4900..5900 -> "5 GHz"
            frequencyMhz >= 5925 -> "6 GHz"
            else -> "Desconocida"
        }

    val signalPercent: Int get() = ((levelDbm + 100) * 2).coerceIn(0, 100)
}
