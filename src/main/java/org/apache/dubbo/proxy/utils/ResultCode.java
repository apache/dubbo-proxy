package org.apache.dubbo.proxy.utils;

public enum ResultCode {

    OK(0),

    TIMEOUT(1),

    BIZERROR(2),

    NETWORKERROR(3),

    SERIALIZATION(4);


    private int code;

    ResultCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
