package com.example.scanmonitor

import android.accounts.Account
import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v3.inventory.InventoryConnector
import com.clover.sdk.v3.inventory.Item
import java.util.concurrent.Executors

class ScanMonitorService : Service() {

    private var inventoryConnector: InventoryConnector? = null
    private var cloverAccount: Account? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceProperly()

        // Get the Clover account
        cloverAccount = CloverAccount.getAccount(this)

        if (cloverAccount == null) {
            Log.e("ScanMonitor", "No Clover account found!")
            return
        }

        // Connect to Clover inventory
        inventoryConnector = InventoryConnector(this, cloverAccount!!, null)
        inventoryConnector?.connect()
    }

    // Call this when a barcode is scanned
    fun checkItemExists(barcode: String) {
        executor.execute {
            try {
                val item: Item? = inventoryConnector?.getItemByCode(barcode)

                if (item == null) {
                    // Item not found in Clover inventory — trigger alert
                    playAlert()
                } else {
                    Log.d("ScanMonitor", "Item found: ${item.name}")
                }
            } catch (e: Exception) {
                Log.e("ScanMonitor", "Error checking inventory: ${e.message}")
            }
        }
    }

    private fun playAlert() {
        val player = MediaPlayer.create(this, R.raw.alert)
        player?.apply {
            setOnCompletionListener { mp -> mp.release() }
            start()
        }
        Toast.makeText(this, "Item not found! Please cancel and rescan.", Toast.LENGTH_LONG).show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val barcode = intent?.getStringExtra("barcode")
        if (!barcode.isNullOrEmpty()) {
            checkItemExists(barcode)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        inventoryConnector?.disconnect()
        executor.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceProperly() {
        val channelId = "scan_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Scan Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Scan Monitor Running")
            .setContentText("Listening for barcode scans")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }
}