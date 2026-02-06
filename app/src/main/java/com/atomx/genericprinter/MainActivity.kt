package com.atomx.genericprinter

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private lateinit var listView: ListView
    private lateinit var btnPrint: Button

    private val deviceList = mutableListOf<BluetoothDevice>()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ensureBluetoothPermissions()

        btnPrint = findViewById(R.id.btnPrint)
        listView = findViewById(R.id.listViewDevices)

        btnPrint.setOnClickListener {
            val device = getAutoPrinterDevice()

            if (device != null) {
                printToDevice(device)
            } else {
                showBondedDevices()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            printToDevice(device)
        }
    }

    /**
     * 1️⃣ Try connected
     * 2️⃣ Try bonded
     */
    @SuppressLint("MissingPermission")
    private fun getAutoPrinterDevice(): BluetoothDevice? {
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is OFF", Toast.LENGTH_SHORT).show()
            return null
        }

        // Try CONNECTED device (best guess)
        bluetoothAdapter.bondedDevices.firstOrNull {
            it.bondState == BluetoothDevice.BOND_BONDED
        }?.let {
            return it
        }

        return null
    }

    /**
     * Show bonded devices in ListView
     */
    @SuppressLint("MissingPermission")
    private fun showBondedDevices() {
        val bonded = bluetoothAdapter.bondedDevices

        if (bonded.isEmpty()) {
            Toast.makeText(this, "No paired Bluetooth devices found", Toast.LENGTH_LONG).show()
            return
        }

        deviceList.clear()
        deviceList.addAll(bonded)

        val names = bonded.map {
            "${it.name ?: "Unknown"}\n${it.address}"
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            names
        )

        listView.adapter = adapter
        listView.visibility = ListView.VISIBLE

        Toast.makeText(this, "Select printer from list", Toast.LENGTH_SHORT).show()
    }

    /**
     * Print using selected device
     */
    @SuppressLint("MissingPermission")
    private fun printToDevice(device: BluetoothDevice) {
        Toast.makeText(this, "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()

        val printer = PrinterFactory.bluetooth(
            mac = device.address,
            config = PrinterConfig(paperWidthPx = 384, debug = true)
        )

        if (!printer.connect()) {
            Toast.makeText(this, "Failed to connect ${device.name}", Toast.LENGTH_LONG).show()
            return
        }

        try {
            printer.reset()
            printer.printText("Hello from AtomX")
            val bmp = BitmapFactory.decodeResource(resources, R.drawable.ali)
            printer.printBitmap(bmp, center = true)
            printer.feed(3)
            printer.cut()
        } catch (e: Exception) {
            Toast.makeText(this, "Print error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            printer.disconnect()
        }
    }

    /**
     * Permissions
     */
    private fun ensureBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val perms = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            val need = perms.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (need.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, need.toTypedArray(), 101)
            }
        }
    }
}
