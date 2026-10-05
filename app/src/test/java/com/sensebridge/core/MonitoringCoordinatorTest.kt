package com.sensebridge.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sensebridge.core.monitoring.MonitoringCoordinator
import com.sensebridge.core.service.SenseBridgeForegroundService
import com.sensebridge.data.repository.UserPreferences
import com.sensebridge.data.repository.UserPreferencesRepository
import com.sensebridge.input.sound.AudioRecorderManager
import com.sensebridge.input.sound.DecibelEnergyGate
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoringCoordinatorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockContext: Context = mockk(relaxed = true)
    private val mockRecorder: AudioRecorderManager = mockk(relaxed = true)
    private val mockPreferencesRepo: UserPreferencesRepository = mockk(relaxed = true)
    private val energyGate = DecibelEnergyGate()

    private val recorderRecordingState = MutableStateFlow(false)
    private val recorderDecibelState = MutableStateFlow(0.0)
    private val preferencesFlow = MutableStateFlow(
        UserPreferences(
            enableHaptic = true,
            enableVoice = true,
            enableVisual = true,
            thresholdDb = 60.0,
            isBackgroundMonitoringEnabled = true
        )
    )

    private lateinit var coordinator: MonitoringCoordinator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(ContextCompat::class)
        mockkObject(SenseBridgeForegroundService.Companion)

        every { SenseBridgeForegroundService.start(any()) } returns Unit
        every { SenseBridgeForegroundService.stop(any()) } returns Unit

        every { mockRecorder.isRecording } returns recorderRecordingState
        every { mockRecorder.currentDecibels } returns recorderDecibelState
        every { mockPreferencesRepo.userPreferencesFlow } returns preferencesFlow
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED

        coordinator = MonitoringCoordinator(
            context = mockContext,
            audioRecorderManager = mockRecorder,
            energyGate = energyGate,
            preferencesRepository = mockPreferencesRepo,
            dispatcher = testDispatcher
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun startMonitoring_whenPermittedAndBackgroundEnabled_startsServiceAndRecorder() {
        coordinator.startMonitoring()

        verify { SenseBridgeForegroundService.start(mockContext) }
        verify { mockRecorder.startListening() }
    }

    @Test
    fun stopMonitoring_stopsRecorderAndService() {
        coordinator.stopMonitoring()

        verify { mockRecorder.stopListening() }
        verify { SenseBridgeForegroundService.stop(mockContext) }
    }

    @Test
    fun startIfPermittedAndEnabled_whenDisabled_doesNotStart() {
        preferencesFlow.value = preferencesFlow.value.copy(isBackgroundMonitoringEnabled = false)
        testDispatcher.scheduler.advanceUntilIdle()

        coordinator.startIfPermittedAndEnabled()

        verify(exactly = 0) { mockRecorder.startListening() }
        verify(exactly = 0) { SenseBridgeForegroundService.start(any()) }
    }

    @Test
    fun setBackgroundMonitoring_persistsPreference() = runTest {
        coordinator.setBackgroundMonitoring(false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockPreferencesRepo.updateBackgroundMonitoringEnabled(false) }
        verify { mockRecorder.setBackgroundMonitoring(false) }
    }
}
