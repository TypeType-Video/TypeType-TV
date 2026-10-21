package video.typetype.tv.data.remote

class TtlCache<K : Any, V : Any>(
    private val ttlMillis: Long,
    private val maxEntries: Int,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private class Entry<V>(val value: V, val storedAt: Long)

    private val entries = LinkedHashMap<K, Entry<V>>()

    @Synchronized
    fun fresh(key: K): V? = entries[key]?.takeIf { now() - it.storedAt <= ttlMillis }?.value

    @Synchronized
    fun stale(key: K): V? = entries[key]?.value

    @Synchronized
    fun put(key: K, value: V) {
        entries.remove(key)
        entries[key] = Entry(value, now())
        while (entries.size > maxEntries) entries.remove(entries.keys.first())
    }

    @Synchronized
    fun clear() = entries.clear()
}
