package com.qianyunyi.rider.internal.henshin.ability;


import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AbilityManager {

	// 存储每个玩家的当前能力索引（临时状态），使用 ConcurrentHashMap 保证线程安全
	private static final Map<UUID, Integer> playerAbilityIndexes = new ConcurrentHashMap<>();

	/**
	 * 玩家登出时清理索引，防止内存泄漏
	 */
	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() != null) {
			playerAbilityIndexes.remove(event.getEntity().getUUID());
		}
	}

	public static void execute(LivingEntity entity) {
		if (entity == null || entity.level().isClientSide) return;

		if (entity instanceof ServerPlayer serverPlayer) {
			RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
			if (data == null) return;

			// 判断是否为变身状态
			if(data.getCurrentState() != HenshinState.ACTIVE &&
					data.getCurrentState() != HenshinState.ACTIVE_AWAKENING) {
				return;
			}

			// 获取当前形态配置 - 使用您提供的getActiveFormConfig方法
			FormConfig form = getActiveFormConfig(data);
			if (form == null) return;

			// 获取能力列表
			List<IRiderSkill> abilities = form.getAbilities();
			if (abilities.isEmpty()) return;

			// 获取或初始化能力索引
			UUID playerId = serverPlayer.getUUID();
			int currentIndex = playerAbilityIndexes.getOrDefault(playerId, 0);

			if (entity.isCrouching()) {
				// 蹲下状态：切换能力
				if (abilities.size() > 1) {
					currentIndex = (currentIndex + 1) % abilities.size();
					playerAbilityIndexes.put(playerId, currentIndex);

					// 获取当前能力
					IRiderSkill currentAbility = abilities.get(currentIndex);

					// 提示当前能力
					HenshinUtil.sendMessage(serverPlayer,
							"当前能力：" + currentAbility.getDisplayName() ,
							ChatFormatting.RED);
				}
			} else {
				// 非蹲下状态：执行当前能力
				IRiderSkill ability = abilities.get(currentIndex);
				if (ability.isAvailable(serverPlayer)) {
					ability.skill(serverPlayer);
				}
			}
		}
	}

	public static void executeByIndex(LivingEntity entity, int index) {
		if (entity == null || entity.level().isClientSide) return;

		if (entity instanceof ServerPlayer serverPlayer) {
			RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
			if (data == null) return;

			if (data.getCurrentState() != HenshinState.ACTIVE &&
					data.getCurrentState() != HenshinState.ACTIVE_AWAKENING) {
				return;
			}

			FormConfig form = getActiveFormConfig(data);
			if (form == null) return;

			List<IRiderSkill> abilities = form.getAbilities();
			if (abilities.isEmpty() || index < 0 || index >= abilities.size()) return;

			IRiderSkill ability = abilities.get(index);
			if (ability.isAvailable(serverPlayer)) {
				ability.skill(serverPlayer);
			}
		}
	}

	// 使用您提供的getActiveFormConfig方法
	private static FormConfig getActiveFormConfig(RiderPlayerData data) {
		RiderConfig cfg = RiderRegistryHelper.getConfig(data.getActiveRiderName()).orElse(null);
		return cfg == null ? null : cfg.getForm(data.getActiveFormName());
	}
}