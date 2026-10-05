package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.client.gui.wheel.FormSwitchWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class KuugaFormSwitchCooldownMessage implements IMessage {

    public static final int COOLDOWN_TICKS = 100;

    private final int cooldownTicks;

    public KuugaFormSwitchCooldownMessage() {
        this.cooldownTicks = COOLDOWN_TICKS;
    }

    public KuugaFormSwitchCooldownMessage(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public KuugaFormSwitchCooldownMessage(FriendlyByteBuf buf) {
        this.cooldownTicks = buf.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(cooldownTicks);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Minecraft.getInstance().level == null) return;
                FormSwitchWheel.setFormSwitchCooldown(
                        Minecraft.getInstance().level.getGameTime() + cooldownTicks);
            }));
        }
        ctx.setPacketHandled(true);
    }
}