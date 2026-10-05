package com.qianyunyi.rider.network;


import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.network.packet.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NetworkHandler {
    private static final String PROTOCOL = "1";
    // 注意：ResourceLocation.parse("kamen_rider_interwoven_worlds") 会生成 minecraft:kamen_rider_interwoven_worlds
    // 必须显式指定命名空间，否则通道注册到 minecraft 而非你的 mod
    private static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "network");

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_ID, () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int id = 0;
    private static boolean initialized = false;

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent e) {
        if (initialized) return;
        initialized = true;

        e.enqueueWork(() -> {
            // 客户端消息 (Server -> Client)
            // 注意：SyncRiderDataPacket 是数据同步主通道，必须第一个注册，
            // 否则 RiderDataSyncHandler 发送时会抛 IllegalArgumentException: Invalid message
            registerClientMessage(SyncRiderDataPacket.class);
            registerClientMessage(SyncHurtFractionPacket.class);
            registerClientMessage(SyncPlayerDataPacket.class);
            registerClientMessage(SyncModuleDataPacket.class);
            registerClientMessage(AddonSavedDataSyncMessage.class);
            registerClientMessage(AddonPlayerVariablesSyncMessage.class);
            registerClientMessage(AnimationMessage.class);
            registerClientMessage(KuugaHUDStateMessage.class);
            registerClientMessage(HUDControlMessage.class);
            registerClientMessage(WhiteScreenEffectMessage.class);
            registerClientMessage(UpdateHUDMessage.class);
            registerClientMessage(MessageUpdateBossBar.class);
            registerClientMessage(UpdateItemDataPacket.class);
            registerClientMessage(StartAutoAimPacket.class);
            registerClientMessage(StopAutoAimPacket.class);
            registerClientMessage(MessageComicBlackWhiteEffect.class);
            registerClientMessage(MessageSupernaturalKillingIntent.class);
            registerClientMessage(YellowLightningSyncMessage.class);
            registerClientMessage(CameraPerspectiveSyncMessage.class);
            registerClientMessage(ScreenShakeSyncMessage.class);
            registerClientMessage(SupernaturalSensesTrackPacket.class);
            registerClientMessage(KuugaFormSwitchCooldownMessage.class);

            // 服务端消息 (Client -> Server)
            registerServerMessage(SyncCarriedItemPacket.class);
            registerServerMessage(SyncEquipmentSlotPacket.class);
            registerServerMessage(SetArmorAnimPacket.class);
            registerServerMessage(SetItemAnimPacket.class);
            registerServerMessage(TitanSwordComboPacket.class);
        });
    }

    private static <T extends IMessage> void registerClientMessage(Class<T> messageType) {
        registerMessage(messageType, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static <T extends IMessage> void registerServerMessage(Class<T> messageType) {
        registerMessage(messageType, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <T extends IMessage> void registerMessage(Class<T> messageType, NetworkDirection direction) {
        CHANNEL.registerMessage(
                id++,
                messageType,
                IMessage::encode,
                createDecoder(messageType),
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> msg.handle(ctx.get()));
                    ctx.get().setPacketHandled(true);
                },
                Optional.of(direction)
        );
    }

    private static final Map<Class<?>, Function<FriendlyByteBuf, ?>> DECODER_CACHE = new ConcurrentHashMap<>();

    private static <T> Function<FriendlyByteBuf, T> createDecoder(Class<T> messageType) {
        @SuppressWarnings("unchecked")
        Function<FriendlyByteBuf, T> cached = (Function<FriendlyByteBuf, T>) DECODER_CACHE.get(messageType);
        if (cached != null) return cached;

        Function<FriendlyByteBuf, T> decoder = buf -> {
            try {
                try {
                    Method decodeMethod = messageType.getMethod("decode", FriendlyByteBuf.class);
                    if (java.lang.reflect.Modifier.isStatic(decodeMethod.getModifiers())) {
                        return messageType.cast(decodeMethod.invoke(null, buf));
                    }
                } catch (NoSuchMethodException ignored) {}

                try {
                    Constructor<T> ctor = messageType.getConstructor(FriendlyByteBuf.class);
                    return ctor.newInstance(buf);
                } catch (NoSuchMethodException ignored) {}

                throw new RuntimeException("无法实例化消息 " + messageType.getSimpleName() + ": 缺少 decode 静态方法或 FriendlyByteBuf 构造函数");
            } catch (Exception e) {
                throw new RuntimeException("无法实例化消息 " + messageType.getSimpleName(), e);
            }
        };

        DECODER_CACHE.put(messageType, decoder);
        return decoder;
    }

    /* ========== 消息发送方法 ========== */

    // 发送给服务端（包括发送者）
    public static void sendToServer(IMessage msg) {
        CHANNEL.sendToServer(msg);
    }

    // 发送给指定玩家（包括发送者）
    public static void sendTo(ServerPlayer player, IMessage msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    // 发送给指定玩家（包括发送者）
    public static void sendToPlayer(IMessage msg, ServerPlayer player) {
        sendTo(player, msg); // sendToPlayer是sendTo的别名
    }

    // 发送给所有玩家（包括发送者）
    public static void sendToAll(IMessage msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    // 发送给指定维度的所有玩家
    public static void sendToDimension(ResourceKey<Level> dim, IMessage msg) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dim), msg);
    }

    // 发送给跟踪实体（包括发送者）
    public static void sendToTracking(Entity entity, IMessage msg) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }

    // 发送给跟踪玩家（包括发送者）
    public static void sendToTrackingPlayers(IMessage msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), msg);
    }

    // 发送给所有玩家（包括服务端）
    public static <MSG extends IMessage> void sendToAny(MSG message) {
        if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
            CHANNEL.send(PacketDistributor.ALL.noArg(), message);
        } else {
            CHANNEL.sendToServer(message);
        }
    }
}