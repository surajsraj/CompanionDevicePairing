package com.example.companiondevicepairing

import android.bluetooth.BluetoothDevice
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.DeviceFilter
import android.companion.WifiDeviceFilter
import android.content.IntentSender
import android.net.wifi.ScanResult
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.companiondevicepairing.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.util.concurrent.Executor


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val deviceManager: CompanionDeviceManager by lazy {
        getSystemService(COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    }

    private lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>
    private val viewModel: MainViewModel by viewModels()

    val executor: Executor = Executor { it.run() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.requestType.collect { requestType ->
                    if (requestType.isNotEmpty()) {
                        prepareDevicePairingList(requestType)
                    }
                }
            }
        }

        launcher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            when (result?.resultCode) {
                RESULT_OK -> {
                    // The user chose to pair the app with a Bluetooth device.

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val associationInfo: AssociationInfo? =
                            result.data?.getParcelableExtra(
                                CompanionDeviceManager.EXTRA_ASSOCIATION,
                                AssociationInfo::class.java
                            )
                        associationInfo?.let {
                            toastDeviceInfo(it, viewModel.requestType.value)
                        }
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun prepareDevicePairingList(requestType: String) {

        var pairingRequestBuilder = AssociationRequest.Builder()
            .setSingleDevice(false)

        when (requestType) {
            getString(R.string.ble) -> {
                val blDeviceFilter: DeviceFilter<BluetoothDevice> = BluetoothDeviceFilter.Builder()
                    .build()
                pairingRequestBuilder = pairingRequestBuilder.addDeviceFilter(blDeviceFilter)
            }

            getString(R.string.wifi) -> {
                val wifiDeviceFilter: DeviceFilter<ScanResult> = WifiDeviceFilter.Builder()
                    .build()
                pairingRequestBuilder = pairingRequestBuilder.addDeviceFilter(wifiDeviceFilter)
            }
        }
        var pairingRequest: AssociationRequest = pairingRequestBuilder.build()

        // When the app tries to pair with a Bluetooth/Wifi device, show the
        // corresponding dialog box to the user.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            deviceManager.associate(pairingRequest,
                executor,
                object : CompanionDeviceManager.Callback() {
                    // Called when a device is found. Launch the IntentSender so the user
                    // can select the device they want to pair with.
                    override fun onAssociationPending(intentSender: IntentSender) {
                        requestAssociation(intentSender, requestType)
                    }

                    override fun onAssociationCreated(associationInfo: AssociationInfo) {
                        // AssociationInfo object is created and get association id and the
                        // macAddress.
                        toastDeviceInfo(associationInfo, requestType)
                    }

                    override fun onFailure(errorMessage: CharSequence?) {
                        // Handle the failure.
                        Log.e("MainActivity", "Device Pairing Failure : " + errorMessage.toString())
                    }
                })
        } else {
            deviceManager.associate(
                pairingRequest,
                object : CompanionDeviceManager.Callback() {

                    override fun onDeviceFound(chooserLauncher: IntentSender) {
                        requestAssociation(chooserLauncher, requestType)
                    }

                    override fun onFailure(error: CharSequence?) {
                        // Handle the failure.
                        Log.e("MainActivity", "Device Pairing Failure : " + error.toString())
                    }
                }, null
            )
        }
    }

    private fun requestAssociation(intentSender: IntentSender, requestType: String) {
        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
    }

    private fun toastDeviceInfo(associationInfo: AssociationInfo, requestType: String) {
        when (requestType) {
            getString(R.string.ble) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    associationInfo.associatedDevice?.bluetoothDevice?.let { device ->
                        Toast.makeText(
                            this,
                            device.address,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            getString(R.string.wifi) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    associationInfo.associatedDevice?.wifiDevice?.let { device ->
                        Toast.makeText(
                            this,
                            "${device.wifiSsid}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}