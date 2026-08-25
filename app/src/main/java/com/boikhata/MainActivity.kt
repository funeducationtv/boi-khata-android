package com.boikhata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.boikhata.presentation.RootApp
import com.boikhata.presentation.SessionManager
import com.boikhata.presentation.theme.BoiKhataTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoiKhataTheme {
                RootApp(sessionManager)
            }
        }

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> sessionManager.checkAutoLock()
                Lifecycle.Event.ON_PAUSE -> sessionManager.updateActivity()
                else -> {}
            }
        })
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionManager.updateActivity()
    }
}
