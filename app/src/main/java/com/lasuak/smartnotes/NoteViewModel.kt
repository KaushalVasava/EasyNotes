package com.lasuak.smartnotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.lasuak.smartnotes.database.Note
import com.lasuak.smartnotes.database.NoteDatabase
import com.lasuak.smartnotes.database.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteViewModel(application: Application): AndroidViewModel(application){
    private val repository : NoteRepository

    init{
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
    }
    val allNotes: LiveData<List<Note>> = repository.allNotes
    val allNotesById: LiveData<List<Note>> = repository.allNotesById
    val allNotesByHighToLow: LiveData<List<Note>> = repository.allNotesByHighToLow
    val allNotesByLowToHigh: LiveData<List<Note>> = repository.allNotesByLowToHigh
    val allNotesByRed: LiveData<List<Note>> = repository.allNotesByRed
    val allNotesByYellow: LiveData<List<Note>> = repository.allNotesByYellow
    val allNotesByGreen: LiveData<List<Note>> = repository.allNotesByGreen


//    fun getData():LiveData<List<Note>>{
//        return allNotes
//    }
//    fun getDataById():LiveData<List<Note>>{
//        return allNotesById
//    }
//    fun getDataByHighToLow():LiveData<List<Note>>{
//        return allNotesByHighToLow
//    }
//    fun getDataByLowToHigh():LiveData<List<Note>>{
//        return allNotesByLowToHigh
//    }

    fun insert(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(note)
    }
    fun delete(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(note)
    }
    fun update(note: Note)=viewModelScope.launch {
        repository.update(note)
    }
    fun deleteAll()=viewModelScope.launch {
        repository.deleteAll()
    }
    fun searchDatabase(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabase(searchQuery)
    }
    fun searchDatabaseById(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseById(searchQuery)
    }
    fun searchDatabaseByHighToLow(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseByHighToLow(searchQuery)
    }
    fun searchDatabaseByLowToHigh(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseByLowToHigh(searchQuery)
    }
    fun searchDatabaseByRed(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseByRed(searchQuery)
    }
    fun searchDatabaseByYellow(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseByRed(searchQuery)
    }
    fun searchDatabaseByGreen(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseByRed(searchQuery)
    }
}
