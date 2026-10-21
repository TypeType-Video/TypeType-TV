package video.typetype.tv.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object InstanceUrls {
    fun resolve(apiBase: HttpUrl, reference: String?): String? {
        val trimmed = reference?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        trimmed.toHttpUrlOrNull()?.let { return it.toString() }
        if (!trimmed.startsWith("/")) return null
        val prefix = apiBase.encodedPath.trimEnd('/')
        val path = trimmed.substringBefore('?').substringBefore('#')
        val query = trimmed.substringAfter('?', "").substringBefore('#')
        return apiBase.newBuilder()
            .encodedPath(prefix + path)
            .encodedQuery(query.ifEmpty { null })
            .fragment(null)
            .build()
            .toString()
    }
}
