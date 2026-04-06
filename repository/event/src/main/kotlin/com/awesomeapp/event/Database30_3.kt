package com.awesomeapp.event

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity30_1::class], version = 1, exportSchema = false)
abstract class Database30_3 : RoomDatabase() {
    abstract fun dao(): Dao30_4
}