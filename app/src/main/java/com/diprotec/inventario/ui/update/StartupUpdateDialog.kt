package com.diprotec.inventario.ui.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.ui.common.AppFloatingMessage
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "STARTUP_UPDATE"

@Composable
fun StartupUpdateDialog(
    onOptionalDismissed: () -> Unit = {},
    onUpdateStarted: () -> Unit = {},
    vm: StartupUpdateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by vm.state.collectAsState()

    var currentDownloadId by rememberSaveable { mutableLongStateOf(-1L) }
    var isDownloading by rememberSaveable { mutableStateOf(false) }
    var installerOpened by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(context, currentDownloadId, installerOpened, s.apkFileName) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

                val completedId = intent.getLongExtra(
                    DownloadManager.EXTRA_DOWNLOAD_ID,
                    -1L
                )

                if (completedId <= 0L) return
                if (currentDownloadId <= 0L) return
                if (completedId != currentDownloadId) return
                if (installerOpened) return

                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val info = getDownloadInfo(dm, completedId)

                if (info == null) {
                    AppFloatingMessage.error(
                        "No se pudo leer el estado de la descarga"
                    )

                    scope.launch { vm.resetPendingUpdate() }
                    isDownloading = false
                    currentDownloadId = -1L
                    return
                }

                Log.d(TAG, "downloadId=${info.id}")
                Log.d(TAG, "status=${info.status}")
                Log.d(TAG, "reason=${info.reason}")
                Log.d(TAG, "reasonText=${info.reasonText}")
                Log.d(TAG, "localUri=${info.localUri}")
                Log.d(TAG, "uri=${info.uri}")

                if (info.status == DownloadManager.STATUS_FAILED) {
                    AppFloatingMessage.error(
                        "Descarga falló: ${info.reasonText}"
                    )

                    scope.launch { vm.resetPendingUpdate() }
                    isDownloading = false
                    currentDownloadId = -1L
                    return
                }

                if (info.status != DownloadManager.STATUS_SUCCESSFUL) return

                deleteOldApksExcept(s.apkFileName)

                scope.launch {
                    vm.markApkDownloaded(completedId)
                }

                val uri = dm.getUriForDownloadedFile(completedId)

                if (uri == null) {
                    AppFloatingMessage.error(
                        "La descarga terminó, pero no se pudo abrir el archivo"
                    )

                    isDownloading = false
                    currentDownloadId = -1L
                    return
                }

                openInstaller(
                    context = ctx,
                    uri = uri,
                    onInstallerOpened = {
                        installerOpened = true
                    },
                    onFinished = {
                        isDownloading = false
                        currentDownloadId = -1L
                    }
                )
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    LaunchedEffect(currentDownloadId, installerOpened, s.apkFileName) {
        if (currentDownloadId <= 0L) return@LaunchedEffect
        if (installerOpened) return@LaunchedEffect

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        while (currentDownloadId > 0L && !installerOpened) {
            delay(1000)

            val info = getDownloadInfo(dm, currentDownloadId) ?: continue

            if (info.status == DownloadManager.STATUS_FAILED) {
                Log.e(TAG, "Descarga fallida: ${info.reasonText}")
                isDownloading = false
                currentDownloadId = -1L
                break
            }

            if (info.status == DownloadManager.STATUS_SUCCESSFUL) {
                deleteOldApksExcept(s.apkFileName)

                val finishedId = currentDownloadId
                val uri = dm.getUriForDownloadedFile(finishedId)

                if (uri == null) {
                    Log.e(TAG, "Descarga completada pero uri nula")
                    isDownloading = false
                    currentDownloadId = -1L
                    break
                }

                vm.markApkDownloaded(finishedId)

                openInstaller(
                    context = context,
                    uri = uri,
                    onInstallerOpened = {
                        installerOpened = true
                    },
                    onFinished = {
                        isDownloading = false
                        currentDownloadId = -1L
                    }
                )

                break
            }
        }
    }

    if (!s.showDialog) return

    AlertDialog(
        onDismissRequest = {
            if (!s.mandatory && !isDownloading) {
                vm.dismissOptionalUpdate()
                onOptionalDismissed()
            }
        },
        title = {
            Text(
                if (s.mandatory) {
                    "Actualización obligatoria"
                } else {
                    "Nueva versión disponible"
                }
            )
        },
        text = {
            Text(
                buildString {
                    append("Se ha detectado una nueva versión disponible.")

                    if (isDownloading) {
                        append(" Descargando actualización...")
                    } else if (s.mandatory) {
                        append(" Debe actualizar para continuar.")
                    } else {
                        append(" Puede actualizar ahora o hacerlo más tarde.")
                    }

                    if (s.apkFileName.isNotBlank()) {
                        append("\n\nArchivo: ")
                        append(s.apkFileName)
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isDownloading) return@TextButton

                    val url = s.apkUrl
                    val fileName = s.apkFileName

                    if (url.isBlank()) {
                        AppFloatingMessage.error(
                            "No hay URL de actualización disponible"
                        )
                        return@TextButton
                    }

                    if (fileName.isBlank()) {
                        AppFloatingMessage.error(
                            "No hay nombre de archivo para la actualización"
                        )
                        return@TextButton
                    }

                    installerOpened = false

                    val existingFile = getDownloadTargetFile(fileName)
                    val downloadManager =
                        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                    if (s.apkDownloaded && s.downloadId > 0L && existingFile.exists()) {
                        val existingUri = downloadManager.getUriForDownloadedFile(s.downloadId)

                        if (existingUri != null) {
                            deleteOldApksExcept(fileName)

                            openInstaller(
                                context = context,
                                uri = existingUri,
                                onInstallerOpened = {
                                    installerOpened = true
                                },
                                onFinished = {
                                    isDownloading = false
                                    currentDownloadId = -1L
                                }
                            )

                            onUpdateStarted()
                            return@TextButton
                        }
                    }

                    deleteOldApksExcept(fileName)

                    if (existingFile.exists()) {
                        runCatching { existingFile.delete() }
                            .onFailure {
                                Log.e(TAG, "No se pudo eliminar APK anterior", it)
                            }
                    }

                    currentDownloadId = downloadApkToDownloads(
                        context = context,
                        url = url,
                        fileName = fileName
                    )

                    isDownloading = true

                    vm.saveDownloadState(
                        mandatory = s.mandatory,
                        downloadId = currentDownloadId,
                        apkFileName = fileName,
                        apkUrl = url,
                        apkDownloaded = false
                    )

                    onUpdateStarted()

                    AppFloatingMessage.info("Descarga iniciada")

                    Log.d(TAG, "url=$url")
                    Log.d(TAG, "fileName=$fileName")
                    Log.d(TAG, "downloadId=$currentDownloadId")
                }
            ) {
                Text(if (isDownloading) "Descargando..." else "Actualizar")
            }
        },
        dismissButton = {
            if (!s.mandatory && !isDownloading) {
                TextButton(
                    onClick = {
                        vm.dismissOptionalUpdate()
                        onOptionalDismissed()
                    }
                ) {
                    Text("Más tarde")
                }
            }
        }
    )
}

private fun openInstaller(
    context: Context,
    uri: Uri,
    onInstallerOpened: () -> Unit,
    onFinished: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        AppFloatingMessage.error(
            "Permite instalar apps desconocidas para continuar"
        )

        val settingsIntent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(settingsIntent)
        onFinished()
        return
    }

    try {
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }

        onInstallerOpened()
        Log.d(TAG, "Abriendo instalador. uri=$uri")
        context.startActivity(installIntent)
    } catch (t: Throwable) {
        Log.e(TAG, "No se pudo abrir el instalador", t)

        AppFloatingMessage.error(
            "No se pudo abrir el instalador: ${t.message}"
        )
    } finally {
        onFinished()
    }
}

private fun downloadApkToDownloads(
    context: Context,
    url: String,
    fileName: String
): Long {
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("Descargando actualización")
        .setDescription(fileName)
        .setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)
        .setMimeType("application/vnd.android.package-archive")
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            fileName
        )

    val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    return downloadManager.enqueue(request)
}

private fun deleteOldApksExcept(
    fileNameToKeep: String
) {
    val downloadDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    val keepName = fileNameToKeep.trim()

    downloadDir
        .listFiles()
        .orEmpty()
        .filter { file ->
            file.isFile &&
                    file.name.endsWith(".apk", ignoreCase = true) &&
                    file.name != keepName
        }
        .forEach { file ->
            runCatching {
                file.delete()
            }.onFailure {
                Log.e(TAG, "No se pudo eliminar APK antiguo: ${file.name}", it)
            }
        }
}

private fun getDownloadTargetFile(
    fileName: String
): File {
    val downloadDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    return File(downloadDir, fileName)
}

private data class DownloadInfo(
    val id: Long,
    val status: Int,
    val reason: Int,
    val reasonText: String,
    val localUri: String?,
    val uri: String?
)

private fun getDownloadInfo(
    downloadManager: DownloadManager,
    downloadId: Long
): DownloadInfo? {
    val query = DownloadManager.Query().setFilterById(downloadId)
    val cursor: Cursor = downloadManager.query(query) ?: return null

    cursor.use {
        if (!it.moveToFirst()) return null

        val id = it.getLong(
            it.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
        )

        val status = it.getInt(
            it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
        )

        val reason = it.getInt(
            it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
        )

        val localUri = runCatching {
            it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
        }.getOrNull()

        val uri = runCatching {
            it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_URI))
        }.getOrNull()

        return DownloadInfo(
            id = id,
            status = status,
            reason = reason,
            reasonText = mapDownloadReason(status, reason),
            localUri = localUri,
            uri = uri
        )
    }
}

private fun mapDownloadReason(
    status: Int,
    reason: Int
): String {
    return when (status) {
        DownloadManager.STATUS_FAILED -> when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "no se puede reanudar"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "almacenamiento no encontrado"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "el archivo ya existe"
            DownloadManager.ERROR_FILE_ERROR -> "error de archivo"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "error de datos HTTP"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "espacio insuficiente"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "demasiadas redirecciones"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "código HTTP no manejado"
            DownloadManager.ERROR_UNKNOWN -> "error desconocido"
            else -> "motivo=$reason"
        }

        DownloadManager.STATUS_PAUSED -> when (reason) {
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "en cola por Wi-Fi"
            DownloadManager.PAUSED_UNKNOWN -> "pausada"
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "esperando red"
            DownloadManager.PAUSED_WAITING_TO_RETRY -> "esperando reintento"
            else -> "pausada, motivo=$reason"
        }

        DownloadManager.STATUS_PENDING -> "pendiente"
        DownloadManager.STATUS_RUNNING -> "descargando"
        DownloadManager.STATUS_SUCCESSFUL -> "completada"
        else -> "estado=$status motivo=$reason"
    }
}