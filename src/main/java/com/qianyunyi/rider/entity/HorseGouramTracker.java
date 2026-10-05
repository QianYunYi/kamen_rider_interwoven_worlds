package com.qianyunyi.rider.entity;

import net.minecraft.world.entity.animal.horse.Horse;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HorseGouramTracker {
    private static final Set<UUID> HORSES_WITH_GOURAM = new HashSet<>();

    public static boolean hasGouram(Horse horse) {
        return HORSES_WITH_GOURAM.contains(horse.getUUID());
    }

    public static void attach(Horse horse) {
        HORSES_WITH_GOURAM.add(horse.getUUID());
    }

    public static void detach(Horse horse) {
        HORSES_WITH_GOURAM.remove(horse.getUUID());
    }
}