package com.awesomeapp.sync

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity15_1::class], version = 1, exportSchema = false)
abstract class Database15_3 : RoomDatabase() {
    abstract fun dao(): Dao15_4
}