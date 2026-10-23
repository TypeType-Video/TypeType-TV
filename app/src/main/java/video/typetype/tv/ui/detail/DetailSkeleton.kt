package video.typetype.tv.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Shapes
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.components.SkeletonBox

@Composable
fun DetailSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading)
    Box(modifier.fillMaxSize().semantics { contentDescription = description }) {
        InkBackground()
        Column(modifier = Modifier.padding(start = Dimens.SafeStart, top = Dimens.SafeTop + 34.dp)) {
            SkeletonBox(Modifier.size(width = 80.dp, height = 10.dp), RoundedCornerShape(5.dp))
            Spacer(Modifier.height(14.dp))
            SkeletonBox(Modifier.size(width = 420.dp, height = 36.dp), RoundedCornerShape(8.dp))
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SkeletonBox(Modifier.size(32.dp), CircleShape)
                SkeletonBox(Modifier.size(width = 140.dp, height = 14.dp), RoundedCornerShape(7.dp))
            }
            Spacer(Modifier.height(18.dp))
            SkeletonBox(Modifier.size(width = 480.dp, height = 12.dp), RoundedCornerShape(6.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.size(width = 440.dp, height = 12.dp), RoundedCornerShape(6.dp))
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) { SkeletonBox(Modifier.size(width = 118.dp, height = 40.dp), Shapes.Button) }
            }
        }
    }
}

@Composable
fun DescriptionSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SkeletonBox(Modifier.size(width = 480.dp, height = 12.dp), RoundedCornerShape(6.dp))
        SkeletonBox(Modifier.size(width = 440.dp, height = 12.dp), RoundedCornerShape(6.dp))
    }
}

@Composable
fun RelatedSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = Dimens.SafeStart),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        repeat(5) {
            Column {
                SkeletonBox(Modifier.size(width = Dimens.CardWidth, height = 110.dp))
                Spacer(Modifier.height(10.dp))
                SkeletonBox(Modifier.width(150.dp).height(12.dp), RoundedCornerShape(6.dp))
            }
        }
    }
}
