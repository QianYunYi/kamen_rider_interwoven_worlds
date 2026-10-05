// src/main/java/com/rider/qianyunyi/common/RiderAssertions.java
package com.qianyunyi.rider.config.common;

import javax.annotation.Nullable;

public final class RiderAssertions {
    private RiderAssertions() {}

    public static void notNull(@Nullable Object obj, String message) {
        if (obj == null) {
            throw new RiderFrameworkException(message);
        }
    }

    public static void state(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    public static void argument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new RiderFrameworkException(message);
        }
    }
}