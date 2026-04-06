package com.awesomeapp.alarm

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity31_1::class], version = 1, exportSchema = false)
abstract class Database31_3 : RoomDatabase() {
    abstract fun dao(): Dao31_4
}