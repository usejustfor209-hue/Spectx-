package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MessageBubble
import com.example.ui.viewmodel.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    onNavigateToRealtimeVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val autoSpeak by viewModel.autoSpeak.collectAsState()
    val selectedImage by viewModel.selectedImageBitmap.collectAsState()
    val recognizedVoiceText by viewModel.voiceManager.recognizedText.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Sync recognized speech into input text or auto-send
    LaunchedEffect(recognizedVoiceText) {
        if (recognizedVoiceText.isNotBlank()) {
            inputText = recognizedVoiceText
        }
    }

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo picker using Android Photo Picker API (Google Play safe)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                viewModel.setSelectedImage(bitmap)
            } catch (_: Exception) {}
        }
    }

    // Record audio permission launcher
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
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isListening || isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Omni AI Assistant",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSpeaking) "Speaking..." else if (isListening) "Listening..." else "Gemini 3.5 Flash • Active",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            actions = {
                // Realtime voice mode shortcut
                IconButton(
                    onClick = onNavigateToRealtimeVoice,
                    modifier = Modifier.testTag("action_realtime_voice")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Real-time Voice Mode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Auto-speak toggle
                IconButton(
                    onClick = { viewModel.setAutoSpeak(!autoSpeak) },
                    modifier = Modifier.testTag("action_toggle_speak")
                ) {
                    Icon(
                        imageVector = if (autoSpeak) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle auto-speak",
                        tint = if (autoSpeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Clear history
                IconButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier.testTag("action_clear_chat")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Suggestion prompt chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChipItem(label = "⏰ Remind in 15m") {
                viewModel.sendUserMessage("Remind me in 15 minutes to drink a glass of water")
            }
            SuggestionChipItem(label = "📅 Auto-Plan My Day") {
                viewModel.autoPlanDay("Complete priority work tasks, 45 min workout, read book, prep dinner")
            }
            SuggestionChipItem(label = "💻 Kotlin Coroutines") {
                viewModel.sendUserMessage("Explain Kotlin Coroutines StateFlow vs SharedFlow with clean examples")
            }
            SuggestionChipItem(label = "🌐 Translate to Hindi") {
                viewModel.sendUserMessage("Translate this to natural conversational Hindi: 'Let's meet tomorrow to review our progress'")
            }
            SuggestionChipItem(label = "⚡ Quick Math") {
                viewModel.sendUserMessage("Solve this calculation: (450 * 18) / 12 + 85")
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    onSpeak = { text -> viewModel.speakText(text) }
                )
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Omni AI is processing...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Selected Image Preview
        AnimatedVisibility(visible = selectedImage != null) {
            selectedImage?.let { bmp ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Attached photo",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Photo attached for Vision analysis",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { viewModel.setSelectedImage(null) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove photo",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Voice Listening Indicator Bar
        AnimatedVisibility(visible = isListening) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Listening",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (recognizedVoiceText.isNotBlank()) recognizedVoiceText else "Listening to your voice...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.voiceManager.stopListening() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop listening",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Bottom Input Row
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach image button
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("attach_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Text field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask anything or schedule a task...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("message_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Voice mic button
                IconButton(
                    onClick = {
                        if (isListening) {
                            viewModel.voiceManager.stopListening()
                        } else {
                            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("voice_record_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice input",
                        tint = if (isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Send button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() || selectedImage != null) {
                            viewModel.sendUserMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = (inputText.isNotBlank() || selectedImage != null) && !isLoading,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() || selectedImage != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (inputText.isNotBlank() || selectedImage != null) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionChipItem(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
