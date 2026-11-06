package za.co.neroland.neroruins.data;

import java.util.Optional;

public record VaultClaimPolicy(Mode mode, long cooldownMillis) {
    public enum Mode { PER_PLAYER, FIRST_COME, TIMED }
    private static final long HOUR_MILLIS = 3_600_000L;

    public VaultClaimPolicy {
        if (mode == null || (mode == Mode.TIMED ? cooldownMillis <= 0 : cooldownMillis != 0)) {
            throw new IllegalArgumentException("Invalid vault claim policy");
        }
    }

    public static Optional<VaultClaimPolicy> parse(String value) {
        if (value == null) return Optional.empty();
        if (value.equals("per_player")) return Optional.of(new VaultClaimPolicy(Mode.PER_PLAYER, 0));
        if (value.equals("first_come")) return Optional.of(new VaultClaimPolicy(Mode.FIRST_COME, 0));
        if (!value.matches("timed\\([1-9][0-9]{0,6}\\)")) return Optional.empty();
        long hours = Long.parseLong(value.substring(6, value.length() - 1));
        return Optional.of(new VaultClaimPolicy(Mode.TIMED, hours * HOUR_MILLIS));
    }

    public boolean allows(boolean anyClaim, Long playerClaimTime, long now) {
        if (now < 0) return false;
        return switch (mode) {
            case FIRST_COME -> !anyClaim;
            case PER_PLAYER -> playerClaimTime == null;
            case TIMED -> playerClaimTime == null || (now >= playerClaimTime && now - playerClaimTime >= cooldownMillis);
        };
    }
}
