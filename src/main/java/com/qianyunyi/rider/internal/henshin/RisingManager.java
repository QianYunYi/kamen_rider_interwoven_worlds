package com.qianyunyi.rider.internal.henshin;


import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.api.rider.registry.IRiderRegistry;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public class RisingManager {

	private static final Logger log = LoggerFactory.getLogger(RisingManager.class);

	// 状态处理器接口
	@FunctionalInterface
	private interface StateHandler {
		void handle(ServerPlayer player, boolean isCrouching, RiderPlayerData dataManager);
	}

	// 状态处理器映射
	private static final Map<HenshinState, StateHandler> STATE_HANDLERS = createStateHandlers();

	private static Map<HenshinState, StateHandler> createStateHandlers() {
		Map<HenshinState, StateHandler> handlers = new EnumMap<>(HenshinState.class);
		handlers.put(HenshinState.INACTIVE, (player, isCrouching, dataManager) -> handleInactiveState(player, isCrouching, dataManager));
		handlers.put(HenshinState.STANDBY, (player, isCrouching, dataManager) -> handleTransformableState(player, isCrouching, dataManager));
		handlers.put(HenshinState.STANDBY_AWAKENING, (player, isCrouching, dataManager) -> handleTransformableState(player, isCrouching, dataManager));
		handlers.put(HenshinState.ACTIVE, (player, isCrouching, dataManager) -> handleTransformableState(player, isCrouching, dataManager));
		handlers.put(HenshinState.ACTIVE_AWAKENING, (player, isCrouching, dataManager) -> handleTransformableState(player, isCrouching, dataManager));
		return handlers;
	}

	/**
	 * 执行驱动器升华
	 */
	public static void execute(LivingEntity entity) {
		if (!(entity instanceof ServerPlayer serverPlayer) || serverPlayer.level().isClientSide) {
			return;
		}

		if (serverPlayer.getEffect(KamenRiderInterwovenWorldsModMobEffects.HENSHIN_CANNOT.get()) != null) {
			return;
		}

		RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(serverPlayer);
		if (dataManager.isHenshining()||dataManager.isSkillActive()) {
			return;
		}

		HenshinState currentState = dataManager.getCurrentState();
		StateHandler handler = STATE_HANDLERS.get(currentState);
		if (handler != null) {
			handler.handle(serverPlayer, serverPlayer.isCrouching(), dataManager);
		} else {
			HenshinUtil.sendMessage(serverPlayer, "未知变身状态", ChatFormatting.RED);
		}
	}

	/**
	 * 处理未变身状态
	 */
	private static void handleInactiveState(ServerPlayer player, boolean isCrouching,
											RiderPlayerData dataManager) {
		// 未变身状态的处理逻辑
	}

	/**
 * 处理可变换形态的状态（待机/激活）
	 * 仅处理同级轮询切换（C 键），进阶/回滚由轮盘（V 键）处理
	 */
	private static void handleTransformableState(ServerPlayer player, boolean isCrouching,
												 RiderPlayerData dataManager) {
		String activeRiderName = dataManager.getActiveRiderName();
		String activeFormName = dataManager.getActiveFormName();

		if (activeRiderName == null || activeFormName == null) {
			return;
		}

		RiderConfig riderConfig = RiderRegistryHelper.getConfig(activeRiderName).orElse(null);
		if (riderConfig == null || !riderConfig.isSwitchRider()) {
			return;
		}

		FormConfig formConfig = riderConfig.getForm(activeFormName);
		if (formConfig == null) {
			return;
		}

		dataManager.setTargetRiderName(activeRiderName);

		// 仅处理同级轮询切换
		String nextFormName = calculateNextForm(activeFormName, player);
		if (nextFormName.equals(activeFormName)) {
			return;
		}

		dataManager.setTargetFormName(nextFormName);

		FormConfig nextForm = riderConfig.getForm(nextFormName);
		if (nextForm == null) {
			HenshinUtil.sendMessage(player, "未找到形态: " + nextFormName, ChatFormatting.RED);
			return;
		}

		executeFormSwitch(player, riderConfig, nextForm, dataManager);
	}

	/**
	 * 计算下一个同级轮询形态
	 */
	private static String calculateNextForm(String currentFormName, ServerPlayer player) {
		String nextForm = IRiderRegistry.getNextCycleForm(currentFormName, player);
		if (nextForm != null && IRiderRegistry.canCycle(currentFormName)) {
			HenshinUtil.sendMessage(player, nextForm, ChatFormatting.GREEN);
			return nextForm;
		} else {
			HenshinUtil.sendMessage(player, "该形态没有可轮询的派生形态", ChatFormatting.YELLOW);
			return currentFormName;
		}
	}

	/**
	 * 执行形态切换
	 */
	private static void executeFormSwitch(ServerPlayer player, RiderConfig riderConfig,
										  FormConfig nextForm, RiderPlayerData dataManager) {
		HenshinState currentState = dataManager.getCurrentState();
		String nextFormName = nextForm.getFormName();

		if (currentState == HenshinState.STANDBY || currentState == HenshinState.STANDBY_AWAKENING) {
			// 待机状态切换
			dataManager.setActiveFormName(nextFormName);
		} else {
			// 激活状态切换
			riderConfig.switchForm(player, nextFormName);
		}

        KamenRiderInterwovenWorldsMod.LOGGER.info("切换到形态: {}", nextFormName);
	}
}