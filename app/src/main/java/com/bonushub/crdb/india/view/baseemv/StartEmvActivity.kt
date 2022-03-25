package com.bonushub.crdb.india.view.baseemv

import com.usdk.apiservice.aidl.emv.EMVEventHandler

class StartEmvActivity : BaseActivity() {

    override var emvEventHandler: EMVEventHandler
        get() = super.emvEventHandler
        set(value) {}

    override fun doInitEMV() {
        super.doInitEMV()

    }
}