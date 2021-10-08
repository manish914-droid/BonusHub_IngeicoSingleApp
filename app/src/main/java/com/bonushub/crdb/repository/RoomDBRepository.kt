package com.bonushub.crdb.repository

import com.bonushub.crdb.model.TerminalCommunicationTable
import com.bonushub.crdb.db.AppDao
import javax.inject.Inject

class RoomDBRepository @Inject  constructor(private val appDao: AppDao){
    suspend fun  insertdata(student: TerminalCommunicationTable) = appDao.insert(student)

    suspend fun  fetchdata() = appDao.fetch()
}