package video.typetype.tv.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Shapes
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.components.SkeletonBox

@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading)
    Box(modifier.fillMaxSize().semantics { contentDescription = description }) {
        InkBackground()
        Column(
            modifier = Modifier.padding(
                start = Dimens.SafeStart,
                top = Dimens.TopNavHeight + Dimens.SafeTop + 38.dp,
            ),
        ) {
            SkeletonBox(Modifier.size(width = 88.dp, height = 10.dp), RoundedCornerShape(5.dp))
            Spacer(Modifier.height(14.dp))
            SkeletonBox(Modifier.size(width = 360.dp, height = 36.dp), RoundedCornerShape(8.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.size(width = 250.dp, height = 36.dp), RoundedCornerShape(8.dp))
            Spacer(Modifier.height(16.dp))
            SkeletonBox(Modifier.size(width = 430.dp, height = 12.dp), RoundedCornerShape(6.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.size(width = 390.dp, height = 12.dp), RoundedCornerShape(6.dp))
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBox(Modifier.size(width = 112.dp, height = 40.dp), Shapes.Button)
                SkeletonBox(Modifier.size(width = 132.dp, height = 40.dp), Shapes.Button)
            }
            Spacer(Modifier.height(34.dp))
            SkeletonBox(Modifier.size(width = 150.dp, height = 10.dp), RoundedCornerShape(5.dp))
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                repeat(5) {
                    Column {
                        SkeletonBox(Modifier.size(width = Dimens.CardWidth, height = 110.dp))
                        Spacer(Modifier.height(10.dp))
                        SkeletonBox(Modifier.width(150.dp).height(12.dp), RoundedCornerShape(6.dp))
                        Spacer(Modifier.height(6.dp))
                        SkeletonBox(Modifier.width(100.dp).height(10.dp), RoundedCornerShape(5.dp))
                    }
                }
            }
        }
    }
}
