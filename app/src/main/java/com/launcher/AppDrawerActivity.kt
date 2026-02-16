package com.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.launcher.ui.AppDrawer
import com.launcher.ui.theme.LauncherTheme

class AppDrawerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppDrawer(
                        onClose = { finish() },
                        onAppLaunch = { packageName ->
                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                            intent?.let {
                                startActivity(it)
                                finish() // Close drawer after launching app
                            }
                        }
                    )
                }
            }
        }
    }
}
