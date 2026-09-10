package com.example.agendatecnico

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AgendaApp() }
    }
}

@Composable
fun AgendaApp() {
    val context = LocalContext.current
    val db = remember { com.example.agendatecnico.data.AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf("clients") }
    var selectedClient by remember { mutableStateOf<Long?>(null) }
    var selectedMachine by remember { mutableStateOf<Long?>(null) }
    var selectedRepair by remember { mutableStateOf<Long?>(null) }

    MaterialTheme {
        when (screen) {
            "clients" -> ClientList(
                db = db,
                onClient = { selectedClient = it; screen = "machines" },
                onAdd = { screen = "addClient" }
            )
            "addClient" -> ClientForm(
                onBack = { screen = "clients" },
                onSave = { c ->
                    scope.launch {
                        db.clientDao().insert(c)
                        screen = "clients"
                    }
                }
            )
            "machines" -> MachineList(
                db = db,
                clientId = selectedClient!!,
                onBack = { screen = "clients" },
                onMachine = { selectedMachine = it; screen = "repairs" },
                onAdd = { screen = "addMachine" }
            )
            "addMachine" -> MachineForm(
                onBack = { screen = "machines" },
                onSave = { m ->
                    scope.launch {
                        db.machineDao().insert(m)
                        screen = "machines"
                    }
                },
                clientId = selectedClient!!
            )
            "repairs" -> RepairList(
                db = db,
                machineId = selectedMachine!!,
                onBack = { screen = "machines" },
                onRepair = { selectedRepair = it; screen = "repairDetail" },
                onAdd = { screen = "addRepair" }
            )
            "addRepair" -> RepairForm(
                onBack = { screen = "repairs" },
                onSave = { r ->
                    scope.launch {
                        val id = db.repairDao().insert(r)
                        selectedRepair = id
                        screen = "repairDetail"
                    }
                },
                machineId = selectedMachine!!
            )
            "repairDetail" -> RepairDetail(
                db = db,
                repairId = selectedRepair!!,
                onBack = { screen = "repairs" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)? = null, action: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Atrás")
            }
        },
        actions = {
            if (action != null) IconButton(onClick = action) {
                Icon(Icons.Default.Add, "Agregar")
            }
        }
    )
}

@Composable
fun ClientList(
    db: com.example.agendatecnico.data.AppDatabase,
    onClient: (Long) -> Unit,
    onAdd: () -> Unit
) {
    val clients by db.clientDao().observeAll().collectAsState(initial = emptyList())
    Scaffold(
        topBar = { AppTopBar("Agenda del Técnico", action = onAdd) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Text(
                "Clientes",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
            if (clients.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay clientes. Pulsa + para agregar el primero.")
                }
            } else {
                LazyColumn {
                    items(clients) { c ->
                        ListItem(
                            headlineContent = { Text(c.name) },
                            supportingContent = {
                                Text(listOf(c.phone, c.city).filter { it.isNotBlank() }.joinToString(" • "))
                            },
                            leadingContent = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.clickable { onClient(c.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun ClientForm(onBack: () -> Unit, onSave: (com.example.agendatecnico.data.Client) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    FormScaffold("Nuevo cliente", onBack) {
        Field("Nombre / Razón social", name) { name = it }
        Field("Teléfono", phone) { phone = it }
        Field("Correo electrónico", email) { email = it }
        Field("Domicilio", address) { address = it }
        Field("Ciudad", city) { city = it }
        Field("Notas", notes, singleLine = false) { notes = it }
        Button(
            onClick = { if (name.isNotBlank()) onSave(com.example.agendatecnico.data.Client(
                name = name, phone = phone, email = email, address = address, city = city, notes = notes
            )) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar cliente") }
    }
}

@Composable
fun MachineList(
    db: com.example.agendatecnico.data.AppDatabase,
    clientId: Long,
    onBack: () -> Unit,
    onMachine: (Long) -> Unit,
    onAdd: () -> Unit
) {
    val machines by db.machineDao().observeForClient(clientId).collectAsState(initial = emptyList())
    Scaffold(topBar = { AppTopBar("Máquinas del cliente", onBack, onAdd) }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(machines) { m ->
                ListItem(
                    headlineContent = { Text("${m.brand} ${m.model}".trim().ifBlank { "Máquina sin marca/modelo" }) },
                    supportingContent = {
                        Text(listOf(m.machineType, "N° serie: ${m.serialNumber}").filter { it.isNotBlank() }.joinToString(" • "))
                    },
                    leadingContent = { Icon(Icons.Default.Build, null) },
                    modifier = Modifier.clickable { onMachine(m.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun MachineForm(
    onBack: () -> Unit,
    onSave: (com.example.agendatecnico.data.Machine) -> Unit,
    clientId: Long
) {
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    FormScaffold("Nueva máquina", onBack) {
        Field("Marca", brand) { brand = it }
        Field("Modelo", model) { model = it }
        Field("Número de serie", serial) { serial = it }
        Field("Tipo de máquina", type) { type = it }
        Field("Estado / condición", condition) { condition = it }
        Field("Observaciones", notes, singleLine = false) { notes = it }
        Button(
            onClick = { onSave(com.example.agendatecnico.data.Machine(
                clientId = clientId, brand = brand, model = model, serialNumber = serial,
                machineType = type, condition = condition, notes = notes
            )) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar máquina") }
    }
}

@Composable
fun RepairList(
    db: com.example.agendatecnico.data.AppDatabase,
    machineId: Long,
    onBack: () -> Unit,
    onRepair: (Long) -> Unit,
    onAdd: () -> Unit
) {
    val repairs by db.repairDao().observeForMachine(machineId).collectAsState(initial = emptyList())
    Scaffold(topBar = { AppTopBar("Historial de reparaciones", onBack, onAdd) }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(repairs) { r ->
                ListItem(
                    headlineContent = { Text(r.date) },
                    supportingContent = { Text("${r.status} • ${r.description}") },
                    leadingContent = { Icon(Icons.Default.BuildCircle, null) },
                    modifier = Modifier.clickable { onRepair(r.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun RepairForm(
    onBack: () -> Unit,
    onSave: (com.example.agendatecnico.data.Repair) -> Unit,
    machineId: Long
) {
    var description by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf("") }
    var labor by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }

    FormScaffold("Nueva reparación", onBack) {
        Field("Fecha", today(), readOnly = true) {}
        Field("Descripción del trabajo realizado", description, false) { description = it }
        Field("Diagnóstico", diagnosis, false) { diagnosis = it }
        Field("Repuestos utilizados", parts, false) { parts = it }
        Field("Mano de obra", labor) { labor = it }
        Field("Total", total) { total = it }
        Button(
            onClick = { if (description.isNotBlank()) onSave(com.example.agendatecnico.data.Repair(
                machineId = machineId,
                date = today(),
                description = description,
                diagnosis = diagnosis,
                parts = parts,
                labor = labor,
                total = total.replace(",", ".").toDoubleOrNull() ?: 0.0
            )) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar reparación") }
    }
}

@Composable
fun RepairDetail(
    db: com.example.agendatecnico.data.AppDatabase,
    repairId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var repair by remember { mutableStateOf<com.example.agendatecnico.data.Repair?>(null) }
    val photos by db.photoDao().observeForRepair(repairId).collectAsState(initial = emptyList())

    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            tempUri?.let { uri ->
                scope.launch {
                    db.photoDao().insert(com.example.agendatecnico.data.RepairPhoto(
                        repairId = repairId, uri = uri.toString()
                    ))
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.filesDir, "pictures").apply { mkdirs() }
            val photo = File(file, "repair_${repairId}_${System.currentTimeMillis()}.jpg")
            tempUri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", photo
            )
            cameraLauncher.launch(tempUri)
        }
    }

    LaunchedEffect(repairId) {
        repair = db.repairDao().get(repairId)
    }

    Scaffold(topBar = { AppTopBar("Detalle de reparación", onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repair?.let { r ->
                item { Text("Fecha: ${r.date}", style = MaterialTheme.typography.titleMedium) }
                item { Text("Trabajo realizado: ${r.description}") }
                if (r.diagnosis.isNotBlank()) item { Text("Diagnóstico: ${r.diagnosis}") }
                if (r.parts.isNotBlank()) item { Text("Repuestos: ${r.parts}") }
                if (r.labor.isNotBlank()) item { Text("Mano de obra: ${r.labor}") }
                item { Text("Total: ${"%.2f".format(r.total)}") }
            }

            item {
                Button(
                    onClick = {
                        repair?.let { r ->
                            val message = buildString {
                                append("🔧 REPORTE DE REPARACIÓN\\n\\n")
                                append("Fecha: ${r.date}\\n")
                                append("Trabajo realizado: ${r.description}\\n")
                                if (r.diagnosis.isNotBlank()) append("Diagnóstico: ${r.diagnosis}\\n")
                                if (r.parts.isNotBlank()) append("Repuestos: ${r.parts}\\n")
                                if (r.labor.isNotBlank()) append("Mano de obra: ${r.labor}\\n")
                                append("Total: ${"%.2f".format(r.total)}\\n")
                                append("Estado: ${r.status}")
                            }

                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                                setPackage("com.whatsapp")
                            }

                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // Si WhatsApp no está instalado, se abre el selector de compartir.
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, message)
                                        },
                                        "Compartir reparación"
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir por WhatsApp")
                }
            }

            item {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                            val dir = File(context.filesDir, "pictures").apply { mkdirs() }
                            val photo = File(dir, "repair_${repairId}_${System.currentTimeMillis()}.jpg")
                            tempUri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", photo
                            )
                            cameraLauncher.launch(tempUri)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tomar foto del equipo")
                }
            }

            item {
                Text("Fotos del trabajo (${photos.size})", style = MaterialTheme.typography.titleMedium)
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.heightIn(min = 0.dp, max = 500.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photos) { p ->
                        AsyncImage(
                            model = Uri.parse(p.uri),
                            contentDescription = "Foto de reparación",
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = { AppTopBar(title, onBack) }) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
            }
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        readOnly = readOnly,
        minLines = if (singleLine) 1 else 3
    )
}

fun today(): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
