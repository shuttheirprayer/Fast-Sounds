package com.misanthropy.linggango.fastsounds.mixin;

import com.misanthropy.linggango.fastsounds.Config;
import com.misanthropy.linggango.fastsounds.Fastsounds;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@Mixin(value = SoundEngine.class, priority = 999)
public abstract class SoundEngineMixin {

    @Shadow @Final private Map<SoundInstance, ?> instanceToChannel;
    @Shadow private int tickCount;

    @Shadow public abstract void stop(SoundInstance sound);

    @Unique
    private final Set<SoundInstance> fastsounds$pendingEvictions =
            Collections.newSetFromMap(new IdentityHashMap<>());

    @Unique private double fastsounds$lastCamX = Double.NaN;
    @Unique private double fastsounds$lastCamY = Double.NaN;
    @Unique private double fastsounds$lastCamZ = Double.NaN;
    @Unique private float fastsounds$lastLookX, fastsounds$lastLookY, fastsounds$lastLookZ;
    @Unique private float fastsounds$lastUpX, fastsounds$lastUpY, fastsounds$lastUpZ;

    @Inject(
            method = "play",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getSound()Lnet/minecraft/client/resources/sounds/Sound;"),
            cancellable = true
    )
    private void fastsounds$onPlaySound(SoundInstance sound, CallbackInfo ci) {
        if (sound == null) return;

        if (Minecraft.getInstance().isPaused()) return;

        Fastsounds.OPTIMIZER.recordSoundProcessed();

        try {
            fastsounds$pendingEvictions.removeIf(s -> !this.instanceToChannel.containsKey(s));
        } catch (Exception ignored) {
        }

        if (Config.enableDynamicMixing) {
            if (!Fastsounds.OPTIMIZER.shouldPlaySound(sound)) {
                ci.cancel();
                return;
            }
        }

        int activeCount;
        try {
            activeCount = this.instanceToChannel.size();
        } catch (Exception e) {
            activeCount = 0;
        }

        if (activeCount >= Config.maxConcurrentSounds) {
            SoundInstance soundToKill;
            try {
                soundToKill = Fastsounds.OPTIMIZER.getSoundToEvict(
                        this.instanceToChannel.keySet(), fastsounds$pendingEvictions, sound);
            } catch (Exception e) {
                ci.cancel();
                return;
            }

            if (soundToKill != null) {
                try {
                    fastsounds$pendingEvictions.add(soundToKill);
                } catch (Exception ignored) {
                }
                this.stop(soundToKill);
            } else {
                ci.cancel();
            }
        }
    }

    @Inject(method = "updateSource", at = @At("HEAD"), cancellable = true)
    private void fastsounds$throttleUpdateSource(Camera camera, CallbackInfo ci) {
        if (!Config.enableCameraThrottle) return;

        if (camera == null || !camera.isInitialized()) return;

        Vec3 pos = camera.getPosition();
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();

        if (!Double.isNaN(fastsounds$lastCamX) &&
                Math.abs(fastsounds$lastCamX - pos.x) < 0.0001 &&
                Math.abs(fastsounds$lastCamY - pos.y) < 0.0001 &&
                Math.abs(fastsounds$lastCamZ - pos.z) < 0.0001 &&
                Math.abs(fastsounds$lastLookX - look.x()) < 0.0001f &&
                Math.abs(fastsounds$lastLookY - look.y()) < 0.0001f &&
                Math.abs(fastsounds$lastLookZ - look.z()) < 0.0001f &&
                Math.abs(fastsounds$lastUpX - up.x()) < 0.0001f &&
                Math.abs(fastsounds$lastUpY - up.y()) < 0.0001f &&
                Math.abs(fastsounds$lastUpZ - up.z()) < 0.0001f) {

            ci.cancel();
            return;
        }

        fastsounds$lastCamX = pos.x;
        fastsounds$lastCamY = pos.y;
        fastsounds$lastCamZ = pos.z;
        fastsounds$lastLookX = look.x();
        fastsounds$lastLookY = look.y();
        fastsounds$lastLookZ = look.z();
        fastsounds$lastUpX = up.x();
        fastsounds$lastUpY = up.y();
        fastsounds$lastUpZ = up.z();
    }

    @Redirect(method = "tickNonPaused", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;isStopped()Z"))
    private boolean fastsounds$staggeredIsStopped(ChannelAccess.ChannelHandle handle) {
        if (!Config.enableStaggeredStopped) {
            return handle.isStopped();
        }

        int activeCount;
        try {
            activeCount = this.instanceToChannel.size();
        } catch (Exception e) {
            activeCount = 0;
        }

        if (activeCount >= Config.maxConcurrentSounds - 20) {
            return handle.isStopped();
        }

        if (Math.floorMod(this.tickCount + handle.hashCode(), Config.staggeredTickInterval) == 0) {
            return handle.isStopped();
        }

        return false;
    }
}