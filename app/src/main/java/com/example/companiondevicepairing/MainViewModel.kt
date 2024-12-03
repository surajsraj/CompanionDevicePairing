package com.example.companiondevicepairing

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel: ViewModel() {

    private val _requestType = MutableStateFlow<String>("")
    val requestType: StateFlow<String> = _requestType.asStateFlow()

    fun setRequestType(request: String) {
        _requestType.value = request
    }
}