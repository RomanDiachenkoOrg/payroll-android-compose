package com.acme.payroll.data.repository

import com.acme.payroll.data.local.dao.AuditDao
import com.acme.payroll.data.local.entity.AuditLogEntity
import com.acme.payroll.data.session.SessionStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditRepository @Inject constructor(
    private val auditDao: AuditDao,
    private val sessionStore: SessionStore,
) {
    fun observeRecent(): Flow<List<AuditLogEntity>> = auditDao.observeRecent()

    suspend fun log(action: String, entity: String, details: String, actor: String? = null) {
        val who = actor ?: sessionStore.current()?.email ?: "system"
        auditDao.insert(
            AuditLogEntity(
                timestamp = System.currentTimeMillis(),
                userEmail = who,
                action = action,
                entity = entity,
                details = details,
            )
        )
    }
}
