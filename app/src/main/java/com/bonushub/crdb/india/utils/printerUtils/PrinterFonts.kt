package com.bonushub.crdb.india.utils.printerUtils

import android.content.res.AssetManager
import com.bonushub.crdb.india.HDFCApplication

object PrinterFonts {
// Inconsolta
    const val FONT_AGENCYR = "f25bank.ttf"
    var path = ""
    fun initialize(assets: AssetManager?) {
        val fileName = FONT_AGENCYR
        path = HDFCApplication.appContext.externalCacheDir?.path + "/fonts/"
        ExtraFiles.copy("fonts/$fileName", path, fileName, assets, false)
    }
}

