package com.awesomeapp.share

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity16_1::class], version = 1, exportSchema = false)
abstract class Database16_3 : RoomDatabase() {
    abstract fun dao(): Dao16_4
}