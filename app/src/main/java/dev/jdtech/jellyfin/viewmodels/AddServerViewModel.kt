package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.exception.ApiClientException
import java.lang.Exception

class AddServerViewModel(val application: Application) : ViewModel() {
    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean>
        get() = _navigateToLogin

    fun checkServer(baseUrl: String) {
        viewModelScope.launch {
            val jellyfinApi = JellyfinApi.newInstance(application, baseUrl)
            try {
                jellyfinApi.systemApi.getPublicSystemInfo()
                _navigateToLogin.value = true
            } catch (e: Exception) {
                Log.e("JellyfinApi", "${e.message}")
                _navigateToLogin.value = false
            }
        }
    }

    fun onNavigateToLoginDone() {
        _navigateToLogin.value = false
    }
}