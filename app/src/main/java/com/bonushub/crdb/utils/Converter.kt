@file:JvmName("Converter")
package com.bonushub.crdb.utils

import kotlin.experimental.and
import kotlin.experimental.or

/**
 * =========Written By Ajay Thakur (18th Nov 2020)==========
 */


/**
 *@param input input data String which needs to modified
 * @param padChar padding Char to be added on left or right
 * @param totalLen maximum length of output. if input length is greater than total len same will be returned
 * @param toLeft extra char to be added on start or on end of String
 *  */
//region=============================Add Padding==================
fun addPad(input: String, padChar: String, totalLen: Int, toLeft: Boolean = true): String {
    return if (input.length >= totalLen) {
        input
    } else {
        val sb = StringBuilder()
        val remaining = totalLen - input.length
        if (toLeft) {
            for (e in 1..remaining) {
                sb.append(padChar)
            }
            sb.append(input)
        } else {
            sb.append(input)
            for (e in 1..remaining) {
                sb.append(padChar)
            }
        }
        sb.toString()
    }
}
//endregion

//region==================Add Padding========================
fun addPad(input: Int, padChar: String, totalLen: Int, toLeft: Boolean = true): String =
    addPad(input.toString(), padChar, totalLen, toLeft)
//endregion

//region===================String To Nibble Array=============
fun str2NibbleArr(numString: String): ByteArray {
    val len = numString.length / 2
    val result = ByteArray(len)
    var pointer = 0

    while (pointer < len) {
        val n = numString.substring(pointer * 2, (pointer * 2) + 2)
        val high = (n[0].toInt() shl 4).toByte()
        val low = n[1].toByte() and 0xf
        result[pointer] = high or low
        pointer++
    }
    return result
}
//endregion

//region======================Extension Function================
fun String.str2NibbleArray(): ByteArray = str2NibbleArr(this)
//endregion


fun addBytePad(src: ByteArray, len: Int, dest: Byte = 0, isLeft: Boolean = true): ByteArray {
    val diff = len - src.size
    if (diff > 0) {
        val list = mutableListOf<Byte>()
        if (isLeft) {
            for (e in 0..(diff - 1)) {
                list.add(dest)
            }
            list.addAll(src.toList())
        } else {
            list.addAll(src.toList())
            for (e in 0..(len - 1)) {
                list.add(dest)
            }
        }
        return list.toByteArray()
    } else {
        return src
    }



}

fun tlvParser(data: String, map: HashMap<Int, String>) {
    val tagList = setOf(
        "8A",
        "89",
        "91",
        "71",
        "72",
        "9F26",
        "9F10",
        "9F37",
        "9F36",
        "95",
        "9A",
        "9C",
        "9B",
        "9F02",
        "5F2A",
        "9F1A",
        "82",
        "84",
        "5F34",
        "9F27",
        "9F33",
        "9F34",
        "9F35",
        "9F03",
        "9F47",
        "9F06",
        "9F22",
        "DF05",
        "DF06",
        "DF02",
        "DF03",
        "DF04"
    )
    var pointer = 0
    fun reader() {
        if (pointer < data.length) {
            var temp = data.substring(pointer, pointer + 2)
            if (tagList.contains(temp)) {
                pointer += 2
            } else {
                temp = data.substring(pointer, pointer + 4)
                pointer += 4
            }
            val lenStr = data.substring(pointer, pointer + 2)
            pointer += 2

            val len = java.lang.Integer.decode("0x$lenStr") * 2
            val key = java.lang.Integer.decode("0x$temp")

            if (len != 0) {
                val value = data.substring(pointer, pointer + len)
                pointer += len
                map[key] = value
            } else {
                map[key] = ""
            }
            reader()
        }
    }

    reader()
}


/**
 * Converts hex String into ByteArray
 * hex string of two chars have max  int posEntryValue 255 and occupies 1 byte
 * posEntryValue varies from -128 to 127
 * */

fun String.hexStr2ByteArr(): ByteArray {
    val arr = ByteArray(this.length / 2)
    val ca: CharArray = this.toCharArray()

    var index = 0
    while (index < ca.size) {
        val a = hex2Int(ca[index])   // tenth
        val b = hex2Int(ca[index + 1]) //Once
        arr[index / 2] = ((a shl 4) + b).toByte()
        index += 2
    }

    return arr
}

/**
 * Converts byte (-128 to 127) into hex String with two chars
 * */
fun ByteArray.byteArr2HexStr(): String {
    val bu = StringBuilder()
    for (d in this) {
        val dI = d.toUnsigned()
        val be = dI shr 4 // calc for big endian
        val se = dI and 15 // calc for small endian (15 = 0000 1111)
        bu.append(intCharMap(be))
        bu.append(intCharMap(se))
    }

    return bu.toString()
}

/**
 * Converts byteArray into String
 * */
fun ByteArray.byteArr2Str(): String {
    val builder = StringBuilder()
    for (each in this) {
        builder.append(each.toChar())
    }
    return builder.toString()
}


fun ByteArray.byteArr2HexStr(len: Int): String {
    val ba = if (len < size) ByteArray(len) { this[it] } else this
    return ba.byteArr2HexStr()
}

/**
 * Converts string into byte array
 * */
fun String.str2ByteArr(): ByteArray {
    val cArray = this.toCharArray()
    val bA = ByteArray(cArray.size)
    for (index in cArray.indices) {
        bA[index] = cArray[index].toByte()
    }
    return bA
}


/**
 * Converts byte into unsigned int
 * */
fun Byte.toUnsigned(): Int {
    val x = this.toInt()
    return (x shl 24) ushr 24
}

private fun hex2Int(hex: Char): Int {
    return when (hex) {
        in 'a'..'z' -> (hex - 'a' + 10)
        in 'A'..'Z' -> (hex - 'A' + 10)
        in '0'..'9' -> (hex - '0')
        else -> 0
    }
}

fun hexString2String(str: String): String {
    return (str.hexStr2ByteArr()).byteArr2Str()
}


    fun paddingInvoiceRoc(invoiceNo: String?) =
        invoiceNo?.let { addPad(input = it, padChar = "0", totalLen = 6, toLeft = true) }




private fun intCharMap(int: Int): Char {
    return when (int) {
        0 -> '0'
        1 -> '1'
        2 -> '2'
        3 -> '3'
        4 -> '4'
        5 -> '5'
        6 -> '6'
        7 -> '7'
        8 -> '8'
        9 -> '9'
        10 -> 'A'
        11 -> 'B'
        12 -> 'C'
        13 -> 'D'
        14 -> 'E'
        15 -> 'F'
        else -> '?'
    }


}

