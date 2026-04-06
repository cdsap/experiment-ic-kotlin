package com.awesomeapp.app

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity41_1::class], version = 1, exportSchema = false)
abstract class Database41_3 : RoomDatabase() {
    abstract fun dao(): Dao41_4
}