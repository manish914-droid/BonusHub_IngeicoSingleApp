/*
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) 2019-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date	                 Author	                Action
 * 20190110  	         xieYb                  Create
 */

package com.bonushub.crdb.india.utils.convert;


import com.bonushub.crdb.india.utils.impl.ConverterImp;

public class ConvertHelper {
    private ConvertHelper() {
    }

    private static ConverterImp converterImp;



    public static IConvert getConvert() {
        if (converterImp == null) {
            converterImp = new ConverterImp();
        }
        return converterImp;
    }
}
