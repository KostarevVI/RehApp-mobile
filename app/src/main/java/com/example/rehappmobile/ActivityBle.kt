package com.example.rehappmobile

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private var bluetoothGatt: BluetoothGatt? = null

//ТАм есть еще onMtuChanged(), пока скипнул, мб и не понадобится
class ActivityBle : AppCompatActivity() {
    private val mRetrofit = Retrofit.Builder()
        .baseUrl("http://192.168.43.151:5001")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    lateinit var pref: SharedPreferences

    private fun sendTraining(): Boolean {
        if (bluetoothGatt == null) {
            Log.e(ContentValues.TAG, "lost connection")
            return false
        }
        val Service: BluetoothGattService = bluetoothGatt!!.getService(UUID.fromString("6fff889d-c509-408f-9284-5aeefada3f4d"))
        if (Service == null) {
            Log.e(ContentValues.TAG, "service not found!")
            return false
        }
        val charac = Service
            .getCharacteristic(UUID.fromString("09aa0822-08df-42af-913c-428d0355e9b2"))
        if (charac == null) {
            Log.e(ContentValues.TAG, "char not found!")
            return false
        }
        val value = ByteArray(1)
        value[0] = 1.toByte()
        charac.value = value
        return bluetoothGatt!!.writeCharacteristic(charac)
    }



    fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isWritable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, payload: ByteArray) {
        val writeType = when {
            characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> error("Characteristic ${characteristic.uuid} cannot be written to")
        }

        bluetoothGatt?.let { gatt ->
            characteristic.writeType = writeType
            characteristic.value = payload
            gatt.writeCharacteristic(characteristic)
        } ?: error("Not connected to a BLE device!")
    }

    private fun fetchTraining() {
        val serviceAPI = mRetrofit.create(RehServerApi::class.java)
        val token = pref.getString(MainActivity.APP_PREFERENCES_TOKEN, "")
        val call = serviceAPI.getPatientTraining(
            token!!
        )
        call.enqueue(object : retrofit2.Callback<GetTrainingReport> {
            override fun onFailure(call: Call<GetTrainingReport>, t: Throwable) {
                Toast.makeText(
                    ActivityBle.bleAct.applicationContext,
                    t.message,
                    Toast.LENGTH_LONG
                )
                    .show()
            }

            override fun onResponse(
                call: Call<GetTrainingReport>,
                response: Response<GetTrainingReport>
            ) {
                if (response.body() != null) {
                    val getTrainingReport = response.body() as GetTrainingReport
                    val trainingData = getTrainingReport.data as TrainingData
                    if (getTrainingReport.status == "200") {
                        Toast.makeText(
                            ActivityBle.bleAct.applicationContext,
                            "Got training for you!",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } else if (getTrainingReport.status == "204") {
                        Toast.makeText(
                            ActivityBle.bleAct.applicationContext,
                            "No trainings",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } else {
                        Toast.makeText(
                            ActivityBle.bleAct.applicationContext,
                            "Something went wrong",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }

                } else {
                    Toast.makeText(
                        ActivityBle.bleAct.applicationContext,
                        "Error(",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        })
    }

    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            if (isScanning) {
                stopBleScan()
            }
            with(result.device) {
                Log.w("ScanResultAdapter", "Connecting to $address")
                bluetoothGatt = connectGatt(this@ActivityBle, false, gattCallback)
            }

        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    val bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.w(
                    "BluetoothGattCallback",
                    "Error $status encountered for $deviceAddress! Disconnecting..."
                )
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w(
                    "BluetoothGattCallback",
                    "Discovered ${services.size} services for ${device.address}"
                )
                // Consider connection setup as complete here
            }
        }

    }


    private fun setupRecyclerView() {
        findViewById<RecyclerView>(R.id.scan_results_recycler_view).apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@ActivityBle,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }
        val animator = findViewById<RecyclerView>(R.id.scan_results_recycler_view).itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }


    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread {
                findViewById<Button>(R.id.scan_button).text =
                    if (value) "Stop Scan" else "Start Scan"
            }
        }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        val fetch_button: Button = findViewById(R.id.fetch_button)
        fetch_button.setOnClickListener { fetchTraining() }

        val send_button: Button = findViewById(R.id.send_button)
        send_button.setOnClickListener { sendTraining() }

        val scan_button: Button = findViewById(R.id.scan_button)
        scan_button.setOnClickListener {
            if (isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }
        setupRecyclerView()
        bleAct = this
        pref = getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun startBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        } else {
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            requestPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startBleScan()
                }
            }
        }
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Log.i(
                        "ScanCallback",
                        "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address"
                    )
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

    companion object {
        private lateinit var bleAct: ActivityBle
    }

}

