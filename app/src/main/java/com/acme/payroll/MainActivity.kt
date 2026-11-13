package com.acme.payroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.ui.PayrollRoot
import com.acme.payroll.ui.MainViewModel
import com.acme.payroll.ui.theme.PayrollTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            PayrollTheme(darkTheme = state.darkTheme) {
                PayrollRoot(state = state, onLogout = viewModel::logout)
            }
        }
    }
}
