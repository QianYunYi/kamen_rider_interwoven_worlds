package com.qianyunyi.rider.client.camera;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Camera 反射访问器 — 突破 protected setPosition/setRotation 限制。
 * 优先使用 Method 调用，失败时回退到直接字段访问，兼容 Oculus/Embeddium 等光影 Mod。
 */
final class CameraAccessor {
    private static final Method SET_POSITION;
    private static final Method SET_ROTATION;
    private static final Method MOVE;

    private static final Field FIELD_POSITION;
    private static final Field FIELD_XROT;
    private static final Field FIELD_YROT;

    static {
        SET_POSITION = findMethod("setPosition");
        SET_ROTATION = findMethod("setRotation");
        MOVE = findMethod("move");

        FIELD_POSITION = findField("position");
        FIELD_XROT = findField("xRot");
        FIELD_YROT = findField("yRot");
    }

    private static Method findMethod(String name) {
        for (Method method : Camera.class.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static Field findField(String name) {
        try {
            Field field = Camera.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    static void setPosition(Camera camera, double x, double y, double z) {
        try {
            if (SET_POSITION != null) {
                Class<?>[] paramTypes = SET_POSITION.getParameterTypes();
                if (paramTypes.length == 1) {
                    SET_POSITION.invoke(camera, new Vec3(x, y, z));
                } else {
                    SET_POSITION.invoke(camera, x, y, z);
                }
            } else if (FIELD_POSITION != null) {
                FIELD_POSITION.set(camera, new Vec3(x, y, z));
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to set camera position", e);
        }
    }

    static void setRotation(Camera camera, float yRot, float xRot) {
        try {
            if (SET_ROTATION != null) {
                SET_ROTATION.invoke(camera, yRot, xRot);
            } else {
                if (FIELD_XROT != null) FIELD_XROT.setFloat(camera, xRot);
                if (FIELD_YROT != null) FIELD_YROT.setFloat(camera, yRot);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to set camera rotation", e);
        }
    }

    static void move(Camera camera, double distanceOffset, double verticalOffset, double horizontalOffset) {
        try {
            if (MOVE != null) {
                MOVE.invoke(camera, distanceOffset, verticalOffset, horizontalOffset);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to move camera", e);
        }
    }
}