package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.client.effect.YellowLightningEffectHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.BiConsumer;

public class YellowLightningSyncMessage implements IMessage {
    private final UUID entityId;
    private final int phase;       // 0=CHARGING, 1=FLYING, 2=LANDING, 3=INACTIVE
    private final double x, y, z;
    private final double dirX, dirY, dirZ; // 方向向量（仅FLYING阶段有效）
    private final int duration;
    private final long gameTime;

    public YellowLightningSyncMessage(UUID entityId, int phase, double x, double y, double z,
                                      double dirX, double dirY, double dirZ,
                                      int duration, long gameTime) {
        this.entityId = entityId;
        this.phase = phase;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dirX = dirX;
        this.dirY = dirY;
        this.dirZ = dirZ;
        this.duration = duration;
        this.gameTime = gameTime;
    }

    public YellowLightningSyncMessage(FriendlyByteBuf buffer) {
        this.entityId = buffer.readUUID();
        this.phase = buffer.readInt();
        this.x = buffer.readDouble();
        this.y = buffer.readDouble();
        this.z = buffer.readDouble();
        this.dirX = buffer.readDouble();
        this.dirY = buffer.readDouble();
        this.dirZ = buffer.readDouble();
        this.duration = buffer.readInt();
        this.gameTime = buffer.readLong();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(entityId);
        buf.writeInt(phase);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(dirX);
        buf.writeDouble(dirY);
        buf.writeDouble(dirZ);
        buf.writeInt(duration);
        buf.writeLong(gameTime);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        BiConsumer<YellowLightningSyncMessage, NetworkEvent.Context> handler =
                YellowLightningEffectHandler.getHandler();
        if (handler != null) {
            handler.accept(this, ctx);
        }
        ctx.setPacketHandled(true);
    }

    public UUID getEntityId() { return entityId; }
    public int getPhase() { return phase; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getDirX() { return dirX; }
    public double getDirY() { return dirY; }
    public double getDirZ() { return dirZ; }
    public int getDuration() { return duration; }
    public long getGameTime() { return gameTime; }
}