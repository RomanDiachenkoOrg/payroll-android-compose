package com.acme.payroll.ui.employees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.domain.model.PayType
import com.acme.payroll.domain.validation.EmployeeField
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.DateField
import com.acme.payroll.ui.components.DropdownField
import com.acme.payroll.ui.components.LoadingBox

@Composable
fun EmployeeEditScreen(
    onBack: () -> Unit,
    onSaved: (id: Long, created: Boolean) -> Unit,
    viewModel: EmployeeEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedId) {
        state.savedId?.let { onSaved(it, state.isNew) }
    }

    AppScaffold(title = if (state.isNew) "New employee" else "Edit employee", onBack = onBack) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@AppScaffold
        }
        val form = state.form
        val errors = state.errors
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormField("First name", form.firstName, errors[EmployeeField.FIRST_NAME], "field_first_name") { v -> viewModel.update { it.copy(firstName = v) } }
            FormField("Last name", form.lastName, errors[EmployeeField.LAST_NAME], "field_last_name") { v -> viewModel.update { it.copy(lastName = v) } }
            FormField(
                "Work email", form.email, errors[EmployeeField.EMAIL], "field_email",
                keyboardType = KeyboardType.Email,
            ) { v -> viewModel.update { it.copy(email = v) } }
            FormField("Phone (optional)", form.phone, null, "field_phone", keyboardType = KeyboardType.Phone) { v ->
                viewModel.update { it.copy(phone = v) }
            }
            FormField("Job title", form.jobTitle, null, "field_job_title") { v -> viewModel.update { it.copy(jobTitle = v) } }
            DropdownField(
                label = "Department",
                options = departments.map { it.id as Long? to it.name },
                selected = form.departmentId,
                onSelected = { id -> viewModel.update { it.copy(departmentId = id) } },
                isError = errors.containsKey(EmployeeField.DEPARTMENT),
                supportingText = errors[EmployeeField.DEPARTMENT],
                modifier = Modifier.fillMaxWidth().testTag("field_department"),
            )

            Text("Pay type", style = MaterialTheme.typography.labelLarge)
            Row {
                PayType.entries.forEach { type ->
                    Row(
                        Modifier
                            .selectable(
                                selected = form.payType == type,
                                onClick = { viewModel.update { it.copy(payType = type) } },
                                role = Role.RadioButton,
                            )
                            .padding(end = 16.dp)
                            .testTag("pay_type_${type.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = form.payType == type, onClick = null)
                        Text(type.label)
                    }
                }
            }
            FormField(
                if (form.payType == PayType.SALARIED) "Annual salary (USD)" else "Hourly rate (USD)",
                form.amount, errors[EmployeeField.AMOUNT], "field_amount",
                keyboardType = KeyboardType.Decimal,
            ) { v -> viewModel.update { it.copy(amount = v) } }
            DateField(
                label = "Hire date",
                date = form.hireDate,
                onDateSelected = { d -> viewModel.update { it.copy(hireDate = d) } },
                isError = errors.containsKey(EmployeeField.HIRE_DATE),
                supportingText = errors[EmployeeField.HIRE_DATE],
                testTag = "field_hire_date",
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pension plan (3%)", modifier = Modifier.weight(1f))
                Switch(
                    checked = form.pensionOptIn,
                    onCheckedChange = { c -> viewModel.update { it.copy(pensionOptIn = c) } },
                    modifier = Modifier.testTag("field_pension"),
                )
            }
            FormField(
                "Other monthly deductions (USD)",
                form.otherDeduction, errors[EmployeeField.OTHER_DEDUCTION], "field_other_deduction",
                keyboardType = KeyboardType.Decimal,
            ) { v -> viewModel.update { it.copy(otherDeduction = v) } }

            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("employee_save"),
            ) { Text(if (state.isNew) "Create employee" else "Save changes") }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    error: String?,
    tag: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Words else KeyboardCapitalization.None,
        ),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}
