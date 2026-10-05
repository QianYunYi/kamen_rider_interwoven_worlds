package com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.entity.BeatChaser2000Entity;
import com.qianyunyi.rider.entity.GouramEntity;
import com.qianyunyi.rider.entity.vehicle.gouram.GouramConfig;
import com.qianyunyi.rider.entity.HorseGouramTracker;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.CameraPerspectiveSyncMessage;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import java.util.List;

/**
 * 召唤/收起哥莱姆技能
 * - 如果周围有属于玩家的哥莱姆，收起它
 * - 如果没有哥莱姆，召唤一个新的并绑定为主人
 * - 哥莱姆会从远处飞过来
 */
public class SummonGouram implements IRiderSkill {

    private static final Map<UUID, Long> COLLISION_COOLDOWN = new HashMap<>();
    private static final int COLLISION_COOLDOWN_TICKS = 200; // 10秒

    public static void setCollisionCooldown(ServerPlayer player) {
        COLLISION_COOLDOWN.put(player.getUUID(), player.level().getGameTime() + COLLISION_COOLDOWN_TICKS);
    }

    public static boolean isOnCollisionCooldown(ServerPlayer player) {
        Long expireTick = COLLISION_COOLDOWN.get(player.getUUID());
        if (expireTick == null) return false;
        if (player.level().getGameTime() >= expireTick) {
            COLLISION_COOLDOWN.remove(player.getUUID());
            return false;
        }
        return true;
    }

    @Override
    public String getDisplayName() {
        return "召唤哥莱姆";
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/skill/gouram.png");
    }

    // 搜索半径
    private static final double SEARCH_RADIUS = 64.0;
    // 冷却时间（秒）
    private static final int COOLDOWN_SECONDS = 5;
    // 哥莱姆死亡冷却时间（秒）1分钟
    private static final int DEATH_COOLDOWN_SECONDS = 60;
    private long cooldownStartTick = 0;

    /**
     * 释放技能
     * 检查周围是否有哥莱姆，如果有则收起，如果没有则召唤
     */
    @Override
    public void skill(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;
        if (data.isSkillActive()) return;
        if (isOnCollisionCooldown(serverPlayer)) {
            HenshinUtil.sendMessage(serverPlayer, "哥莱姆召唤冷却中", ChatFormatting.RED);
            return;
        }
        data.setSkillActive(true);

        ServerLevel level = (ServerLevel) serverPlayer.level();

        if (serverPlayer.getVehicle() instanceof GouramEntity gouramVehicle) {
            serverPlayer.stopRiding();
            dismissGouram(gouramVehicle, serverPlayer);
            data.setSkillActive(false);
            return;
        }

        if (serverPlayer.getVehicle() instanceof BeatChaser2000Entity beatChaser) {
            handleMotorcycleGouram(level, serverPlayer, beatChaser);
            data.setSkillActive(false);
            return;
        }

        // 骑马时：哥莱姆附着/分离
        if (serverPlayer.getVehicle() instanceof Horse horse) {
            handleHorseGouram(level, serverPlayer, horse);
            data.setSkillActive(false);
            return;
        }

        // 查找玩家拥有的哥莱姆
        GouramEntity existingGouram = findOwnerGouram(level, serverPlayer);

        if (existingGouram != null) {
            // 收起哥莱姆（不添加冷却）
            dismissGouramIfOnHorse(existingGouram);
            dismissGouram(existingGouram, serverPlayer);
        } else {
            // 召唤哥莱姆（添加冷却并标记召唤）
            summonGouram(level, serverPlayer);
            // 标记哥莱姆已被召唤
            KuugaDataModule kuugaData = data.getKuugaData();
            if (kuugaData != null) {
                kuugaData.setGouramSummoned(true);
            }
            cooldownStartTick = serverPlayer.level().getGameTime();
            setCollisionCooldown(serverPlayer);
        }

        data.setSkillActive(false);
    }

    /**
     * 处理摩托车上的哥莱姆：清除周围自己的哥莱姆后合体/分离
     */
    private void handleMotorcycleGouram(ServerLevel level, ServerPlayer serverPlayer, BeatChaser2000Entity beatChaser) {
        GouramEntity existingGouram = findOwnerGouram(level, serverPlayer);
        if (existingGouram != null) {
            dismissGouramIfOnHorse(existingGouram);
            dismissGouram(existingGouram, serverPlayer);
        }
        if (beatChaser.isGouramCombined()) {
            beatChaser.disintegrateGouram();
        } else {
            beatChaser.combineGouram();
            cooldownStartTick = serverPlayer.level().getGameTime();
            setCollisionCooldown(serverPlayer);
        }
    }

    /**
     * 处理马上的哥莱姆：如果周围有自己的哥莱姆则让它过来附着，否则召唤新的
     */
    private void handleHorseGouram(ServerLevel level, ServerPlayer serverPlayer, Horse horse) {
        if (HorseGouramTracker.hasGouram(horse)) {
            // 分离：立即卸掉马铠，召唤实体播放gouram_disintegration
            HorseGouramTracker.detach(horse);
            GouramEntity gouram = new GouramEntity(KamenRiderInterwovenWorldsModEntities.GOURAM.get(), level);
            gouram.setOwnerUUID(serverPlayer.getUUID());
            gouram.startRiding(horse, true);
            level.addFreshEntity(gouram);
            DelayedTask.run(level, 1, () -> {
                gouram.setAnimation(GouramConfig.ANIM_GOURAM_DISINTEGRATION);
            });
            DelayedTask.run(level, 40, () -> {
                if (gouram.isAlive()) {
                    gouram.remove(GouramEntity.RemovalReason.DISCARDED);
                }
            });
        } else {
            // 检查周围是否有自己的哥莱姆
            GouramEntity existingGouram = findOwnerGouram(level, serverPlayer);
            if (existingGouram != null) {
                // 让已有的哥莱姆过来附着到马上
                dismissGouramIfOnHorse(existingGouram);
                existingGouram.stopRiding();
                existingGouram.moveTo(horse.getX(), horse.getY() + 0.5, horse.getZ(), existingGouram.getYRot(), existingGouram.getXRot());
                existingGouram.startRiding(horse, true);
                DelayedTask.run(level, 1, () -> {
                    existingGouram.setAnimation(GouramConfig.ANIM_GOURAM_COMBINE);
                });
                final Horse targetHorse = horse;
                DelayedTask.run(level, 60, () -> {
                    if (existingGouram.isAlive()) {
                        existingGouram.remove(GouramEntity.RemovalReason.DISCARDED);
                    }
                    HorseGouramTracker.attach(targetHorse);
                });
            } else {
                // 召唤：先召唤实体骑乘在马上，播放gouram_combine，3秒后装配马铠
                GouramEntity gouram = new GouramEntity(KamenRiderInterwovenWorldsModEntities.GOURAM.get(), level);
                gouram.setOwnerUUID(serverPlayer.getUUID());
                gouram.startRiding(horse, true);
                level.addFreshEntity(gouram);
                DelayedTask.run(level, 1, () -> {
                    gouram.setAnimation(GouramConfig.ANIM_GOURAM_COMBINE);
                });
                final Horse targetHorse = horse;
                DelayedTask.run(level, 60, () -> {
                    if (gouram.isAlive()) {
                        gouram.remove(GouramEntity.RemovalReason.DISCARDED);
                    }
                    HorseGouramTracker.attach(targetHorse);
                });
            }
            cooldownStartTick = serverPlayer.level().getGameTime();
            setCollisionCooldown(serverPlayer);
        }
    }

    /**
     * 如果哥莱姆附着在马上，先取消马的附着状态
     */
    private void dismissGouramIfOnHorse(GouramEntity gouram) {
        if (gouram.getVehicle() instanceof Horse horse) {
            HorseGouramTracker.detach(horse);
        }
    }

    /**
     * 查找属于玩家的哥莱姆
     */
    private GouramEntity findOwnerGouram(ServerLevel level, ServerPlayer player) {
        AABB searchArea = new AABB(
                player.getX() - SEARCH_RADIUS, player.getY() - SEARCH_RADIUS, player.getZ() - SEARCH_RADIUS,
                player.getX() + SEARCH_RADIUS, player.getY() + SEARCH_RADIUS, player.getZ() + SEARCH_RADIUS
        );

        List<GouramEntity> gourams = level.getEntitiesOfClass(GouramEntity.class, searchArea);

        for (GouramEntity gouram : gourams) {
            if (gouram.getOwnerUUID() != null && gouram.getOwnerUUID().equals(player.getUUID())) {
                return gouram;
            }
        }
        return null;
    }

    /**
     * 收起哥莱姆
     */
    private void dismissGouram(GouramEntity gouram, ServerPlayer player) {
        // 移除所有乘客
        for (Entity passenger : gouram.getPassengers()) {
            gouram.removePassenger(passenger);
        }

        // 添加粒子效果
        addDismissParticles(gouram, (ServerLevel) player.level());

        // 播放音效
        //player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);

        // 移除哥莱姆实体
        gouram.remove(GouramEntity.RemovalReason.DISCARDED);
    }

    /**
     * 召唤哥莱姆
     * 高空时触发救援模式：在玩家下方生成，直冲玩家并自动骑乘
     */
    private void summonGouram(ServerLevel level, ServerPlayer player) {
        int surfaceY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                player.blockPosition()).getY();
        boolean isHighAltitude = player.getY() > surfaceY + GouramConfig.highAltitudeThreshold;

        if (isHighAltitude) {
            rescueSummonGouram(level, player);
        } else {
            normalSummonGouram(level, player);
        }
    }

    private void normalSummonGouram(ServerLevel level, ServerPlayer player) {
        double spawnDistance = 10.0;
        double angle = player.getYRot() * (Math.PI / 180.0);
        double x = player.getX() - Math.sin(angle) * spawnDistance;
        double z = player.getZ() + Math.cos(angle) * spawnDistance;

        double y = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos((int) x, 0, (int) z)).getY() + 2.0;

        GouramEntity gouram = new GouramEntity(KamenRiderInterwovenWorldsModEntities.GOURAM.get(), level);
        gouram.moveTo(x, y, z, player.getYRot(), 0);
        gouram.setOwnerUUID(player.getUUID());

        addSummonParticles(gouram, level);
        level.addFreshEntity(gouram);
    }

    private void rescueSummonGouram(ServerLevel level, ServerPlayer player) {
        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        double facingRad = Math.toRadians(player.getYRot());
        double facingX = -Math.sin(facingRad);
        double facingZ = Math.cos(facingRad);

        double spawnY = playerY - 30;
        double centerX = playerX + facingX * 30;
        double centerZ = playerZ + facingZ * 30;

        BlockPos bestPos = null;
        for (int step = 0; step <= 10; step++) {
            int sign = (step % 2 == 0) ? 1 : -1;
            int offset = (step + 1) / 2 * sign;
            double testX = centerX + facingZ * offset;
            double testZ = centerZ - facingX * offset;
            BlockPos testPos = new BlockPos((int) testX, (int) spawnY, (int) testZ);
            if (!level.getBlockState(testPos).isSolid() && !level.getBlockState(testPos.above()).isSolid()) {
                int surfaceY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, testPos).getY();
                if (spawnY > surfaceY + 1) {
                    bestPos = testPos;
                    break;
                }
            }
        }

        if (bestPos == null) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,
                GouramConfig.slowFallingDurationTicks, 1, false, false, false));
        player.setDeltaMovement(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);

        HenshinUtil.sendAnimationMessage(player, "character_falls");
        NetworkHandler.sendToPlayer(new CameraPerspectiveSyncMessage(2), player);

        GouramEntity gouram = new GouramEntity(KamenRiderInterwovenWorldsModEntities.GOURAM.get(), level);
        gouram.moveTo(bestPos.getX() + 0.5, bestPos.getY() + 0.5, bestPos.getZ() + 0.5, player.getYRot(), 0);
        gouram.setOwnerUUID(player.getUUID());
        gouram.setRescueRush(true);
        addRescueSummonParticles(gouram, level);
        level.addFreshEntity(gouram);
    }

    private void addRescueSummonParticles(GouramEntity gouram, ServerLevel level) {
        for (int i = 0; i < 30; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 4;
            double offsetY = (level.random.nextDouble() - 0.5) * 4;
            double offsetZ = (level.random.nextDouble() - 0.5) * 4;
            level.sendParticles(ParticleTypes.FIREWORK,
                    gouram.getX(), gouram.getY() + 1, gouram.getZ(),
                    1, offsetX, offsetY, offsetZ, 0.05);
        }
    }

    /**
     * 添加召唤粒子效果
     */
    private void addSummonParticles(GouramEntity gouram, ServerLevel level) {
        for (int i = 0; i < 20; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2;
            double offsetY = (level.random.nextDouble() - 0.5) * 2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2;
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    gouram.getX(), gouram.getY() + 1, gouram.getZ(),
                    1, offsetX, offsetY, offsetZ, 0.1);
        }
    }

    /**
     * 添加收起粒子效果
     */
    private void addDismissParticles(GouramEntity gouram, ServerLevel level) {
        for (int i = 0; i < 30; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 3;
            double offsetY = (level.random.nextDouble() - 0.5) * 3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 3;
            level.sendParticles(ParticleTypes.PORTAL,
                    gouram.getX(), gouram.getY() + 1, gouram.getZ(),
                    1, offsetX, offsetY, offsetZ, 0.1);
        }
    }

    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {}

    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        long currentTick = serverPlayer.level().getGameTime();

        // 检查哥莱姆死亡冷却（1分钟）
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data != null) {
            KuugaDataModule kuugaData = data.getKuugaData();
            if (kuugaData != null && kuugaData.getGouramDeathTick() > 0) {
                long deathElapsed = (currentTick - kuugaData.getGouramDeathTick()) / 20;
                long deathRemaining = DEATH_COOLDOWN_SECONDS - deathElapsed;
                if (deathRemaining > 0) {
                    HenshinUtil.sendMessage(serverPlayer, "哥莱姆已死亡，召唤冷却中，剩余时间：" + deathRemaining + "秒", ChatFormatting.RED);
                    return false;
                }
                // 冷却结束，清除死亡标记
                kuugaData.setGouramDeathTick(0);
            }
        }

        // 检查普通技能冷却
        long elapsed = (currentTick - cooldownStartTick) / 20;
        long remaining = COOLDOWN_SECONDS - elapsed;
        if (remaining > 0) {
            HenshinUtil.sendMessage(serverPlayer, "技能冷却中，剩余时间：" + remaining + "秒", ChatFormatting.RED);
            return false;
        }
        return true;
    }
}