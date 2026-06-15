package com.misanthropy.linggango.fastsounds;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Fastsounds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLE_DYNAMIC_MIXING;
    private static final ForgeConfigSpec.IntValue MAX_CONCURRENT_SOUNDS;

    public static boolean enableDynamicMixing = true;
    public static int maxConcurrentSounds = 230;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        ENABLE_DYNAMIC_MIXING = builder.define("enableDynamicMixing", true);
        MAX_CONCURRENT_SOUNDS = builder.defineInRange("maxConcurrentSounds", 230, 8, 245);
        builder.pop();

        SPEC = builder.build();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        bakeConfig();
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        bakeConfig();
    }

    private static void bakeConfig() {
        enableDynamicMixing = ENABLE_DYNAMIC_MIXING.get();
        maxConcurrentSounds = MAX_CONCURRENT_SOUNDS.get();
    }
}