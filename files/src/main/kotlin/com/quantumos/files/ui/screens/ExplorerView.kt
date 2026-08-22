package com.quantumos.files.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Glyph
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.QuantumIcon
import com.quantumos.files.viewmodel.FileExplorerViewModel
import com.quantumos.files.viewmodel.FileItem
import com.quantumos.appshell.GlyphLabel

/*
 * ExplorerView -- ported from the standalone app's ExplorerView (same domain logic, restyled onto
 * :app-shell's Phosphor tokens/Chakra Petch font instead of hardcoded hex + FontFamily.Monospace).
 * The generic/unrestricted file browser behavior (raw path breadcrumb, NEW SECTOR/ALLOCATE FILE
 * actions that can create arbitrary folders/files anywhere) is kept exactly as-is (fix-pass §5).
 * Row icons now come from the shared QuantumIcons house set (Core Apps Polish Pass §2) -- the four
 * seeded top-level categories (FIELD-LOGS/CAPTURES/COMMS-CACHE/MAPS) get their own glyph, any other
 * directory falls back to the generic Folder glyph.
 */
private fun categoryGlyph(dirName: String): Glyph = when (dirName) {
    "FIELD-LOGS" -> Glyph.CategoryFieldLogs
    "CAPTURES" -> Glyph.CategoryCaptures
    "COMMS-CACHE" -> Glyph.CategoryCommsCache
    "MAPS" -> Glyph.CategoryMaps
    else -> Glyph.Folder
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorerView(
    viewModel: FileExplorerViewModel,
    primaryColor: Color,
    dimColor: Color,
    onDecryptRequested: () -> Unit,
    onNewDirClick: () -> Unit,
    onNewFileClick: () -> Unit,
    onDeleteRequested: (FileItem) -> Unit
) {
    val files by viewModel.files.collectAsState()
    val path by viewModel.currentPath.collectAsState()

    val currentSelected = viewModel.selectedFile

    if (currentSelected != null) {
        // Detailed document viewer/editor mode.
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SECTOR: ${currentSelected.name}",
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 14.sp,
                    color = primaryColor
                )

                Row {
                    if (!viewModel.isEditingFile) {
                        Button(
                            onClick = { viewModel.isEditingFile = true },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, primaryColor)
                        ) {
                            Text("EDIT", color = primaryColor, fontSize = 11.sp, fontFamily = Fonts.ChakraPetch)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.triggerDeepDecrypt(currentSelected, viewModel.fileContentText)
                                onDecryptRequested()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, primaryColor)
                        ) {
                            Text("DECRYPT AI", color = primaryColor, fontSize = 11.sp, fontFamily = Fonts.ChakraPetch)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.saveFileEdits(currentSelected, viewModel.fileContentText)
                                viewModel.isEditingFile = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            border = BorderStroke(1.dp, primaryColor)
                        ) {
                            Text("SAVE", color = Phosphor.Crt, fontSize = 11.sp, fontFamily = Fonts.ChakraPetch)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.openFile(currentSelected) // Reload.
                                viewModel.isEditingFile = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, dimColor)
                        ) {
                            Text("ABORT", color = dimColor, fontSize = 11.sp, fontFamily = Fonts.ChakraPetch)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = Phosphor.Crt,
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
            ) {
                if (viewModel.isEditingFile) {
                    OutlinedTextField(
                        value = viewModel.fileContentText,
                        onValueChange = { viewModel.fileContentText = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = Fonts.ChakraPetch,
                            fontSize = 13.sp,
                            color = primaryColor
                        ),
                        modifier = Modifier.fillMaxSize(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = primaryColor,
                            focusedTextColor = primaryColor,
                            unfocusedTextColor = primaryColor
                        )
                    )
                } else {
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        item {
                            Text(
                                text = viewModel.fileContentText,
                                fontFamily = Fonts.ChakraPetch,
                                fontSize = 13.sp,
                                color = primaryColor,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Directory explorer file-list view.
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SECTOR: /QUANTUM$path",
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 13.sp,
                    color = primaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (path.isNotEmpty()) {
                    TextButton(onClick = { viewModel.navigateUp() }) {
                        GlyphLabel(
                            Glyph.Back, "UP", primaryColor, Fonts.ChakraPetch,
                            fontSize = 11.sp, iconSize = 11.dp, trailing = true,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (files.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "EMPTY SECTOR GRID",
                            color = dimColor,
                            fontFamily = Fonts.ChakraPetch,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(files) { file ->
                            FileGridCard(
                                file = file,
                                primaryColor = primaryColor,
                                dimColor = dimColor,
                                onClick = {
                                    if (file.isDirectory) {
                                        viewModel.navigateToDir(file.path)
                                    } else {
                                        viewModel.openFile(file)
                                    }
                                },
                                onDelete = { onDeleteRequested(file) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action rail -- NEW SECTOR / ALLOCATE FILE, unrestricted (fix-pass §5).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNewDirClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, primaryColor)
                ) {
                    Text("NEW SECTOR", color = primaryColor, fontFamily = Fonts.ChakraPetch, fontSize = 12.sp)
                }

                Button(
                    onClick = onNewFileClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, primaryColor)
                ) {
                    Text("ALLOCATE FILE", color = primaryColor, fontFamily = Fonts.ChakraPetch, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridCard(
    file: FileItem,
    primaryColor: Color,
    dimColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete
            ),
        color = Phosphor.Crt,
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                QuantumIcon(
                    glyph = if (file.isDirectory) categoryGlyph(file.name) else Glyph.FileDoc,
                    tint = primaryColor,
                    size = 24.dp
                )

                Text(
                    text = file.permissions,
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 9.sp,
                    color = dimColor
                )
            }

            Column {
                Text(
                    text = file.name,
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 11.sp,
                    color = primaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val desc = if (file.isDirectory) "SUB-SECTOR" else "${file.size} Bytes"
                Text(
                    text = desc,
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 9.sp,
                    color = dimColor
                )
            }
        }
    }
}
