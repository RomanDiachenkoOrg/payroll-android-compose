package com.acme.payroll.data.repository

import com.acme.payroll.data.local.dao.UserDao
import com.acme.payroll.data.session.SessionStore
import com.acme.payroll.domain.PasswordHasher
import com.acme.payroll.domain.model.Session
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LoginResult {
    data class Success(val session: Session) : LoginResult
    data object InvalidCredentials : LoginResult
}

interface AuthRepository {
    suspend fun login(email: String, password: String): LoginResult
    suspend fun logout()
}

@Singleton
class RoomAuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val sessionStore: SessionStore,
    private val audit: AuditRepository,
) : AuthRepository {

    override suspend fun login(email: String, password: String): LoginResult {
        val user = userDao.getByEmail(email.trim())
        if (user == null || !PasswordHasher.verify(password, user.salt, user.passwordHash)) {
            audit.log("LOGIN_FAILED", "User", "Failed sign-in for ${email.trim()}", actor = email.trim())
            return LoginResult.InvalidCredentials
        }
        val session = Session(
            userId = user.id,
            email = user.email,
            displayName = user.displayName,
            role = user.role,
            employeeId = user.employeeId,
            departmentId = user.departmentId,
        )
        sessionStore.save(session)
        userDao.touchLogin(user.id, System.currentTimeMillis())
        audit.log("LOGIN", "User", "Signed in", actor = user.email)
        return LoginResult.Success(session)
    }

    override suspend fun logout() {
        sessionStore.current()?.let { audit.log("LOGOUT", "User", "Signed out", actor = it.email) }
        sessionStore.clear()
    }
}
