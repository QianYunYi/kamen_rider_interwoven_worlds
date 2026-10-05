package com.qianyunyi.rider.internal.registry.config;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Objects;
import java.util.UUID;

/**
 * 属性修饰符配置类
 */
public class AttributeModifierConfig {
    private final Attribute attribute;
    private final double amount;
    private final int operation;
    private final EquipmentSlot slot;
    private final UUID uuid; // 可选：用于持久化修饰符

    public AttributeModifierConfig(Attribute attribute, double amount,
                                   AttributeModifier.Operation operation,
                                   EquipmentSlot slot) {
        this(attribute, amount, operation, slot, UUID.randomUUID());
    }

    public AttributeModifierConfig(Attribute attribute, double amount,
                                   AttributeModifier.Operation operation,
                                   EquipmentSlot slot, UUID uuid) {
        this.attribute = attribute;
        this.amount = amount;
        this.operation = operation.toValue();
        this.slot = slot;
        this.uuid = uuid;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public double getAmount() {
        return amount;
    }

    public AttributeModifier.Operation getOperation() {
        return AttributeModifier.Operation.fromValue(operation);
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * 创建属性修饰符实例
     */
    public AttributeModifier createModifier() {
        return new AttributeModifier(
                uuid,
                attribute.getDescriptionId(),
                amount,
                AttributeModifier.Operation.fromValue(operation)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeModifierConfig that = (AttributeModifierConfig) o;
        return Double.compare(that.amount, amount) == 0 &&
                operation == that.operation &&
                Objects.equals(attribute, that.attribute) &&
                slot == that.slot &&
                Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, amount, operation, slot, uuid);
    }
}