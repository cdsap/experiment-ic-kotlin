package com.awesomeapp.comment

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity10_1::class], version = 1, exportSchema = false)
abstract class Database10_3 : RoomDatabase() {
    abstract fun dao(): Dao10_4
}