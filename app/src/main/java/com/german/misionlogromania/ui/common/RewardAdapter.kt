package com.german.misionlogromania.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.german.misionlogromania.R
import com.german.misionlogromania.ui.rewards.Reward

class RewardAdapter(
    private val rewards: List<Reward>,
    private val childStars: Long,
    private val onRedeem: (Reward) -> Unit
) : RecyclerView.Adapter<RewardAdapter.RewardViewHolder>() {

    inner class RewardViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.tvRewardTitle)
        val starsText: TextView = view.findViewById(R.id.tvRewardStars)
        val btnRedeem: Button = view.findViewById(R.id.btnRedeem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward, parent, false)
        return RewardViewHolder(view)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        val reward = rewards[position]
        holder.titleText.text = reward.title
        holder.starsText.text = "${reward.requiredStars} ⭐"
        holder.btnRedeem.isEnabled = childStars >= reward.requiredStars
        holder.btnRedeem.setOnClickListener { onRedeem(reward) }
    }

    override fun getItemCount() = rewards.size
}