package com.example.scanmonitor

import android.accounts.Account
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v3.inventory.InventoryConnector
import java.util.concurrent.Executors

class ScanMonitorService : Service() {

    private var inventoryConnector: InventoryConnector? = null
    private var cloverAccount: Account? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var cachedSkus: Set<String> = emptySet()

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceProperly()

        cloverAccount = CloverAccount.getAccount(this)
        if (cloverAccount == null) {
            showToast("No Clover account found!")
            Log.e("ScanMonitor", "No Clover account found!")
            return
        }

        inventoryConnector = InventoryConnector(this, cloverAccount!!, null)
        inventoryConnector?.connect()
        loadInventoryCache()
    }

    private fun loadInventoryCache() {
        executor.execute {
            try {
                val items = inventoryConnector?.getItems()
                cachedSkus = items
                    ?.mapNotNull { it.sku }
                    ?.filter { it.isNotBlank() }
                    ?.toHashSet()
                    ?: emptySet()

                showToast("Inventory cached: ${cachedSkus.size} SKUs loaded")
                Log.d("ScanMonitor", "Inventory cached: ${cachedSkus.size} SKUs loaded")
            } catch (e: Exception) {
                showToast("Error loading inventory: ${e.message}")
                Log.e("ScanMonitor", "Error loading inventory cache: ${e.message}")
            }
        }
    }

    fun checkItemExists(barcode: String) {
        executor.execute {
            try {
                if (cachedSkus.isEmpty()) {
                    Log.w("ScanMonitor", "Cache empty, reloading...")
                    loadInventoryCache()
                    return@execute
                }

                val found = cachedSkus.contains(barcode)

                if (!found) {
                    playAlert()
                } else {
                    Log.d("ScanMonitor", "Item found for barcode: $barcode")
                }

            } catch (e: Exception) {
                Log.e("ScanMonitor", "Error checking barcode: ${e.message}")
            }
        }
    }

    // Single helper — all Toasts go through here, always safe
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this@ScanMonitorService, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun playAlert() {
        val player = MediaPlayer.create(this, R.raw.alert)
        player?.apply {
            setOnCompletionListener { mp -> mp.release() }
            start()
        }
        showToast("Item not found! Please cancel and rescan.")
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

    @SuppressLint("ForegroundServiceType")
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