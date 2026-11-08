package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.credentials.CredentialManager
import com.example.auth.AuthManager
import com.example.data.model.DocRecord
import com.example.data.model.DocReminder
import com.example.data.model.UserProfile
import com.example.data.repository.DocuVaultRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.DocuBluePrimary
import com.example.ui.theme.DocuVaultTheme
import com.example.util.PdfToolkitUtil
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.io.File

sealed interface ActiveScreen {
    data object MainTabs : ActiveScreen
    data class DocumentDetail(val doc: DocRecord) : ActiveScreen
    data object Vault : ActiveScreen
    data object Reminders : ActiveScreen
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check contract wiring with launch Intent before setContent or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            DocuVaultTheme(darkTheme = isDarkTheme) {
                AppGate(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = it }
                )
            }
        }
    }
}

@Composable
fun AppGate(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isCheckingAuth by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            isCheckingAuth = false
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                isCheckingAuth = false
            },
            onUnauthenticated = {
                isCheckingAuth = false
            }
        )
    }

    if (isCheckingAuth) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = DocuBluePrimary)
        }
    } else if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            MainFeatureContainer(
                user = currentUser!!,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onSignOut = {
                    AuthManager.signOut(
                        credentialManager = credentialManager,
                        scope = scope,
                        onSignOutComplete = {
                            currentUser = null
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun MainFeatureContainer(
    user: FirebaseUser,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { DocuVaultRepository(context) }

    var profile by remember {
        mutableStateOf(
            UserProfile(
                userId = user.uid,
                fullName = user.displayName ?: "DocuVault User",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString()
            )
        )
    }

    LaunchedEffect(user.uid) {
        try {
            profile = repository.getOrCreateUserProfile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val normalDocuments by repository.observeDocuments(isVault = false).collectAsState(initial = emptyList())
    val vaultDocuments by repository.observeDocuments(isVault = true).collectAsState(initial = emptyList())
    val reminders by repository.observeReminders().collectAsState(initial = emptyList())

    var currentTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Files, 2: Scan, 3: Tools, 4: Profile
    var activeScreen by remember { mutableStateOf<ActiveScreen>(ActiveScreen.MainTabs) }
    var selectedToolInitialId by remember { mutableStateOf<String?>(null) }

    // Seed sample document if library is brand new
    fun seedSampleDocument() {
        scope.launch {
            try {
                val sampleFile = PdfToolkitUtil.createSamplePdf(
                    context = context,
                    title = "Business_Contract_Sample",
                    content = """
                        DOCUVAULT MASTER SERVICES AGREEMENT
                        Effective Date: November 2026
                        Parties: Enterprise Solutions Corp & DocuVault Client

                        1. OBLIGATIONS & SCOPE
                        The contractor shall furnish document management and verification services.
                        All deliverables must be completed by December 31, 2026.

                        2. FINANCIAL TERMS
                        Total Fee: $4,500.00 USD net 30 days upon delivery.
                        Late payments incur 1.5% monthly service charge.

                        3. CONFIDENTIALITY
                        All records and intellectual property remain strictly confidential.
                    """.trimIndent()
                )
                repository.createDocument(
                    title = "Business_Contract_Sample",
                    category = "Contracts",
                    folder = "Business",
                    pageCount = 1,
                    fileSizeBytes = sampleFile.length(),
                    storagePath = sampleFile.absolutePath,
                    isVault = false,
                    isFavorite = true,
                    ocrText = "DOCUVAULT MASTER SERVICES AGREEMENT\nTotal Fee: $4,500.00 USD\nDeliverables due Dec 31, 2026."
                )
                Toast.makeText(context, "Sample document created", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    when (val screen = activeScreen) {
        is ActiveScreen.DocumentDetail -> {
            BackHandler { activeScreen = ActiveScreen.MainTabs }
            DocumentDetailScreen(
                doc = screen.doc,
                onBack = { activeScreen = ActiveScreen.MainTabs },
                onToggleFavorite = {
                    scope.launch {
                        repository.toggleFavorite(screen.doc.id, screen.doc.isFavorite)
                    }
                },
                onDelete = {
                    scope.launch {
                        repository.deleteDocument(screen.doc.id)
                        activeScreen = ActiveScreen.MainTabs
                        Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                    }
                },
                onAddReminder = { title ->
                    scope.launch {
                        repository.createReminder(
                            title = "Review: $title",
                            dueDate = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() + 86400000L * 30))
                        )
                        Toast.makeText(context, "Reminder added for $title", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        is ActiveScreen.Vault -> {
            BackHandler { activeScreen = ActiveScreen.MainTabs }
            VaultScreen(
                vaultDocuments = vaultDocuments,
                onBack = { activeScreen = ActiveScreen.MainTabs },
                onOpenDocument = { doc -> activeScreen = ActiveScreen.DocumentDetail(doc) },
                onAddDocumentToVault = { title, category ->
                    scope.launch {
                        val vaultFile = PdfToolkitUtil.createSamplePdf(
                            context = context,
                            title = title,
                            content = "CONFIDENTIAL ENCRYPTED RECORD\nTitle: $title\nCategory: $category\nSealed in DocuVault AES-256 local vault."
                        )
                        repository.createDocument(
                            title = title,
                            category = category,
                            folder = "Vault",
                            pageCount = 1,
                            fileSizeBytes = vaultFile.length(),
                            storagePath = vaultFile.absolutePath,
                            isVault = true,
                            isFavorite = false
                        )
                    }
                },
                onDeleteDocument = { doc ->
                    scope.launch { repository.deleteDocument(doc.id) }
                }
            )
        }

        is ActiveScreen.Reminders -> {
            BackHandler { activeScreen = ActiveScreen.MainTabs }
            RemindersScreen(
                reminders = reminders,
                onBack = { activeScreen = ActiveScreen.MainTabs },
                onCreateReminder = { title, cat, due ->
                    scope.launch { repository.createReminder(title, due, "", cat) }
                },
                onToggleCompleted = { rem ->
                    scope.launch { repository.toggleReminderCompleted(rem.id, rem.isCompleted) }
                },
                onDeleteReminder = { rem ->
                    scope.launch { repository.deleteReminder(rem.id) }
                }
            )
        }

        ActiveScreen.MainTabs -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    DocuVaultBottomNavigation(
                        currentTab = currentTab,
                        onTabSelect = { tabIndex ->
                            currentTab = tabIndex
                            selectedToolInitialId = null
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        0 -> HomeScreen(
                            profile = profile,
                            documents = normalDocuments,
                            onNavigateToScan = { currentTab = 2 },
                            onNavigateToTools = { toolId ->
                                selectedToolInitialId = toolId
                                currentTab = 3
                            },
                            onNavigateToVault = { activeScreen = ActiveScreen.Vault },
                            onNavigateToAi = {
                                val target = normalDocuments.firstOrNull()
                                if (target != null) {
                                    activeScreen = ActiveScreen.DocumentDetail(target)
                                } else {
                                    seedSampleDocument()
                                }
                            },
                            onNavigateToReminders = { activeScreen = ActiveScreen.Reminders },
                            onOpenDocument = { doc -> activeScreen = ActiveScreen.DocumentDetail(doc) },
                            onToggleFavorite = { doc ->
                                scope.launch { repository.toggleFavorite(doc.id, doc.isFavorite) }
                            },
                            onDeleteDocument = { doc ->
                                scope.launch { repository.deleteDocument(doc.id) }
                            },
                            onQuickAddSampleDoc = { seedSampleDocument() }
                        )

                        1 -> FilesScreen(
                            documents = normalDocuments,
                            onOpenDocument = { doc -> activeScreen = ActiveScreen.DocumentDetail(doc) },
                            onToggleFavorite = { doc ->
                                scope.launch { repository.toggleFavorite(doc.id, doc.isFavorite) }
                            },
                            onDeleteDocument = { doc ->
                                scope.launch { repository.deleteDocument(doc.id) }
                            }
                        )

                        2 -> ScanScreen(
                            onScanSaved = { title, category, file, pageCount, ocrText ->
                                scope.launch {
                                    repository.createDocument(
                                        title = title,
                                        category = category,
                                        folder = "Scans",
                                        pageCount = pageCount.toLong(),
                                        fileSizeBytes = file.length(),
                                        storagePath = file.absolutePath,
                                        isVault = false,
                                        isFavorite = false,
                                        ocrText = ocrText
                                    )
                                    Toast.makeText(context, "Scan saved to DocuVault", Toast.LENGTH_SHORT).show()
                                    currentTab = 1
                                }
                            }
                        )

                        3 -> ToolsScreen(
                            documents = normalDocuments,
                            initialToolId = selectedToolInitialId,
                            onDocumentCreated = { title, category, file, pages, ocrText ->
                                scope.launch {
                                    repository.createDocument(
                                        title = title,
                                        category = category,
                                        folder = "Processed",
                                        pageCount = pages.toLong(),
                                        fileSizeBytes = file.length(),
                                        storagePath = file.absolutePath,
                                        isVault = false,
                                        isFavorite = false,
                                        ocrText = ocrText
                                    )
                                    Toast.makeText(context, "New document created: $title", Toast.LENGTH_SHORT).show()
                                    currentTab = 1
                                }
                            }
                        )

                        4 -> ProfileScreen(
                            profile = profile,
                            totalStorageBytes = normalDocuments.sumOf { it.fileSizeBytes } + vaultDocuments.sumOf { it.fileSizeBytes },
                            totalDocuments = normalDocuments.size + vaultDocuments.size,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = onToggleTheme,
                            onUpdateDisplayName = { newName ->
                                scope.launch {
                                    repository.updateUserProfile(newName, if (isDarkTheme) "dark" else "light", profile.subscriptionPlan)
                                    profile = profile.copy(fullName = newName)
                                }
                            },
                            onNavigateToReminders = { activeScreen = ActiveScreen.Reminders },
                            onNavigateToVault = { activeScreen = ActiveScreen.Vault },
                            onSignOut = onSignOut,
                            onDeleteAccount = {
                                scope.launch {
                                    try {
                                        normalDocuments.forEach { repository.deleteDocument(it.id) }
                                        vaultDocuments.forEach { repository.deleteDocument(it.id) }
                                        reminders.forEach { repository.deleteReminder(it.id) }
                                        user.delete()
                                        onSignOut()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocuVaultBottomNavigation(
    currentTab: Int,
    onTabSelect: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { onTabSelect(0) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DocuBluePrimary,
                selectedTextColor = DocuBluePrimary,
                indicatorColor = DocuBluePrimary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_item_home")
        )

        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { onTabSelect(1) },
            icon = { Icon(Icons.Default.Folder, contentDescription = "Files") },
            label = { Text("Files", fontSize = 11.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DocuBluePrimary,
                selectedTextColor = DocuBluePrimary,
                indicatorColor = DocuBluePrimary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_item_files")
        )

        // Prominent Center Scan Item
        NavigationBarItem(
            selected = currentTab == 2,
            onClick = { onTabSelect(2) },
            icon = {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DocuBluePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            label = { Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DocuBluePrimary) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.testTag("nav_item_scan")
        )

        NavigationBarItem(
            selected = currentTab == 3,
            onClick = { onTabSelect(3) },
            icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
            label = { Text("Tools", fontSize = 11.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DocuBluePrimary,
                selectedTextColor = DocuBluePrimary,
                indicatorColor = DocuBluePrimary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_item_tools")
        )

        NavigationBarItem(
            selected = currentTab == 4,
            onClick = { onTabSelect(4) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 11.sp, fontWeight = if (currentTab == 4) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DocuBluePrimary,
                selectedTextColor = DocuBluePrimary,
                indicatorColor = DocuBluePrimary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_item_profile")
        )
    }
}
