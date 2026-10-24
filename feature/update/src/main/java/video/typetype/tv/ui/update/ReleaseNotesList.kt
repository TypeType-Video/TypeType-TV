package video.typetype.tv.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.update.AppRelease
import video.typetype.tv.data.update.NoteBlock
import video.typetype.tv.data.update.ReleaseNotes
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel

// The notes of every release the update brings, newest first. Each block takes the focus, so Up and Down scroll
// through notes longer than the panel.
@Composable
internal fun ReleaseNotesList(releases: List<AppRelease>, modifier: Modifier = Modifier) {
    val parsed = remember(releases) { releases.map { it to ReleaseNotes.parse(it.notes) } }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextLabel(stringResource(R.string.update_whats_new).uppercase(), Type.Label, color = { Palette.TextLow })
        parsed.forEach { (release, blocks) ->
            NoteLine(release.version.toString(), Type.Title, Palette.AccentSoft)
            blocks.forEach { block ->
                when (block) {
                    is NoteBlock.Heading -> NoteLine(block.text, Type.Body, Palette.TextHigh)
                    is NoteBlock.Item -> NoteLine(block.text, Type.Body, Palette.TextMid, bullet = true)
                    is NoteBlock.Paragraph -> NoteLine(block.text, Type.Body, Palette.TextMid)
                }
            }
        }
    }
}

@Composable
private fun NoteLine(text: String, style: TextStyle, color: Color, bullet: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.Card)
            .background(if (focused) Palette.SurfaceRaised else Color.Transparent)
            .focusable(interactionSource = source)
            .semantics { contentDescription = text }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (bullet) TextLabel("•", style, color = { color })
        TextLabel(text, style, color = { color })
    }
}
