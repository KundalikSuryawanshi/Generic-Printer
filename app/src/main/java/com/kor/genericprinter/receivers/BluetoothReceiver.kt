package com.kor.genericprinter.receivers
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothReceiver : BroadcastReceiver(){

    val TAG = "bluetooth receiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action

        if(action== BluetoothAdapter.ACTION_STATE_CHANGED){

            when(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR)){
                BluetoothAdapter.STATE_OFF->{
                    Log.d(TAG,"Bluetooth off")
                }
                BluetoothAdapter.STATE_TURNING_OFF->{
                    Log.d(TAG,"Bluetooth turning off")
                }
                BluetoothAdapter.STATE_ON->{
                    Log.d(TAG,"Bluetooth On")
                }
                BluetoothAdapter.STATE_TURNING_ON->{
                    Log.d(TAG,"Bluetooth turning on")
                }
            }
        }
    }
}