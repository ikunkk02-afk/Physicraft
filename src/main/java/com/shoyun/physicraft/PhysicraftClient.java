package com.shoyun.physicraft;

import com.shoyun.physicraft.client.PhysicsBlockRenderer;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Physicraft.MODID, dist = Dist.CLIENT)
public class PhysicraftClient {
    public PhysicraftClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(PhysicraftClient::onClientSetup);
        modEventBus.addListener(PhysicraftClient::registerRenderers);
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        Physicraft.LOGGER.info("Physicraft client ready for {}", Minecraft.getInstance().getUser().getName());
    }

    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Physicraft.PHYSICS_BLOCK.get(), PhysicsBlockRenderer::new);
    }
}
