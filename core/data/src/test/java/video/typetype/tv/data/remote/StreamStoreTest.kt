package video.typetype.tv.data.remote

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.tv.data.dto.StreamDto
import java.util.concurrent.atomic.AtomicInteger

class StreamStoreTest {
    @Test
    fun concurrentCallersShareOneLoad() = runBlocking {
        val calls = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val store = StreamStore({ url ->
            calls.incrementAndGet()
            gate.await()
            StreamDto(id = url)
        })
        val first = async { store.get("a") }
        val second = async { store.get("a") }
        gate.complete(Unit)
        assertEquals("a", first.await().id)
        assertEquals("a", second.await().id)
        assertEquals(1, calls.get())
    }

    @Test
    fun aLoadedStreamIsServedFromTheCache() = runBlocking {
        val calls = AtomicInteger()
        val store = StreamStore({ url -> calls.incrementAndGet(); StreamDto(id = url) })
        store.get("a")
        store.get("a")
        assertEquals(1, calls.get())
    }

    @Test
    fun aLoadSurvivesTheCallerThatStartedIt() = runBlocking {
        val calls = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val store = StreamStore({ url ->
            calls.incrementAndGet()
            gate.await()
            StreamDto(id = url)
        })
        val abandoned = async { store.get("a") }
        while (calls.get() == 0) kotlinx.coroutines.yield()
        abandoned.cancel()
        gate.complete(Unit)
        assertEquals("a", store.get("a").id)
        assertEquals(1, calls.get())
    }
}
