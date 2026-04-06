package com.awesomeapp.group

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Entity17_1(
    @PrimaryKey val id: Long,
    val title: String,
    val updatedAt: Long
)