package video.typetype.tv.data.net

interface SecretStore {
    fun get(key: String): String?

    fun put(key: String, value: String)

    fun remove(key: String)
}

class InMemorySecretStore : SecretStore {
    private val values = mutableMapOf<String, String>()

    @Synchronized
    override fun get(key: String): String? = values[key]

    @Synchronized
    override fun put(key: String, value: String) {
        values[key] = value
    }

    @Synchronized
    override fun remove(key: String) {
        values.remove(key)
    }
}
