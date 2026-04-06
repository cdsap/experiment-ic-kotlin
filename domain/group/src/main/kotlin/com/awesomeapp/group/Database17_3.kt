package com.awesomeapp.group

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity17_1::class], version = 1, exportSchema = false)
abstract class Database17_3 : RoomDatabase() {
    abstract fun dao(): Dao17_4
}