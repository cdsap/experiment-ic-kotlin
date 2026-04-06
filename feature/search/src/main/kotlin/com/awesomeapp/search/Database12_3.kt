package com.awesomeapp.search

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity12_1::class], version = 1, exportSchema = false)
abstract class Database12_3 : RoomDatabase() {
    abstract fun dao(): Dao12_4
}