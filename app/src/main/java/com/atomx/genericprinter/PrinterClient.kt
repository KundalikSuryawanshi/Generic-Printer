package com.atomx.genericprinter

import android.graphics.Bitmap

class PrinterClient(
    private val connection: PrinterConnection,
    private val config: PrinterConfig = PrinterConfig()
) {

    init {
        PrinterLogger.enabled = config.debug
    }

    fun connect(): Boolean {
        PrinterLogger.d("connect() called")
        return connection.connect()
    }

    fun disconnect() {
        PrinterLogger.d("disconnect() called")
        connection.close()
    }

    fun printText(text: String, newLine: Boolean = true) {
        ensureConnected()
        connection.write(EscPosCommands.textUtf8(text))
        if (newLine) connection.write(EscPosCommands.LF)
    }

    fun printBitmap(bitmap: Bitmap, center: Boolean = true) {
        ensureConnected()

        if (center) connection.write(EscPosCommands.ALIGN_CENTER) else connection.write(EscPosCommands.ALIGN_LEFT)
        val bytes = EscPosImage.toRasterBytes(bitmap, config.paperWidthPx)
        connection.write(bytes)
        connection.write(EscPosCommands.LF)
    }

    fun feed(lines: Int = 3) {
        ensureConnected()
        connection.write(EscPosCommands.feed(lines))
    }

    fun cut(partial: Boolean = true) {
        ensureConnected()
        connection.write(if (partial) EscPosCommands.CUT_PARTIAL else EscPosCommands.CUT_FULL)
    }

    fun reset() {
        ensureConnected()
        connection.write(EscPosCommands.INIT)
    }

    private fun ensureConnected() {
        if (!connection.isConnected()) throw IllegalStateException("Printer not connected")
    }
}
