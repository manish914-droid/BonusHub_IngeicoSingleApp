/*
package com.bonushub.india.crdb.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainCoroutineScope

@InstallIn(ApplicationComponent::class)
@Module
object CoroutinesScopesModule {

    @Singleton
    @MainCoroutineScope
    @Provides
    fun providesDefaultCoroutineScope(@DefaultDispatcher defaultDispatcher: CoroutineDispatcher): CoroutineScope
    = CoroutineScope(SupervisorJob() + defaultDispatcher)

    @Singleton
    @MainCoroutineScope
    @Provides
    fun providesIOCoroutineScope(@IoDispatcher ioDispatcher: CoroutineDispatcher): CoroutineScope
            = CoroutineScope(SupervisorJob() + ioDispatcher)

    @Singleton
    @MainCoroutineScope
    @Provides
    fun providesMainCoroutineScope(@MainDispatcher mainDispatcher: CoroutineDispatcher): CoroutineScope
            = CoroutineScope(SupervisorJob() + mainDispatcher)
}

*/
