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
import java.util.List;
import java.util.Map;

/**
 * 形态切换管理器
 * 整合潜行轮询（RisingManager）与通用切换（FormSwitching）逻辑
 */
public class FormSwitchManager {

	private static final Logger log = LoggerFactory.getLogger(FormSwitchManager.class);

	// ==================== 状态处理器 ====================

	@FunctionalInterface
	private interface StateHandler {
		void handle(ServerPlayer player, boolean isCrouching, RiderPlayerData data);
	}

	private static final Map<HenshinState, StateHandler> STATE_HANDLERS = createStateHandlers();

	private static Map<HenshinState, StateHandler> createStateHandlers() {
		Map<HenshinState, StateHandler> handlers = new EnumMap<>(HenshinState.class);
		handlers.put(HenshinState.INACTIVE, FormSwitchManager::handleInactive);
		handlers.put(HenshinState.STANDBY, FormSwitchManager::handleSwitchable);
		handlers.put(HenshinState.STANDBY_AWAKENING, FormSwitchManager::handleSwitchable);
		handlers.put(HenshinState.ACTIVE, FormSwitchManager::handleSwitchable);
		handlers.put(HenshinState.ACTIVE_AWAKENING, FormSwitchManager::handleSwitchable);
		return handlers;
	}

	// ==================== 公共入口 ====================

	/**
	 * 执行形态切换（驱动器升华/通用切换统一入口）
	 */
	public static void execute(LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player) || player.level().isClientSide) return;
		if (player.getEffect(KamenRiderInterwovenWorldsModMobEffects.HENSHIN_CANNOT.get()) != null) return;

		RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
		if (data.isHenshining() || data.isSkillActive()) return;

		StateHandler handler = STATE_HANDLERS.get(data.getCurrentState());
		if (handler != null) {
			handler.handle(player, player.isCrouching(), data);
		} else {
			HenshinUtil.sendMessage(player, "未知变身状态", ChatFormatting.RED);
		}
	}

	/**
	 * 通用形态切换（FormSwitching原逻辑整合）
	 * 按可切换形态列表顺序轮询
	 */
	public static void executeSwitchable(ServerPlayer player) {
		RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
		if (data.isHenshining() || data.isSkillActive()) return;

		// 非激活状态无法切换
		if (!isSwitchableState(data.getCurrentState())) {
			HenshinUtil.sendMessage(player, "非强化形态变身状态，无法切换形态", ChatFormatting.RED);
			return;
		}

		String riderName = data.getActiveRiderName();
		String formName = data.getActiveFormName();

		RiderConfig riderConfig = RiderRegistryHelper.getConfig(riderName).orElse(null);
		if (riderConfig == null || !riderConfig.isSwitchRider()) return;

		FormConfig currentForm = riderConfig.getForm(formName);
		if (currentForm == null) return;

		// 获取可切换形态列表并计算下一个
		List<String> switchableForms = currentForm.getSwitchableForms();
		if (switchableForms.isEmpty()) {
			HenshinUtil.sendMessage(player, "无可切换形态", ChatFormatting.YELLOW);
			return;
		}

		int nextIndex = (data.getCurrentSwitchIndex() + 1) % switchableForms.size();
		String nextFormName = switchableForms.get(nextIndex);

		FormConfig nextForm = riderConfig.getForm(nextFormName);
		if (nextForm == null) {
			HenshinUtil.sendMessage(player, "未找到目标形态: " + nextFormName, ChatFormatting.RED);
			return;
		}

		// 执行切换
		doSwitch(player, riderConfig, currentForm, nextForm, data);
		data.setCurrentSwitchIndex(nextIndex);
	}

	// ==================== 状态处理器实现 ====================

	/** 未变身状态：无操作 */
	private static void handleInactive(ServerPlayer player, boolean isCrouching, RiderPlayerData data) {
		// 未变身状态下不处理形态切换
	}

	/** 可切换状态：执行同级轮询切换 */
	private static void handleSwitchable(ServerPlayer player, boolean isCrouching, RiderPlayerData data) {
		String activeRider = data.getActiveRiderName();
		String activeForm = data.getActiveFormName();

		if (activeRider == null || activeForm == null) return;

		RiderConfig riderConfig = RiderRegistryHelper.getConfig(activeRider).orElse(null);
		if (riderConfig == null || !riderConfig.isSwitchRider()) return;

		FormConfig currentForm = riderConfig.getForm(activeForm);
		if (currentForm == null) return;

		data.setTargetRiderName(activeRider);

		// 统一使用同级轮询切换（进阶/回滚由轮盘处理）
		String nextFormName = calculateNextByCycle(activeForm, player);

		if (nextFormName.equals(activeForm)) return;

		data.setTargetFormName(nextFormName);

		FormConfig nextForm = riderConfig.getForm(nextFormName);
		if (nextForm == null) {
			HenshinUtil.sendMessage(player, "未找到形态: " + nextFormName, ChatFormatting.RED);
			return;
		}

		doSwitch(player, riderConfig, currentForm, nextForm, data);
	}

	// ==================== 形态计算策略 ====================

	/**
	 * 同级轮询策略：通过IRiderRegistry获取下一个同级形态
	 */
	private static String calculateNextByCycle(String currentFormName, ServerPlayer player) {
		String nextForm = IRiderRegistry.getNextCycleForm(currentFormName, player);
		if (nextForm != null && !nextForm.isEmpty()) {
			HenshinUtil.sendMessage(player, nextForm, ChatFormatting.GREEN);
			return nextForm;
		}

		HenshinUtil.sendMessage(player, "该形态没有可轮询的派生形态", ChatFormatting.YELLOW);
		return currentFormName;
	}

	// ==================== 切换执行 ====================

	/**
	 * 执行形态切换（统一底层）
	 */
	private static void doSwitch(ServerPlayer player, RiderConfig riderConfig,
								 FormConfig currentForm, FormConfig nextForm, RiderPlayerData data) {
		HenshinState state = data.getCurrentState();
		String nextFormName = nextForm.getFormName();

		if (state == HenshinState.STANDBY || state == HenshinState.STANDBY_AWAKENING) {
			// 待机状态：仅更新数据，不触发完整变身流程
			data.setActiveFormName(nextFormName);
		} else {
			// 激活状态：调用骑士配置的形态切换
			riderConfig.switchForm(player, nextFormName);
		}

		KamenRiderInterwovenWorldsMod.LOGGER.info("切换到形态: {}", nextFormName);
	}

	// ==================== 工具方法 ====================

	private static boolean isSwitchableState(HenshinState state) {
		return state == HenshinState.ACTIVE || state == HenshinState.ACTIVE_AWAKENING
				|| state == HenshinState.STANDBY || state == HenshinState.STANDBY_AWAKENING;
	}
}