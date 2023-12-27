package com.kor.genericprinter.adapters

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kor.genericprinter.R
import com.kor.genericprinter.listeners.OnItemClickListener

class BluetoothDeviceAdapter(private val deviceList: List<BluetoothDevice>, private val listener: OnItemClickListener) :
    RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_device_name)
        val addressTextView: TextView = itemView.findViewById(R.id.tv_device_address)
        val statusTextView: TextView = itemView.findViewById(R.id.tv_device_bond_status)

        fun bind(position: Int, listener: OnItemClickListener) {
            itemView.setOnClickListener {
                listener.onItemClick(position)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_layout, parent, false)
        return DeviceViewHolder(itemView)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val currentDevice = deviceList[position]

        holder.nameTextView.text = currentDevice.name ?: "Unknown"
        holder.addressTextView.text = currentDevice.address
        holder.statusTextView.text = currentDevice.bondState.toString()

        holder.bind(position, listener)

    }

    override fun getItemCount(): Int {
        return deviceList.size
    }
}
