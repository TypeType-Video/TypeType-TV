package video.typetype.tv.data.remote

import android.content.Context
import androidx.annotation.StringRes

interface TextProvider {
    fun text(@StringRes id: Int): String
}

class AndroidTextProvider(private val context: Context) : TextProvider {
    override fun text(id: Int): String = context.getString(id)
}
