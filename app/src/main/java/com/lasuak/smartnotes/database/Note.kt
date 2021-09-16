package com.lasuak.smartnotes.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_table" )
data class Note(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "time") val time: String,
    @ColumnInfo(name = "priority") val priority: Int,
    @ColumnInfo(name = "reminderTime") val reminderTime:String?,

    @PrimaryKey(autoGenerate = true)
    val id :Int
)