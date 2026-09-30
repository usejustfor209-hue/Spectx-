package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiApiClient
import com.example.ui.viewmodel.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentApiKey = viewModel.geminiClient.getApiKey()
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var showApiKey by remember { mutableStateOf(false) }

    var selectedModel by remember { mutableStateOf(viewModel.geminiClient.getSelectedModel()) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var speechPitch by remember { mutableFloatStateOf(1.0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("Settings & Intelligence", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Key Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Gemini API Key", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = if (currentApiKey.isNotBlank()) "Configured & Ready" else "Key required for cloud AI",
                                fontSize = 11.sp,
                                color = if (currentApiKey.isNotBlank()) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Enter Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input_field"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { showApiKey = !showApiKey }) {
                            Text(if (showApiKey) "Hide" else "Show", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.geminiClient.setCustomApiKey(apiKeyInput)
                                Toast.makeText(context, "API Key saved successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("save_api_key_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Key")
                        }
                    }
                }
            }

            // Model Selector Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Active AI Model", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Select cutting-edge Gemini model", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedModel == GeminiApiClient.MODEL_FLASH,
                            onClick = {
                                selectedModel = GeminiApiClient.MODEL_FLASH
                                viewModel.geminiClient.setSelectedModel(GeminiApiClient.MODEL_FLASH)
                            },
                            label = { Text("Gemini 3.5 Flash") },
                            leadingIcon = if (selectedModel == GeminiApiClient.MODEL_FLASH) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )

                        FilterChip(
                            selected = selectedModel == GeminiApiClient.MODEL_PRO,
                            onClick = {
                                selectedModel = GeminiApiClient.MODEL_PRO
                                viewModel.geminiClient.setSelectedModel(GeminiApiClient.MODEL_PRO)
                            },
                            label = { Text("Gemini 3.1 Pro") },
                            leadingIcon = if (selectedModel == GeminiApiClient.MODEL_PRO) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedModel == GeminiApiClient.MODEL_FLASH) {
                            "⚡ Gemini 3.5 Flash: Ultra-fast responses, ideal for real-time voice, task scheduling, and daily assistance."
                        } else {
                            "🧠 Gemini 3.1 Pro: Deep reasoning, ideal for complex coding, STEM questions, and deep multi-step analysis."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // Voice Text-To-Speech Settings Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Voice Output Engine", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Speech speed and pitch adjustments", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Speech Speed: ${"%.1f".format(speechRate)}x", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            viewModel.voiceManager.setSpeechRate(it)
                        },
                        valueRange = 0.75f..1.5f,
                        steps = 3
                    )

                    Text("Pitch Tone: ${"%.1f".format(speechPitch)}x", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Slider(
                        value = speechPitch,
                        onValueChange = {
                            speechPitch = it
                            viewModel.voiceManager.setPitch(it)
                        },
                        valueRange = 0.75f..1.5f,
                        steps = 3
                    )

                    Button(
                        onClick = {
                            viewModel.speakText("Namaste! This is your Omni AI voice assistant test.")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Voice Output")
                    }
                }
            }

            // App info card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Omni AI Assistant v1.0", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Powered by Google AI Gemini models with automated task alarms, Room offline storage, and real-time speech interaction.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
