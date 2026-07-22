package com.hacybeyker.scaffoldingandroidcompose.feature.home.data

import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.Greeting
import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.GreetingRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Sample data source kept in memory so the scaffolding has zero storage dependencies.
 * Replace it with Room/DataStore/network sources in a real feature.
 */
@Singleton
class InMemoryGreetingRepository @Inject constructor() : GreetingRepository {

    private val name = MutableStateFlow(DEFAULT_NAME)

    override fun observeGreeting(): Flow<Greeting> = name.map { Greeting(name = it) }

    override suspend fun updateName(name: String) {
        this.name.value = name
    }

    private companion object {
        const val DEFAULT_NAME = "Android"
    }
}
