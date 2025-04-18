package pl.wsei.pam.lab06.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import pl.wsei.pam.lab06.TodoTask

class DatabaseTodoTaskRepository(val dao: TodoTaskDao) : TodoTaskRepository {

    override fun getAllAsStream(): Flow<List<TodoTask>> {
        return dao.findAll().map { it ->
            it.map {
                it.toModel()
            }
        }
    }

    override fun getItemAsStream(id: Int): Flow<TodoTask?> {
        return dao.find(id).map {
            it?.toModel()
        }
    }

    override suspend fun getAllOnce(): List<TodoTask> {
        val entities = dao.findAll().first()
        return entities.map { it.toModel() }
    }

    override suspend fun insertItem(item: TodoTask) {
        dao.insertAll(TodoTaskEntity.fromModel(item))
    }

    override suspend fun deleteItem(item: TodoTask) {
        dao.removeById(TodoTaskEntity.fromModel(item))
    }

    override suspend fun updateItem(item: TodoTask) {
        dao.update(TodoTaskEntity.fromModel(item))
    }

    override suspend fun getItemOnce(id: Int): TodoTask? =
        dao.find(id).firstOrNull()?.toModel()

    override suspend fun getItemByTitleOnce(title: String): TodoTask? =
        dao.findByTitle(title)?.toModel()
}