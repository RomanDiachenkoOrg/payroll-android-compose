package com.acme.payroll.di

import com.acme.payroll.data.repository.AuthRepository
import com.acme.payroll.data.repository.EmployeeRepository
import com.acme.payroll.data.repository.RoomAuthRepository
import com.acme.payroll.data.repository.RoomEmployeeRepository
import com.acme.payroll.data.session.SessionStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAuthRepository(impl: RoomAuthRepository): AuthRepository

    @Binds
    abstract fun bindEmployeeRepository(impl: RoomEmployeeRepository): EmployeeRepository

    companion object {
        @Provides
        fun provideClock(): Clock = Clock.systemDefaultZone()

        @Provides
        @Named("lastEmail")
        fun provideLastEmail(store: SessionStore): Flow<String> = store.settings.map { it.lastEmail }
    }
}
