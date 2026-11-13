# Acme Payroll (Android, Jetpack Compose)

Payroll & HR app for a small company: employees, weekly timesheets, monthly pay runs,
payslips and payroll cost reports. Everything runs on the device: data lives in a local
Room database that is filled with sample data on first launch, so no backend is needed.

## Tech stack

- Kotlin 2.0, coroutines + `StateFlow`
- Jetpack Compose, Material 3, Navigation Compose (bottom navigation with nested graphs)
- MVVM with `ViewModel`s, Hilt for dependency injection
- Room (DAOs, `@Relation` queries, migrations, exported schema in `app/schemas`)
- DataStore Preferences for the session and user settings
- `java.time` for dates, `BigDecimal` for money
- JUnit 4 + kotlinx-coroutines-test for unit tests

## Prerequisites

| Tool | Version |
|------|---------|
| JDK | 17 or newer (21 works) |
| Android SDK | Platform 35, Build-Tools 35 |
| Android Studio (optional) | Ladybug 2024.2 or newer |
| Device / emulator | Android 8.0 (API 26) or newer |

Gradle itself is provided by the wrapper (`gradlew`), no local install required.

## Run

```bash
# point Gradle at your SDK once (or open the project in Android Studio, which does it for you)
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew assembleDebug                 # build app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                  # install on a connected device/emulator
adb shell am start -n com.acme.payroll/.MainActivity
```

On Windows use `gradlew.bat` instead of `./gradlew`.

The first start creates `payroll.db` and loads sample data (5 departments, 25 employees,
timesheets for the last months, two paid pay runs and an open draft run). To start over,
clear the app data: `adb shell pm clear com.acme.payroll`.

## Demo accounts

All demo users share the password `Passw0rd!`.

| Email | Role | Can do |
|-------|------|--------|
| `admin@acme.test` | Admin | Everything: employees, pay runs, departments, tax settings, users, audit log |
| `manager@acme.test` | Manager (Engineering) | Approve/reject Engineering timesheets, view employees and reports |
| `employee@acme.test` | Employee (Emily Turner) | Own profile and payslips, enter and submit own timesheets |

## Screens

Bottom navigation (items depend on the role):

- **Login** - email/password with validation and a wrong-password message.
- **Home** - headcount, last payroll total, pending timesheets, next pay date, payroll chart, recent activity.
- **Employees** - search, department and status filters, sorting, paging (10 per page);
  employee detail with salary history ("Add raise") and payslips; create/edit form with validation;
  deactivate/reactivate with confirmation. Employees see **Profile** (their own record) instead.
- **Timesheets** - list with status filter; weekly entry (7 days, 0-24 h), save draft, submit;
  approve or reject with a reason (managers for their department, admins for everyone).
- **Pay runs** (admin) - create a run for a month, calculate, review lines, approve, mark as paid (locked).
- **Payslip** - full breakdown, print-friendly view, share as CSV.
- **More** - Departments (CRUD; departments with active employees cannot be deleted),
  Reports (payroll cost by department for a month range, CSV export),
  Settings (theme, remembered email; admin: tax bands & rates, users, audit log with filters), sign out.

## Payroll rules

- Salaried: gross = annual salary / 12.
- Hourly: rate x regular hours + rate x 1.5 x overtime hours; overtime = hours above 40 in a week
  (approved timesheets whose week starts inside the pay period).
- Income tax per month, progressive: 0-1,000 at 0%, 1,000-4,000 at 15%, above 4,000 at 25% (editable).
- Social contribution 5% of gross; pension 3% of gross for employees who opted in; fixed other deductions.
- Net = gross - tax - social - pension - other, amounts rounded half-up to cents (USD).

## Project structure

```
app/src/main/java/com/acme/payroll
├── PayrollApp.kt, MainActivity.kt
├── data
│   ├── local          Room database, entities, DAOs, type converters, migrations
│   ├── repository     repositories used by the view models
│   ├── seed           sample data loaded on first launch
│   └── session        DataStore-backed session and settings
├── di                 Hilt modules
├── domain             payroll calculator, validators, models
├── ui                 Compose screens + view models per feature, navigation, theme
└── util               dates, CSV, share intent
app/src/test           JUnit tests (calculator, validators, view models)
```

## Tests

```bash
./gradlew testDebugUnitTest
```

Reports are written to `app/build/reports/tests/testDebugUnitTest/index.html`.
CI (GitHub Actions) builds the debug APK, runs the unit tests and uploads the APK as an artifact.
