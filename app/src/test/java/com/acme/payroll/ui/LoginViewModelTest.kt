package com.acme.payroll.ui

import com.acme.payroll.data.repository.AuthRepository
import com.acme.payroll.data.repository.LoginResult
import com.acme.payroll.domain.model.Role
import com.acme.payroll.domain.model.Session
import com.acme.payroll.ui.login.LoginViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuth : AuthRepository {
        var calls = 0
        override suspend fun login(email: String, password: String): LoginResult {
            calls++
            return if (email == "admin@acme.test" && password == "Passw0rd!") {
                LoginResult.Success(Session(1, email, "Alex Morgan", Role.ADMIN, null, null))
            } else {
                LoginResult.InvalidCredentials
            }
        }

        override suspend fun logout() = Unit
    }

    @Test
    fun `remembered email is prefilled`() {
        val vm = LoginViewModel(FakeAuth(), flowOf("manager@acme.test"))
        assertEquals("manager@acme.test", vm.state.value.email)
    }

    @Test
    fun `empty form shows field errors without calling repository`() {
        val auth = FakeAuth()
        val vm = LoginViewModel(auth, flowOf(""))
        vm.submit()
        assertEquals("Email is required", vm.state.value.emailError)
        assertEquals("Password is required", vm.state.value.passwordError)
        assertEquals(0, auth.calls)
    }

    @Test
    fun `invalid email format is rejected`() {
        val vm = LoginViewModel(FakeAuth(), flowOf(""))
        vm.onEmailChange("admin")
        vm.onPasswordChange("x")
        vm.submit()
        assertEquals("Enter a valid email address", vm.state.value.emailError)
    }

    @Test
    fun `wrong password shows message and clears password`() {
        val vm = LoginViewModel(FakeAuth(), flowOf(""))
        vm.onEmailChange("admin@acme.test")
        vm.onPasswordChange("nope")
        vm.submit()
        assertEquals("Incorrect email or password", vm.state.value.error)
        assertEquals("", vm.state.value.password)
        assertFalse(vm.state.value.signedIn)
    }

    @Test
    fun `correct credentials sign in`() {
        val vm = LoginViewModel(FakeAuth(), flowOf(""))
        vm.onEmailChange(" admin@acme.test ")
        vm.onPasswordChange("Passw0rd!")
        vm.submit()
        assertTrue(vm.state.value.signedIn)
        assertNull(vm.state.value.error)
    }
}
