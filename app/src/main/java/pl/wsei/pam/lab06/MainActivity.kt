package pl.wsei.pam.lab06

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pl.wsei.pam.MainActivity
import pl.wsei.pam.lab06.data.AppContainer
import pl.wsei.pam.lab06.data.AppViewModelProvider
import pl.wsei.pam.lab06.data.FormViewModel
import pl.wsei.pam.lab06.data.ListViewModel
import pl.wsei.pam.lab06.data.LocalDateConverter
import pl.wsei.pam.lab06.data.LocalDateConverter.Companion.toEpochMillis
import pl.wsei.pam.lab06.data.NotificationBroadcastReceiver
import pl.wsei.pam.lab06.data.TodoApplication
import java.time.LocalDate

class Lab06Activity : ComponentActivity() {

    companion object {
        lateinit var container: AppContainer
        var currentPendingIntent: PendingIntent? = null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = (application as TodoApplication).container
        createNotificationChannel()

        setContent {
            Lab06Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }

        // Wywołaj funkcję planującą powiadomienie
        scheduleNearestUndoneTaskAlarm()
    }

    var currentPendingIntent: PendingIntent? = null  // globalnie np. w companion object

    private fun scheduleNearestUndoneTaskAlarm() = runBlocking {
        // 1) Pobierz z bazy wszystkie zadania
        val allTasks = container.todoTaskRepository.getAllOnce()
        // 2) Filtr niewykonanych, sort
        val undoneSorted = allTasks
            .filter { !it.isDone }
            .sortedBy { it.deadline }

        if (undoneSorted.isNotEmpty()) {
            // Anuluj poprzedni alarm
            cancelCurrentAlarm()

            // Najbliższe zadanie
            val nearest = undoneSorted.first()

            // Wczytaj preferencje
            val settings = loadAlarmSettings()  // definicja loadAlarmSettings() poniżej

            // Wylicz moment alarmu = 1 dzień przed deadline
            val daysBefore = settings.daysBefore  // np. 1
            val triggerTime = nearest.deadline.toEpochMillis() - (daysBefore * 24L * 60 * 60 * 1000)

            // Jeżeli termin już minął, to nie ustawiamy
            if (triggerTime > System.currentTimeMillis()) {
                scheduleExactAlarm(
                    triggerTime,
                    nearest.title,
                    intervalHours = settings.repeatIntervalHours
                )
            }
        }
    }


    // Anulowanie obecnego alarmu
    private fun cancelCurrentAlarm() {
        currentPendingIntent?.let { pending ->
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pending)
            currentPendingIntent = null
        }
    }

    // Tworzymy nowy alarm
    fun scheduleExactAlarm(triggerAt: Long, taskTitle: String, intervalHours: Int = 0 ) {
        val intent = Intent(this, NotificationBroadcastReceiver::class.java).apply {
            putExtra(titleExtra,   taskTitle)
            putExtra(messageExtra, "Zbliża się termin zadania!")
        }

        val pi = PendingIntent.getBroadcast(
            this,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        currentPendingIntent = pi

        val alarmMgr = getSystemService(ALARM_SERVICE) as AlarmManager

        if (intervalHours > 0) {
            val intervalMs = intervalHours * 60L * 60 * 1_000
            alarmMgr.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                intervalMs,
                pi
            )
        } else {
            alarmMgr.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pi
            )
        }
    }

    private fun createNotificationChannel() {
        val name = "Lab06 channel"
        val descriptionText = "Lab06 is channel for notifications for approaching tasks."
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance).apply {
            description = descriptionText
        }
        getSystemService(this, NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun loadAlarmSettings(): AlarmSettings {
        val prefs = getSharedPreferences("alarm_prefs", MODE_PRIVATE)
        val days = prefs.getInt("daysBefore", 1)
        val hours = prefs.getInt("repeatHours", 4)
        return AlarmSettings(days, hours)
    }

    fun saveAlarmSettings(settings: AlarmSettings) {
        val prefs = getSharedPreferences("alarm_prefs", MODE_PRIVATE)
        prefs.edit()
            .putInt("daysBefore", settings.daysBefore)
            .putInt("repeatHours", settings.repeatIntervalHours)
            .apply()
    }
}

const val notificationID = 121
const val channelID = "Lab06 channel"
const val titleExtra = "title"
const val messageExtra = "message"

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") { ListScreen(navController = navController) }
        composable("settings") { SettingsScreen(navController) }
        composable(
            route = "form?itemId={itemId}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments!!.getInt("itemId")
            FormScreen(navController, itemId = id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    navController: NavController,
    title: String,
    showBackIcon: Boolean,
    route: String,
    onSaveClick: () -> Unit = {}
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (showBackIcon) {
                IconButton(onClick = {
                    navController.navigateUp()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            // Pokaż "Zapisz" TYLKO gdy route == "form"
            if (route == "form") {
                OutlinedButton(
                    onClick = onSaveClick
                ) {
                    Text(
                        text = "Zapisz",
                        fontSize = 18.sp
                    )
                }
            } else {
                // Ekran listy (route="list") – przykładowe ikony
                IconButton(onClick = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                        restoreState    = true
                    }
                }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "")
                }
                IconButton(onClick = {
                    // Home
                }) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = "")
                }
                IconButton(onClick = {
                    Lab06Activity.container.notificationHandler.showSimpleNotification()
                }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(navController: NavController, viewModel: ListViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val listUiState by viewModel.listUiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("form") },
                shape = RectangleShape
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add task",
                    modifier = Modifier.scale(1.5f)
                )
            }
        },
        topBar = {
            AppTopBar(
                navController = navController,
                title = "List",
                showBackIcon = false,
                route = "list"
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {

                items(listUiState.items, key = { it.id }) { item ->
                    ListItem(
                        item = item,
                        onDelete = { toDelete ->
                            viewModel.delete(toDelete)
                        },
                        onEdit = { chosenItem ->
                            navController.navigate("form?itemId=${chosenItem.id}")
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun ListItem(item: TodoTask, onDelete: (TodoTask) -> Unit, modifier: Modifier = Modifier, onEdit: (TodoTask) -> Unit = {}) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onEdit(item) },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = item.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Wykonanie do dnia: ${item.deadline}",
                fontSize = 14.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Zrobione: ")
                val decoration = if (item.isDone) TextDecoration.Underline else null
                Text(
                    text = if (item.isDone) "Tak" else "Nie",
                    textDecoration = decoration,
                    fontSize = 16.sp
                )
            }

            Text(text = "Priority: ${item.priority}")

            Row(verticalAlignment = Alignment.CenterVertically) {
                // np. Tekst/ikona do usunięcia
                IconButton(
                    onClick = { onDelete(item) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,  // musisz dodać import
                        contentDescription = "Delete task"
                    )
                }
                Text("Usuń zadanie")
            }
        }
    }
}

 /* fun todoTasks(): List<TodoTask> {
    return listOf(
        TodoTask(1,"Programming", LocalDate.of(2024, 4, 18), false, Priority.Niski),
        TodoTask(2,"Teaching", LocalDate.of(2024, 5, 12), false, Priority.Wysoki),
        TodoTask(3,"Learning", LocalDate.of(2024, 6, 28), true, Priority.Niski),
        TodoTask(4,"Cooking", LocalDate.of(2024, 8, 18), false, Priority.Sredni),
    )
} */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    navController: NavController,
    itemId: Int,
    viewModel: FormViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {

    LaunchedEffect(itemId) {
        if (itemId != 0) {                   // tryb edycji
            viewModel.loadItem(itemId)       // <- dodajesz tę metodę w VM
        } else {                             // tryb „nowe”
            viewModel.clear()                // <- reset stanu (metoda pomocnicza)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = "Form",
                showBackIcon = true,
                route = "form",
                onSaveClick = {
                    coroutineScope.launch {
                        if (uiState.isValid) {
                            viewModel.save()
                            navController.navigateUp()
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // Główna kolumna
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pole tytułu
            OutlinedTextField(
                value = uiState.todoTask.title,
                onValueChange = {
                    // Kopiujemy stan z nowym tytułem
                    viewModel.updateUiState(uiState.todoTask.copy(title = it))
                },
                label = { Text("Tytuł") },
                modifier = Modifier.fillMaxWidth()
            )

            // Wybór daty (deadline)
            DeadlinePicker(
                dateMillis = uiState.todoTask.deadline,
                onDateSelected = { newMillis ->
                    viewModel.updateUiState(uiState.todoTask.copy(deadline = newMillis))
                }
            )

            // Switch: isDone
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = uiState.todoTask.isDone,
                    onCheckedChange = { checked ->
                        viewModel.updateUiState(uiState.todoTask.copy(isDone = checked))
                    }
                )
                Text(
                    text = if (uiState.todoTask.isDone) "Zrobione" else "Nie zrobione",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Radio buttons – wybór priorytetu
            Text(text = "Priority:")
            PriorityRadioGroup(
                selectedPriority = uiState.todoTask.priority,
                onPrioritySelected = { newPriority ->
                    viewModel.updateUiState(uiState.todoTask.copy(priority = newPriority))
                }
            )

            // Komunikat walidacji
            if (!uiState.isValid) {
                Text(
                    text = "Uwaga: tytuł nie może być pusty, a data nie wcześniejsza od dziś.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Przykładowy przycisk Zapisz
            Button(
                onClick = {
                    coroutineScope.launch {
                        if (uiState.isValid) {
                            viewModel.save()
                            navController.navigate("list")
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Zapisz zadanie")
            }
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    // Stan do edycji
    var daysBefore by remember { mutableStateOf(1) }
    var repeatHours by remember { mutableStateOf(4) }

    // Wczytanie początkowe z preferencji (można w LaunchedEffect)
    LaunchedEffect(true) {
        val settings = (context as Lab06Activity).loadAlarmSettings()
        daysBefore = settings.daysBefore
        repeatHours = settings.repeatIntervalHours
    }

    Scaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = "Settings",
                showBackIcon = true,
                route = "list"
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Input do daysBefore
            Text("Ile dni przed terminem?")
            OutlinedTextField(
                value = daysBefore.toString(),
                onValueChange = { newValue ->
                    daysBefore = newValue.toIntOrNull() ?: 1
                }
            )
            // Input do repeatHours
            Text("Co ile godzin powtarzać?")
            OutlinedTextField(
                value = repeatHours.toString(),
                onValueChange = { newValue ->
                    repeatHours = newValue.toIntOrNull() ?: 4
                }
            )
            Button(onClick = {
                (context as Lab06Activity).saveAlarmSettings(
                    AlarmSettings(
                        daysBefore = daysBefore,
                        repeatIntervalHours = repeatHours
                    )
                )
                navController.navigateUp()
            }) {
                Text("Zapisz ustawienia")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlinePicker(
    dateMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateMillis
    )

    LaunchedEffect(dateMillis) {
        if (datePickerState.selectedDateMillis != dateMillis) {
            datePickerState.selectedDateMillis = dateMillis
        }
    }

    var showDialog by remember { mutableStateOf(false) }

    val displayDate = datePickerState.selectedDateMillis
        ?.let { LocalDateConverter.fromMillis(it).toString() }
        ?: "Brak daty"

    Column {
        Text("Deadline: $displayDate")
        Button(onClick = { showDialog = true }) { Text("Wybierz datę") }
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        datePickerState.selectedDateMillis
                            ?.let(onDateSelected)
                    }
                ) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun PriorityRadioGroup(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit
) {
    val priorities = listOf(Priority.Niski.name, Priority.Sredni.name, Priority.Wysoki.name)
    Column {
        priorities.forEach { prio ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (prio == selectedPriority),
                    onClick = { onPrioritySelected(prio) }
                )
                Text(text = prio)
            }
        }
    }
}


enum class Priority {
    Wysoki, Sredni, Niski
}

data class TodoTask(
    val id: Int = 0,
    val title: String,
    val deadline: LocalDate,
    val isDone: Boolean,
    val priority: Priority
)

data class AlarmSettings(
    val daysBefore: Int = 1,
    val repeatIntervalHours: Int = 4
)

@Composable
fun Lab06Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = Typography(),
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    Lab06Theme {
        MainScreen()
    }
}