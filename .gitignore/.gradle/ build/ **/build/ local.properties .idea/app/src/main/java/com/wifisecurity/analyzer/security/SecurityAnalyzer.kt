package com.wifisecurity.analyzer.security

import com.wifisecurity.analyzer.network.WifiNetwork

data class AuditFinding(val title: String, val detail: String, val severity: Severity)
enum class Severity { GOOD, INFO, WARNING, CRITICAL }
data class AuditResult(val score: Int, val label: String, val findings: List<AuditFinding>)

object SecurityAnalyzer {
    fun analyze(network: WifiNetwork): AuditResult {
        var score = 100
        val findings = mutableListOf<AuditFinding>()
        when {
            network.security.startsWith("Abierta") -> {
                score -= 75
                findings += AuditFinding("Red abierta", "No se detectó protección de autenticación en la información anunciada por la red.", Severity.CRITICAL)
            }
            network.security == "WEP" -> {
                score -= 65
                findings += AuditFinding("Cifrado obsoleto", "WEP se considera inseguro. Configura WPA2-AES o WPA3 en tu router.", Severity.CRITICAL)
            }
            network.security == "WPA2/WPA" -> {
                score -= 15
                findings += AuditFinding("WPA/WPA2 detectado", "La información del anuncio no permite confirmar todos los parámetros. Prefiere WPA3 o WPA2-AES.", Severity.WARNING)
            }
            network.security == "WPA3" -> {
                findings += AuditFinding("WPA3 anunciado", "La red anuncia WPA3. Esto es una buena señal, pero no certifica toda la configuración.", Severity.GOOD)
            }
            else -> {
                score -= 10
                findings += AuditFinding("Seguridad no concluyente", "Android no entregó información suficiente para clasificar con certeza el cifrado.", Severity.INFO)
            }
        }
        findings += AuditFinding("WPS no verificado", "El escaneo pasivo de Android no permite confirmar si WPS está habilitado o protegido.", Severity.INFO)
        findings += AuditFinding("Firmware no verificado", "Se necesita el modelo y la versión del router para revisar el firmware.", Severity.INFO)

        val safeScore = score.coerceIn(0, 100)
        val label = when (safeScore) {
            in 0..30 -> "Riesgo crítico"
            in 31..50 -> "Riesgo alto"
            in 51..70 -> "Riesgo medio"
            in 71..90 -> "Buena seguridad"
            else -> "Seguridad excelente"
        }
        return AuditResult(safeScore, label, findings)
    }
}
