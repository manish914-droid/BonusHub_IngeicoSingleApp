package com.bonushub.crdb.di.scope

@MustBeDocumented
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class BHFieldParseIndex(val index: Int)