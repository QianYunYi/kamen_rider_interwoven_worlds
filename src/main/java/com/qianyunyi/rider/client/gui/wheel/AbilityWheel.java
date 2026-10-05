package com.qianyunyi.rider.client.gui.wheel;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.network.AbilityMessage;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 技能选择轮盘。
 * 按下 Z 键打开，显示当前形态所有可用技能。
 * 松手时自动选中执行（由 RadialWheelOverlay.close() 触发动作）。
 * 技能图标由 IRiderSkill.getIcon() 提供纹理路径。
 */
public class AbilityWheel extends RadialWheelOverlay {

    public static final AbilityWheel INSTANCE = new AbilityWheel();

    private List<RadialOption> cachedOptions = List.of();
    private String cachedRiderName = "";
    private String cachedFormName = "";

    private AbilityWheel() {}

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

        String riderName = data.getActiveRiderName();
        String formName = data.getActiveFormName();
        if (riderName == null || formName == null) return List.of();

        if (Objects.equals(riderName, cachedRiderName)
                && Objects.equals(formName, cachedFormName)
                && !cachedOptions.isEmpty()) {
            return cachedOptions;
        }

        RiderConfig riderConfig = RiderRegistryHelper.getConfig(riderName).orElse(null);
        if (riderConfig == null) return List.of();

        FormConfig formConfig = riderConfig.getForm(formName);
        if (formConfig == null) return List.of();

        List<IRiderSkill> abilities = formConfig.getAbilities();
        if (abilities.isEmpty()) return List.of();

        List<RadialOption> options = new ArrayList<>(abilities.size());
        for (int i = 0; i < abilities.size(); i++) {
            IRiderSkill skill = abilities.get(i);
            ResourceLocation icon = skill.getIcon();
            final int index = i;
            options.add(RadialOption.ofTexture(
                    icon,
                    Component.literal(skill.getDisplayName()),
                    entity -> {
                        if (Minecraft.getInstance().getConnection() != null) {
                            KamenRiderInterwovenWorldsMod.PACKET_HANDLER
                                    .sendToServer(new AbilityMessage(1, index));
                        }
                    }
            ));
        }

        cachedRiderName = riderName;
        cachedFormName = formName;
        cachedOptions = options;
        return options;
    }
}