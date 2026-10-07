package com.sakukasir.pos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakukasir.pos.data.RepositoryProvider
import com.sakukasir.pos.ui.*

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val bluetoothPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            val repo = remember { RepositoryProvider.get(applicationContext) }
            val vm: PosViewModel = viewModel(factory = PosViewModelFactory(repo))
            val state by vm.state.collectAsState()
            SideEffect {
                val dark = state.darkTheme
                val surface = if (dark) android.graphics.Color.parseColor("#0F0F0D") else android.graphics.Color.parseColor("#FAFAF7")
                window.statusBarColor = surface
                window.navigationBarColor = surface
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            SakuKasirTheme(dark = state.darkTheme) {
                if (state.user == null) LoginScreen(vm) else AppShell(vm) { vm.logout() }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            bluetoothPermission.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
        }
    }
}
