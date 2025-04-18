package pl.wsei.pam.lab06.data

import kotlinx.coroutines.flow.Flow
import pl.wsei.pam.lab06.TodoTask

interface TodoTaskRepository {
    fun getAllAsStream(): Flow<List<TodoTask>>
    fun getItemAsStream(id: Int): Flow<TodoTask?>
    suspend fun getAllOnce(): List<TodoTask>
    suspend fun insertItem(item: TodoTask)
    suspend fun deleteItem(item: TodoTask)
    suspend fun updateItem(item: TodoTask)
    suspend fun getItemOnce(id: Int): TodoTask?
    suspend fun getItemByTitleOnce(title: String): TodoTask?
}