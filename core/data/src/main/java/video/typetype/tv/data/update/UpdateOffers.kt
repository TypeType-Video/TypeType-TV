package video.typetype.tv.data.update

enum class UpdateChannel(val wire: String) { Stable("stable"), Beta("beta") }

// One published version of the app, with what its release page says and the files it carries.
data class AppRelease(
    val version: AppVersion,
    val tag: String,
    val notes: String,
    val apkUrl: String,
    val manifestUrl: String,
)

// The release to install, and every release newer than the installed one, newest first, whose notes say what changes.
data class UpdateOffer(val target: AppRelease, val releases: List<AppRelease>)

object UpdateOffers {
    // The stable channel only ever offers stable releases. The beta channel offers whichever is newest, so a beta box
    // also moves on to the stable release that ends a beta.
    fun pick(installed: AppVersion, channel: UpdateChannel, releases: List<AppRelease>): UpdateOffer? {
        val newer = releases
            .filter { it.version > installed && (channel == UpdateChannel.Beta || !it.version.isBeta) }
            .sortedByDescending { it.version }
        return newer.firstOrNull()?.let { UpdateOffer(it, newer) }
    }
}
