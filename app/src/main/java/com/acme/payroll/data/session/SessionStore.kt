package com.acme.payroll.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.acme.payroll.domain.model.Role
import com.acme.payroll.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val rememberEmail: Boolean = true,
    val lastEmail: String = "",
    val employeesPageSize: Int = 10,
    val darkTheme: Boolean = false,
)

@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val session: Flow<Session?> = dataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID] ?: return@map null
        Session(
            userId = userId,
            email = prefs[Keys.EMAIL].orEmpty(),
            displayName = prefs[Keys.DISPLAY_NAME].orEmpty(),
            role = prefs[Keys.ROLE]?.let { runCatching { Role.valueOf(it) }.getOrNull() } ?: Role.EMPLOYEE,
            employeeId = prefs[Keys.EMPLOYEE_ID],
            departmentId = prefs[Keys.DEPARTMENT_ID],
        )
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            rememberEmail = prefs[Keys.REMEMBER_EMAIL] ?: true,
            lastEmail = prefs[Keys.LAST_EMAIL].orEmpty(),
            employeesPageSize = prefs[Keys.PAGE_SIZE] ?: 10,
            darkTheme = prefs[Keys.DARK_THEME] ?: false,
        )
    }

    suspend fun current(): Session? = session.first()

    suspend fun save(session: Session) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.EMAIL] = session.email
            prefs[Keys.DISPLAY_NAME] = session.displayName
            prefs[Keys.ROLE] = session.role.name
            if (session.employeeId != null) prefs[Keys.EMPLOYEE_ID] = session.employeeId else prefs.remove(Keys.EMPLOYEE_ID)
            if (session.departmentId != null) prefs[Keys.DEPARTMENT_ID] = session.departmentId else prefs.remove(Keys.DEPARTMENT_ID)
            if (prefs[Keys.REMEMBER_EMAIL] != false) prefs[Keys.LAST_EMAIL] = session.email
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.DISPLAY_NAME)
            prefs.remove(Keys.ROLE)
            prefs.remove(Keys.EMPLOYEE_ID)
            prefs.remove(Keys.DEPARTMENT_ID)
        }
    }

    suspend fun setRememberEmail(enabled: Boolean) {
        dataStore.edit {
            it[Keys.REMEMBER_EMAIL] = enabled
            if (!enabled) it.remove(Keys.LAST_EMAIL)
        }
    }

    suspend fun setPageSize(size: Int) {
        dataStore.edit { it[Keys.PAGE_SIZE] = size }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    private object Keys {
        val USER_ID = longPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val ROLE = stringPreferencesKey("role")
        val EMPLOYEE_ID = longPreferencesKey("employee_id")
        val DEPARTMENT_ID = longPreferencesKey("department_id")
        val REMEMBER_EMAIL = booleanPreferencesKey("remember_email")
        val LAST_EMAIL = stringPreferencesKey("last_email")
        val PAGE_SIZE = intPreferencesKey("employees_page_size")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }
}
