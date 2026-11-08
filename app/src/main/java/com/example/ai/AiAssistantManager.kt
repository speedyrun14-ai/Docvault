package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

sealed interface AiResult {
    data class Success(val text: String) : AiResult
    data class Error(val message: String) : AiResult
}

class AiAssistantManager {

    private val model: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun summarizeDocument(
        documentText: String,
        isDetailed: Boolean = false
    ): AiResult = withContext(Dispatchers.IO) {
        if (documentText.isBlank()) {
            return@withContext AiResult.Error("Document content is empty. Please scan or load a document with text.")
        }
        val prompt = if (isDetailed) {
            "Provide an in-depth, structured summary of the following document. Include key sections, entities, dates, obligations, and financial figures where present:\n\n$documentText"
        } else {
            "Provide a concise executive summary (3-5 bullet points) of the core meaning of this document:\n\n$documentText"
        }

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                AiResult.Success(text)
            } else {
                AiResult.Error("No summary could be generated.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiAssistant", "Failed to summarize document", e)
            AiResult.Error(e.localizedMessage ?: "AI service is currently unavailable. Please verify network connection.")
        }
    }

    suspend fun askDocumentQuestion(
        documentText: String,
        question: String
    ): Flow<String> = flow {
        if (documentText.isBlank()) {
            emit("Error: Document contains no text to analyze.")
            return@flow
        }
        val prompt = """
            You are DocuVault AI Assistant, an expert document analyst.
            Answer the user's question accurately using ONLY the provided document context whenever possible.
            If the answer cannot be determined from the document, clearly state that.

            DOCUMENT CONTEXT:
            $documentText

            QUESTION:
            $question
        """.trimIndent()

        try {
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        emit(chunkText)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiAssistant", "Streaming Q&A failed", e)
            emit("\n[Error: ${e.localizedMessage ?: "AI response interrupted"}]")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun extractKeyPoints(documentText: String): AiResult = withContext(Dispatchers.IO) {
        val prompt = "Extract all key bullet points, critical action items, due dates, and names from this document:\n\n$documentText"
        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) AiResult.Success(text) else AiResult.Error("Unable to extract key points.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiAssistant", "Key points extraction failed", e)
            AiResult.Error(e.localizedMessage ?: "AI service unavailable.")
        }
    }

    suspend fun translateText(documentText: String, targetLanguage: String): AiResult = withContext(Dispatchers.IO) {
        val prompt = "Translate the following document text into $targetLanguage accurately, preserving original format:\n\n$documentText"
        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) AiResult.Success(text) else AiResult.Error("Unable to translate text.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiAssistant", "Translation failed", e)
            AiResult.Error(e.localizedMessage ?: "AI translation service unavailable.")
        }
    }

    suspend fun explainClause(clauseText: String): AiResult = withContext(Dispatchers.IO) {
        val prompt = "Explain the following legal, technical, or complex text in clear, plain English that anyone can easily understand. Highlight any risks or notable terms:\n\n$clauseText"
        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) AiResult.Success(text) else AiResult.Error("Unable to explain clause.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiAssistant", "Explanation failed", e)
            AiResult.Error(e.localizedMessage ?: "AI service unavailable.")
        }
    }
}
