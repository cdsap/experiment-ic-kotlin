package com.awesomeapp.forecast

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity40_1::class], version = 1, exportSchema = false)
abstract class Database40_3 : RoomDatabase() {
    abstract fun dao(): Dao40_4
}