package video.typetype.tv.data.net

import kotlinx.serialization.json.Json

object ApiJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }
}
