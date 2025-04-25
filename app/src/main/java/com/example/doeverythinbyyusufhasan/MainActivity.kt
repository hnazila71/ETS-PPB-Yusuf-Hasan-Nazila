package com.example.doeverythinbyyusufhasan

import com.example.doeverythinbyyusufhasan.TaskStorage

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.doeverythinbyyusufhasan.ui.theme.DoEverythinByYusufHasanTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class Task(
    val id: Int,
    val title: String,
    val deadline: LocalDateTime,
    val isDone: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DoEverythinByYusufHasanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TaskManagerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerApp() {
    val tasks = remember { mutableStateListOf<Task>() }
    val context = LocalContext.current

    // Load tasks saat pertama kali
    LaunchedEffect(Unit) {
        val loadedTasks = TaskStorage.loadTasks(context)
        android.util.Log.d("MainActivity", "Loaded tasks: ${loadedTasks.size}")
        tasks.clear()
        tasks.addAll(loadedTasks)
    }

    // Simpan setiap kali tasks berubah (menggunakan snapshotFlow)
    LaunchedEffect(Unit) {
        snapshotFlow { tasks.toList() }
            .collect {
                android.util.Log.d("MainActivity", "Saving ${it.size} tasks...")
                TaskStorage.saveTasks(context, it)
            }
    }

    val showDialog = remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
    val todayFormatted = today.format(formatter)

    val now = LocalDateTime.now()
    val nearestDeadline = tasks.filter { !it.isDone && it.deadline.isAfter(now) }
        .minByOrNull { it.deadline }
        ?.deadline?.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")) ?: "-"

    var sortMode by remember { mutableStateOf("deadline") } // "deadline" atau "name"

    val activeTasks = tasks.filter { !it.isDone }.let {
        if (sortMode == "name") it.sortedBy { task -> task.title }
        else it.sortedBy { task -> task.deadline }
    }

    val completedTasks = tasks.filter { it.isDone }.let {
        if (sortMode == "name") it.sortedBy { task -> task.title }
        else it.sortedBy { task -> task.deadline }
    }

    Scaffold(
        topBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Daily Task Manager",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hari ini: $todayFormatted",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Deadline terdekat: $nearestDeadline",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog.value = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 62.dp, end = 32.dp)
            ) {
                Text("Tambah Tugas")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (showDialog.value) {
                TaskInputDialog(
                    onAdd = {
                        tasks.add(it)
                        showDialog.value = false
                    },
                    onDismiss = { showDialog.value = false }
                )
            }

            Text("Urutkan berdasarkan:", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { sortMode = "name" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sortMode == "name") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (sortMode == "name") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Nama")
                }

                Button(
                    onClick = { sortMode = "deadline" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sortMode == "deadline") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (sortMode == "deadline") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Deadline")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OverdueTaskList(activeTasks, tasks)
            Text("Tugas Aktif", style = MaterialTheme.typography.titleMedium)
            TaskList(activeTasks, tasks)

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Completed", style = MaterialTheme.typography.titleMedium)
            TaskList(completedTasks, tasks)
        }
    }
}

@Composable
fun TaskInputDialog(onAdd: (Task) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var selectedDateTime by remember { mutableStateOf<LocalDateTime?>(null) }

    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    val calendar = Calendar.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (title.text.isNotBlank() && selectedDateTime != null) {
                        onAdd(
                            Task(
                                id = title.text.hashCode(),
                                title = title.text,
                                deadline = selectedDateTime!!
                            )
                        )
                    }
                }
            ) {
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
        title = { Text("Tambah Tugas") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Tugas") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    DatePickerDialog(context, { _, y, m, d ->
                        TimePickerDialog(context, { _, h, min ->
                            selectedDateTime = LocalDateTime.of(y, m + 1, d, h, min)
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                }) {
                    Text("Pilih Deadline")
                }
                selectedDateTime?.let {
                    Text("Deadline: ${it.format(formatter)}")
                }
            }
        }
    )
}

@Composable
fun TaskList(tasks: List<Task>, allTasks: MutableList<Task>) {
    items@ LazyColumn {
        items(tasks, key = { it.id }) { task ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                if (task.isDone) {
                    CompletedTaskItemContent(task, { updated ->
                        val index = allTasks.indexOfFirst { it.id == task.id }
                        if (index != -1) {
                            allTasks[index] = allTasks[index].copy(isDone = !task.isDone)
                        }
                    }, { allTasks.remove(task) })
                } else {
                    ActiveTaskItemContent(task, { updated ->
                        val index = allTasks.indexOfFirst { it.id == task.id }
                        if (index != -1) {
                            allTasks[index] = allTasks[index].copy(isDone = !task.isDone)
                        }
                    }, { allTasks.remove(task) })
                }
            }
        }
    }
}

@Composable
fun OverdueTaskList(tasks: List<Task>, allTasks: MutableList<Task>) {
    val now = LocalDateTime.now()
    val overdueTasks = tasks.filter { !it.isDone && it.deadline.isBefore(now) }
    if (overdueTasks.isNotEmpty()) {
        Text(" Terlewati", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(overdueTasks, key = { it.id }) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    ActiveTaskItemContent(task, { updated ->
                        val index = allTasks.indexOfFirst { it.id == task.id }
                        if (index != -1) {
                            allTasks[index] = allTasks[index].copy(isDone = !task.isDone)
                        }
                    }, { allTasks.remove(task) })
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ActiveTaskItemContent(task: Task, onToggleDone: (Task) -> Unit, onDelete: (Task) -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(
                    checked = task.isDone,
                    onCheckedChange = { onToggleDone(task) }
                )
                Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(onClick = { onDelete(task) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
        Text(text = task.deadline.format(formatter), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CompletedTaskItemContent(task: Task, onToggleDone: (Task) -> Unit, onDelete: (Task) -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggleDone(task) })
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.merge(
                    TextStyle(textDecoration = TextDecoration.LineThrough)
                )
            )
        }
        Text(text = task.deadline.format(formatter), style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTaskManager() {
    DoEverythinByYusufHasanTheme {
        TaskManagerApp()
    }
}