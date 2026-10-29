package com.juragan38.pos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "juragan38_session")

data class Session(
    val fullName: String,
    val username: String,
    val role: String,
    val outletId: String = "",
    val outletName: String = "",
    val shiftKey: String = ""
) {
    /** Shift belum dipilih -> aplikasi harus membuka layar pilih outlet & shift. */
    val hasShift: Boolean get() = shiftKey.isNotBlank() && outletName.isNotBlank()
}

object ThemeMode {
    const val SYSTEM = "SYSTEM"
    const val LIGHT = "LIGHT"
    const val DARK = "DARK"
}

@Singleton
class SessionManager @Inject constructor(private val context: Context) {

    private val keyName = stringPreferencesKey("full_name")
    private val keyUsername = stringPreferencesKey("username")
    private val keyRole = stringPreferencesKey("role")
    private val keyOutletId = stringPreferencesKey("outlet_id")
    private val keyOutletName = stringPreferencesKey("outlet_name")
    private val keyShift = stringPreferencesKey("shift_key")

    val session: Flow<Session?> = context.dataStore.data.map { prefs ->
        val username = prefs[keyUsername]
        if (username.isNullOrBlank()) null
        else Session(
            fullName = prefs[keyName].orEmpty(),
            username = username,
            role = prefs[keyRole].orEmpty(),
            outletId = prefs[keyOutletId].orEmpty(),
            outletName = prefs[keyOutletName].orEmpty(),
            shiftKey = prefs[keyShift].orEmpty()
        )
    }

    suspend fun save(session: Session) {
        context.dataStore.edit { prefs ->
            prefs[keyName] = session.fullName
            prefs[keyUsername] = session.username
            prefs[keyRole] = session.role
            prefs[keyOutletId] = session.outletId
            prefs[keyOutletName] = session.outletName
            prefs[keyShift] = session.shiftKey
        }
    }

    suspend fun saveShift(outletId: String, outletName: String, shiftKey: String) {
        context.dataStore.edit { prefs ->
            prefs[keyOutletId] = outletId
            prefs[keyOutletName] = outletName
            prefs[keyShift] = shiftKey
        }
    }

    suspend fun clearShift() {
        context.dataStore.edit { prefs ->
            prefs[keyOutletId] = ""
            prefs[keyOutletName] = ""
            prefs[keyShift] = ""
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    // ------------------------------------------------------------ tema

    private fun themeKey(username: String) = stringPreferencesKey("theme_" + username.lowercase())

    /** Tema disimpan per akun, jadi tiap user punya pilihan sendiri di HP ini. */
    suspend fun themeOf(username: String): String =
        context.dataStore.data.first()[themeKey(username)] ?: ThemeMode.SYSTEM

    suspend fun setTheme(username: String, mode: String) {
        context.dataStore.edit { it[themeKey(username)] = mode }
    }
}
