package video.typetype.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Shapes

private const val SHELF_SKELETON_CARDS = 5

/** A card still loading, sized like a [VideoCard] at the same width. */
@Composable
fun CardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier) {
        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Spacer(Modifier.height(10.dp))
        SkeletonBox(Modifier.size(width = 150.dp, height = 12.dp), Shapes.Chip)
        Spacer(Modifier.height(6.dp))
        SkeletonBox(Modifier.size(width = 96.dp, height = 10.dp), Shapes.Chip)
        Spacer(Modifier.height(6.dp))
        SkeletonBox(Modifier.size(width = 120.dp, height = 10.dp), Shapes.Chip)
    }
}

/** A whole shelf still loading, laid out like a [ShelfRow]. It takes no focus. */
@Composable
fun ShelfSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading)
    Column(modifier.semantics { contentDescription = description }) {
        SkeletonBox(Modifier.padding(start = Dimens.SafeStart).size(width = 150.dp, height = 10.dp), Shapes.Chip)
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.padding(start = Dimens.SafeStart, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        ) {
            repeat(SHELF_SKELETON_CARDS) { CardSkeleton(Modifier.width(Dimens.CardWidth)) }
        }
    }
}
