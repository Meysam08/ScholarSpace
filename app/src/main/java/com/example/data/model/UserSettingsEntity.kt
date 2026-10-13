package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val focusDurationMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakInterval: Int = 4,
    val libraryViewMode: String = "GRID", // "GRID", "LIST"
    val autoStartBreaks: Boolean = false,
    val soundEnabled: Boolean = true
)
