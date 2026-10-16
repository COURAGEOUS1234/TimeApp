package com.gabriel.timeapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import com.gabriel.timeapp.ui.AppRoot
import com.gabriel.timeapp.ui.ThemeState
import com.gabriel.timeapp.ui.Themes
import com.gabriel.timeapp.ui.TerminalTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_ALARM = 0
        const val TAB_WORLD = 1
        const val TAB_STOPWATCH = 2
        const val TAB_TIMER = 3
        const val TAB_POMODORO = 4
    }

    private val pendingTab = mutableIntStateOf(-1)
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifs.ensure(this)
        Store.migrate(this)
        ThemeState.palette = Themes.of(Store.settings(this).theme)
        pendingTab.intValue = intent?.getIntExtra(EXTRA_TAB, -1) ?: -1
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        setContent {
            TerminalTheme {
                AppRoot(
                    pendingTab = pendingTab,
                    resumeTick = resumeTick.intValue,
                    onSettingsChanged = { applyKeepScreenOn() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val t = intent.getIntExtra(EXTRA_TAB, -1)
        if (t >= 0) pendingTab.intValue = t
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
        ThemeState.palette = Themes.of(Store.settings(this).theme)
        applyKeepScreenOn()
    }

    private fun applyKeepScreenOn() {
        if (Store.settings(this).keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
