package video.typetype.tv.data.net

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.GeneralSecurityException

class TinkSecretStore(context: Context) : SecretStore {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val aead: Aead = createAead(context)

    override fun get(key: String): String? {
        val stored = preferences.getString(key, null) ?: return null
        return try {
            val plain = aead.decrypt(Base64.decode(stored, Base64.NO_WRAP), key.toByteArray())
            String(plain)
        } catch (_: GeneralSecurityException) {
            preferences.edit().remove(key).apply()
            null
        }
    }

    override fun put(key: String, value: String) {
        val sealed = aead.encrypt(value.toByteArray(), key.toByteArray())
        preferences.edit().putString(key, Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    private companion object {
        const val PREFERENCES = "typetype_secrets"

        fun createAead(context: Context): Aead {
            AeadConfig.register()
            val handle = AndroidKeysetManager.Builder()
                .withSharedPref(context, "typetype_keyset", "typetype_keyset_store")
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://typetype_master_key")
                .build()
                .keysetHandle
            return handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        }
    }
}
