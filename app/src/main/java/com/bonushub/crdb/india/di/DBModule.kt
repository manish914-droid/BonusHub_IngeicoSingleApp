package com.bonushub.crdb.india.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.db.AppDatabase
import com.bonushub.crdb.india.repository.IKeyExchange
import com.bonushub.crdb.india.repository.RoomDBRepository
import com.bonushub.crdb.india.repository.keyexchangeDataSource
import com.bonushub.crdb.india.repository.keyexchangeDataSourcenew
import com.bonushub.crdb.india.utils.DeviceHelper
import com.usdk.apiservice.aidl.algorithm.UAlgorithm
import com.usdk.apiservice.aidl.emv.UEMV
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.UPinpad
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object DBModule {
    lateinit var appDatabase: AppDatabase

    @Provides
    fun provideAppDao(appDatabase: AppDatabase): AppDao {
        return appDatabase.appDao
    }



    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {

       /* return Room.databaseBuilder(appContext, AppDatabase::class.java, "HdfcDB")
            .fallbackToDestructiveMigration()
            .build()*/

        appDatabase =  Room.databaseBuilder(appContext, AppDatabase::class.java, "database.db")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    GlobalScope.launch {
                    //   AppDatabase.onCreate(appDatabase, appContext) // in companion of MyDatabase
                    }

                }
            })
            .build()
        return appDatabase
    }

    @Singleton
    @Provides
    fun providekeyechangeDataSourcenew(appDao: AppDao): IKeyExchange {
        return keyexchangeDataSourcenew(appDao)
    }


    @Singleton
    @USDKScope
    @Provides
    fun provideAlgoritm() : UAlgorithm?{
        return DeviceHelper.getAlgorithm()
    }


    @Singleton
    @USDKScope
    @Provides
    fun providePINPAD() : UPinpad?{
        return DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP)
    }



    @Provides
    fun providekeyechangeDataSource(appDao: AppDao): IKeyExchange {
        return keyexchangeDataSource(appDao)
    }

    @Singleton
    @MainCoroutineScope
    @Provides
    fun providesIOCoroutineScope(@IoDispatcher ioDispatcher: CoroutineDispatcher): CoroutineScope
            = CoroutineScope(SupervisorJob() + ioDispatcher)


    @Singleton
    @Provides
    fun provideDBRepository(appDao: AppDao,keyexcngeDataSource: keyexchangeDataSource,
                                   @IoDispatcher ioDispatcher: CoroutineDispatcher,
                                   @MainCoroutineScope  coroutineScope: CoroutineScope
    ) = RoomDBRepository(appDao,keyexcngeDataSource,ioDispatcher,coroutineScope)

    @USDKScope
    @Provides
    fun provideUemv() : UEMV?{
        return DeviceHelper.getEMV()
    }



}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainCoroutineScope

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class USDKScope


//https://codelabs.developers.google.com/codelabs/android-hilt/#6
//https://github.com/google-developer-training/android-kotlin-fundamentals-apps/blob/master/RecyclerViewFundamentals/app/src/main/java/com/example/android/trackmysleepquality/sleeptracker/SleepTrackerFragment.kt

