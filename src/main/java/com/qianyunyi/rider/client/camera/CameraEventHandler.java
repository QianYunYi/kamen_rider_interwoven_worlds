package com.qianyunyi.rider.client.camera;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.entity.GouramEntity;
import com.qianyunyi.rider.entity.vehicle.gouram.GouramState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 相机事件处理器 — 每帧将相机控制权交给 CameraController 和 ScreenShakeController
 */
@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, value = Dist.CLIENT)
public class CameraEventHandler {

    private static float smoothedRoll = 0f;         // 平滑后的滚转角（度）
    private static final float ROLL_SMOOTH = 0.15f; // 滚转平滑系数

    /**
     * 每帧更新相机状态
     * @param event 客户端 tick事件
     * @see TickEvent
     * @see CameraController
     * @see ScreenShakeController
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        ScreenShakeController.getInstance().tick();

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        CameraController controller = CameraController.getInstance();

        controller.setFovMultiplier(1.0f);

        // 如果超自然感官追踪已激活，跳过其他相机模式覆盖
        CameraController.CameraMode current = controller.getCurrentMode();
        CameraController.CameraMode target = controller.getTargetMode();
        boolean sensesTrackingActive = (current == CameraController.CameraMode.SUPERNATURAL_SENSES_TRACKING
                || target == CameraController.CameraMode.SUPERNATURAL_SENSES_TRACKING);
        if (sensesTrackingActive) {
            return;
        }

        // 空中冲刺：越肩视角 + FOV 效果（由玩家实际速度驱动）
        RiderPlayerData riderData = RiderPlayerVariablesProvider.get(player);
        if (riderData != null && riderData.isKickDashing()) {
            controller.requestMode(CameraController.CameraMode.KICK_DASH);
            double actualSpeed = player.getDeltaMovement().length() * 20.0;
            controller.setKickDashSpeed(actualSpeed);
            controller.setFovMultiplier(controller.getKickDashFovMultiplier());
            return;
        }

        

        // 退出冲刺模式
        CameraController.CameraMode currentMode = controller.getCurrentMode();
        CameraController.CameraMode targetMode = controller.getTargetMode();
        if ((currentMode == CameraController.CameraMode.KICK_DASH
                || targetMode == CameraController.CameraMode.KICK_DASH)) {
            controller.requestMode(CameraController.CameraMode.NORMAL);
            controller.setFovMultiplier(1.0f);
        }

        if (player.getVehicle() instanceof GouramEntity gouram) {
            if (gouram.getState() == GouramState.RIDE_BACK) {
                controller.setCurrentLean(gouram.getLeanAmount());
            }
            // 仅当救援仍在进行（主动带人飞行）时才强制救援视角；
            // 若已载上玩家且救援结束，则释放救援视角，恢复正常骑行/默认视角
            if (gouram.isRescueActive()) {
                controller.setTrackingTarget(null);
                controller.requestMode(CameraController.CameraMode.RESCUE_OVER_SHOULDER);
                double speed = gouram.getDeltaMovement().length();
                double speedRatio = Mth.clamp(speed / 0.8, 0.0, 1.0);
                float fov = 1.0f + (float) speedRatio * 0.35f;
                controller.setFovMultiplier(fov);
                float targetRoll = controller.getLeanForCameraRoll();
                smoothedRoll += (targetRoll - smoothedRoll) * ROLL_SMOOTH;
                return;
            }
        } else {
            controller.setCurrentLean(0f);
        }

        float targetRoll = controller.getLeanForCameraRoll();
        smoothedRoll += (targetRoll - smoothedRoll) * ROLL_SMOOTH;

        AABB searchArea = player.getBoundingBox().inflate(80);
        List<GouramEntity> nearby = player.level().getEntitiesOfClass(GouramEntity.class, searchArea,
                g -> g.isRescueActive() && g.getOwnerUUID() != null
                        && g.getOwnerUUID().equals(player.getUUID()));

        if (!nearby.isEmpty()) {
            GouramEntity rescueGouram = nearby.get(0);
            controller.setTrackingTarget(rescueGouram);
            controller.requestMode(CameraController.CameraMode.TRACKING_GOURAM);
            double speed = rescueGouram.getDeltaMovement().length();
            double speedFactor = Mth.clamp(speed * 0.4, 0.0, 0.15);
            double distance = player.distanceTo(rescueGouram);
            double proximityFactor = Mth.clamp(1.0 / Math.max(distance, 2.0), 0.0, 0.2);
            controller.setFovMultiplier(1.0f + (float) (speedFactor + proximityFactor));
            return;
        }

        CameraController.CameraMode currentMode2 = controller.getCurrentMode();
        CameraController.CameraMode targetMode2 = controller.getTargetMode();
        boolean inRescueMode = currentMode2 == CameraController.CameraMode.TRACKING_GOURAM
                || targetMode2 == CameraController.CameraMode.TRACKING_GOURAM
                || currentMode2 == CameraController.CameraMode.RESCUE_OVER_SHOULDER
                || targetMode2 == CameraController.CameraMode.RESCUE_OVER_SHOULDER;

        if (inRescueMode) {
            // 仅当附近已不存在仍在进行中的救援时，才自动恢复为正常视角。
            // 若哥莱姆仍处于救援飞行中（尚未抵达撤离点），应保持救援视角连续，
            // 否则每隔 2.5s 就会与上方的救援视角请求来回切换，造成视角周期性抽搐。
            boolean rescueStillActive = false;
            if (player.getVehicle() instanceof GouramEntity ridingGouram) {
                rescueStillActive = ridingGouram.isRescueActive();
            } else {
                AABB activeSearch = player.getBoundingBox().inflate(80);
                rescueStillActive = !player.level().getEntitiesOfClass(GouramEntity.class, activeSearch,
                        g -> g.isRescueActive() && g.getOwnerUUID() != null
                                && g.getOwnerUUID().equals(player.getUUID())).isEmpty();
            }
            if (!rescueStillActive) {
                long elapsed = System.currentTimeMillis() - controller.getModeSetTime();
                if (elapsed > 2500) {
                    controller.setTrackingTarget(null);
                    controller.requestMode(CameraController.CameraMode.NORMAL);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Camera camera = event.getCamera();
        float partialTick = (float) event.getPartialTick();

        CameraController.getInstance().applyCameraTransform(camera, partialTick);
        CameraController.getInstance().saveCameraPosition(camera.getPosition());

        event.setRoll(smoothedRoll);

        Vec3 shakeOffset = ScreenShakeController.getInstance().getShakeOffset(camera, partialTick);
        if (shakeOffset.lengthSqr() > 0.0001) {
            Vec3 currentPos = camera.getPosition();
            CameraAccessor.setPosition(camera,
                    currentPos.x + shakeOffset.x,
                    currentPos.y + shakeOffset.y,
                    currentPos.z + shakeOffset.z
            );
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        float multiplier = CameraController.getInstance().getFovMultiplier();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (mc.player.getVehicle() instanceof GouramEntity gouram && (gouram.isRescueActive() || gouram.isRescueMounted())) {
                double speed = gouram.getDeltaMovement().length();
                double speedRatio = Mth.clamp(speed / 0.8, 0.0, 1.0);
                multiplier = 1.0f + (float) speedRatio * 0.35f;
            }
        }

        if (Float.isNaN(multiplier) || Float.isInfinite(multiplier)) {
            return;
        }
        if (multiplier != 1.0f) {
            double newFov = event.getFOV() * multiplier;
            event.setFOV(newFov);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Roll is applied in onComputeCameraAngles via event.setRoll() before frustum culling.
        // GameRenderer's pPoseStack.mulPose(Axis.ZP.rotationDegrees(roll)) produces the same
        // matrix as the pre-multiply here: R_z(roll) * R_x(pitch) * R_y(yaw).
        // Applying roll here again would double it and cause frustum-mismatch.
    }
}