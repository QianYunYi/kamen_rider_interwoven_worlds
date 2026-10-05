package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.network.AddonMapVariables;
import com.qianyunyi.rider.network.AddonWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.network.NetworkEvent;

/**
 * 同步保存数据
 *
 * @author QuanYunYi
 */
public final class AddonSavedDataSyncMessage implements IMessage {
    // 0: AddonMapVariables 1: AddonWorldData
    public final int type;
    // 数据
    public final SavedData data;

    /**
     * @param type 0: AddonMapVariables 1: AddonWorldData
     * @param data 数据
     */
    public AddonSavedDataSyncMessage(int type, SavedData data) {
        this.type = type;
        this.data = data;
    }

    /**
     * @param buf 数据缓冲
     */
    public AddonSavedDataSyncMessage(FriendlyByteBuf buf) {
        this.type = buf.readInt();
        CompoundTag tag = buf.readNbt();

        // 修复：保证所有分支都给 data 赋值
        if (tag != null) {
            this.data = (type == 0 ? AddonMapVariables.load(tag) : AddonWorldData.load(tag));
        } else {
            // 空标签时创建一个空数据对象，避免未初始化
            this.data = type == 0 ? new AddonMapVariables() : new AddonWorldData();
        }
    }

    /**
     * @param buf 数据缓冲
     */
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(type);
        buf.writeNbt(data.save(new CompoundTag()));
    }

    /**
     * @param ctx 网络上下文
     */
    @Override
    public void handle(NetworkEvent.Context ctx) {
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> {
                if (type == 0) {
                    AddonMapVariables.clientSide = (AddonMapVariables) data;
                } else {
                    AddonWorldData.clientSide = (AddonWorldData) data;
                }
            });
        }
        ctx.setPacketHandled(true);
    }
}