package com.qianyunyi.rider.event.listener.rider.kuuga;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.api.rider.listener.IPlayerEventListener;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.item.base.BaseAnimatedArmorItem;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class GrowingKuugaHenshinListener implements IPlayerEventListener {
    public static final String TYPE = "growing_kuuga_henshin";
    private static final int MAX_ATTACK_COUNT = 5; // 5次攻击完成变身
    private ServerPlayer serverPlayer;
    private boolean isRegistered = false;
    private int attackCount; // 攻击计数器

    /**
     * 初始化监听器
     * @param serverPlayer 服务端玩家
     */
    @Override
    public void initialize(ServerPlayer serverPlayer) {
        this.serverPlayer = serverPlayer;
        this.attackCount = 0; // 初始化计数器
    }

    @Override
    public boolean isRegistered() {
        return isRegistered;
    }

    /**
     * 注册监听器
     */
    @Override
    public void register() {
        if (!isRegistered && serverPlayer != null) {
            MinecraftForge.EVENT_BUS.register(this);
            isRegistered = true;
            KamenRiderInterwovenWorldsMod.LOGGER.info("注册监听器{}给玩家: {}", TYPE, serverPlayer.getUUID());
        }
    }

    /**
     * 注销监听器
     */
    @Override
    public void unregister() {
        if (isRegistered) {
            MinecraftForge.EVENT_BUS.unregister(this);
            isRegistered = false;
            KamenRiderInterwovenWorldsMod.LOGGER.info("注销监听器{}给玩家: {}", TYPE, serverPlayer.getUUID());
        }
    }

    /**
     * 检查是否应该激活
     * @param serverPlayer 服务端玩家
     * @return 是否应该激活
     */
    @Override
    public boolean shouldBeActive(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        return data.getActiveRiderName().equals("Kuuga") &&
                data.getActiveFormName().equals("base") &&
                data.getCurrentState().equals(HenshinState.STANDBY) &&
                !data.isHenshining();
    }

    /**
     * 检查是否永久禁用
     * @param serverPlayer 玩家
     * @return 是否永久禁用
     */
    @Override
    public boolean isPermanentlyDisabled(ServerPlayer serverPlayer) {
        return RiderPlayerVariablesProvider.get(serverPlayer).getKuugaData().isMightyKuuga();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @SubscribeEvent
    public void onLivingAttack(AttackEntityEvent attackEvent) {
        // 检查是否是激活器监听的玩家造成的伤害
        if (!(attackEvent.getEntity() instanceof ServerPlayer attacker)) return;
        if (!attacker.getUUID().equals(serverPlayer.getUUID())) return;
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        //获取初生形态配置
        String riderName = data.getActiveRiderName();
        if (!riderName.equals("Kuuga")) {
            unregister();
            return;
        }
        RiderConfig cfg = RiderRegistryHelper.getConfig(data.getActiveRiderName()).orElseThrow();
        FormConfig baseForm = cfg.getForm("base");

        attackCount++; // 增加攻击计数
        ((ServerLevel)serverPlayer.level()).sendParticles(ParticleTypes.FLASH,
                serverPlayer.getX(), serverPlayer.getY() + 0.5, serverPlayer.getZ(),
                10, 0.5, 0.5, 0.5, 0.1);
        //攻击次数是单数时候播放PUNCH_HENSHIN_1，双数时候播放PUNCH_HENSHIN_2，攻击次数等于1时装备头盔胸甲靴子
        if (attackCount % 2 == 0) {
            serverPlayer.level().playSound(null,
                    serverPlayer.getOnPos(),
                    KamenRiderInterwovenWorldsModSounds.PUNCH_HENSHIN_2.get(),
                    SoundSource.PLAYERS,
                    1,
                    0);
        } else if (attackCount !=MAX_ATTACK_COUNT ) {
            serverPlayer.level().playSound(null,
                    serverPlayer.getOnPos(),
                    KamenRiderInterwovenWorldsModSounds.PUNCH_HENSHIN_1.get(),
                    SoundSource.PLAYERS,
                    1,
                    0);
        }
        if (attackCount == 1) {
            data.setHenshining(true);
            // 装备头盔、胸甲和靴子
            baseForm.equipArmorExcludeBelt(serverPlayer);
            // 为所有装备的盔甲设置NBT标签
            setArmorNbtTags();
        } else if (attackCount == MAX_ATTACK_COUNT) {
            KamenRiderInterwovenWorldsMod.LOGGER.info("最终攻击");
            // 装备腰带并播放summons动画
            HenshinUtil.equipBeltAndPlayAnimation(serverPlayer,baseForm,"summons");
            // 播放最终攻击音效
            serverPlayer.level().playSound(null,
                    serverPlayer.getOnPos(),
                    KamenRiderInterwovenWorldsModSounds.GROWING_HENSHIN.get(),
                    SoundSource.PLAYERS,
                    1,
                    0);

            // 为所有装备的盔甲设置NBT标签
            setArmorNbtTags();

            ItemStack itemBelt = serverPlayer.getItemBySlot(EquipmentSlot.LEGS);
            CompoundTag tag = itemBelt.getOrCreateTag();
            tag.putBoolean("ActiveAmadam", true);
            tag.putBoolean("ActivePanel", true);
            tag.putBoolean("ActiveRune", true);
            tag.putString("ActiveAmadam", "orangeAmadam");
            tag.putString("ActivePanel", "basePanel");
            tag.putString("ActiveRune",   "baseRune");
            BaseAnimatedArmorItem.setAnim(itemBelt, "idle");

            DelayedTask.run(serverPlayer.level(), 20, () -> {
                HenshinUtil.equipHelmetChestplateBootsAndPlayAnimation(serverPlayer,"idle");
                data.setHenshining(false);
                baseForm.getBehavior().handleQuickHenshin(serverPlayer, cfg, baseForm);
                //注销监听器
                unregister();
                this.attackCount = 0;
            });
        } else {
            // 为所有装备的盔甲设置NBT标签
            setArmorNbtTags();
        }
    }

    /**
     * 为所有盔甲部件设置NBT标签
     * 根据攻击次数设置不同的变身阶段标签
     */
    private void setArmorNbtTags() {
        // 清除之前的变身标签
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack itemStack = serverPlayer.getItemBySlot(slot);
            if (!itemStack.isEmpty()) {
                CompoundTag tag = itemStack.getOrCreateTag();
                // 清除所有阶段标签
                for (int i = 1; i <= 5; i++) {
                    tag.remove("henshin" + i);
                }
                // 设置当前阶段的标签
                tag.putString("henshin" + attackCount, "true");
                // 设置所有装备的动画
                String animationName = "growing_form_kuuga_henshin_" + attackCount;
                // 播放装备动画
                HenshinUtil.equipHelmetChestplateBootsAndPlayAnimation(serverPlayer,animationName);
            }
        }
    }
}