package video.typetype.tv.data

object SettingActions {
    private const val UNBLOCK_PREFIX = "unblock:"

    fun unblockId(channelUrl: String): String = "$UNBLOCK_PREFIX$channelUrl"

    fun unblockedChannel(actionId: String): String? = actionId.removePrefix(UNBLOCK_PREFIX).takeIf { actionId.startsWith(UNBLOCK_PREFIX) }
}
