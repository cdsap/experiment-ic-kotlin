package com.awesomeapp.document

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity34_1::class], version = 1, exportSchema = false)
abstract class Database34_3 : RoomDatabase() {
    abstract fun dao(): Dao34_4
}