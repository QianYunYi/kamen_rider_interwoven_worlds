package com.qianyunyi.rider.internal.henshin;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

public class SummonDriverManager {

    /** 向后兼容入口：使用玩家当前的 targetRiderName，允许回退。 */
    public static void execute(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide || !(entity instanceof Player player)) return;
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        executeWithFallback((ServerPlayer) player, data.getTargetRiderName());
    }

    /**
     * 轮盘选中后调用：客户端已确保骑士有效，服务端只做状态检查，不再选骑士。
     */
    public static void executeForRider(ServerPlayer player, String riderName) {
        if (player == null || riderName == null || riderName.isEmpty()) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);

        // 基础状态检查
        if (data.getCurrentState() != HenshinState.INACTIVE) return;
        if (data.isHenshining() ||
                player.getEffect(KamenRiderInterwovenWorldsModMobEffects.HENSHIN_CANNOT.get()) != null) return;

        List<String> unlocked = data.getUnlockedRiders();
        if (unlocked.isEmpty()) return;

        // 轮盘传来的名字，只做存在性校验，不兜底
        if (!unlocked.contains(riderName)) return;

        summon(player, data, riderName);
    }

    /**
     * 带回退的入口：名字无效时回退到 targetRiderName 或第一个已解锁骑士。
     */
    private static void executeWithFallback(ServerPlayer player, @Nullable String riderName) {
        if (player == null) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);

        if (data.getCurrentState() != HenshinState.INACTIVE) return;
        if (data.isHenshining() ||
                player.getEffect(KamenRiderInterwovenWorldsModMobEffects.HENSHIN_CANNOT.get()) != null) return;

        List<String> unlocked = data.getUnlockedRiders();
        if (unlocked.isEmpty()) return;

        // 回退逻辑：只在这里出现一次
        String target = (riderName != null && !riderName.isEmpty() && unlocked.contains(riderName))
                ? riderName
                : data.getTargetRiderName();

        if (target == null || target.isEmpty() || !unlocked.contains(target)) {
            target = unlocked.get(0);
        }

        summon(player, data, target);
    }

    /** 真正的召唤逻辑，抽出来避免重复。 */
    private static void summon(ServerPlayer player, RiderPlayerData data, String riderName) {
        data.setTargetRiderName(riderName);

        RiderConfig riderCfg = RiderRegistryHelper.getConfig(riderName).orElse(null);
        if (riderCfg == null) return;

        String targetForm = data.getTargetFormName();
        FormConfig formCfg = (targetForm == null || targetForm.isEmpty())
                ? riderCfg.getForm("base")
                : riderCfg.getForm(targetForm);

        if (formCfg == null) formCfg = riderCfg.getForm("base");
        if (formCfg == null) return;

        // 设置当前的骑士和形态名称（确保后续轮盘能正确读取）
        data.setActiveRiderName(riderName);
        data.setActiveFormName(formCfg.getFormName());

        formCfg.getBehavior().enterStandbyState(player, riderCfg, formCfg);
    }
}