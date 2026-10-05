package com.qianyunyi.rider.entity.animation;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import org.jetbrains.annotations.NotNull;

public class MotorcycleLeanAnimation implements IAnimation {
    private float leanAmount;
    private float pitchAmount;
    private boolean active;

    public void setLeanAmount(float leanAmount) {
        this.leanAmount = leanAmount;
    }

    public float getLeanAmount() {
        return leanAmount;
    }

    public void setPitchAmount(float pitchAmount) {
        this.pitchAmount = pitchAmount;
    }

    public float getPitchAmount() {
        return pitchAmount;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void tick() {}

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public @NotNull Vec3f get3DTransform(@NotNull String modelName, @NotNull TransformType type, float tickDelta, @NotNull Vec3f value0) {
        if (active && "body".equals(modelName)) {
            if (type == TransformType.ROTATION) {
                float leanRad = (float) Math.toRadians(leanAmount);
                float pitchRad = (float) Math.toRadians(pitchAmount);
                return new Vec3f(value0.getX() - pitchRad, value0.getY(), value0.getZ() - leanRad);
            }
            if (type == TransformType.POSITION) {
                return value0;
            }
        }
        return value0;
    }

    @Override
    public void setupAnim(float tickDelta) {}
}