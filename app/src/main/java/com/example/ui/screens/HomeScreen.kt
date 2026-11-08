package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DocRecord
import com.example.data.model.UserProfile
import com.example.ui.components.ActionCard
import com.example.ui.components.DocumentItemCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StorageIndicatorCard
import com.example.ui.theme.DocuBluePrimary
import com.example.ui.theme.DocuCyanSecondary
import com.example.ui.theme.DocuWarning

@Composable
fun HomeScreen(
    profile: UserProfile,
    documents: List<DocRecord>,
    onNavigateToScan: () -> Unit,
    onNavigateToTools: (String?) -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onOpenDocument: (DocRecord) -> Unit,
    onToggleFavorite: (DocRecord) -> Unit,
    onDeleteDocument: (DocRecord) -> Unit,
    onQuickAddSampleDoc: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredDocs = remember(documents, searchQuery) {
        if (searchQuery.isBlank()) documents
        else documents.filter { it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    val recentDocs = remember(filteredDocs) { filteredDocs.take(5) }
    val favoriteDocs = remember(filteredDocs) { filteredDocs.filter { it.isFavorite } }
    val totalStorageBytes = remember(documents) { documents.sumOf { it.fileSizeBytes } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Welcome Header Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!profile.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(DocuBluePrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.fullName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = DocuBluePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = profile.fullName.ifBlank { "DocuVault User" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onNavigateToReminders,
                        modifier = Modifier.testTag("btn_notifications_reminders")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Reminders",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onNavigateToVault,
                        modifier = Modifier.testTag("btn_header_vault")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Vault",
                            tint = Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }

        // Global Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search docs, invoices, contracts...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_home_search"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DocuBluePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // Quick Scan CTA Hero Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_quick_scan"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DocuBluePrimary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Document Scanner",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Auto edge detection, filters & instant OCR",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToScan,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_quick_scan_now")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = DocuBluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scan Now",
                                color = DocuBluePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Storage Usage Indicator
        item {
            StorageIndicatorCard(
                usedBytes = totalStorageBytes,
                documentCount = documents.size
            )
        }

        // Quick Action Shortcuts: AI Assistant & Secure Vault
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigateToAi() }
                        .testTag("shortcut_ai_assistant"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DocuCyanSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DocuCyanSecondary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI Assistant", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Summarize & Q&A", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigateToVault() }
                        .testTag("shortcut_secure_vault"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Secure Vault", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Encrypted Files", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Action Cards Grid (6 primary PDF tools)
        item {
            SectionHeader(title = "PDF Toolkit")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionCard(
                        title = "Scan Document",
                        subtitle = "Camera & OCR",
                        icon = Icons.Default.CameraAlt,
                        accentColor = DocuBluePrimary,
                        onClick = onNavigateToScan,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Image to PDF",
                        subtitle = "Convert Photos",
                        icon = Icons.Default.Image,
                        accentColor = Color(0xFF10B981),
                        onClick = { onNavigateToTools("img_to_pdf") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionCard(
                        title = "Merge PDF",
                        subtitle = "Combine files",
                        icon = Icons.Default.CallMerge,
                        accentColor = Color(0xFF8B5CF6),
                        onClick = { onNavigateToTools("merge") },
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Compress PDF",
                        subtitle = "Reduce size",
                        icon = Icons.Default.Compress,
                        accentColor = Color(0xFFEC4899),
                        onClick = { onNavigateToTools("compress") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionCard(
                        title = "Extract Text",
                        subtitle = "OCR Engine",
                        icon = Icons.Default.TextFields,
                        accentColor = Color(0xFFF59E0B),
                        onClick = { onNavigateToTools("ocr") },
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "PDF Editor",
                        subtitle = "Annotate & Sign",
                        icon = Icons.Default.Edit,
                        accentColor = Color(0xFF06B6D4),
                        onClick = { onNavigateToTools("sign") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent Documents Section
        item {
            SectionHeader(
                title = "Recent Documents",
                actionText = if (documents.isEmpty()) "+ Add Sample" else null,
                onActionClick = if (documents.isEmpty()) onQuickAddSampleDoc else null
            )
        }

        if (recentDocs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No documents yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan or create your first document to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onQuickAddSampleDoc,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Create Sample Document")
                        }
                    }
                }
            }
        } else {
            items(recentDocs) { doc ->
                DocumentItemCard(
                    doc = doc,
                    onClick = { onOpenDocument(doc) },
                    onFavoriteToggle = { onToggleFavorite(doc) },
                    onMenuClick = { onOpenDocument(doc) }
                )
            }
        }

        // Favorites Section if any
        if (favoriteDocs.isNotEmpty()) {
            item {
                SectionHeader(title = "Starred Documents")
            }
            items(favoriteDocs) { doc ->
                DocumentItemCard(
                    doc = doc,
                    onClick = { onOpenDocument(doc) },
                    onFavoriteToggle = { onToggleFavorite(doc) },
                    onMenuClick = { onOpenDocument(doc) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
