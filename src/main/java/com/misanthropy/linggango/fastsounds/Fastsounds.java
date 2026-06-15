package com.misanthropy.linggango.fastsounds;

import com.misanthropy.linggango.fastsounds.optimization.SoundOptimizer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Fastsounds.MODID)
public class Fastsounds {
    public static final String MODID = "fastsounds";
    public static final SoundOptimizer OPTIMIZER = new SoundOptimizer();

    public Fastsounds() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
}