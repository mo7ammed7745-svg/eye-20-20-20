package com.mohamed.eye2020.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BreakRecord::class],
    version = 1,
    exportSchema = true,
)
abstract class EyeDatabase : RoomDatabase() {
    abstract fun breakRecordDao(): BreakRecordDao

    companion object {
        const val NAME = "eye2020.db"
    }
}
