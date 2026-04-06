package com.awesomeapp.note

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity35_1::class], version = 1, exportSchema = false)
abstract class Database35_3 : RoomDatabase() {
    abstract fun dao(): Dao35_4
}