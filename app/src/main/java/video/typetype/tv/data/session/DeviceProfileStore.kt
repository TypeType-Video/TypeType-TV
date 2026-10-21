package video.typetype.tv.data.session

import android.content.Context
import android.os.Build
import video.typetype.tv.data.DeviceProfile
import java.util.UUID

class DeviceProfileStore(context: Context, private val clientVersion: String) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun profile(): DeviceProfile = DeviceProfile(identifier(), deviceName(), clientVersion)

    private fun identifier(): String =
        preferences.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(KEY_ID, it).apply()
        }

    private fun deviceName(): String =
        listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { DEFAULT_NAME }

    private companion object {
        const val PREFERENCES = "device_profile"
        const val KEY_ID = "device_id"
        const val DEFAULT_NAME = "Android TV"
    }
}
