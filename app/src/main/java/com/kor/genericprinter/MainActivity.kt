package com.kor.genericprinter

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atomx.blutox.DiscoverabilityReceiver
import com.kor.genericprinter.adapters.BluetoothDeviceAdapter
import com.kor.genericprinter.databinding.ActivityMainBinding
import com.kor.genericprinter.listeners.OnItemClickListener
import com.kor.genericprinter.receivers.BluetoothReceiver
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mBluetoothManager: BluetoothManager
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothDevicesList: MutableList<BluetoothDevice>
    private lateinit var mBluetoothDeviceRecyclerView: RecyclerView

    lateinit var receiver1: BluetoothReceiver
    lateinit var receiver2: DiscoverabilityReceiver

    private lateinit var context: Context

    val PERMISSION_REQUEST_CODE = 101
    private val TAG = "main activity"

    //Constants for the Handler
    val STATE_LISTENING = 1
    val STATE_CONNECTING = 2
    val STATE_CONNECTED = 3
    val STATE_CONNECTION_FAILED = 4
    val STATE_MESSAGE_RECEIVED = 5

    //Constants for App Name and UUID
    val APP_NAME = "Generic-Printer"
    //val MY_UUID = UUID.fromString("ac2ebe84-e062-11e8-9f32-f2801f1b9fd1")
    val MY_UUID = UUID.fromString("2e70b6e2-3823-4273-a91f-948bf64bd632")
    private var MY_UUID2: UUID? = null

    //Reference variable for SendReceive object
    private lateinit var sendReceive: SendReceive

    private lateinit var adapter: BluetoothDeviceAdapter

    //handler to get connection status
    private val handler = Handler(Handler.Callback { msg ->
        when (msg.what) {
            STATE_LISTENING -> {
                mBinding.tvStatus.text = "Listening"
            }

            STATE_CONNECTING -> {
                mBinding.tvStatus.text = "Connecting"
            }

            STATE_CONNECTED -> {
                mBinding.tvStatus.text = "Connected"
            }

            STATE_CONNECTION_FAILED -> {
                mBinding.tvStatus.text = "Connection Failed"
            }

            STATE_MESSAGE_RECEIVED -> {
                val readBuff = msg.obj as ByteArray
                val tempMsg = String(readBuff, 0, msg.arg1)
                mBinding.tvMessage.text = tempMsg
                Toast.makeText(this@MainActivity, "message is: $tempMsg", Toast.LENGTH_SHORT).show()


            }

        }
        true
    })

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(mBinding.root)

        context = this@MainActivity

        //check bluetooth permission over android 13
        checkBluetoothPermissions()

        MY_UUID2 = generateBluetoothUUID()
        Log.d("uuid", "${MY_UUID2.toString()}")

        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        //mBluetoothAdapter = mBluetoothManager.adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        mBluetoothDeviceRecyclerView = mBinding.rvPairedDevices
        mBluetoothDevicesList = mutableListOf()

        receiver1 = BluetoothReceiver()
        receiver2 = DiscoverabilityReceiver()

        enableDisableBluetooth()

        mBinding.btnDiscoverable.setOnClickListener {
            discoverability()
        }

        val layoutManager = LinearLayoutManager(this)
        adapter = BluetoothDeviceAdapter(mBluetoothDevicesList, this)
        mBluetoothDeviceRecyclerView.layoutManager = layoutManager
        mBluetoothDeviceRecyclerView.adapter = adapter

        initializeListeners()

        mBinding.btnPrint.setOnClickListener {
            Toast.makeText(this@MainActivity,"Printing is in processing", Toast.LENGTH_SHORT).show()
        }

    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()

        //get paired devices and set in recycler view
        mBluetoothDevicesList.clear()
        mBluetoothDevicesList.addAll(getPairedBluetoothDevices())
        adapter.notifyDataSetChanged()

        //start server class / ready to listen
        val serverClass = ServerClass()
        serverClass.start()

        //enable bluetooth pop up
        if (!mBluetoothAdapter.isEnabled) {
            mBluetoothAdapter.enable()
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(intent)

            val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(receiver1, intentFilter)
        }

    }

    //initialize click listeners
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initializeListeners() {

        mBinding.btnPairedDevices.setOnClickListener {
            mBluetoothDevicesList.clear()
            mBluetoothDevicesList.addAll(getPairedBluetoothDevices())
            adapter.notifyDataSetChanged()
        }

        //listener
        mBinding.btnListenDevices.setOnClickListener {
            val serverClass = ServerClass()
            serverClass.start()
        }

        //send message
        mBinding.btnSend.setOnClickListener {
            if (mBinding.etMessageBox.text.isEmpty()) {
                Toast.makeText(this, "Please Enter Message", Toast.LENGTH_SHORT).show()
            } else {
                val message = mBinding.etMessageBox.text.toString()
                //mBinding.etMessageBox.text.clear()
                sendReceive.write(message.toByteArray())
            }
        }
    }

    // Check and request Bluetooth permissions
    @RequiresApi(Build.VERSION_CODES.S)
    private val bluetoothPermissions = arrayOf(
        BLUETOOTH,
        BLUETOOTH_ADMIN,
        BLUETOOTH_CONNECT
    )

    //bluetooth permission over android 13
    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkBluetoothPermissions(): Boolean {
        var permission = false
        val permissionsToRequest = ArrayList<String>()

        for (permission in bluetoothPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
            permission = true
        } else {
            Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show()
        }
        return permission
    }

    //Handle permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "please allow permission", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //bluetooth turn on off
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    private fun enableDisableBluetooth() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED -> {
                //permission granted
                Toast.makeText(this@MainActivity, "Permission Granted!", Toast.LENGTH_SHORT).show()
            }

            shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_CONNECT) -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),
                    101
                )
            }
        }
        mBinding.btnTurnOnOf.setOnClickListener {
            if (!mBluetoothAdapter.isEnabled) {
                mBluetoothAdapter.enable()
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(intent)

                val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(receiver1, intentFilter)
            }
            if (mBluetoothAdapter.isEnabled) {
                mBluetoothAdapter.disable()

                val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(receiver1, intentFilter)
            }
        }

    }


    //discoverability for other device to connect
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun discoverability() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30)
        startActivity(discoverableIntent)

        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        registerReceiver(receiver2, intentFilter)
    }


    //to get paired device list
    @SuppressLint("MissingPermission")
    fun getPairedBluetoothDevices(): List<BluetoothDevice> {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        val pairedDeviceList: MutableList<BluetoothDevice> = mutableListOf()

        for (device in pairedDevices) {
            pairedDeviceList.add(device)
        }

        return pairedDeviceList
    }

    //Thread for the Server class
    @SuppressLint("MissingPermission")
    inner class ServerClass : Thread() {
        //Reference variable for BluetoothServerSocket object
        private lateinit var bluetoothServerSocket: BluetoothServerSocket

        init {
            try {
                //Initialisation of BluetoothServerSocket object
                bluetoothServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    APP_NAME, MY_UUID
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun run() {
            var bluetoothSocket: BluetoothSocket? = null
            while (bluetoothSocket == null) {
                try {
                    val message = Message.obtain()
                    message.what = STATE_CONNECTING
                    handler.sendMessage(message)

                    //Listen for the connection
                    bluetoothSocket = bluetoothServerSocket.accept()
                } catch (e: IOException) {
                    e.printStackTrace()
                    val message = Message.obtain()
                    message.what = STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                    break
                }
                /*If the connection is established, we will get a socket which will be used to
                send and receive the messages*/
                if (bluetoothSocket != null) {
                    val message = Message.obtain()
                    message.what = STATE_CONNECTED
                    handler.sendMessage(message)
                    //Do something for Send/Receive
                    sendReceive = SendReceive(bluetoothSocket)
                    sendReceive.start()
                    break
                }
            }
        }
    }

    //Thread for the client class
    @SuppressLint("MissingPermission")
    inner class ClientClass(device: BluetoothDevice) : Thread() {
        private lateinit var bluetoothSocket: BluetoothSocket
        private var bluetoothDevice: BluetoothDevice = device

        init {
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun run() {
            try {
                bluetoothSocket.connect()
                val message = Message.obtain()
                val messageText = mBinding.etMessageBox.text.toString()
                message.what = STATE_CONNECTED
                message.obj = messageText // Set the text message as the message object

                handler.sendMessage(message)

                // Do something for Send/Receive
                sendReceive = SendReceive(bluetoothSocket)
                sendReceive.start()
            } catch (e: IOException) {
                e.printStackTrace()
                val message = Message.obtain()
                message.what = STATE_CONNECTION_FAILED
                handler.sendMessage(message)
            }
        }
    }

    //Thread for sending and receiving messages
    inner class SendReceive(socket: BluetoothSocket) : Thread() {
        private var bluetoothSocket: BluetoothSocket
        private var inputStream: InputStream
        private var outputStream: OutputStream

        init {
            bluetoothSocket = socket
            var tempIn: InputStream? = null
            var tempOut: OutputStream? = null
            try {
                tempIn = bluetoothSocket.inputStream
                tempOut = bluetoothSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            inputStream = tempIn!!
            outputStream = tempOut!!
        }

        override fun run() {
            var buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = inputStream.read(buffer)
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer)
                        .sendToTarget()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }


    //click on listener for recycler view

    override fun onItemClick(position: Int) {
        val clientClass = ClientClass(mBluetoothDevicesList[position])
        clientClass.start()
        mBinding.tvStatus.text = "Connecting"
    }

    //to generate random UUID
    private fun generateBluetoothUUID(): UUID {
        return UUID.randomUUID()
    }

    //unregister receiver and release media player instance
    override fun onDestroy() {
        super.onDestroy()
        //unregisterReceiver(receiver1)

    }
}
