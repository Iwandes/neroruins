package za.co.neroland.neroruins.guardian;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Sentinel: NeroRuins' one mob, a mechanical ruin guard. Melee up close; at range it winds up a
 * charged shot (sparks and a rising tone for one second, rooted in place) and then fires a hitscan
 * arc. The arc hurts only its target and never touches blocks.
 *
 * <p>The three marks are three entity types sharing this class (see decision 031): each type has
 * its own attributes, loot table, texture and scale through vanilla's per-type plumbing, so nothing
 * about the tier needs saving, syncing or reapplying.
 */
public class Sentinel extends Monster {

    /** One Sentinel mark: base stats and the charged-shot damage. */
    public enum Mark {
        // Mk1 hits like a zombie (3) so an unarmoured explorer survives a first encounter; later marks escalate.
        MK1(24.0, 3.0, 4.0, 2.0F, 1.0F),
        MK2(40.0, 5.0, 8.0, 4.0F, 1.1F),
        MK3(64.0, 7.0, 12.0, 6.0F, 1.25F);

        private final double health;
        private final double damage;
        private final double armor;
        private final float shotDamage;
        private final float scale;

        Mark(double health, double damage, double armor, float shotDamage, float scale) {
            this.health = health;
            this.damage = damage;
            this.armor = armor;
            this.shotDamage = shotDamage;
            this.scale = scale;
        }

        public double health() { return health; }
        public double damage() { return damage; }
        public double armor() { return armor; }
        public float shotDamage() { return shotDamage; }
        /** Client model scale; the hitbox is scaled to match in {@code ModEntities}. */
        public float scale() { return scale; }
        /** 1-based tier used by spawn tables. */
        public int tier() { return ordinal() + 1; }

        public AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes()
                    .add(Attributes.MAX_HEALTH, health)
                    .add(Attributes.ATTACK_DAMAGE, damage)
                    .add(Attributes.ARMOR, armor)
                    .add(Attributes.MOVEMENT_SPEED, 0.23)
                    .add(Attributes.FOLLOW_RANGE, 24.0)
                    .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
        }
    }

    private final Mark mark;

    public Sentinel(EntityType<? extends Sentinel> type, Level level, Mark mark) {
        super(type, level);
        this.mark = mark;
        this.xpReward = 5 * mark.tier();
    }

    public Mark mark() { return mark; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new ChargedShotGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Sentinel.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.IRON_GOLEM_STEP; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 0.6F, 1.4F);
    }

    /** Wind-up, then a hitscan arc at the target. Rooted while charging so the tell is readable. */
    static final class ChargedShotGoal extends Goal {
        private final Sentinel sentinel;
        private int charge;
        private int cooldown;

        ChargedShotGoal(Sentinel sentinel) {
            this.sentinel = sentinel;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = sentinel.getTarget();
            return target != null && target.isAlive() && GuardianRules.inShotRange(sentinel.distanceToSqr(target))
                    && sentinel.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = sentinel.getTarget();
            return charge < GuardianRules.WIND_UP_TICKS && target != null && target.isAlive()
                    && sentinel.distanceToSqr(target) <= GuardianRules.SHOT_MAX_RANGE * GuardianRules.SHOT_MAX_RANGE;
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void start() {
            charge = 0;
            sentinel.getNavigation().stop();
            sentinel.playSound(SoundEvents.BEACON_ACTIVATE, 0.8F, 1.6F);
        }

        @Override
        public void tick() {
            LivingEntity target = sentinel.getTarget();
            if (target == null) return;
            sentinel.getNavigation().stop();
            sentinel.getLookControl().setLookAt(target, 30.0F, 30.0F);
            charge++;
            if (!(sentinel.level() instanceof ServerLevel level)) return;
            if (charge % 2 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, sentinel.getX(), sentinel.getEyeY(), sentinel.getZ(),
                        3, 0.25, 0.25, 0.25, 0.05);
            }
            if (charge == GuardianRules.WIND_UP_TICKS) fire(level, target);
        }

        @Override
        public void stop() {
            cooldown = GuardianRules.SHOT_COOLDOWN_TICKS;
            charge = 0;
        }

        private void fire(ServerLevel level, LivingEntity target) {
            if (!sentinel.getSensing().hasLineOfSight(target)) return;
            Vec3 from = sentinel.getEyePosition();
            Vec3 to = target.getEyePosition();
            Vec3 step = to.subtract(from);
            int points = Math.max(4, (int) (step.length() * 3));
            for (int i = 0; i <= points; i++) {
                Vec3 at = from.add(step.scale(i / (double) points));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            sentinel.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.8F);
            target.hurtServer(level, sentinel.damageSources().mobAttack(sentinel), sentinel.mark().shotDamage());
        }
    }
}
