package com.qianyunyi.rider.util.riderKickUtil;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.SyncRiderDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 骑士踢/必杀位移工具
 * 仅服务端调用
 */
public final class KickMotionUtil {

    /**
     * 让玩家沿当前视线方向冲刺，直到落地或达到最大距离
     * @param player       目标玩家
     * @param baseSpeed    基础速度（方块/秒）
     * @param maxSpeed     最大速度（方块/秒）
     * @param acceleration 每Tick加速度（方块/秒²）
     * @param maxDistance  最大飞行距离（方块）
     * @param onEnd        冲刺结束时回调
     */
    public static void dash(ServerPlayer player, double baseSpeed, double maxSpeed,
                            double acceleration, double maxDistance, Runnable onEnd) {
        // 注册 Tick 任务
        new KickMotionTask(player, baseSpeed, maxSpeed, acceleration, maxDistance, onEnd).start();
    }

    //解除加速度
    public static void stopMotion(ServerPlayer sp) {
        sp.setDeltaMovement(Vec3.ZERO);
    }

    /* ---------- 内部 Tick 任务 ---------- */
    private static class KickMotionTask {
        private final ServerPlayer player;
        private final double baseSpeed;
        private final double maxSpeed;
        private final double acceleration;
        private final double maxDistance;
        private final Runnable onEnd;
        private boolean isActive = true;
        private int ticksInAir = 0;
        private Vec3 startPos;
        private Vec3 lastPosition;

        KickMotionTask(ServerPlayer player, double baseSpeed, double maxSpeed,
                       double acceleration, double maxDistance, Runnable onEnd) {
            this.player = player;
            this.baseSpeed = baseSpeed;
            this.maxSpeed = maxSpeed;
            this.acceleration = acceleration;
            this.maxDistance = maxDistance;
            this.onEnd = onEnd;
            this.startPos = player.position();
            this.lastPosition = player.position();
        }

        void start() {
            // 初始速度
            updateVelocity();
            player.hurtMarked = true; // 立即同步
            // 标记玩家正在冲刺（用于客户端视角旋转）
            RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
            if (data != null) {
                data.setKickDashing(true);
                // 立即同步到客户端，避免周期性同步延迟导致状态丢失
                NetworkHandler.sendToPlayer(SyncRiderDataPacket.core(data, player.getUUID()), player);
            }
            // 使用 Forge 事件总线每 Tick 更新
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void onServerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent e) {
            if (!isActive) return;
            if (e.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
            if (e.player != player) return;

            // 检查玩家是否落地
            if (player.onGround()) {
                endMotion();
                return;
            }

            // 增加空中时间计数
            ticksInAir++;

            // 检查是否达到最大距离
            double distanceTraveled = player.position().distanceTo(startPos);
            if (distanceTraveled >= maxDistance) {
                endMotion();
                return;
            }

            // 更新速度
            updateVelocity();
        }

        private void updateVelocity() {
            // 获取当前视线方向
            Vec3 look = player.getLookAngle().normalize();

            // 计算当前速度（基于空中时间）
            double currentSpeed = baseSpeed + (acceleration * ticksInAir / 20.0);
            currentSpeed = Math.min(currentSpeed, maxSpeed);

            // 计算每Tick水平速度（沿视线方向）
            Vec3 horizontalVelocity = look.scale(currentSpeed / 20.0);

            // 距离衰减 - 平方根曲线，让减速更平缓自然
            double distanceTraveled = player.position().distanceTo(startPos);
            double linearFactor = Math.max(0, 1.0 - (distanceTraveled / maxDistance));
            double distanceFactor = Math.sqrt(linearFactor);
            horizontalVelocity = horizontalVelocity.scale(distanceFactor);

            // 重力不受距离衰减影响，始终独立生效
            double gravity = -0.04;
            Vec3 velocity = horizontalVelocity.add(0, gravity, 0);

            // 阻止玩家WASD输入覆盖方向
            player.xxa = 0;
            player.zza = 0;

            // 设置玩家速度
            player.setDeltaMovement(velocity);
            player.hurtMarked = true;

            // 记录位置用于下一帧计算
            lastPosition = player.position();
        }

        private void endMotion() {
            // 停止运动
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;

            // 清除冲刺标记
            RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
            if (data != null) {
                data.setKickDashing(false);
            }

            // 注销事件监听
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
            isActive = false;

            // 执行回调
            onEnd.run();
        }
    }
}