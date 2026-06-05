package com.example.scanmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BarcodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != "com.example.scanmonitor.SCAN") return

        val barcode = intent.getStringExtra("com.symbol.datawedge.data_string")
        Log.d("BARCODE", "Scanned: $barcode")

        if (!barcode.isNullOrEmpty()) {
            // Pass barcode to service to check against Clover inventory
            val serviceIntent = Intent(context, ScanMonitorService::class.java)
            serviceIntent.putExtra("barcode", barcode)
            context?.startForegroundService(serviceIntent)
        }
    }
}