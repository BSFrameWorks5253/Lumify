package com.lumify.editor.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import com.lumify.editor.core.EditState
import com.lumify.editor.ui.screens.ExportFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class BatchQueueItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val bitmap: Bitmap,
    val editState: EditState,
    val format: ExportFormat = ExportFormat.JPEG,
    val quality: Int = 95,
    val preserveExif: Boolean = true
)

object BatchQueueManager {

    private val _queue = MutableStateFlow<List<BatchQueueItem>>(emptyList())
    val queue: StateFlow<List<BatchQueueItem>> = _queue.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _progress = MutableStateFlow(0f) // 0.0 to 1.0
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    fun addItem(item: BatchQueueItem) {
        _queue.value = _queue.value + item
    }

    fun addItems(items: List<BatchQueueItem>) {
        _queue.value = _queue.value + items
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _progress.value = 0f
        _statusText.value = ""
    }

    fun startBatchExport(context: Context) {
        if (_queue.value.isEmpty()) return
        _isExporting.value = true
        val intent = Intent(context, BatchExportService::class.java).apply {
            action = BatchExportService.ACTION_START_EXPORT
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun cancelBatchExport(context: Context) {
        val intent = Intent(context, BatchExportService::class.java).apply {
            action = BatchExportService.ACTION_CANCEL_EXPORT
        }
        context.startService(intent)
        _isExporting.value = false
    }

    internal fun updateProgress(current: Int, total: Int, currentTitle: String) {
        _progress.value = current.toFloat() / total.coerceAtLeast(1)
        _statusText.value = "Exporting $current of $total: $currentTitle"
    }

    internal fun finishExport(successCount: Int) {
        _isExporting.value = false
        _queue.value = emptyList()
        _progress.value = 1f
        _statusText.value = "Batch export finished: $successCount photos saved"
    }
}
