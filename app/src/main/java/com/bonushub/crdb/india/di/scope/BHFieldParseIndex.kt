package com.bonushub.crdb.india.di.scope

import com.bonushub.crdb.india.utils.EDashboardItem

@MustBeDocumented
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class BHFieldParseIndex(val index: Int)
annotation class BHDashboardItem(
    val item: EDashboardItem,
    val childItem: EDashboardItem = EDashboardItem.NONE,
    val childItem2:EDashboardItem = EDashboardItem.NONE
)

@MustBeDocumented
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class BHFieldName(val name: String, val isToShow: Boolean = true)