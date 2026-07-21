package com.quantumos.files.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Glyph
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.QuantumIcon
import com.quantumos.files.viewmodel.FileExplorerViewModel
import com.quantumos.files.viewmodel.FileItem

/*
 * FilesScreen -- the FILES module's own top-level content, hosted inside AppShell's content slot.
 * Owns FILES's private in-app navigation model (EXPLORER/TERMINAL/DECRYPT/QUARK) -- a docked
 * module is free to have its own internal screens/tabs; this is NOT the launcher's HOME/APPS/
 * STATUS/LOG channel strip, which is launcher-only chrome owned by :app-shell.
 *
 * All private chrome from the standalone app (opaque nameplate header, corner registration marks,
 * bottom tab Surface with elevation, the whole-screen CRT flicker rememberInfiniteTransition, the
 * PIN-lock security overlay, the FloatingQuarkWidget, and the roll-down VitalityPanel) is deleted
 * outright -- replaced by :app-shell's shared chrome (Core Apps Fix-Pass, Decision 86).
 */
enum class FileChannel { EXPLORER, TERMINAL, DECRYPTOR, QUARK_CHAT }

@Composable
fun FilesScreen(
    viewModel: FileExplorerViewModel,
    themeColor: Color,
    dimColor: Color,
    contentPadding: PaddingValues
) {
    var activeChannel by remember { mutableStateOf(FileChannel.EXPLORER) }

    var showNewDirDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var pendingDeleteFile by remember { mutableStateOf<FileItem?>(null) }

    var inputDirName by remember { mutableStateOf("") }
    var inputFileName by remember { mutableStateOf("") }
    var inputFileContent by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // Internal channel strip -- FILES's own tabs, terse phosphor microcopy, no Material bottom
        // navigation bar/elevation.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ChannelTabButton(
                label = "EXPLORER",
                glyph = Glyph.Folder,
                isActive = activeChannel == FileChannel.EXPLORER,
                color = themeColor,
                onClick = { activeChannel = FileChannel.EXPLORER }
            )
            ChannelTabButton(
                label = "TERMINAL",
                glyph = Glyph.Terminal,
                isActive = activeChannel == FileChannel.TERMINAL,
                color = themeColor,
                onClick = { activeChannel = FileChannel.TERMINAL }
            )
            ChannelTabButton(
                label = "DECRYPT",
                glyph = Glyph.Decrypt,
                isActive = activeChannel == FileChannel.DECRYPTOR,
                color = themeColor,
                onClick = { activeChannel = FileChannel.DECRYPTOR }
            )
            ChannelTabButton(
                label = "QUARK",
                glyph = Glyph.QuarkChat,
                isActive = activeChannel == FileChannel.QUARK_CHAT,
                color = themeColor,
                onClick = { activeChannel = FileChannel.QUARK_CHAT }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (activeChannel) {
                FileChannel.EXPLORER -> ExplorerView(
                    viewModel = viewModel,
                    primaryColor = themeColor,
                    dimColor = dimColor,
                    onDecryptRequested = { activeChannel = FileChannel.DECRYPTOR },
                    onNewDirClick = { showNewDirDialog = true },
                    onNewFileClick = { showNewFileDialog = true },
                    onDeleteRequested = { file ->
                        pendingDeleteFile = file
                        showDeleteConfirmDialog = true
                    }
                )
                FileChannel.TERMINAL -> TerminalView(
                    viewModel = viewModel,
                    primaryColor = themeColor,
                    dimColor = dimColor
                )
                FileChannel.DECRYPTOR -> DecryptorView(
                    viewModel = viewModel,
                    primaryColor = themeColor,
                    dimColor = dimColor
                )
                FileChannel.QUARK_CHAT -> QuarkChatView(
                    viewModel = viewModel,
                    primaryColor = themeColor,
                    dimColor = dimColor
                )
            }
        }
    }

    if (showNewDirDialog) {
        NewDirDialog(
            themeColor = themeColor,
            dimColor = dimColor,
            name = inputDirName,
            onNameChange = { inputDirName = it },
            onConfirm = {
                if (inputDirName.isNotBlank()) {
                    viewModel.createDirectory(inputDirName.trim())
                    inputDirName = ""
                    showNewDirDialog = false
                }
            },
            onDismiss = { showNewDirDialog = false }
        )
    }

    if (showNewFileDialog) {
        NewFileDialog(
            themeColor = themeColor,
            dimColor = dimColor,
            name = inputFileName,
            content = inputFileContent,
            onNameChange = { inputFileName = it },
            onContentChange = { inputFileContent = it },
            onConfirm = {
                if (inputFileName.isNotBlank()) {
                    viewModel.createNewFile(inputFileName.trim(), inputFileContent)
                    inputFileName = ""
                    inputFileContent = ""
                    showNewFileDialog = false
                }
            },
            onDismiss = { showNewFileDialog = false }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            dimColor = dimColor,
            fileName = pendingDeleteFile?.name.orEmpty(),
            onConfirm = {
                pendingDeleteFile?.let { viewModel.deleteFileItem(it) }
                showDeleteConfirmDialog = false
                pendingDeleteFile = null
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

@Composable
fun ChannelTabButton(
    label: String,
    glyph: Glyph,
    isActive: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val alpha = if (isActive) 1f else 0.5f
    val background = if (isActive) color.copy(alpha = 0.15f) else Color.Transparent

    Surface(
        onClick = onClick,
        color = background,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            QuantumIcon(glyph, tint = color.copy(alpha = alpha), size = 20.dp)
            Text(
                text = label,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 9.sp,
                color = color.copy(alpha = alpha)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewDirDialog(
    themeColor: Color,
    dimColor: Color,
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ALLOCATE SUB-SECTOR", color = themeColor, fontFamily = Fonts.ChakraPetch) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Directory Name", color = dimColor) },
                textStyle = TextStyle(color = themeColor, fontFamily = Fonts.ChakraPetch),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = dimColor,
                    cursorColor = themeColor,
                    focusedTextColor = themeColor,
                    unfocusedTextColor = themeColor
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("ALLOCATE", color = themeColor, fontFamily = Fonts.ChakraPetch)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ABORT", color = dimColor, fontFamily = Fonts.ChakraPetch)
            }
        },
        containerColor = Phosphor.Crt,
        modifier = Modifier.border(1.dp, themeColor)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewFileDialog(
    themeColor: Color,
    dimColor: Color,
    name: String,
    content: String,
    onNameChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ALLOCATE LOG ARCHIVE", color = themeColor, fontFamily = Fonts.ChakraPetch) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("File Name (e.g. log.txt)", color = dimColor) },
                    textStyle = TextStyle(color = themeColor, fontFamily = Fonts.ChakraPetch),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = dimColor,
                        cursorColor = themeColor,
                        focusedTextColor = themeColor,
                        unfocusedTextColor = themeColor
                    )
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text("File Content Data", color = dimColor) },
                    textStyle = TextStyle(color = themeColor, fontFamily = Fonts.ChakraPetch),
                    modifier = Modifier.height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = dimColor,
                        cursorColor = themeColor,
                        focusedTextColor = themeColor,
                        unfocusedTextColor = themeColor
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("ALLOCATE", color = themeColor, fontFamily = Fonts.ChakraPetch)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ABORT", color = dimColor, fontFamily = Fonts.ChakraPetch)
            }
        },
        containerColor = Phosphor.Crt,
        modifier = Modifier.border(1.dp, themeColor)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteConfirmDialog(
    dimColor: Color,
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PURGE SECTOR CONFIRMATION", color = Phosphor.Warn, fontFamily = Fonts.ChakraPetch) },
        text = {
            Text(
                text = "Are you sure you want to permanently erase '$fileName' from sector memory?",
                color = Phosphor.Warn,
                fontFamily = Fonts.ChakraPetch
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("PURGE", color = Phosphor.Warn, fontFamily = Fonts.ChakraPetch)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ABORT", color = dimColor, fontFamily = Fonts.ChakraPetch)
            }
        },
        containerColor = Phosphor.Crt,
        modifier = Modifier.border(1.dp, Phosphor.Warn)
    )
}
