package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.api.rider.behavior.IHUDManager;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class HUDControlMessage implements IMessage {
    private final String riderName;
    private final String formName;
    private final boolean shouldShow;

    public HUDControlMessage(String riderName, String formName, boolean shouldShow) {
        this.riderName = riderName;
        this.formName = formName;
        this.shouldShow = shouldShow;
    }

    public HUDControlMessage(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readUtf(), buf.readBoolean());
    }

    public static HUDControlMessage decode(FriendlyByteBuf buf) {
        return new HUDControlMessage(buf);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(riderName);
        buf.writeUtf(formName);
        buf.writeBoolean(shouldShow);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Optional<FormConfig> formConfigOpt = RiderRegistryHelper.getFormConfig(riderName, formName);
            if (formConfigOpt.isPresent()) {
                FormConfig formConfig = formConfigOpt.get();
                Supplier<IHUDManager> hudSupplier = formConfig.getHudManagerSupplier();
                if (hudSupplier != null) {
                    IHUDManager hud = hudSupplier.get();
                    if (hud != null) {
                        if (shouldShow) {
                            hud.show();
                        } else {
                            hud.hide();
                        }
                    }
                }
            } else {
                KamenRiderInterwovenWorldsMod.LOGGER.warn(
                        "[HUDControlMessage] 未找到形态配置: {}/{}", riderName, formName);
            }
        });
        ctx.setPacketHandled(true);
    }
}