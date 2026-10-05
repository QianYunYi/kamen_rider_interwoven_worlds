package com.qianyunyi.rider.client.gui.wheel;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.network.SummonDriverSelectMessage;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 骑士驱动器选择轮盘。
 */
public class DriverSelectWheel extends RadialWheelOverlay {

    public static final DriverSelectWheel INSTANCE = new DriverSelectWheel();

    private final Map<RiderConfig, ItemStack> iconCache = new HashMap<>();
    private List<RadialOption> cachedOptions = List.of();
    private List<String> cachedRiderList = List.of();

    private DriverSelectWheel() {}

    public void invalidateCache() {
        iconCache.clear();
        cachedOptions = List.of();
        cachedRiderList = List.of();
    }

    @Override
    public List<RadialOption> getOptions() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return List.of();

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        List<String> unlocked = data.getUnlockedRiders();
        if (unlocked.isEmpty()) return List.of();

        // 用列表内容直接比较，避免 hashCode 碰撞风险
        if (Objects.equals(unlocked, cachedRiderList) && !cachedOptions.isEmpty()) {
            return cachedOptions;
        }

        List<RadialOption> options = new ArrayList<>(unlocked.size());
        for (String riderName : unlocked) {
            RiderConfig cfg = RiderRegistryHelper.getConfig(riderName).orElse(null);
            if (cfg == null) continue;

            ItemStack icon = iconCache.computeIfAbsent(cfg, c -> {
                Item item = c.getDriverItem();
                return item != null ? new ItemStack(item) : ItemStack.EMPTY;
            });

            options.add(RadialOption.of(
                    icon,
                    Component.literal(cfg.getRiderName()),
                    serverPlayer -> KamenRiderInterwovenWorldsMod.PACKET_HANDLER.sendToServer(
                            new SummonDriverSelectMessage(cfg.getRiderName())
                    )
            ));
        }

        cachedRiderList = new ArrayList<>(unlocked); // 保存副本防外部修改
        cachedOptions = options;
        return options;
    }
}