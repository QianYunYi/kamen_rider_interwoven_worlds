package com.qianyunyi.rider.internal.surgery;


import com.qianyunyi.rider.world.inventory.SurgicalConsoleInterfaceMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * 改造手术事件
 */
public class SurgeryEvent extends Event {

    private final ServerPlayer player;
    private final SurgeryRecipe recipe;
    private final SurgicalConsoleInterfaceMenu menu;

    public SurgeryEvent(ServerPlayer player, SurgeryRecipe recipe, SurgicalConsoleInterfaceMenu menu) {
        this.player = player;
        this.recipe = recipe;
        this.menu = menu;
    }

    public ServerPlayer getPlayer() { return player; }
    public SurgeryRecipe getRecipe() { return recipe; }
    public SurgicalConsoleInterfaceMenu getMenu() { return menu; }

    /**
     * 手术开始事件（可取消）
     */
    public static class Start extends SurgeryEvent {
        public Start(ServerPlayer player, SurgeryRecipe recipe, SurgicalConsoleInterfaceMenu menu) {
            super(player, recipe, menu);
        }

        @Override
        public boolean isCancelable() {
            return true;
        }
    }

    /**
     * 手术完成事件
     */
    public static class Complete extends SurgeryEvent {
        public Complete(ServerPlayer player, SurgeryRecipe recipe, SurgicalConsoleInterfaceMenu menu) {
            super(player, recipe, menu);
        }
    }
}