package com.acme.payroll.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.acme.payroll.ui.login.LoginScreen
import com.acme.payroll.ui.navigation.MainScaffold

@Composable
fun PayrollRoot(state: MainUiState, onLogout: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val session = state.session
        when {
            !state.ready -> Box(Modifier.fillMaxSize().testTag("app_loading"), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            session == null -> LoginScreen()
            else -> key(session.userId) {
                MainScaffold(session = session, onLogout = onLogout)
            }
        }
    }
}
