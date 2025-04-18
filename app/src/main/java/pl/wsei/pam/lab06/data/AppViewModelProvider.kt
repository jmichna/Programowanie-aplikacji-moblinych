package pl.wsei.pam.lab06.data

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            // Dla ekranu listy (ListViewModel)
            ListViewModel(
                repository = todoApplication().container.todoTaskRepository
            )
        }
        initializer {
            // Dla ekranu formularza (FormViewModel)
            FormViewModel(
                repository = todoApplication().container.todoTaskRepository,
                dateProvider = todoApplication().container.currentDateProvider
            )
        }
    }
}

fun CreationExtras.todoApplication(): TodoApplication {
    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
    return app as TodoApplication
}
