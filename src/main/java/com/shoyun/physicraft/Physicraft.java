package com.shoyun.physicraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.shoyun.physicraft.entity.PhysicsBlockEntity;
import com.shoyun.physicraft.item.PhysicsConverterItem;
import com.shoyun.physicraft.physics.BulletNative;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Physicraft.MODID)
public class Physicraft {
    public static final String MODID = "physicraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredItem<PhysicsConverterItem> PHYSICS_CONVERTER = ITEMS.register("physics_converter",
            () -> new PhysicsConverterItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<EntityType<?>, EntityType<PhysicsBlockEntity>> PHYSICS_BLOCK = ENTITY_TYPES.register(
            "physics_block",
            () -> EntityType.Builder.of(PhysicsBlockEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("physics_block"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("physicraft",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.physicraft"))
                    .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
                    .icon(() -> PHYSICS_CONVERTER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(PHYSICS_CONVERTER.get()))
                    .build());

    public Physicraft(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (BulletNative.ensureLoaded()) {
                LOGGER.info("Libbulletjme {} loaded for {}", BulletNative.version(), BulletNative.platform());
            } else {
                LOGGER.error("Libbulletjme failed to load: {}", BulletNative.failureReason());
            }
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PHYSICS_CONVERTER.get());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Physicraft server starting (native loaded: {})", BulletNative.isLoaded());
    }
}
