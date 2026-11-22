package video.typetype.tv.data.device

class MemoryDeviceStore : DeviceStore {
    private val values = mutableMapOf<String, Any>()

    override fun flag(key: String, fallback: Boolean) = values[key] as? Boolean ?: fallback

    override fun putFlag(key: String, value: Boolean) {
        values[key] = value
    }

    override fun text(key: String, fallback: String) = values[key] as? String ?: fallback

    override fun putText(key: String, value: String) {
        values[key] = value
    }
}
