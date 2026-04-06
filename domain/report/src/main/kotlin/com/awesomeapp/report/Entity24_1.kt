package com.awesomeapp.report

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Entity24_1(
    @PrimaryKey val id: Long,
    val title: String,
    val updatedAt: Long
)