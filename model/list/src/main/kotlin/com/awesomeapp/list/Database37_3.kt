package com.awesomeapp.list

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity37_1::class], version = 1, exportSchema = false)
abstract class Database37_3 : RoomDatabase() {
    abstract fun dao(): Dao37_4
}