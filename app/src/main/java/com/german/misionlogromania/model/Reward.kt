package com.german.misionlogromania.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Reward(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val level: String = "",
    val points: Int = 0
) : Parcelable