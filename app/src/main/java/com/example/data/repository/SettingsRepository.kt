package com.example.data.repository

import com.example.data.dao.SettingsDao
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SettingsRepository(private val settingsDao: SettingsDao) {

    val settings: Flow<UserSettingsEntity?> = settingsDao.getSettings()

    suspend fun getSettingsSync(): UserSettingsEntity = withContext(Dispatchers.IO) {
        settingsDao.getSettingsSync() ?: UserSettingsEntity().also {
            settingsDao.insertOrUpdate(it)
        }
    }

    suspend fun updateSettings(settings: UserSettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun updateThemeMode(mode: String) = withContext(Dispatchers.IO) {
        val current = getSettingsSync()
        settingsDao.insertOrUpdate(current.copy(themeMode = mode))
    }

    suspend fun updateTimerDurations(focusMin: Int, shortBreakMin: Int, longBreakMin: Int) = withContext(Dispatchers.IO) {
        val current = getSettingsSync()
        settingsDao.insertOrUpdate(
            current.copy(
                focusDurationMinutes = focusMin,
                shortBreakMinutes = shortBreakMin,
                longBreakMinutes = longBreakMin
            )
        )
    }

    suspend fun updateLibraryViewMode(mode: String) = withContext(Dispatchers.IO) {
        val current = getSettingsSync()
        settingsDao.insertOrUpdate(current.copy(libraryViewMode = mode))
    }

    suspend fun toggleAutoStartBreaks(enabled: Boolean) = withContext(Dispatchers.IO) {
        val current = getSettingsSync()
        settingsDao.insertOrUpdate(current.copy(autoStartBreaks = enabled))
    }
}
