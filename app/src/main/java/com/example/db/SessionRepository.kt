package com.example.db

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessions(): Flow<List<SessionEntity>>
    suspend fun getSessionById(id: Long): SessionEntity?
    fun getChordsForSession(sessionId: Long): Flow<List<ChordTimelineEntity>>
    suspend fun saveSessionWithChords(session: SessionEntity, chords: List<ChordTimelineEntity>): Long
    suspend fun deleteSession(sessionId: Long)
}
