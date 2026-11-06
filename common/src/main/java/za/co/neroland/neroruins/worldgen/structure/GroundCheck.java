package za.co.neroland.neroruins.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GroundCheck(int radius, int maxHeightDelta, boolean requireDry, int minDepth, int headroom) {
    public static final Codec<GroundCheck> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 64).fieldOf("radius").forGetter(GroundCheck::radius),
            Codec.intRange(0, 64).fieldOf("max_height_delta").forGetter(GroundCheck::maxHeightDelta),
            Codec.BOOL.optionalFieldOf("require_dry", true).forGetter(GroundCheck::requireDry),
            Codec.intRange(0, 128).optionalFieldOf("min_depth", 0).forGetter(GroundCheck::minDepth),
            Codec.intRange(0, 128).optionalFieldOf("headroom", 0).forGetter(GroundCheck::headroom)
    ).apply(instance, GroundCheck::new));

    public GroundCheck {
        if (radius < 1 || radius > 64 || maxHeightDelta < 0 || maxHeightDelta > 64
                || minDepth < 0 || minDepth > 128 || headroom < 0 || headroom > 128) {
            throw new IllegalArgumentException("Invalid ground-check bounds");
        }
    }

    public boolean accepts(int[] surface, int[] floor, int minY, int worldHeight) {
        if (surface == null || floor == null || surface.length == 0 || surface.length != floor.length
                || worldHeight <= 0) {
            throw new IllegalArgumentException("Invalid terrain samples");
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int index = 0; index < surface.length; index++) {
            int height = surface[index];
            if ((long) height - minY < minDepth) return false;
            if ((long) height + headroom > (long) minY + worldHeight) return false;
            if (requireDry && height > floor[index]) return false;
            min = Math.min(min, height);
            max = Math.max(max, height);
        }
        return (long) max - min <= maxHeightDelta;
    }
}
