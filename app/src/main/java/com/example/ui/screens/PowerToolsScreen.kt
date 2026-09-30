package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.SavedNote
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PowerTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val promptTemplate: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerToolsScreen(
    viewModel: AssistantViewModel,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedNotes by viewModel.savedNotes.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeToolForDialog by remember { mutableStateOf<PowerTool?>(null) }
    var toolInputText by remember { mutableStateOf("") }

    val tools = listOf(
        PowerTool(
            id = "briefing",
            title = "Morning Briefing",
            description = "Smart executive summary of your day, energy, and priorities",
            icon = Icons.Default.WbSunny,
            color = Color(0xFFF59E0B),
            promptTemplate = "Generate an energizing, concise executive morning briefing for today with key productivity tips and focus guidelines."
        ),
        PowerTool(
            id = "code",
            title = "Code & Debugger",
            description = "Write clean code, debug syntax, explain algorithms",
            icon = Icons.Default.Code,
            color = Color(0xFF06B6D4),
            promptTemplate = "Write clean, robust code or debug this requirement:"
        ),
        PowerTool(
            id = "translate",
            title = "Language Translator",
            description = "Fluent English, Hindi, Hinglish, and international languages",
            icon = Icons.Default.Translate,
            color = Color(0xFF8B5CF6),
            promptTemplate = "Translate this text accurately into natural Hindi and English:"
        ),
        PowerTool(
            id = "email",
            title = "Email & Letter Drafter",
            description = "Professional emails, business proposals, and letters",
            icon = Icons.Default.Email,
            color = Color(0xFF10B981),
            promptTemplate = "Draft a polished and impactful professional email for:"
        ),
        PowerTool(
            id = "math",
            title = "Math & Science Solver",
            description = "Step-by-step calculus, statistics, equations, formulas",
            icon = Icons.Default.Calculate,
            color = Color(0xFFEC4899),
            promptTemplate = "Solve this step-by-step with clear explanations:"
        ),
        PowerTool(
            id = "brainstorm",
            title = "Creative Brainstormer",
            description = "Startup ideas, YouTube scripts, campaign strategies",
            icon = Icons.Default.Psychology,
            color = Color(0xFF3B82F6),
            promptTemplate = "Brainstorm 5 innovative, high-impact ideas for:"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text("Omni Power Suite", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Executive Tools", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("AI Notes Vault (${savedNotes.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        if (selectedTab == 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tools) { tool ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeToolForDialog = tool
                                toolInputText = ""
                            }
                            .testTag("tool_card_${tool.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(tool.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = null,
                                    tint = tool.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = tool.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tool.description,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // Notes Vault
            if (savedNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NoteAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No saved notes yet",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ask Omni AI in chat to save memos or key summaries, and they will be safely organized here.",
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(savedNotes, key = { it.id }) { note ->
                        SavedNoteCard(
                            note = note,
                            onDelete = {
                                viewModel.viewModelScope.launch {
                                    AppDatabase.getInstance(context).savedNoteDao().deleteNote(note)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Tool Execution Dialog
    activeToolForDialog?.let { tool ->
        AlertDialog(
            onDismissRequest = { activeToolForDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(tool.icon, contentDescription = null, tint = tool.color)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tool.title, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tool.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = toolInputText,
                        onValueChange = { toolInputText = it },
                        label = { Text("Details / Requirements") },
                        placeholder = { Text("e.g. details, topic, question...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fullPrompt = if (toolInputText.isNotBlank()) {
                            "${tool.promptTemplate}\n\n$toolInputText"
                        } else {
                            tool.promptTemplate
                        }
                        viewModel.sendUserMessage(fullPrompt)
                        activeToolForDialog = null
                        onNavigateToChat()
                    }
                ) {
                    Text("Execute in Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeToolForDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SavedNoteCard(
    note: SavedNote,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()).format(Date(note.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(note.title, note.content))
                        Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.content,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dateStr,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
