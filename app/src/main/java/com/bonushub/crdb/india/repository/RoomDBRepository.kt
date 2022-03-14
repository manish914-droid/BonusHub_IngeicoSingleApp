package com.bonushub.crdb.india.repository

import com.bonushub.crdb.india.db.AppDao

import com.bonushub.crdb.india.di.IoDispatcher
import com.bonushub.crdb.india.di.MainCoroutineScope
import com.bonushub.crdb.india.model.local.TerminalCommunicationTable
import com.bonushub.crdb.india.utils.ResponseHandler
import com.bonushub.crdb.india.utils.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomDBRepository @Inject constructor(private val appDao: AppDao,
                                           private val keyexchangeDataSource: keyexchangeDataSource,
                                           @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
                                           @MainCoroutineScope private val applicationScope: CoroutineScope) {




    suspend fun  insertdata(student: TerminalCommunicationTable) = appDao.insertTerminalCommunicationData(student)
    suspend fun  fetchdata() =appDao.fetch()
    //region======================Get TPT Data:-
    suspend fun  fetcDashboarddata() = appDao.getAllTerminalParameterTableData()?.get(0)
    //endregion



    //  suspend fun  insertTid(tid: String, backToCalled: ApiCallback) = keyexchangeDataSource.startExchange(tid)

    suspend fun fetchInitData(tid: String): Flow<Result<ResponseHandler>> = flow{
        emit(keyexchangeDataSource.startExchange1(tid))
    }.flowOn(ioDispatcher)

    //  suspend fun execute(): Flow<DataState<List<Blog>>> = flow
}