package pl.wsei.pam.lab06.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import pl.wsei.pam.lab06.Priority
import pl.wsei.pam.lab06.TodoTask
import java.time.LocalDate

class FormViewModel(
    private val repository: TodoTaskRepository,
    private val dateProvider: CurrentDateProvider
) : ViewModel() {


    private val _uiState = MutableStateFlow(TodoTaskUiState())
    var uiState: StateFlow<TodoTaskUiState> = _uiState

    suspend fun loadItem(id: Int) {
        repository.getItemAsStream(id).firstOrNull()?.let { task ->
            _uiState.value = task.toTodoTaskUiState(isValid = true)
        }
    }

    suspend fun save() {
        if (!validate()) return
        val task = _uiState.value.todoTask.toTodoTask()
        if (task.id == 0) repository.insertItem(task)
        else               repository.updateItem(task)
    }

    fun clear() {
        _uiState.value = TodoTaskUiState()
    }

    fun updateUiState(form: TodoTaskForm) {
        _uiState.value = TodoTaskUiState(
            todoTask = form,
            isValid = validate(form)
        )
    }

    private fun validate(form: TodoTaskForm = _uiState.value.todoTask): Boolean {
        val today = dateProvider.currentDate
        val selected = LocalDateConverter.fromMillis(form.deadline)
        return form.title.isNotBlank() && !selected.isBefore(today)
    }
}

data class TodoTaskUiState(
    var todoTask: TodoTaskForm = TodoTaskForm(),
    val isValid: Boolean = false
)

data class TodoTaskForm(
    val id: Int = 0,
    val title: String = "",
    val deadline: Long = LocalDateConverter.toMillis(LocalDate.now()),
    val isDone: Boolean = false,
    val priority: String = Priority.Niski.name
)

fun TodoTask.toTodoTaskUiState(isValid: Boolean = false): TodoTaskUiState = TodoTaskUiState(
    todoTask = this.toTodoTaskForm(),
    isValid = isValid
)

fun TodoTaskForm.toTodoTask(): TodoTask = TodoTask(
    id = id,
    title = title,
    deadline = LocalDateConverter.fromMillis(deadline),
    isDone = isDone,
    priority = Priority.valueOf(priority)
)

fun TodoTask.toTodoTaskForm(): TodoTaskForm = TodoTaskForm(
    id = id,
    title = title,
    deadline = LocalDateConverter.toMillis(deadline),
    isDone = isDone,
    priority = priority.name
)
