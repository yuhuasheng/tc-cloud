package com.foxconn.plm.entity.constants;

/**
 * @author Robert
 */
public enum TCUserEnum {
    DEV("dev"), SPAS1("spas1"), SPAS2("spas2"), SPAS3("spas3"), SPAS4("spas4");

    private final String value;

    TCUserEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
