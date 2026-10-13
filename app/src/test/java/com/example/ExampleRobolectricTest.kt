package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.file.FileManager
import com.example.data.model.CollectionEntity
import com.example.data.repository.LibraryRepository
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var fileManager: FileManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fileManager = FileManager(context)
        libraryRepository = LibraryRepository(database.collectionDao(), database.materialDao(), fileManager)
        taskRepository = TaskRepository(database.taskDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun read_string_from_context() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ScholarSpace", appName)
    }

    @Test
    fun collection_circular_hierarchy_prevention() = runBlocking {
        // Create root -> child -> grandchild
        val root = libraryRepository.createCollection("Root Subject", null)
        val child = libraryRepository.createCollection("Child Subject", root.id)
        val grandchild = libraryRepository.createCollection("Grandchild Subject", child.id)

        // Cannot move root into its child or grandchild
        assertFalse(libraryRepository.canMoveCollection(root.id, child.id))
        assertFalse(libraryRepository.canMoveCollection(root.id, grandchild.id))

        // Cannot move into self
        assertFalse(libraryRepository.canMoveCollection(child.id, child.id))

        // Moving grandchild to root is allowed
        assertTrue(libraryRepository.canMoveCollection(grandchild.id, null))
        // Moving grandchild to root collection is allowed
        assertTrue(libraryRepository.canMoveCollection(grandchild.id, root.id))
    }

    @Test
    fun task_creation_and_completion_toggle() = runBlocking {
        val task = taskRepository.createTask(
            title = "Review Discrete Math Slides",
            notes = "Focus on induction proofs",
            isMyDay = true
        )
        assertEquals("Review Discrete Math Slides", task.title)
        assertFalse(task.isCompleted)
        assertTrue(task.isMyDay)

        taskRepository.toggleCompleted(task)
        val updated = database.taskDao().getTaskByIdSync(task.id)
        assertTrue(updated?.isCompleted == true)
    }

    @Test
    fun task_my_day_includes_explicit_and_today_due_date() = runBlocking {
        val today = System.currentTimeMillis()
        val taskWithTodayDueDate = taskRepository.createTask(
            title = "Assignment due today",
            dueDate = today,
            isMyDay = false
        )
        val taskExplicitMyDay = taskRepository.createTask(
            title = "Read Chapter 1",
            isMyDay = true
        )
        val myDayList = taskRepository.getMyDayTasks().first()
        assertTrue(myDayList.any { it.id == taskWithTodayDueDate.id })
        assertTrue(myDayList.any { it.id == taskExplicitMyDay.id })
    }

    @Test
    fun screen_bottom_nav_items_are_non_null() {
        val items = com.example.ui.navigation.Screen.bottomNavItems
        assertEquals(4, items.size)
        assertTrue(items.all { it.route.isNotBlank() && it.title.isNotBlank() })
    }
}
