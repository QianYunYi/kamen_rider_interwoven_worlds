package com.qianyunyi.rider.event.listener.rider.kuuga;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.api.rider.listener.IPlayerEventListener;
import com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.GrowingFormKuugaHenshinBehavior;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.entity.kaijin.gurongi.daguba.NDagubaZebaEntity;
import com.qianyunyi.rider.event.listener.GlobalListenerManager;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.SummonEntityUtil;
import com.qianyunyi.rider.util.TitleUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;

public class DagubaDeathListener implements IPlayerEventListener {
    public static final String TYPE = "daguba_death";

    private ServerPlayer serverPlayer;
    private boolean isRegistered = false;
    private final DagubaSummonListener summonListener1;
    private final DagubaSummon2Listener summonListener2;
    private final int eventType; // 1 或 2

    private NDagubaZebaEntity dagubaEntity;
    private boolean isDagubaDead = false;

    public DagubaDeathListener(DagubaSummonListener summonListener) {
        this.summonListener1 = summonListener;
        this.summonListener2 = null;
        this.eventType = 1;
    }

    public DagubaDeathListener(DagubaSummon2Listener summonListener) {
        this.summonListener1 = null;
        this.summonListener2 = summonListener;
        this.eventType = 2;
    }

    @Override
    public void initialize(ServerPlayer serverPlayer) {
        this.serverPlayer = serverPlayer;
    }

    @Override
    public boolean isRegistered() {
        return isRegistered;
    }

    @Override
    public void register() {
        if (!isRegistered && serverPlayer != null) {
            MinecraftForge.EVENT_BUS.register(this);
            isRegistered = true;
            KamenRiderInterwovenWorldsMod.LOGGER.info("注册达古巴死亡监听器给玩家: {}", serverPlayer.getUUID());
        }
    }

    @Override
    public void unregister() {
        if (isRegistered) {
            MinecraftForge.EVENT_BUS.unregister(this);
            isRegistered = false;
            KamenRiderInterwovenWorldsMod.LOGGER.info("注销达古巴死亡监听器给玩家: {}", serverPlayer.getUUID());
        }
    }

    @Override
    public boolean shouldBeActive(ServerPlayer serverPlayer) {
        return false; // 不参与全局管理
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        KamenRiderInterwovenWorldsMod.LOGGER.debug("进入达古巴死亡事件");
        LivingEntity entity = (LivingEntity) event.getEntity();

        if (entity == serverPlayer) {
            KamenRiderInterwovenWorldsMod.LOGGER.debug("玩家死亡事件触发");
            // 无论被谁杀死，都执行复活逻辑
            handlePlayerDeath(event);
        } else if (entity == dagubaEntity) {
            KamenRiderInterwovenWorldsMod.LOGGER.debug("达古巴死亡事件");
            handleDagubaDeath(event);
        }
    }

    private void handlePlayerDeath(LivingDeathEvent event) {
        // 播放击杀音效
        serverPlayer.level().playSound(null,
                serverPlayer.getOnPos(),
                KamenRiderInterwovenWorldsModSounds.KILLED_BY_DAGUBA.get(),
                SoundSource.PLAYERS,
                1.0F, // 音量
                1.0F  // 音调
        );

        // 取消死亡事件
        event.setCanceled(true);

        // 执行原地复活逻辑
        handlePlayerRevival();
    }

    /**
     * 生成达古巴实体
     */
    public void spawnDaguba() {
        if (serverPlayer == null || !(serverPlayer.level() instanceof ServerLevel level)) return;

        SummonEntityUtil.summonDirect(level, serverPlayer, KamenRiderInterwovenWorldsModEntities.N_DAGUBA_ZEBA);

        DelayedTask.run(level, 100, () -> {
            // 使用标题显示登场台词
            if (eventType == 1) {
                TitleUtils.sendTitleMessage(serverPlayer, "「再对我露出笑容吧」", ChatFormatting.DARK_RED);
            } else {
                TitleUtils.sendTitleMessage(serverPlayer, "「さあ、楽しい戦いをしよう」", ChatFormatting.DARK_RED);
            }
            serverPlayer.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F, 0.5F);
            serverPlayer.playSound(SoundEvents.WITHER_SPAWN, 1.0F, 0.3F);
        });
    }


    private void handlePlayerRevival() {
        //给予30秒的失明
        serverPlayer.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                600, 0, false, false));
        //给予30秒的呕吐
        serverPlayer.addEffect(new MobEffectInstance(
                MobEffects.CONFUSION,
                600, 0, false, false));
        //给予30秒的虚弱
        serverPlayer.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                600, 0, false, false));
        //给予30秒的无敌时间
        serverPlayer.invulnerableTime = 600;
        //给予30秒的不可移动效果
        serverPlayer.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                600, 0, false, false));
        // 恢复全部生命值
        serverPlayer.setHealth(serverPlayer.getMaxHealth() / 2.0f);

        // 切换为初生形态
        GrowingFormKuugaHenshinBehavior.switchToBaseForm(serverPlayer);

        // 给予10分钟不可变身效果
        serverPlayer.addEffect(new MobEffectInstance(
                KamenRiderInterwovenWorldsModMobEffects.HENSHIN_CANNOT.get(),
                12000, 0, false, false));

        GlobalListenerManager.updateListenerState(serverPlayer);

        // 传送达古巴到玩家身边并设置无敌状态
        teleportDagubaToPlayerAndSetInvulnerable();

        // 30秒后清除所有达古巴实体
        scheduleClearDaguba(30 * 20); // 30秒 * 20 ticks/秒

        // 设置玩家数据
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (eventType == 1) {
            data.getKuugaData().setHasSummonedDaguba(true);
            data.getKuugaData().setHasSummonedDaguba2(false);
        } else {
            data.getKuugaData().setHasSummonedDaguba2(true);
        }

        // 标记事件结束
        endEvent();
    }

    // 传送达古巴到玩家身边并设置无敌状态
    private void teleportDagubaToPlayerAndSetInvulnerable() {
        if (serverPlayer == null || !(serverPlayer.level() instanceof ServerLevel level)) return;

        if (dagubaEntity != null) {
            // 传送到达古巴玩家身边
            BlockPos playerPos = serverPlayer.blockPosition();
            dagubaEntity.teleportTo(playerPos.getX(), playerPos.getY(), playerPos.getZ());

            // 设置达古巴无敌
            dagubaEntity.setInvulnerable(true);

            // 停止AI
            dagubaEntity.setNoAi(true);

            KamenRiderInterwovenWorldsMod.LOGGER.info("已将达古巴传送到玩家身边，并设置为无敌和停止AI");
        } else {
            KamenRiderInterwovenWorldsMod.LOGGER.info("未找到达古巴实体，无法传送");
        }
    }

    // 清除所有达古巴实体
    private void scheduleClearDaguba(int delayTicks) {
        if (serverPlayer == null || !(serverPlayer.level() instanceof ServerLevel level)) return;

        DelayedTask.run(level, delayTicks, l -> {
            if (dagubaEntity != null) {
                dagubaEntity.discard();
                KamenRiderInterwovenWorldsMod.LOGGER.info("已清除达古巴实体");
            }
        });
    }

    // 处理达古巴死亡
    private void handleDagubaDeath(LivingDeathEvent event) {
        // 在服务端生成掉落物
        if (!event.getEntity().level().isClientSide()) {
            LivingEntity daguba = (LivingEntity) event.getEntity();
            DamageSource damageSource = event.getSource();

            // 使用 spawnAtLocation 掉落物品
            //daguba.spawnAtLocation(new ItemStack(KamenRiderInterwovenWorldsModItems.NIGHTMARE_ECHO_OF_DAGUBA.get()));
        }

        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (eventType == 1) {
            data.getKuugaData().setHasSummonedDaguba(true);
        } else {
            data.getKuugaData().setHasSummonedDaguba2(true);
        }

        GlobalListenerManager.updateListenerState(serverPlayer);

        // 标记事件结束
        endEvent();
    }

    private void endEvent() {
        // 清除达古巴实体
        if (dagubaEntity != null) {
            dagubaEntity.discard();
            dagubaEntity = null;
        }

        isDagubaDead = true;
        unregister();

        if (summonListener1 != null) {
            summonListener1.unregister();
        }
        if (summonListener2 != null) {
            summonListener2.unregister();
        }

        KamenRiderInterwovenWorldsMod.LOGGER.info("达古巴事件监听器已结束");
    }


    public boolean isDagubaDead() {
        return isDagubaDead;
    }
}