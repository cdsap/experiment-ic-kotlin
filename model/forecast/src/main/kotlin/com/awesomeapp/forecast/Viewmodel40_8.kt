package com.awesomeapp.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class Viewmodel40_8 @Inject constructor(
    private val useCase: Usecase40_6
) : ViewModel() {
    private val _state = MutableStateFlow(State40_7())
    val state: StateFlow<State40_7> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            useCase.seedIfEmpty()
            useCase().collectLatest { items: List<Model40_2> ->
                _state.emit(_state.value.copy(items = items, isLoading = false))
            }
        }
    }
}