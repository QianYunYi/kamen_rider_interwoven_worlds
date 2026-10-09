package com.qianyunyi.rider.internal.registry.config;

import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.api.rider.IFormSwitchBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * 骑士配置类 - 保留所有原有功能
 */
public class RiderConfig {

    private final String riderName;
    private final Item driverItem;
    private final IFormSwitchBehavior formSwitchBehavior;
    private final Map<String, FormConfig> forms = new HashMap<>();

    /**
     * 构造方法
     */
    public RiderConfig(String riderName, Item driverItem, IFormSwitchBehavior formSwitchBehavior) {
        this.riderName = riderName;
        this.driverItem = driverItem;
        this.formSwitchBehavior = formSwitchBehavior;
    }

    /**
     * 添加形态配置
     */
    public void addForm(String formName, FormConfig config) {
        forms.put(formName, config);
    }

    /**
     * 获取形态配置
     */
    public FormConfig getForm(String formName) {
        return forms.get(formName);
    }

    /**
     * 获取当前形态配置
     */
    public FormConfig getCurrentForm(ServerPlayer player) {
        return forms.get(RiderPlayerVariablesProvider.get(player).getActiveFormName());
    }

    /**
     * 检查切换形态类是否不为空
     */
    public boolean isSwitchRider() {
        return formSwitchBehavior != null;
    }

    /**
     * 切换形态
     */
    public void switchForm(ServerPlayer player, String formName) {
        if (!forms.containsKey(formName)) return;
        formSwitchBehavior.handleFormSwitch(player, this, getCurrentForm(player), getForm(formName));
    }

    /**
     * 检查是否符合变身条件
     */
    public boolean canTransform(ServerPlayer player) {
        return getCurrentForm(player).canTransform(player);
    }

    /**
     * 处理变身
     */
    public void handleHenshin(ServerPlayer player) {
        getCurrentForm(player).getBehavior().handleHenshin(player, this, getCurrentForm(player));
    }

    /**
     * 处理解除变身
     */
    public void handleDeactivation(ServerPlayer player) {
        getCurrentForm(player).getBehavior().handleUnForm(player, this, getCurrentForm(player));
    }

    // ==================== Getter 方法 ====================

    public String getRiderName() {
        return riderName;
    }

    public Item getDriverItem() {
        return driverItem;
    }

    public Map<String, FormConfig> getForms() {
        return forms;
    }

    public boolean isForm(String formName) {
        return forms.containsKey(formName);
    }

    public RiderConfig getValue() {
        return this;
    }
}