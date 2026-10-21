package video.typetype.tv.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import video.typetype.tv.data.SponsorSegment

@Stable
class SponsorNotice {
    var segment by mutableStateOf<SponsorSegment?>(null)
        private set

    private val bypassedStarts = mutableSetOf<Long>()

    fun isBypassed(candidate: SponsorSegment): Boolean = candidate.startMs in bypassedStarts

    fun skipped(skippedSegment: SponsorSegment) {
        segment = skippedSegment
    }

    fun undo(): Long? {
        val current = segment ?: return null
        bypassedStarts += current.startMs
        segment = null
        return current.startMs
    }

    fun dismiss() {
        segment = null
    }
}
