package video.typetype.tv.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateOffersTest {
    private fun version(text: String) = requireNotNull(AppVersion.parse(text))

    private fun release(tag: String) = AppRelease(version(tag), tag, "notes $tag", "apk $tag", "manifest $tag")

    private val published = listOf("v0.1.0", "v0.1.1-beta.1", "v0.1.1-beta.2", "v0.1.1", "v0.2.0-beta.1").map(::release)

    @Test
    fun aBetaComesBeforeTheStableReleaseOfTheSameNumber() {
        val ordered = listOf("0.1.1", "0.1.1-beta.2", "0.1.0", "0.1.1-beta.10", "0.10.0").map(::version).sorted()
        assertEquals(listOf("0.1.0", "0.1.1-beta.2", "0.1.1-beta.10", "0.1.1", "0.10.0"), ordered.map { it.toString() })
    }

    @Test
    fun onlyTheFormatsOfTheReleaseWorkflowAreRead() {
        assertEquals(AppVersion(1, 2, 3, 4), AppVersion.parse("v1.2.3-beta.4"))
        assertNull(AppVersion.parse("1.2"))
        assertNull(AppVersion.parse("1.2.3-rc.1"))
        assertNull(AppVersion.parse("99999999999.0.0"))
    }

    @Test
    fun theStableChannelSkipsBetas() {
        val offer = UpdateOffers.pick(version("0.1.0"), UpdateChannel.Stable, published)
        assertEquals("v0.1.1", offer?.target?.tag)
        assertEquals(listOf("v0.1.1"), offer?.releases?.map { it.tag })
    }

    @Test
    fun theBetaChannelTakesTheNewestAndListsEveryNewerRelease() {
        val offer = UpdateOffers.pick(version("0.1.1-beta.1"), UpdateChannel.Beta, published)
        assertEquals("v0.2.0-beta.1", offer?.target?.tag)
        assertEquals(listOf("v0.2.0-beta.1", "v0.1.1", "v0.1.1-beta.2"), offer?.releases?.map { it.tag })
    }

    @Test
    fun aBetaBoxOnTheStableChannelWaitsForTheNextStableRelease() {
        assertEquals("v0.1.1", UpdateOffers.pick(version("0.1.1-beta.2"), UpdateChannel.Stable, published)?.target?.tag)
        assertNull(UpdateOffers.pick(version("0.2.0-beta.1"), UpdateChannel.Stable, published))
    }

    @Test
    fun nothingIsOfferedToTheNewestVersion() {
        assertNull(UpdateOffers.pick(version("0.2.0-beta.1"), UpdateChannel.Beta, published))
        assertTrue(UpdateOffers.pick(version("0.1.1"), UpdateChannel.Stable, emptyList()) == null)
    }
}
