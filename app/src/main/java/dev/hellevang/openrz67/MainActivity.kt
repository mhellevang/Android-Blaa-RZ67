package dev.hellevang.openrz67

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hellevang.openrz67.bluetooth.BluetoothInitializer
import dev.hellevang.openrz67.bluetooth.BluetoothManager
import dev.hellevang.openrz67.permissions.PermissionManager
import dev.hellevang.openrz67.ui.components.HeaderComponent
import dev.hellevang.openrz67.ui.components.TriggerButtonPanel
import dev.hellevang.openrz67.ui.theme.Colors
import dev.hellevang.openrz67.ui.theme.Dimens
import dev.hellevang.openrz67.ui.theme.OpenRZ67Theme
import dev.hellevang.openrz67.viewmodel.TriggerControlViewModel
import kotlinx.coroutines.MainScope


class MainActivity : ComponentActivity() {
    private val scope = MainScope()
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var bluetoothInitializer: BluetoothInitializer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set system UI colors to match theme
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Colors.StatusBarColor.toColorInt()
        window.navigationBarColor = Colors.NavigationBarColor.toColorInt()
        
        permissionManager = PermissionManager(this)
        bluetoothInitializer = BluetoothInitializer(this)
        
        permissionManager.checkAndRequestPermissions()
        bluetoothInitializer.initialize()
        
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
        val viewModel: TriggerControlViewModel = if (::bluetoothManager.isInitialized) {
            viewModel { TriggerControlViewModel(bluetoothManager) }
        } else {
            return // Don't render if Bluetooth manager isn't ready
        }
        
        val connectionState by viewModel.connectionState.collectAsState()
        val isConnected by viewModel.isConnected.collectAsState()

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
            HeaderComponent()
            Spacer(modifier = Modifier.padding(top = Dimens.TopSectionPadding))

            // Debug
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.TopSectionPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(connectionState, color = MaterialTheme.colors.onBackground)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = Dimens.TopSectionPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TriggerButtonPanel(viewModel, isConnected)
            }
        }
    }





    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        OpenRZ67Theme {
            // Preview version without ViewModel dependencies
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                HeaderComponent()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::bluetoothManager.isInitialized) {
            bluetoothManager.cleanup()
        }
    }

}
