package com.qianyunyi.rider.config.common;

public class RiderFrameworkException extends RuntimeException {
    public RiderFrameworkException(String message) {
        super(message);
    }

    public RiderFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public static RiderFrameworkException of(String format, Object... args) {
        return new RiderFrameworkException(String.format(format, args));
    }
}