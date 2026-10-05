package com.qianyunyi.rider.client.gui.wheel;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.api.rider.registry.IRiderRegistry;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.network.FormSwitchMessage;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 形态选择轮盘（双模式）。
 * <p>
 * 根据 targetRiderName 和 targetFormName 显示可选形态：
 * <ul>
 *   <li>{@link Mode#CYCLE} — Shift+V 触发，显示同级形态（如青龙→天马→泰坦）</li>
 *   <li>{@link Mode#ADVANCEMENT} — V 触发，显示当前进化路线所有已解锁形态</li>
 * </ul>
 * 未变身时也可以打开，根据 targetRiderName 显示形态，选择后进入对应形态待机。
 * 松手时自动选中执行（由 {@link RadialWheelOverlay#close()} 触发动作）。
 */
public class FormSwitchWheel extends RadialWheelOverlay {

    public static final FormSwitchWheel INSTANCE = new FormSwitchWheel();

    /** 轮盘模式 */
    public enum Mode {
        /** 同级形态轮盘（Shift+V） */
        CYCLE,
        /** 进化路线轮盘（V） */
        ADVANCEMENT
    }

    private Mode currentMode = Mode.CYCLE;

    // 形态切换冷却：仅在服务端完成切换后由 KuugaFormSwitchCooldownMessage 写入客户端
    private static long formSwitchCooldownEndTick = 0L;

    /**
     * 设置形态切换轮盘的冷却结束时间（客户端游戏刻）。
     * @param endTick 冷却结束时的游戏刻
     */
    public static void setFormSwitchCooldown(long endTick) {
        formSwitchCooldownEndTick = endTick;
    }

    /**
     * 判断当前是否处于形态切换冷却中（冷却内禁止打开轮盘）。
     * @return true 表示仍在冷却
     */
    public static boolean isOnFormSwitchCooldown() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        return mc.level.getGameTime() < formSwitchCooldownEndTick;
    }

    // 形态选择缓存
    private List<RadialOption> cachedOptions = List.of();
    private String cachedRiderName = "";
    private String cachedFormName = "";
    private Mode cachedMode = Mode.CYCLE;
    private HenshinState cachedState = HenshinState.INACTIVE;

    private FormSwitchWheel() {}

    /**
     * 设置轮盘模式（在打开轮盘前调用）
     */
    public void setMode(Mode mode) {
        this.currentMode = mode;
        invalidateCache();
    }

    public void invalidateCache() {
        cachedOptions = List.of();
        cachedRiderName = "";
        cachedFormName = "";
    }

    @Override
    public List<RadialOption> getOptions() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return List.of();

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) return List.of();

        // ==================== 任何状态：基于 targetRiderName 显示形态选择 ====================
        String targetRider = data.getTargetRiderName();
        if (targetRider == null || targetRider.isEmpty()) {
            targetRider = data.getActiveRiderName();
        }
        if (targetRider == null || targetRider.isEmpty()) return List.of();
        final String resolvedRider = targetRider;

        RiderConfig riderConfig = RiderRegistryHelper.getConfig(targetRider).orElse(null);
        if (riderConfig == null) return List.of();

        String targetForm = data.getTargetFormName();
        if (targetForm == null || targetForm.isEmpty()) {
            targetForm = data.getActiveFormName();
            if (targetForm == null || targetForm.isEmpty()) {
                targetForm = "base";
            }
        }

        HenshinState state = data.getCurrentState();

        // 缓存命中：如果骑士、形态、模式、状态均未变，直接返回缓存
        if (Objects.equals(resolvedRider, cachedRiderName)
                && Objects.equals(targetForm, cachedFormName)
                && currentMode == cachedMode
                && state == cachedState
                && !cachedOptions.isEmpty()) {
            return cachedOptions;
        }

        // 根据模式获取形态列表
        List<String> formNames;
        if (currentMode == Mode.CYCLE) {
            // 同级形态：从 IRiderRegistry 获取（返回所有同级形态，需过滤解锁状态）
            formNames = IRiderRegistry.getCycleForms(targetForm);
        } else {
            // 进化路线：从 IRiderRegistry 获取，已过滤未解锁形态
            formNames = IRiderRegistry.getAdvancementLine(targetForm, player);
        }

        // 过滤：只保留已解锁的形态（CYCLE 模式 getCycleForms 不检查解锁状态）
        formNames = formNames.stream()
                .filter(name -> IRiderRegistry.isFormUnlocked(name, player))
                .toList();

        // 构建轮盘选项（不排除当前形态，允许重新选择自身）
        List<RadialOption> options = new ArrayList<>();
        for (String formName : formNames) {
            FormConfig formConfig = riderConfig.getForm(formName);
            ItemStack icon = ItemStack.EMPTY;
            if (formConfig != null && formConfig.getArmorSet() != null
                    && formConfig.getArmorSet().length > 0) {
                icon = new ItemStack(formConfig.getArmorSet()[0]); // 头盔物品作为图标
            }

            final String selectedForm = formName;
            options.add(RadialOption.of(
                    icon,
                    Component.literal(formName),
                    entity -> {
                        // 客户端更新 targetRiderName 和 targetFormName
                        RiderPlayerData d = RiderPlayerVariablesProvider.get(player);
                        if (d != null) {
                            // targetRiderName 已经是选择好的，这里再确认一次
                            d.setTargetRiderName(resolvedRider);
                            d.setTargetFormName(selectedForm);
                        }
                        // 发送形态选择消息到服务端
                        KamenRiderInterwovenWorldsMod.PACKET_HANDLER.sendToServer(
                                new FormSwitchMessage(selectedForm)
                        );
                    }
            ));
        }

        cachedRiderName = resolvedRider;
        cachedFormName = targetForm;
        cachedMode = currentMode;
        cachedState = state;
        cachedOptions = options;
        return options;
    }
}