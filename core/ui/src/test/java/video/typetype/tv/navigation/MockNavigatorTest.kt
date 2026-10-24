package video.typetype.tv.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockNavigatorTest {
    @Test
    fun `starts on home with nothing to pop`() {
        val navigator = MockNavigator()
        assertEquals(Route.Home, navigator.current)
        assertFalse(navigator.canPop())
    }

    @Test
    fun `push and pop walk the stack`() {
        val navigator = MockNavigator()
        navigator.push(Route.Detail("a"))
        navigator.push(Route.Player("a"))
        assertEquals(Route.Player("a"), navigator.current)
        navigator.pop()
        assertEquals(Route.Detail("a"), navigator.current)
        navigator.pop()
        assertEquals(Route.Home, navigator.current)
    }

    @Test
    fun `selecting a tab resets the stack`() {
        val navigator = MockNavigator()
        navigator.push(Route.Detail("a"))
        navigator.selectTab(Route.Library)
        assertEquals(1, navigator.stack.size)
        assertEquals(Route.Library, navigator.current)
    }

    @Test
    fun `back from another tab goes home`() {
        val navigator = MockNavigator()
        navigator.selectTab(Route.Settings)
        assertTrue(navigator.canPop())
        navigator.pop()
        assertEquals(Route.Home, navigator.current)
        assertFalse(navigator.canPop())
    }

    @Test
    fun `replace swaps only the top route`() {
        val navigator = MockNavigator()
        navigator.push(Route.Player("a"))
        navigator.replaceTop(Route.Player("b"))
        assertEquals(2, navigator.stack.size)
        assertEquals(Route.Player("b"), navigator.current)
    }

    @Test
    fun `only top level routes map to a tab`() {
        assertEquals(TopTab.Search, Route.Search.topTab())
        assertNull(Route.Detail("a").topTab())
        assertNull(Route.Player("a").topTab())
    }
}
