package com.sensebridge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sensebridge.ui.blind.BlindAssistScreen
import com.sensebridge.ui.communication.CommunicationScreen
import com.sensebridge.ui.deaf.DeafAssistScreen
import com.sensebridge.ui.home.HomeScreen
import com.sensebridge.ui.home.HomeViewModel
import com.sensebridge.ui.navigation.NavRoute
import com.sensebridge.ui.ocr.OcrReaderScreen
import com.sensebridge.ui.theme.BgDark
import com.sensebridge.ui.theme.SenseBridgeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val deafAssistViewModel: com.sensebridge.ui.deaf.DeafAssistViewModel by viewModels()
    private val blindAssistViewModel: com.sensebridge.ui.blind.BlindAssistViewModel by viewModels()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions granted or denied handled gracefully by individual managers
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()

        setContent {
            SenseBridgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    SenseBridgeNavGraph(
                        homeViewModel = homeViewModel,
                        deafAssistViewModel = deafAssistViewModel,
                        blindAssistViewModel = blindAssistViewModel
                    )
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.VIBRATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            requestPermissionsLauncher.launch(ungranted.toTypedArray())
        }
    }
}

@Composable
fun SenseBridgeNavGraph(
    homeViewModel: HomeViewModel,
    deafAssistViewModel: com.sensebridge.ui.deaf.DeafAssistViewModel,
    blindAssistViewModel: com.sensebridge.ui.blind.BlindAssistViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoute.Home.route
    ) {
        composable(NavRoute.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(NavRoute.DeafAssist.route) {
            DeafAssistScreen(
                viewModel = deafAssistViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoute.BlindAssist.route) {
            BlindAssistScreen(
                viewModel = blindAssistViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoute.OcrReader.route) {
            OcrReaderScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoute.Communication.route) {
            CommunicationScreen(onBack = { navController.popBackStack() })
        }
    }
}
