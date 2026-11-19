package com.example.ui.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.audio.VoiceEngineState
import com.example.data.model.AiMessage
import com.example.data.model.Reminder
import com.example.ui.theme.NoorBorderGreen
import com.example.ui.theme.NoorCardGreen
import com.example.ui.theme.NoorCrimson
import com.example.ui.theme.NoorCrimsonSurface
import com.example.ui.theme.NoorDarkGreen
import com.example.ui.theme.NoorDeepestGreen
import com.example.ui.theme.NoorElevatedGreen
import com.example.ui.theme.NoorGold
import com.example.ui.theme.NoorGoldSoft
import com.example.ui.theme.NoorGoldSurface
import com.example.ui.theme.NoorIvory
import com.example.ui.theme.NoorMintAccent
import com.example.ui.theme.NoorSageText
import java.util.Locale

@Composable
fun NoorAiScreen(
    messages: List<AiMessage>,
    reminders: List<Reminder>,
    streamingText: String?,
    errorText: String?,
    liveVoiceReplyEnabled: Boolean,
    voiceState: VoiceEngineState,
    onToggleLiveVoiceReply: (Boolean) -> Unit,
    onSendMessage: (String, Boolean) -> Unit,
    onSpeakBubble: (String, String) -> Unit,
    onStopVoice: () -> Unit,
    onDeleteReminder: (String) -> Unit
) {
    val context = LocalContext.current
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickPrompts = remember {
        listOf(
            "আজকে মনটা খুব খারাপ, সান্ত্বনা ও দোয়া দিন",
            "নামাজ ও অজুর সঠিক নিয়ম সহজ করে বলুন",
            "পানি নেওয়া বা ইস্তিনজার সঠিক নিয়ম কী?",
            "বিপদে Self Defence ও পালানোর উপায় কী?",
            "আমার কালকে সকাল ৬ টায় উঠাইয়া দিও"
        )
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                // Mukhe Koile AI Mukhe Uttor Dibo -> always auto-speak reply when spoken via mic!
                onSendMessage(spoken, true)
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchBanglaVoiceRecognizer(speechLauncher)
        }
    }

    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText != null) {
            val targetIndex = (messages.size - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoorDarkGreen)
    ) {
        // ChatGPT-style Live Chat + Female Voice Header
        Surface(color = NoorDeepestGreen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NoorGoldSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Noor Live AI",
                                tint = NoorGold
                            )
                        }
                        Column {
                            Text(
                                text = "Noor — Live Chat & Voice AI",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (voiceState.isSpeaking) {
                                    "Noor কথা বলছে (Female Voice)..."
                                } else {
                                    "মুখে বললে AI মুখে উত্তর দিবে • অফলাইনেও কাজ করে"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = NoorMintAccent
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (voiceState.isSpeaking) {
                            IconButton(onClick = onStopVoice) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Stop Voice",
                                    tint = NoorCrimson
                                )
                            }
                        }
                        FilterChip(
                            selected = liveVoiceReplyEnabled,
                            onClick = { onToggleLiveVoiceReply(!liveVoiceReplyEnabled) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(if (liveVoiceReplyEnabled) "Voice ON" else "Muted") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NoorGold,
                                selectedLabelColor = NoorDeepestGreen,
                                selectedLeadingIconColor = NoorDeepestGreen,
                                containerColor = NoorCardGreen,
                                labelColor = NoorSageText
                            )
                        )
                    }
                }

                // Active Boss Alarms / Reminders Strip
                if (reminders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(reminders.take(5), key = { it.id }) { rem ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NoorCardGreen,
                                border = BorderStroke(1.dp, NoorGold.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = NoorGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%02d:%02d • %s",
                                            rem.hour,
                                            rem.minute,
                                            rem.title
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NoorIvory,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Alarm",
                                        tint = NoorSageText,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onDeleteReminder(rem.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && streamingText == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                        border = BorderStroke(1.dp, NoorGold.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "আসসালামু আলাইকুম Boss! আমি Noor AI",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "নামাজ, অজু, পানি নেওয়া, দোয়া, Self Defence অথবা মন খারাপ লাগলে সরাসরি বাংলায় লিখুন বা মাইকে বলুন — আমি মুখে উত্তরও দেব!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = NoorIvory
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    isUser = msg.sender == "USER",
                    text = msg.text,
                    reference = msg.reference,
                    onSpeak = { onSpeakBubble("msg_${msg.id}", msg.text) }
                )
            }

            if (streamingText != null) {
                item {
                    ChatBubble(
                        isUser = false,
                        text = if (streamingText.isBlank()) "Ji Boss, ভাবছি ও কুরআন-হাদীস দেখছি..." else streamingText,
                        reference = null,
                        onSpeak = {}
                    )
                }
            }

            if (!errorText.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NoorCrimsonSurface,
                        border = BorderStroke(1.dp, NoorCrimson)
                    ) {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NoorIvory,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Quick Prompts + Mon Kharap Button
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AssistChip(
                    onClick = {
                        onSendMessage("Boss Apnar Mon Kharap? Asen Ekta Dua Pori — আমার মন খারাপ, আমাকে সান্ত্বনা ও দোয়া দিন", true)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = NoorGold, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("মন খারাপ? সান্ত্বনা ও দোয়া", color = NoorGold) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = NoorGoldSurface),
                    border = BorderStroke(1.dp, NoorGold)
                )
            }
            items(quickPrompts) { prompt ->
                AssistChip(
                    onClick = { onSendMessage(prompt, liveVoiceReplyEnabled) },
                    label = { Text(prompt, color = NoorGoldSoft) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = NoorCardGreen),
                    border = BorderStroke(1.dp, NoorBorderGreen)
                )
            }
        }

        // Chat + Live Voice Input Bar
        Surface(color = NoorDeepestGreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("বাংলায় প্রশ্ন লিখুন বা মাইকে বলুন...", color = NoorSageText) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input"),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NoorGold,
                        unfocusedBorderColor = NoorBorderGreen,
                        focusedTextColor = NoorIvory,
                        unfocusedTextColor = NoorIvory,
                        focusedContainerColor = NoorCardGreen,
                        unfocusedContainerColor = NoorCardGreen
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        val hasAudio = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasAudio) {
                            launchBanglaVoiceRecognizer(speechLauncher)
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NoorElevatedGreen)
                        .testTag("ai_voice_mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Live Voice Chat",
                        tint = NoorGold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val q = inputText
                            inputText = ""
                            onSendMessage(q, liveVoiceReplyEnabled)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NoorGold)
                        .testTag("ai_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = NoorDeepestGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    isUser: Boolean,
    text: String,
    reference: String?,
    onSpeak: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 315.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) NoorElevatedGreen else NoorCardGreen
            ),
            border = BorderStroke(
                1.dp,
                if (isUser) NoorBorderGreen else NoorGold.copy(alpha = 0.55f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Noor AI",
                            style = MaterialTheme.typography.labelMedium,
                            color = NoorGold,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak Answer",
                            tint = NoorGold,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(onClick = onSpeak)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NoorIvory
                )
                if (!reference.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NoorGoldSurface
                    ) {
                        Text(
                            text = "সূত্র: $reference",
                            style = MaterialTheme.typography.labelMedium,
                            color = NoorGoldSoft,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun launchBanglaVoiceRecognizer(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Boss, বলুন — আমি শুনছি...")
    }
    runCatching { launcher.launch(intent) }
}
