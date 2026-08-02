package com.example.timetracker

import android.app.Application
import com.example.timetracker.data.AppDatabase
import com.example.timetracker.data.TimeTrackerRepository

class TimeTrackerApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        TimeTrackerRepository(
            database.categoryDao(),
            database.timeRecordDao()
        )
    }
}
