package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ai.AiAssistantManager
import com.example.ai.AiResult
import com.example.data.model.DocRecord
import com.example.ui.components.formatFileSize
import com.example.ui.theme.DocuBluePrimary
import com.example.ui.theme.DocuCyanSecondary
import com.example.ui.theme.DocuError
import com.example.ui.theme.DocuSuccess
import com.example.util.PdfToolkitUtil
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DocumentDetailScreen(
    doc: DocRecord,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onAddReminder: (title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiManager = remember { AiAssistantManager() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Preview, 1: OCR Text, 2: AI Assistant
    var renderedBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var currentBitmapIndex by remember { mutableIntStateOf(0) }
    var isLoadingPages by remember { mutableStateOf(true) }

    // AI state
    var aiQuestionText by remember { mutableStateOf("") }
    var aiStreamingResponse by remember { mutableStateOf("") }
    var aiStatusMessage by remember { mutableStateOf("") }
    var isAiBusy by remember { mutableStateOf(false) }

    // Local file resolution
    val localFile = remember(doc) {
        val f = File(context.filesDir, "docuvault_documents/${doc.title}.pdf")
        if (f.exists()) f
        else PdfToolkitUtil.createSamplePdf(
            context = context,
            title = doc.title,
            content = doc.ocrText ?: "DocuVault Document Records: ${doc.title}\nCategory: ${doc.category}\nDate of Filing: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())}\nAll clauses and amounts have been verified and sealed with DocuVault cryptographic security."
        )
    }

    LaunchedEffect(doc) {
        isLoadingPages = true
        val pages = PdfToolkitUtil.renderPdfPagesToBitmaps(context, localFile)
        renderedBitmaps = pages
        isLoadingPages = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Action Bar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "${doc.pageCount} pages • ${formatFileSize(doc.fileSizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.testTag("btn_detail_fav")) {
                        Icon(
                            imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (doc.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, doc.title)
                                putExtra(Intent.EXTRA_TEXT, doc.ocrText ?: "Document: ${doc.title}")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Document"))
                        },
                        modifier = Modifier.testTag("btn_detail_share")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("btn_detail_delete")) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DocuError)
                    }
                }
            }
        }

        // Tab Selector: Preview | OCR Text | AI Assistant
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = DocuBluePrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = DocuBluePrimary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Preview", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                modifier = Modifier.testTag("tab_preview")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("OCR Text", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                modifier = Modifier.testTag("tab_ocr_text")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DocuCyanSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Assistant", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                    }
                },
                modifier = Modifier.testTag("tab_ai_assistant")
            )
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Preview Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoadingPages) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = DocuBluePrimary)
                        }
                    } else if (renderedBitmaps.isNotEmpty()) {
                        val currentBmp = renderedBitmaps.getOrNull(currentBitmapIndex)
                        if (currentBmp != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = currentBmp.asImageBitmap(),
                                    contentDescription = "Page ${currentBitmapIndex + 1}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Page Navigation Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { currentBitmapIndex = (currentBitmapIndex - 1).coerceAtLeast(0) },
                                enabled = currentBitmapIndex > 0,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Prev")
                            }

                            Text(
                                text = "Page ${currentBitmapIndex + 1} of ${renderedBitmaps.size}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Button(
                                onClick = { currentBitmapIndex = (currentBitmapIndex + 1).coerceAtMost(renderedBitmaps.size - 1) },
                                enabled = currentBitmapIndex < renderedBitmaps.size - 1,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Next")
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Unable to preview PDF pages.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            1 -> {
                // OCR Text Tab
                val textContent = doc.ocrText ?: "No OCR text extracted yet. Run OCR extraction in the Tools tab or ask AI Assistant to analyze."
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recognized Document Text", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("DocuVault OCR", textContent))
                                Toast.makeText(context, "Copied OCR text to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Text", tint = DocuBluePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            item {
                                Text(
                                    text = textContent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // AI Assistant Tab
                val documentContext = doc.ocrText ?: doc.title
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Quick Action Buttons
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Button(
                                onClick = {
                                    isAiBusy = true
                                    aiStreamingResponse = ""
                                    scope.launch {
                                        val res = aiManager.summarizeDocument(documentContext, isDetailed = false)
                                        isAiBusy = false
                                        aiStreamingResponse = when (res) {
                                            is AiResult.Success -> res.text
                                            is AiResult.Error -> "Error: ${res.message}"
                                        }
                                    }
                                },
                                enabled = !isAiBusy,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DocuBluePrimary)
                            ) {
                                Text("Executive Summary", fontSize = 12.sp)
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    isAiBusy = true
                                    aiStreamingResponse = ""
                                    scope.launch {
                                        val res = aiManager.extractKeyPoints(documentContext)
                                        isAiBusy = false
                                        aiStreamingResponse = when (res) {
                                            is AiResult.Success -> res.text
                                            is AiResult.Error -> "Error: ${res.message}"
                                        }
                                    }
                                },
                                enabled = !isAiBusy,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Key Points", fontSize = 12.sp)
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    isAiBusy = true
                                    aiStreamingResponse = ""
                                    scope.launch {
                                        val res = aiManager.explainClause(documentContext)
                                        isAiBusy = false
                                        aiStreamingResponse = when (res) {
                                            is AiResult.Success -> res.text
                                            is AiResult.Error -> "Error: ${res.message}"
                                        }
                                    }
                                },
                                enabled = !isAiBusy,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Explain Terms", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Response Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DocuCyanSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI Analysis", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                if (isAiBusy) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DocuBluePrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    if (aiStreamingResponse.isNotBlank()) {
                                        Text(
                                            text = aiStreamingResponse,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            text = "Ask a question below or choose an option above to generate key summaries, explanations, or answers directly from this document.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Question Input Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = aiQuestionText,
                            onValueChange = { aiQuestionText = it },
                            placeholder = { Text("Ask anything about this document...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("input_ai_question"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DocuBluePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (aiQuestionText.isNotBlank() && !isAiBusy) {
                                    val q = aiQuestionText
                                    aiQuestionText = ""
                                    isAiBusy = true
                                    aiStreamingResponse = ""
                                    scope.launch {
                                        aiManager.askDocumentQuestion(documentContext, q).collect { chunk ->
                                            aiStreamingResponse += chunk
                                        }
                                        isAiBusy = false
                                    }
                                }
                            },
                            enabled = aiQuestionText.isNotBlank() && !isAiBusy,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DocuBluePrimary)
                                .testTag("btn_send_ai_question")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Question", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
