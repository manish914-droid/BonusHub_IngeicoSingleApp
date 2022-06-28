package com.bonushub.crdb.india.di

import android.content.Context
import com.bonushub.crdb.india.repository.SearchCardDefaultRepository
import com.bonushub.crdb.india.repository.SearchCardRepository
import com.usdk.apiservice.aidl.algorithm.UAlgorithm
import com.usdk.apiservice.aidl.emv.UEMV
import com.usdk.apiservice.aidl.pinpad.UPinpad
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    @Provides
    @ActivityScoped
    fun provideShoppingrepository(@USDKScope algo: UAlgorithm?,
                                  @USDKScope ipinpad: UPinpad?, @USDKScope emv: UEMV?,
                                  @ActivityContext context: Context) : SearchCardRepository{

        return SearchCardDefaultRepository(algo,ipinpad,emv,context)
    }

}




//https://codelabs.developers.google.com/codelabs/android-hilt/#6
//https://github.com/google-developer-training/android-kotlin-fundamentals-apps/blob/master/RecyclerViewFundamentals/app/src/main/java/com/example/android/trackmysleepquality/sleeptracker/SleepTrackerFragment.kt