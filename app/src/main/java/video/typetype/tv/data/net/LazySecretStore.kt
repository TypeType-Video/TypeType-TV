package video.typetype.tv.data.net

class LazySecretStore(create: () -> SecretStore) : SecretStore {
    private val delegate = lazy(create)

    fun warmUp() {
        delegate.value
    }

    override fun get(key: String): String? = delegate.value.get(key)

    override fun put(key: String, value: String) = delegate.value.put(key, value)

    override fun remove(key: String) = delegate.value.remove(key)
}
