package com.zaziapps.mtg_scanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaziapps.mtg_scanner.data.model.CustomCollection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCollectionsListScreen(
    customCollections: List<CustomCollection>,
    onMenuClick: () -> Unit,
    onCollectionClick: (CustomCollection) -> Unit,
    onAddCollectionConfirm: (name: String, description: String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var newCollectionDesc by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MIS COLECCIONES") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir Colección")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (customCollections.isEmpty()) {
                Text(
                    text = "Aún no has creado colecciones personalizadas.\nPresiona + para crear una.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(customCollections) { collection ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCollectionClick(collection) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = collection.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (collection.description.isNotEmpty()) {
                                        Text(
                                            text = collection.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(
                                        text = "${collection.cardCount} cartas",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    newCollectionName = ""
                    newCollectionDesc = ""
                },
                title = { Text("Nueva Colección") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCollectionName,
                            onValueChange = { newCollectionName = it },
                            label = { Text("Nombre de la colección") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newCollectionDesc,
                            onValueChange = { newCollectionDesc = it },
                            label = { Text("Descripción (Opcional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = newCollectionName.isNotBlank(),
                        onClick = {
                            onAddCollectionConfirm(newCollectionName, newCollectionDesc)
                            showDialog = false
                            newCollectionName = ""
                            newCollectionDesc = ""
                        }
                    ) {
                        Text("Crear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
