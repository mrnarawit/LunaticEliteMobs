package com.lunaticelitemobs.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Visual config (per player): config/lunaticelitemobs-client.toml */
public final class EliteClientConfig {
    public enum Quality { LOW, MEDIUM, HIGH }

    /** Aura pieces that can be combined freely on any tier. */
    public enum Style { PARTICLES, ORBS, BLADES, SIGIL, WINGS }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.EnumValue<Quality> QUALITY;
    public static final ModConfigSpec.DoubleValue RENDER_DISTANCE;
    public static final ModConfigSpec.DoubleValue SIZE_SCALE;
    public static final ModConfigSpec.DoubleValue WIDTH_PADDING;
    public static final ModConfigSpec.DoubleValue ANIMATION_SPEED;
    public static final ModConfigSpec.BooleanValue HIDE_WHEN_INVISIBLE;
    public static final ModConfigSpec.BooleanValue HIDE_NAMETAG;

    public static final TierVisual[] TIERS = new TierVisual[EliteConfig.TIER_COUNT];

    public static final class TierVisual {
        public final ModConfigSpec.ConfigValue<List<? extends String>> styles;
        public final ModConfigSpec.ConfigValue<String> primaryColor;
        public final ModConfigSpec.ConfigValue<String> secondaryColor;
        public final ModConfigSpec.IntValue particleIntervalTicks;
        public final ModConfigSpec.IntValue particleCount;
        public final ModConfigSpec.DoubleValue particleSize;
        public final ModConfigSpec.IntValue orbCount;
        public final ModConfigSpec.IntValue bladeCount;
        public final ModConfigSpec.DoubleValue rotationSpeed;
        public final ModConfigSpec.DoubleValue alpha;

        TierVisual(ModConfigSpec.Builder b, int n, List<String> st, String c1, String c2, int interval, int count,
                   double pSize, int orbs, int blades, double rot) {
            b.comment("Tier " + n + " aura").push("tier" + n);
            styles = b.comment("Aura pieces (combine any): particles, orbs, blades, sigil, wings")
                    .defineListAllowEmpty("styles", st, o -> o instanceof String);
            primaryColor = b.comment("Hex color, e.g. #FF3030").define("primaryColor", c1);
            secondaryColor = b.define("secondaryColor", c2);
            particleIntervalTicks = b.comment("Ticks between particle bursts (higher = fewer particles, less lag).")
                    .defineInRange("particleIntervalTicks", interval, 1, 200);
            particleCount = b.comment("Particles per burst.").defineInRange("particleCount", count, 0, 20);
            particleSize = b.defineInRange("particleSize", pSize, 0.1, 4.0);
            orbCount = b.defineInRange("orbCount", orbs, 1, 12);
            bladeCount = b.defineInRange("bladeCount", blades, 1, 12);
            rotationSpeed = b.comment("Rotation speed multiplier for orbs / blades / sigil.")
                    .defineInRange("rotationSpeed", rot, 0.0, 10.0);
            alpha = b.comment("Opacity multiplier.").defineInRange("alpha", 1.0, 0.05, 1.0);
            b.pop();
        }
    }

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("aura");
        ENABLED = b.comment("Show elite auras.").define("enabled", true);
        QUALITY = b.comment("LOW = fewer particles and simpler shapes (best FPS).").defineEnum("quality", Quality.MEDIUM);
        RENDER_DISTANCE = b.comment("Auras farther than this (blocks) are not drawn.").defineInRange("renderDistance", 48.0, 4.0, 256.0);
        SIZE_SCALE = b.comment("Global size multiplier. Aura size already follows each mob's hitbox.").defineInRange("sizeScale", 1.0, 0.1, 5.0);
        WIDTH_PADDING = b.comment("Extra blocks added around the hitbox for orbit / sigil radius.").defineInRange("widthPadding", 0.35, 0.0, 5.0);
        ANIMATION_SPEED = b.defineInRange("animationSpeed", 1.0, 0.0, 10.0);
        HIDE_WHEN_INVISIBLE = b.comment("Hide aura when the mob is invisible.").define("hideWhenInvisible", true);
        HIDE_NAMETAG = b.comment("Hide the floating name tag of elites (the prefixed name still shows in Jade, death messages, etc.).",
                        "Elites whose tier has alwaysShowName = true in the common config still show it.")
                .define("hideEliteNameTag", true);
        b.pop();

        b.push("tiers");
        TIERS[0] = new TierVisual(b, 1, List.of("particles"), "#FF3030", "#D01818", 5, 1, 1.0, 3, 4, 1.0);
        TIERS[1] = new TierVisual(b, 2, List.of("orbs"), "#FF3030", "#A00000", 6, 1, 1.1, 3, 4, 1.0);
        TIERS[2] = new TierVisual(b, 3, List.of("blades"), "#E02020", "#FF8080", 6, 1, 1.2, 3, 4, 1.0);
        TIERS[3] = new TierVisual(b, 4, List.of("sigil", "wings"), "#FF2828", "#1E0000", 5, 1, 1.4, 3, 4, 1.0);
        b.pop();

        SPEC = b.build();
    }

    private EliteClientConfig() {}

    // ---------- parsed cache ----------
    public static final class Parsed {
        public final Set<Style> styles;
        public final float[] primary, secondary;

        Parsed(TierVisual v) {
            Set<Style> s = EnumSet.noneOf(Style.class);
            for (String name : v.styles.get()) {
                try {
                    s.add(Style.valueOf(name.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                }
            }
            styles = s;
            primary = parseColor(v.primaryColor.get(), 0xFF3030);
            secondary = parseColor(v.secondaryColor.get(), 0xA00000);
        }
    }

    private static volatile Parsed[] parsed;

    public static Parsed parsed(int tier) {
        Parsed[] p = parsed;
        if (p == null) {
            p = new Parsed[TIERS.length];
            for (int i = 0; i < TIERS.length; i++) p[i] = new Parsed(TIERS[i]);
            parsed = p;
        }
        return p[Math.max(1, Math.min(p.length, tier)) - 1];
    }

    public static TierVisual tier(int tier) {
        return TIERS[Math.max(1, Math.min(TIERS.length, tier)) - 1];
    }

    public static float[] parseColor(String s, int fallback) {
        int rgb = fallback;
        try {
            String h = s.trim();
            if (h.startsWith("#")) h = h.substring(1);
            if (h.startsWith("0x") || h.startsWith("0X")) h = h.substring(2);
            rgb = Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
        }
        return new float[]{((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f};
    }

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) parsed = null;
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) parsed = null;
    }
}
