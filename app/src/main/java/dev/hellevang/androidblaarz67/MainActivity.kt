package dev.hellevang.androidblaarz67

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.juul.kable.*
import com.juul.kable.State
import dev.hellevang.androidblaarz67.ui.theme.AndroidBlaaRZ67Theme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow


class MainActivity : ComponentActivity() {
    private val scope = MainScope()
    lateinit var bluetoothLeScanner: BluetoothLeScanner
    lateinit var peripheral: Peripheral
    private val connectionAttempt = AtomicInteger()

    companion object {
        // UUID of RZ67 Blaa services
        val targetServiceUUID: UUID = UUID.fromString("c9239c9e-6fc9-4168-b3aa-53105eb990b0")

        // UUID of RZ67 Blaa characteristic
        val targetCharacteristicUUID: UUID = UUID.fromString("458d4dc9-349f-401d-b092-a2b1c55f5319")
        var connectionState by mutableStateOf("Not connected")
        var mState = mutableStateOf(0) //
        const val STATE_NONE = 0 // we're doing nothing
        const val STATE_CONNECTED = 3 // now connected to a remote device

        var startDelayedTrigger by mutableStateOf(false)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val takeResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(applicationContext, "Bluetooth ON", Toast.LENGTH_LONG).show()

            } else {
                Toast.makeText(applicationContext, "Bluetooth Off", Toast.LENGTH_LONG).show()
            }
        }
        val takePermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    takeResultLauncher.launch(intent)
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Bluetooth Permission is not Granted",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        takePermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        scope.launch {
            val advertisement = Scanner {
                filters = listOf(Filter.Service(targetServiceUUID))
            }.advertisements.first()

            awaitAll(
                async {
                    peripheral = scope.peripheral(advertisement)
                }
            )
            scope.enableAutoReconnect()
            scope.connect()
            scope.launch {
                peripheral.state.collect { state ->
                    connectionState = state.toString()
                    if (state is State.Connected) {
                        mState.value = STATE_CONNECTED
                    } else {
                        mState.value = STATE_NONE
                    }
                    println("Connection State $connectionState")
                }
            }
        }

        setContent {
            AndroidBlaaRZ67Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    AndroidBlaaRZ67App()
                }
            }
        }
    }

    private fun CoroutineScope.enableAutoReconnect() {
        peripheral.state
            .filter { it is State.Disconnected }
            .onEach {
                val timeMillis =
                    backoff(
                        base = 500L,
                        multiplier = 2f,
                        retry = connectionAttempt.getAndIncrement()
                    )
                println { "Waiting $timeMillis ms to reconnect..." }
                delay(timeMillis)
                connect()
            }
            .launchIn(this)
    }

    private fun CoroutineScope.connect() {
        connectionAttempt.incrementAndGet()
        launch {
            println { "connect" }
            try {
                peripheral.connect()
                connectionAttempt.set(0)
            } catch (e: ConnectionLostException) {
                println { "Connection attempt failed" }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Preview
    @Composable
    fun AndroidBlaaRZ67App() {

        val image =
            painterResource(R.drawable.dall_e_2023_01_03_20_12_46___a_synthwave_sketch_of_the_mamiya_rz67_)
        Box {
            Image(
                painter = image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxHeight(),
                contentScale = ContentScale.Fit
            )
        }

        Column {
            HeaderText()
            Spacer(modifier = Modifier.padding(top = 25.dp))

            // Debug
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(connectionState, color = Color.White)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TriggerButtonPanel()
            }
        }
    }


    @Composable
    fun ToggleButton(toggleButton: () -> Unit, modifier: Modifier = Modifier, text: String) {
        Button(onClick = { toggleButton() }, modifier = modifier) { Text(text) }
    }

    @Composable
    fun TriggerButtonPanel() {
        var triggerType by remember { mutableStateOf(TriggerType.Direct) }
        fun delayedTrigger() {
            if (startDelayedTrigger) {
                sendSignal(SignalType.Trigger, false)
                startDelayedTrigger = false
            }
        }

        Row(
            modifier = Modifier.padding(top = 100.dp)
        ) {
            when (triggerType) {
                // If current trigger type is countdown, show direct button
                TriggerType.Countdown -> {
                    ToggleButton(toggleButton = {
                        triggerType = TriggerType.Direct
                    }, text = "Mode")
                }
                else -> {
                    // If current trigger type is direct, show countdown button
                    ToggleButton(toggleButton = {
                        triggerType = TriggerType.Countdown
                    }, text = "Mode")
                }
            }
        }
        Text(
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(start = 16.dp, end = 16.dp),
            text = triggerType.name
        )
        Spacer(modifier = Modifier.padding(top = 15.dp))
        when (triggerType) {
            TriggerType.Direct -> {
                Text(
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .padding(start = 16.dp, end = 16.dp),
                    text = "Press the button to take a picture"
                )
            }
            TriggerType.Countdown -> {
                if (startDelayedTrigger) {
                    StartTimer(::delayedTrigger)
                } else {
                    Text(
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .padding(start = 16.dp, end = 16.dp),
                        text = "Press to start 10s countdown"
                    )
                }
            }
        }

        Button(
            onClick =
            {
                if (triggerType == TriggerType.Direct) {
                    sendSignal(SignalType.Trigger)
                } else {
                    startDelayedTrigger = if (startDelayedTrigger) {
                        // Cancel timer
                        sendSignal(SignalType.Blink, false)
                        false
                    } else {
                        sendSignal(SignalType.Blink, true)
                        true
                    }
                }
            },
            modifier = Modifier
                .padding(top = 50.dp),
            enabled = mState.value == STATE_CONNECTED,
        )
        {
            Text(text = "Trigger shutter", modifier = Modifier.padding(end = 10.dp))
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = null,
                tint = Color.White
            )
        }

    }

    enum class TriggerType {
        Direct,
        Countdown
    }

    @Composable
    fun StartTimer(done: () -> Unit) {
        val timeLeftMs by rememberCountdownTimerState(10_000)
        Text(
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(start = 16.dp, end = 16.dp),
            text = "$timeLeftMs ms left"
        )

        LaunchedEffect(timeLeftMs) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    while (isActive && timeLeftMs > 0) {
                        delay(50)
                    }
                    when (timeLeftMs) {
                        0L -> {
                            done()
                        }
                    }
                }
            }
        }
        when (timeLeftMs) {
            0L -> {
                Toast.makeText(applicationContext, "Take picture", Toast.LENGTH_SHORT).show()
            }
        }

    }

    @Composable
    fun rememberCountdownTimerState(
        initialMillis: Long,
        step: Long = 50
    ): MutableState<Long> {
        val timeLeft = remember { mutableStateOf(initialMillis) }
        LaunchedEffect(initialMillis, step) {
            val startTime = SystemClock.uptimeMillis()
            while (isActive && timeLeft.value > 0) {
                // how much time actually passed
                val duration = (SystemClock.uptimeMillis() - startTime).coerceAtLeast(0)
                timeLeft.value = (initialMillis - duration).coerceAtLeast(0)
                delay(step.coerceAtMost(timeLeft.value))
            }
        }
        return timeLeft
    }

    @Composable
    fun HeaderText() {
        Text(
            text = "Blåtann RZ67",
            fontSize = 36.sp,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(start = 10.dp, top = 24.dp)
        )
        Text(
            text = "A Mamiya RZ67 bluetooth trigger",
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(start = 16.dp, end = 16.dp)
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        AndroidBlaaRZ67Theme {
            AndroidBlaaRZ67App()
        }
    }

    enum class SignalType {
        Trigger,
        Blink
    }

    private fun sendSignal(signalType: SignalType = SignalType.Trigger, on: Boolean = true) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val characteristic = characteristicOf(
                    targetServiceUUID.toString(),
                    targetCharacteristicUUID.toString()
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
                    SignalType.Blink -> {
                        if (on) {
                            println("Sending blink signal")
                            peripheral.write(characteristic, byteArrayOf(21.toByte()))
                        } else {
                            println("Sending stop signal")
                            peripheral.write(characteristic, byteArrayOf(20.toByte()))
                        }
                    }
                }
            }
        }
    }


    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
        )
        val permission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                1
            )
        }
    }

    private fun backoff(
        base: Long,
        multiplier: Float,
        retry: Int,
    ): Long = (base * multiplier.pow(retry - 1)).toLong()

}
