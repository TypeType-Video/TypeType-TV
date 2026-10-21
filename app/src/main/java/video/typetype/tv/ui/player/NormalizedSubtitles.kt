package video.typetype.tv.ui.player

import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@UnstableApi
@androidx.annotation.OptIn(UnstableApi::class)
object CueNormalizer {
    fun normalize(cues: List<Cue>): List<Cue> = cues.map { cue ->
        if (cue.text == null) {
            cue
        } else {
            cue.buildUpon()
                .setLine(Cue.DIMEN_UNSET, Cue.TYPE_UNSET)
                .setLineAnchor(Cue.TYPE_UNSET)
                .setPosition(Cue.DIMEN_UNSET)
                .setPositionAnchor(Cue.TYPE_UNSET)
                .setSize(Cue.DIMEN_UNSET)
                .setVerticalType(Cue.TYPE_UNSET)
                .build()
        }
    }
}

@UnstableApi
@androidx.annotation.OptIn(UnstableApi::class)
class NormalizedSubtitles(private val player: Player, private val view: PlayerView) : Player.Listener {
    fun attach() {
        player.addListener(this)
    }

    fun detach() {
        player.removeListener(this)
    }

    override fun onCues(cueGroup: CueGroup) {
        view.subtitleView?.setCues(CueNormalizer.normalize(cueGroup.cues))
    }
}
