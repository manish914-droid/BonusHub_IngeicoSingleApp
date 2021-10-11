package com.bonushub.crdb.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.repository.RoomDBRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
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

        appDatabase =  Room.databaseBuilder(
            appContext,
            AppDatabase::class.java, "database.db"
        )
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


    @Provides
    fun provideStudentDBRepository(appDao: AppDao) = RoomDBRepository(appDao)

}


//https://codelabs.developers.google.com/codelabs/android-hilt/#6
//https://github.com/google-developer-training/android-kotlin-fundamentals-apps/blob/master/RecyclerViewFundamentals/app/src/main/java/com/example/android/trackmysleepquality/sleeptracker/SleepTrackerFragment.kt