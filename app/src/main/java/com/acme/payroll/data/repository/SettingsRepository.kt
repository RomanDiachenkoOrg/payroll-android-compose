package com.acme.payroll.data.repository

import androidx.room.withTransaction
import com.acme.payroll.data.local.PayrollDatabase
import com.acme.payroll.data.local.dao.SettingsDao
import com.acme.payroll.data.local.dao.UserDao
import com.acme.payroll.data.local.entity.PayrollSettingEntity
import com.acme.payroll.data.local.entity.TaxBandEntity
import com.acme.payroll.data.local.entity.UserEntity
import com.acme.payroll.domain.payroll.PayrollRates
import com.acme.payroll.domain.payroll.TaxBand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val db: PayrollDatabase,
    private val settingsDao: SettingsDao,
    private val userDao: UserDao,
    private val audit: AuditRepository,
) {
    fun observeRates(): Flow<PayrollRates> =
        combine(settingsDao.observeTaxBands(), settingsDao.observeSettings()) { bands, settings ->
            toRates(bands, settings)
        }

    suspend fun getRates(): PayrollRates = toRates(settingsDao.getTaxBands(), settingsDao.getSettings())

    fun observeUsers(): Flow<List<UserEntity>> = userDao.observeAll()

    suspend fun saveRates(rates: PayrollRates) {
        db.withTransaction {
            settingsDao.replaceTaxBands(
                rates.taxBands.map { TaxBandEntity(fromAmount = it.from, upToAmount = it.upTo, ratePercent = it.ratePercent) }
            )
            settingsDao.upsertSetting(PayrollSettingEntity(PayrollSettingEntity.SOCIAL_PERCENT, rates.socialPercent.toPlainString()))
            settingsDao.upsertSetting(PayrollSettingEntity(PayrollSettingEntity.PENSION_PERCENT, rates.pensionPercent.toPlainString()))
        }
        audit.log(
            "UPDATE",
            "TaxSettings",
            rates.taxBands.joinToString("; ") { "${it.from}-${it.upTo ?: "+"}: ${it.ratePercent}%" } +
                " | social ${rates.socialPercent}% | pension ${rates.pensionPercent}%",
        )
    }

    private fun toRates(bands: List<TaxBandEntity>, settings: List<PayrollSettingEntity>): PayrollRates {
        val defaults = PayrollRates.DEFAULT
        val map = settings.associate { it.key to it.value }
        return PayrollRates(
            taxBands = if (bands.isEmpty()) defaults.taxBands else bands.map { TaxBand(it.fromAmount, it.upToAmount, it.ratePercent) },
            socialPercent = map[PayrollSettingEntity.SOCIAL_PERCENT]?.toBigDecimalOrNull() ?: defaults.socialPercent,
            pensionPercent = map[PayrollSettingEntity.PENSION_PERCENT]?.toBigDecimalOrNull() ?: defaults.pensionPercent,
        )
    }

    companion object {
        fun validateBands(bands: List<TaxBand>): String? {
            if (bands.isEmpty()) return "At least one tax band is required"
            if (bands.first().from.signum() != 0) return "The first band must start at 0"
            bands.zipWithNext().forEach { (a, b) ->
                if (a.upTo == null || a.upTo.compareTo(b.from) != 0) return "Bands must be contiguous"
            }
            if (bands.last().upTo != null) return "The last band must be open-ended"
            if (bands.any { it.ratePercent < BigDecimal.ZERO || it.ratePercent > BigDecimal(100) }) {
                return "Rates must be between 0 and 100"
            }
            return null
        }
    }
}
