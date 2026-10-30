package video.typetype.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Shapes

@Composable
fun VideoGridSkeleton(modifier: Modifier = Modifier, header: Boolean = true) {
    val description = stringResource(R.string.loading)
    Column(modifier.fillMaxSize().semantics { contentDescription = description }) {
        if (header) {
            SkeletonBox(Modifier.size(width = 80.dp, height = 10.dp), Shapes.Chip)
            Spacer(Modifier.height(20.dp))
        }
        repeat(2) {
            Row(Modifier.padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                repeat(3) {
                    Column(Modifier.weight(1f)) {
                        SkeletonBox(Modifier.fillMaxWidth().height(118.dp))
                        Spacer(Modifier.height(10.dp))
                        SkeletonBox(Modifier.size(width = 150.dp, height = 12.dp), Shapes.Chip)
                    }
                }
            }
        }
    }
}
