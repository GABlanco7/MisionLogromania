package com.german.misionlogromania.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Mission(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val difficulty: String = "",
    val minAge: Int = 0,
    val maxAge: Int = 0,
    val category: String = ""
) : Parcelable