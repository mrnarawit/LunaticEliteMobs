package com.lunaticelitemobs.elite;

import com.lunaticelitemobs.LunaticEliteMobs;
import com.lunaticelitemobs.config.EliteConfig;
import com.lunaticelitemobs.network.EliteNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Npc;
import net.neoforged.neoforge.common.Tags;

public final class EliteManager {
    public static final TagKey<EntityType<?>> TAG_BLACKLIST = TagKey.create(Registries.ENTITY_TYPE, LunaticEliteMobs.id("blacklist"));
    public static final TagKey<EntityType<?>> TAG_WHITELIST = TagKey.create(Registries.ENTITY_TYPE, LunaticEliteMobs.id("whitelist"));

    private static final ResourceLocation HEALTH_ID = LunaticEliteMobs.id("elite_health");
    private static final ResourceLocation ATTACK_ID = LunaticEliteMobs.id("elite_attack");
    private static final ResourceLocation SPEED_ID = LunaticEliteMobs.id("elite_speed");
    private static final ResourceLocation FLY_SPEED_ID = LunaticEliteMobs.id("elite_flying_speed");
    private static final ResourceLocation ARMOR_ID = LunaticEliteMobs.id("elite_armor");
    private static final ResourceLocation TOUGHNESS_ID = LunaticEliteMobs.id("elite_toughness");
    private static final ResourceLocation KNOCKBACK_ID = LunaticEliteMobs.id("elite_knockback_resistance");

    private EliteManager() {}

    /** Elite tier of an entity, 0 if not elite. Does not create the attachment. */
    public static int getTier(Entity e) {
        return e.hasData(ModAttachments.ELITE_TIER) ? e.getData(ModAttachments.ELITE_TIER) : 0;
    }

    // ------------------------------------------------------------------ eligibility

    public static boolean isBoss(Mob mob) {
        if (mob instanceof EnderDragon || mob instanceof WitherBoss) return true;
        if (mob.getType().is(Tags.EntityTypes.BOSSES)) return true;
        return EliteConfig.cache().extraBosses.matches(BuiltInRegistries.ENTITY_TYPE, mob.getType());
    }

    /** Listed in entityWhitelist (id / #tag / @mod) or the data tag. */
    public static boolean isWhitelisted(EntityType<?> type) {
        return type.is(TAG_WHITELIST) || EliteConfig.cache().whitelist.matches(BuiltInRegistries.ENTITY_TYPE, type);
    }

    /** Listed in entityBlacklist / modBlacklist (id / #tag / @mod) or the data tag. */
    public static boolean isBlacklisted(EntityType<?> type) {
        return type.is(TAG_BLACKLIST) || EliteConfig.cache().blacklist.matches(BuiltInRegistries.ENTITY_TYPE, type);
    }

    /** Neutral, villager-like and tamed mobs: never elites, no matter what the config says. */
    public static boolean isNeverAllowed(Mob mob) {
        if (mob instanceof NeutralMob) return true;
        if (mob instanceof Npc || mob instanceof AbstractVillager) return true;
        if (mob instanceof TamableAnimal) return true;
        return mob instanceof OwnableEntity own && own.getOwnerUUID() != null;
    }

    /** Hostile-only check. Neutral, villager-like, animal and tamed mobs are never accepted. */
    public static boolean isHostile(Mob mob) {
        if (isNeverAllowed(mob)) return false;

        EntityType<?> type = mob.getType();
        if (isWhitelisted(type)) return true;
        if (EliteConfig.WHITELIST_ONLY.get()) return false;

        if (mob instanceof Enemy) return true; // zombies, skeletons, hoglins, shulkers, slimes...
        if (type.getCategory() == MobCategory.MONSTER) {
            return !(mob instanceof Animal) && !(mob instanceof AbstractGolem) && !(mob instanceof WaterAnimal);
        }
        return false;
    }

    public static boolean isEligible(Mob mob) {
        if (!EliteConfig.ENABLED.get()) return false;
        EliteConfig.Cache c = EliteConfig.cache();
        if (isBlacklisted(mob.getType())) return false;
        if (c.dimensionBlacklist.contains(mob.level().dimension().location().toString())) return false;
        if (!EliteConfig.ALLOW_BABIES.get() && mob.isBaby()) return false;
        if (EliteConfig.EXCLUDE_BOSSES.get() && isBoss(mob)) return false;
        if (mob.getMaxHealth() > EliteConfig.MAX_BASE_HEALTH.get()) return false;
        return isHostile(mob);
    }

    /** Tier from health: (max health x check multiplier) compared to thresholds. */
    public static int healthTier(Mob mob) {
        double checked = mob.getMaxHealth() * EliteConfig.TIER_CHECK_MULTIPLIER.get();
        if (checked > EliteConfig.TIER4_HEALTH.get()) return 4;
        if (checked > EliteConfig.TIER3_HEALTH.get()) return 3;
        if (checked > EliteConfig.TIER2_HEALTH.get()) return 2;
        return 1;
    }

    /**
     * Called when a mob spawns. Either rolls now or, with rollDelayTicks > 0, schedules the roll so
     * other mods (In Control, level scaling...) finish changing the mob first.
     * forcedTier: 0 = normal roll, -1 = guaranteed with random tier, 1-4 = guaranteed that tier.
     */
    public static void scheduleRoll(Mob mob, int forcedTier) {
        mob.setData(ModAttachments.PROCESSED, true);
        int delay = EliteConfig.ROLL_DELAY_TICKS.get();
        if (delay <= 0) {
            runRoll(mob, forcedTier);
            return;
        }
        mob.setData(ModAttachments.PENDING_ROLL, delay);
        if (forcedTier != 0) mob.setData(ModAttachments.FORCED_TIER, forcedTier);
    }

    /** Ticks down a scheduled roll. Call every server tick for mobs that have PENDING_ROLL. */
    public static void tickPending(Mob mob) {
        int left = mob.getData(ModAttachments.PENDING_ROLL) - 1;
        if (left > 0) {
            mob.setData(ModAttachments.PENDING_ROLL, left);
            return;
        }
        mob.removeData(ModAttachments.PENDING_ROLL);
        int forced = 0;
        if (mob.hasData(ModAttachments.FORCED_TIER)) {
            forced = mob.getData(ModAttachments.FORCED_TIER);
            mob.removeData(ModAttachments.FORCED_TIER);
        }
        if (mob.isAlive()) runRoll(mob, forced);
    }

    private static void runRoll(Mob mob, int forcedTier) {
        if (forcedTier > 0) makeElite(mob, forcedTier);
        else if (forcedTier < 0) makeElite(mob, rollTier(mob));
        else tryRoll(mob);
    }

    /** guaranteedElites entry matching this mob, or null. */
    public static EliteConfig.Guarantee findGuarantee(Mob mob) {
        EliteConfig.Cache c = EliteConfig.cache();
        if (c.guarantees.isEmpty() || isNeverAllowed(mob) || isBlacklisted(mob.getType())) return null;
        if (c.dimensionBlacklist.contains(mob.level().dimension().location().toString())) return null;
        for (EliteConfig.Guarantee g : c.guarantees) {
            if (g.filter().matches(BuiltInRegistries.ENTITY_TYPE, mob.getType())) return g;
        }
        return null;
    }

    /** Normal elite roll (guarantees first, then the regular chance). */
    public static void tryRoll(Mob mob) {
        if (!EliteConfig.ENABLED.get()) return;
        RandomSource r = mob.getRandom();
        EliteConfig.Guarantee g = findGuarantee(mob);
        if (g != null && r.nextDouble() * 100.0 < g.chancePercent()) {
            makeElite(mob, g.tier() > 0 ? g.tier() : rollTier(mob));
            return;
        }
        if (!isEligible(mob)) return;
        int minTier = healthTier(mob);
        double chance = EliteConfig.ELITE_CHANCE_PERCENT.get() / 100.0 * EliteConfig.tier(minTier).spawnChanceMultiplier.get();
        if (r.nextDouble() >= chance) return;
        makeElite(mob, rollTier(mob));
    }

    /** Pick a tier: never below the health tier; above it, weighted by tierN.randomTierWeight. */
    public static int rollTier(Mob mob) {
        int min = healthTier(mob);
        if (!EliteConfig.RANDOM_TIER_UPGRADE.get()) return min;
        double total = 0;
        for (int t = min; t <= EliteConfig.TIER_COUNT; t++) total += EliteConfig.tier(t).randomTierWeight.get();
        if (total <= 0) return min;
        double roll = mob.getRandom().nextDouble() * total;
        for (int t = min; t <= EliteConfig.TIER_COUNT; t++) {
            roll -= EliteConfig.tier(t).randomTierWeight.get();
            if (roll < 0) return t;
        }
        return EliteConfig.TIER_COUNT;
    }

    // ------------------------------------------------------------------ apply / remove

    public static void makeElite(Mob mob, int tier) {
        tier = Math.max(1, Math.min(EliteConfig.TIER_COUNT, tier));
        EliteConfig.Tier s = EliteConfig.tier(tier);

        addModifier(mob, Attributes.MAX_HEALTH, HEALTH_ID, s.healthMultiplier.get() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addModifier(mob, Attributes.ATTACK_DAMAGE, ATTACK_ID, s.attackBonus.get(), AttributeModifier.Operation.ADD_VALUE);
        addModifier(mob, Attributes.MOVEMENT_SPEED, SPEED_ID, s.speedBonusPercent.get() / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(mob, Attributes.FLYING_SPEED, FLY_SPEED_ID, s.speedBonusPercent.get() / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(mob, Attributes.ARMOR, ARMOR_ID, s.armorBonus.get(), AttributeModifier.Operation.ADD_VALUE);
        addModifier(mob, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, s.armorToughnessBonus.get(), AttributeModifier.Operation.ADD_VALUE);
        addModifier(mob, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, s.knockbackResistance.get(), AttributeModifier.Operation.ADD_VALUE);

        mob.setHealth(mob.getMaxHealth());
        // Other mods (difficulty / RPG scaling) may raise max health after us, so top up again for a few ticks.
        mob.setData(ModAttachments.FULL_HEAL_TICKS, 10);
        mob.setData(ModAttachments.ELITE_TIER, tier);
        mob.setData(ModAttachments.PROCESSED, true);
        if (EliteConfig.PERSISTENT.get()) mob.setPersistenceRequired();
        applyName(mob, tier);
        EliteNetwork.syncToTrackers(mob, tier);
    }

    // ------------------------------------------------------------------ name prefix

    /** "Diabolic Zombie": prefix in the tier color + the mob's own name. */
    private static void applyName(Mob mob, int tier) {
        if (!EliteConfig.NAME_PREFIX_ENABLED.get()) return;
        EliteConfig.Tier s = EliteConfig.tier(tier);
        String prefix = s.namePrefix.get().trim();
        if (prefix.isEmpty()) return;

        // remember a name the mob already had (name tag, other mods) so we prefix that instead
        if (!mob.hasData(ModAttachments.NAMED) && mob.hasCustomName() && mob.getCustomName() != null) {
            mob.setData(ModAttachments.ORIGINAL_NAME, mob.getCustomName().getString());
        }
        Component base = mob.hasData(ModAttachments.ORIGINAL_NAME)
                ? Component.literal(mob.getData(ModAttachments.ORIGINAL_NAME))
                : mob.getType().getDescription();

        MutableComponent name = Component.literal(prefix + " ").withStyle(parseStyle(s.nameColor.get())).append(base.copy());
        mob.setCustomName(name);
        mob.setCustomNameVisible(s.alwaysShowName.get());
        mob.setData(ModAttachments.NAMED, true);
    }

    private static void removeName(Mob mob) {
        if (!mob.hasData(ModAttachments.NAMED)) return;
        if (mob.hasData(ModAttachments.ORIGINAL_NAME)) {
            mob.setCustomName(Component.literal(mob.getData(ModAttachments.ORIGINAL_NAME)));
            mob.removeData(ModAttachments.ORIGINAL_NAME);
        } else {
            mob.setCustomName(null);
            mob.setCustomNameVisible(false);
        }
        mob.removeData(ModAttachments.NAMED);
    }

    /** "red", "dark_red,bold", "#FF3030", "#8B0000,italic" */
    public static Style parseStyle(String spec) {
        Style style = Style.EMPTY;
        for (String raw : spec.split(",")) {
            String part = raw.trim().toLowerCase(Locale.ROOT);
            if (part.isEmpty()) continue;
            if (part.startsWith("#")) {
                try {
                    style = style.withColor(TextColor.fromRgb(Integer.parseInt(part.substring(1), 16) & 0xFFFFFF));
                } catch (NumberFormatException ignored) {
                }
            } else {
                ChatFormatting f = ChatFormatting.getByName(part);
                if (f != null) style = style.applyFormat(f);
            }
        }
        return style;
    }

    public static void removeElite(Mob mob) {
        for (Holder<Attribute> a : new Holder[]{Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED,
                Attributes.FLYING_SPEED, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE}) {
            AttributeInstance inst = mob.getAttribute(a);
            if (inst == null) continue;
            for (ResourceLocation id : new ResourceLocation[]{HEALTH_ID, ATTACK_ID, SPEED_ID, FLY_SPEED_ID, ARMOR_ID, TOUGHNESS_ID, KNOCKBACK_ID}) {
                inst.removeModifier(id);
            }
        }
        if (mob.getHealth() > mob.getMaxHealth()) mob.setHealth(mob.getMaxHealth());
        if (mob.hasData(ModAttachments.ELITE_TIER)) mob.removeData(ModAttachments.ELITE_TIER);
        removeName(mob);
        EliteNetwork.syncToTrackers(mob, 0);
    }

    private static void addModifier(Mob mob, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount == 0.0) return;
        inst.addPermanentModifier(new AttributeModifier(id, amount, op));
    }
}
