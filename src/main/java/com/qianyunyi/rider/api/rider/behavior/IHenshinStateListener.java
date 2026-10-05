package com.qianyunyi.rider.api.rider.behavior;

import net.minecraft.server.level.ServerPlayer;

public interface IHenshinStateListener {
    void register();
    void unregister();
    void setServerPlayer(ServerPlayer serverPlayer);
}