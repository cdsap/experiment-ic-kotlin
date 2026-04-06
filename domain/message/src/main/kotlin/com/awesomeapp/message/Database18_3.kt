package com.awesomeapp.message

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity18_1::class], version = 1, exportSchema = false)
abstract class Database18_3 : RoomDatabase() {
    abstract fun dao(): Dao18_4
}