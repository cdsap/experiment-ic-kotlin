package com.awesomeapp.todo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Entity36_1(
    @PrimaryKey val id: Long,
    val title: String,
    val updatedAt: Long
)