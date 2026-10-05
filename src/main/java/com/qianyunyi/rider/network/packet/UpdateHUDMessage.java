package com.qianyunyi.rider.network.packet;


import com.qianyunyi.rider.api.network.IMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class UpdateHUDMessage implements IMessage {
    private final float energy;
    private final float maxEnergy;

    public UpdateHUDMessage(float energy, float maxEnergy) {
        this.energy = energy;
        this.maxEnergy = maxEnergy;
    }

    // 必须添加的解码构造函数
    public UpdateHUDMessage(FriendlyByteBuf buf) {
        this(buf.readFloat(), buf.readFloat());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(energy);
        buf.writeFloat(maxEnergy);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            //KuugaEnergyHUD.updateEnergy(energy, maxEnergy);
        });
        ctx.setPacketHandled(true);
    }
}