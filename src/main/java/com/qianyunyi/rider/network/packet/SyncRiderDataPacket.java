package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.data.RiderPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * 骑士数据同步包 - 1.20.1 Capability 版本
 *
 * 支持三种同步模式：
 * - FULL: 全量同步（登录、重生、维度切换）
 * - CORE: 仅核心字段（状态变化频繁时）
 * - MODULE: 仅脏模块数据（能量值等高频变化）
 */
public class SyncRiderDataPacket implements IMessage {

    public enum SyncType {
        FULL,   // 全量数据
        CORE,   // 核心字段
        MODULE  // 模块差异
    }

    private final SyncType type;
    private final CompoundTag data;
    private final UUID playerId;

    /** 全量同步 */
    public SyncRiderDataPacket(RiderPlayerData riderData, UUID playerId) {
        this.type = SyncType.FULL;
        this.data = riderData.serializeNBT();
        this.playerId = playerId;
    }

    /** 核心字段/模块差异同步 */
    public SyncRiderDataPacket(CompoundTag data, UUID playerId, SyncType type) {
        this.type = type;
        this.data = data;
        this.playerId = playerId;
    }

    /** 从缓冲区解码 */
    public SyncRiderDataPacket(FriendlyByteBuf buf) {
        this.type = SyncType.values()[buf.readByte()];
        this.data = buf.readNbt();
        this.playerId = buf.readUUID();
    }

    /** 编码到缓冲区 */
    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(type.ordinal());
        buf.writeNbt(data);
        buf.writeUUID(playerId);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;

            Player player = Minecraft.getInstance().level.getPlayerByUUID(playerId);
            if (player == null) return;

            RiderPlayerData riderData = RiderPlayerVariablesProvider.get(player);

            switch (type) {
                case FULL -> {
                    // 全量替换
                    riderData.deserializeNBT(data);
                }
                case CORE -> {
                    // 仅更新核心字段
                    if (data.contains("state")) {
                        riderData.setCurrentState(HenshinState.fromName(data.getString("state")));
                    }
                    if (data.contains("activeRider")) {
                        riderData.setActiveRiderName(data.getString("activeRider"));
                    }
                    if (data.contains("activeForm")) {
                        riderData.setActiveFormName(data.getString("activeForm"));
                    }
                    if (data.contains("switchIndex")) {
                        riderData.setCurrentSwitchIndex(data.getInt("switchIndex"));
                    }
                    if (data.contains("isHenshining")) {
                        riderData.setHenshining(data.getBoolean("isHenshining"));
                    }
                    if (data.contains("isSkillActive")) {
                        riderData.setSkillActive(data.getBoolean("isSkillActive"));
                    }
                    if (data.contains("isKickDashing")) {
                        riderData.setKickDashing(data.getBoolean("isKickDashing"));
                    }
                    if (data.contains("targetRider")) {
                        riderData.setTargetRiderName(data.getString("targetRider"));
                    }
                    if (data.contains("targetForm")) {
                        riderData.setTargetFormName(data.getString("targetForm"));
                    }
                    if (data.contains("unlockedRiders")) {
                        String ridersStr = data.getString("unlockedRiders");
                        riderData.setUnlockedRiders(
                                ridersStr.isEmpty()
                                        ? new ArrayList<>()
                                        : Arrays.asList(ridersStr.split(","))
                        );
                    }
                    if (data.contains("lastHurtFraction")) {
                        riderData.setLastHurtFraction(data.getFloat("lastHurtFraction"));
                    }
                }
                case MODULE -> {
                    // 更新模块数据
                    if (data.contains("modules", 10)) {
                        CompoundTag modulesTag = data.getCompound("modules");
                        for (String key : modulesTag.getAllKeys()) {
                            var module = riderData.getModule(key);
                            if (module != null) {
                                module.deserializeNBT(modulesTag.getCompound(key));
                                module.clearDirty();
                            }
                        }
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }

    // ========== 便捷构造方法 ==========

    /** 创建核心字段同步包（使用位掩码，零字符串比较） */
    public static SyncRiderDataPacket core(RiderPlayerData data, UUID playerId) {
        CompoundTag tag = new CompoundTag();
        int flags = data.getDirtyFlags();

        if ((flags & 0x1) != 0) tag.putString("state", data.getCurrentState().name());
        if ((flags & (1 << 1)) != 0) tag.putString("activeRider", data.getActiveRiderName());
        if ((flags & (1 << 2)) != 0) tag.putString("activeForm", data.getActiveFormName());
        if ((flags & (1 << 3)) != 0) tag.putInt("switchIndex", data.getCurrentSwitchIndex());
        if ((flags & (1 << 4)) != 0) tag.putBoolean("isHenshining", data.isHenshining());
        if ((flags & (1 << 5)) != 0) tag.putBoolean("isSkillActive", data.isSkillActive());
        if ((flags & (1 << 6)) != 0) tag.putBoolean("isKickDashing", data.isKickDashing());
        if ((flags & (1 << 7)) != 0) tag.putBoolean("isSupernaturalEarthActive", data.isSupernaturalEarthActive());
        if ((flags & (1 << 8)) != 0) tag.putString("unlockedRiders", String.join(",", data.getUnlockedRiders()));
        if ((flags & (1 << 9)) != 0) tag.putString("targetRider", data.getTargetRiderName());
        if ((flags & (1 << 10)) != 0) tag.putString("targetForm", data.getTargetFormName());
        if ((flags & (1 << 14)) != 0) tag.putFloat("lastHurtFraction", data.getLastHurtFraction());

        return new SyncRiderDataPacket(tag, playerId, SyncType.CORE);
    }

    /** 创建模块差异同步包 */
    public static SyncRiderDataPacket modules(RiderPlayerData data, UUID playerId) {
        CompoundTag modulesTag = new CompoundTag();
        for (var module : data.getDirtyModules()) {
            modulesTag.put(module.getModuleName(), module.serializeNBT());
        }
        CompoundTag tag = new CompoundTag();
        tag.put("modules", modulesTag);
        return new SyncRiderDataPacket(tag, playerId, SyncType.MODULE);
    }
}