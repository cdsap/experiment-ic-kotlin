package com.awesomeapp.log

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity25_1::class], version = 1, exportSchema = false)
abstract class Database25_3 : RoomDatabase() {
    abstract fun dao(): Dao25_4
}