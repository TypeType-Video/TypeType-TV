package video.typetype.tv.data.device

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import video.typetype.tv.data.ContentSource

// The services the home shows, chosen from the home itself and kept on this TV. One of them always stays: a home
// with none would have nothing to load.
class HomeServices(private val store: DeviceStore) {
    private val state = MutableStateFlow(read())
    val current: StateFlow<Set<ContentSource>> = state.asStateFlow()

    // Returns false when the toggle would leave no service, and keeps the choice as it is.
    fun toggle(source: ContentSource): Boolean {
        if (source !in ContentSource.extractors) return false
        val next = if (source in state.value) state.value - source else state.value + source
        if (next.isEmpty()) return false
        store.putText(KEY, ContentSource.extractors.filter { it in next }.joinToString(SEPARATOR) { it.name })
        state.value = next
        return true
    }

    private fun read(): Set<ContentSource> {
        val saved = store.text(KEY, "").split(SEPARATOR).mapNotNull { name ->
            ContentSource.extractors.firstOrNull { it.name == name }
        }.toSet()
        return saved.ifEmpty { ContentSource.extractors.toSet() }
    }

    private companion object {
        const val KEY = "home-services"
        const val SEPARATOR = ","
    }
}
