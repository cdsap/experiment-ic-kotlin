package com.awesomeapp.post

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity11_1::class], version = 1, exportSchema = false)
abstract class Database11_3 : RoomDatabase() {
    abstract fun dao(): Dao11_4
}