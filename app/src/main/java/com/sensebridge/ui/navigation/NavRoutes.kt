package com.sensebridge.ui.navigation

sealed class NavRoute(val route: String) {
    data object Home : NavRoute("home")
    data object DeafAssist : NavRoute("deaf_assist")
    data object BlindAssist : NavRoute("blind_assist")
    data object OcrReader : NavRoute("ocr_reader")
    data object Communication : NavRoute("communication")
    data object Settings : NavRoute("settings")
}
