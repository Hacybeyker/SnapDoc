package com.hacybeyker.scaffoldingandroidcompose.feature.home.domain

import kotlinx.coroutines.flow.Flow

interface GreetingRepository {
    fun observeGreeting(): Flow<Greeting>

    suspend fun updateName(name: String)
}
