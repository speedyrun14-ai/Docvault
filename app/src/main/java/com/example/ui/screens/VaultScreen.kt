package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.model.DocRecord
import com.example.ui.components.DocumentItemCard
import com.example.ui.theme.DocuBluePrimary
import com.example.ui.theme.DocuError
import com.example.ui.theme.DocuNavyDark
import com.example.ui.theme.DocuSuccess
import com.example.util.SecureVaultManager

@Composable
fun VaultScreen(
    vaultDocuments: List<DocRecord>,
    onBack: () -> Unit,
    onOpenDocument: (DocRecord) -> Unit,
    onAddDocumentToVault: (title: String, category: String) -> Unit,
    onDeleteDocument: (DocRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var isSettingUpPin by remember { mutableStateOf(!SecureVaultManager.isVaultSetup(context)) }
    var setupFirstPin by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var newDocTitle by remember { mutableStateOf("") }
    var newDocCategory by remember { mutableStateOf("Personal") }

    fun triggerBiometricPrompt() {
        if (activity == null) {
            Toast.makeText(context, "Biometric authentication not supported in this context", Toast.LENGTH_SHORT).show()
            return
        }

        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(context)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock DocuVault Vault")
                .setSubtitle("Confirm fingerprint or face biometric to access encrypted documents")
                .setNegativeButtonText("Use PIN")
                .build()

            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        isUnlocked = true
                        pinError = false
                        Toast.makeText(context, "Vault unlocked with biometrics", Toast.LENGTH_SHORT).show()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // User cancelled or selected "Use PIN"
                    }

                    override fun onAuthenticationFailed() {
                        Toast.makeText(context, "Biometric not recognized. Try again or enter PIN.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            prompt.authenticate(promptInfo)
        } else {
            Toast.makeText(context, "No biometrics enrolled. Use PIN to unlock.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Vault Header
        Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_vault_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Encrypted Document Vault",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (isUnlocked) {
                    IconButton(
                        onClick = {
                            isUnlocked = false
                            enteredPin = ""
                        },
                        modifier = Modifier.testTag("btn_lock_vault")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock Vault", tint = Color(0xFFF59E0B))
                    }
                }
            }
        }

        if (!isUnlocked) {
            // Locked Keypad State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isSettingUpPin) "Set Up Vault PIN" else "Enter Vault PIN",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (isSettingUpPin) "Create a 4-digit PIN for your AES-256 encrypted storage" else "Your sensitive documents are encrypted and protected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // PIN Dots
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (i in 0 until 4) {
                        val isFilled = enteredPin.length > i
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        pinError -> DocuError
                                        isFilled -> DocuBluePrimary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                        )
                    }
                }

                if (pinError) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Incorrect PIN. Please try again.",
                        color = DocuError,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Numeric Keypad
                Column(
                    modifier = Modifier.width(260.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 1..3) {
                                val digit = row * 3 + col
                                KeypadButton(text = "$digit") {
                                    if (enteredPin.length < 4) {
                                        enteredPin += "$digit"
                                        pinError = false
                                        if (enteredPin.length == 4) {
                                            if (isSettingUpPin) {
                                                SecureVaultManager.setupPin(context, enteredPin)
                                                isSettingUpPin = false
                                                isUnlocked = true
                                                Toast.makeText(context, "Vault PIN Configured", Toast.LENGTH_SHORT).show()
                                            } else {
                                                if (SecureVaultManager.verifyPin(context, enteredPin)) {
                                                    isUnlocked = true
                                                } else {
                                                    pinError = true
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Row: Biometric / 0 / Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                triggerBiometricPrompt()
                            },
                            modifier = Modifier.size(64.dp).testTag("btn_vault_biometric")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = "Biometric Unlock", tint = DocuBluePrimary, modifier = Modifier.size(32.dp))
                        }

                        KeypadButton(text = "0") {
                            if (enteredPin.length < 4) {
                                enteredPin += "0"
                                pinError = false
                                if (enteredPin.length == 4) {
                                    if (isSettingUpPin) {
                                        SecureVaultManager.setupPin(context, enteredPin)
                                        isSettingUpPin = false
                                        isUnlocked = true
                                    } else {
                                        if (SecureVaultManager.verifyPin(context, enteredPin)) {
                                            isUnlocked = true
                                        } else {
                                            pinError = true
                                            enteredPin = ""
                                        }
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            // Unlocked Vault Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Private Encrypted Files",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${vaultDocuments.size} files protected with AES-256",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_add_to_vault")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add File")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (vaultDocuments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = DocuSuccess, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Vault is empty", fontWeight = FontWeight.Bold)
                            Text("Add your IDs, tax records, or passwords here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(vaultDocuments) { doc ->
                            DocumentItemCard(
                                doc = doc,
                                onClick = { onOpenDocument(doc) },
                                onFavoriteToggle = {},
                                onMenuClick = { onOpenDocument(doc) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Store Document in Vault") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newDocTitle,
                        onValueChange = { newDocTitle = it },
                        label = { Text("Document Title (e.g. Passport Scan)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newDocCategory,
                        onValueChange = { newDocCategory = it },
                        label = { Text("Category (e.g. Identity, Financial)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDocTitle.isNotBlank()) {
                            onAddDocumentToVault(newDocTitle, newDocCategory)
                            showAddDialog = false
                            newDocTitle = ""
                        }
                    }
                ) {
                    Text("Encrypt & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun KeypadButton(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
