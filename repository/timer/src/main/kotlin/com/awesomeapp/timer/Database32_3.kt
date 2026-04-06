package com.awesomeapp.timer

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity32_1::class], version = 1, exportSchema = false)
abstract class Database32_3 : RoomDatabase() {
    abstract fun dao(): Dao32_4
}