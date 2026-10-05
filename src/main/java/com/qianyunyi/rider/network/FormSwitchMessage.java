package com.qianyunyi.rider.network;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端选中形态轮盘的选项后，向服务端发送的消息。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public record FormSwitchMessage(String targetFormName) {

    public FormSwitchMessage(String targetFormName) {
        this.targetFormName = targetFormName == null ? "" : targetFormName;
    }

    public static void buffer(FormSwitchMessage msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.targetFormName);
    }

    public static FormSwitchMessage decode(FriendlyByteBuf buf) {
        return new FormSwitchMessage(buf.readUtf());
    }

    public static void handler(FormSwitchMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.get().getSender();
            if (serverPlayer == null) return;
            RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
            if (data == null) return;

            String targetForm = msg.targetFormName();
            String targetRider = data.getTargetRiderName();
            data.setTargetFormName(targetForm);

            if (targetRider == null || targetRider.isEmpty()) return;

            RiderConfig riderConfig = RiderRegistryHelper.getConfig(targetRider).orElse(null);
            if (riderConfig == null) return;

            FormConfig formConfig = riderConfig.getForm(targetForm);
            if (formConfig == null) return;

            HenshinState state = data.getCurrentState();
            String activeForm = data.getActiveFormName();

            if (state == HenshinState.INACTIVE) {
                // 未变身状态：进入对应形态的待机
                data.setActiveRiderName(targetRider);
                data.setActiveFormName(targetForm);
                formConfig.getBehavior().enterStandbyState(serverPlayer, riderConfig, formConfig);
            } else if (state == HenshinState.STANDBY || state == HenshinState.STANDBY_AWAKENING) {
                // 待机状态：更新形态后直接触发变身
                data.setActiveFormName(targetForm);
                formConfig.getBehavior().handleHenshin(serverPlayer, riderConfig, formConfig);
            } else {
                // 激活状态：调用骑士配置的形态切换
                if (!targetRider.equals(data.getActiveRiderName())) {
                    KamenRiderInterwovenWorldsMod.LOGGER.warn("[FormSwitch] targetRider mismatch: target={}, active={}",
                            targetRider, data.getActiveRiderName());
                }
                riderConfig.switchForm(serverPlayer, targetForm);
            }

            KamenRiderInterwovenWorldsMod.LOGGER.info("形态轮盘选择: 骑士={}, 形态={}, 状态={}",
                    targetRider, targetForm, state);
        });
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        KamenRiderInterwovenWorldsMod.addNetworkMessage(
                FormSwitchMessage.class,
                FormSwitchMessage::buffer,
                FormSwitchMessage::decode,
                FormSwitchMessage::handler
        );
    }
}