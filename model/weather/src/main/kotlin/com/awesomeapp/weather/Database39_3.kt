package com.awesomeapp.weather

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity39_1::class], version = 1, exportSchema = false)
abstract class Database39_3 : RoomDatabase() {
    abstract fun dao(): Dao39_4
}