package com.nexus.iptv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            // Normaliza o layout para 960dp de largura em qualquer TV (design baseline 960x540dp)
            val dm = resources.displayMetrics
            val longSide = maxOf(dm.widthPixels, dm.heightPixels)
            CompositionLocalProvider(LocalDensity provides Density(longSide / 960f, 1f)) {
                NexusTheme {
                    val vm: AppVm = viewModel()
                    NexusApp(vm) { finish() }
                }
            }
        }
    }
}

@Composable
fun NexusApp(vm: AppVm, onExit: () -> Unit) {
    BackHandler { if (!vm.back()) onExit() }
    Box(Modifier.fillMaxSize().background(NX.Black)) {
        when (val s = vm.screen) {
            Screen.Login -> LoginScreen(vm, onExit)
            Screen.Home -> HomeScreen(vm)
            Screen.Live -> LiveScreen(vm)
            Screen.Movies -> VodScreen(vm, false)
            Screen.Series -> VodScreen(vm, true)
            Screen.Search -> SearchScreen(vm)
            Screen.MyList -> MyListScreen(vm)
            Screen.Settings -> SettingsScreen(vm)
            Screen.Player -> PlayerScreen(vm)
            is Screen.Detail -> DetailScreen(vm, s.item)
        }
    }
}
