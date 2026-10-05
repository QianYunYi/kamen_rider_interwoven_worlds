package com.qianyunyi.rider.internal.triggers;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.internal.henshin.system.FirstTimeTriggerSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 祝福首次触发处理器
 * 负责在玩家首次进入世界时给予祝福效果
 */
public class BlessingTriggerHandler implements FirstTimeTriggerSystem.FirstTimeTriggerHandler {

    public static final String HANDLER_ID = "rider_qianyunyi:blessing_trigger";

    // 祝福效果持续时间（ticks）
    private static final int EFFECT_DURATION = 12000; // 10分钟

    // 注册到首次触发系统
    static {
        FirstTimeTriggerSystem.registerHandler(HANDLER_ID, new BlessingTriggerHandler());
    }

    @Override
    public boolean shouldTrigger(ServerPlayer player) {
        // 50%概率触发
        return player.level().random.nextFloat() <= 0.5f;
    }

    @Override
    public void execute(ServerPlayer player) {
//        // 添加祝福效果
//        player.addEffect(new MobEffectInstance(
//                RiderAdventureAffiliatedModMobEffects.AGITO_FACTOR.get(),
//                EFFECT_DURATION, 1, false, true)); // 显示图标
//
//        player.addEffect(new MobEffectInstance(
//                RiderAdventureAffiliatedModMobEffects.SEEDOF_LIGHT_POWER.get(),
//                EFFECT_DURATION, 1, false, true)); // 显示图标
    }

    @Override
    public boolean hasTriggered(ServerPlayer player) {
        BlessingData data = BlessingData.get(player.serverLevel().getLevel());
        return data.hasTriggered(player.getUUID());
    }

    @Override
    public void markTriggered(ServerPlayer player) {
        BlessingData data = BlessingData.get(player.serverLevel().getLevel());
        data.markTriggered(player.getUUID());
    }

    /**
     * 基于存档的祝福记录存储
     */
    public static class BlessingData extends SavedData {
        private final Set<UUID> triggeredPlayers = ConcurrentHashMap.newKeySet();

        /**
         * 获取当前存档的祝福记录
         * @param level 服务器世界
         * @return 祝福记录实例
         */
        public static BlessingData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    BlessingData::load,
                    BlessingData::new,
                    "rider_adventure_affiliated_blessing_triggers"
            );
        }

        /**
         * 检查玩家是否已触发
         * @param playerId 玩家UUID
         * @return 是否已触发
         */
        public boolean hasTriggered(UUID playerId) {
            return triggeredPlayers.contains(playerId);
        }

        /**
         * 标记玩家已触发
         * @param playerId 玩家UUID
         */
        public void markTriggered(UUID playerId) {
            triggeredPlayers.add(playerId);
            setDirty(); // 标记数据需要保存
        }

        /**
         * 从NBT加载数据
         * @param nbt NBT数据
         * @return 加载后的实例
         */
        public static BlessingData load(CompoundTag nbt) {
            BlessingData data = new BlessingData();
            if (nbt.contains("triggeredPlayers", Tag.TAG_LIST)) {
                ListTag list = nbt.getList("triggeredPlayers", Tag.TAG_STRING);
                for (Tag tag : list) {
                    try {
                        UUID uuid = UUID.fromString(tag.getAsString());
                        data.triggeredPlayers.add(uuid);
                    } catch (IllegalArgumentException e) {
                        KamenRiderInterwovenWorldsMod.LOGGER.error("无效的UUID格式: {}", tag.getAsString());
                    }
                }
            }
            return data;
        }

        /**
         * 保存数据到NBT
         * @param nbt 目标NBT
         * @return 包含数据的NBT
         */
        @Override
        public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
            ListTag list = new ListTag();
            for (UUID uuid : triggeredPlayers) {
                list.add(StringTag.valueOf(uuid.toString()));
            }
            nbt.put("triggeredPlayers", list);
            return nbt;
        }
    }
}