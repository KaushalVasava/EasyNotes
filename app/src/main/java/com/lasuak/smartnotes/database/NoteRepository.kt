package com.lasuak.smartnotes.database

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData


class NoteRepository(private val noteDao: NoteDao) {

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes()
    val allNotesById: LiveData<List<Note>> = noteDao.getAllNotesById()
    val allNotesByHighToLow: LiveData<List<Note>> = noteDao.getAllNotesByHigh()
    val allNotesByLowToHigh: LiveData<List<Note>> = noteDao.getAllNotesByLow()
    val allNotesByRed: LiveData<List<Note>> = noteDao.getAllNotesByRed()
    val allNotesByYellow: LiveData<List<Note>> = noteDao.getAllNotesByYellow()
    val allNotesByGreen: LiveData<List<Note>> = noteDao.getAllNotesByGreen()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(note: Note){
        noteDao.insert(note)
    }
    @WorkerThread
    suspend fun delete(note: Note){
        noteDao.delete(note)
    }
    @WorkerThread
    suspend fun update(note: Note){
        noteDao.update(note)
    }
    @WorkerThread
    suspend fun deleteAll(){
        noteDao.deleteAll()
    }
    @WorkerThread
    fun searchDatabase(searchQuery: String): LiveData<List<Note>> {
        return noteDao.searchDatabase(searchQuery)
    }
    @WorkerThread
    fun searchDatabaseById(searchQuery: String): LiveData<List<Note>> {
        return noteDao.searchDatabaseById(searchQuery)
    }
    @WorkerThread
    fun searchDatabaseByHighToLow(searchQuery: String): LiveData<List<Note>> {
        return noteDao.searchDatabaseByHighToLow(searchQuery)
    }
    @WorkerThread
    fun searchDatabaseByLowToHigh(searchQuery: String): LiveData<List<Note>> {
        return noteDao.searchDatabaseByLowtoHigh(searchQuery)
    }
    @WorkerThread
    fun searchDatabaseByRed(searchQuery: String): LiveData<List<Note>> {
        return noteDao.searchDatabaseByRed(searchQuery)
    }
    @WorkerThread
    fun searchDatabaseByYellow(searchQuery: String): LiveData<List<Note>> {
        return noteDao.searchDatabaseByRed(searchQuery)
    }
    @WorkerThread
    fun searchDatabaseByGreen(searchQuery: String): LiveData<List<Note>> {
        return noteDao.searchDatabaseByRed(searchQuery)
    }
}
