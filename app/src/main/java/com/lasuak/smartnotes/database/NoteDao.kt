package com.lasuak.smartnotes.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NoteDao {
    @Query("SELECT * FROM note_table ORDER BY time DESC")
    fun getAllNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM note_table ORDER BY id ASC")
    fun getAllNotesById(): LiveData<List<Note>>

    @Query("SELECT * FROM note_table ORDER BY priority DESC")
    fun getAllNotesByLow(): LiveData<List<Note>>

    @Query("SELECT * FROM note_table ORDER BY priority ASC")
    fun getAllNotesByHigh(): LiveData<List<Note>>

    @Query("SELECT * FROM note_table where priority=1")//ORDER BY priority=1 DESC")
    fun getAllNotesByRed(): LiveData<List<Note>>
    @Query("SELECT * FROM note_table where priority=2")
    fun getAllNotesByYellow(): LiveData<List<Note>>
    @Query("SELECT * FROM note_table where priority=3")
    fun getAllNotesByGreen(): LiveData<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(note: Note)

    @Query("DELETE FROM note_table")
    suspend fun deleteAll()

    @Query("SELECT * FROM note_table WHERE title LIKE :searchQuery or note LIKE :searchQuery ORDER BY time DESC")
    fun searchDatabase(searchQuery: String): LiveData<List<Note>>

    @Query("SELECT * FROM note_table WHERE title LIKE :searchQuery or note LIKE :searchQuery ORDER BY id ASC")
    fun searchDatabaseById(searchQuery: String): LiveData<List<Note>>

    @Query("SELECT * FROM note_table WHERE title LIKE :searchQuery or note LIKE :searchQuery ORDER BY priority ASC")
    fun searchDatabaseByHighToLow(searchQuery: String): LiveData<List<Note>>

    @Query("SELECT * FROM note_table WHERE title LIKE :searchQuery or note LIKE :searchQuery ORDER BY priority DESC")
    fun searchDatabaseByLowtoHigh(searchQuery: String): LiveData<List<Note>>

    @Query("SELECT * FROM note_table WHERE title LIKE :searchQuery or note LIKE :searchQuery ORDER BY priority ASC")//" and priority=1")
    fun searchDatabaseByRed(searchQuery: String): LiveData<List<Note>>

    @Query("SELECT * FROM note_table WHERE title LIKE :searchQuery or note LIKE :searchQuery ORDER BY priority DESC")//and priority=2")
    fun searchDatabaseByYellow(searchQuery: String): LiveData<List<Note>>

    @Query("SELECT * FROM note_table WHERE title LIKE :searchQuery or note LIKE :searchQuery ORDER BY priority DESC")
    fun searchDatabaseByGreen(searchQuery: String): LiveData<List<Note>>

}
