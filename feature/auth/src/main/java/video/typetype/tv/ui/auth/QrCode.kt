package video.typetype.tv.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import video.typetype.tv.theme.Shapes

private class QrModules(val count: Int, val path: Path)

private fun encode(content: String): QrModules {
    val hints = mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
    val path = Path()
    for (y in 0 until matrix.height) {
        for (x in 0 until matrix.width) {
            if (matrix[x, y]) path.addRect(Rect(x.toFloat(), y.toFloat(), x + 1f, y + 1f))
        }
    }
    return QrModules(matrix.width, path)
}

@Composable
fun QrCode(content: String, description: String, size: Dp, modifier: Modifier = Modifier) {
    val modules = remember(content) { encode(content) }
    Box(
        modifier
            .size(size)
            .clip(Shapes.Panel)
            .background(Color.White)
            .padding(size / 14)
            .semantics { contentDescription = description },
    ) {
        Canvas(Modifier.size(size - size / 7)) {
            scale(this.size.width / modules.count, pivot = Offset.Zero) {
                drawPath(modules.path, Color.Black)
            }
        }
    }
}
