package com.juragan38.pos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "juragan38_session")

data class Session(val fullName: String, val username: String, val role: String)

@Singleton
class SessionManager @Inject constructor(private val context: Context) {

    private val keyName = stringPreferencesKey("full_name")
    private val keyUsername = stringPreferencesKey("username")
    private val keyRole = stringPreferencesKey("role")

    val session: Flow<Session?> = context.dataStore.data.map { prefs ->
        val username = prefs[keyUsername]
        if (username.isNullOrBlank()) null
        else Session(
            fullName = prefs[keyName].orEmpty(),
            username = username,
            role = prefs[keyRole].orEmpty()
        )
    }

    suspend fun save(session: Session) {
        context.dataStore.edit { prefs ->
            prefs[keyName] = session.fullName
            prefs[keyUsername] = session.username
            prefs[keyRole] = session.role
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
