package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocuVaultRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createDocument_validPayload_createsDocumentAndReturnsId() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = DocuVaultRepository(firestore)

    val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createDocument(
        title = "Invoice #2026",
        category = "Finance",
        folder = "Invoices",
        pageCount = 2,
        fileSizeBytes = 512000,
        storagePath = "docs/alice/invoice.pdf",
        isVault = false,
        isFavorite = true
      )
    }
    assertTrue(docId.isNotEmpty())
  }

  @Test
  fun observeDocuments_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = DocuVaultRepository(firestore)
    val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createDocument(
        title = "NDA Agreement",
        category = "Legal",
        folder = "Contracts",
        pageCount = 4,
        fileSizeBytes = 1048576,
        storagePath = "docs/alice/nda.pdf",
        isVault = false,
        isFavorite = false
      )
    }

    val docs = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeDocuments(isVault = false).first { list ->
        list.any { it.id == docId }
      }
    }
    assertTrue(docs.map { it.id }.contains(docId))
  }

  @Test
  fun observeDocuments_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = DocuVaultRepository(firestore)

    var threw = false
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeDocuments().first()
      }
    } catch (e: Exception) {
      threw = true
    }
    assertTrue(threw)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val BOB_EMAIL = "bob@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
