package com.atomx.blutox

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DiscoverabilityReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context?, intent: Intent?) {

        val TAG = "discoverable receiver"
        val action = intent?.action
        if (action != null) {

            if(action == BluetoothAdapter.ACTION_SCAN_MODE_CHANGED){

                when(intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,BluetoothAdapter.ERROR)){
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE ->{
                        Log.d(TAG,"Connectable")
                    }
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE->{
                        Log.d(TAG,"Connectable Discoverable")
                    }
                    BluetoothAdapter.SCAN_MODE_NONE->{
                        Log.d(TAG,"Connectable NONE")
                    }
                    BluetoothAdapter.STATE_CONNECTING->{
                        Log.d(TAG,"Connectable Connecting")
                    }
                    BluetoothAdapter.STATE_CONNECTED->{
                        Log.d(TAG,"Connectable Connected")
                    }
                }
            }
        }
    }

}