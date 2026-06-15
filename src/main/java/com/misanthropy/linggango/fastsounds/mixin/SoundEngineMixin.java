package com.misanthropy.linggango.fastsounds.mixin;

import com.misanthropy.linggango.fastsounds.Config;
import com.misanthropy.linggango.fastsounds.Fastsounds;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Shadow @Final private Map<SoundInstance, ?> instanceToChannel;

    @Shadow public abstract void stop(SoundInstance sound);

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void fastsounds$onPlaySound(SoundInstance sound, CallbackInfo ci) {
        if (sound == null) {
            return;
        } else {
            sound.getLocation();
        }

        Fastsounds.OPTIMIZER.recordSoundProcessed();

        if (Config.enableDynamicMixing) {
            boolean shouldPlay = Fastsounds.OPTIMIZER.shouldPlaySound(
                    sound.getLocation(),
                    sound.getX(),
                    sound.getY(),
                    sound.getZ()
            );

            if (!shouldPlay) {
                ci.cancel();
                return;
            }
        }

        if (this.instanceToChannel.size() >= Config.maxConcurrentSounds) {
            SoundInstance soundToKill = Fastsounds.OPTIMIZER.getSoundToEvict(this.instanceToChannel.keySet());

            if (soundToKill != null) {
                this.stop(soundToKill);
            } else {
                ci.cancel();
            }
        }
    }
}