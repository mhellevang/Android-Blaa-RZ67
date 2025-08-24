package dev.hellevang.openrz67

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import dev.hellevang.openrz67.ui.theme.OpenRZ67Theme
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import dev.hellevang.openrz67.bluetooth.BluetoothManager


class MainActivity : ComponentActivity() {
    private val scope = MainScope()
    private lateinit var bluetoothManager: BluetoothManager
    private var countdownJob: Job? = null

    companion object {
        var startDelayedTrigger by mutableStateOf(false)
        var countdownTimeLeft by mutableStateOf(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set system UI colors to match theme
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = "#FBE7C9".toColorInt()
        window.navigationBarColor = "#FBE7C9".toColorInt()
        
        checkPermissions()
        initializeBluetooth()
        
        bluetoothManager = BluetoothManager(this, scope)
        bluetoothManager.initialize()

        setContent {
            OpenRZ67Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    OpenRZ67App()
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    @Composable
    fun OpenRZ67App() {
        val connectionState by if (::bluetoothManager.isInitialized) {
            bluetoothManager.connectionState.collectAsState()
        } else {
            remember { mutableStateOf("Initializing...") }
        }
        val isConnected by if (::bluetoothManager.isInitialized) {
            bluetoothManager.isConnected.collectAsState()
        } else {
            remember { mutableStateOf(false) }
        }

        val image =
            painterResource(R.drawable.d3fe691b34130991a5bf05a25d54d74300316eaff150963be736948feb5ec159)
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

        Column(
            modifier = Modifier.statusBarsPadding()
        ) {
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
                Text(connectionState, color = MaterialTheme.colors.onBackground)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TriggerButtonPanel(isConnected)
            }
        }
    }


    @Composable
    fun ToggleButton(toggleButton: () -> Unit, modifier: Modifier = Modifier, text: String) {
        Button(onClick = { toggleButton() }, modifier = modifier) { Text(text) }
    }

    @Composable
    fun TriggerButtonPanel(isConnected: Boolean) {
        var triggerType by remember { mutableStateOf(TriggerType.Direct) }

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
            color = MaterialTheme.colors.onBackground,
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
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .padding(start = 16.dp, end = 16.dp),
                    text = "Press the button to take a picture"
                )
            }
            TriggerType.Countdown -> {
                if (startDelayedTrigger) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            fontSize = 18.sp,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                                .padding(start = 16.dp, end = 16.dp),
                            text = "Arduino countdown in progress..."
                        )
                        if (countdownTimeLeft > 0) {
                            Text(
                                fontSize = 32.sp,
                                color = MaterialTheme.colors.secondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                                    .padding(top = 8.dp),
                                text = "$countdownTimeLeft"
                            )
                        }
                    }
                } else {
                    Text(
                        fontSize = 18.sp,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .padding(start = 16.dp, end = 16.dp),
                        text = "Press to start 10s countdown on Arduino"
                    )
                }
            }
        }

        Button(
            onClick =
            {
                if (::bluetoothManager.isInitialized) {
                    if (triggerType == TriggerType.Direct) {
                        bluetoothManager.sendSignal(BluetoothManager.SignalType.Trigger)
                    } else {
                        startDelayedTrigger = if (startDelayedTrigger) {
                            // Cancel Arduino countdown
                            bluetoothManager.sendSignal(BluetoothManager.SignalType.ArduinoCountdown, false)
                            stopCountdownTimer()
                            false
                        } else {
                            // Start Arduino countdown
                            bluetoothManager.sendSignal(BluetoothManager.SignalType.ArduinoCountdown, true)
                            startCountdownTimer()
                            true
                        }
                    }
                }
            },
            modifier = Modifier
                .padding(top = 50.dp),
            enabled = isConnected,
        )
        {
            Text(text = "Trigger shutter", modifier = Modifier.padding(end = 10.dp))
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = null,
                tint = MaterialTheme.colors.onPrimary
            )
        }

    }

    enum class TriggerType {
        Direct,
        Countdown
    }


    @Composable
    fun HeaderText() {
        Text(
            text = "OpenRZ67",
            fontSize = 36.sp,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(start = 10.dp, top = 24.dp)
        )
        Text(
            text = "A Mamiya RZ67 bluetooth trigger",
            fontSize = 24.sp,
            color = MaterialTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        OpenRZ67Theme {
            OpenRZ67App()
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

    private fun initializeBluetooth() {
        val bluetoothSystemManager = getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val bluetoothAdapter = bluetoothSystemManager.adapter
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
    }

    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownTimeLeft = 10
        countdownJob = scope.launch {
            repeat(10) {
                delay(1000)
                countdownTimeLeft--
            }
            // Countdown finished
            startDelayedTrigger = false
            countdownTimeLeft = 0
        }
    }

    private fun stopCountdownTimer() {
        countdownJob?.cancel()
        countdownTimeLeft = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bluetoothManager.isInitialized) {
            bluetoothManager.cleanup()
        }
    }

}
