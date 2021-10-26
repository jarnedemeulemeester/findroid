package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.DeviceOptions
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject internal constructor(
    private val api: JellyfinApi
) : ViewModel() {

    fun updateDeviceName(name: String) {
        api.jellyfin.deviceInfo?.id?.let { id ->
            viewModelScope.launch(IO) {
                api.deviceApi.updateDeviceOptions(id, DeviceOptions(name))
            }
        }
    }
}