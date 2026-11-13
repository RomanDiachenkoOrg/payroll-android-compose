package com.acme.payroll.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.acme.payroll.data.local.PayrollDatabase
import com.acme.payroll.data.local.dao.AuditDao
import com.acme.payroll.data.local.dao.DepartmentDao
import com.acme.payroll.data.local.dao.EmployeeDao
import com.acme.payroll.data.local.dao.PayRunDao
import com.acme.payroll.data.local.dao.SettingsDao
import com.acme.payroll.data.local.dao.TimesheetDao
import com.acme.payroll.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PayrollDatabase =
        Room.databaseBuilder(context, PayrollDatabase::class.java, PayrollDatabase.NAME)
            .addMigrations(*PayrollDatabase.ALL_MIGRATIONS)
            .build()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("session") }

    @Provides fun departmentDao(db: PayrollDatabase): DepartmentDao = db.departmentDao()
    @Provides fun employeeDao(db: PayrollDatabase): EmployeeDao = db.employeeDao()
    @Provides fun timesheetDao(db: PayrollDatabase): TimesheetDao = db.timesheetDao()
    @Provides fun payRunDao(db: PayrollDatabase): PayRunDao = db.payRunDao()
    @Provides fun userDao(db: PayrollDatabase): UserDao = db.userDao()
    @Provides fun auditDao(db: PayrollDatabase): AuditDao = db.auditDao()
    @Provides fun settingsDao(db: PayrollDatabase): SettingsDao = db.settingsDao()
}
