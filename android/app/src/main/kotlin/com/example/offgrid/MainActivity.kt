package com.example.offgrid

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.UUID

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.offgrid/ble_advertising"
    
    // BLE Constants
    private val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CLIENT_CHAR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    
    // BLE State
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeAdvertiser: Any? = null
    private var bluetoothGattServer: BluetoothGattServer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "startAdvertising" -> {
                        val deviceName = call.argument<String>("deviceName")
                        if (deviceName != null) {
                            startBleAdvertising(deviceName)
                            result.success("Advertising started with name: $deviceName")
                        } else {
                            result.error("INVALID_ARGS", "deviceName is required", null)
                        }
                    }
                    "stopAdvertising" -> {
                        stopBleAdvertising()
                        result.success("Advertising stopped")
                    }
                    else -> result.notImplemented()
                }
            }
        
        // Initialize GATT Server
        initializeGattServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBleAdvertising()
        stopGattServer()
    }

    private fun initializeGattServer() {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter == null) {
                android.util.Log.e("BLE", "Bluetooth adapter is null - GATT server cannot start")
                return
            }

            // Create GATT Server
            bluetoothGattServer = bluetoothManager?.openGattServer(this, GattServerCallback())
            
            if (bluetoothGattServer == null) {
                android.util.Log.e("BLE", "Failed to create GATT server")
                return
            }

            // Create and add service
            val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            
            // Create characteristic with READ/WRITE/NOTIFY properties
            val characteristic = BluetoothGattCharacteristic(
                CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            // Add Client Characteristic Configuration descriptor (for notifications)
            val descriptor = BluetoothGattDescriptor(
                CLIENT_CHAR_CONFIG,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            characteristic.addDescriptor(descriptor)

            service.addCharacteristic(characteristic)
            bluetoothGattServer?.addService(service)
            
            android.util.Log.i("BLE", "✓ GATT Server initialized with Nordic UART service")
            android.util.Log.i("BLE", "✓ Service UUID: $SERVICE_UUID")
            android.util.Log.i("BLE", "✓ Characteristic UUID: $CHAR_UUID")
        } catch (e: Exception) {
            android.util.Log.e("BLE", "Error initializing GATT server: ${e.message}")
        }
    }

    private fun startBleAdvertising(deviceName: String) {
        try {
            if (bluetoothAdapter == null) {
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                bluetoothAdapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            }

            if (bluetoothAdapter == null) {
                android.util.Log.e("BLE", "Bluetooth adapter is null")
                return
            }

            // Set the device name
            bluetoothAdapter?.name = deviceName
            android.util.Log.i("BLE", "Device name set to: $deviceName")

            // Start BLE advertising (API 21+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
                if (advertiser != null) {
                    val settings = AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                        .setConnectable(true)
                        .setTimeout(0) // Advertise indefinitely
                        .build()

                    val data = AdvertiseData.Builder()
                        .setIncludeDeviceName(true)
                        .build()

                    advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
                        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                            android.util.Log.i("BLE", "✓ BLE advertising started successfully")
                        }

                        override fun onStartFailure(errorCode: Int) {
                            android.util.Log.e("BLE", "✗ Failed to start BLE advertising. Error code: $errorCode")
                        }
                    })
                    bluetoothLeAdvertiser = advertiser
                } else {
                    android.util.Log.w("BLE", "BLE advertising not supported on this device")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BLE", "Error starting BLE advertising: ${e.message}")
        }
    }

    private fun stopBleAdvertising() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val advertiser = bluetoothLeAdvertiser as? android.bluetooth.le.BluetoothLeAdvertiser
                if (advertiser != null) {
                    advertiser.stopAdvertising(object : AdvertiseCallback() {
                        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {}
                        override fun onStartFailure(errorCode: Int) {}
                    })
                    android.util.Log.i("BLE", "BLE advertising stopped")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BLE", "Error stopping BLE advertising: ${e.message}")
        }
    }

    private fun stopGattServer() {
        try {
            bluetoothGattServer?.close()
            android.util.Log.i("BLE", "GATT server stopped")
        } catch (e: Exception) {
            android.util.Log.e("BLE", "Error stopping GATT server: ${e.message}")
        }
    }

    // GATT Server Callback - handles incoming connections and data
    private inner class GattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: android.bluetooth.BluetoothDevice?, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                android.util.Log.i("BLE", "✓✓✓ CLIENT CONNECTED: ${device?.address} (${device?.name})")
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                android.util.Log.i("BLE", "✗ CLIENT DISCONNECTED: ${device?.address}")
            }
        }

        override fun onCharacteristicReadRequest(
            device: android.bluetooth.BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            android.util.Log.d("BLE", "READ request from ${device?.address}")
            if (characteristic?.uuid == CHAR_UUID) {
                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    android.bluetooth.BluetoothGatt.GATT_SUCCESS,
                    offset,
                    characteristic.value
                )
            }
        }

        override fun onCharacteristicWriteRequest(
            device: android.bluetooth.BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (characteristic?.uuid == CHAR_UUID && value != null) {
                android.util.Log.i("BLE", "✓ RECEIVED DATA: ${value.size} bytes from ${device?.address}")
                characteristic.value = value
            }

            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    android.bluetooth.BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )
            }
        }

        override fun onNotificationSent(device: android.bluetooth.BluetoothDevice?, status: Int) {
            android.util.Log.d("BLE", "Notification sent to ${device?.address}, status=$status")
        }

        override fun onDescriptorReadRequest(
            device: android.bluetooth.BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            if (descriptor?.uuid == CLIENT_CHAR_CONFIG) {
                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    android.bluetooth.BluetoothGatt.GATT_SUCCESS,
                    offset,
                    byteArrayOf(0x01, 0x00)
                )
            }
        }

        override fun onDescriptorWriteRequest(
            device: android.bluetooth.BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            android.util.Log.d("BLE", "Descriptor write from ${device?.address}")
            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    android.bluetooth.BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )
            }
        }
    }
}
