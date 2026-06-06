package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 120000L,
    val tempoBpm: Int = 73,
    val rootKey: String = "D#m"
)
