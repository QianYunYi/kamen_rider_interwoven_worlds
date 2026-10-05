package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.api.network.IMessage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/**
 * 模块级数据同步包
 * 哪个模块脏了，就同步整个模块的NBT，不拆字段
 */
public final class SyncModuleDataPacket implements IMessage {

    private final String moduleName;
    private final CompoundTag moduleData;
    private final UUID target;

    public SyncModuleDataPacket(String moduleName, CompoundTag moduleData, UUID target) {
        this.moduleName = moduleName;
        this.moduleData = moduleData;
        this.target = target != null ? target : Util.NIL_UUID;
    }

    public SyncModuleDataPacket(FriendlyByteBuf buf) {
        this.moduleName = buf.readUtf(Short.MAX_VALUE);
        this.moduleData = buf.readNbt();
        this.target = buf.readUUID();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(moduleName);
        buf.writeNbt(moduleData);
        buf.writeUUID(target);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                LocalPlayer clientPlayer = Minecraft.getInstance().player;
                if (clientPlayer != null &&
                        (target.equals(Util.NIL_UUID) || clientPlayer.getUUID().equals(target))) {

                    RiderPlayerData manager = RiderPlayerVariablesProvider.get(clientPlayer);
                    manager.batchUpdate(() -> {
                        // 单模块兼容
                        if (!"batch".equals(moduleName)) {
                            var module = manager.getModule(moduleName);
                            if (module != null) module.deserializeNBT(moduleData);
                            return;
                        }
                        // 批量同步
                        for (String key : moduleData.getAllKeys()) {
                            var module = manager.getModule(key);
                            if (module != null && moduleData.contains(key, 10)) {
                                module.deserializeNBT(moduleData.getCompound(key));
                            }
                        }
                    });
                }
            }
        }));
        ctx.setPacketHandled(true);
    }

    public String getModuleName() { return moduleName; }
    public CompoundTag getModuleData() { return moduleData; }
    public UUID getTarget() { return target; }
}