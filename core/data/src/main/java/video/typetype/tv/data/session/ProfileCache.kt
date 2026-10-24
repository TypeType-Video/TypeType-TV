package video.typetype.tv.data.session

import video.typetype.tv.data.dto.UserProfileDto
import video.typetype.tv.data.net.ApiJson
import video.typetype.tv.data.net.SecretStore

class ProfileCache(private val store: SecretStore) {
    fun get(): UserProfileDto? = store.get(KEY)?.let { raw ->
        runCatching { ApiJson.instance.decodeFromString(UserProfileDto.serializer(), raw) }.getOrNull()
    }

    fun put(profile: UserProfileDto) {
        store.put(KEY, ApiJson.instance.encodeToString(UserProfileDto.serializer(), profile))
    }

    fun clear() {
        store.remove(KEY)
    }

    private companion object {
        const val KEY = "profile"
    }
}
