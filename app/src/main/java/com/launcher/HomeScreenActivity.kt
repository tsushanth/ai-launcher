package com.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.launcher.ui.HomeScreen
import com.launcher.ui.theme.LauncherTheme

class HomeScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        onOpenAppDrawer = {
                            startActivity(Intent(this, AppDrawerActivity::class.java))
                        },
                        onOpenSettings = {
                            startActivity(Intent(this, LauncherSettingsActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh apps list when returning to home screen
    }

    override fun onBackPressed() {
        // Do nothing - prevent exiting launcher
    }
}
