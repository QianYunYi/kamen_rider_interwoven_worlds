package com.qianyunyi.rider.client.camera;

import com.qianyunyi.rider.entity.GouramEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 自定义相机控制器 — 支持技能视角平滑过渡
 *
 * 相机模式：
 *   NORMAL              — 默认，使用玩家保存的相机类型
 *   OVER_SHOULDER       — 越肩视角：人物在屏幕左侧，准星方向不变
 *   RESCUE_OVER_SHOULDER — 救援越肩视角：镜头拉远，玩家看起来更小
 *   TRACKING_GOURAM     — 追踪哥莱姆：镜头持续给哥莱姆特写
 *   FIRST_PERSON_ZOOM   — 第一人称拉伸（预留，用于射击武器ADS）
 *
 * 过渡时长：500ms，使用 easeInOutCubic 缓动
 */
@OnlyIn(Dist.CLIENT)
public class CameraController {
    public enum CameraMode {
        NORMAL,
        OVER_SHOULDER,
        RESCUE_OVER_SHOULDER,
        TRACKING_GOURAM,
        KICK_DASH,
        FIRST_PERSON_ZOOM,
        SUPERNATURAL_SENSES_TRACKING
    }

    private static final CameraController INSTANCE = new CameraController();

    private CameraMode currentMode = CameraMode.NORMAL;
    private CameraMode targetMode = CameraMode.NORMAL;
    private CameraType savedCameraType = CameraType.FIRST_PERSON;
    private long transitionStartNano = 0;
    private long modeSetTime = 0;
    private Entity trackingTarget;
    private float fovMultiplier = 1.0f;
    private Vec3 smoothedCamPos = null;
    private Vec3 smoothedOffset = null;
    private float currentLean = 0f;
    private Vec3 transitionStartPos = null;
    private Vec3 savedCameraPosition = null;
    private static final double CAM_SMOOTH_FACTOR = 0.30;
    private static final long TRANSITION_DURATION_NANO = 500_000_000L;

    // 越肩视角偏移量（右、上、后）
    private static final double SHOULDER_OFFSET_RIGHT = 0.8;
    private static final double SHOULDER_OFFSET_UP    = 0.3;
    private static final double SHOULDER_OFFSET_BACK  = -2.2;

    // 救援越肩视角偏移量（右、上、后）— 镜头拉远，玩家看起来更小
    private static final double RESCUE_OFFSET_RIGHT = 0.5;
    private static final double RESCUE_OFFSET_UP    = 0.5;
    private static final double RESCUE_OFFSET_BACK  = -4.0;

    // 空中冲刺越肩视角偏移量（右、上、后）
    private static final double KICK_DASH_OFFSET_RIGHT = 0.8;
    private static final double KICK_DASH_OFFSET_UP    = 0.5;
    private static final double KICK_DASH_OFFSET_BACK  = -1.5;

    // 正常第三人称相机距离（用于退出过渡时回到正常第三人称位置）
    private static final double NORMAL_THIRD_PERSON_DISTANCE = -4.0;

    // 超自然感官·上帝视角追踪参数
    private static final double SENSES_SELF_VIEW_HEIGHT = 3.0;    // 自视模式：摄像机在玩家头顶的高度
    private static final double SENSES_CAMERA_HEIGHT = 3.0;       // 追踪模式：摄像机在目标头上的高度
    private static final double SENSES_CAMERA_TILT_ANGLE = 55.0;   // 俯视角角度（度）

    // Rider Kick 实际速度驱动参数（由 CameraEventHandler 每帧从玩家速度同步）
    private double currentDashSpeed = 0.0;
    private static final double KICK_DASH_MAX_REF_SPEED = 60.0;
    private static final double KICK_DASH_SPEED_BACK_PULL = 2.5;
    private static final float KICK_DASH_FOV_STRETCH = 0.35f;

    // 超自然感官·平滑旋转跟踪
    private float sensesSmoothYaw = 0f;
    private float sensesSmoothPitch = 0f;
    private boolean sensesSmoothInitialized = false;
    private static final double SENSES_ROTATION_SMOOTH = 0.06;  // 旋转平滑系数（越小越慢）

    private CameraController() {}

    public static CameraController getInstance() {
        return INSTANCE;
    }

    public void setTrackingTarget(Entity target) {
        this.trackingTarget = target;
    }

    public Entity getTrackingTarget() {
        return trackingTarget;
    }

    /** 请求切换相机模式 */
    public void requestMode(CameraMode mode) {
        if (this.targetMode == mode) return;

        boolean wasNormal = (currentMode == CameraMode.NORMAL && targetMode == CameraMode.NORMAL);
        CameraMode oldTargetMode = this.targetMode;

        this.targetMode = mode;
        this.transitionStartNano = System.nanoTime();
        this.modeSetTime = System.nanoTime();

        this.transitionStartPos = this.savedCameraPosition;
        this.smoothedOffset = null;

        if (mode == CameraMode.SUPERNATURAL_SENSES_TRACKING) {
            sensesSmoothInitialized = false;
            // 不重置 smoothedCamPos，让过渡更平滑
        }

        if (mode == CameraMode.OVER_SHOULDER || mode == CameraMode.RESCUE_OVER_SHOULDER || mode == CameraMode.TRACKING_GOURAM || mode == CameraMode.KICK_DASH || mode == CameraMode.SUPERNATURAL_SENSES_TRACKING) {
            Minecraft mc = Minecraft.getInstance();
            if (wasNormal) {
                savedCameraType = mc.options.getCameraType();
            }
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        if (mode == CameraMode.NORMAL && (oldTargetMode == CameraMode.KICK_DASH || this.currentMode == CameraMode.KICK_DASH)) {
            this.currentDashSpeed = 0.0;
        }
    }

    /** 每帧在 ViewportEvent.ComputeCameraAngles 中调用，应用相机偏移 */
    public void applyCameraTransform(Camera camera, float partialTick) {
		if (currentMode == CameraMode.NORMAL && targetMode == CameraMode.NORMAL) return;

		Entity entity = camera.getEntity();
		if (entity == null) return;

		long now = System.nanoTime();
		float elapsed = (now - transitionStartNano) / (float) TRANSITION_DURATION_NANO;
		float t;

		// 以目标模式为准，避免过渡期间“当前模式 + 目标模式”两两叠加，
		// 导致旧模式（如追踪的摇摆相机）覆盖新模式，造成视角抽搐
		boolean isRescueOverShoulder = targetMode == CameraMode.RESCUE_OVER_SHOULDER;
		boolean isTracking = targetMode == CameraMode.TRACKING_GOURAM;
		boolean isKickDash = targetMode == CameraMode.KICK_DASH
				|| currentMode == CameraMode.KICK_DASH;
		boolean isSensesTracking = targetMode == CameraMode.SUPERNATURAL_SENSES_TRACKING
				|| currentMode == CameraMode.SUPERNATURAL_SENSES_TRACKING;

		if (targetMode == CameraMode.OVER_SHOULDER || targetMode == CameraMode.RESCUE_OVER_SHOULDER
				|| targetMode == CameraMode.TRACKING_GOURAM || targetMode == CameraMode.KICK_DASH
				|| targetMode == CameraMode.SUPERNATURAL_SENSES_TRACKING) {
			t = Mth.clamp(elapsed, 0f, 1f);
			if (t >= 1f) currentMode = targetMode;
		} else {
			t = 1f - Mth.clamp(elapsed, 0f, 1f);
			if (elapsed >= 1f) {
				currentMode = CameraMode.NORMAL;
				trackingTarget = null;
				smoothedCamPos = null;
				smoothedOffset = null;
				fovMultiplier = 1.0f;
				Minecraft.getInstance().options.setCameraType(savedCameraType);
				return;
			}
		}

		t = easeInOutCubic(t);

		if (isTracking) {
			if (trackingTarget == null || !trackingTarget.isAlive()) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.level != null && mc.player != null) {
					AABB searchArea = mc.player.getBoundingBox().inflate(80);
					List<GouramEntity> nearby = mc.level.getEntitiesOfClass(GouramEntity.class, searchArea,
							g -> g.isRescueActive() && g.getOwnerUUID() != null
									&& g.getOwnerUUID().equals(mc.player.getUUID()));
					if (!nearby.isEmpty()) {
						trackingTarget = nearby.get(0);
					}
				}
			}
			if (trackingTarget != null && trackingTarget.isAlive()) {
				applyTrackingTransform(camera, entity, trackingTarget, t);
				return;
			}
		}

		if (isRescueOverShoulder && entity.getVehicle() instanceof GouramEntity gouram) {
			applyRescueOverShoulderTransform(camera, entity, gouram, partialTick, t);
			return;
		}

		if (isKickDash) {
			applyKickDashTransform(camera, entity, partialTick, t);
			return;
		}

		if (isSensesTracking) {
			applySupernaturalSensesTransform(camera, entity, partialTick, t);
			return;
		}

		Vec3 lookDir = entity.getLookAngle();
		double yawRad = Math.toRadians(entity.getYRot());
		Vec3 rightDir = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
		Vec3 eyePos = entity.getEyePosition(partialTick);

		double offsetRight = isRescueOverShoulder ? RESCUE_OFFSET_RIGHT : SHOULDER_OFFSET_RIGHT;
		double offsetUp = isRescueOverShoulder ? RESCUE_OFFSET_UP : SHOULDER_OFFSET_UP;
		double offsetBack = isRescueOverShoulder ? RESCUE_OFFSET_BACK : SHOULDER_OFFSET_BACK;

		Vec3 shoulderPos = eyePos
				.add(rightDir.scale(offsetRight))
				.add(0, offsetUp, 0)
				.add(lookDir.scale(offsetBack));

		Vec3 startPos = eyePos.add(getModeOffset(currentMode, lookDir, rightDir));

		Vec3 interpolatedPos;
		if (targetMode == CameraMode.NORMAL) {
			Vec3 returnTarget;
			if (savedCameraType == CameraType.FIRST_PERSON) {
				returnTarget = eyePos;
			} else {
				returnTarget = eyePos.add(lookDir.scale(NORMAL_THIRD_PERSON_DISTANCE));
			}
			interpolatedPos = startPos.add(returnTarget.subtract(startPos).scale(1f - t));
		} else {
			interpolatedPos = startPos.add(shoulderPos.subtract(startPos).scale(t));
		}
		CameraAccessor.setPosition(camera, interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);
	}

	private void applyKickDashTransform(Camera camera, Entity player, float partialTick, float t) {
		Vec3 lookDir = player.getLookAngle();
		double yawRad = Math.toRadians(player.getYRot());
		Vec3 rightDir = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
		Vec3 eyePos = player.getEyePosition(partialTick);

		boolean isTransitioning = (currentMode != targetMode);
		boolean isExiting = (targetMode == CameraMode.NORMAL);

		float speedFactor = getKickDashSpeedFactor();

		Vec3 targetOffset = rightDir.scale(KICK_DASH_OFFSET_RIGHT)
				.add(0, KICK_DASH_OFFSET_UP, 0)
				.add(lookDir.scale(KICK_DASH_OFFSET_BACK - speedFactor * KICK_DASH_SPEED_BACK_PULL));

		Vec3 currentOffset;
		if (isTransitioning) {
			Vec3 startOffset;
			Vec3 endOffset;
			if (isExiting) {
				startOffset = targetOffset;
				endOffset = (savedCameraType == CameraType.FIRST_PERSON)
						? Vec3.ZERO
						: lookDir.scale(NORMAL_THIRD_PERSON_DISTANCE);
				currentOffset = startOffset.add(endOffset.subtract(startOffset).scale(1f - t));
				smoothedOffset = null;
			} else {
				startOffset = getModeOffset(currentMode, lookDir, rightDir);
				endOffset = targetOffset;
				currentOffset = startOffset.add(endOffset.subtract(startOffset).scale(t));
				smoothedOffset = targetOffset;
			}
		} else {
			if (smoothedOffset == null) {
				smoothedOffset = targetOffset;
			} else {
				double s = CAM_SMOOTH_FACTOR;
				smoothedOffset = new Vec3(
						smoothedOffset.x + (targetOffset.x - smoothedOffset.x) * s,
						smoothedOffset.y + (targetOffset.y - smoothedOffset.y) * s,
						smoothedOffset.z + (targetOffset.z - smoothedOffset.z) * s
				);
			}
			currentOffset = smoothedOffset;
		}

		if (Double.isNaN(currentOffset.x) || Double.isNaN(currentOffset.y) || Double.isNaN(currentOffset.z)
				|| Double.isInfinite(currentOffset.x) || Double.isInfinite(currentOffset.y) || Double.isInfinite(currentOffset.z)) {
			smoothedOffset = null;
			fovMultiplier = 1.0f;
			return;
		}

		if (isExiting) {
			this.fovMultiplier = 1.0f + speedFactor * KICK_DASH_FOV_STRETCH * t;
		} else {
			this.fovMultiplier = 1.0f + speedFactor * KICK_DASH_FOV_STRETCH;
		}

		Vec3 cameraPos = eyePos.add(currentOffset);
		CameraAccessor.setPosition(camera, cameraPos.x, cameraPos.y, cameraPos.z);
	}

	private Vec3 getModeOffset(CameraMode mode, Vec3 lookDir, Vec3 rightDir) {
		if (mode == CameraMode.OVER_SHOULDER) {
			return rightDir.scale(SHOULDER_OFFSET_RIGHT)
					.add(0, SHOULDER_OFFSET_UP, 0)
					.add(lookDir.scale(SHOULDER_OFFSET_BACK));
		}
		if (mode == CameraMode.RESCUE_OVER_SHOULDER) {
			return rightDir.scale(RESCUE_OFFSET_RIGHT)
					.add(0, RESCUE_OFFSET_UP, 0)
					.add(lookDir.scale(RESCUE_OFFSET_BACK));
		}
		if (savedCameraType == CameraType.FIRST_PERSON) {
			return Vec3.ZERO;
		}
		return lookDir.scale(NORMAL_THIRD_PERSON_DISTANCE);
	}

	private float getKickDashSpeedFactor() {
		return (float) Mth.clamp(currentDashSpeed / KICK_DASH_MAX_REF_SPEED, 0.0, 1.0);
	}

	public float getKickDashFovMultiplier() {
		float speedFactor = getKickDashSpeedFactor();
		return 1.0f + speedFactor * KICK_DASH_FOV_STRETCH;
	}

	public void setKickDashSpeed(double speed) {
		this.currentDashSpeed = speed;
	}

	public void saveCameraPosition(Vec3 pos) {
		this.savedCameraPosition = new Vec3(pos.x, pos.y, pos.z);
	}

	private void applyRescueOverShoulderTransform(Camera camera, Entity player, GouramEntity gouram, float partialTick, float t) {
        float playerYaw = player.getViewYRot(partialTick);
        float playerPitch = player.getViewXRot(partialTick);
        float lean = gouram.getLeanAmount();

		float yawRad = (float) Math.toRadians(playerYaw);
		float pitchRad = (float) Math.toRadians(playerPitch);
		float leanRad = (float) Math.toRadians(lean);

		float cosYaw = Mth.cos(yawRad);
		float sinYaw = Mth.sin(yawRad);
		float cosPitch = Mth.cos(pitchRad);
		float sinPitch = Mth.sin(pitchRad);
		float cosLean = Mth.cos(leanRad);
		float sinLean = Mth.sin(leanRad);

		Vec3 forward = new Vec3(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch).normalize();
		Vec3 baseRight = new Vec3(cosYaw, 0, sinYaw).normalize();
		Vec3 baseUp = forward.cross(baseRight).normalize();

		Vec3 right = baseRight.scale(cosLean).add(baseUp.scale(sinLean));
		Vec3 up = baseUp.scale(cosLean).subtract(baseRight.scale(sinLean));

		double speed = gouram.getDeltaMovement().length();
		double speedRatio = Mth.clamp(speed / 0.8, 0.0, 1.0);
		this.fovMultiplier = 1.0f + (float) speedRatio * 0.35f;

		double r = RESCUE_OFFSET_RIGHT;
		double u = RESCUE_OFFSET_UP;
		double b = RESCUE_OFFSET_BACK - speedRatio * 1.5;

		Vec3 eyePos = player.getEyePosition(partialTick);
		Vec3 targetOffset = right.scale(r).add(up.scale(u)).add(forward.scale(b));

		boolean isTransitioning = (currentMode != targetMode);

		Vec3 currentOffset;
		if (isTransitioning) {
			Vec3 startOffset = getModeOffset(currentMode, forward, right);

			Vec3 endOffset;
			float blend;
			if (targetMode == CameraMode.NORMAL) {
				endOffset = (savedCameraType == CameraType.FIRST_PERSON)
						? Vec3.ZERO
						: forward.scale(NORMAL_THIRD_PERSON_DISTANCE);
				blend = 1f - t;
			} else {
				endOffset = targetOffset;
				blend = t;
			}
			currentOffset = startOffset.add(endOffset.subtract(startOffset).scale(blend));
			smoothedOffset = targetOffset;
		} else {
			if (smoothedOffset == null) {
				smoothedOffset = targetOffset;
			} else {
				double s = CAM_SMOOTH_FACTOR;
				smoothedOffset = new Vec3(
						smoothedOffset.x + (targetOffset.x - smoothedOffset.x) * s,
						smoothedOffset.y + (targetOffset.y - smoothedOffset.y) * s,
						smoothedOffset.z + (targetOffset.z - smoothedOffset.z) * s
				);
			}
			currentOffset = smoothedOffset;
		}

		if (Double.isNaN(currentOffset.x) || Double.isNaN(currentOffset.y) || Double.isNaN(currentOffset.z)
				|| Double.isInfinite(currentOffset.x) || Double.isInfinite(currentOffset.y) || Double.isInfinite(currentOffset.z)) {
			smoothedOffset = null;
			fovMultiplier = 1.0f;
			return;
		}

		Vec3 cameraPos = eyePos.add(currentOffset);
		CameraAccessor.setPosition(camera, cameraPos.x, cameraPos.y, cameraPos.z);
	}

	public float getFovMultiplier() {
		return fovMultiplier;
	}

	public void setFovMultiplier(float fovMultiplier) {
		this.fovMultiplier = fovMultiplier;
	}

	public float getLeanForCameraRoll() {
		return currentLean;
	}

	public void setCurrentLean(float lean) {
		this.currentLean = lean;
	}

    private void applyTrackingTransform(Camera camera, Entity player, Entity target, float t) {
        Vec3 targetPos = target.getEyePosition(1.0f);
        Vec3 playerPos = player.getEyePosition(1.0f);

        Vec3 playerToTarget = targetPos.subtract(playerPos);
        double distance = playerToTarget.length();
        if (distance < 0.01) return;

        Vec3 dir = playerToTarget.normalize();
        Vec3 sideDir = new Vec3(-Math.cos(Math.toRadians(player.getYRot())), 0, -Math.sin(Math.toRadians(player.getYRot())));

        double speed = target.getDeltaMovement().length();
        double time = System.nanoTime() * 1e-9;

        double sway = Math.sin(time * 1.5) * 0.5 + Math.cos(time * 2.1) * 0.25;
        double heightSway = Math.cos(time * 1.3) * 0.3;

        double sideDist = 3.0 + sway;
        double heightOffset = 2.0 + heightSway;

        double camDistance = Mth.clamp(distance * 0.3, 2.0, 8.0);
        Vec3 targetOffset = dir.scale(-camDistance)
                .add(sideDir.scale(sideDist))
                .add(0, heightOffset, 0);

        if (smoothedOffset == null) {
            smoothedOffset = targetOffset;
        } else {
            double smooth = 0.12;
            smoothedOffset = new Vec3(
                    smoothedOffset.x + (targetOffset.x - smoothedOffset.x) * smooth,
                    smoothedOffset.y + (targetOffset.y - smoothedOffset.y) * smooth,
                    smoothedOffset.z + (targetOffset.z - smoothedOffset.z) * smooth
            );
        }

        if (Double.isNaN(smoothedOffset.x) || Double.isNaN(smoothedOffset.y) || Double.isNaN(smoothedOffset.z)
                || Double.isInfinite(smoothedOffset.x) || Double.isInfinite(smoothedOffset.y) || Double.isInfinite(smoothedOffset.z)) {
            smoothedOffset = null;
            fovMultiplier = 1.0f;
            return;
        }

        Vec3 interpolatedOffset = smoothedOffset.scale(t);
        Vec3 cameraPos = playerPos.add(interpolatedOffset);

        CameraAccessor.setPosition(camera, cameraPos.x, cameraPos.y, cameraPos.z);

        double speedFactor = Mth.clamp(speed * 0.4, 0.0, 0.15);
        double proximityFactor = Mth.clamp(1.0 / Math.max(distance, 2.0), 0.0, 0.2);
        fovMultiplier = 1.0f + (float)(speedFactor + proximityFactor);
    }

    /**
     * 超自然感官·上帝视角追踪变换
     *
     * 两种模式：
     *   1. 自视模式（trackingTarget == null）：摄像机在玩家头顶 1.8 格，俯视玩家自身
     *   2. 追踪模式（trackingTarget != null）：摄像机在目标上方高处，俯视目标
     *
     * 自动检测上方方块阻挡并降低摄像机高度。
     */
    private void applySupernaturalSensesTransform(Camera camera, Entity player, float partialTick, float t) {
        Vec3 playerEyePos = player.getEyePosition(partialTick);
        Vec3 playerFeetPos = player.position();

        // 判断当前模式：自视(无目标) 还是 追踪(有实体目标)
        boolean isEntityTracking = trackingTarget != null && trackingTarget.isAlive();

        Vec3 targetPos;          // 摄像机注视的目标点
        Vec3 targetCamPos;       // 摄像机目标位置

        if (isEntityTracking) {
            // === 追踪模式：摄像机在目标上方高处俯视目标 ===
            Vec3 targetFeetPos = trackingTarget.position();
            double safeHeight = getClearHeightAbove(trackingTarget, targetFeetPos);
            targetPos = trackingTarget.getBoundingBox().getCenter();
            targetCamPos = new Vec3(targetPos.x, targetFeetPos.y + safeHeight, targetPos.z);
        } else {
            // === 自视模式：摄像机在玩家头顶 1.8 格，俯视玩家 ===
            double safeHeight = Math.min(SENSES_SELF_VIEW_HEIGHT, getClearHeightAbove(player, playerFeetPos));
            targetPos = playerFeetPos.add(0, 0.5, 0);  // 玩家躯干中心
            targetCamPos = new Vec3(playerEyePos.x, playerFeetPos.y + safeHeight, playerEyePos.z);
        }

        // 计算摄像机实际位置（入场过渡 + 平稳期平滑跟随）
        Vec3 currentCamPos;
        if (transitionStartPos != null && t < 1.0f) {
            // 入场过渡：从当前视角位置平滑上升到上帝视角（使用 easeInOutCubic）
            double et = easeInOutCubic(t);
            currentCamPos = new Vec3(
                    transitionStartPos.x + (targetCamPos.x - transitionStartPos.x) * et,
                    transitionStartPos.y + (targetCamPos.y - transitionStartPos.y) * et,
                    transitionStartPos.z + (targetCamPos.z - transitionStartPos.z) * et
            );
            sensesSmoothInitialized = false;
        } else {
            // 平稳期：平滑跟随基准实体移动
            if (smoothedCamPos == null) {
                smoothedCamPos = targetCamPos;
                sensesSmoothInitialized = false;
            }
            double smooth = 0.18;
            smoothedCamPos = new Vec3(
                    smoothedCamPos.x + (targetCamPos.x - smoothedCamPos.x) * smooth,
                    smoothedCamPos.y + (targetCamPos.y - smoothedCamPos.y) * smooth,
                    smoothedCamPos.z + (targetCamPos.z - smoothedCamPos.z) * smooth
            );
            currentCamPos = smoothedCamPos;
        }

        // 计算从当前摄像机位置指向目标的方向
        Vec3 lookDir = targetPos.subtract(currentCamPos).normalize();
        float targetYaw = (float) Math.toDegrees(Math.atan2(-lookDir.x, lookDir.z));
        float targetPitch = Mth.clamp((float) Math.toDegrees(-Math.asin(lookDir.y)), -89.9f, 89.9f);

        // 初始化平滑旋转
        if (!sensesSmoothInitialized) {
            sensesSmoothYaw = camera.getYRot();
            sensesSmoothPitch = camera.getXRot();
            sensesSmoothInitialized = true;
        }

        // 平滑旋转：自视↔追踪切换 或 目标切换时平滑过渡
        float yawDiff = normalizeAngle(targetYaw - sensesSmoothYaw);
        float pitchDiff = targetPitch - sensesSmoothPitch;

        float rotSpeed = 0.08f;
        float absYaw = Math.abs(yawDiff);
        if (absYaw > 30f) {
            rotSpeed = Math.min(0.15f, 0.08f + absYaw * 0.0004f);
        }

        sensesSmoothYaw += yawDiff * rotSpeed;
        sensesSmoothPitch += pitchDiff * 0.08f;

        // 应用位置和旋转
        CameraAccessor.setPosition(camera, currentCamPos.x, currentCamPos.y, currentCamPos.z);
        CameraAccessor.setRotation(camera, sensesSmoothYaw, sensesSmoothPitch);
    }

    /**
     * 检测玩家上方可用空间，找到最安全的摄像机高度。
     * 使用射线检测从玩家头部向上扫描，遇到方块阻挡时降低高度。
     */
    private double getClearHeightAbove(Entity entity, Vec3 feetPos) {
        Level level = entity.level();
        if (level == null) return SENSES_CAMERA_HEIGHT;

        double maxCheck = SENSES_CAMERA_HEIGHT + 2;

        // 从头部向上射线检测
        Vec3 start = new Vec3(feetPos.x, feetPos.y + 1.0, feetPos.z);
        Vec3 end = start.add(0, maxCheck, 0);

        ClipContext ctx = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
        BlockHitResult result = level.clip(ctx);

        if (result.getType() == HitResult.Type.MISS) {
            return SENSES_CAMERA_HEIGHT; // 无阻挡
        }

        // 有阻挡：摄像机放在阻挡下方 0.5 格处，最低不低于 3 格
        double hitY = result.getLocation().y;
        double safeY = hitY - feetPos.y - 0.5;
        return Math.max(3.0, safeY);
    }

    /** 将角度规范到 [-180, 180] 范围 */
    private static float normalizeAngle(float angle) {
        while (angle > 180f) angle -= 360f;
        while (angle < -180f) angle += 360f;
        return angle;
    }

    /** easeInOutCubic 缓动函数 */
    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3f) / 2f;
    }

    public boolean isActive() {
        return currentMode != CameraMode.NORMAL || targetMode != CameraMode.NORMAL;
    }

    public CameraMode getCurrentMode() {
        return currentMode;
    }

    public CameraMode getTargetMode() {
        return targetMode;
    }

    public long getModeSetTime() {
        return modeSetTime;
    }

    public void forceReset() {
        this.currentMode = CameraMode.NORMAL;
        this.targetMode = CameraMode.NORMAL;
        this.trackingTarget = null;
        this.smoothedCamPos = null;
        this.smoothedOffset = null;
        this.transitionStartPos = null;
        this.savedCameraPosition = null;
        this.fovMultiplier = 1.0f;
        this.sensesSmoothInitialized = false;
        if (Minecraft.getInstance().options != null) {
            Minecraft.getInstance().options.setCameraType(savedCameraType);
        }
    }
}