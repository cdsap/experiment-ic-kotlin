package com.awesomeapp.todo

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase36_6 @Inject constructor(
    private val repository: Repository36_5
) {
    operator fun invoke(): Flow<List<Model36_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}