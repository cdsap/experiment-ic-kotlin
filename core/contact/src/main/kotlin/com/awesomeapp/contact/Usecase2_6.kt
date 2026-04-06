package com.awesomeapp.contact

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase2_6 @Inject constructor(
    private val repository: Repository2_5
) {
    operator fun invoke(): Flow<List<Model2_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}