package com.bonushub.crdb.india.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.di.scope.BHDashboardItem
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.TerminalParameterTable
import com.bonushub.crdb.india.utils.EDashboardItem
import com.bonushub.crdb.india.view.fragments.DashboardFragment
import com.google.gson.Gson

class DashboardRepository {

    companion object{

        @Synchronized
        fun getInstance():DashboardRepository{
            return DashboardRepository()
        }
    }

    suspend fun getEDashboardItem():LiveData<ArrayList<EDashboardItem>>{
        val dataList = MutableLiveData<ArrayList<EDashboardItem>>()
       val itemList = mutableListOf<EDashboardItem>()
         val list1 = arrayListOf<EDashboardItem>()
         val list2 = arrayListOf<EDashboardItem>()

        var table: TerminalParameterTable? = null
        if(AppPreference.getLogin()) {
            table = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("1")
        }else{
            table = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("-1")
        }
//        var table =  DBModule.appDatabase.appDao.getTerminalParameterTableData()
        if (table != null) {
            if (DashboardFragment.toRefresh || itemList.isEmpty()) {
                itemList.clear()
                list1.clear()
                list2.clear()
                if (table != null) {
                    val tableClass =
                        table::class.java //Class Name (class com.bonushub.pax.utilss.TerminalParameterTable)
                    Log.d("tptTData:- ", Gson().toJson(table.reservedValues))
                    for (e in tableClass.declaredFields) {
                        val ann = e.getAnnotation(BHDashboardItem::class.java)
                        //If table's field  having the particular annotation as @BHDasboardItem then it returns the value ,If not then return null
                        if (ann != null) {
                            e.isAccessible = true
                            val t = e.get(table) as String
                            if (t == "1") {
                                itemList.add(ann.item)
                                if (ann.childItem != EDashboardItem.NONE) {
                                    itemList.add(ann.childItem)
                                    //itemList.add(ann.childItem2)
                                }
                            }
                        }
                    }

                } else {
                    itemList.add(EDashboardItem.NONE)
                }
                Log.d("itemList===>:- ", Gson().toJson(itemList))
                //itemList.add(EDashboardItem.MERCHANT_REFERRAL)  // disable MRP

                ///65655655555555555555555555555555itemList.add(EDashboardItem.PREAUTH_VIEW)

                Log.d("itemList===>:- ", Gson().toJson(itemList))
                // This list is a list where all types of preath available which was enable by backend
                val totalPreAuthItem = mutableListOf<EDashboardItem>()
               totalPreAuthItem.addAll(itemList)

                //After converting we are getting the total preauth trans type available(by retainAll fun)
                //It returns true if any praauth item is available and return false if no preauth item found
                val isAnyPreAuthItemAvailable = totalPreAuthItem.retainAll { item ->
                    item == EDashboardItem.PREAUTH || item == EDashboardItem.PREAUTH_COMPLETE
                          ||item == EDashboardItem.PREAUTH_VIEW || item == EDashboardItem.VOID_PREAUTH || item == EDashboardItem.PENDING_PREAUTH
                }

              if (isAnyPreAuthItemAvailable) {
                    itemList.removeAll { item ->
                        item == EDashboardItem.PREAUTH || item == EDashboardItem.PREAUTH_COMPLETE
                                || item == EDashboardItem.VOID_PREAUTH ||item == EDashboardItem.PREAUTH_VIEW
                                || item == EDashboardItem.VOID_PREAUTH || item == EDashboardItem.PENDING_PREAUTH
                    }
                    if (totalPreAuthItem.size > 0) {
                        val preAuth = EDashboardItem.PRE_AUTH_CATAGORY
                        preAuth.childList = totalPreAuthItem
                        itemList.add(preAuth)
                    }
                }

                itemList.sortWith(compareBy { it.rank })
                // Below code is used for dashboard items divided into view less and view more functionality
                list1.addAll(itemList)
                dataList.postValue(list1)
            }
            }
        return dataList
        }
    }

