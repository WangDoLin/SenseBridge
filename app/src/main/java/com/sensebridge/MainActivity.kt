package com.sensebridge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sensebridge.core.monitoring.MonitoringCoordinator
import com.sensebridge.output.visual.AlertOverlayManager
import com.sensebridge.ui.blind.BlindAssistScreen
import com.sensebridge.ui.blind.BlindAssistViewModel
import com.sensebridge.ui.communication.CommunicationScreen
import com.sensebridge.ui.communication.CommunicationViewModel
import com.sensebridge.ui.components.AlertFlashOverlay
import com.sensebridge.ui.deaf.DeafAssistScreen
import com.sensebridge.ui.deaf.DeafAssistViewModel
import com.sensebridge.ui.home.HomeScreen
import com.sensebridge.ui.home.HomeViewModel
import com.sensebridge.ui.navigation.NavRoute
import com.sensebridge.ui.ocr.OcrReaderScreen
import com.sensebridge.ui.ocr.OcrReaderViewModel
import com.sensebridge.ui.settings.SettingsScreen
import com.sensebridge.ui.settings.SettingsViewModel
import com.sensebridge.ui.theme.BgDark
import com.sensebridge.ui.theme.SenseBridgeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var alertOverlayManager: AlertOverlayManager

    @Inject
    lateinit var monitoringCoordinator: MonitoringCoordinator

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            monitoringCoordinator.startIfPermittedAndEnabled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()
        monitoringCoordinator.startIfPermittedAndEnabled()

        setContent {
            SenseBridgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SenseBridgeNavGraph()
                        // Drawn last so it covers whichever screen is open
                        AlertFlashOverlay(overlayManager = alertOverlayManager)
                    }
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
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
fun SenseBridgeNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoute.Home.route
    ) {
        composable(NavRoute.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(NavRoute.DeafAssist.route) {
            val viewModel: DeafAssistViewModel = hiltViewModel()
            DeafAssistScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoute.BlindAssist.route) {
            val viewModel: BlindAssistViewModel = hiltViewModel()
            BlindAssistScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoute.OcrReader.route) {
            val viewModel: OcrReaderViewModel = hiltViewModel()
            OcrReaderScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoute.Communication.route) {
            val viewModel: CommunicationViewModel = hiltViewModel()
            CommunicationScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoute.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
