package org.lanenode

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Screen() } }
    }

    @Composable
    fun Screen() {
        val ctx = this
        var cfg by remember { mutableStateOf(Config.load(ctx)) }
        var tick by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) { while (true) { delay(1000); tick++ } }

        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Lane Node", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "LAN listener on Wi-Fi, egress bound to the cellular modem.",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = cfg.listenPort.toString(),
                    onValueChange = { cfg = cfg.copy(listenPort = it.toIntOrNull() ?: 8899) },
                    label = { Text("Listen port (SOCKS5 + HTTP CONNECT)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(
                        checked = cfg.useUpstream,
                        onCheckedChange = { cfg = cfg.copy(useUpstream = it) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Chain to upstream proxy (keep this ON)")
                }

                OutlinedTextField(
                    value = cfg.upstreamHost,
                    onValueChange = { cfg = cfg.copy(upstreamHost = it.trim()) },
                    label = { Text("Upstream proxy host") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cfg.upstreamPort.toString(),
                    onValueChange = { cfg = cfg.copy(upstreamPort = it.toIntOrNull() ?: 0) },
                    label = { Text("Upstream port") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cfg.upstreamUser,
                    onValueChange = { cfg = cfg.copy(upstreamUser = it.trim()) },
                    label = { Text("Upstream username") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cfg.upstreamPass,
                    onValueChange = { cfg = cfg.copy(upstreamPass = it.trim()) },
                    label = { Text("Upstream password") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cfg.maxActive.toString(),
                    onValueChange = {
                        cfg = cfg.copy(maxActive = it.toIntOrNull()?.coerceIn(1, 512) ?: 48)
                    },
                    label = { Text("Max active flows (anti-stampede)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cfg.uploadKbps.toString(),
                    onValueChange = {
                        cfg = cfg.copy(uploadKbps = it.toIntOrNull()?.coerceAtLeast(0) ?: 1400)
                    },
                    label = { Text("Upload cap kbps (0=off; ~75% of cell up)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Hotspot tip: point every device at this phone’s SOCKS — do NOT let them NAT raw through the hotspot. Carrier sees one phone; we pace the uplink so bufferbloat dies.",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        Config.save(ctx, cfg)
                        startForegroundService(Intent(ctx, LaneService::class.java))
                    }) { Text("Start") }

                    OutlinedButton(onClick = {
                        stopService(Intent(ctx, LaneService::class.java))
                    }) { Text("Stop") }
                }

                HorizontalDivider()

                val s = LaneService.server
                val b = LaneService.binder
                val status = buildString {
                    appendLine("tick            $tick")
                    appendLine("service         ${if (LaneService.running) "RUNNING" else "stopped"}")
                    appendLine("cellular        ${if (b?.isUp == true) "UP" else "down"}")
                    appendLine("lan address     ${LaneService.lanAddress()}")
                    appendLine("listen          ${cfg.listenPort}")
                    appendLine("active conns    ${s?.active?.get() ?: 0} / ${cfg.maxActive}")
                    appendLine("rejected        ${s?.rejected?.get() ?: 0}")
                    appendLine("upload cap      ${if (cfg.uploadKbps > 0) "${cfg.uploadKbps} kbps" else "off"}")
                    appendLine("uploaded        ${fmt(s?.up?.get() ?: 0)}")
                    appendLine("downloaded      ${fmt(s?.down?.get() ?: 0)}")
                    appendLine("errors          ${s?.errors?.get() ?: 0}")
                    LaneService.lastError?.let { appendLine("last error      $it") }
                }
                Text(status, fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall)

                HorizontalDivider()
                Text(
                    "Clients → socks5://${LaneService.lanAddress()}:${cfg.listenPort}\n" +
                    "Phone apps can use MetaClash too; LAN devices should use this SOCKS, not bare hotspot NAT.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    private fun fmt(n: Long): String = when {
        n > 1_000_000_000 -> "%.2f GB".format(n / 1e9)
        n > 1_000_000 -> "%.1f MB".format(n / 1e6)
        n > 1_000 -> "%.0f KB".format(n / 1e3)
        else -> "$n B"
    }
}
