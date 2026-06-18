package com.misanthropy.linggango.fastsounds.optimization;

import com.misanthropy.linggango.fastsounds.Config;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.Set;

public class SoundOptimizer {
    private final Long2LongOpenHashMap recentSounds = new Long2LongOpenHashMap();
    private final Object2IntOpenHashMap<ResourceLocation> tickSoundCounts = new Object2IntOpenHashMap<>();

    private long lastCleanupTime = 0L;
    private long lastGameTick = -1L;

    private final Set<String> criticalSounds = Set.of(
            "player", "note_block", "creeper", "explode", "chest", "tnt", "portal"
    );

    public SoundOptimizer() {
        recentSounds.defaultReturnValue(0L);
        tickSoundCounts.defaultReturnValue(0);
    }

    public synchronized void recordSoundProcessed() {
        long now = System.currentTimeMillis();

        if (now - lastCleanupTime > 2000L) {
            long cutoffTime = now - 1000L;
            try {
                recentSounds.long2LongEntrySet().removeIf(entry -> entry.getLongValue() < cutoffTime);
            } catch (Exception ignored) {
            }
            lastCleanupTime = now;
        }
    }

    private long getCooldownForSource(SoundSource source) {
        if (source == SoundSource.AMBIENT || source == SoundSource.WEATHER) return 200L;
        return 100L;
    }

    private boolean refreshTickIfNeeded() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            tickSoundCounts.clear();
            return false;
        }
        long currentTick = mc.level.getGameTime();
        if (currentTick != lastGameTick) {
            tickSoundCounts.clear();
            lastGameTick = currentTick;
        }
        return true;
    }

    private boolean isExempt(ResourceLocation soundId, List<? extends String> whitelist) {
        if (whitelist == null || whitelist.isEmpty()) return false;
        String idString = soundId.toString();
        for (String entry : whitelist) {
            if (idString.contains(entry)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean shouldPlaySound(SoundInstance sound) {
        if (sound == null) return false;

        sound.getSound();

        SoundSource source = sound.getSource();

        if (source == SoundSource.MASTER || source == SoundSource.MUSIC ||
                source == SoundSource.RECORDS || source == SoundSource.VOICE) {
            return true;
        }

        if (sound.isRelative() || sound.getAttenuation() == SoundInstance.Attenuation.NONE) {
            return true;
        }

        ResourceLocation soundId = sound.getLocation();
        String path = soundId.getPath();
        if (isCriticalSound(path)) return true;

        float volume = sound.getVolume();
        if (Config.enableVolumeCull && volume < 0.05f) {
            if (!isExempt(soundId, Config.volumeCullWhitelist)) {
                return false;
            }
        }

        double x = sound.getX();
        double y = sound.getY();
        double z = sound.getZ();

        if (Config.enableDistanceCull) {
            if (!isExempt(soundId, Config.distanceCullWhitelist)) {
                LocalPlayer player = Minecraft.getInstance().player;

                if (player != null) {
                    double distSq = player.distanceToSqr(x, y, z);
                    double maxDist = Config.maxSoundDistance;
                    if (distSq > maxDist * maxDist) return false;
                }
            }
        }

        if (source == SoundSource.HOSTILE || source == SoundSource.NEUTRAL ||
                source == SoundSource.PLAYERS || source == SoundSource.BLOCKS) {

            int gridX = (int) x >> 2;
            int gridZ = (int) z >> 2;
            long batchKey = ((long) soundId.hashCode() << 32) | ((long) (gridX & 0xFFFF) << 16) | (gridZ & 0xFFFFL);

            long now = System.currentTimeMillis();
            long lastPlayed = recentSounds.get(batchKey);
            long cooldown = getCooldownForSource(source);

            if (lastPlayed != 0L && (now - lastPlayed) < cooldown) {
                if (!isExempt(soundId, Config.cooldownWhitelist)) {
                    return false;
                }
            }

            if (Config.enableSameTickDedup) {
                if (!isExempt(soundId, Config.sameTickWhitelist)) {
                    if (refreshTickIfNeeded()) {
                        int count = tickSoundCounts.getInt(soundId);
                        if (count >= Config.maxSameTickSounds) return false;
                        tickSoundCounts.put(soundId, count + 1);
                    }
                }
            }

            recentSounds.put(batchKey, now);
        }

        return true;
    }

    private boolean isCriticalSound(String path) {
        for (String critical : criticalSounds) {
            if (path.contains(critical)) return true;
        }
        return false;
    }

    public synchronized SoundInstance getSoundToEvict(SoundInstance[] activeSounds, Set<SoundInstance> exclude) {
        LocalPlayer player = Minecraft.getInstance().player;
        double maxDistSq = -1.0;
        SoundInstance furthestSound = null;
        SoundInstance fallbackSound = null;
        int lowestScore = Integer.MAX_VALUE;

        for (SoundInstance sound : activeSounds) {
            if (sound == null) continue;
            if (exclude.contains(sound)) continue;

            if (sound.isRelative() || sound.getAttenuation() == SoundInstance.Attenuation.NONE) {
                continue;
            }

            SoundSource source = sound.getSource();
            if (source == SoundSource.MASTER || source == SoundSource.MUSIC ||
                    source == SoundSource.RECORDS || source == SoundSource.VOICE) {
                continue;
            }

            if (isExempt(sound.getLocation(), Config.evictionWhitelist)) {
                continue;
            }

            if (player != null) {
                double distSq = player.distanceToSqr(sound.getX(), sound.getY(), sound.getZ());
                if (distSq > maxDistSq) {
                    maxDistSq = distSq;
                    furthestSound = sound;
                }
            } else {
                int score = getPriorityScore(source);
                if (score < lowestScore) {
                    lowestScore = score;
                    fallbackSound = sound;
                }
            }
        }
        return player != null ? furthestSound : fallbackSound;
    }

    private int getPriorityScore(SoundSource source) {
        if (source == null) return 5;

        return switch (source) {
            case AMBIENT -> 0;
            case WEATHER -> 1;
            case NEUTRAL -> 2;
            case BLOCKS -> 3;
            case HOSTILE -> 4;
            case PLAYERS -> 5;
            case MUSIC, RECORDS, VOICE, MASTER -> 10;
        };
    }
}