package com.wakeup.esmoglogger.serialcommunication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.wakeup.esmoglogger.data.SharedESmogData
import com.wakeup.esmoglogger.ui.log.SharedLogData
import java.io.InputStream
import java.net.Socket

class SerialCommunication(private val context: Context?) {
    private val usbManager: UsbManager = context!!.getSystemService(Context.USB_SERVICE) as UsbManager
    private var port: UsbSerialPort? = null
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { setupUsbConnection() }
                        SharedLogData.addLog("USB permission set")
                    } else {
                        SharedLogData.addLog("USB permission denied")
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    device?.let { requestUsbPermission(it) }
                    SharedLogData.addLog("USB device atached")
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    closeUsbConnection()
                    SharedLogData.addLog("USB device detached")
                }
            }
        }
    }
    fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (context != null) {
            ContextCompat.registerReceiver(
                context,
                usbReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val permissionIntent = android.app.PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    fun setupConnection() {
        // Detect emulator (simple check, adjust as needed)
        val isEmulator = android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("Emulator")
        if (isEmulator) {
            setupTcpConnection()
        } else {
            setupUsbConnection()
        }
    }

    private fun setupUsbConnection() {
        val prober = UsbSerialProber.getDefaultProber()
        val availableDrivers = prober.findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            SharedLogData.addLog("No USB device found")
            return
        }

        val driver: UsbSerialDriver = availableDrivers[0]
        val device = driver.device
        if (!usbManager.hasPermission(device)) {
            requestUsbPermission(device)
            return
        }

        port = driver.ports[0]
        try {
            port?.open(usbManager.openDevice(device))
            port?.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            SharedLogData.addLog("Serial port opened")
            startReading()
        } catch (e: Exception) {
            SharedLogData.addLog("Error opening port: ${e.message}")
        }
    }

    private fun setupTcpConnection(host: String = "10.0.2.2", port: Int = 12345) {
        try {
            socket = Socket(host, port)
            inputStream = socket?.getInputStream()
            SharedLogData.addLog("Connected to TCP server at $host:$port")
            startReading()
        } catch (e: Exception) {
            SharedLogData.addLog("Connection error: ${e.message}")
        }
    }

    private fun startReading() {
        Thread {
            val buffer = ByteArray(1024)
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val numBytesRead = if (port != null) {
                        port?.read(buffer, 1000) ?: -1
                    } else {
                        inputStream?.read(buffer) ?: -1
                    }
                    if (numBytesRead > 0) {
                        val receivedData = String(buffer, 0, numBytesRead)
                        // 1827E-03,2050
                        // Signal level: 1827E-03
                        // Frequency: 2050
                        try {
                            val (val1, val2) = receivedData.split(",")
                            val levelFrq = Pair(val1.toFloat(), val2.trim().toInt())
                            SharedESmogData.addLvlFrq(levelFrq)
                        } catch (e: Exception) {
                            SharedLogData.addLog("Read error: ${e.message}")
                            break
                        }
                    } else if (numBytesRead == -1 && inputStream != null) {
                        SharedLogData.addLog("TCP connection closed")
                        break
                    }
                } catch (e: Exception) {
                    SharedLogData.addLog("Read error: ${e.message}")
                    break
                }
            }
        }.start()
    }

    private fun closeUsbConnection() {
        try {
            port?.close()
            SharedLogData.addLog("Serial port closed")
        } catch (e: Exception) {
            SharedLogData.addLog("Error closing USB port: ${e.message}")
        }
        port = null
    }

    fun closeConnection() {
        try {
            if (port != null) {
                closeUsbConnection()
            } else if (inputStream != null) {
                inputStream?.close()
                socket?.close()
                SharedLogData.addLog("TCP connection closed")
            }
        } catch (e: Exception) {
            SharedLogData.addLog("Error closing connection: ${e.message}")
        }
        socket = null
        inputStream = null
    }

    fun cleanup() {
        closeConnection()
        context?.unregisterReceiver(usbReceiver)
    }

    init {
        registerUsbReceiver()
    }
}