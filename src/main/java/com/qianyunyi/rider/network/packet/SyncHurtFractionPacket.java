package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/**
 * 服务端→客户端：受伤瞬间即时同步“减免后实际扣除生命值的比例”，
 * 用于客户端受击特效（镜头摇晃、红色遮罩等）的破防放行判断，避免依赖周期脏同步造成延迟。
 */
public class SyncHurtFractionPacket implements IMessage {

    private final UUID playerId;
    private final float hurtFraction;

    public SyncHurtFractionPacket(UUID playerId, float hurtFraction) {
        this.playerId = playerId;
        this.hurtFraction = hurtFraction;
    }

    public SyncHurtFractionPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.hurtFraction = buf.readFloat();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeFloat(hurtFraction);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Minecraft.getInstance().level == null) return;
                Player player = Minecraft.getInstance().level.getPlayerByUUID(playerId);
                if (player == null) return;
                RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
                if (data != null) {
                    data.setLastHurtFraction(hurtFraction);
                }
            }));
        }
        ctx.setPacketHandled(true);
    }
}