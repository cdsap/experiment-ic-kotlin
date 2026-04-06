package com.awesomeapp.user

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity7_1::class], version = 1, exportSchema = false)
abstract class Database7_3 : RoomDatabase() {
    abstract fun dao(): Dao7_4
}