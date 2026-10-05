package com.qianyunyi.rider.network;

import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGameStage;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGlobalManager;
import com.qianyunyi.rider.network.packet.SyncPlayerDataPacket;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * 保存世界数据
 *
 * @author QianYunYi
 */
public class AddonWorldData extends SavedData {
    private static final String DATA_NAME = "kamen_rider_adventure_affiliated_world";
    public static AddonWorldData clientSide = new AddonWorldData();

    // 基鲁鲁游戏 古朗基生成计数器
    public int GurongiSpawn = 0;

    // 当前基基鲁游戏全球阶段
    private GurongiGameStage gameStage = GurongiGameStage.NONE;

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        nbt.putInt("GurongiSpawn", this.GurongiSpawn);
        nbt.putString("gameStage", this.gameStage.name());
        return nbt;
    }

    public static AddonWorldData load(CompoundTag tag) {
        AddonWorldData data = new AddonWorldData();
        if (tag.contains("GurongiSpawn")) {
            data.GurongiSpawn = tag.getInt("GurongiSpawn");
        }
        if (tag.contains("gameStage")) {
            try {
                data.gameStage = GurongiGameStage.valueOf(tag.getString("gameStage"));
            } catch (IllegalArgumentException e) {
                data.gameStage = GurongiGameStage.NONE;
            }
        }
        return data;
    }

    public static AddonWorldData get(LevelAccessor world) {
        if (world instanceof ServerLevel level) {
            return level.getDataStorage()
                    .computeIfAbsent(AddonWorldData::load, AddonWorldData::new, DATA_NAME);
        } else {
            return clientSide;
        }
    }

    public CompoundTag serializeNBT() {
        return save(new CompoundTag());
    }

    // 同步数据到客户端
    public void sync(LevelAccessor world) {
        setDirty();
        if (!world.isClientSide()) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.DIMENSION.with(() -> ((Level) world).dimension()),
                    new SyncPlayerDataPacket(this.save(new CompoundTag()), Util.NIL_UUID)
            );
        }
    }

    // ==================== GurongiSpawn 相关辅助方法 ====================

    public int getGurongiSpawn() {
        return this.GurongiSpawn;
    }

    public void setGurongiSpawn(int value) {
        this.GurongiSpawn = value;
        this.setDirty();
        GurongiGlobalManager.checkAndUpdate();
    }

    public void addGurongiSpawn(int value) {
        this.GurongiSpawn += value;
        this.setDirty();
        GurongiGlobalManager.checkAndUpdate();
    }

    public void incrementGurongiSpawn() {
        this.GurongiSpawn++;
        this.setDirty();
        GurongiGlobalManager.checkAndUpdate();
    }

    public void decrementGurongiSpawn() {
        this.GurongiSpawn--;
        this.setDirty();
        GurongiGlobalManager.checkAndUpdate();
    }

    public void resetGurongiSpawn() {
        this.GurongiSpawn = 0;
        this.setDirty();
        GurongiGlobalManager.checkAndUpdate();
    }

    public int gurongiSpawn() {
        return this.GurongiSpawn;
    }

    // ==================== ★ 新增：游戏阶段管理 ====================

    /**
     * 获取当前世界的基基鲁游戏阶段。
     */
    public GurongiGameStage getGameStage() {
        return gameStage;
    }

    /**
     * 设置当前世界的基基鲁游戏阶段。
     *
     * @param stage 新阶段
     */
    public void setGameStage(GurongiGameStage stage) {
        this.gameStage = stage;
        this.setDirty();
        GurongiGlobalManager.checkAndUpdate();
    }

    /**
     * 推进到下一个游戏阶段（按 AUDITION → SELECTION → SEMI_FINAL → WHITE_DARK_DUEL → FINAL 顺序）。
     *
     * @return 推进后的阶段，如果已在最终阶段则返回当前阶段
     */
    public GurongiGameStage advanceGameStage() {
        switch (gameStage) {
            case NONE:
                return setAndReturn(GurongiGameStage.AUDITION);
            case AUDITION:
                return setAndReturn(GurongiGameStage.SELECTION);
            case SELECTION:
                return setAndReturn(GurongiGameStage.SEMI_FINAL);
            case SEMI_FINAL:
                return setAndReturn(GurongiGameStage.WHITE_DARK_DUEL);
            case WHITE_DARK_DUEL:
                return setAndReturn(GurongiGameStage.FINAL);
            default:
                return gameStage;
        }
    }

    private GurongiGameStage setAndReturn(GurongiGameStage stage) {
        this.gameStage = stage;
        this.setDirty();
        return stage;
    }
}