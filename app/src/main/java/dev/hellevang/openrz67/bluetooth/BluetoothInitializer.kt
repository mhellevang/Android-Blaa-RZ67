package dev.hellevang.openrz67.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class BluetoothInitializer(private val activity: ComponentActivity) {
    
    fun initialize() {
        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        val takePermission = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                if (bluetoothAdapter?.isEnabled != true) {
                    // Android 12+ - guide user to Settings
                    Toast.makeText(
                        activity.applicationContext,
                        "Please enable Bluetooth in Settings",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    activity.startActivity(intent)
                } else {
                    Toast.makeText(activity.applicationContext, "Bluetooth is already enabled", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(
                    activity.applicationContext,
                    "Bluetooth Permission is not Granted",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        
        takePermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }
}