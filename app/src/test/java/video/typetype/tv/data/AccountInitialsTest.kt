package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountInitialsTest {
    @Test
    fun twoWordsUseTheirFirstLetters() = assertEquals("TT", AccountInitials.of("TV Test"))

    @Test
    fun oneWordUsesItsFirstTwoLetters() = assertEquals("PR", AccountInitials.of("priveetee"))

    @Test
    fun extraWordsAreIgnored() = assertEquals("AB", AccountInitials.of("  Ada  Byron Lovelace "))

    @Test
    fun guestsAndMissingNamesHaveStableMarkers() {
        assertEquals("G", AccountInitials.of("Someone", guest = true))
        assertEquals("?", AccountInitials.of(null))
        assertEquals("?", AccountInitials.of("   "))
    }
}
