package video.typetype.tv.data.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface Connectivity {
    val online: StateFlow<Boolean>
}

object AlwaysOnline : Connectivity {
    override val online: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
}

class AndroidConnectivity(context: Context) : Connectivity {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val networks = HashSet<Network>()
    private val state = MutableStateFlow(manager?.let(::hasInternet) ?: true)

    override val online: StateFlow<Boolean> = state.asStateFlow()

    init {
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        runCatching {
            manager?.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = update { networks.add(network) }

                    override fun onLost(network: Network) = update { networks.remove(network) }
                },
            )
        }
    }

    private fun update(change: () -> Unit) {
        synchronized(networks) {
            change()
            state.value = networks.isNotEmpty()
        }
    }

    private fun hasInternet(manager: ConnectivityManager): Boolean =
        manager.activeNetwork?.let { manager.getNetworkCapabilities(it) }
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
}
