package com.awesomeapp.file

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity33_1::class], version = 1, exportSchema = false)
abstract class Database33_3 : RoomDatabase() {
    abstract fun dao(): Dao33_4
}