package pl.wsei.pam.lab06.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoTaskDao {
    @Insert
    suspend fun insertAll(vararg tasks: TodoTaskEntity)

    @Delete
    suspend fun removeById(item: TodoTaskEntity)

    @Update
    suspend fun update(item: TodoTaskEntity)

    @Query("Select * from tasks")
    fun findAll(): Flow<List<TodoTaskEntity>>

    @Query("Select * from tasks where id == :id LIMIT 1")
    fun find(id: Int): Flow<TodoTaskEntity?>

    @Query("SELECT * FROM tasks WHERE title = :title LIMIT 1")
    suspend fun findByTitle(title: String): TodoTaskEntity?
}
