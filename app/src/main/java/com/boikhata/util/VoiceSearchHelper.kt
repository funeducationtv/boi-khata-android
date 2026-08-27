package com.boikhata.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for Bengali voice search functionality
 * Supports voice input for book search, customer lookup, and navigation
 */
@Singleton
class VoiceSearchHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var currentListener: VoiceSearchListener? = null

    interface VoiceSearchListener {
        fun onResult(transcript: String)
        fun onError(error: String)
        fun onPartialResult(partial: String)
    }

    /**
     * Start listening for Bengali voice input
     * @param listener Callback for search results
     */
    fun startVoiceSearch(listener: VoiceSearchListener) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("ভয়েস সার্চ এই ডিভাইসে সমর্থিত নয়")
            return
        }

        if (isListening) {
            stopListening()
        }

        currentListener = listener
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // Ready to listen
            }

            override fun onBeginningOfSpeech() {
                // User started speaking
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Sound level changed
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }

            override fun onEndOfSpeech() {
                // User finished speaking
            }

            override fun onError(error: Int) {
                val errorMessage = getErrorMessage(error)
                listener.onError(errorMessage)
                stopListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { transcript ->
                    listener.onResult(transcript)
                }
                stopListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partials?.firstOrNull()?.let { partial ->
                    listener.onPartialResult(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Event received
            }
        })

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            listener.onError("ভয়েস সার্চ শুরু করা যায়নি: ${e.message}")
            stopListening()
        }
    }

    /**
     * Stop listening and cleanup resources
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
        currentListener = null
    }

    /**
     * Check if currently listening
     */
    fun isCurrentlyListening(): Boolean = isListening

    /**
     * Get human-readable error message in Bengali
     */
    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "অডিও রেকর্ডিং সমস্যা"
            SpeechRecognizer.ERROR_CLIENT -> "ক্লায়েন্ট সাইড সমস্যা"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন অনুমতি প্রয়োজন"
            SpeechRecognizer.ERROR_NETWORK -> "নেটওয়ার্ক সংযোগ সমস্যা"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "নেটওয়ার্ক টাইমআউট"
            SpeechRecognizer.ERROR_NO_MATCH -> "কোনো মিল পাওয়া যায়নি"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ভয়েস সার্ভিস ব্যস্ত"
            SpeechRecognizer.ERROR_SERVER -> "সার্ভার সমস্যা"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কথা বলা শেষ হয়ে গেছে"
            else -> "অজানা ত্রুটি occurred"
        }
    }

    /**
     * Launch system voice input activity directly
     * Useful for custom UI integration
     */
    fun launchVoiceInputActivity(requestCode: Int = 1001) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "বলুন...")
        }
        
        // Note: Caller should handle startActivityForResult
        // This is a helper to create the intent
    }
}
