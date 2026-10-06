package com.portalhomebase.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.portalhomebase.app.data.BoardState
import com.portalhomebase.app.data.Prefs
import com.portalhomebase.app.ui.App
import com.portalhomebase.app.ui.theme.HomebaseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val prefs = Prefs(this)
        setContent {
            HomebaseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state = remember { BoardState(prefs) }
                    App(state)
                }
            }
        }
    }
}
