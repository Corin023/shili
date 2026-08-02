package com.example.timetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "time_records",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"]), Index(value = ["startTime"])]
)
data class TimeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long? = null,
    val categoryName: String = "",
    val startTime: Instant = Instant.now(),
    val endTime: Instant? = null,
    val durationSeconds: Long = 0,
    val notes: String = ""
)
