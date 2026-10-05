package com.qianyunyi.rider.entity.vehicle.gouram;

import com.qianyunyi.rider.entity.GouramEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
/**
 * 哥莱姆驾驶行为
 */
public class GouramRideBehavior {
    private final GouramEntity gouram;

    boolean hoverMode = false;
    float rideBackCurrentRoll = 0f;
    float rideBackSpeed = 0f;
    double rideBackInertiaX = 0;
    double rideBackInertiaZ = 0;
    float rideBackTargetYaw = 0f;
    int rideBackDriveTime = 0;
    float rideBackCachedSin = 0f;
    float rideBackCachedCos = 1f;
    float rideBackPlayerYawOffset = 0f;
    float leanAmount = 0f;

    public GouramRideBehavior(GouramEntity gouram) {
        this.gouram = gouram;
    }

    public boolean isHoverMode() {
        return hoverMode;
    }

    public float getLeanAmount() {
        return leanAmount;
    }

    public void toggleHoverMode() {
        hoverMode = !hoverMode;
        if (hoverMode) {
            rideBackSpeed = 0;
            rideBackInertiaX = 0;
            rideBackInertiaZ = 0;
        }
    }

    public void initRideBack() {
        hoverMode = false;
        rideBackSpeed = 0f;
        rideBackInertiaX = 0;
        rideBackInertiaZ = 0;
        rideBackTargetYaw = gouram.getYRot();
        rideBackDriveTime = 0;
        rideBackPlayerYawOffset = 0f;
        refreshRideBackYawCache();
    }

    public void travelGrabHand(LivingEntity passenger) {
        float forward = passenger.zza;
        float strafe = passenger.xxa;
        boolean hasInput = Math.abs(forward) > 0.01f || Math.abs(strafe) > 0.01f;
        if (!hasInput) {
            gouram.setDeltaMovement(0, 0, 0);
            gouram.move(MoverType.SELF, gouram.getDeltaMovement());
            return;
        }

        float vertical = 0;
        float horizForward = forward;

        float pitch = passenger.getXRot();
        if (forward > 0.01f && pitch < -10) {
            double pitchRad = Math.toRadians(-pitch);
            vertical = forward * (float) Math.sin(pitchRad) * GouramConfig.grabVerticalSensitivity;
            horizForward = forward * (float) Math.cos(pitchRad);
        } else if (pitch < -20) {
            vertical = GouramConfig.grabVerticalSensitivity;
        } else if (pitch > 20) {
            vertical = -GouramConfig.grabVerticalSensitivity * 0.625F;
        }

        int groundY = gouram.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gouram.blockPosition()).getY();
        if (gouram.getY() < groundY + GouramConfig.grabMinGroundHeight) {
            vertical = Math.max(vertical, GouramConfig.grabLiftForce);
        }

        float yawRad = (float) Math.toRadians(gouram.getYRot());
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);
        double motionX = strafe * cosYaw - horizForward * sinYaw;
        double motionZ = strafe * sinYaw + horizForward * cosYaw;

        gouram.setDeltaMovement(motionX * GouramConfig.grabForwardSpeed, vertical, motionZ * GouramConfig.grabForwardSpeed);
        gouram.move(MoverType.SELF, gouram.getDeltaMovement());
    }

    public void travelRideBack(LivingEntity passenger) {
        float forward = passenger.zza;
        float strafe = -passenger.xxa;

        if (hoverMode) {
            rideBackSpeed *= GouramConfig.hoverModeDecay;
            rideBackInertiaX *= GouramConfig.hoverModeDecay;
            rideBackInertiaZ *= GouramConfig.hoverModeDecay;
            if (Math.abs(rideBackSpeed) < 0.01f) rideBackSpeed = 0;
            leanAmount *= 0.85f;
            float vertical = (float) (gouram.getDeltaMovement().y * GouramConfig.hoverModeStabilityFactor);
             gouram.doSuperTravel(new Vec3(strafe * 0.2f, vertical, forward * 0.2f));
            return;
        }

        boolean isAccelerating = forward > 0.01f;

        gouram.setYRot(normalizeAngle(gouram.getYRot() + strafe * GouramConfig.pillionBaseTurnSpeed * GouramConfig.pillionTurnSmooth));
        refreshRideBackYawCache();

        passenger.yBodyRot = gouram.getYRot();
        passenger.yBodyRotO = gouram.yRotO;

        float rawYawOffset = normalizeAngle(passenger.getYRot() - gouram.getYRot());
        float maxLookYaw = 60.0f;
        if (Math.abs(passenger.getXRot()) < 85f) {
            rideBackPlayerYawOffset = rawYawOffset;
        }
        rideBackPlayerYawOffset = Mth.clamp(rideBackPlayerYawOffset, -maxLookYaw, maxLookYaw);
        passenger.setYRot(normalizeAngle(gouram.getYRot() + rideBackPlayerYawOffset));
        passenger.setYHeadRot(passenger.getYRot());
        passenger.yHeadRotO = passenger.yHeadRot;

        if (isAccelerating) {
            rideBackDriveTime = Math.min(rideBackDriveTime + 1, GouramConfig.pillionMaxDriveTime);
            float driveRatio = (float) rideBackDriveTime / GouramConfig.pillionMaxDriveTime;

            double inertiaForward = rideBackInertiaX * rideBackCachedSin + rideBackInertiaZ * rideBackCachedCos;
            double inertiaSide = rideBackInertiaX * rideBackCachedCos - rideBackInertiaZ * rideBackCachedSin;

            rideBackSpeed += (float) inertiaForward * (0.2f + driveRatio * 0.2f);
            rideBackInertiaX = rideBackCachedCos * inertiaSide * 0.5f;
            rideBackInertiaZ = -rideBackCachedSin * inertiaSide * 0.5f;

            float accel = GouramConfig.pillionAcceleration + (GouramConfig.pillionMaxAcceleration - GouramConfig.pillionAcceleration) * driveRatio;
            rideBackSpeed += accel;
            rideBackSpeed = Math.min(rideBackSpeed, GouramConfig.pillionForwardSpeed);

            rideBackInertiaX *= 0.9;
            rideBackInertiaZ *= 0.9;
        } else {
            rideBackDriveTime = Math.max(0, rideBackDriveTime - 2);
            if (Math.abs(rideBackSpeed) > 0.01f) {
                float driveRatio = (float) rideBackDriveTime / GouramConfig.pillionMaxDriveTime;
                double inertiaFactor = 0.70 + driveRatio * 0.25;
                double inertiaAmount = rideBackSpeed * inertiaFactor;
                rideBackInertiaX += rideBackCachedSin * inertiaAmount;
                rideBackInertiaZ += rideBackCachedCos * inertiaAmount;
                rideBackSpeed = 0;
            }
            rideBackInertiaX *= GouramConfig.pillionInertiaFriction;
            rideBackInertiaZ *= GouramConfig.pillionInertiaFriction;
        }

        float totalSpeed = (float) Math.sqrt(
                rideBackSpeed * rideBackSpeed + rideBackInertiaX * rideBackInertiaX + rideBackInertiaZ * rideBackInertiaZ);
        float totalSpeedRatio = Mth.clamp(totalSpeed / GouramConfig.pillionForwardSpeed, 0.0f, 1.0f);

        updateLeanAmount(strafe, totalSpeedRatio);

        double finalVelX = rideBackInertiaX + rideBackCachedSin * rideBackSpeed;
        double finalVelZ = rideBackInertiaZ + rideBackCachedCos * rideBackSpeed;

        double spd = Math.sqrt(finalVelX * finalVelX + finalVelZ * finalVelZ);
        double limit = GouramConfig.pillionForwardSpeed * 1.2;
        if (spd > limit) {
            finalVelX = finalVelX / spd * limit;
            finalVelZ = finalVelZ / spd * limit;
        }

        float vertical = 0;
        if (passenger.getXRot() < -10) {
            vertical = GouramConfig.pillionAscendSpeed * (Math.abs(passenger.getXRot()) / 45f);
        } else if (passenger.getXRot() > 10) {
            vertical = GouramConfig.pillionDescendSpeed * (Math.abs(passenger.getXRot()) / 45f);
        }

        int groundY = gouram.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gouram.blockPosition()).getY();
        if (gouram.getY() < groundY + GouramConfig.grabMinGroundHeight) {
            vertical = Math.max(vertical, GouramConfig.grabLiftForce);
        }

        gouram.setDeltaMovement(finalVelX, vertical, finalVelZ);
        gouram.move(MoverType.SELF, gouram.getDeltaMovement());
    }

    private void updateLeanAmount(float strafe, float totalSpeedRatio) {
        float maxLean = 30.0f;
        float leanSmooth = 0.2f;
        float leanMultiplier = 0.3f + totalSpeedRatio * 0.7f;
        float targetLean = strafe * maxLean * leanMultiplier;
        leanAmount += (targetLean - leanAmount) * leanSmooth;
    }

    public void refreshRideBackYawCache() {
        float yawRad = (float) Math.toRadians(gouram.getYRot());
        rideBackCachedSin = -(float) Math.sin(yawRad);
        rideBackCachedCos = (float) Math.cos(yawRad);
    }

    private static float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }
}