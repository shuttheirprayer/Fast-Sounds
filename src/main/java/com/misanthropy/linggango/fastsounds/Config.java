package com.misanthropy.linggango.fastsounds;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = Fastsounds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLE_DYNAMIC_MIXING;
    private static final ForgeConfigSpec.IntValue MAX_CONCURRENT_SOUNDS;
    private static final ForgeConfigSpec.BooleanValue ENABLE_DISTANCE_CULL;
    private static final ForgeConfigSpec.IntValue MAX_SOUND_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue ENABLE_VOLUME_CULL;
    private static final ForgeConfigSpec.BooleanValue ENABLE_SAME_TICK_DEDUP;
    private static final ForgeConfigSpec.IntValue MAX_SAME_TICK_SOUNDS;

    // New optimization configurations
    private static final ForgeConfigSpec.BooleanValue ENABLE_CAMERA_THROTTLE;
    private static final ForgeConfigSpec.BooleanValue ENABLE_STAGGERED_STOPPED;
    private static final ForgeConfigSpec.IntValue STAGGERED_TICK_INTERVAL;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISTANCE_CULL_WHITELIST;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> VOLUME_CULL_WHITELIST;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SAME_TICK_WHITELIST;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> COOLDOWN_WHITELIST;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EVICTION_WHITELIST;

    public static boolean enableDynamicMixing = true;
    public static int maxConcurrentSounds = 230;
    public static boolean enableDistanceCull = true;
    public static int maxSoundDistance = 64;
    public static boolean enableVolumeCull = true;
    public static boolean enableSameTickDedup = true;
    public static int maxSameTickSounds = 3;

    // New optimization static fields
    public static boolean enableCameraThrottle = true;
    public static boolean enableStaggeredStopped = true;
    public static int staggeredTickInterval = 5;

    public static List<? extends String> distanceCullWhitelist = List.of();
    public static List<? extends String> volumeCullWhitelist = List.of("ambientsounds");
    public static List<? extends String> sameTickWhitelist = List.of("ambientsounds");
    public static List<? extends String> cooldownWhitelist = List.of("ambientsounds");
    public static List<? extends String> evictionWhitelist = List.of("ambientsounds");

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        ENABLE_DYNAMIC_MIXING = builder.define("enableDynamicMixing", true);
        MAX_CONCURRENT_SOUNDS = builder.defineInRange("maxConcurrentSounds", 230, 8, 245);
        ENABLE_DISTANCE_CULL = builder.define("enableDistanceCull", true);
        MAX_SOUND_DISTANCE = builder.defineInRange("maxSoundDistance", 64, 16, 256);
        ENABLE_VOLUME_CULL = builder.define("enableVolumeCull", true);
        ENABLE_SAME_TICK_DEDUP = builder.define("enableSameTickDedup", true);
        MAX_SAME_TICK_SOUNDS = builder.defineInRange("maxSameTickSounds", 3, 1, 20);
        builder.pop();

        builder.push("optimizations");
        ENABLE_CAMERA_THROTTLE = builder.comment("Throttles audio listener and camera calculations when the player is standing still.")
                .define("enableCameraThrottle", true);
        ENABLE_STAGGERED_STOPPED = builder.comment("Staggers expensive OpenAL status queries, verifying if a sound is stopped once every few ticks instead of every single frame.")
                .define("enableStaggeredStopped", true);
        STAGGERED_TICK_INTERVAL = builder.comment("The tick interval at which staggered status checks run per sound channel.")
                .defineInRange("staggeredTickInterval", 5, 1, 20);
        builder.pop();

        builder.push("whitelists");
        DISTANCE_CULL_WHITELIST = builder.defineList("distanceCullWhitelist", List.of(), o -> o instanceof String);
        VOLUME_CULL_WHITELIST = builder.defineList("volumeCullWhitelist", List.of("ambientsounds"), o -> o instanceof String);
        SAME_TICK_WHITELIST = builder.defineList("sameTickWhitelist", List.of("ambientsounds"), o -> o instanceof String);
        COOLDOWN_WHITELIST = builder.defineList("cooldownWhitelist", List.of("ambientsounds"), o -> o instanceof String);
        EVICTION_WHITELIST = builder.defineList("evictionWhitelist", List.of("ambientsounds"), o -> o instanceof String);
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
        enableDistanceCull = ENABLE_DISTANCE_CULL.get();
        maxSoundDistance = MAX_SOUND_DISTANCE.get();
        enableVolumeCull = ENABLE_VOLUME_CULL.get();
        enableSameTickDedup = ENABLE_SAME_TICK_DEDUP.get();
        maxSameTickSounds = MAX_SAME_TICK_SOUNDS.get();

        // Bake new optimization properties
        enableCameraThrottle = ENABLE_CAMERA_THROTTLE.get();
        enableStaggeredStopped = ENABLE_STAGGERED_STOPPED.get();
        staggeredTickInterval = STAGGERED_TICK_INTERVAL.get();

        distanceCullWhitelist = DISTANCE_CULL_WHITELIST.get();
        volumeCullWhitelist = VOLUME_CULL_WHITELIST.get();
        sameTickWhitelist = SAME_TICK_WHITELIST.get();
        cooldownWhitelist = COOLDOWN_WHITELIST.get();
        evictionWhitelist = EVICTION_WHITELIST.get();
    }
}