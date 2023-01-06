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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

class MainActivity : ComponentActivity() {
    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var takePermission: ActivityResultLauncher<String>
    lateinit var takeResultLauncher: ActivityResultLauncher<Intent>
    lateinit var mConnectedThread: ConnectedThread

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
                contentScale = ContentScale.Crop
            )
        }
        enableBlueTooth();
        val bluetoothDevice: BluetoothDevice? = findPairedDevices("RZ67 Blaa")

        Column {
            HeaderText()
            Spacer(modifier = Modifier.padding(top = 25.dp))

            // Debug
            Column {
                if (bluetoothDevice != null) {
                    Button(onClick = {
                        val connectThread = ConnectThread(bluetoothDevice)
                        connectThread.run()

                    }) {
                        Text("Connect to ${bluetoothDevice.name}", color = Color.Red)
                    }
                    Text("Paired devices: ${bluetoothDevice?.name}", color = Color.LightGray)
                }
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
            var pairedDevices = bluetoothAdapter.bondedDevices
            val find = pairedDevices.find { it.name == name }
            if (find == null) {
                Toast.makeText(applicationContext, "No Bluetooth Device Found", Toast.LENGTH_SHORT)
                    .show()
            } else {
                return find
            }
        }
        return null
    }

    private fun enableBlueTooth() {
        takePermission.launch(Manifest.permission.BLUETOOTH_CONNECT) // Setup bluetooth
    }

    @Composable
    fun ToggleButton(toggleButton: () -> Unit, modifier: Modifier = Modifier, text: String) {
        Button(onClick = { toggleButton() }, modifier = modifier) { Text(text) }
    }

    @Composable
    fun TriggerButtonPanel() {
        var triggerType by remember { mutableStateOf(TriggerType.Direct) }
        Row(
            modifier = Modifier.padding(top = 100.dp)
        ) {
            ToggleButton(toggleButton = { triggerType = TriggerType.Direct }, text = "Direct shot")
            ToggleButton(
                toggleButton = { triggerType = TriggerType.Countdown },
                modifier = Modifier.padding(start = 25.dp),
                text = "Countdown"
            )
        }
        Text(
            fontSize = 24.sp,
            color = Color.LightGray,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(start = 16.dp, end = 16.dp),
            text = "Mode: ${triggerType.name}"
        )
        Button(
            onClick = {
                val action = when (triggerType) {
                    TriggerType.Direct -> 11
                    TriggerType.Countdown -> 20
                }
                mConnectedThread.write(byteArrayOf(action.toByte())) },
            modifier = Modifier
                .padding(top = 150.dp),
            enabled = this::mConnectedThread.isInitialized
        ) {
            Text(text = "Trigger shutter")
        }
    }

    enum class TriggerType {
        Direct,
        Countdown
    }


    @Composable
    fun HeaderText() {
        Text(
            text = "Android Blaa RZ67",
            fontSize = 36.sp,
            color = Color.LightGray,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(start = 10.dp, top = 24.dp)
        )
        Text(
            text = "A Mamiya RZ67 bluetooth trigger",
            fontSize = 24.sp,
            color = Color.LightGray,
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

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

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

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createInsecureRfcommSocketToServiceRecord(uuid)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    socket.connect()
                    connected(mmSocket!!)
                } catch (e: IOException) {
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                }
                Toast.makeText(
                    applicationContext,
                    "Socket is connected: " + socket.isConnected,
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                Toast.makeText(applicationContext, "foo", Toast.LENGTH_SHORT).show()
                mmSocket?.close()
            } catch (e: IOException) {

            }
        }

        private fun connected(mmSocket: BluetoothSocket) {
            // Start the thread to manage the connection and perform transmissions
            mConnectedThread = ConnectedThread(mmSocket)
            mConnectedThread.start()
        }
    }

    class ConnectedThread(socket: BluetoothSocket) : Thread() {
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
        }

        override fun run() {
            val buffer = ByteArray(1024) // buffer store for the stream
            var bytes: Int? // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = mmInStream?.read(buffer)
                    val incomingMessage = bytes?.let { String(buffer, 0, it) }
                    // TODO
                } catch (e: IOException) {
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
