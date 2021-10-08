/*
 * ===========================================================================================
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) 2019-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date                  Author	                 Action
 * 20190108  	         Kim.L                   Create
 * ===========================================================================================
 */
package com.bonushub.crdb.utils.impl;



import com.bonushub.crdb.utils.convert.IConvert;

import java.text.NumberFormat;
import java.util.Arrays;

public class ConverterImp implements IConvert {
    private static final char[] ARRAY_OF_CHAR = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static ConverterImp converterImp;

    public ConverterImp() {
    }

    private static int strByte2Int(byte b) {
        int j;
        if ((b >= 'a') && (b <= 'z')) {
            j = b - 'a' + 0x0A;
        } else {
            if ((b >= 'A') && (b <= 'Z'))
                j = b - 'A' + 0x0A;
            else
                j = b - '0';
        }
        return j;
    }

    public String bcdToStr(byte[] b) {
        if (b == null) {
            throw new IllegalArgumentException("bcdToStr input arg is null");
        }

        StringBuilder localStringBuilder = new StringBuilder(b.length * 2);
        for (byte i : b) {
            localStringBuilder.append(ARRAY_OF_CHAR[((i & 0xF0) >>> 4)]);
            localStringBuilder.append(ARRAY_OF_CHAR[(i & 0xF)]);
        }

        return localStringBuilder.toString();
    }

    @Override
    public String bcdToStr(byte[] b, int length) {
        if (b == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = 0; i < length; ++i) {
            sb.append(ARRAY_OF_CHAR[((b[i] & 0xF0) >>> 4)]);
            sb.append(ARRAY_OF_CHAR[(b[i] & 0xF)]);
        }

        return sb.toString();
    }

    @Override
    public byte[] strToBcdPaddingLeft(String str) {
        return strToBcd(str, EPaddingPosition.PADDING_LEFT);
    }

    @Override
    public byte[] strToBcdPaddingRight(String str) {
        return strToBcd(str, EPaddingPosition.PADDING_RIGHT);
    }

    public byte[] strToBcd(String str, EPaddingPosition paddingPosition) {
        if ((str == null) || (paddingPosition == null)) {
            throw new IllegalArgumentException("strToBcd input arg is null");
        }
        String s = str;
        int len = s.length();
        if (len % 2 != 0) {
            if (paddingPosition == EPaddingPosition.PADDING_RIGHT)
                s = s + "0";
            else {
                s = "0" + s;
            }
            len = s.length();
        }
        if (len >= 2) {
            len /= 2;
        }
        byte[] bcd = new byte[len];
        byte[] strBytes = s.getBytes();

        for (int p = 0; p < strBytes.length / 2; p++) {
            bcd[p] = (byte) ((strByte2Int(strBytes[(2 * p)]) << 4) + strByte2Int(strBytes[(2 * p + 1)]));
        }

        return bcd;
    }

    public void longToByteArray(long l, byte[] to, int offset, EEndian endian) {
        if ((to == null) || (endian == null)) {
            throw new IllegalArgumentException("longToByteArray input arg is null");
        }

        if (endian == EEndian.BIG_ENDIAN) {
            to[offset] = (byte) (int) (l >>> 56 & 0xFF);
            to[(offset + 1)] = (byte) (int) (l >>> 48 & 0xFF);
            to[(offset + 2)] = (byte) (int) (l >>> 40 & 0xFF);
            to[(offset + 3)] = (byte) (int) (l >>> 32 & 0xFF);
            to[(offset + 4)] = (byte) (int) (l >>> 24 & 0xFF);
            to[(offset + 5)] = (byte) (int) (l >>> 16 & 0xFF);
            to[(offset + 6)] = (byte) (int) (l >>> 8 & 0xFF);
            to[(offset + 7)] = (byte) (int) (l & 0xFF);
            return;
        }
        to[(offset + 7)] = (byte) (int) (l >>> 56 & 0xFF);
        to[(offset + 6)] = (byte) (int) (l >>> 48 & 0xFF);
        to[(offset + 5)] = (byte) (int) (l >>> 40 & 0xFF);
        to[(offset + 4)] = (byte) (int) (l >>> 32 & 0xFF);
        to[(offset + 3)] = (byte) (int) (l >>> 24 & 0xFF);
        to[(offset + 2)] = (byte) (int) (l >>> 16 & 0xFF);
        to[(offset + 1)] = (byte) (int) (l >>> 8 & 0xFF);
        to[offset] = (byte) (int) (l & 0xFF);
    }

    public byte[] longToByteArray(long l, EEndian endian) {
        if (endian == null) {
            throw new IllegalArgumentException("longToByteArray input arg is null");
        }

        byte[] arrayOfByte = new byte[8];

        if (endian == EEndian.BIG_ENDIAN) {
            arrayOfByte[0] = (byte) (int) (l >>> 56 & 0xFF);
            arrayOfByte[1] = (byte) (int) (l >>> 48 & 0xFF);
            arrayOfByte[2] = (byte) (int) (l >>> 40 & 0xFF);
            arrayOfByte[3] = (byte) (int) (l >>> 32 & 0xFF);
            arrayOfByte[4] = (byte) (int) (l >>> 24 & 0xFF);
            arrayOfByte[5] = (byte) (int) (l >>> 16 & 0xFF);
            arrayOfByte[6] = (byte) (int) (l >>> 8 & 0xFF);
            arrayOfByte[7] = (byte) (int) (l & 0xFF);
        } else {
            arrayOfByte[7] = (byte) (int) (l >>> 56 & 0xFF);
            arrayOfByte[6] = (byte) (int) (l >>> 48 & 0xFF);
            arrayOfByte[5] = (byte) (int) (l >>> 40 & 0xFF);
            arrayOfByte[4] = (byte) (int) (l >>> 32 & 0xFF);
            arrayOfByte[3] = (byte) (int) (l >>> 24 & 0xFF);
            arrayOfByte[2] = (byte) (int) (l >>> 16 & 0xFF);
            arrayOfByte[1] = (byte) (int) (l >>> 8 & 0xFF);
            arrayOfByte[0] = (byte) (int) (l & 0xFF);
        }

        return arrayOfByte;
    }

    public void intToByteArray(int paramInt1, byte[] paramArrayOfByte, int paramInt2, EEndian paramEEndian) {
        if ((paramArrayOfByte == null) || (paramEEndian == null)) {
            throw new IllegalArgumentException("longToByteArray input arg is null");
        }

        if (paramEEndian == EEndian.BIG_ENDIAN) {
            paramArrayOfByte[paramInt2] = (byte) (paramInt1 >>> 24 & 0xFF);
            paramArrayOfByte[(paramInt2 + 1)] = (byte) (paramInt1 >>> 16 & 0xFF);
            paramArrayOfByte[(paramInt2 + 2)] = (byte) (paramInt1 >>> 8 & 0xFF);
            paramArrayOfByte[(paramInt2 + 3)] = (byte) (paramInt1 & 0xFF);
            return;
        }
        paramArrayOfByte[paramInt2] = (byte) (paramInt1 & 0xFF);
        paramArrayOfByte[(paramInt2 + 1)] = (byte) (paramInt1 >>> 8 & 0xFF);
        paramArrayOfByte[(paramInt2 + 2)] = (byte) (paramInt1 >>> 16 & 0xFF);
        paramArrayOfByte[(paramInt2 + 3)] = (byte) (paramInt1 >>> 24 & 0xFF);
    }

    public byte[] intToByteArray(int paramInt, EEndian paramEEndian) {
        if (paramEEndian == null) {
            throw new IllegalArgumentException("intToByteArray input arg is null");
        }

        byte[] arrayOfByte = new byte[4];
        int offset = 0;

        if (paramEEndian == EEndian.BIG_ENDIAN) {
            arrayOfByte[0] = (byte) (paramInt >>> 24 & 0xFF);
            arrayOfByte[1] = (byte) (paramInt >>> 16 & 0xFF);
            arrayOfByte[2] = (byte) (paramInt >>> 8 & 0xFF);
            arrayOfByte[3] = (byte) (paramInt & 0xFF);
        } else {
            arrayOfByte[0] = (byte) (paramInt & 0xFF);
            arrayOfByte[1] = (byte) (paramInt >>> 8 & 0xFF);
            arrayOfByte[2] = (byte) (paramInt >>> 16 & 0xFF);
            arrayOfByte[3] = (byte) (paramInt >>> 24 & 0xFF);
        }

        for (int j = 0; j < arrayOfByte.length; ++j) {
            if (arrayOfByte[j] != 0) {
                return Arrays.copyOfRange(arrayOfByte, j, arrayOfByte.length);
            }
        }

        return arrayOfByte;
    }

    public void shortToByteArray(short paramShort, byte[] paramArrayOfByte, int paramInt, EEndian paramEEndian) {
        if ((paramArrayOfByte == null) || (paramEEndian == null)) {
            throw new IllegalArgumentException("shortToByteArray input arg is null");
        }

        if (paramEEndian == EEndian.BIG_ENDIAN) {
            paramArrayOfByte[paramInt] = (byte) (paramShort >>> 8 & 0xFF);
            paramArrayOfByte[(paramInt + 1)] = (byte) (paramShort & 0xFF);
            return;
        }
        paramArrayOfByte[paramInt] = (byte) (paramShort & 0xFF);
        paramArrayOfByte[(paramInt + 1)] = (byte) (paramShort >>> 8 & 0xFF);
    }

    public byte[] shortToByteArray(short paramShort, EEndian paramEEndian) {
        if (paramEEndian == null) {
            throw new IllegalArgumentException("shortToByteArray input arg is null");
        }

        byte[] arrayOfByte = new byte[2];

        if (paramEEndian == EEndian.BIG_ENDIAN) {
            arrayOfByte[0] = (byte) (paramShort >>> 8 & 0xFF);
            arrayOfByte[1] = (byte) (paramShort & 0xFF);
        } else {
            arrayOfByte[0] = (byte) (paramShort & 0xFF);
            arrayOfByte[1] = (byte) (paramShort >>> 8 & 0xFF);
        }

        return arrayOfByte;
    }

    public long longFromByteArray(byte[] paramArrayOfByte, int paramInt, EEndian paramEEndian) {
        if ((paramArrayOfByte == null) || (paramEEndian == null)) {
            throw new IllegalArgumentException("longFromByteArray input arg is null");
        }

        if (paramEEndian == EEndian.BIG_ENDIAN) {
            return paramArrayOfByte[(paramInt + 4)] << 24 & 0xFF000000 | paramArrayOfByte[(paramInt + 5)] << 16 & 0xFF0000 |
                    paramArrayOfByte[(paramInt + 6)] << 8 & 0xFF00 | paramArrayOfByte[(paramInt + 7)] & 0xFF;
        }
        return paramArrayOfByte[(paramInt + 3)] << 24 & 0xFF000000 | paramArrayOfByte[(paramInt + 2)] << 16 & 0xFF0000 |
                paramArrayOfByte[(paramInt + 1)] << 8 & 0xFF00 | paramArrayOfByte[paramInt] & 0xFF;
    }

    public int intFromByteArray(byte[] paramArrayOfByte, int paramInt, EEndian paramEEndian) {
        if ((paramArrayOfByte == null) || (paramEEndian == null)) {
            throw new IllegalArgumentException("intFromByteArray input arg is null");
        }

        if (paramEEndian == EEndian.BIG_ENDIAN) {
            return paramArrayOfByte[paramInt] << 24 & 0xFF000000 | paramArrayOfByte[(paramInt + 1)] << 16 & 0xFF0000 |
                    paramArrayOfByte[(paramInt + 2)] << 8 & 0xFF00 | paramArrayOfByte[(paramInt + 3)] & 0xFF;
        }
        return paramArrayOfByte[(paramInt + 3)] << 24 & 0xFF000000 | paramArrayOfByte[(paramInt + 2)] << 16 & 0xFF0000 |
                paramArrayOfByte[(paramInt + 1)] << 8 & 0xFF00 | paramArrayOfByte[paramInt] & 0xFF;
    }

    public short shortFromByteArray(byte[] paramArrayOfByte, int paramInt, EEndian paramEEndian) {
        if ((paramArrayOfByte == null) || (paramEEndian == null)) {
            throw new IllegalArgumentException("shortFromByteArray input arg is null");
        }

        if (paramEEndian == EEndian.BIG_ENDIAN) {
            return (short) (paramArrayOfByte[paramInt] << 8 & 0xFF00 | paramArrayOfByte[(paramInt + 1)] & 0xFF);
        }
        return (short) (paramArrayOfByte[(paramInt + 1)] << 8 & 0xFF00 | paramArrayOfByte[paramInt] & 0xFF);
    }

    public String stringPadding(String paramString, char paramChar, long paramLong, EPaddingPosition paramEPaddingPosition) {
        if ((paramString == null) || (paramEPaddingPosition == null)) {
            throw new IllegalArgumentException("stringPadding input arg is null");
        }

        if (paramString.length() >= paramLong) {
            return paramString;
        }

        if (paramEPaddingPosition == EPaddingPosition.PADDING_RIGHT) {
            StringBuilder sb = new StringBuilder(paramString);
            for (int i = 0; i < paramLong - paramString.length(); i++) {
                sb.append(paramChar);
            }

            return sb.toString();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramLong - paramString.length(); i++) {
            sb.append(paramChar);
        }

        sb.append(paramString);
        return sb.toString();
    }

    public boolean isByteArrayValueSame(byte[] paramArrayOfByte1, int paramInt1, byte[] paramArrayOfByte2, int paramInt2, int paramInt3) {
        if ((paramArrayOfByte1 == null) || (paramArrayOfByte2 == null)) {
            return false;
        }

        if ((paramInt1 + paramInt3 > paramArrayOfByte1.length) || (paramInt2 + paramInt3 > paramArrayOfByte2.length)) {
            return false;
        }

        for (int i = 0; i < paramInt3; i++) {
            if (paramArrayOfByte1[(paramInt1 + i)] != paramArrayOfByte2[(paramInt2 + i)]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getPaddedNumber(long num, int digit) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(false);
        nf.setMaximumIntegerDigits(digit);
        nf.setMinimumIntegerDigits(digit);
        return nf.format(num);
    }
}
