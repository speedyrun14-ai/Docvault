package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.DocRecord
import com.example.data.model.DocReminder
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DocuVaultRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun getOrCreateUserProfile(): UserProfile {
        val user = auth.currentUser ?: throw IllegalStateException("User not signed in")
        val uid = user.uid
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid)
                // Update last login
                docRef.update("lastLoginAt", FieldValue.serverTimestamp()).await()
                profile
            } else {
                val newProfile = UserProfile(
                    userId = uid,
                    fullName = user.displayName ?: user.email?.substringBefore("@") ?: "DocuVault User",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString(),
                    authProvider = user.providerData.firstOrNull { it.providerId != "firebase" }?.providerId ?: "google.com",
                    emailVerified = user.isEmailVerified,
                    accountStatus = "active",
                    preferredTheme = "system",
                    preferredLanguage = "en",
                    subscriptionPlan = "free"
                )
                val payload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "fullName" to newProfile.fullName,
                    "email" to newProfile.email,
                    "authProvider" to newProfile.authProvider,
                    "emailVerified" to newProfile.emailVerified,
                    "accountStatus" to newProfile.accountStatus,
                    "preferredTheme" to newProfile.preferredTheme,
                    "preferredLanguage" to newProfile.preferredLanguage,
                    "subscriptionPlan" to newProfile.subscriptionPlan,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp()
                )
                if (newProfile.photoUrl != null) {
                    payload["photoUrl"] = newProfile.photoUrl
                }
                docRef.set(payload).await()
                newProfile
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateUserProfile(fullName: String, theme: String = "system", plan: String = "free") {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "fullName" to fullName,
                    "preferredTheme" to theme,
                    "subscriptionPlan" to plan
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeDocuments(isVault: Boolean = false): Flow<List<DocRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/documents"
        emitAll(
            db.collection("users").document(uid).collection("documents")
                .whereEqualTo("isVault", isVault)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DocRecord::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createDocument(
        title: String,
        category: String,
        folder: String,
        pageCount: Long,
        fileSizeBytes: Long,
        storagePath: String,
        isVault: Boolean = false,
        isFavorite: Boolean = false,
        ocrText: String? = null,
        summary: String? = null,
        expiryDate: String? = null
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("documents").document()
        val payload = mutableMapOf<String, Any?>(
            "userId" to uid,
            "title" to title,
            "category" to category,
            "folder" to folder,
            "pageCount" to pageCount,
            "fileSizeBytes" to fileSizeBytes,
            "storagePath" to storagePath,
            "isVault" to isVault,
            "isFavorite" to isFavorite,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (ocrText != null) payload["ocrText"] = ocrText
        if (summary != null) payload["summary"] = summary
        if (expiryDate != null) payload["expiryDate"] = expiryDate

        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleFavorite(documentId: String, currentFavorite: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("documents").document(documentId)
        try {
            docRef.update(
                mapOf(
                    "isFavorite" to !currentFavorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateDocumentText(documentId: String, ocrText: String?, summary: String?) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("documents").document(documentId)
        val updates = mutableMapOf<String, Any?>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (ocrText != null) updates["ocrText"] = ocrText
        if (summary != null) updates["summary"] = summary

        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDocument(documentId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("documents").document(documentId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeReminders(): Flow<List<DocReminder>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/reminders"
        emitAll(
            db.collection("users").document(uid).collection("reminders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DocReminder::class.java).sortedBy { it.dueDate?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createReminder(
        title: String,
        dueDate: com.google.firebase.Timestamp,
        documentId: String = "",
        category: String = "Renewal"
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("reminders").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title,
            "dueDate" to dueDate,
            "documentId" to documentId,
            "category" to category,
            "isCompleted" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleReminderCompleted(reminderId: String, currentStatus: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("reminders").document(reminderId)
        try {
            docRef.update("isCompleted", !currentStatus).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteReminder(reminderId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("reminders").document(reminderId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
