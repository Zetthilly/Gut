package com.example.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SessionRepositoryImpl(
    private val sessionDao: SessionDao
) : SessionRepository {

    override fun getAllSessions(): Flow<List<SessionEntity>> {
        return sessionDao.getAllSessions()
    }

    override suspend fun getSessionById(id: Long): SessionEntity? = withContext(Dispatchers.IO) {
        sessionDao.getSessionById(id)
    }

    override fun getChordsForSession(sessionId: Long): Flow<List<ChordTimelineEntity>> {
        return sessionDao.getChordsForSession(sessionId)
    }

    override suspend fun saveSessionWithChords(
        session: SessionEntity,
        chords: List<ChordTimelineEntity>
    ): Long = withContext(Dispatchers.IO) {
        val sessionId = sessionDao.insertSession(session)
        val mappedChords = chords.map { it.copy(sessionId = sessionId) }
        sessionDao.insertChords(mappedChords)
        sessionId
    }

    override suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        sessionDao.deleteSessionById(sessionId)
    }
}
