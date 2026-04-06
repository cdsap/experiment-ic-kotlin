package com.awesomeapp.identity

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity4_1::class], version = 1, exportSchema = false)
abstract class Database4_3 : RoomDatabase() {
    abstract fun dao(): Dao4_4
}