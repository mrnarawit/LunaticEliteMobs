package com.lunaticelitemobs.config;

import com.lunaticelitemobs.elite.EliteLoot;
import com.lunaticelitemobs.util.RegistryFilter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Gameplay config: config/lunaticelitemobs-common.toml */
public final class EliteConfig {
    public static final int TIER_COUNT = 4;

    public static final ModConfigSpec SPEC;

    // ---------- general ----------
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue ELITE_CHANCE_PERCENT;
    public static final ModConfigSpec.DoubleValue MAX_BASE_HEALTH;
    public static final ModConfigSpec.DoubleValue TIER_CHECK_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue TIER2_HEALTH;
    public static final ModConfigSpec.DoubleValue TIER3_HEALTH;
    public static final ModConfigSpec.DoubleValue TIER4_HEALTH;
    public static final ModConfigSpec.BooleanValue RANDOM_TIER_UPGRADE;
    public static final ModConfigSpec.BooleanValue PERSISTENT;
    public static final ModConfigSpec.IntValue ROLL_DELAY_TICKS;

    // ---------- names ----------
    public static final ModConfigSpec.BooleanValue NAME_PREFIX_ENABLED;

    // ---------- filters ----------
    public static final ModConfigSpec.BooleanValue EXCLUDE_BOSSES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_BOSSES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_WHITELIST;
    public static final ModConfigSpec.BooleanValue WHITELIST_ONLY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOD_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSION_BLACKLIST;
    public static final ModConfigSpec.BooleanValue ALLOW_SPAWNERS;
    public static final ModConfigSpec.BooleanValue ALLOW_BABIES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GUARANTEED;

    // ---------- combat ----------
    public static final ModConfigSpec.DoubleValue RANGED_BONUS_FACTOR;

    // ---------- loot ----------
    public static final ModConfigSpec.BooleanValue REQUIRE_PLAYER_KILL;
    public static final ModConfigSpec.BooleanValue ALLOW_FAKE_PLAYERS;
    public static final ModConfigSpec.DoubleValue GEAR_MIN_DURABILITY;
    public static final ModConfigSpec.DoubleValue GEAR_MAX_DURABILITY;
    public static final ModConfigSpec.BooleanValue ONLY_LOOT_ENCHANTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RPG_NAMESPACES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RPG_ITEM_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RPG_EXTRA_ITEMS;
    public static final ModConfigSpec.BooleanValue ENCHANT_RPG_ITEMS;

    public static final Tier[] TIERS = new Tier[TIER_COUNT];

    /** Default values for one tier. */
    private record D(String prefix, String color, boolean showName,
                     double chanceMul, double weight, double hp, double atk, double spd, double armor, double tough, double kb, double xp,
                     int encMin, int encMax,
                     double gearChance, int gearRolls, List<String> gear,
                     double rpgChance, int rpgRolls, double rpgArmor, double rpgMin, double rpgMax, List<String> rpgFallback,
                     double treasureChance, int treasureRolls, List<String> treasure,
                     List<String> extra) {}

    public static final class Tier {
        public final ModConfigSpec.ConfigValue<String> namePrefix;
        public final ModConfigSpec.ConfigValue<String> nameColor;
        public final ModConfigSpec.BooleanValue alwaysShowName;

        public final ModConfigSpec.DoubleValue spawnChanceMultiplier;
        public final ModConfigSpec.DoubleValue randomTierWeight;
        public final ModConfigSpec.DoubleValue healthMultiplier;
        public final ModConfigSpec.DoubleValue attackBonus;
        public final ModConfigSpec.DoubleValue speedBonusPercent;
        public final ModConfigSpec.DoubleValue armorBonus;
        public final ModConfigSpec.DoubleValue armorToughnessBonus;
        public final ModConfigSpec.DoubleValue knockbackResistance;
        public final ModConfigSpec.DoubleValue xpMultiplier;

        public final ModConfigSpec.IntValue enchantLevelMin;
        public final ModConfigSpec.IntValue enchantLevelMax;

        public final ModConfigSpec.DoubleValue gearDropChancePercent;
        public final ModConfigSpec.IntValue gearRolls;
        public final ModConfigSpec.ConfigValue<List<? extends String>> gearItems;

        public final ModConfigSpec.DoubleValue rpgDropChancePercent;
        public final ModConfigSpec.IntValue rpgRolls;
        public final ModConfigSpec.DoubleValue rpgArmorChancePercent;
        public final ModConfigSpec.DoubleValue rpgPowerMin;
        public final ModConfigSpec.DoubleValue rpgPowerMax;
        public final ModConfigSpec.ConfigValue<List<? extends String>> rpgFallbackItems;

        public final ModConfigSpec.DoubleValue treasureChancePercent;
        public final ModConfigSpec.IntValue treasureRolls;
        public final ModConfigSpec.ConfigValue<List<? extends String>> treasurePool;

        public final ModConfigSpec.ConfigValue<List<? extends String>> extraDrops;

        Tier(ModConfigSpec.Builder b, int n, D d) {
            b.comment("Tier " + n).push("tier" + n);

            b.push("name");
            namePrefix = b.comment("Word put in front of the mob's name. Empty = no prefix for this tier.").define("prefix", d.prefix);
            nameColor = b.comment("Prefix style: color name (red, dark_red, gold, dark_purple...) or hex #FF3030, plus optional formats: \"dark_red,bold\"")
                    .define("color", d.color);
            alwaysShowName = b.comment("Show the name tag above the mob all the time. When false the name is only changed (Jade, death messages...),",
                            "and the floating name tag is hidden (see client config aura.hideEliteNameTag).")
                    .define("alwaysShowName", d.showName);
            b.pop();

            b.push("stats");
            spawnChanceMultiplier = b.comment("Multiplies eliteChancePercent for mobs whose HEALTH puts them in this tier. Lower = rarer.")
                    .defineInRange("spawnChanceMultiplier", d.chanceMul, 0.0, 1.0);
            randomTierWeight = b.comment("Weight used to pick the tier of a new elite (needs allowRandomTierUpgrade).",
                            "Only tiers at or above the mob's health tier can be picked.")
                    .defineInRange("randomTierWeight", d.weight, 0.0, 1000.0);
            healthMultiplier = b.comment("Max health multiplier (1.4 = +40%).").defineInRange("healthMultiplier", d.hp, 1.0, 100.0);
            attackBonus = b.comment("Flat attack damage added (also to projectiles, see combat.rangedBonusFactor).")
                    .defineInRange("attackBonus", d.atk, 0.0, 1000.0);
            speedBonusPercent = b.defineInRange("speedBonusPercent", d.spd, 0.0, 500.0);
            armorBonus = b.defineInRange("armorBonus", d.armor, 0.0, 30.0);
            armorToughnessBonus = b.defineInRange("armorToughnessBonus", d.tough, 0.0, 20.0);
            knockbackResistance = b.defineInRange("knockbackResistance", d.kb, 0.0, 1.0); // 0.2 = 20%
            xpMultiplier = b.defineInRange("xpMultiplier", d.xp, 0.0, 1000.0);
            b.pop();

            b.comment("Loot. Item lines accept an item id or #itemTag (a tag drops a random item from it).").push("loot");
            enchantLevelMin = b.comment("Enchanting level used for gear and enchanted books (30 = max enchanting table).")
                    .defineInRange("enchantLevelMin", d.encMin, 0, 100);
            enchantLevelMax = b.defineInRange("enchantLevelMax", d.encMax, 0, 100);

            gearDropChancePercent = b.comment("Vanilla-style enchanted weapons. Chance (%) per roll.")
                    .defineInRange("gearDropChancePercent", d.gearChance, 0.0, 100.0);
            gearRolls = b.defineInRange("gearRolls", d.gearRolls, 0, 20);
            gearItems = b.comment("Lines: \"idOrTag\" or \"idOrTag;weight\"   e.g. \"minecraft:diamond_sword;3\", \"#minecraft:swords\"")
                    .defineListAllowEmpty("gearItems", d.gear, o -> o instanceof String);

            rpgDropChancePercent = b.comment("RPG Series / Plus equipment. Chance (%) per roll.")
                    .defineInRange("rpgDropChancePercent", d.rpgChance, 0.0, 100.0);
            rpgRolls = b.defineInRange("rpgRolls", d.rpgRolls, 0, 20);
            rpgArmorChancePercent = b.comment("Chance (%) that an RPG drop is ARMOR (the rest are weapons / accessories).")
                    .defineInRange("rpgArmorChancePercent", d.rpgArmor, 0.0, 100.0);
            rpgPowerMin = b.comment("RPG items are ranked weakest (0.0) to strongest (1.0) by their stats; this tier picks from this slice.")
                    .defineInRange("rpgPowerMin", d.rpgMin, 0.0, 1.0);
            rpgPowerMax = b.defineInRange("rpgPowerMax", d.rpgMax, 0.0, 1.0);
            rpgFallbackItems = b.comment("Used (enchanted) when no RPG mod is installed. Lines: \"idOrTag\" or \"idOrTag;weight\".")
                    .defineListAllowEmpty("rpgFallbackItems", d.rpgFallback, o -> o instanceof String);

            treasureChancePercent = b.comment("Material / treasure pool. Each roll has this chance (%) to pick ONE weighted entry.")
                    .defineInRange("treasureChancePercent", d.treasureChance, 0.0, 100.0);
            treasureRolls = b.defineInRange("treasureRolls", d.treasureRolls, 0, 50);
            treasurePool = b.comment("Lines: \"idOrTag;min;max;weight\" and optionally \";enchant\" (uses this tier's enchant levels).",
                            "  \"minecraft:book;1;1;10;enchant\" = random enchanted book")
                    .defineListAllowEmpty("treasurePool", d.treasure, o -> o instanceof String);

            extraDrops = b.comment("Independent extra drops, each rolled separately: \"idOrTag;min;max;chancePercent[;enchant]\"")
                    .defineListAllowEmpty("extraDrops", d.extra, o -> o instanceof String);
            b.pop();

            b.pop();
        }
    }

    private static final String RPG_SKILL_ORB = "skill_tree_rpgs:orb_of_oblivion";

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("General").push("general");
        ENABLED = b.define("enabled", true);
        ELITE_CHANCE_PERCENT = b.comment("Chance (%) for an eligible hostile mob to become an elite. 0.01 - 100")
                .defineInRange("eliteChancePercent", 1.0, 0.01, 100.0);
        MAX_BASE_HEALTH = b.comment("Mobs with more max health than this never become elites (checked after other mods like In Control changed it).")
                .defineInRange("maxBaseHealth", 1000.0, 1.0, 1.0E9);
        TIER_CHECK_MULTIPLIER = b.comment("Minimum tier is chosen from (mob max health x this) vs the thresholds below.",
                        "Default 1.4 = health after the tier 1 bonus (+40%).")
                .defineInRange("tierHealthCheckMultiplier", 1.4, 0.0, 100.0);
        TIER2_HEALTH = b.comment("Checked health above this -> at least tier 2").defineInRange("tier2HealthThreshold", 500.0, 0.0, 1.0E9);
        TIER3_HEALTH = b.comment("Checked health above this -> at least tier 3").defineInRange("tier3HealthThreshold", 1000.0, 0.0, 1.0E9);
        TIER4_HEALTH = b.comment("Checked health above this -> tier 4").defineInRange("tier4HealthThreshold", 1500.0, 0.0, 1.0E9);
        RANDOM_TIER_UPGRADE = b.comment("If true, each new elite picks its tier randomly using tierN.stats.randomTierWeight (never below its health tier).",
                        "Set false to choose tier purely by health.")
                .define("allowRandomTierUpgrade", true);
        PERSISTENT = b.comment("If true, elites never despawn.").define("elitesPersistent", false);
        ROLL_DELAY_TICKS = b.comment("Wait this many ticks after a mob spawns before rolling / applying elite stats.",
                        "This lets mods like In Control, difficulty or level-scaling mods change the mob's health first,",
                        "so the elite bonus is applied ON TOP of their changes. 0 = apply instantly on spawn.")
                .defineInRange("rollDelayTicks", 5, 0, 200);
        b.pop();

        b.push("names");
        NAME_PREFIX_ENABLED = b.comment("Add the tier prefix (Elite / Empowered / Warlord / Diabolic) in front of elite names.",
                        "Prefix text and color are set per tier in tiers.tierN.name.")
                .define("enabled", true);
        b.pop();

        b.comment("Which mobs can become elites. Only hostile mobs are chosen automatically;",
                "neutral mobs (wolves, bees, endermen, iron golems...), villagers, animals and tamed mobs are always excluded.",
                "Entity list lines accept:  \"minecraft:zombie\" (id)   \"#minecraft:raiders\" (entity tag)   \"@alexsmobs\" or \"alexsmobs:*\" (whole mod)")
                .push("filters");
        EXCLUDE_BOSSES = b.comment("Exclude bosses (entity tag c:bosses plus extraBossEntities).").define("excludeBosses", true);
        EXTRA_BOSSES = b.comment("Extra entities treated as bosses (ids, #tags, @mods).")
                .defineListAllowEmpty("extraBossEntities", List.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:warden", "minecraft:elder_guardian"), o -> o instanceof String);
        ENTITY_BLACKLIST = b.comment("Mobs that never become elites (ids, #tags, @mods). Data tag #lunaticelitemobs:blacklist also works.")
                .defineListAllowEmpty("entityBlacklist", List.of("minecraft:piglin"), o -> o instanceof String);
        ENTITY_WHITELIST = b.comment("Mobs that can become elites (ids, #tags, @mods). Data tag #lunaticelitemobs:whitelist also works.",
                        "whitelistOnly = false: allowed IN ADDITION to auto-detected hostile mobs.",
                        "whitelistOnly = true : ONLY these mobs can become elites.",
                        "Neutral mobs, villagers and tamed mobs are never allowed. Blacklist always wins.")
                .defineListAllowEmpty("entityWhitelist", List.of(), o -> o instanceof String);
        WHITELIST_ONLY = b.define("whitelistOnly", false);
        MOD_BLACKLIST = b.comment("Mod ids whose mobs never become elites (same as \"@modid\" in entityBlacklist).")
                .defineListAllowEmpty("modBlacklist", List.of(), o -> o instanceof String);
        DIMENSION_BLACKLIST = b.comment("Dimension ids where elites never spawn, e.g. \"minecraft:the_end\".")
                .defineListAllowEmpty("dimensionBlacklist", List.of(), o -> o instanceof String);
        ALLOW_SPAWNERS = b.comment("Allow mobs from monster spawners / trial spawners to become elites.").define("allowFromSpawners", false);
        ALLOW_BABIES = b.define("allowBabies", true);
        GUARANTEED = b.comment("Mobs that ALWAYS spawn as elites. Lines: \"idOrTagOrMod\", \"idOrTagOrMod;tier\" or \"idOrTagOrMod;tier;chancePercent\"",
                        "  \"minecraft:wither_skeleton\"        -> always elite, tier picked by the normal rules",
                        "  \"minecraft:evoker;3\"               -> always tier 3",
                        "  \"#minecraft:raiders;random;50\"     -> 50% chance, random tier",
                        "  \"@mowziesmobs;4\"                   -> every mob from that mod is tier 4",
                        "Ignores eliteChancePercent, the hostile check, maxBaseHealth and the boss filter (listed = intended).",
                        "Still never applies to neutral / villager / tamed mobs, and entityBlacklist still wins.")
                .defineListAllowEmpty("guaranteedElites", List.of(), o -> o instanceof String);
        b.pop();

        b.push("combat");
        RANGED_BONUS_FACTOR = b.comment("Fraction of attackBonus added to projectile / explosion / magic damage from elites. 0 = disabled.")
                .defineInRange("rangedBonusFactor", 1.0, 0.0, 10.0);
        b.pop();

        b.push("loot");
        REQUIRE_PLAYER_KILL = b.comment("Bonus loot and XP only when killed by a player.").define("requirePlayerKill", true);
        ALLOW_FAKE_PLAYERS = b.comment("Count kills by fake players (mod machines) as player kills.").define("allowFakePlayers", false);
        GEAR_MIN_DURABILITY = b.comment("Dropped gear keeps a random fraction of durability between these values.")
                .defineInRange("gearMinDurability", 0.35, 0.0, 1.0);
        GEAR_MAX_DURABILITY = b.defineInRange("gearMaxDurability", 0.9, 0.0, 1.0);
        ONLY_LOOT_ENCHANTS = b.comment("Only use enchantments that can appear on random loot (no Mending etc.).")
                .define("onlyRandomLootEnchants", true);
        RPG_NAMESPACES = b.comment("Mod ids scanned for RPG equipment drops (RPG Series / RPG Series Plus and similar).",
                        "Every armor / weapon / accessory from these mods is ranked by its stats automatically. Mods that are not installed are ignored.",
                        "Tip: press F3+H in game to see an item's mod id in its tooltip.")
                .defineListAllowEmpty("rpgModNamespaces", List.of(
                        "wizards", "paladins", "rogues", "archers", "jewelry", "relics", "arsenal", "armory",
                        "druids", "bard", "elemental_wizards", "forcemaster", "berserker", "more_rpg_classes",
                        "spellbladenext", "deathknights", "witcher"), o -> o instanceof String);
        RPG_ITEM_BLACKLIST = b.comment("Items never dropped from the RPG pool: ids, #itemTags or @mods.")
                .defineListAllowEmpty("rpgItemBlacklist", List.of(RPG_SKILL_ORB), o -> o instanceof String);
        RPG_EXTRA_ITEMS = b.comment("Extra items added to the RPG pool (ranked by stats like the rest): ids, #itemTags or @mods.")
                .defineListAllowEmpty("rpgExtraItems", List.of(), o -> o instanceof String);
        ENCHANT_RPG_ITEMS = b.comment("Also enchant RPG items (using the tier's enchant levels).").define("enchantRpgItems", false);
        b.pop();

        b.comment("Per-tier settings").push("tiers");
        TIERS[0] = new Tier(b, 1, new D("Elite", "red", false,
                1.0, 55.0, 1.4, 5.0, 15.0, 10.0, 0.0, 0.2, 2.0,
                5, 12,
                15.0, 1, List.of("minecraft:iron_sword;3", "minecraft:iron_axe;2", "minecraft:bow;2", "minecraft:crossbow;1"),
                15.0, 1, 75.0, 0.0, 0.45,
                List.of("minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "minecraft:chainmail_leggings",
                        "minecraft:chainmail_boots", "minecraft:iron_helmet", "minecraft:iron_boots"),
                55.0, 2, List.of(
                        "minecraft:gold_ingot;1;3;20", "minecraft:lapis_lazuli;2;6;20", "minecraft:amethyst_shard;1;4;18",
                        "minecraft:quartz;2;6;18", "minecraft:emerald;1;2;12", "minecraft:book;1;1;6;enchant",
                        "minecraft:nautilus_shell;1;1;3", "#minecraft:trim_templates;1;1;3"),
                List.of(RPG_SKILL_ORB + ";1;1;1")));
        TIERS[1] = new Tier(b, 2, new D("Empowered", "gold", false,
                0.7, 25.0, 1.6, 7.0, 17.0, 12.0, 2.0, 0.4, 3.0,
                10, 18,
                20.0, 1, List.of("minecraft:iron_sword;3", "minecraft:iron_axe;2", "minecraft:bow;2", "minecraft:crossbow;2"),
                25.0, 1, 75.0, 0.15, 0.65,
                List.of("minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots"),
                70.0, 2, List.of(
                        "minecraft:gold_ingot;2;4;18", "minecraft:lapis_lazuli;3;8;16", "minecraft:amethyst_shard;2;5;16",
                        "minecraft:quartz;3;8;16", "minecraft:emerald;1;3;14", "minecraft:diamond;1;1;5",
                        "minecraft:book;1;1;10;enchant", "minecraft:nautilus_shell;1;1;5", "#minecraft:trim_templates;1;1;4"),
                List.of(RPG_SKILL_ORB + ";1;1;3")));
        TIERS[2] = new Tier(b, 3, new D("Warlord", "dark_purple", false,
                0.45, 13.0, 1.8, 9.0, 20.0, 15.0, 4.0, 0.6, 4.0,
                15, 24,
                25.0, 1, List.of("minecraft:diamond_sword;3", "minecraft:diamond_axe;2", "minecraft:bow;2", "minecraft:crossbow;2", "minecraft:trident;1"),
                35.0, 2, 75.0, 0.35, 0.85,
                List.of("minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:diamond_helmet", "minecraft:diamond_boots"),
                80.0, 3, List.of(
                        "minecraft:gold_ingot;2;6;14", "minecraft:lapis_lazuli;4;10;12", "minecraft:amethyst_shard;3;6;12",
                        "minecraft:quartz;4;10;12", "minecraft:emerald;2;4;14", "minecraft:diamond;1;2;10",
                        "minecraft:book;1;1;14;enchant", "minecraft:nautilus_shell;1;1;7", "minecraft:golden_apple;1;1;5", "#minecraft:trim_templates;1;1;6"),
                List.of(RPG_SKILL_ORB + ";1;1;6", "minecraft:heart_of_the_sea;1;1;1")));
        TIERS[3] = new Tier(b, 4, new D("Diabolic", "dark_red,bold", false,
                0.25, 7.0, 2.0, 12.0, 22.0, 18.0, 6.0, 0.8, 5.0,
                20, 30,
                30.0, 1, List.of("minecraft:diamond_sword;3", "minecraft:diamond_axe;2", "minecraft:bow;2", "minecraft:crossbow;2", "minecraft:trident;2"),
                45.0, 2, 75.0, 0.55, 1.0,
                List.of("minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots"),
                85.0, 4, List.of(
                        "minecraft:gold_ingot;3;8;12", "minecraft:lapis_lazuli;6;12;10", "minecraft:amethyst_shard;4;8;10",
                        "minecraft:quartz;6;12;10", "minecraft:emerald;3;6;14", "minecraft:diamond;1;3;14",
                        "minecraft:book;1;1;18;enchant", "minecraft:nautilus_shell;1;2;8", "minecraft:golden_apple;1;2;8",
                        "minecraft:netherite_scrap;1;1;2", "#minecraft:trim_templates;1;1;8"),
                List.of(RPG_SKILL_ORB + ";1;1;12", "minecraft:heart_of_the_sea;1;1;3", "minecraft:enchanted_golden_apple;1;1;2")));
        b.pop();

        SPEC = b.build();
    }

    private EliteConfig() {}

    public static Tier tier(int tier) {
        return TIERS[Math.max(1, Math.min(TIER_COUNT, tier)) - 1];
    }

    // ---------- cached parsed values ----------
    private static volatile Cache cache;

    /** One guaranteedElites line. tier 0 = random tier. */
    public record Guarantee(RegistryFilter<EntityType<?>> filter, int tier, double chancePercent) {}

    public static final class Cache {
        public final RegistryFilter<EntityType<?>> extraBosses, blacklist, whitelist;
        public final Set<String> dimensionBlacklist;
        public final List<Guarantee> guarantees = new ArrayList<>();

        Cache() {
            extraBosses = RegistryFilter.parse(EXTRA_BOSSES.get(), Registries.ENTITY_TYPE);
            blacklist = RegistryFilter.parse(ENTITY_BLACKLIST.get(), Registries.ENTITY_TYPE);
            for (String mod : MOD_BLACKLIST.get()) blacklist.add("@" + mod.trim(), Registries.ENTITY_TYPE);
            whitelist = RegistryFilter.parse(ENTITY_WHITELIST.get(), Registries.ENTITY_TYPE);
            dimensionBlacklist = lower(DIMENSION_BLACKLIST.get());
            for (String line : GUARANTEED.get()) {
                String[] p = line.split(";");
                if (p.length == 0 || p[0].isBlank()) continue;
                RegistryFilter<EntityType<?>> f = RegistryFilter.parse(List.of(p[0]), Registries.ENTITY_TYPE);
                int tier = 0;
                if (p.length > 1 && !p[1].trim().equalsIgnoreCase("random")) {
                    try {
                        tier = Math.max(0, Math.min(TIER_COUNT, Integer.parseInt(p[1].trim())));
                    } catch (NumberFormatException ignored) {
                    }
                }
                double chance = 100.0;
                if (p.length > 2) {
                    try {
                        chance = Double.parseDouble(p[2].trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
                guarantees.add(new Guarantee(f, tier, chance));
            }
        }
    }

    public static Set<String> lower(List<? extends String> list) {
        Set<String> set = new HashSet<>();
        for (String s : list) set.add(s.trim().toLowerCase(Locale.ROOT));
        return set;
    }

    public static Cache cache() {
        Cache c = cache;
        if (c == null) cache = c = new Cache();
        return c;
    }

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) invalidate();
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) invalidate();
    }

    private static void invalidate() {
        cache = null;
        EliteLoot.invalidate();
    }
}
