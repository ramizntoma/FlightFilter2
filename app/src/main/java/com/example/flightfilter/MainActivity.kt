package com.example.flightfilter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var status by remember { mutableStateOf("Disconnected") }
            var isConnected by remember { mutableStateOf(false) }

            var altitudeLimitText by remember { mutableStateOf("500") }
            val altitudeLimit = altitudeLimitText.toIntOrNull() ?: 500

            var step1Completed by remember { mutableStateOf(false) }
            var step2Completed by remember { mutableStateOf(false) }

            var flightFilterEnabled by remember { mutableStateOf(true) }
            var proximityAlertEnabled by remember { mutableStateOf(false) }

            val context = this@MainActivity

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("TCP Client with FLARM Altitude Filter", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!step1Completed) {
                        Text("Step 1: Start XC Guide")
                        Button(onClick = {
                            try {
                                val intent = Intent().apply {
                                    setClassName("indysoft.xc_guide", "indysoft.xc_guide.XCGuideActivity")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                step1Completed = true
                            } catch (e: Exception) {
                                val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("market://details?id=indysoft.xc_guide")
                                }
                                context.startActivity(playIntent)
                            }
                        }) {
                            Text("Start XC Guide")
                        }
                    }

                    if (step1Completed && !step2Completed) {
                        Text("Step 2: Start XCTrack")
                        Button(onClick = {
                            try {
                                val intent = Intent().apply {
                                    setClassName("org.xcontest.XCTrack", "org.xcontest.XCTrack.startup.StartupActivity")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                step2Completed = true
                            } catch (e: Exception) {
                                val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("market://details?id=org.xcontest.XCTrack")
                                }
                                context.startActivity(playIntent)
                            }
                        }) {
                            Text("Start XCTrack")
                        }
                    }

                    if (step1Completed && step2Completed) {
                        Text("Step 3: Select Features")
                        Row {
                            Checkbox(checked = flightFilterEnabled, onCheckedChange = { flightFilterEnabled = it })
                            Text("Flight Filter")
                        }
                        Row {
                            Checkbox(checked = proximityAlertEnabled, onCheckedChange = { proximityAlertEnabled = it })
                            Text("Proximity Alert")
                        }

                        Text("Step 4: Filter + Connect")
                        Text("Status: $status")

                        OutlinedTextField(
                            value = altitudeLimitText,
                            onValueChange = { altitudeLimitText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Altitude limit (± meters)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isConnected
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    startForwardingService(
                                        context,
                                        altitudeLimit,
                                        flightFilterEnabled,
                                        proximityAlertEnabled
                                    )
                                    status = "Started service"
                                    isConnected = true
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isConnected
                            ) {
                                Text("Connect")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                onClick = {
                                    stopForwardingService(context)
                                    status = "Disconnected"
                                    isConnected = false
                                },
                                modifier = Modifier.weight(1f),
                                enabled = isConnected
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startForwardingService(
        context: Context,
        altitudeLimit: Int,
        flightFilter: Boolean,
        proximityAlert: Boolean
    ) {
        val intent = Intent(context, FlarmForwardService::class.java).apply {
            putExtra("altitudeLimit", altitudeLimit)
            putExtra("flightFilter", flightFilter)
            putExtra("proximityAlert", proximityAlert)
        }
        context.startForegroundService(intent)
    }

    private fun stopForwardingService(context: Context) {
        context.stopService(Intent(context, FlarmForwardService::class.java))
    }
}
