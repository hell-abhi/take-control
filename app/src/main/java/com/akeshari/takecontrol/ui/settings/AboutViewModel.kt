package com.akeshari.takecontrol.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.PrivacyScore
import com.akeshari.takecontrol.data.model.PrivacyScoreCalculator
import com.akeshari.takecontrol.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _score = MutableStateFlow<PrivacyScore?>(null)
    val score: StateFlow<PrivacyScore?> = _score.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = repository.getInstalledApps().filter { !it.isSystemApp }
            _score.value = PrivacyScoreCalculator.calculate(apps)
        }
    }
}
