package com.misanthropy.linggango.fastsounds.optimization;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.util.Set;

public class SoundOptimizer {
    private final Long2LongOpenHashMap recentSounds = new Long2LongOpenHashMap();
    private long totalSoundsProcessed = 0L;
    private long batchedSoundsCount = 0L;
    private long lastCleanupTime = 0L;
    private final Set<String> criticalSounds = Set.of(
            "player", "note_block", "creeper", "explode", "chest", "tnt", "portal"
    );

    public SoundOptimizer() {
        recentSounds.defaultReturnValue(0L);
    }

    public void recordSoundProcessed() {
        totalSoundsProcessed++;
        long now = System.currentTimeMillis();

        if (now - lastCleanupTime > 2000L) {
            long cutoffTime = now - 1000L;
            recentSounds.long2LongEntrySet().removeIf(entry -> entry.getLongValue() < cutoffTime);
            lastCleanupTime = now;
        }
    }

    public boolean shouldPlaySound(ResourceLocation soundId, double x, double y, double z) {
        String path = soundId.getPath();

        if (isCriticalSound(path)) return true;

        int gridX = (int) x >> 2;
        int gridZ = (int) z >> 2;
        long batchKey = ((long) path.hashCode() << 32) | ((long) (gridX & 0xFFFF) << 16) | (gridZ & 0xFFFFL);

        long now = System.currentTimeMillis();
        long lastPlayed = recentSounds.get(batchKey);

        if (lastPlayed != 0L && (now - lastPlayed) < 100L) {
            batchedSoundsCount++;
            return false;
        }

        recentSounds.put(batchKey, now);
        return true;
    }

    private boolean isCriticalSound(String path) {
        for (String critical : criticalSounds) {
            if (path.contains(critical)) return true;
        }
        return false;
    }

    public SoundInstance getSoundToEvict(Set<SoundInstance> activeSounds) {
        SoundInstance lowestPrioritySound = null;
        int lowestScore = Integer.MAX_VALUE;

        for (SoundInstance sound : activeSounds) {
            int score = getPriorityScore(sound.getSource());

            if (score < lowestScore) {
                lowestScore = score;
                lowestPrioritySound = sound;
            }

            if (lowestScore == 0) {
                break;
            }
        }
        return lowestPrioritySound;
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

    public long getTotalProcessed() { return totalSoundsProcessed; }
    public long getBatchedCount() { return batchedSoundsCount; }
}