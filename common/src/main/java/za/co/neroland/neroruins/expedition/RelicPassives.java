package za.co.neroland.neroruins.expedition;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import za.co.neroland.neroruins.data.ExpeditionRecord;
import za.co.neroland.neroruins.data.PlayerArchive;
import za.co.neroland.neroruins.item.RelicItem;
import za.co.neroland.neroruins.lore.LogCatalog;

/**
 * The small passive each chapter relic gives while held in either hand, with only Core installed. Everything is
 * applied by the server on a fixed interval; holding two copies of a relic, or a relic in each hand, gives each
 * passive once. Effects are short and refreshed, so they end soon after the relic is put away.
 *
 * <p>The compass target cache is in memory only, holds no more than the target the holder is being shown, and
 * forgets players who have left. Nothing here is saved or logged.
 */
public final class RelicPassives {
    public static final int INTERVAL = 40;
    private static final int EFFECT_TICKS = 260;
    private static final int COMPASS_REFRESH_TICKS = 1200;
    private static final int REVEAL_RADIUS = 12;

    public enum Passive {
        /** Surveyor's Compass: bearing to the holder's nearest ruin they have not entered. */
        BEARING,
        /** Pale Survey Lens: night vision, for the lunar dark. */
        NIGHT_VISION,
        /** Director's Seal: Luck, which raises luck-weighted loot rolls. */
        LUCK,
        /** Field Recorder: shows how much of the archive the holder has recorded. */
        RECORDING,
        /** Meridian Keystone: fire resistance, from the freighter's burned cargo deck. */
        FIRE_RESISTANCE,
        /** Reach Thermal Core: the holder never freezes. */
        THAW,
        /** Rootglass Vessel: slow regeneration while below half health. */
        REGROWTH,
        /** Twilight Prism: nearby hostile creatures glow. */
        REVEAL,
        /** Resonance Heart: slowly repairs one worn item the holder is wearing or holding. */
        REPAIR
    }

    private record Target(String dimension, Optional<RuinLocator.Found> found, long computedAt) { }

    private static final Map<UUID, Target> COMPASS = new HashMap<>();

    private RelicPassives() { }

    public static Optional<Passive> forRelic(String id) {
        return Optional.ofNullable(switch (id) {
            case "surveyors_compass" -> Passive.BEARING;
            case "pale_survey_lens" -> Passive.NIGHT_VISION;
            case "directors_seal" -> Passive.LUCK;
            case "field_recorder" -> Passive.RECORDING;
            case "meridian_keystone" -> Passive.FIRE_RESISTANCE;
            case "reach_thermal_core" -> Passive.THAW;
            case "rootglass_vessel" -> Passive.REGROWTH;
            case "twilight_prism" -> Passive.REVEAL;
            case "resonance_heart" -> Passive.REPAIR;
            default -> null;
        });
    }

    /** Regrowth only helps a holder below half health. */
    public static boolean needsRegrowth(float health, float maxHealth) {
        return maxHealth > 0 && health > 0 && health < maxHealth / 2;
    }

    /** The compass recomputes when it has no target, the holder changed world, or the target is stale. */
    public static boolean compassStale(boolean cached, boolean sameDimension, long computedAt, long now) {
        return !cached || !sameDimension || now - computedAt >= COMPASS_REFRESH_TICKS || now < computedAt;
    }

    public static void tick(MinecraftServer server, long now) {
        COMPASS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || !player.isAlive()) continue;
            Set<Passive> held = held(player);
            if (!held.contains(Passive.BEARING)) COMPASS.remove(player.getUUID());
            for (Passive passive : held) apply(player, passive, now);
        }
    }

    private static Set<Passive> held(ServerPlayer player) {
        Set<Passive> passives = EnumSet.noneOf(Passive.class);
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof RelicItem) {
                forRelic(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()).ifPresent(passives::add);
            }
        }
        return passives;
    }

    private static void apply(ServerPlayer player, Passive passive, long now) {
        switch (passive) {
            case BEARING -> bearing(player, now);
            case NIGHT_VISION -> effect(player, MobEffects.NIGHT_VISION);
            case LUCK -> effect(player, MobEffects.LUCK);
            case RECORDING -> actionBar(player, Component.translatable("message.neroruins.relic.recording",
                    PlayerArchive.get(player.level().getServer()).export(player.getUUID()).size(),
                    LogCatalog.ids().size()));
            case FIRE_RESISTANCE -> effect(player, MobEffects.FIRE_RESISTANCE);
            case THAW -> player.setTicksFrozen(0);
            case REGROWTH -> {
                if (needsRegrowth(player.getHealth(), player.getMaxHealth())) effect(player, MobEffects.REGENERATION);
            }
            case REVEAL -> {
                var area = player.getBoundingBox().inflate(REVEAL_RADIUS);
                for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area, mob -> mob instanceof Enemy)) {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, INTERVAL + 20, 0, true, false));
                }
            }
            case REPAIR -> repairOne(player);
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect) {
        player.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, 0, true, false, true));
    }

    private static void repairOne(ServerPlayer player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged()
                    && !(stack.getItem() instanceof RelicItem)) {
                stack.setDamageValue(stack.getDamageValue() - 1);
                return;
            }
        }
    }

    private static void bearing(ServerPlayer player, long now) {
        if (!(player.level() instanceof ServerLevel level)) return;
        String dimension = level.dimension().identifier().toString();
        var own = ExpeditionRecord.get(level.getServer()).export(player.getUUID());
        Target target = COMPASS.get(player.getUUID());
        boolean explored = target != null && target.found().isPresent()
                && RuinLocator.explored(own, dimension, target.found().get());
        if (target == null || explored
                || compassStale(true, target.dimension().equals(dimension), target.computedAt(), now)) {
            target = new Target(dimension, RuinLocator.nearest(level, player.blockPosition(),
                    found -> RuinLocator.explored(own, dimension, found)), now);
            COMPASS.put(player.getUUID(), target);
        }
        actionBar(player, target.found()
                .<Component>map(found -> Component.translatable("message.neroruins.relic.compass",
                        Bearing.describe(found.x() - player.getBlockX(), found.z() - player.getBlockZ())))
                .orElseGet(() -> Component.translatable("message.neroruins.relic.compass.none")));
    }

    private static void actionBar(ServerPlayer player, Component text) {
        player.connection.send(new ClientboundSetActionBarTextPacket(text));
    }
}
