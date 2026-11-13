package com.acme.payroll.data.seed

import com.acme.payroll.domain.model.PayType
import java.time.LocalDate

/** Static sample data loaded into an empty database on first launch. */
internal object SeedData {

    data class Dept(val name: String, val code: String, val costCenter: String)

    data class Person(
        val first: String,
        val last: String,
        val deptCode: String,
        val title: String,
        val payType: PayType,
        /** Annual salary for salaried staff, hourly rate for hourly staff, in dollars. */
        val amount: String,
        val hired: LocalDate,
        val active: Boolean = true,
        val pension: Boolean = false,
        val otherDeduction: String = "0",
    ) {
        val email: String
            get() = "${first.lowercase()}.${last.lowercase().replace("'", "")}@acme.test"
    }

    const val DEMO_PASSWORD = "Passw0rd!"

    val departments = listOf(
        Dept("Engineering", "ENG", "CC-100"),
        Dept("Finance", "FIN", "CC-200"),
        Dept("Sales", "SAL", "CC-300"),
        Dept("Operations", "OPS", "CC-400"),
        Dept("HR", "HR", "CC-500"),
    )

    private fun d(text: String) = LocalDate.parse(text)

    val people = listOf(
        Person("Marcus", "Reed", "ENG", "Engineering Manager", PayType.SALARIED, "132000", d("2016-03-14"), pension = true, otherDeduction = "120"),
        Person("Priya", "Shah", "ENG", "Senior Software Engineer", PayType.SALARIED, "118000", d("2018-07-02"), pension = true, otherDeduction = "120"),
        Person("Daniel", "Kim", "ENG", "Software Engineer", PayType.SALARIED, "96000", d("2020-01-20"), otherDeduction = "120"),
        Person("Sofia", "Alvarez", "ENG", "Software Engineer", PayType.SALARIED, "92500", d("2021-09-13"), pension = true, otherDeduction = "120"),
        Person("Emily", "Turner", "ENG", "QA Technician", PayType.HOURLY, "38.50", d("2022-04-04"), otherDeduction = "45"),
        Person("Jonah", "Price", "ENG", "IT Support Technician", PayType.HOURLY, "29.75", d("2023-02-27")),
        Person("Lena", "Fischer", "ENG", "DevOps Engineer", PayType.SALARIED, "104000", d("2019-05-06"), active = false),
        Person("Grace", "Liu", "FIN", "Finance Director", PayType.SALARIED, "128000", d("2015-11-02"), pension = true, otherDeduction = "150"),
        Person("Omar", "Haddad", "FIN", "Payroll Specialist", PayType.SALARIED, "64000", d("2019-08-19"), pension = true, otherDeduction = "90"),
        Person("Hannah", "Wright", "FIN", "Accountant", PayType.SALARIED, "71000", d("2020-10-05"), otherDeduction = "90"),
        Person("Victor", "Petrov", "FIN", "Accounts Clerk", PayType.HOURLY, "24.00", d("2023-06-12")),
        Person("Chloe", "Martin", "SAL", "Sales Director", PayType.SALARIED, "121000", d("2017-02-20"), pension = true, otherDeduction = "120"),
        Person("Ethan", "Brooks", "SAL", "Account Executive", PayType.SALARIED, "68000", d("2021-03-08")),
        Person("Mia", "Rossi", "SAL", "Account Executive", PayType.SALARIED, "66000", d("2022-01-17"), pension = true),
        Person("Lucas", "Nguyen", "SAL", "Sales Development Rep", PayType.HOURLY, "22.50", d("2023-09-04")),
        Person("Ava", "Johnson", "SAL", "Sales Coordinator", PayType.SALARIED, "52000", d("2018-04-23"), active = false),
        Person("Noah", "Williams", "OPS", "Operations Manager", PayType.SALARIED, "98000", d("2016-08-01"), pension = true, otherDeduction = "120"),
        Person("Isabella", "Garcia", "OPS", "Warehouse Lead", PayType.HOURLY, "27.00", d("2019-03-18"), pension = true, otherDeduction = "45"),
        Person("Liam", "O'Connor", "OPS", "Warehouse Associate", PayType.HOURLY, "19.50", d("2021-06-28")),
        Person("Zoe", "Clarke", "OPS", "Warehouse Associate", PayType.HOURLY, "19.50", d("2022-11-14")),
        Person("Samuel", "Okafor", "OPS", "Logistics Coordinator", PayType.SALARIED, "58000", d("2020-07-27")),
        Person("Ruby", "Evans", "OPS", "Delivery Driver", PayType.HOURLY, "21.00", d("2018-10-01"), active = false),
        Person("Olivia", "Bennett", "HR", "HR Director", PayType.SALARIED, "112000", d("2014-05-12"), pension = true, otherDeduction = "150"),
        Person("Henry", "Scott", "HR", "HR Generalist", PayType.SALARIED, "61000", d("2021-01-11")),
        Person("Amelia", "Ward", "HR", "Recruiter", PayType.SALARIED, "59000", d("2022-08-22"), pension = true),
    )

    /** Employee e-mails the demo accounts are linked to. */
    const val MANAGER_EMPLOYEE_EMAIL = "marcus.reed@acme.test"
    const val EMPLOYEE_EMPLOYEE_EMAIL = "emily.turner@acme.test"
}
