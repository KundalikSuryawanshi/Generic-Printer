package com.kor.genericprinter.printer

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class PrinterHelper {

    private val PRINTER_WIDTH = 384
    private val INITIAL_MARGIN_LEFT = -4
    private val BIT_WIDTH = 384
    private val WIDTH = 48
    private val HEAD = 8
    private val CENTER = 300
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1b, 'a'.code.toByte(), 0x01)
    var mBluetoothAdapter: BluetoothAdapter? = null
    var mSocket: BluetoothSocket? = null
    var mOutputStream: OutputStream? = null
    var mInputStream: InputStream? = null
    var workerThread: Thread? = null
    lateinit var readBuffer: ByteArray
    var readBufferPosition = 0




}