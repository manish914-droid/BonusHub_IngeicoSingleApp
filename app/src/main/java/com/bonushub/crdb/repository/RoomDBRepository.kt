package com.bonushub.crdb.repository

import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.model.*
import com.bonushub.crdb.utils.NoResponseException
import com.bonushub.crdb.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class RoomDBRepository @Inject  constructor(private val appDao: AppDao,
                                            private val keyexchangeDataSource: keyexchangeDataSource){


    suspend fun  insertdata(student: TerminalCommunicationTable) = appDao.insert(student)
    suspend fun  fetchdata() = appDao.fetch()

     suspend fun  insertTid(tid: String, backToCalled: ApiCallback) = keyexchangeDataSource.startExchange(tid)

    suspend fun fetchTrendingMovies(tid: String): Flow<Result<NoResponseException>> = flow{
        emit(keyexchangeDataSource.startExchange(tid))

    }

  //  suspend fun execute(): Flow<DataState<List<Blog>>> = flow
}