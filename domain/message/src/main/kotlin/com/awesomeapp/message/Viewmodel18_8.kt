package com.awesomeapp.message

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
class Viewmodel18_8 @Inject constructor(
    private val useCase: Usecase18_6
) : ViewModel() {
    private val _state = MutableStateFlow(State18_7())
    val state: StateFlow<State18_7> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            useCase.seedIfEmpty()
            useCase().collectLatest { items: List<Model18_2> ->
                _state.emit(_state.value.copy(items = items, isLoading = false))
            }
        }
    }
}