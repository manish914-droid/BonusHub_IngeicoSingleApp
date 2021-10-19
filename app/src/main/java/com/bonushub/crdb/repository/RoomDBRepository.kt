package com.bonushub.crdb.repository

import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.model.*
import com.bonushub.crdb.utils.ResponseHandler
import com.bonushub.crdb.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class RoomDBRepository @Inject  constructor(private val appDao: AppDao,
                                            private val keyexchangeDataSource: keyexchangeDataSource){


    suspend fun  insertdata(student: TerminalCommunicationTable) = appDao.insert(student)
    suspend fun  fetchdata() =appDao.fetch()
    //region======================Get TPT Data:-
    suspend fun  fetcDashboarddata() = appDao.getAllTerminalParameterTableData()?.get(0)
    //endregion

   //  suspend fun  insertTid(tid: String, backToCalled: ApiCallback) = keyexchangeDataSource.startExchange(tid)

    suspend fun fetchInitData(tid: String): Flow<Result<ResponseHandler>> = flow{
        emit(keyexchangeDataSource.startExchange1(tid))

    }.flowOn(Dispatchers.IO)

  //  suspend fun execute(): Flow<DataState<List<Blog>>> = flow
}