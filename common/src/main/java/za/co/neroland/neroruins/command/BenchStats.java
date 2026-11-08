package za.co.neroland.neroruins.command;

import java.util.Arrays;

/**
 * The world-free part of {@code /neroruins bench}: where the trials go and how their timings and piece counts are
 * summarised. Trials sit on a square grid of chunk offsets centred on the caller, {@link #STRIDE_CHUNKS} apart, so
 * every trial samples different terrain and the same arguments always sample the same chunks.
 */
public final class BenchStats {
    public static final int DEFAULT_SAMPLES = 32;
    public static final int MAX_SAMPLES = 256;
    /** Sixteen chunks (256 blocks) between trials: wider than any hub's jigsaw bound, so trials never overlap. */
    public static final int STRIDE_CHUNKS = 16;

    /** The summary the command reports. Times cover every attempt; piece and span figures cover produced starts. */
    public record Result(int attempts, int starts, double meanMillis, double p95Millis, double maxMillis,
            double meanPieces, int maxPieces, int maxSpanX, int maxSpanY, int maxSpanZ) { }

    private final long[] nanos;
    private int attempts;
    private int starts;
    private long pieces;
    private int maxPieces;
    private int maxSpanX;
    private int maxSpanY;
    private int maxSpanZ;

    public BenchStats(int samples) {
        checkSamples(samples);
        this.nanos = new long[samples];
    }

    public static void checkSamples(int samples) {
        if (samples < 1 || samples > MAX_SAMPLES) {
            throw new IllegalArgumentException("Bench samples must be between 1 and " + MAX_SAMPLES);
        }
    }

    /** The chunk offset {dx, dz} of trial {@code index} of {@code samples}, on a centred square grid. */
    public static int[] offset(int index, int samples) {
        checkSamples(samples);
        if (index < 0 || index >= samples) throw new IllegalArgumentException("Trial index out of range");
        int side = (int) Math.ceil(Math.sqrt(samples));
        int half = side / 2;
        return new int[] { (index % side - half) * STRIDE_CHUNKS, (index / side - half) * STRIDE_CHUNKS };
    }

    /** Records one attempt's duration, whether or not it produced a start. */
    public void attempt(long elapsedNanos) {
        if (elapsedNanos < 0) throw new IllegalArgumentException("Negative duration");
        if (attempts == nanos.length) throw new IllegalStateException("More attempts than samples");
        nanos[attempts++] = elapsedNanos;
    }

    /** Records a produced start: its piece count and bounding-box spans. */
    public void start(int pieceCount, int spanX, int spanY, int spanZ) {
        if (pieceCount < 0 || spanX < 0 || spanY < 0 || spanZ < 0) throw new IllegalArgumentException("Negative size");
        if (starts == attempts) throw new IllegalStateException("A start must follow its attempt");
        starts++;
        pieces += pieceCount;
        maxPieces = Math.max(maxPieces, pieceCount);
        maxSpanX = Math.max(maxSpanX, spanX);
        maxSpanY = Math.max(maxSpanY, spanY);
        maxSpanZ = Math.max(maxSpanZ, spanZ);
    }

    public Result result() {
        if (attempts == 0) throw new IllegalStateException("No attempts recorded");
        long[] sorted = Arrays.copyOf(nanos, attempts);
        Arrays.sort(sorted);
        long total = 0;
        for (long value : sorted) total += value;
        return new Result(attempts, starts, millis(total) / attempts, millis(percentile(sorted, 95)),
                millis(sorted[sorted.length - 1]), starts == 0 ? 0.0 : (double) pieces / starts, maxPieces,
                maxSpanX, maxSpanY, maxSpanZ);
    }

    /** Nearest-rank percentile of an ascending, non-empty array. */
    static long percentile(long[] ascending, int percent) {
        if (ascending.length == 0) throw new IllegalArgumentException("No values");
        if (percent < 1 || percent > 100) throw new IllegalArgumentException("Percent out of range");
        int rank = (int) Math.ceil(percent / 100.0 * ascending.length);
        return ascending[rank - 1];
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
