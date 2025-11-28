package com.german.misionlogromania.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.german.misionlogromania.R

class MissionsAdapter(private val missions: List<String>) :
    RecyclerView.Adapter<MissionsAdapter.MissionViewHolder>() {

    class MissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val missionTextView: TextView = itemView.findViewById(R.id.missionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mission, parent, false)
        return MissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: MissionViewHolder, position: Int) {
        holder.missionTextView.text = missions[position]
    }

    override fun getItemCount(): Int = missions.size
}