package video.typetype.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import video.typetype.tv.ui.AppEntry
import video.typetype.tv.ui.util.ReferenceCanvas

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as TypeTypeApp).container
        setContent { ReferenceCanvas { AppEntry(container) } }
        // Every screen paints its own background; the theme one only covers the start and would be filled again each frame.
        window.setBackgroundDrawable(null)
    }
}
