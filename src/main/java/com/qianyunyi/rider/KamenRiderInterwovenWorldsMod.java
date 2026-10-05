package com.qianyunyi.rider;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.AbstractMap;

import com.qianyunyi.rider.config.AdventureConfig;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiHuntingLaw;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModTabs;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModParticleTypes;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMenus;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModBlocks;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModBlockEntities;
import com.qianyunyi.rider.internal.surgery.ModRecipeTypes;

@Mod("kamen_rider_interwoven_worlds")
public class KamenRiderInterwovenWorldsMod {
	public static final Logger LOGGER = LogManager.getLogger(KamenRiderInterwovenWorldsMod.class);
	public static final String MODID = "kamen_rider_interwoven_worlds";

	public KamenRiderInterwovenWorldsMod() {
		// Start of user code block mod constructor
		// 注册配置文件
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AdventureConfig.SPEC,
				"kamen_rider_interwoven_worlds-common.toml");
		// End of user code block mod constructor
		MinecraftForge.EVENT_BUS.register(this);
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		KamenRiderInterwovenWorldsModSounds.REGISTRY.register(bus);
		KamenRiderInterwovenWorldsModBlocks.REGISTRY.register(bus);
		KamenRiderInterwovenWorldsModBlockEntities.REGISTRY.register(bus);
		KamenRiderInterwovenWorldsModItems.REGISTRY.register(bus);
		KamenRiderInterwovenWorldsModEntities.REGISTRY.register(bus);
		KamenRiderInterwovenWorldsModTabs.REGISTRY.register(bus);
		KamenRiderInterwovenWorldsModMobEffects.REGISTRY.register(bus);
		KamenRiderInterwovenWorldsModParticleTypes.REGISTRY.register(bus);
		KamenRiderInterwovenWorldsModMenus.REGISTRY.register(bus);
		ModRecipeTypes.RECIPE_TYPES.register(bus);
		ModRecipeTypes.RECIPE_SERIALIZERS.register(bus);

		// Start of user code block mod init
		// 在 mod 事件总线上注册配置重载监听
		bus.addListener(this::onConfigEvent);
		// End of user code block mod init
	}

	// Start of user code block mod methods

	/**
	 * 配置文件加载/重载时，同步更新 GurongiHuntingLaw 的实体类型注册表。
	 */
	@SubscribeEvent
	public void onConfigEvent(ModConfigEvent event) {
		if (event.getConfig().getSpec() == AdventureConfig.SPEC) {
			java.util.List<String> lintoList = new java.util.ArrayList<>(
					AdventureConfig.GURONGI_LINTO_ENTITY_TYPES.get());
			java.util.List<String> illagerList = new java.util.ArrayList<>(
					AdventureConfig.GURONGI_ILLAGER_ENTITY_TYPES.get());
			GurongiHuntingLaw.reloadFromConfig(lintoList, illagerList);
			LOGGER.info("[{}] 古朗基狩猎法则配置已重载", MODID);
		}
	}

	// End of user code block mod methods
	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
	private static int messageID = 0;

	public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
		PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
		messageID++;
	}

	private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

	public static void queueServerWork(int tick, Runnable action) {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
			workQueue.add(new AbstractMap.SimpleEntry<>(action, tick));
	}

	@SubscribeEvent
	public void tick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			// 处理延迟任务队列
			List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
			workQueue.forEach(work -> {
				work.setValue(work.getValue() - 1);
				if (work.getValue() == 0)
					actions.add(work);
			});
			actions.forEach(e -> e.getKey().run());
			workQueue.removeAll(actions);
		}
	}
}