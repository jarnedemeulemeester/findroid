package dev.jdtech.jellyfin.settings.presentation.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsFileEditViewModel
@Inject
constructor(
    private val application: Application,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsFileEditState())
    val state = _state.asStateFlow()

    private var filePath = ""

    fun loadFile(filePath: String) {
        this.filePath = filePath

        val file = File(application.filesDir, filePath)
        val text = if (file.exists()) {
            file.readText()
        } else ""
        _state.update { it.copy(initialText = text) }
    }

    fun onAction(action: SettingsFileEditAction) {
        when (action) {
            is SettingsFileEditAction.OnSave -> {
                val file = File(application.filesDir, filePath)
                file.writeText(action.text)
            }
            else -> Unit
        }
    }
}