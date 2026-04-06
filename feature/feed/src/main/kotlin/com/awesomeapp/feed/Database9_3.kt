package com.awesomeapp.feed

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity9_1::class], version = 1, exportSchema = false)
abstract class Database9_3 : RoomDatabase() {
    abstract fun dao(): Dao9_4
}