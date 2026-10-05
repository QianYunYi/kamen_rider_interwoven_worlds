package com.qianyunyi.rider.item;

import com.qianyunyi.rider.entity.projectile.DragonWandEntity;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.item.base.BaseTridentAnimationItem;
import com.qianyunyi.rider.item.renderer.DragonWandItemRenderer;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class DragonWandItem extends BaseTridentAnimationItem {

    private static final int TELESCOPIC_DURATION = 10;

    public DragonWandItem() {
        super(new Properties()
                .stacksTo(1)
                .fireResistant()
                .rarity(Rarity.UNCOMMON)
                .durability(500));
    }

    @Override
    protected DragonWandEntity createThrownEntity(Level level, LivingEntity shooter, ItemStack stack) {
        return new DragonWandEntity(level, shooter, stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            ItemStack stack = player.getItemInHand(hand);
            if (isNormalDragonWand(stack)
                    && !"idle_open".equals(BaseTridentAnimationItem.getDefaultAnim(stack))
                    && !"telescopic".equals(BaseTridentAnimationItem.getAnim(stack))) {
                openDragonWand(sp, hand);
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    private static void openDragonWand(ServerPlayer sp, InteractionHand hand) {
        sp.level().playSound(null, sp.blockPosition(),
                KamenRiderInterwovenWorldsModSounds.DRAGON_ROD_OPEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        EquipmentSlot slot = (hand == InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        ItemStack stack = sp.getItemBySlot(slot);
        if (!isNormalDragonWand(stack)) return;

        ItemStack opening = stack;
        HenshinUtil.equipWeaponAndPlayAnimation(sp, slot, stack, "telescopic");
        DelayedTask.run(sp.level(), TELESCOPIC_DURATION, () -> {
            ItemStack current = sp.getItemBySlot(slot);
            if (current == opening && isNormalDragonWand(current)) {
                BaseTridentAnimationItem.setDefaultAnim(current, "idle_open");
                HenshinUtil.equipWeaponAndPlayAnimation(sp, slot, current, "idle_open");
            }
        });
    }

    private static boolean isNormalDragonWand(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof DragonWandItem;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new DragonWandItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}