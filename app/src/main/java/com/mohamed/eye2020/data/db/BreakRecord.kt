package com.mohamed.eye2020.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per break whose countdown actually reached zero. The session start timestamp is
 * unique so a process restart can never record the same completed break twice.
 */
@Entity(
    tableName = "break_records",
    indices = [
        Index(value = ["sessionStartedAtMillis"], unique = true),
        Index(value = ["completedAtMillis"]),
    ],
)
data class BreakRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionStartedAtMillis: Long,
    val completedAtMillis: Long,
    val durationSeconds: Int,
)
