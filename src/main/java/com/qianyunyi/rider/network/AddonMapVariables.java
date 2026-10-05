package com.qianyunyi.rider.network;


import com.qianyunyi.rider.network.packet.AddonSavedDataSyncMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.network.PacketDistributor;

/**
 * 地图变量
 *
 * @author QuanYunYi
 */
public final class AddonMapVariables extends SavedData {
    public static final String DATA_NAME = "rider_adventure_affiliated_map";
    public boolean threeHenshin = false;
    public boolean kingFollower = false;
    // 已移除：public int GurongiSpawn = 0;

    /* 单例缓存，客户端用 */
    public static AddonMapVariables clientSide = new AddonMapVariables();

    /* 从 NBT 加载 */
    public static AddonMapVariables load(CompoundTag tag) {
        AddonMapVariables data = new AddonMapVariables();
        data.read(tag);
        return data;
    }

    /* 读取 NBT */
    public void read(CompoundTag nbt) {
        this.threeHenshin = nbt.getBoolean("threeHenshin");
        this.kingFollower = nbt.getBoolean("kingFollower");
        // 已移除：this.GurongiSpawn = nbt.getInt("GurongiSpawn");
    }

    /* 保存到 NBT */
    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putBoolean("threeHenshin", this.threeHenshin);
        nbt.putBoolean("kingFollower", this.kingFollower);
        // 已移除：nbt.putInt("GurongiSpawn", this.GurongiSpawn);
        return nbt;
    }

    /* 获取实例：服务端从 SavedData，客户端用缓存 */
    public static AddonMapVariables get(LevelAccessor world) {
        if (world instanceof ServerLevel level) {
            return level.getDataStorage()
                    .computeIfAbsent(AddonMapVariables::load, AddonMapVariables::new, DATA_NAME);
        } else {
            return clientSide;
        }
    }

    /* 服务端 → 所有客户端同步 */
    public void syncData(LevelAccessor world) {
        this.setDirty();
        if (!world.isClientSide()) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.ALL.noArg(),
                    new AddonSavedDataSyncMessage(0, this)
            );
        }
    }

    public CompoundTag serializeNBT() {
        return save(new CompoundTag());
    }
}