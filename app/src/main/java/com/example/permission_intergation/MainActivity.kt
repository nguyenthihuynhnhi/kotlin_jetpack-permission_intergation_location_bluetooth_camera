package com.example.permission_intergation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PermissionIntegrateApp()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PermissionIntegrateApp() {
        val context = LocalContext.current

        // Trạng thái của các quyền
        val locationGranted = remember { mutableStateOf(false) }
        val bluetoothGranted = remember { mutableStateOf(false) }
        val cameraGranted = remember { mutableStateOf(false) }

        // Bluetooth Manager và Adapter
        val bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        // Launcher cho quyền Camera
        val cameraLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            cameraGranted.value = granted

            if (!granted) {
                Toast.makeText(context, "Camera permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

        // Launcher cho quyền Bluetooth
        val bluetoothLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.BLUETOOTH] == true &&
                    permissions[Manifest.permission.BLUETOOTH_ADMIN] == true &&
                    (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
                            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true)

            bluetoothGranted.value = granted

            if (granted) {
                enableBluetoothIfNeeded(bluetoothAdapter)
                cameraLauncher.launch(Manifest.permission.CAMERA)
            } else {
                Toast.makeText(context, "Bluetooth permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

        // Launcher cho quyền Location
        val locationLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            locationGranted.value = granted

            if (granted) {
                // Nếu quyền Location được cấp, yêu cầu quyền Bluetooth
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    bluetoothLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                } else {
                    bluetoothLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN
                        )
                    )
                }
            } else {
                Toast.makeText(context, "Location permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Permission Integrate") }) },
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = {
                        // Yêu cầu quyền Location
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }) {
                        Text("Request Permissions")
                    }

                    Text(
                        text = "Location: ${if (locationGranted.value) "Granted" else "Not Granted"}\n" +
                                "Bluetooth: ${if (bluetoothGranted.value) "Granted" else "Not Granted"}\n" +
                                "Camera: ${if (cameraGranted.value) "Granted" else "Not Granted"}"
                    )
                }
            }
        )
    }

    private fun enableBluetoothIfNeeded(bluetoothAdapter: BluetoothAdapter) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (!bluetoothAdapter.isEnabled) {
                    val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivity(enableBluetoothIntent)
                }
            }
        } else {
            if (!bluetoothAdapter.isEnabled) {
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(enableBluetoothIntent)
            }
        }
    }
}