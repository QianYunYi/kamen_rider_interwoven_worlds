package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.item.base.BaseAnimatedArmorItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端通知服务器：把我某槽盔甲的动画设为 xxx
 */
public class SetArmorAnimPacket implements IMessage {

    private final EquipmentSlot slot;
    private final String anim;

    /** 编码构造：供客户端发送 */
    public SetArmorAnimPacket(EquipmentSlot slot, String anim) {
        this.slot = slot;
        this.anim = anim;
    }

    /** 解码构造：由网络反射调用 */
    public SetArmorAnimPacket(FriendlyByteBuf buf) {
        this.slot = buf.readEnum(EquipmentSlot.class);
        this.anim = buf.readUtf(32);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(slot);
        buf.writeUtf(anim, 32);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        Player player = ctx.getSender();
        if (player == null) return;

        ItemStack stack = player.getItemBySlot(slot);
        if (stack.getItem() instanceof BaseAnimatedArmorItem) {
            // 写服务器权威 NBT
            BaseAnimatedArmorItem.setAnim(stack, anim);
        }
    }
}