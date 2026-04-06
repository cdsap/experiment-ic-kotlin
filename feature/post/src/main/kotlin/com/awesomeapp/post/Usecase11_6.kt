package com.awesomeapp.post

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase11_6 @Inject constructor(
    private val repository: Repository11_5
) {
    operator fun invoke(): Flow<List<Model11_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}