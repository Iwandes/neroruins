package za.co.neroland.neroruins.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public final class ArchiveExport {
    private static final String DIRECTORY = "neroruins-exports";

    private ArchiveExport() { }

    public static Path write(Path worldRoot, UUID player, Map<String, Long> archive,
            Map<ClaimLedger.Location, Long> claims) throws IOException {
        Path directory = directory(worldRoot, true);
        Path output = directory.resolve(player + ".json");
        JsonObject root = new JsonObject();
        root.addProperty("player", player.toString());
        JsonObject entries = new JsonObject();
        new TreeMap<>(archive).forEach(entries::addProperty);
        root.add("archive", entries);
        JsonArray vaultClaims = new JsonArray();
        claims.entrySet().stream().sorted(java.util.Comparator
                .comparing((Map.Entry<ClaimLedger.Location, Long> entry) -> entry.getKey().dimension())
                .thenComparingInt(entry -> entry.getKey().x()).thenComparingInt(entry -> entry.getKey().y())
                .thenComparingInt(entry -> entry.getKey().z())).forEach(entry -> {
                    JsonObject claim = new JsonObject();
                    claim.addProperty("dimension", entry.getKey().dimension());
                    claim.addProperty("x", entry.getKey().x());
                    claim.addProperty("y", entry.getKey().y());
                    claim.addProperty("z", entry.getKey().z());
                    claim.addProperty("claimed_at", entry.getValue());
                    vaultClaims.add(claim);
                });
        root.add("vault_claims", vaultClaims);
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n";
        Files.writeString(output, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        return Path.of(DIRECTORY, output.getFileName().toString());
    }

    public static void erase(Path worldRoot, UUID player) throws IOException {
        Path directory = directory(worldRoot, false);
        if (directory != null) Files.deleteIfExists(directory.resolve(player + ".json"));
    }

    private static Path directory(Path root, boolean create) throws IOException {
        Path directory = root.resolve(DIRECTORY);
        if (create) Files.createDirectories(directory);
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return null;
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Archive export directory is not a directory");
        }
        return directory;
    }
}
