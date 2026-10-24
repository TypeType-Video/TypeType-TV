package video.typetype.tv.data.device

import android.content.Context

// Choices kept on this box only, whatever account is signed in.
interface DeviceStore {
    fun flag(key: String, fallback: Boolean): Boolean

    fun putFlag(key: String, value: Boolean)

    fun text(key: String, fallback: String): String

    fun putText(key: String, value: String)
}

class PreferencesDeviceStore(context: Context, name: String = PERFORMANCE) : DeviceStore {
    private val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun flag(key: String, fallback: Boolean) = preferences.getBoolean(key, fallback)

    override fun putFlag(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()

    override fun text(key: String, fallback: String) = preferences.getString(key, fallback) ?: fallback

    override fun putText(key: String, value: String) = preferences.edit().putString(key, value).apply()

    companion object {
        const val PERFORMANCE = "performance"
        const val DECODERS = "decoders"
        const val UPDATES = "updates"
    }
}
