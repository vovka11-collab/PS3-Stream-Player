package com.vovka11.ps3streamplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vovka11.ps3streamplayer.service.StreamingService
import com.vovka11.ps3streamplayer.service.StreamingService.Companion.PORT

class MainActivity : ComponentActivity() {
    private var streamingService: StreamingService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StreamingService.StreamingBinder
            streamingService = binder.getService()
            isServiceBound = true
            Log.d("MainActivity", "Service bound successfully")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            streamingService = null
            Log.d("MainActivity", "Service disconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Intent(this, StreamingService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            startService(intent)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        onStartStreaming = { startStreaming() },
                        onStopStreaming = { stopStreaming() }
                    )
                }
            }
        }
    }

    private fun startStreaming() {
        Intent(this, StreamingService::class.java).apply {
            action = "START_STREAMING"
            startService(this)
        }
        Log.d("MainActivity", "Streaming started on http://localhost:$PORT")
    }

    private fun stopStreaming() {
        Intent(this, StreamingService::class.java).apply {
            action = "STOP_STREAMING"
            startService(this)
        }
        Log.d("MainActivity", "Streaming stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}

@Composable
fun MainScreen(
    onStartStreaming: () -> Unit,
    onStopStreaming: () -> Unit
) {
    var serverRunning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        Text(
            "PS3 Stream Player",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
            color = Color.White,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Text(
            "DLNA Media Server for PS3",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB0B0B0),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF16213E)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Server Status:", color = Color(0xFFB0B0B0))
                    Text(
                        if (serverRunning) "ONLINE" else "OFFLINE",
                        color = if (serverRunning) Color(0xFF00FF00) else Color(0xFFFF0000),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Port:", color = Color(0xFFB0B0B0))
                    Text(PORT.toString(), color = Color.White)
                }
            }
        }

        // Start/Stop Button
        Button(
            onClick = {
                serverRunning = !serverRunning
                if (serverRunning) {
                    onStartStreaming()
                } else {
                    onStopStreaming()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (serverRunning) Color(0xFFE74C3C) else Color(0xFF27AE60),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = if (serverRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = "Toggle Server",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                if (serverRunning) "STOP SERVER" else "START SERVER",
                fontSize = 16.sp
            )
        }

        // Info Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF16213E)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "How it works:",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                val features = listOf(
                    "✓ Acts as DLNA/UPnP Media Server",
                    "✓ Converts M8N3 streams to MP4",
                    "✓ Streams over local network",
                    "✓ Compatible with PS3 media player",
                    "✓ Automatic format detection"
                )
                
                features.forEach { feature ->
                    Text(
                        feature,
                        color = Color(0xFFB0B0B0),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F3460)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "Setup:",
                    color = Color(0xFFE74C3C),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                val steps = listOf(
                    "1. Start server with button above",
                    "2. PS3 on same network",
                    "3. PS3: Media Server → Settings",
                    "4. Scan for Media Servers",
                    "5. Select 'PS3 Stream Player'",
                    "6. Browse and play videos"
                )
                
                steps.forEach { step ->
                    Text(
                        step,
                        color = Color(0xFFB0B0B0),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }

        // App Version
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "v1.0.0",
            color = Color(0xFF666666),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
