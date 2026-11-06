package za.co.neroland.neroruins.vault;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class VaultDelivery {
    public enum Result { DELIVERED, NOT_CLAIMABLE, FAILED, ROLLBACK_FAILED }
    private VaultDelivery() { }

    public static <T> Result commit(List<T> original, List<T> planned, BooleanSupplier claim,
            BiConsumer<Integer, T> write, Runnable restoreClaim, Consumer<Throwable> report) {
        if (original.size() != planned.size()) throw new IllegalArgumentException("Inventory plan size changed");
        if (!claim.getAsBoolean()) return Result.NOT_CLAIMABLE;
        try {
            for (int i = 0; i < planned.size(); i++) write.accept(i, planned.get(i));
            return Result.DELIVERED;
        } catch (RuntimeException | LinkageError failure) {
            report.accept(failure);
            try {
                for (int i = 0; i < original.size(); i++) write.accept(i, original.get(i));
                restoreClaim.run();
                return Result.FAILED;
            } catch (RuntimeException | LinkageError rollbackFailure) {
                report.accept(rollbackFailure);
                return Result.ROLLBACK_FAILED;
            }
        }
    }
}
