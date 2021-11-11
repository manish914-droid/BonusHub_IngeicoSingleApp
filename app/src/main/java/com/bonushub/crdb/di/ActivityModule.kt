package com.bonushub.crdb.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.repository.*
import com.bonushub.crdb.utils.DeviceHelper
import com.usdk.apiservice.aidl.algorithm.UAlgorithm
import com.usdk.apiservice.aidl.emv.UEMV
import com.usdk.apiservice.aidl.pinpad.UPinpad
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ActivityContext

import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    @Provides
    @ActivityScoped
    fun provideShoppingrepository(@USDKScope algo: UAlgorithm?,
                                  @USDKScope ipinpad: UPinpad?,@USDKScope emv: UEMV?,
                                  @ActivityContext context: Context) : SearchCardRepository{

        return SearchCardDefaultRepository(algo,ipinpad,emv,context)
    }

}




//https://codelabs.developers.google.com/codelabs/android-hilt/#6
//https://github.com/google-developer-training/android-kotlin-fundamentals-apps/blob/master/RecyclerViewFundamentals/app/src/main/java/com/example/android/trackmysleepquality/sleeptracker/SleepTrackerFragment.kt