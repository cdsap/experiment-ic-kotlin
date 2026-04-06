package com.awesomeapp.analytics

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity23_1::class], version = 1, exportSchema = false)
abstract class Database23_3 : RoomDatabase() {
    abstract fun dao(): Dao23_4
}