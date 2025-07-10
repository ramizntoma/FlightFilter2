package com.example.flightfilter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val tcpServerIp = "127.0.0.1"
    private val tcpServerPort = 10113
    private val udpTargetIp = "127.0.0.1"
    private val udpTargetPort = 10112

    private var isForwardingRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var status by remember { mutableStateOf("Disconnected") }
            var receivedMessages by remember { mutableStateOf("") }

            var altitudeLimitText by remember { mutableStateOf("500") }
            val altitudeLimit = altitudeLimitText.toIntOrNull() ?: 500

            var step1Completed by remember { mutableStateOf(false) }
            var step2Completed by remember { mutableStateOf(false) }

            // Feature selections
            var flightFilterEnabled by remember { mutableStateOf(true) }
            var proximityAlertEnabled by remember { mutableStateOf(false) }

            // UI content
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("TCP Client with FLARM Altitude Filter", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!step1Completed) {
                        Text(
                            "Step 1: Start XC Guide and set FLARM port to 10113 and WAIT until airplanes are shown on map before returning to this app",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(
                            onClick = { launchXCGuide(); step1Completed = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start XC Guide")
                        }
                        Image(
                            painter = painterResource(id = R.drawable.sc),
                            contentDescription = "XC Guide Screenshot",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (step1Completed && !step2Completed) {
                        Text(
                            "Step 2: Start XC Track and set UDP port to 10112",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(
                            onClick = { launchXCTrack(); step2Completed = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start XC Track")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (step1Completed && step2Completed) {
                        // Step 3: Select Features
                        Text("Step 3: Select Features", style = MaterialTheme.typography.titleMedium)
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = flightFilterEnabled,
                                onCheckedChange = { flightFilterEnabled = it }
                            )
                            Text("Flight Filter")
                        }
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = proximityAlertEnabled,
                                onCheckedChange = { proximityAlertEnabled = it }
                            )
                            Text("Proximity Alert")
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Step 4: Filter + Connect
                        Text("Step 4: Filter + Connect", style = MaterialTheme.typography.titleMedium)
                        Text("Status: ${if (isForwardingRunning) "Connected" else "Disconnected"}")
                        Text("XC Guide Port (TCP): $tcpServerPort")
                        Text("XC Track Port (UDP): $udpTargetPort")
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = altitudeLimitText,
                            onValueChange = { newVal -> altitudeLimitText = newVal.filter { it.isDigit() } },
                            label = { Text("Altitude limit (± meters)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isForwardingRunning
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp, max = 400.dp)
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(text = receivedMessages)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            Button(
                                onClick = {
                                    startForwardingService(
                                        altitudeLimitText.toIntOrNull() ?: 500,
                                        flightFilterEnabled
                                    )
                                    isForwardingRunning = true
                                    status = "Connected"
                                },
                                enabled = !isForwardingRunning,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Connect")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = {
                                    stopForwardingService()
                                    isForwardingRunning = false
                                    status = "Disconnected"
                                    receivedMessages = ""
                                },
                                enabled = isForwardingRunning,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchXCGuide() {
        try {
            val intent = Intent().apply {
                setClassName("indysoft.xc_guide", "indysoft.xc_guide.XCGuideActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=indysoft.xc_guide")
            }
            startActivity(playIntent)
        }
    }

    private fun launchXCTrack() {
        try {
            val intent = Intent().apply {
                setClassName("org.xcontest.XCTrack", "org.xcontest.XCTrack.startup.StartupActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=org.xcontest.XCTrack")
            }
            startActivity(playIntent)
        }
    }

    private fun startForwardingService(altitudeLimit: Int, flightFilterEnabled: Boolean) {
        val intent = Intent(this, FlarmForwardService::class.java).apply {
            putExtra("tcpServerIp", tcpServerIp)
            putExtra("tcpServerPort", tcpServerPort)
            putExtra("udpTargetIp", udpTargetIp)
            putExtra("udpTargetPort", udpTargetPort)
            putExtra("altitudeLimit", altitudeLimit)
            putExtra("flightFilterEnabled", flightFilterEnabled)
        }
        startForegroundService(intent)
    }

    private fun stopForwardingService() {
        val intent = Intent(this, FlarmForwardService::class.java)
        stopService(intent)
    }
}
