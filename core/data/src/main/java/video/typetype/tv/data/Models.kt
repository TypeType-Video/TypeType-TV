package video.typetype.tv.data

import androidx.annotation.StringRes

data class Channel(
    val id: String,
    val name: String,
    val handle: String,
    val subscribers: String,
    val url: String = "",
    val avatarUrl: String? = null,
)

data class Chapter(
    val title: String,
    val startSeconds: Int,
    val thumbnailUrl: String? = null,
)

data class Video(
    val id: String,
    val title: String,
    val channel: Channel,
    val durationSeconds: Int,
    val views: String,
    val age: String,
    val genre: String,
    val description: String,
    val resumeFraction: Float? = null,
    val isLive: Boolean = false,
    val quality: String = "",
    val chapters: List<Chapter> = emptyList(),
    val focusY: Float = 0.5f,
    val thumbnailUrl: String? = null,
) {
    val source: ContentSource? get() = ContentSource.of(id)
    val isShort: Boolean get() = "/shorts/" in id
}

data class Shelf(
    val id: String,
    val title: String,
    val videos: List<Video>,
    val wide: Boolean = false,
)

data class HomeContent(
    val featured: List<Video>,
    val shelves: List<Shelf>,
)

sealed interface LibrarySection {
    val id: String

    data object History : LibrarySection {
        override val id = "history"
    }

    data object Favorites : LibrarySection {
        override val id = "favorites"
    }

    data object WatchLater : LibrarySection {
        override val id = "watch-later"
    }

    data object Downloads : LibrarySection {
        override val id = "downloads"
    }

    data class Saved(
        override val id: String,
        val url: String,
        val name: String,
        val count: Int,
    ) : LibrarySection

    data class Playlist(
        override val id: String,
        val name: String,
        val count: Int,
    ) : LibrarySection
}

sealed interface SettingItem {
    val id: String

    data class Blocked(
        override val id: String,
        val name: String,
    ) : SettingItem

    data class Toggle(
        override val id: String,
        @StringRes val titleRes: Int,
        @StringRes val summaryRes: Int,
        val defaultValue: Boolean,
    ) : SettingItem

    data class Choice(
        override val id: String,
        @StringRes val titleRes: Int,
        val options: List<String>,
        val defaultIndex: Int,
        val wireValues: List<String> = options,
        @StringRes val summaryRes: Int? = null,
    ) : SettingItem

    data class Info(
        override val id: String,
        @StringRes val titleRes: Int,
        val value: String,
    ) : SettingItem

    data class Action(
        override val id: String,
        @StringRes val titleRes: Int,
        @StringRes val summaryRes: Int,
    ) : SettingItem
}

data class BlockedChannel(
    val url: String,
    val name: String,
)

data class SettingsGroup(
    val id: String,
    @StringRes val titleRes: Int,
    val items: List<SettingItem>,
)

data class VideoActivity(
    val watchLater: Boolean = false,
    val favorite: Boolean = false,
    val subscribed: Boolean = false,
    val resumePositionMs: Long = 0L,
)

fun Video.withResume(positionMs: Long): Video {
    if (durationSeconds <= 0 || positionMs < RESUME_MINIMUM_MS) return this
    val fraction = (positionMs / MILLIS_PER_SECOND.toFloat() / durationSeconds).coerceIn(0f, 1f)
    return if (fraction >= RESUME_FINISHED_FRACTION) this else copy(resumeFraction = fraction)
}

private const val RESUME_MINIMUM_MS = 5_000L
private const val MILLIS_PER_SECOND = 1_000
private const val RESUME_FINISHED_FRACTION = 0.97f

data class PlaylistChoice(
    val id: String,
    val name: String,
    val count: Int,
    val contains: Boolean,
)
