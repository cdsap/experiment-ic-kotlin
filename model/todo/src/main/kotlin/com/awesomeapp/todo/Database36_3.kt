package com.awesomeapp.todo

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity36_1::class], version = 1, exportSchema = false)
abstract class Database36_3 : RoomDatabase() {
    abstract fun dao(): Dao36_4
}