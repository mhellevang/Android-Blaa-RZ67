package dev.hellevang.androidblaarz67

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
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
import dev.hellevang.androidblaarz67.ui.theme.AndroidBlaaRZ67Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : ComponentActivity() {
    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var takePermission: ActivityResultLauncher<String>
    lateinit var takeResultLauncher: ActivityResultLauncher<Intent>
    lateinit var mConnectThread: ConnectThread
    lateinit var mConnectedThread: ConnectedThread
    var bluetoothDevice by mutableStateOf<BluetoothDevice?>(null)

    companion object {
        var connectionState by mutableStateOf("Not connected")
        var mState = mutableStateOf(0) //
        const val STATE_NONE = 0 // we're doing nothing
        const val STATE_CONNECTING = 2 // now initiating an outgoing connection
        const val STATE_CONNECTED = 3 // now connected to a remote device
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        takePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
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
        takeResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(applicationContext, "Bluetooth ON", Toast.LENGTH_LONG).show()

            } else {
                Toast.makeText(applicationContext, "Bluetooth Off", Toast.LENGTH_LONG).show()
            }
        }
        enableBluetooth()
        connect()

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

    private fun connect() {
        if (this::mConnectThread.isInitialized) {
            mConnectThread.cancel()
        }
        mConnectThread = ConnectThread()
        mConnectThread.start()
    }


    private fun connected(mmSocket: BluetoothSocket) {
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(mmSocket)
        mConnectedThread.start()
    }

    private fun connectionLost() {
        connectionState = "Connection Lost"
        mState = mutableStateOf(STATE_NONE)
        mConnectedThread.cancel()
        connect()
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

    private fun findPairedDevices(name: String): BluetoothDevice? {
        // Find all paired devices
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val pairedDevices = bluetoothAdapter.bondedDevices
            val find = pairedDevices.find { it.name == name }
            if (find != null) {
                return find
            }
        }
        return null
    }

    private fun enableBluetooth() {
        takePermission.launch(Manifest.permission.BLUETOOTH_CONNECT) // Setup bluetooth
    }

    @Composable
    fun ToggleButton(toggleButton: () -> Unit, modifier: Modifier = Modifier, text: String) {
        Button(onClick = { toggleButton() }, modifier = modifier) { Text(text) }
    }

    @Composable
    fun TriggerButtonPanel() {
        var triggerType by remember { mutableStateOf(TriggerType.Direct) }
        var showCountDownTimer by remember { mutableStateOf(false) }

        fun triggerCountdownShutter() {
            triggerShutter()
            showCountDownTimer = false
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
                if (showCountDownTimer) {
                    StartTimer(::triggerCountdownShutter)
                } else {
                    Text(
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .padding(start = 16.dp, end = 16.dp),
                        text = "Press to start countdown"
                    )
                }
            }
        }

        Button(
            onClick =
            {
                if (triggerType == TriggerType.Direct) {
                    triggerShutter()
                } else {
                    showCountDownTimer = !showCountDownTimer
                }
            },
            modifier = Modifier
                .padding(top = 50.dp),
            enabled = this::mConnectedThread.isInitialized,
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
            text = "$timeLeftMs ms left")
        when (timeLeftMs) {
            0L -> {
                Toast.makeText(applicationContext, "Take picture", Toast.LENGTH_SHORT).show()
                done()
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

    private fun triggerShutter() {
        mConnectedThread.write(byteArrayOf(10.toByte()))
    }

    @SuppressLint("MissingPermission")
    inner class ConnectThread : Thread() {
        private lateinit var mmSocket: BluetoothSocket

        /**
         * Warning! THIS UUID IS NOT RANDOM!
         *
         * Even though the android docs state:
         * "To get a UUID to use with your app, you can use one of the many random UUID generators
         *  on the web, then initialize a UUID with fromString(String)."
         *
         *  This is a lie. Do not change.
         */
        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        override fun run() {

            mState = mutableStateOf(STATE_CONNECTING)

            connectionState = "Looking for RZ67..."
            var count = 0

            while (mState.value != STATE_CONNECTED) {
                count++
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    bluetoothDevice = findPairedDevices("RZ67 Blaa").also {
                        bluetoothAdapter.cancelDiscovery()
                    }

                    bluetoothDevice?.createRfcommSocketToServiceRecord(uuid)?.let {
                        mmSocket = it
                        mmSocket.connect()
                        connected(mmSocket)
                    }
                } catch (connectException: IOException) {
                    // Unable to connect; close the socket and return.
                    try {
                        mmSocket.close()
                    } catch (closeException: IOException) {

                    }
                }
                if (count > 10) {
                    connectionState = "Can't connect. Is the trigger paired to the phone?"
                }
                sleep(100)
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {

            }
        }

    }

    @SuppressLint("MissingPermission")
    inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private val mmSocket: BluetoothSocket
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
            mState = mutableStateOf(STATE_CONNECTED)
            connectionState = "Connected to ${bluetoothDevice?.name}"
        }

        override fun run() {
            val buffer = ByteArray(1024) // buffer store for the stream
            var bytes: Int? // bytes returned from read()

            // Keep listening to the InputStream while connected
            while (mState.value == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream!!.read(buffer)
                    // TODO

                } catch (e: IOException) {
                    connectionLost()
                    break
                }
            }
        }

        fun write(bytes: ByteArray?) {
            try {
                mmOutStream?.write(bytes)
            } catch (e: IOException) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
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

}
