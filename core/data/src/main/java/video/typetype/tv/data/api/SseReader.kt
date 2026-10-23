package video.typetype.tv.data.api

import okio.BufferedSource

data class SseEvent(val name: String, val data: String)

// Server-sent events: "event:" and "data:" lines, a blank line ends an event, ":" starts a comment
// (the server's keep-alive).
class SseReader(private val source: BufferedSource) {
    fun next(): SseEvent? {
        var name = DEFAULT_EVENT
        val data = StringBuilder()
        while (true) {
            val line = source.readUtf8Line() ?: return null
            when {
                line.isEmpty() -> if (data.isNotEmpty()) return SseEvent(name, data.toString()) else name = DEFAULT_EVENT
                line.startsWith(":") -> Unit
                line.startsWith("event:") -> name = line.removePrefix("event:").trim()
                line.startsWith("data:") -> {
                    if (data.isNotEmpty()) data.append('\n')
                    data.append(line.removePrefix("data:").removePrefix(" "))
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_EVENT = "message"
    }
}
