package com.example.smooth60

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Smooth60Screen() }
    }
}

@Composable
private fun Smooth60Screen() {
    var selectedVideo by remember { mutableStateOf<Uri?>(null) }
    var status by remember {
        mutableStateOf("Choose a video. This prototype includes the RIFE frame-inference core; full video decode/encode is the next integration step.")
    }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedVideo = uri
        if (uri != null) status = "Video selected. URI permission is granted for this session."
    }

    MaterialTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Smooth60", style = MaterialTheme.typography.headlineLarge)
                Text("AI frame interpolation • 30 → 60 fps")
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Target settings", style = MaterialTheme.typography.titleMedium)
                        Text("Output: 3840 × 2160 (4K), 60 fps")
                        Text("Interpolation: RIFE ONNX")
                        Text("Mode: balanced • on-device")
                    }
                }
                Button(
                    onClick = { picker.launch(arrayOf("video/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Select source video") }

                Text(selectedVideo?.lastPathSegment ?: "No video selected")
                OutlinedButton(
                    onClick = { status = "The video processing pipeline is not yet connected in this starter build. See README for the exact next steps." },
                    enabled = selectedVideo != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Prepare 4K 60fps export") }

                HorizontalDivider()
                Text("Status", style = MaterialTheme.typography.titleMedium)
                Text(status)
                Text(
                    "Important: this is a developer starter, not a finished converter APK. It does not yet export a processed video.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
