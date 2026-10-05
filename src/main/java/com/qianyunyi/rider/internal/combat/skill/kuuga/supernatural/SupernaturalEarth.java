package com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.entity.effect.FallingBlockEffectEntity;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.AnimationMessage;
import com.qianyunyi.rider.network.packet.ScreenShakeSyncMessage;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SupernaturalEarth implements IRiderSkill {

    @Override
    public String getDisplayName() {
        return "超自然之土";
    }

    private static final String SUPERNATURAL_EARTH_ANIM = "supernatural_earth";

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/skill/supernatural_earth.png");
    }

    private static final int MIN_BLOCK_COUNT = 8;
    private static final int MAX_BLOCK_COUNT = 18;
    private static final double GATHER_RADIUS = 8.0;
    private static final double CONVERGE_HEIGHT = 2.5;
    private static final int HOVER_TICKS = 15;
    private static final double LAUNCH_DISTANCE = 25.0;
    private static final double LAUNCH_SPEED = 1.8;
    private static final int COOLDOWN_SECONDS = 12;
    // 亚玛达姆能量消耗
    private static final float ENERGY_COST = 20.0f;
    // 方块最大聚集时间（tick），与 FallingBlockEffectEntity 中 life > 40 保持一致
    private static final int MAX_GATHER_TICKS = 40;
    private static final int TOTAL_PREPARE_TICKS = MAX_GATHER_TICKS + HOVER_TICKS;
    private long cooldownStartTick = 0;

    @Override
    public void skill(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;
        if (data.isSkillActive()) return;
        data.setSkillActive(true);

        // 检查并消耗亚玛达姆能量
        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null) {
            data.setSkillActive(false);
            return;
        }
        if (kuugaData.getAmadamEnergy() < ENERGY_COST) {
            HenshinUtil.sendMessage(serverPlayer, "亚玛达姆能量不足！需要 " + (int) ENERGY_COST + " 点能量", ChatFormatting.GOLD);
            data.setSkillActive(false);
            return;
        }
        kuugaData.removeAmadamEnergy(ENERGY_COST);

        NetworkHandler.sendToTrackingPlayers(
                new AnimationMessage(SUPERNATURAL_EARTH_ANIM, serverPlayer.getUUID(), true), serverPlayer);
        serverPlayer.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F, 0.5F);

        ServerLevel serverLevel = (ServerLevel) serverPlayer.level();
        Vec3 playerPos = serverPlayer.position();

        Vec3 convergePoint = playerPos.add(0, CONVERGE_HEIGHT, 0);

        List<BlockPos> groundBlocks = findNearbyGroundBlocks(serverLevel, playerPos, GATHER_RADIUS);
        Collections.shuffle(groundBlocks, new java.util.Random());
        int blockCount = Math.min(MAX_BLOCK_COUNT, groundBlocks.size());

        if (blockCount < MIN_BLOCK_COUNT) {
            serverPlayer.sendSystemMessage(
                    Component.literal("周围没有足够的方块！"));
            data.setSkillActive(false);
            return;
        }

        cooldownStartTick = serverPlayer.level().getGameTime();
        data.setSupernaturalEarthActive(true);

        // 计算统一发射时刻：所有方块聚集完成后 + 悬浮时间，届时一起发射
        long launchTick = serverLevel.getGameTime() + TOTAL_PREPARE_TICKS;

        for (int i = 0; i < blockCount; i++) {
            BlockPos pos = groundBlocks.get(i);
            BlockState blockState = serverLevel.getBlockState(pos);
            Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

            FallingBlockEffectEntity rockEntity = new FallingBlockEffectEntity(
                    serverLevel,
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    blockState,
                    FallingBlockEffectEntity.EffectType.GATHER_LAUNCH,
                    convergePoint,
                    200,
                    HOVER_TICKS,
                    serverPlayer
            );
            rockEntity.setLaunchSpeed(LAUNCH_SPEED);
            rockEntity.setSharedLaunchTick(launchTick);
            Vec3 lookAngle = serverPlayer.getLookAngle().normalize();
            Vec3 targetPoint = convergePoint.add(lookAngle.scale(LAUNCH_DISTANCE));
            rockEntity.setTargetPoint(targetPoint);
            serverLevel.addFreshEntity(rockEntity);

            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    3, 0.2, 0.2, 0.2, 0.05
            );
        }

        DelayedTask.run(serverLevel, 3, () -> {
            for (int i = 0; i < 3; i++) {
                int finalI = i;
                DelayedTask.run(serverLevel, i * 4, () -> {
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                            convergePoint.x, convergePoint.y, convergePoint.z,
                            15, 1.0, 1.0, 1.0, 0.1);
                    serverLevel.playSound(null, convergePoint.x, convergePoint.y, convergePoint.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, serverPlayer.getSoundSource(),
                            0.4F, 0.5F + finalI * 0.2F);
                });
            }
        });

        DelayedTask.run(serverLevel, TOTAL_PREPARE_TICKS, () -> {
            serverPlayer.playSound(SoundEvents.WARDEN_SONIC_BOOM, 0.8F, 0.5F);
        });

        // 方块落地后屏幕震动（聚集 + 悬浮 + 飞行落地 ≈ TOTAL_PREPARE_TICKS + 20tick）
        DelayedTask.run(serverLevel, TOTAL_PREPARE_TICKS + 20, () -> {
            Vec3 impactCenter = convergePoint.add(
                    serverPlayer.getLookAngle().normalize().scale(LAUNCH_DISTANCE));
            for (ServerPlayer p : serverLevel.getServer().getPlayerList().getPlayers()) {
                if (p.distanceToSqr(impactCenter.x, impactCenter.y, impactCenter.z) <= 100.0) {
                    NetworkHandler.sendToPlayer(new ScreenShakeSyncMessage(0.35f, 15), p);
                }
            }
        });

        DelayedTask.run(serverLevel, TOTAL_PREPARE_TICKS + 20, () -> {
            data.setSkillActive(false);
            data.setSupernaturalEarthActive(false);
        });
    }

    private List<BlockPos> findNearbyGroundBlocks(ServerLevel level, Vec3 center, double radius) {
        List<BlockPos> blocks = new ArrayList<>();
        int cx = (int) center.x;
        int cy = (int) center.y;
        int cz = (int) center.z;
        int r = (int) Math.ceil(radius);

        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                double dx = x - center.x;
                double dz = z - center.z;
                if (dx * dx + dz * dz > radius * radius) continue;

                for (int y = cy - 1; y <= cy + 1; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.isSolidRender(level, pos)
                            && !state.is(Blocks.BEDROCK) && !state.is(Blocks.OBSIDIAN)) {
                        blocks.add(pos);
                        break;
                    }
                }
            }
        }
        return blocks;
    }

    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {}

    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        long currentTick = serverPlayer.level().getGameTime();
        long elapsed = (currentTick - cooldownStartTick) / 20;
        long remaining = COOLDOWN_SECONDS - elapsed;
        if (remaining > 0) {
            HenshinUtil.sendMessage(serverPlayer, "技能冷却中，剩余时间：" + remaining + "秒", ChatFormatting.GOLD);
            return false;
        }
        return true;
    }
}