package com.awesomeapp.cart

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase6_6 @Inject constructor(
    private val repository: Repository6_5
) {
    operator fun invoke(): Flow<List<Model6_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}