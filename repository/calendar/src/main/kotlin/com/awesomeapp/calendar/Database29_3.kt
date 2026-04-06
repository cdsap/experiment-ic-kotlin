package com.awesomeapp.calendar

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity29_1::class], version = 1, exportSchema = false)
abstract class Database29_3 : RoomDatabase() {
    abstract fun dao(): Dao29_4
}