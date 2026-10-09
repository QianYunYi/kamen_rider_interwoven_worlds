package com.qianyunyi.rider.entity.vehicle.gouram;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * 哥莱姆自主防卫行为
 * <p>
 * 取代原来的巡逻行为，改为主动冲撞靠近玩家的敌对怪物。
 * 工作流程：
 * <ol>
 *   <li><b>扫描</b>：检测玩家周围20格范围内的敌对怪物，幻翼享有更高优先级</li>
 *   <li><b>冲撞</b>：高速冲向目标怪物，命中后造成伤害和击退</li>
 *   <li><b>返回</b>：冲撞完成后返回玩家上方安全高度</li>
 *   <li><b>循环</b>：如果怪物存活或其他怪物存在，继续冲撞</li>
 * </ol>
 * 对幻翼进行特殊优化：更远的检测范围、更高的优先级、预判拦截算法。
 */
public class GouramPatrolBehavior {
    private final GouramEntity gouram;

    /** 当前锁定的冲撞目标 */
    LivingEntity currentTarget = null;
    /** 防卫阶段：0=扫描目标, 1=冲撞中, 2=返回空中 */
    int chargePhase = 0;
    /** 当前阶段的计时器 */
    int chargeTick = 0;
    /** 冲撞冷却时间（tick），防止连续冲撞过于频繁 */
    int chargeCooldown = 0;
    /** 冲撞完成后返回的目标位置 */
    Vec3 returnPosition = null;
    /** 上次扫描的偏航角，用于无威胁时环绕飞行 */
    float hoverAngle = 0f;

    /** 地面掠过冷却（保留兼容旧接口） */
    int groundSkimCooldown = 0;

    public GouramPatrolBehavior(GouramEntity gouram) {
        this.gouram = gouram;
    }

    /**
     * 每tick更新冷却计时器
     */
    public void tickCooldowns() {
        if (chargeCooldown > 0) chargeCooldown--;
        if (groundSkimCooldown > 0) groundSkimCooldown--;
    }

    /**
     * 判断冲撞冷却是否激活（保留兼容旧接口）
     */
    public boolean isGroundSkimCooldownActive() {
        return chargeCooldown > 0;
    }

    /**
     * 判断巡逻行为是否正处于活跃控制状态（冲撞中或返回中）
     * 用于 {@code travel()} 判断是否需要跳过默认物理摩擦
     */
    public boolean isActivelyControlling() {
        return chargePhase == 1 || chargePhase == 2 || chargeCooldown > 0;
    }

    /**
     * 主行为入口（由GouramEntity在IDLE状态且导航空闲时调用）
     * 执行自主防卫逻辑：扫描威胁 → 冲撞 → 返回 → 循环
     * <p>
     * 注意：此方法只设置 {@code setDeltaMovement}，不再调用 {@code move()}，
     * 实际移动由 {@code travel()} 统一处理，避免双重移动和摩擦力干扰导致卡顿。
     */
    public void doIdlePatrol() {
        Player owner = gouram.getOwner();
        if (owner == null) return;

        if (chargeCooldown > 0) {
            // 冷却中，悬停在主人上方
            hoverAboveOwner(owner);
            return;
        }

        switch (chargePhase) {
            case 0: scanAndAcquireTarget(owner); break;
            case 1: doCharge(owner); break;
            case 2: doReturn(owner); break;
        }
    }

    /**
     * 地面掠过检测（保留兼容旧接口，不再使用）
     */
    public void checkGroundSkim() {
    }

    /**
     * 阶段0：扫描玩家周围的敌对怪物并锁定目标
     * <p>
     * 优先级规则：
     * <ul>
     *   <li>幻翼（Phantom）享有最高优先级，检测距离判定时给予70%折扣</li>
     *   <li>其他怪物按距离玩家最近优先</li>
     * </ul>
     * 如果没有威胁，哥莱姆环绕玩家飞行警戒。
     */
    private void scanAndAcquireTarget(Player owner) {
        AABB scanArea = owner.getBoundingBox().inflate(20);
        List<LivingEntity> threats = gouram.level().getEntitiesOfClass(LivingEntity.class, scanArea,
                e -> e instanceof Monster && e.isAlive() && e != gouram);

        if (threats.isEmpty()) {
            // 没有威胁，环绕玩家飞行警戒
            hoverAroundOwner(owner);
            return;
        }

        // 优先选择幻翼（距离判定时大幅打折），其次选择距离玩家最近的怪物
        LivingEntity bestTarget = threats.stream()
                .min(Comparator.comparingDouble(e -> {
                    double dist = e.distanceToSqr(owner);
                    if (e instanceof Phantom) {
                        dist *= 0.3; // 幻翼优先级大幅提升，等效于距离感知缩短到55%
                    }
                    return dist;
                }))
                .orElse(null);

        if (bestTarget != null) {
            currentTarget = bestTarget;
            chargePhase = 1;
            chargeTick = 0;
        }
    }

    /**
     * 无威胁时环绕主人飞行警戒
     * 在玩家上方约5格处，以8格半径缓慢环绕
     */
    private void hoverAroundOwner(Player owner) {
        hoverAngle += 0.03f;
        double radius = 8.0;
        double targetX = owner.getX() + Math.cos(hoverAngle) * radius;
        double targetY = owner.getY() + 5;
        double targetZ = owner.getZ() + Math.sin(hoverAngle) * radius;

        moveToward(targetX, targetY, targetZ, 0.3);
    }

    /**
     * 冷却中时悬停在主人正上方
     */
    private void hoverAboveOwner(Player owner) {
        double targetX = owner.getX();
        double targetY = owner.getY() + 5;
        double targetZ = owner.getZ();

        moveToward(targetX, targetY, targetZ, 0.4);
    }

    /**
     * 阶段1：冲撞目标怪物
     * <p>
     * 以高速冲向目标，命中后造成伤害和击退，然后进入返回阶段。
     * 对幻翼使用预判拦截算法：根据幻翼的当前速度预判其未来位置，提前飞到拦截点。
     */
    private void doCharge(Player owner) {
        // 目标已死亡或无效，返回空中
        if (currentTarget == null || !currentTarget.isAlive()) {
            transitionToReturn(owner);
            return;
        }

        chargeTick++;

        // 超时保护：冲撞超过50tick（2.5秒）未命中则放弃
        if (chargeTick > 50) {
            transitionToReturn(owner);
            chargeCooldown = 15;
            return;
        }

        // 计算冲撞目标位置
        double targetX, targetY, targetZ;
        boolean isPhantom = currentTarget instanceof Phantom;

        if (isPhantom) {
            // 幻翼特殊处理：预判其移动轨迹，飞到前方拦截
            Vec3 phantomMotion = currentTarget.getDeltaMovement();
            double leadTicks = 8; // 预判8tick后的位置
            targetX = currentTarget.getX() + phantomMotion.x * leadTicks;
            targetY = currentTarget.getY() + phantomMotion.y * leadTicks;
            targetZ = currentTarget.getZ() + phantomMotion.z * leadTicks;
            // 幻翼通常在高空，确保拦截点不低于地面
            if (targetY < owner.getY() + 2) {
                targetY = owner.getY() + 2;
            }
        } else {
            // 地面怪物：瞄准其身体中心
            targetX = currentTarget.getX();
            targetY = currentTarget.getY() + currentTarget.getBbHeight() * 0.5;
            targetZ = currentTarget.getZ();
        }

        double dx = targetX - gouram.getX();
        double dy = targetY - gouram.getY();
        double dz = targetZ - gouram.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 冲撞速度：幻翼更快以匹配其飞行速度
        double speed = isPhantom ? 2.2 : 1.8;

        if (dist < 2.5) {
            // 命中目标！造成伤害和击退
            float damage = isPhantom ? 12.0f : 8.0f;
            currentTarget.hurt(gouram.damageSources().mobAttack(gouram), damage);

            // 击退：从哥莱姆指向怪物方向
            Vec3 knockback = currentTarget.position().subtract(gouram.position()).normalize().scale(3.5);
            currentTarget.setDeltaMovement(currentTarget.getDeltaMovement().add(knockback.x, 0.5, knockback.z));
            currentTarget.hurtMarked = true;

            // 冲撞完成，返回空中
            transitionToReturn(owner);
            chargeCooldown = 15;
            return;
        }

        // 高速冲向目标（只设速度，由travel()统一移动，避免双重移动卡顿）
        gouram.setDeltaMovement(dx / dist * speed, dy / dist * speed, dz / dist * speed);

        // 面向目标
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        gouram.setYRot(targetYaw);
        gouram.yBodyRot = gouram.getYRot();
    }

    /**
     * 阶段2：冲撞完成后返回玩家上方安全位置
     * <p>
     * 返回位置为玩家正上方5格。到达后回到扫描阶段，如果怪物还活着会再次被锁定。
     */
    private void doReturn(Player owner) {
        chargeTick++;

        // 超时保护：返回超过60tick（3秒）强制回到扫描
        if (chargeTick > 60) {
            resetToScan();
            return;
        }

        if (returnPosition == null) {
            returnPosition = new Vec3(owner.getX(), owner.getY() + 5, owner.getZ());
        }

        double dx = returnPosition.x - gouram.getX();
        double dy = returnPosition.y - gouram.getY();
        double dz = returnPosition.z - gouram.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 2.0) {
            // 已返回空中，回到扫描阶段
            resetToScan();
            return;
        }

        // 中速返回（只设速度，由travel()统一移动）
        double speed = 1.0;
        gouram.setDeltaMovement(dx / dist * speed, dy / dist * speed, dz / dist * speed);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        gouram.setYRot(targetYaw);
        gouram.yBodyRot = gouram.getYRot();
    }

    /**
     * 通用移动方法：让哥莱姆向目标坐标移动
     * @param speed 移动速度
     */
    private void moveToward(double targetX, double targetY, double targetZ, double speed) {
        double dx = targetX - gouram.getX();
        double dy = targetY - gouram.getY();
        double dz = targetZ - gouram.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 1.0) {
            gouram.setDeltaMovement(0, 0, 0);
            return;
        }

        gouram.setDeltaMovement(dx / dist * speed, dy / dist * speed, dz / dist * speed);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        gouram.setYRot(targetYaw);
        gouram.yBodyRot = gouram.getYRot();
    }

    /**
     * 过渡到返回阶段
     */
    private void transitionToReturn(Player owner) {
        chargePhase = 2;
        chargeTick = 0;
        returnPosition = new Vec3(owner.getX(), owner.getY() + 5, owner.getZ());
    }

    /**
     * 重置到扫描阶段
     */
    private void resetToScan() {
        chargePhase = 0;
        chargeTick = 0;
        currentTarget = null;
        returnPosition = null;
    }
}