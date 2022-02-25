package com.example.cameraxsample.camera2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainApplication
import kotlinx.coroutines.launch

class MainViewModel(val app: Application) : AndroidViewModel(app) {
    private var repository: VCameraRepository? = null

    init {
        val application = getApplication() as MainApplication
        repository = application.getRespository()
    }

    fun deleteFiles() {
        viewModelScope.launch {
            repository?.deleteAllImages(app)
        }
    }

}