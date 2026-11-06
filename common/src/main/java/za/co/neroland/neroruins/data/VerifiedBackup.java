package za.co.neroland.neroruins.data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import za.co.neroland.nerolandcore.data.SavedDataCompat;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;

final class VerifiedBackup {
    private VerifiedBackup() { }

    static <T extends SavedData> void refresh(ServerLevel level, SavedDataType<T> type, T data, String name) {
        SavedDataRecovery.backupNow(level, type, data, name);
        Tag expected = SavedDataCompat.codec(type, level)
                .encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), data).getOrThrow();
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        Path folder = DimensionType.getStorageFolder(level.dimension(), root);
        // Core's backup API is best-effort and does not report write failure.
        Path file = folder.resolve("data").resolve(name.replace(':', '_').replace('/', '_') + "_backup.dat");
        verify(file, expected);
    }

    static void verify(Path file, Tag expected) {
        try {
            CompoundTag backup = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            if (!Objects.equals(expected, backup.get("data"))) {
                throw new IllegalStateException("Saved-data backup does not match the current state");
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not verify saved-data backup", failure);
        }
    }
}
