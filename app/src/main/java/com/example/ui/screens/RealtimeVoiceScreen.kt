package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimatedVoiceOrb
import com.example.ui.viewmodel.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeVoiceScreen(
    viewModel: AssistantViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.setDuplexVoiceMode(false)
        onBack()
    }

    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val rmsDb by viewModel.voiceManager.rmsDb.collectAsState()
    val recognizedText by viewModel.voiceManager.recognizedText.collectAsState()
    val isDuplexActive by viewModel.isDuplexVoiceModeActive.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()

    val lastAiResponse = messages.lastOrNull { it.role == "model" }?.content ?: ""

    var speechRate by remember { mutableFloatStateOf(1.0f) }

    // Start voice session when entering screen
    DisposableEffect(Unit) {
        viewModel.setDuplexVoiceMode(true)
        onDispose {
            viewModel.setDuplexVoiceMode(false)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.voiceManager.startListening()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Live Voice Companion",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.setDuplexVoiceMode(false)
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    if (isSpeaking) viewModel.voiceManager.stopSpeaking()
                }) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Audio mute",
                        tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Tag
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                    isListening -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    isSpeaking -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    isLoading -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isListening -> Color(0xFFEF4444)
                                    isSpeaking -> MaterialTheme.colorScheme.primary
                                    isLoading -> MaterialTheme.colorScheme.secondary
                                    else -> Color.Gray
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isListening -> "Listening to your voice..."
                            isSpeaking -> "Omni AI is speaking..."
                            isLoading -> "Thinking with Gemini 3.5..."
                            else -> "Tap the Orb to Speak"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isListening -> Color(0xFFEF4444)
                            isSpeaking -> MaterialTheme.colorScheme.primary
                            isLoading -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Interactive Voice Orb
            AnimatedVoiceOrb(
                isListening = isListening,
                isSpeaking = isSpeaking,
                rmsDb = rmsDb,
                orbSize = 150.dp,
                onClick = {
                    if (isListening) {
                        viewModel.voiceManager.stopListening()
                    } else {
                        audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.testTag("interactive_voice_orb")
            )

            Text(
                text = if (isListening) "Tap to send speech" else "Tap to Speak (Hindi / English)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Realtime user transcription card
            AnimatedVisibility(visible = recognizedText.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "You said:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = recognizedText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Latest AI spoken reply card
            if (lastAiResponse.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Omni AI:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { viewModel.speakText(lastAiResponse) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Replay voice",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = lastAiResponse,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hands-free conversation toggle card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Hands-Free Auto Duplex",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Listens automatically after AI finishes speaking",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDuplexActive,
                            onCheckedChange = { active -> viewModel.setDuplexVoiceMode(active) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Voice Speed: ${"%.1f".format(speechRate)}x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            viewModel.voiceManager.setSpeechRate(it)
                        },
                        valueRange = 0.75f..1.5f,
                        steps = 3
                    )
                }
            }
        }
    }
}
