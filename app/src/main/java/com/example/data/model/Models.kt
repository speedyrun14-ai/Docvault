package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val authProvider: String = "google.com",
    val emailVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    val accountStatus: String = "active",
    val preferredTheme: String = "system",
    val preferredLanguage: String = "en",
    val subscriptionPlan: String = "free"
)

@IgnoreExtraProperties
data class DocRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "General",
    val folder: String = "Documents",
    val pageCount: Long = 1L,
    val fileSizeBytes: Long = 0L,
    val storagePath: String = "",
    val isVault: Boolean = false,
    val isFavorite: Boolean = false,
    val ocrText: String? = null,
    val summary: String? = null,
    val expiryDate: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DocReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val documentId: String = "",
    val title: String = "",
    val dueDate: Timestamp? = null,
    val category: String = "Renewal",
    val isCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
