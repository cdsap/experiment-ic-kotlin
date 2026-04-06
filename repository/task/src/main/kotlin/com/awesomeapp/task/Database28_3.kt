package com.awesomeapp.task

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity28_1::class], version = 1, exportSchema = false)
abstract class Database28_3 : RoomDatabase() {
    abstract fun dao(): Dao28_4
}