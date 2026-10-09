package com.qianyunyi.rider.entity.vehicle.gouram;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 哥莱姆救援行为控制器
 * <p>
 * 负责处理哥莱姆的两种救援模式：
 * <ul>
 *   <li><b>救援冲刺（rescueRush）</b>：哥莱姆飞向主人并让其骑乘到背上</li>
 *   <li><b>救援撤离（rescueClear）</b>：哥莱姆抓住主人后飞向高空安全位置，途中对沿途怪物造成伤害和击退</li>
 * </ul>
 * 两种行为都会接管哥莱姆的移动控制，使其按预设轨迹飞行。
 */
public class GouramRescueBehavior {
    /** 所属的哥莱姆实体 */
    private final GouramEntity gouram;

    /** 是否正在进行救援冲刺 */
    boolean rescueRush = false;
    /** 是否正在进行救援撤离 */
    boolean rescueClear = false;
    /** 救援撤离的当前阶段（0=清场扫荡, 1=抓取主人, 2=飞向目标点） */
    int rescueClearPhase = 0;
    /** 救援撤离阶段2的计时器（超时保护） */
    int rescueClearTick = 0;
    /** 清场阶段的扫荡目标坐标 */
    double rescueClearSweepX;
    double rescueClearSweepY;
    double rescueClearSweepZ;
    /** 救援撤离的目标X坐标 */
    double rescueClearTargetX;
    /** 救援撤离的目标Y坐标 */
    double rescueClearTargetY;
    /** 救援撤离的目标Z坐标 */
    double rescueClearTargetZ;
    /** 平滑过渡的偏航角，用于实现流畅的转向动画 */
    float rescueSmoothedYaw = 0f;
    /** 偏航角是否已初始化（防止首次计算时跳变） */
    boolean rescueYawInitialized = false;
    /** 救援冲刺的当前阶段（0=飞到主人上方, 1=飞到主人前方远处, 2=接近并骑乘） */
    int rescueRushPhase = 0;
    /** 主人是否已骑乘到哥莱姆上 */
    boolean rescueMounted = false;

    public GouramRescueBehavior(GouramEntity gouram) {
        this.gouram = gouram;
    }

    /**
     * 判断当前是否有任何救援行为正在执行
     * @return 如果正在执行救援冲刺或救援撤离则返回true
     */
    public boolean isActive() {
        return rescueRush || rescueClear;
    }

    /**
     * 判断主人是否已通过救援行为骑乘到哥莱姆上
     * @return 已骑乘返回true
     */
    public boolean isRescueMounted() {
        return rescueMounted;
    }

    /**
     * 设置救援冲刺状态
     * @param rush 是否启用救援冲刺
     */
    public void setRescueRush(boolean rush) {
        this.rescueRush = rush;
        this.rescueRushPhase = 0;
        this.rescueMounted = false;
        gouram.setRescueActiveData(rush);
    }

    /**
     * 设置救援撤离状态
     * @param clear 是否启用救援撤离
     */
    public void setRescueClear(boolean clear) {
        this.rescueClear = clear;
        this.rescueClearPhase = 0;
        this.rescueClearTick = 0;
        this.rescueClearSweepX = 0;
        this.rescueClearSweepY = 0;
        this.rescueClearSweepZ = 0;
        gouram.setRescueActiveData(clear);
    }

    /**
     * 当乘客（主人）从哥莱姆上离开时调用
     * 清除所有救援状态（包括撤离中 Cancel 的情况）并同步数据到客户端
     */
    public void onPassengerRemoved() {
        if (rescueMounted) {
            rescueMounted = false;
        }
        if (rescueRush || rescueClear) {
            rescueRush = false;
            rescueClear = false;
            rescueClearPhase = 0;
            rescueClearTick = 0;
            rescueYawInitialized = false;
            gouram.setRescueActiveData(false);
        }
    }

    /**
     * 每tick调用，执行救援行为逻辑
     * 根据当前激活的救援模式调用对应的处理方法，并执行实体移动
     * <p>
     * 注意：gouram.move() 仅在服务端执行，客户端只做偏航/状态更新。
     * 如果客户端也执行 move()，会导致实体移动与服务器同步的位置冲突，
     * 每次服务端的同步包到来时实体"回弹"，骑乘相机随之前后抖动。
     */
    public void tick() {
        if (rescueRush) {
            doRescueRush();
            if (!gouram.level().isClientSide()) {
                gouram.move(MoverType.SELF, gouram.getDeltaMovement());
            }
        }
        if (rescueClear) {
            doRescueClear();
            if (!gouram.level().isClientSide()) {
                gouram.move(MoverType.SELF, gouram.getDeltaMovement());
            }
        }
    }

    /**
     * 判断是否应跳过默认的移动AI
     * 救援行为期间由本类接管移动控制，应跳过默认移动逻辑
     * @return 如果正在执行救援行为则返回true
     */
    public boolean shouldSkipDefaultTravel() {
        return rescueRush || rescueClear;
    }

    /**
     * 执行救援冲刺逻辑
     * <p>
     * 分为三个阶段：
     * <ol>
     *   <li>阶段0：哥莱姆快速飞到主人正上方约6格高度</li>
     *   <li>阶段1：哥莱姆飞到主人前方约12格的位置</li>
     *   <li>阶段2：哥莱姆减速接近主人，距离足够近时让主人骑乘</li>
     * </ol>
     * 整个过程使用平滑的偏航角过渡，避免转向生硬。
     */
    private void doRescueRush() {
        Player owner = gouram.getOwner();
        if (owner == null) {
            resetRescueRush();
            return;
        }

        // 根据主人朝向计算前方方向向量
        double facingRad = Math.toRadians(owner.getYRot());
        double facingX = -Math.sin(facingRad);
        double facingZ = Math.cos(facingRad);

        double targetX, targetY, targetZ;

        if (rescueRushPhase == 0) {
            // 阶段0：飞到主人正上方6格高度
            targetX = owner.getX();
            targetY = owner.getY() + 6;
            targetZ = owner.getZ();

            double distToTarget = Math.sqrt(
                    Math.pow(targetX - gouram.getX(), 2) +
                            Math.pow(targetY - gouram.getY(), 2) +
                            Math.pow(targetZ - gouram.getZ(), 2));
            if (distToTarget < 8 || gouram.getY() >= owner.getY() + 3) {
                rescueRushPhase = 1;
            }
        } else if (rescueRushPhase == 1) {
            // 阶段1：飞到主人前方12格、上方4格的位置
            targetX = owner.getX() - facingX * 12;
            targetY = owner.getY() + 4;
            targetZ = owner.getZ() - facingZ * 12;

            double distToTarget = Math.sqrt(
                    Math.pow(targetX - gouram.getX(), 2) +
                            Math.pow(targetY - gouram.getY(), 2) +
                            Math.pow(targetZ - gouram.getZ(), 2));
            if (distToTarget < 6) {
                rescueRushPhase = 2;
            }
        } else {
            // 阶段2：减速飞到主人前方3格、上方1.5格的位置，准备骑乘
            targetX = owner.getX() - facingX * 3;
            targetY = owner.getY() + 1.5;
            targetZ = owner.getZ() - facingZ * 3;
        }

        double dx = targetX - gouram.getX();
        double dy = targetY - gouram.getY();
        double dz = targetZ - gouram.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 距离足够近且处于阶段2时，让主人骑乘
        if (dist < GouramConfig.rescueCatchDistance && rescueRushPhase == 2) {
            rescueRush = false;
            rescueYawInitialized = false;
            rescueRushPhase = 0;
            // 已成功载上玩家，本次救援飞行结束，必须释放 RESCUE_ACTIVE，
            // 否则客户端会一直停留在救援视角而无法恢复到正常视角
            gouram.setRescueActiveData(false);
            gouram.setDeltaMovement(0, 0, 0);
            gouram.setYRot(owner.getYRot());
            gouram.yBodyRot = gouram.getYRot();
            if (gouram.requestState(GouramState.RIDE_BACK)) {
                gouram.getRideBehavior().initRideBack();
                owner.startRiding(gouram);
                rescueMounted = true;
            }
            return;
        }

        // 根据阶段调整移动速度
        double speed = GouramConfig.rescueRushSpeed;
        if (rescueRushPhase == 0) {
            speed *= 1.3; // 阶段0加速，快速到达主人上方
        } else if (rescueRushPhase == 2) {
            speed *= 0.8; // 阶段2减速，精准接近
        }

        double motionX = dx / dist * speed;
        double motionY = dy / dist * speed;
        double motionZ = dz / dist * speed;

        gouram.setDeltaMovement(motionX, motionY, motionZ);

        // 计算目标偏航角，不同阶段有不同的朝向策略
        float targetYaw;
        if (rescueRushPhase == 0) {
            // 阶段0：始终面向主人
            targetYaw = (float) Math.toDegrees(Math.atan2(-(owner.getX() - gouram.getX()), owner.getZ() - gouram.getZ()));
        } else if (rescueRushPhase == 1) {
            // 阶段1：部分面向主人，部分朝向主人前方，实现侧向飞行效果
            float facePlayerYaw = (float) Math.toDegrees(Math.atan2(-(owner.getX() - gouram.getX()), owner.getZ() - gouram.getZ()));
            float yawToOwner = normalizeAngle(owner.getYRot() - facePlayerYaw);
            targetYaw = normalizeAngle(facePlayerYaw + yawToOwner * 0.4f);
        } else {
            // 阶段2：朝向与主人一致
            targetYaw = owner.getYRot();
        }

        // 平滑偏航角过渡
        if (!rescueYawInitialized) {
            rescueSmoothedYaw = gouram.getYRot();
            rescueYawInitialized = true;
        }
        float yawDiff = normalizeAngle(targetYaw - rescueSmoothedYaw);
        float yawSpeed = rescueRushPhase == 0 ? 0.12f : 0.18f;
        rescueSmoothedYaw = normalizeAngle(rescueSmoothedYaw + yawDiff * yawSpeed);
        gouram.setYRot(rescueSmoothedYaw);
        gouram.yBodyRot = gouram.getYRot();
    }

    /**
     * 重置救援冲刺的所有状态
     */
    private void resetRescueRush() {
        rescueRush = false;
        rescueYawInitialized = false;
        rescueRushPhase = 0;
        rescueMounted = false;
        gouram.setRescueActiveData(false);
    }

    /**
     * 执行救援撤离逻辑
     * <p>
     * 分为三个阶段：
     * <ol>
     *   <li><b>阶段0 - 清场扫荡</b>：哥莱姆飞向主人周围的一个偏移位置（不直接飞向主人），
     *       沿途对怪物造成伤害和击退，确保清出一条安全通道</li>
     *   <li><b>阶段1 - 抓取主人</b>：哥莱姆从扫荡位置飞向主人，抓住主人（GRAB_HAND状态）</li>
     *   <li><b>阶段2 - 撤离飞行</b>：带着主人飞向预先计算好的较远安全位置，到达后结束撤离</li>
     * </ol>
     */
    private void doRescueClear() {
        Player owner = gouram.getOwner();
        if (owner == null) {
            rescueClear = false;
            rescueYawInitialized = false;
            gouram.setRescueActiveData(false);
            return;
        }

        if (rescueClearPhase == 0) {
            // 阶段0：清场扫荡 —— 飞向主人周围的一个偏移位置，不直接飞向主人
            if (rescueClearSweepX == 0 && rescueClearSweepY == 0 && rescueClearSweepZ == 0) {
                // 首次进入阶段0，计算扫荡目标位置
                findRescueClearSweepTarget(owner);
            }

            double dx = rescueClearSweepX - gouram.getX();
            double dy = rescueClearSweepY - gouram.getY();
            double dz = rescueClearSweepZ - gouram.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < 2.0) {
                // 到达扫荡位置，进入抓取阶段
                rescueClearPhase = 1;
                rescueYawInitialized = false;
                return;
            }

            // 以较快速度飞向扫荡目标
            double speed = GouramConfig.rescueRushSpeed * 1.2;
            gouram.setDeltaMovement(dx / dist * speed, dy / dist * speed, dz / dist * speed);

            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            targetYaw = normalizeAngle(targetYaw);
            if (!rescueYawInitialized) {
                rescueSmoothedYaw = targetYaw;
                rescueYawInitialized = true;
            }
            float yawDiff = normalizeAngle(targetYaw - rescueSmoothedYaw);
            rescueSmoothedYaw = normalizeAngle(rescueSmoothedYaw + yawDiff * 0.25f);
            gouram.setYRot(rescueSmoothedYaw);
            gouram.yBodyRot = gouram.getYRot();

            // 扫荡途中对周围怪物造成伤害和击退
            damageAndKnockbackEnemies();
        } else if (rescueClearPhase == 1) {
            // 阶段1：抓取主人 —— 从扫荡位置飞向主人
            double dx = owner.getX() - gouram.getX();
            double dy = owner.getY() - gouram.getY();
            double dz = owner.getZ() - gouram.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < GouramConfig.rescueCatchDistance) {
                // 距离足够近，抓住主人并寻找撤离目标位置
                gouram.setDeltaMovement(0, 0, 0);
                if (gouram.requestState(GouramState.GRAB_HAND)) {
                    owner.startRiding(gouram);
                    // 标记已载上玩家，供客户端识别乘坐状态
                    rescueMounted = true;
                    findRescueClearTarget(owner);
                    rescueClearPhase = 2;
                    rescueClearTick = 0;
                    rescueYawInitialized = false;
                }
                return;
            }

            // 飞向主人
            double speed = GouramConfig.rescueRushSpeed;
            gouram.setDeltaMovement(dx / dist * speed, dy / dist * speed, dz / dist * speed);

            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            targetYaw = normalizeAngle(targetYaw);
            if (!rescueYawInitialized) {
                rescueSmoothedYaw = targetYaw;
                rescueYawInitialized = true;
            }
            float yawDiff = normalizeAngle(targetYaw - rescueSmoothedYaw);
            rescueSmoothedYaw = normalizeAngle(rescueSmoothedYaw + yawDiff * 0.3f);
            gouram.setYRot(rescueSmoothedYaw);
            gouram.yBodyRot = gouram.getYRot();

            // 接近主人时继续对周围怪物造成伤害
            damageAndKnockbackEnemies();
        } else {
            // 阶段2：带着主人飞向目标安全位置
            rescueClearTick++;
            if (rescueClearTick > 200) {
                // 超时保护，强制结束撤离
                rescueClear = false;
                gouram.setRescueActiveData(false);
                return;
            }

            double dx = rescueClearTargetX - gouram.getX();
            double dy = rescueClearTargetY - gouram.getY();
            double dz = rescueClearTargetZ - gouram.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < 1.5) {
                // 到达目标位置，撤离完成
                rescueClear = false;
                gouram.setRescueActiveData(false);
                return;
            }

            // 以90%速度飞向目标（相较之前的80%略微提速，弥补更远距离的飞行时间）
            double speed = GouramConfig.rescueRushSpeed;
            gouram.setDeltaMovement(dx / dist * speed * 0.9, dy / dist * speed * 0.9, dz / dist * speed * 0.9);

            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            targetYaw = normalizeAngle(targetYaw);
            // 阶段2必须初始化偏航角为当前实体的实际偏航（保留阶段1的朝向），
            // 而不是直接跳变到targetYaw，否则180度的瞬间旋转会通过positionRider()
            // 中的Math.sin(-yaw)*forwardOffset将骑手从实体一侧瞬间拽到另一侧，造成剧烈抖动
            if (!rescueYawInitialized) {
                rescueSmoothedYaw = gouram.getYRot();
                rescueYawInitialized = true;
            }
            // 使用小的平滑系数（0.12），让偏航从"朝向玩家"缓慢过渡到"朝向撤离目标"，
            // 对应positionRider中骑手偏移量的渐变，消除相机抖动
            float yawDiff = normalizeAngle(targetYaw - rescueSmoothedYaw);
            rescueSmoothedYaw = normalizeAngle(rescueSmoothedYaw + yawDiff * 0.12f);
            gouram.setYRot(rescueSmoothedYaw);
            gouram.yBodyRot = gouram.getYRot();
        }
    }

    /**
     * 寻找救援撤离的清场扫荡目标位置
     * <p>
     * 在玩家前方（基于玩家朝向）计算一个偏移位置，让哥莱姆先飞到该位置，
     * 途中撞飞沿途的怪物，而不是直接飞向玩家。
     * 目标位置在玩家前方约8格、高度与玩家持平或略高。
     *
     * @param player 主人玩家
     */
    private void findRescueClearSweepTarget(Player player) {
        // 基于玩家朝向计算前方偏移
        double facingRad = Math.toRadians(player.getYRot());
        double facingX = -Math.sin(facingRad);
        double facingZ = Math.cos(facingRad);

        // 扫荡目标：玩家前方8格，高度与玩家持平（略高1格避免撞地）
        rescueClearSweepX = player.getX() - facingX * 8;
        rescueClearSweepY = player.getY() + 1.5;
        rescueClearSweepZ = player.getZ() - facingZ * 8;
    }

    /**
     * 寻找救援撤离的安全目标位置
     * <p>
     * 在玩家周围随机搜索一个较远且不阻挡实体的安全位置。
     * 最多尝试30次随机搜索，优先选择地面以上至少3格高度、且距离玩家至少25格的位置。
     * 高度控制在玩家上方12~24格（maxClimb~maxClimb*2），水平距离25~searchRadius格，
     * 确保撤离时能真正飞向远处高空。
     *
     * @param player 主人玩家
     */
    private void findRescueClearTarget(Player player) {
        int maxClimb = GouramConfig.rescueMaxClimb;
        int searchRadius = GouramConfig.rescueSearchRadius;
        ServerLevel level = (ServerLevel) gouram.level();

        // 默认目标：玩家上方较远处（maxClimb*2 + 10 确保足够高）
        double bestX = player.getX();
        double bestY = player.getY() + maxClimb * 2.0 + 10;
        double bestZ = player.getZ();
        boolean found = false;

        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            // 确保距离至少25格，最远searchRadius格
            double dist = Math.max(25, searchRadius * 0.5) + level.random.nextDouble() * (searchRadius * 0.5);
            double testX = player.getX() + Math.cos(angle) * dist;
            // 高度控制在玩家上方12~24格（maxClimb~maxClimb*2），确保飞得够高够远
            double testY = player.getY() + maxClimb + level.random.nextDouble() * maxClimb;
            double testZ = player.getZ() + Math.sin(angle) * dist;

            BlockPos testPos = new BlockPos((int) testX, (int) testY, (int) testZ);
            if (!level.getBlockState(testPos).isSolid() && !level.getBlockState(testPos.above()).isSolid()) {
                int surfaceY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, testPos).getY();
                if (testY > surfaceY + 3) {
                    bestX = testX;
                    bestY = testY;
                    bestZ = testZ;
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            // 后备方案：在玩家周围随机选择一个远处高空的位置
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = Math.max(25, searchRadius * 0.5) + level.random.nextDouble() * (searchRadius * 0.5);
            bestX = player.getX() + Math.cos(angle) * dist;
            bestY = player.getY() + maxClimb * 2.0 + 10;
            bestZ = player.getZ() + Math.sin(angle) * dist;
        }

        rescueClearTargetX = bestX;
        rescueClearTargetY = bestY;
        rescueClearTargetZ = bestZ;
    }

    /**
     * 对哥莱姆周围2.5格范围内的敌对怪物造成伤害和击退
     * <p>
     * 仅对Monster类型的实体生效，玩家和自身不受影响。
     * 伤害值和击退力度由GouramConfig配置。
     */
    private void damageAndKnockbackEnemies() {
        AABB checkArea = gouram.getBoundingBox().inflate(2.5);
        ServerLevel level = (ServerLevel) gouram.level();
        Player owner = gouram.getOwner();

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, checkArea,
                e -> e != gouram && e != owner && !(e instanceof Player) && e.isAlive());

        for (LivingEntity entity : entities) {
            if (entity instanceof Monster) {
                entity.hurt(gouram.damageSources().mobAttack(gouram), GouramConfig.rescueClearDamage);
                // 计算从哥莱姆指向怪物的击退方向
                Vec3 knockback = entity.position().subtract(gouram.position()).normalize().scale(GouramConfig.rescueKnockbackLevel);
                entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.x, 0.4, knockback.z));
                entity.hurtMarked = true;
            }
        }
    }

    /**
     * 将角度标准化到 [-180, 180] 范围内
     * @param angle 原始角度
     * @return 标准化后的角度
     */
    private static float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }
}