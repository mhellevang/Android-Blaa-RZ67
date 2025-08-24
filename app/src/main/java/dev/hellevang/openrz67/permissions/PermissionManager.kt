package dev.hellevang.openrz67.permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class PermissionManager(private val activity: Activity) {
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_PRIVILEGED
    )
    
    fun checkAndRequestPermissions() {
        val permission = ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.BLUETOOTH_SCAN
        )
        
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                requiredPermissions,
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    fun hasBluetoothPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }
}