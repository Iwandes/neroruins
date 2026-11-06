package za.co.neroland.neroruins.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VerifiedBackupTest {
    @Test
    void acceptsOnlyTheCurrentBackup(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("backup.dat");
        CompoundTag expected = new CompoundTag();
        expected.putString("state", "erased");
        CompoundTag wrapper = new CompoundTag();
        wrapper.put("data", expected.copy());
        NbtIo.writeCompressed(wrapper, file);
        assertDoesNotThrow(() -> VerifiedBackup.verify(file, expected));

        CompoundTag stale = new CompoundTag();
        stale.putString("state", "before erasure");
        wrapper.put("data", stale);
        NbtIo.writeCompressed(wrapper, file);
        assertThrows(IllegalStateException.class, () -> VerifiedBackup.verify(file, expected));
        assertThrows(UncheckedIOException.class, () -> VerifiedBackup.verify(directory.resolve("missing.dat"), expected));
    }
}
