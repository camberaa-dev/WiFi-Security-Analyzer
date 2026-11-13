package com.wifisecurity.analyzer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wifisecurity.analyzer.network.WifiNetwork
import com.wifisecurity.analyzer.network.WifiScanner
import com.wifisecurity.analyzer.security.SecurityAnalyzer
import com.wifisecurity.analyzer.security.Severity
import kotlinx.coroutines.launch

private enum class Tab(val title: String) {
    HOME("Inicio"), SCAN("Escáner"), AUDIT("Auditoría"), TOOLS("Herramientas")
}

@Composable
fun WifiAnalyzerApp() {
    val context = LocalContext.current
    val scanner = remember { WifiScanner(context) }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(Tab.HOME) }
    var networks by remember { mutableStateOf<List<WifiNetwork>>(emptyList()) }
    var selectedNetwork by remember { mutableStateOf<WifiNetwork?>(null) }
    var status by remember { mutableStateOf("Listo para analizar") }
    var scanning by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            scope.launch {
                scanning = true
                status = "Buscando redes..."
                val result = scanner.scan()
                result.onSuccess { networks = it; status = "${it.size} redes detectadas" }
                    .onFailure { status = it.message ?: "No se pudo escanear" }
                scanning = false
            }
        } else status = "Permiso de ubicación denegado. Android lo requiere para mostrar resultados WiFi."
    }

    fun startScan() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        } else {
            scope.launch {
                scanning = true
                status = "Buscando redes..."
                scanner.scan().onSuccess { networks = it; status = "${it.size} redes detectadas" }
                    .onFailure { status = it.message ?: "Error de escaneo" }
                scanning = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0B1828)) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                when (tab) {
                                    Tab.HOME -> Icons.Default.Dashboard
                                    Tab.SCAN -> Icons.Default.Wifi
                                    Tab.AUDIT -> Icons.Default.Security
                                    Tab.TOOLS -> Icons.Default.Tune
                                }, contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("WIFI SECURITY", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("ANALYZER  •  LOCAL AUDIT", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
            }
            when (selectedTab) {
                Tab.HOME -> Dashboard(status, networks.size, selectedNetwork, onScan = { startScan() }, scanning)
                Tab.SCAN -> ScannerPage(networks, status, scanning, onScan = { startScan() }, onSelect = {
                    selectedNetwork = it
                    selectedTab = Tab.AUDIT
                })
                Tab.AUDIT -> AuditPage(selectedNetwork)
                Tab.TOOLS -> ToolsPage()
            }
        }
    }
}

@Composable
private fun Dashboard(status: String, count: Int, selected: WifiNetwork?, onScan: () -> Unit, scanning: Boolean) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Centro de seguridad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Visibilidad y diagnóstico de tu red inalámbrica.", color = Color(0xFF9CB2C9))
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF102B43)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Radar, null, tint = Color(0xFF22D3EE), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("NETWORK MONITOR", fontWeight = FontWeight.Bold)
                    }
                    Text(status, color = Color(0xFFB8D8EA))
                    Button(onClick = onScan, enabled = !scanning, modifier = Modifier.fillMaxWidth()) {
                        if (scanning) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Search, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (scanning) "Escaneando..." else "Escanear redes WiFi")
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("REDES", count.toString(), Icons.Default.Wifi, Modifier.weight(1f))
                MetricCard("SELECCIÓN", if (selected == null) "—" else "1", Icons.Default.CheckCircle, Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("Red seleccionada")
            if (selected == null) InfoCard("Ninguna red seleccionada", "Abre el escáner y selecciona una red para consultar su evaluación.")
            else InfoCard(selected.ssid, "${selected.security}  •  ${selected.band}  •  ${selected.levelDbm} dBm")
        }
        item { Text("Nota: los resultados dependen de los permisos y límites de escaneo de Android.", color = Color(0xFF8299AF), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ScannerPage(networks: List<WifiNetwork>, status: String, scanning: Boolean, onScan: () -> Unit, onSelect: (WifiNetwork) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Escáner WiFi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(status, color = Color(0xFF9CB2C9), modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        Button(onClick = onScan, enabled = !scanning, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Buscar redes")
        }
        Spacer(Modifier.height(12.dp))
        if (networks.isEmpty()) {
            InfoCard("No hay resultados todavía", "Activa WiFi, concede el permiso solicitado y pulsa Buscar redes.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(networks) { network ->
                    Card(onClick = { onSelect(network) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF102033))) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(network.ssid, fontWeight = FontWeight.SemiBold)
                                    Text(network.security, color = Color(0xFF9CB2C9), style = MaterialTheme.typography.bodySmall)
                                }
                                Text("${network.levelDbm} dBm", color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { network.signalPercent / 100f }, modifier = Modifier.fillMaxWidth())
                            Text("${network.band}  •  ${network.frequencyMhz} MHz", color = Color(0xFF8299AF), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditPage(network: WifiNetwork?) {
    if (network == null) {
        InfoCard("Selecciona una red", "Ve al Escáner WiFi, busca redes y selecciona una para generar su evaluación.")
        return
    }
    val result = remember(network) { SecurityAnalyzer.analyze(network) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Auditoría de seguridad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(network.ssid, color = Color(0xFF9CB2C9))
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF102B43))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PUNTUACIÓN ESTIMADA", color = Color(0xFF9CB2C9), style = MaterialTheme.typography.labelMedium)
                    Text("${result.score}/100", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(result.label, fontWeight = FontWeight.SemiBold)
                    Text("Evaluación pasiva basada en datos anunciados por la red.", color = Color(0xFF9CB2C9), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { SectionTitle("Hallazgos") }
        items(result.findings) { finding ->
            val color = when (finding.severity) {
                Severity.GOOD -> Color(0xFF34D399)
                Severity.INFO -> Color(0xFF60A5FA)
                Severity.WARNING -> Color(0xFFFBBF24)
                Severity.CRITICAL -> Color(0xFFFB7185)
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF102033))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.ReportProblem, null, tint = color)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(finding.title, fontWeight = FontWeight.SemiBold)
                        Text(finding.detail, color = Color(0xFF9CB2C9), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsPage() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Herramientas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        InfoCard("Diagnóstico de conectividad", "La siguiente etapa añadirá pruebas reales de latencia, conectividad y velocidad.")
        InfoCard("Analizador de canales", "Se implementará con las frecuencias y canales disponibles en los resultados del escáner.")
        InfoCard("WiFi Password Security Lab", "Evaluación local de contraseñas propias y recomendaciones. No ejecuta ataques contra redes.")
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF102033))) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(title, color = Color(0xFF9CB2C9), style = MaterialTheme.typography.labelSmall)
        }
    }
}
@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
@Composable private fun InfoCard(title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF102033)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Color(0xFF9CB2C9), style = MaterialTheme.typography.bodySmall)
        }
    }
}
