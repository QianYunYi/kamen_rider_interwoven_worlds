package com.qianyunyi.rider.internal.combat.skill;


import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.data.enums.SkillType;
import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SwordItem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RiderFinishingMoveManager {
	// 使用线程安全的ConcurrentHashMap
	private static final Map<UUID, IRiderSkill> activeSkills = new ConcurrentHashMap<>();
	// 存储玩家空手技能切换状态的Map
	private static final Map<UUID, Boolean> emptyHandToggleMap = new ConcurrentHashMap<>();

	public static void execute(LivingEntity entity) {
		if (entity == null) return;
		if (entity.level().isClientSide) return;

		if (entity instanceof ServerPlayer serverPlayer) {
			RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
			if (data == null) return;

			//判断是否为变身状态
			if(data.getCurrentState() != HenshinState.ACTIVE &&
					data.getCurrentState() != HenshinState.ACTIVE_AWAKENING) {
				data.setSkillActive(false);
				return;
			}
			// 获取当前激活的形态配置
			FormConfig form = getActiveFormConfig(data);
			if (form == null) return;

			// ===== 主副手检查 =====
			if (serverPlayer.getMainHandItem().isEmpty() && serverPlayer.getOffhandItem().isEmpty()) {
				// 获取骑士踢和骑士拳技能
				IRiderSkill kickSkill = form.getSkill(SkillType.RIDER_KICK);
				IRiderSkill punchSkill = form.getSkill(SkillType.RIDER_PUNCH);

				// 判断是否有可用的技能
				IRiderSkill skillToUse;
				SkillType skillTypeToUse;

				if (kickSkill == null && punchSkill == null) {
					// 两个技能都没有，直接返回
					return;
				} else if (kickSkill != null && punchSkill == null) {
					// 只有骑士踢，使用骑士踢
					skillToUse = kickSkill;
					skillTypeToUse = SkillType.RIDER_KICK;
				} else if (kickSkill == null) {
					// 只有骑士拳，使用骑士拳
					skillToUse = punchSkill;
					skillTypeToUse = SkillType.RIDER_PUNCH;
				} else {
					// 两个技能都有，使用切换逻辑
					// 获取当前玩家的空手技能切换状态
					boolean usePunch = emptyHandToggleMap.getOrDefault(serverPlayer.getUUID(), false);

					// 如果玩家在潜行，则切换技能类型
					if (serverPlayer.isCrouching()) {
						usePunch = !usePunch; // 切换状态
						emptyHandToggleMap.put(serverPlayer.getUUID(), usePunch);
						HenshinUtil.sendMessage(serverPlayer, "切换技能: " + (usePunch ? "骑士拳" : "骑士踢"), ChatFormatting.YELLOW);
						return;
					}

					// 根据切换状态决定使用哪个技能
					if (usePunch) {
						skillToUse = punchSkill;
						skillTypeToUse = SkillType.RIDER_PUNCH;
					} else {
						skillToUse = kickSkill;
						skillTypeToUse = SkillType.RIDER_KICK;
					}
				}

				// 检查技能是否可用（形态限制 + 冷却）
                if(!form.canUseSkill(serverPlayer, skillTypeToUse)) return;
                if (!skillToUse.isAvailable(serverPlayer)) return;

                UUID ServerPlayerId = serverPlayer.getUUID();
                // 存储当前激活的技能
                activeSkills.put(ServerPlayerId, skillToUse);
                // 执行技能
                skillToUse.skill(serverPlayer);
            } else if(serverPlayer.getMainHandItem().getItem() instanceof SwordItem) {
				// 获取剑斩技能
				IRiderSkill slash = form.getSkill(SkillType.SWORD_SLASH);
				if(!form.canUseSkill(serverPlayer, SkillType.SWORD_SLASH)) return;
				if (slash == null || !slash.isAvailable(serverPlayer)) return;

				UUID ServerPlayerId = serverPlayer.getUUID();
				// 存储当前激活的技能
				activeSkills.put(ServerPlayerId, slash);
				// 执行技能
				slash.skill(serverPlayer);
			}
		}
	}

	/**
	 * 释放技能（松开按键时调用）
	 * @param entity 实体
	 */
	public static void releaseSkill(LivingEntity entity) {
		if (entity == null) return;
		if (entity.level().isClientSide) return;

		if (entity instanceof ServerPlayer ServerPlayer) {
			UUID ServerPlayerId = ServerPlayer.getUUID();
			// 获取当前激活的技能
			IRiderSkill skill = activeSkills.get(ServerPlayerId);

			if (skill != null) {
				// 调用技能的释放方法
				skill.releaseSkill(ServerPlayer, false);
				// 从映射中移除
				activeSkills.remove(ServerPlayerId);
			}
		}
	}

	/**
	 * 获取玩家是否有可切换的技能
	 * @param player 玩家
	 * @return true表示玩家同时拥有骑士踢和骑士拳，可以切换
	 */
	public static boolean hasToggleableSkills(ServerPlayer player) {
		RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
		if (data == null) return false;

		FormConfig form = getActiveFormConfig(data);
		if (form == null) return false;

		IRiderSkill kickSkill = form.getSkill(SkillType.RIDER_KICK);
		IRiderSkill punchSkill = form.getSkill(SkillType.RIDER_PUNCH);

		// 两个技能都有才能切换
		return kickSkill != null && punchSkill != null;
	}

	/**
	 * 获取玩家当前的空手技能状态
	 * @param player 玩家
	 * @return true表示使用骑士拳，false表示使用骑士踢
	 */
	public static boolean getEmptyHandSkillType(ServerPlayer player) {
		return emptyHandToggleMap.getOrDefault(player.getUUID(), false);
	}

	/**
	 * 设置玩家的空手技能状态
	 * @param player 玩家
	 * @param usePunch true表示使用骑士拳，false表示使用骑士踢
	 */
	public static void setEmptyHandSkillType(ServerPlayer player, boolean usePunch) {
		// 只有在玩家有可切换技能时才设置状态
		if (hasToggleableSkills(player)) {
			emptyHandToggleMap.put(player.getUUID(), usePunch);
		}
	}

	/**
	 * 重置玩家的空手技能切换状态
	 * @param player 玩家
	 */
	public static void resetEmptyHandToggle(ServerPlayer player) {
		emptyHandToggleMap.remove(player.getUUID());
	}

	/**
	 * 获取指定玩家的激活技能
	 * @param ServerPlayer 玩家
	 * @return 激活的技能实例
	 */
	public static IRiderSkill getActiveSkill(ServerPlayer ServerPlayer) {
		return activeSkills.get(ServerPlayer.getUUID());
	}

	/**
	 * 移除指定玩家的激活技能
	 * @param ServerPlayer 玩家
	 */
	public static void removeActiveSkill(ServerPlayer ServerPlayer) {
		activeSkills.remove(ServerPlayer.getUUID());
		// 同时移除切换状态
		emptyHandToggleMap.remove(ServerPlayer.getUUID());
	}

	private static FormConfig getActiveFormConfig(RiderPlayerData data) {
		RiderConfig cfg = RiderRegistryHelper.getConfig(data.getActiveRiderName()).orElse(null);
		return cfg == null ? null : cfg.getForm(data.getActiveFormName());
	}
}