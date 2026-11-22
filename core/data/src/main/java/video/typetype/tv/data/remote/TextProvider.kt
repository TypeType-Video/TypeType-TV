package video.typetype.tv.data.remote

import android.content.Context
import androidx.annotation.StringRes

interface TextProvider {
    fun text(@StringRes id: Int): String

    fun text(@StringRes id: Int, vararg args: Any): String = text(id).format(*args)
}

class AndroidTextProvider(private val context: Context) : TextProvider {
    override fun text(id: Int): String = context.getString(id)

    override fun text(id: Int, vararg args: Any): String = context.getString(id, *args)
}
