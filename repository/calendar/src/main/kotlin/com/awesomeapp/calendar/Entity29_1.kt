package com.awesomeapp.calendar

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Entity29_1(
    @PrimaryKey val id: Long,
    val title: String,
    val updatedAt: Long
)