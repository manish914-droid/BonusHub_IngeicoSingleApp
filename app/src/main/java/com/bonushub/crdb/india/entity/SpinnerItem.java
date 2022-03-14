package com.bonushub.crdb.india.entity;

/**
 * 下拉框每个选项的内容
 */
public class SpinnerItem {
    int value;
    String name;
    String strValue;

    public SpinnerItem (int value, String name) {
        this.value = value;
        this.name = name;
    }

    public SpinnerItem (String value, String name) {
        this.strValue = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getStringValue() {
        return strValue;
    }

    @Override
    public String toString() {
        return name;
    }
}
