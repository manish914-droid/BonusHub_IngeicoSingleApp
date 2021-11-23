package com.bonushub.crdb.model.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromTIDSList(tidList: List<String?>?): String? {
        val type = object : TypeToken<List<String?>?>() {}.type
        return Gson().toJson(tidList, type)
    }

    @TypeConverter
    fun toTIDSList(tidString: String?): List<String>? {
        val type = object : TypeToken<List<String?>?>() {}.type
        return Gson().fromJson<List<String>>(tidString, type)
    }

    @TypeConverter
    fun fromInitDataList(initDataListList: List<InitDataListList?>?): String? {
        val type = object : TypeToken<List<InitDataListList?>?>() {}.type
        return Gson().toJson(initDataListList, type)
    }

    @TypeConverter
    fun toInitDataSList(initDataListList: String?): List<InitDataListList>? {
        val type = object : TypeToken<List<InitDataListList?>?>() {}.type
        return Gson().fromJson<List<InitDataListList>>(initDataListList, type)
    }

}