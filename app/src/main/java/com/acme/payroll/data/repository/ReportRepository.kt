package com.acme.payroll.data.repository

import com.acme.payroll.data.local.dao.PayRunDao
import com.acme.payroll.data.local.entity.DepartmentCostRow
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.util.Csv
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val payRunDao: PayRunDao,
) {
    suspend fun costByDepartment(from: YearMonth, to: YearMonth): List<DepartmentCostRow> =
        payRunDao.costByDepartment(from.atDay(1), to.atEndOfMonth())

    fun toCsv(rows: List<DepartmentCostRow>): String = Csv.build(
        header = listOf("Department", "Headcount", "Gross", "Income tax", "Social", "Pension", "Net", "Employer cost"),
        rows = rows.map {
            listOf(
                it.departmentName,
                it.headcount,
                Money.plain(it.grossCents),
                Money.plain(it.taxCents),
                Money.plain(it.socialCents),
                Money.plain(it.pensionCents),
                Money.plain(it.netCents),
                Money.plain(it.grossCents + it.socialCents),
            )
        },
    )
}
