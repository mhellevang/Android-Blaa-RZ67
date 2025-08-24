package dev.hellevang.openrz67.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hellevang.openrz67.bluetooth.BluetoothManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TriggerControlViewModel(
    private val bluetoothManager: BluetoothManager
) : ViewModel() {
    
    // Trigger type state
    private val _triggerType = MutableStateFlow(TriggerType.Direct)
    val triggerType: StateFlow<TriggerType> = _triggerType.asStateFlow()
    
    // Countdown state
    private val _startDelayedTrigger = MutableStateFlow(false)
    val startDelayedTrigger: StateFlow<Boolean> = _startDelayedTrigger.asStateFlow()
    
    private val _countdownTimeLeft = MutableStateFlow(0)
    val countdownTimeLeft: StateFlow<Int> = _countdownTimeLeft.asStateFlow()
    
    // Bluetooth state (forwarded from BluetoothManager)
    val connectionState: StateFlow<String> = bluetoothManager.connectionState
    val isConnected: StateFlow<Boolean> = bluetoothManager.isConnected
    
    private var countdownJob: Job? = null
    
    enum class TriggerType {
        Direct,
        Countdown
    }
    
    fun toggleTriggerType() {
        _triggerType.value = when (_triggerType.value) {
            TriggerType.Direct -> TriggerType.Countdown
            TriggerType.Countdown -> TriggerType.Direct
        }
    }
    
    fun handleTriggerButtonClick() {
        when (_triggerType.value) {
            TriggerType.Direct -> {
                bluetoothManager.sendSignal(BluetoothManager.SignalType.Trigger)
            }
            TriggerType.Countdown -> {
                if (_startDelayedTrigger.value) {
                    // Cancel countdown
                    stopCountdown()
                } else {
                    // Start countdown
                    startCountdown()
                }
            }
        }
    }
    
    private fun startCountdown() {
        bluetoothManager.sendSignal(BluetoothManager.SignalType.ArduinoCountdown, true)
        _startDelayedTrigger.value = true
        startCountdownTimer()
    }
    
    private fun stopCountdown() {
        bluetoothManager.sendSignal(BluetoothManager.SignalType.ArduinoCountdown, false)
        _startDelayedTrigger.value = false
        stopCountdownTimer()
    }
    
    private fun startCountdownTimer() {
        countdownJob?.cancel()
        _countdownTimeLeft.value = 10
        countdownJob = viewModelScope.launch {
            repeat(10) {
                delay(1000)
                _countdownTimeLeft.value = _countdownTimeLeft.value - 1
            }
            // Countdown finished
            _startDelayedTrigger.value = false
            _countdownTimeLeft.value = 0
        }
    }
    
    private fun stopCountdownTimer() {
        countdownJob?.cancel()
        _countdownTimeLeft.value = 0
    }
    
    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        bluetoothManager.cleanup()
    }
}