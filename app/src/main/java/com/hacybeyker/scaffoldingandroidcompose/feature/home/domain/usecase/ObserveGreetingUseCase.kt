package com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.usecase

import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.Greeting
import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.GreetingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveGreetingUseCase @Inject constructor(private val repository: GreetingRepository) {
    operator fun invoke(): Flow<Greeting> = repository.observeGreeting()
}
