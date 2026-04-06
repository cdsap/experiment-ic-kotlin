package com.awesomeapp.checkout

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Entity5_1(
    @PrimaryKey val id: Long,
    val title: String,
    val updatedAt: Long
)