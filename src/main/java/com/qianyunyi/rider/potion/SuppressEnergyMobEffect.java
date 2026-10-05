
package com.qianyunyi.rider.potion;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.potion.action.SuppressEnergyOnEffectActiveEndAction;
import com.qianyunyi.rider.potion.action.SuppressEnergyOnEffectActiveTickAction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;

public class SuppressEnergyMobEffect extends MobEffect {
	public SuppressEnergyMobEffect() {
		super(MobEffectCategory.HARMFUL, -1);
	}

	//开始方法
	@Override
	public void addAttributeModifiers(LivingEntity entity, @NotNull AttributeMap attributeMap, int amplifier) {
		entity.addEffect(new MobEffectInstance(
				KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get(),
				40, 8, false, false));
	}

	//结束方法
	@Override
	public void removeAttributeModifiers(@NotNull LivingEntity entity, @NotNull AttributeMap attributeMap, int amplifier) {
		super.removeAttributeModifiers(entity, attributeMap, amplifier);
		SuppressEnergyOnEffectActiveEndAction.execute(
				entity.level(),entity
		);
	}

	//tick方法
	@Override
	public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
		super.applyEffectTick(entity, amplifier);
		SuppressEnergyOnEffectActiveTickAction.execute(
				entity.level(),entity
		);
		//清除目标再生
		entity.removeEffect(MobEffects.REGENERATION);
	}

	@Override
	public List<ItemStack> getCurativeItems() {
		ArrayList<ItemStack> cures = new ArrayList<ItemStack>();
		return cures;
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}


	@Override
	public void initializeClient(java.util.function.Consumer<IClientMobEffectExtensions> consumer) {
		consumer.accept(new IClientMobEffectExtensions() {
			@Override
			public boolean isVisibleInGui(MobEffectInstance effect) {
				return false;
			}
		});
	}
}
