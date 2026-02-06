package com.atomx.genericprinter

import PrinterClient
import android.bluetooth.BluetoothAdapter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

object PrinterFactory {

    fun bluetooth(mac: String, config: PrinterConfig = PrinterConfig(), adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()): PrinterClient {
        val conn = BluetoothPrinterConnection(macAddress = mac, adapter = adapter)
        return PrinterClient(conn, config)
    }

    fun usb(usbManager: UsbManager, device: UsbDevice, config: PrinterConfig = PrinterConfig()): PrinterClient {
        val conn = UsbPrinterConnection(usbManager, device)
        return PrinterClient(conn, config)
    }
}
