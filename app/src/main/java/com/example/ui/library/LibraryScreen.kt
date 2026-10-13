package com.example.ui.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CollectionEntity
import com.example.data.model.MaterialEntity
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val breadcrumbs by viewModel.breadcrumbs.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Dialog States
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTargetCol by remember { mutableStateOf<CollectionEntity?>(null) }
    var renameTargetMat by remember { mutableStateOf<MaterialEntity?>(null) }
    var colorPickerCol by remember { mutableStateOf<CollectionEntity?>(null) }
    var moveTargetCol by remember { mutableStateOf<CollectionEntity?>(null) }
    var moveTargetMat by remember { mutableStateOf<MaterialEntity?>(null) }
    var deleteTargetCol by remember { mutableStateOf<CollectionEntity?>(null) }
    var deleteTargetMat by remember { mutableStateOf<MaterialEntity?>(null) }
    var imagePreviewMat by remember { mutableStateOf<MaterialEntity?>(null) }
    var customCoverTargetColId by remember { mutableStateOf<String?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }

    // SAF File Picker for materials
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importMaterial(it) }
    }

    // Image Picker for custom covers
    val coverImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val colId = customCoverTargetColId
        if (uri != null && colId != null) {
            viewModel.setCustomCover(colId, uri)
        }
        customCoverTargetColId = null
    }

    // Snackbars
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Back handling for nested collections
    BackHandler(enabled = uiState.currentCollectionId != null || isSearchActive) {
        if (isSearchActive) {
            isSearchActive = false
            viewModel.onSearchQueryChanged("")
        } else {
            val parentId = uiState.currentCollection?.parentId
            viewModel.navigateToCollection(parentId)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search collections & files...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("library_search_input"),
                            trailingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    viewModel.onSearchQueryChanged("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            }
                        )
                    } else {
                        Column {
                            Text(
                                text = uiState.currentCollection?.name ?: "Study Library",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.currentCollectionId == null) {
                                Text(
                                    text = "All Collections & Learning Spaces",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (uiState.currentCollectionId != null && !isSearchActive) {
                        IconButton(
                            onClick = {
                                val parentId = uiState.currentCollection?.parentId
                                viewModel.navigateToCollection(parentId)
                            },
                            modifier = Modifier.testTag("library_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search library")
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (uiState.viewMode == "GRID") Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Switch view mode"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary action: compact, visually balanced outlined button
                Surface(
                    onClick = { fileImportLauncher.launch(arrayOf("*/*")) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("fab_import_file")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Import files",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Primary action: New collection
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "New collection",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("fab_add_collection")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Breadcrumbs Row (if inside a nested collection)
            if (breadcrumbs.isNotEmpty() && !isSearchActive) {
                BreadcrumbRow(
                    breadcrumbs = breadcrumbs,
                    onCrumbClick = { crumbId -> viewModel.navigateToCollection(crumbId) },
                    onRootClick = { viewModel.navigateToCollection(null) }
                )
            }

            // Main Content Area
            if (uiState.isSearching) {
                SearchResultsView(
                    collections = uiState.searchCollectionResults,
                    materials = uiState.searchMaterialResults,
                    onCollectionClick = { col ->
                        isSearchActive = false
                        viewModel.onSearchQueryChanged("")
                        viewModel.navigateToCollection(col.id)
                    },
                    onMaterialClick = { mat ->
                        if (mat.mimeType.startsWith("image/")) {
                            imagePreviewMat = mat
                        } else {
                            viewModel.openMaterial(mat)
                        }
                    }
                )
            } else if (uiState.subCollections.isEmpty() && uiState.materials.isEmpty()) {
                EmptyLibraryView(
                    isRoot = uiState.currentCollectionId == null
                )
            } else {
                val onMatClick: (MaterialEntity) -> Unit = { mat ->
                    if (mat.mimeType.startsWith("image/")) {
                        imagePreviewMat = mat
                    } else {
                        viewModel.openMaterial(mat)
                    }
                }

                if (uiState.viewMode == "GRID") {
                    LibraryGridView(
                        subCollections = uiState.subCollections,
                        materials = uiState.materials,
                        onCollectionClick = { col -> viewModel.navigateToCollection(col.id) },
                        onMaterialClick = onMatClick,
                        onCollectionOptions = { col, action ->
                            when (action) {
                                "rename" -> renameTargetCol = col
                                "color" -> colorPickerCol = col
                                "custom_cover" -> {
                                    customCoverTargetColId = col.id
                                    coverImageLauncher.launch("image/*")
                                }
                                "reset_cover" -> viewModel.removeCustomCover(col.id)
                                "move" -> moveTargetCol = col
                                "delete" -> deleteTargetCol = col
                            }
                        },
                        onMaterialOptions = { mat, action ->
                            when (action) {
                                "rename" -> renameTargetMat = mat
                                "move" -> moveTargetMat = mat
                                "delete" -> deleteTargetMat = mat
                            }
                        }
                    )
                } else {
                    LibraryListView(
                        subCollections = uiState.subCollections,
                        materials = uiState.materials,
                        onCollectionClick = { col -> viewModel.navigateToCollection(col.id) },
                        onMaterialClick = onMatClick,
                        onCollectionOptions = { col, action ->
                            when (action) {
                                "rename" -> renameTargetCol = col
                                "color" -> colorPickerCol = col
                                "custom_cover" -> {
                                    customCoverTargetColId = col.id
                                    coverImageLauncher.launch("image/*")
                                }
                                "reset_cover" -> viewModel.removeCustomCover(col.id)
                                "move" -> moveTargetCol = col
                                "delete" -> deleteTargetCol = col
                            }
                        },
                        onMaterialOptions = { mat, action ->
                            when (action) {
                                "rename" -> renameTargetMat = mat
                                "move" -> moveTargetMat = mat
                                "delete" -> deleteTargetMat = mat
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateCollectionDialog(
            parentName = uiState.currentCollection?.name,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.createCollection(name, colorHex)
                showCreateDialog = false
            }
        )
    }

    renameTargetCol?.let { col ->
        RenameDialog(
            initialName = col.name,
            title = "Rename Collection",
            onDismiss = { renameTargetCol = null },
            onConfirm = { newName ->
                viewModel.renameCollection(col.id, newName)
                renameTargetCol = null
            }
        )
    }

    renameTargetMat?.let { mat ->
        RenameDialog(
            initialName = mat.displayName,
            title = "Rename Material",
            onDismiss = { renameTargetMat = null },
            onConfirm = { newName ->
                viewModel.renameMaterial(mat.id, newName)
                renameTargetMat = null
            }
        )
    }

    colorPickerCol?.let { col ->
        ColorPickerDialog(
            currentColorHex = col.coverColorHex,
            onDismiss = { colorPickerCol = null },
            onConfirm = { hex ->
                viewModel.updateCoverColor(col.id, hex)
                colorPickerCol = null
            }
        )
    }

    moveTargetCol?.let { col ->
        MoveCollectionDialog(
            sourceCollection = col,
            allCollections = uiState.allCollections,
            onDismiss = { moveTargetCol = null },
            onConfirm = { targetParentId ->
                viewModel.moveCollection(col.id, targetParentId)
                moveTargetCol = null
            }
        )
    }

    moveTargetMat?.let { mat ->
        MoveMaterialDialog(
            materialName = mat.displayName,
            currentCollectionId = mat.collectionId,
            allCollections = uiState.allCollections,
            onDismiss = { moveTargetMat = null },
            onConfirm = { targetColId ->
                viewModel.moveMaterial(mat.id, targetColId)
                moveTargetMat = null
            }
        )
    }

    deleteTargetCol?.let { col ->
        DeleteConfirmDialog(
            title = "Delete Collection '${col.name}'?",
            message = "This collection, all its nested subcollections, and their associated files will be removed from your library. This cannot be undone.",
            onDismiss = { deleteTargetCol = null },
            onConfirm = {
                viewModel.deleteCollection(col.id)
                deleteTargetCol = null
            }
        )
    }

    deleteTargetMat?.let { mat ->
        DeleteConfirmDialog(
            title = "Delete '${mat.displayName}'?",
            message = "This file will be permanently removed from your app-managed library storage.",
            onDismiss = { deleteTargetMat = null },
            onConfirm = {
                viewModel.deleteMaterial(mat)
                deleteTargetMat = null
            }
        )
    }

    imagePreviewMat?.let { mat ->
        ImagePreviewDialog(
            material = mat,
            onDismiss = { imagePreviewMat = null },
            onOpenExternal = {
                imagePreviewMat = null
                viewModel.openMaterial(mat)
            }
        )
    }
}

@Composable
fun ImagePreviewDialog(
    material: MaterialEntity,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current
    val imageFile = File(context.filesDir, material.relativeFilePath)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = material.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (imageFile.exists()) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = material.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Text("Image file not found on local storage.", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${formatFileSize(material.fileSizeBytes)} • Stored locally",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenExternal) {
                Text("Open Externally")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun BreadcrumbRow(
    breadcrumbs: List<CollectionEntity>,
    onCrumbClick: (String?) -> Unit,
    onRootClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onRootClick() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            items(breadcrumbs) { crumb ->
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = crumb.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (crumb == breadcrumbs.last()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    fontWeight = if (crumb == breadcrumbs.last()) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onCrumbClick(crumb.id) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun LibraryGridView(
    subCollections: List<CollectionEntity>,
    materials: List<MaterialEntity>,
    onCollectionClick: (CollectionEntity) -> Unit,
    onMaterialClick: (MaterialEntity) -> Unit,
    onCollectionOptions: (CollectionEntity, String) -> Unit,
    onMaterialOptions: (MaterialEntity, String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (subCollections.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "COLLECTIONS (${subCollections.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(subCollections, key = { it.id }) { col ->
                CollectionCard(
                    collection = col,
                    onClick = { onCollectionClick(col) },
                    onOptions = { onCollectionOptions(col, it) }
                )
            }
        }

        if (materials.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "STUDY MATERIALS (${materials.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(materials, key = { it.id }) { mat ->
                MaterialCard(
                    material = mat,
                    onClick = { onMaterialClick(mat) },
                    onOptions = { onMaterialOptions(mat, it) }
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun LibraryListView(
    subCollections: List<CollectionEntity>,
    materials: List<MaterialEntity>,
    onCollectionClick: (CollectionEntity) -> Unit,
    onMaterialClick: (MaterialEntity) -> Unit,
    onCollectionOptions: (CollectionEntity, String) -> Unit,
    onMaterialOptions: (MaterialEntity, String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (subCollections.isNotEmpty()) {
            item {
                Text(
                    text = "COLLECTIONS (${subCollections.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(subCollections, key = { it.id }) { col ->
                CollectionListRow(
                    collection = col,
                    onClick = { onCollectionClick(col) },
                    onOptions = { onCollectionOptions(col, it) }
                )
            }
        }

        if (materials.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "STUDY MATERIALS (${materials.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(materials, key = { it.id }) { mat ->
                MaterialListRow(
                    material = mat,
                    onClick = { onMaterialClick(mat) },
                    onOptions = { onMaterialOptions(mat, it) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun CollectionCard(
    collection: CollectionEntity,
    onClick: () -> Unit,
    onOptions: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val customCoverFile = collection.customCoverPath?.let { File(context.filesDir, it) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("collection_card_${collection.id}")
    ) {
        Column {
            // Cover Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(parseHexColor(collection.coverColorHex))
            ) {
                if (customCoverFile != null && customCoverFile.exists()) {
                    AsyncImage(
                        model = customCoverFile,
                        contentDescription = "Collection cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Folder Icon badge
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Overflow button
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { menuExpanded = false; onOptions("rename") }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Accent Color") },
                            onClick = { menuExpanded = false; onOptions("color") }
                        )
                        DropdownMenuItem(
                            text = { Text("Set Custom Image Cover") },
                            onClick = { menuExpanded = false; onOptions("custom_cover") }
                        )
                        if (collection.customCoverPath != null) {
                            DropdownMenuItem(
                                text = { Text("Reset to Color Cover") },
                                onClick = { menuExpanded = false; onOptions("reset_cover") }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Move Collection") },
                            onClick = { menuExpanded = false; onOptions("move") }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Collection", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onOptions("delete") }
                        )
                    }
                }
            }

            // Collection info
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CollectionListRow(
    collection: CollectionEntity,
    onClick: () -> Unit,
    onOptions: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(parseHexColor(collection.coverColorHex)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { menuExpanded = false; onOptions("rename") }
                    )
                    DropdownMenuItem(
                        text = { Text("Change Color") },
                        onClick = { menuExpanded = false; onOptions("color") }
                    )
                    DropdownMenuItem(
                        text = { Text("Set Custom Image Cover") },
                        onClick = { menuExpanded = false; onOptions("custom_cover") }
                    )
                    if (collection.customCoverPath != null) {
                        DropdownMenuItem(
                            text = { Text("Reset to Color Cover") },
                            onClick = { menuExpanded = false; onOptions("reset_cover") }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Move") },
                        onClick = { menuExpanded = false; onOptions("move") }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onOptions("delete") }
                    )
                }
            }
        }
    }
}

@Composable
fun MaterialCard(
    material: MaterialEntity,
    onClick: () -> Unit,
    onOptions: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("material_card_${material.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(getFileColor(material.mimeType).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getFileIcon(material.mimeType),
                        contentDescription = null,
                        tint = getFileColor(material.mimeType),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open with...") },
                            onClick = { menuExpanded = false; onClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { menuExpanded = false; onOptions("rename") }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Collection") },
                            onClick = { menuExpanded = false; onOptions("move") }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete File", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onOptions("delete") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = material.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatFileSize(material.fileSizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MaterialListRow(
    material: MaterialEntity,
    onClick: () -> Unit,
    onOptions: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(getFileColor(material.mimeType).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFileIcon(material.mimeType),
                    contentDescription = null,
                    tint = getFileColor(material.mimeType),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${getFileTypeLabel(material.mimeType)} • ${formatFileSize(material.fileSizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open with...") },
                        onClick = { menuExpanded = false; onClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { menuExpanded = false; onOptions("rename") }
                    )
                    DropdownMenuItem(
                        text = { Text("Move") },
                        onClick = { menuExpanded = false; onOptions("move") }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onOptions("delete") }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultsView(
    collections: List<CollectionEntity>,
    materials: List<MaterialEntity>,
    onCollectionClick: (CollectionEntity) -> Unit,
    onMaterialClick: (MaterialEntity) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (collections.isEmpty() && materials.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No matching collections or materials found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (collections.isNotEmpty()) {
            item {
                Text(
                    "MATCHING COLLECTIONS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(collections) { col ->
                CollectionListRow(
                    collection = col,
                    onClick = { onCollectionClick(col) },
                    onOptions = {}
                )
            }
        }

        if (materials.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "MATCHING MATERIALS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(materials) { mat ->
                MaterialListRow(
                    material = mat,
                    onClick = { onMaterialClick(mat) },
                    onOptions = {}
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryView(
    isRoot: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRoot) Icons.Default.Folder else Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isRoot) "Your Study Library is Empty" else "This Collection is Empty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isRoot) {
                        "Create collections to organize lecture slides, notes, readings, and videos."
                    } else {
                        "Import documents, PDFs, photos, or video lessons to study anytime offline."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun getFileIcon(mimeType: String): ImageVector {
    return when {
        mimeType.contains("pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
        mimeType.startsWith("image/", ignoreCase = true) -> Icons.Default.Image
        mimeType.startsWith("video/", ignoreCase = true) -> Icons.Default.VideoLibrary
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

fun getFileColor(mimeType: String): Color {
    return when {
        mimeType.contains("pdf", ignoreCase = true) -> Color(0xFFD9534F)
        mimeType.startsWith("image/", ignoreCase = true) -> Color(0xFF4A7C59)
        mimeType.startsWith("video/", ignoreCase = true) -> Color(0xFFD99A42)
        else -> Color(0xFF176B63)
    }
}

fun getFileTypeLabel(mimeType: String): String {
    return when {
        mimeType.contains("pdf", ignoreCase = true) -> "PDF Document"
        mimeType.startsWith("image/", ignoreCase = true) -> "Image"
        mimeType.startsWith("video/", ignoreCase = true) -> "Video Lesson"
        else -> "Document"
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val df = DecimalFormat("#.#")
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${df.format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${df.format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}
