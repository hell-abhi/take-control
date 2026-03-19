package com.akeshari.takecontrol.ui.appdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.database.entity.PermissionChangeEntity
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.repository.AppRepository
import com.akeshari.takecontrol.data.model.PrivacyAlternative
import com.akeshari.takecontrol.data.model.PrivacyAlternativesData
import com.akeshari.takecontrol.util.PrivacyNarrativeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppDetailState(
    val app: AppPermissionInfo? = null,
    val isLoading: Boolean = true,
    val narratives: List<String> = emptyList(),
    val alternatives: List<PrivacyAlternative> = emptyList(),
    val recentChanges: List<PermissionChangeEntity> = emptyList()
)

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AppDetailState())
    val state: StateFlow<AppDetailState> = _state.asStateFlow()

    fun loadApp(packageName: String) {
        viewModelScope.launch {
            _state.value = AppDetailState(isLoading = true)
            val app = repository.getAppByPackage(packageName)
            val narratives = if (app != null) PrivacyNarrativeGenerator.generate(app.permissions) else emptyList()
            val alternatives = PrivacyAlternativesData.getAlternativesForPackage(packageName)
            val changes = repository.getChangesForApp(packageName)
            _state.value = AppDetailState(
                app = app,
                isLoading = false,
                narratives = narratives,
                alternatives = alternatives,
                recentChanges = changes
            )
        }
    }
}
