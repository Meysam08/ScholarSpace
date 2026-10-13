package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CollectionDao
import com.example.data.dao.FocusDao
import com.example.data.dao.MaterialDao
import com.example.data.dao.SettingsDao
import com.example.data.dao.TaskDao
import com.example.data.model.CollectionEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.MaterialEntity
import com.example.data.model.UserSettingsEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CollectionEntity::class,
        MaterialEntity::class,
        TaskEntity::class,
        FocusSessionEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectionDao(): CollectionDao
    abstract fun materialDao(): MaterialDao
    abstract fun taskDao(): TaskDao
    abstract fun focusDao(): FocusDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scholar_space_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize default user settings and clean starter library
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.settingsDao()?.insertOrUpdate(UserSettingsEntity())
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
