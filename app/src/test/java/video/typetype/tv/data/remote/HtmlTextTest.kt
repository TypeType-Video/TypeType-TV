package video.typetype.tv.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.data.dto.CommentDto

class HtmlTextTest {
    @Test
    fun linksAndTagsAreStrippedButTheirTextIsKept() {
        assertEquals("see 10:25 now", HtmlText.plain("see <a href=\"https://x.example/?a=1&amp;t=625\">10:25</a> now"))
    }

    @Test
    fun lineBreaksBecomeNewlines() {
        assertEquals("one\ntwo\nthree", HtmlText.plain("one<br>two<br/>three"))
    }

    @Test
    fun entitiesAreDecoded() {
        assertEquals("\"Tom & Jerry\" <3", HtmlText.plain("&quot;Tom &amp; Jerry&quot; &lt;3"))
        assertEquals("it's", HtmlText.plain("it&#39;s"))
        assertEquals("\u00e9\u2764", HtmlText.plain("&#233;&#x2764;"))
        assertEquals("a b", HtmlText.plain("a&nbsp;b"))
    }

    @Test
    fun brokenNumericEntitiesAreLeftAlone() {
        assertEquals("&#99999999999;", HtmlText.plain("&#99999999999;"))
    }

    @Test
    fun commentsWithoutAnIdentifierOrBodyAreDropped() {
        assertNull(CommentDto(id = "", text = "hello").toComment())
        assertNull(CommentDto(id = "1", text = "<br>").toComment())
    }

    @Test
    fun aCommentKeepsItsMetadata() {
        val comment = CommentDto(
            id = "1", text = "Nice <b>one</b>", author = "@Someone", textualLikeCount = "12",
            publishedTime = "2 days ago", isPinned = true, replyCount = 3, authorAvatarUrl = "https://a.example/x.jpg",
        ).toComment()
        assertEquals("Someone", comment?.author)
        assertEquals("Nice one", comment?.text)
        assertEquals("12", comment?.likes)
        assertEquals(3, comment?.replies)
        assertEquals(true, comment?.pinned)
    }

    @Test
    fun zeroLikesAreHidden() {
        assertEquals("", CommentDto(id = "1", text = "x", textualLikeCount = "0").toComment()?.likes)
    }

    @Test
    fun theAgeComesFromTheTimestampWhenPresent() {
        val now = 1_800_000_000_000L
        val comment = CommentDto(id = "1", text = "x", publishedTime = "emahoreni angu-8 edlule", publishedAt = now - 3 * 86_400_000L).toComment(now)
        assertEquals(Formatters.age(now, now - 3 * 86_400_000L), comment?.published)
        assertEquals("fallback", CommentDto(id = "1", text = "x", publishedTime = "fallback").toComment(now)?.published)
    }
}
