package com.example.doeverythinbyyusufhasan

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TaskStorage {
    private const val FILE_NAME = "tasks.json"
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    // Format tanggal untuk serialize/deserialize
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @Serializable
    data class TaskData(
        val id: Int,
        val title: String,
        val deadline: String,
        val isDone: Boolean
    )

    fun saveTasks(context: Context, tasks: List<Task>) {
        val file = File(context.filesDir, FILE_NAME)
        val taskDataList = tasks.map {
            TaskData(
                id = it.id,
                title = it.title,
                deadline = it.deadline.format(formatter),
                isDone = it.isDone
            )
        }
        file.writeText(json.encodeToString(taskDataList))
    }

    fun loadTasks(context: Context): List<Task> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val content = file.readText()
            val taskDataList = json.decodeFromString<List<TaskData>>(content)
            taskDataList.map {
                Task(
                    id = it.id,
                    title = it.title,
                    deadline = LocalDateTime.parse(it.deadline, formatter),
                    isDone = it.isDone
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}