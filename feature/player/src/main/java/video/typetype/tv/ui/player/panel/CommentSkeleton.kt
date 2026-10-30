package video.typetype.tv.ui.player.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.ui.components.Shimmer
import video.typetype.tv.ui.components.rememberShimmer
import video.typetype.tv.ui.components.shimmer

private val LINE = RoundedCornerShape(6.dp)

@Composable
fun rememberCommentShimmer(): Shimmer = rememberShimmer(base = Palette.SurfaceRaised, highlight = Palette.Outline)

@Composable
fun CommentSkeleton(shimmer: Shimmer, reply: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = if (reply) 28.dp else 0.dp)
            .background(Palette.SurfaceOnInk, Shapes.Panel)
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(if (reply) 26.dp else 32.dp).shimmer(shimmer, CircleShape))
            Spacer(Modifier.fillMaxWidth(0.36f).height(14.dp).shimmer(shimmer, LINE))
        }
        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.fillMaxWidth().height(12.dp).shimmer(shimmer, LINE))
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.fillMaxWidth(0.7f).height(12.dp).shimmer(shimmer, LINE))
    }
}
