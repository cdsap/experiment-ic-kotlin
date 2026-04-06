package com.awesomeapp.location

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity13_1::class], version = 1, exportSchema = false)
abstract class Database13_3 : RoomDatabase() {
    abstract fun dao(): Dao13_4
}