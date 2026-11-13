package com.acme.payroll.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.acme.payroll.data.local.entity.AuditLogEntity
import com.acme.payroll.data.local.entity.DeductionTypeEntity
import com.acme.payroll.data.local.entity.PayrollSettingEntity
import com.acme.payroll.data.local.entity.TaxBandEntity
import com.acme.payroll.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY role, email")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("UPDATE users SET last_login_at = :at WHERE id = :id")
    suspend fun touchLogin(id: Long, at: Long)

    @Insert
    suspend fun insertAll(users: List<UserEntity>)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insert(entry: AuditLogEntity)

    @Insert
    suspend fun insertAll(entries: List<AuditLogEntity>)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM tax_bands ORDER BY from_amount + 0")
    fun observeTaxBands(): Flow<List<TaxBandEntity>>

    @Query("SELECT * FROM tax_bands ORDER BY from_amount + 0")
    suspend fun getTaxBands(): List<TaxBandEntity>

    @Query("DELETE FROM tax_bands")
    suspend fun clearTaxBands()

    @Insert
    suspend fun insertTaxBands(bands: List<TaxBandEntity>)

    @Transaction
    suspend fun replaceTaxBands(bands: List<TaxBandEntity>) {
        clearTaxBands()
        insertTaxBands(bands)
    }

    @Query("SELECT * FROM payroll_settings")
    fun observeSettings(): Flow<List<PayrollSettingEntity>>

    @Query("SELECT * FROM payroll_settings")
    suspend fun getSettings(): List<PayrollSettingEntity>

    @Upsert
    suspend fun upsertSetting(setting: PayrollSettingEntity)

    @Query("SELECT * FROM deduction_types ORDER BY name")
    fun observeDeductionTypes(): Flow<List<DeductionTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeductionTypes(types: List<DeductionTypeEntity>)
}
