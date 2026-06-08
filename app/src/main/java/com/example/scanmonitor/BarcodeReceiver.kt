package com.example.scanmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class BarcodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val barcode = when (intent?.action) {
            // Zebra DataWedge
            "com.example.scanmonitor.SCAN" -> {
                intent.getStringExtra("com.symbol.datawedge.data_string")
            }
            // Clover built-in scanner
            "com.clover.intent.action.BARCODE" -> {
                intent.getStringExtra("com.clover.intent.extra.BARCODE_RESULT")
            }
            else -> null
        }

        if (!barcode.isNullOrEmpty()) {
            Log.d("BARCODE", "Scanned: $barcode")
            val serviceIntent = Intent(context, ScanMonitorService::class.java)
            serviceIntent.putExtra("barcode", barcode)
            context?.startForegroundService(serviceIntent)
        }
    }
}