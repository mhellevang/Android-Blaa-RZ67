package dev.hellevang.openrz67.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import com.juul.kable.ConnectionLostException
import com.juul.kable.Filter
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.characteristicOf
import com.juul.kable.peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

class BluetoothManager(
    private val context: Context,
    private val scope: CoroutineScope = MainScope()
) {
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var peripheral: Peripheral
    private val connectionAttempt = AtomicInteger()

    // State management
    private val _connectionState = MutableStateFlow("Not connected")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    companion object {
        // UUID of openrz67-trigger services
        val TARGET_SERVICE_UUID: UUID = UUID.fromString("c9239c9e-6fc9-4168-b3aa-53105eb990b0")
        
        // UUID of openrz67-trigger characteristic
        val TARGET_CHARACTERISTIC_UUID: UUID = UUID.fromString("458d4dc9-349f-401d-b092-a2b1c55f5319")
    }

    enum class SignalType {
        Trigger,
        ArduinoCountdown
    }

    fun initialize() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        scope.launch {
            try {
                val advertisement = Scanner {
                    filters = listOf(Filter.Service(TARGET_SERVICE_UUID))
                }.advertisements.first()

                awaitAll(
                    async {
                        peripheral = scope.peripheral(advertisement)
                    }
                )
                
                enableAutoReconnect()
                connect()
                
                // Observe connection state
                scope.launch {
                    peripheral.state.collect { state ->
                        _connectionState.value = state.toString()
                        _isConnected.value = state is State.Connected
                        println("Connection State: ${state}")
                    }
                }
            } catch (e: Exception) {
                println("Failed to initialize Bluetooth: ${e.message}")
                _connectionState.value = "Failed to initialize"
            }
        }
    }

    private fun CoroutineScope.enableAutoReconnect() {
        peripheral.state
            .filter { it is State.Disconnected }
            .onEach {
                val timeMillis = backoff(
                    base = 500L,
                    multiplier = 2f,
                    retry = connectionAttempt.getAndIncrement()
                )
                println("Waiting $timeMillis ms to reconnect...")
                delay(timeMillis)
                connect()
            }
            .launchIn(this)
    }

    private fun CoroutineScope.connect() {
        connectionAttempt.incrementAndGet()
        launch {
            println("Connecting...")
            try {
                peripheral.connect()
                connectionAttempt.set(0)
            } catch (e: ConnectionLostException) {
                println("Connection attempt failed: ${e.message}")
            }
        }
    }

    fun sendSignal(signalType: SignalType = SignalType.Trigger, on: Boolean = true): Job {
        return scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val characteristic = characteristicOf(
                        TARGET_SERVICE_UUID.toString(),
                        TARGET_CHARACTERISTIC_UUID.toString()
                    )
                    
                    when (signalType) {
                        SignalType.Trigger -> {
                            if (on) {
                                println("Sending trigger signal")
                                peripheral.write(characteristic, byteArrayOf(11.toByte()))
                            } else {
                                println("Sending stop signal")
                                peripheral.write(characteristic, byteArrayOf(10.toByte()))
                            }
                        }
                        SignalType.ArduinoCountdown -> {
                            if (on) {
                                println("Sending Arduino countdown start signal")
                                peripheral.write(characteristic, byteArrayOf(31.toByte()))
                            } else {
                                println("Sending Arduino countdown stop signal")
                                peripheral.write(characteristic, byteArrayOf(30.toByte()))
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Failed to send signal: ${e.message}")
                }
            }
        }
    }

    private fun backoff(
        base: Long,
        multiplier: Float,
        retry: Int,
    ): Long = (base * multiplier.pow(retry - 1)).toLong()

    fun cleanup() {
        scope.launch {
            try {
                if (this@BluetoothManager::peripheral.isInitialized && _isConnected.value) {
                    peripheral.disconnect()
                }
            } catch (e: Exception) {
                println("Error during cleanup: ${e.message}")
            }
        }
    }
}