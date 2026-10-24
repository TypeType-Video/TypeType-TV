package video.typetype.tv.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import video.typetype.tv.data.net.ApiJson
import java.io.IOException

// What the release workflow attaches to each release next to the APK.
@Serializable
data class UpdateManifest(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val apkAsset: String,
    val sha256: String,
)

@Serializable
private data class ReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val draft: Boolean = false,
    val body: String? = null,
    val assets: List<AssetDto> = emptyList(),
)

@Serializable
private data class AssetDto(val name: String, @SerialName("browser_download_url") val url: String)

interface ReleaseSource {
    suspend fun releases(): List<AppRelease>

    suspend fun manifest(release: AppRelease): UpdateManifest
}

// The releases of the app on GitHub. One call lists them with their notes, so the update panel can say what every
// version since the installed one brings; GitHub allows 60 such calls an hour to a network without an account.
class ReleaseFeed(
    private val client: OkHttpClient,
    private val api: HttpUrl = GITHUB_API.toHttpUrl(),
    private val repository: String = REPOSITORY,
) : ReleaseSource {
    override suspend fun releases(): List<AppRelease> {
        val url = api.newBuilder()
            .addPathSegments("repos/$repository/releases")
            .addQueryParameter("per_page", PAGE_SIZE.toString())
            .build()
        return get(url, ListSerializer(ReleaseDto.serializer())).mapNotNull { it.toRelease() }
    }

    override suspend fun manifest(release: AppRelease): UpdateManifest = get(release.manifestUrl.toHttpUrl(), UpdateManifest.serializer())

    private suspend fun <T> get(url: HttpUrl, serializer: KSerializer<T>): T = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("Accept", "application/vnd.github+json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Release check answered HTTP ${response.code}")
            try {
                ApiJson.instance.decodeFromString(serializer, response.body.string())
            } catch (error: SerializationException) {
                throw IOException("Release check answered an unreadable list", error)
            }
        }
    }

    // A release is only offered once its APK and manifest are both there: the workflow uploads them together.
    private fun ReleaseDto.toRelease(): AppRelease? {
        if (draft) return null
        val version = AppVersion.parse(tagName) ?: return null
        val files = assets.associate { it.name to it.url }
        return AppRelease(
            version = version,
            tag = tagName,
            notes = body.orEmpty(),
            apkUrl = files["TypeType-TV-$tagName.apk"] ?: return null,
            manifestUrl = files[MANIFEST] ?: return null,
        )
    }

    companion object {
        const val GITHUB_API = "https://api.github.com/"
        const val REPOSITORY = "TypeType-Video/TypeType-TV"
        const val RELEASES_PAGE = "https://github.com/$REPOSITORY/releases"
        private const val MANIFEST = "typetype-update.json"
        private const val PAGE_SIZE = 30
    }
}
