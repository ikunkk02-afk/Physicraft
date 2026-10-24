package com.shoyun.physicraft.physics;

import com.shoyun.physicraft.Physicraft;
import com.shoyun.physicraft.entity.PhysicsBlockEntity;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Physicraft.MODID)
public final class PhysicsServerEvents {
    private PhysicsServerEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        LevelPhysics.get((ServerLevel) event.getLevel()).preTick();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        LevelPhysics.discard((ServerLevel) event.getLevel());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PhysicsBlockEntity physics)) {
            return;
        }
        LevelPhysics.get((ServerLevel) event.getLevel()).add(physics);
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PhysicsBlockEntity physics)) {
            return;
        }
        LevelPhysics.get((ServerLevel) event.getLevel()).remove(physics);
    }
}
