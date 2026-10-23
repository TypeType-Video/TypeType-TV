package video.typetype.tv.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import video.typetype.tv.resources.R

// Speech typed into the search field, through the device's recognition service (on most TVs, Google's): the
// words appear as they are heard and the final sentence becomes the query. Nothing is recorded by the app.
@Stable
class VoiceSearch(private val context: Context) {
    val available: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    var listening by mutableStateOf(false)
        private set

    var heard by mutableStateOf("")
        private set

    @get:StringRes
    var notice by mutableStateOf<Int?>(null)
        private set

    // Loudness of the voice from 0 to 1; only the draw phase reads it.
    val level: MutableFloatState = mutableFloatStateOf(0f)

    private var recognizer: SpeechRecognizer? = null

    fun start(onResult: (String) -> Unit) {
        if (!available || listening) return
        val speech = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        speech.setRecognitionListener(Listener(onResult))
        heard = ""
        notice = null
        listening = true
        speech.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName),
        )
    }

    fun stop() {
        if (!listening) return
        recognizer?.stopListening()
    }

    fun cancel() {
        recognizer?.cancel()
        finish()
    }

    fun refused() {
        notice = R.string.voice_permission
    }

    fun clearNotice() {
        notice = null
    }

    fun release() {
        recognizer?.destroy()
        recognizer = null
        finish()
    }

    private fun finish() {
        listening = false
        heard = ""
        level.floatValue = 0f
    }

    private inner class Listener(private val onResult: (String) -> Unit) : RecognitionListener {
        override fun onPartialResults(partialResults: Bundle?) {
            firstLine(partialResults)?.let { heard = it }
        }

        override fun onResults(results: Bundle?) {
            val sentence = firstLine(results)
            finish()
            if (sentence.isNullOrBlank()) notice = R.string.voice_not_heard else onResult(sentence)
        }

        override fun onError(error: Int) {
            finish()
            notice = when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.voice_permission
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> R.string.voice_network
                SpeechRecognizer.ERROR_CLIENT -> null
                else -> R.string.voice_not_heard
            }
        }

        // The service reports roughly -2 to 10 dB; the pulse only needs a soft 0 to 1.
        override fun onRmsChanged(rmsdB: Float) {
            level.floatValue = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun firstLine(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
}

@Composable
fun rememberVoiceSearch(): VoiceSearch {
    val context = LocalContext.current
    val voice = remember { VoiceSearch(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { voice.cancel() }
    DisposableEffect(voice) { onDispose { voice.release() } }
    return voice
}
