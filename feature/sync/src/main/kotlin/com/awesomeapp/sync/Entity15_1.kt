package com.awesomeapp.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Entity15_1(
    @PrimaryKey val id: Long,
    val title: String,
    val updatedAt: Long
)