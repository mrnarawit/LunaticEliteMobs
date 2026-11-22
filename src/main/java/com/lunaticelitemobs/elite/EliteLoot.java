package com.lunaticelitemobs.elite;

import com.lunaticelitemobs.LunaticEliteMobs;
import com.lunaticelitemobs.config.EliteConfig;
import com.lunaticelitemobs.util.RegistryFilter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class EliteLoot {
    private record Scored(Item item, double power) {}

    /** RPG equipment, each list sorted weakest -> strongest. */
    private record Pool(List<Scored> armor, List<Scored> other) {
        boolean isEmpty() {
            return armor.isEmpty() && other.isEmpty();
        }
    }

    private static volatile Pool rpgPool;

    private EliteLoot() {}

    public static void invalidate() {
        rpgPool = null;
    }

    public static List<ItemStack> generate(int tier, RandomSource r, RegistryAccess access) {
        EliteConfig.Tier s = EliteConfig.tier(tier);
        List<ItemStack> out = new ArrayList<>();

        // 1) vanilla-style enchanted weapons
        for (int i = 0; i < s.gearRolls.get(); i++) {
            if (!roll(r, s.gearDropChancePercent.get())) continue;
            Item item = pickWeighted(s.gearItems.get(), r);
            if (item == null) continue;
            ItemStack stack = enchant(new ItemStack(item), s, r, access);
            damage(stack, r);
            out.add(stack);
        }

        // 2) RPG Series / Plus equipment (mostly armor), with vanilla fallback
        for (int i = 0; i < s.rpgRolls.get(); i++) {
            if (!roll(r, s.rpgDropChancePercent.get())) continue;
            ItemStack rpg = pickRpg(s, r);
            if (!rpg.isEmpty()) {
                if (EliteConfig.ENCHANT_RPG_ITEMS.get()) rpg = enchant(rpg, s, r, access);
                out.add(rpg);
            } else {
                Item fb = pickWeighted(s.rpgFallbackItems.get(), r);
                if (fb != null) {
                    ItemStack stack = enchant(new ItemStack(fb), s, r, access);
                    damage(stack, r);
                    out.add(stack);
                }
            }
        }

        // 3) treasure pool: weighted, one entry per successful roll
        List<? extends String> pool = s.treasurePool.get();
        if (!pool.isEmpty()) {
            for (int i = 0; i < s.treasureRolls.get(); i++) {
                if (!roll(r, s.treasureChancePercent.get())) continue;
                String line = pickWeightedLine(pool, 3, r);
                if (line != null) addEntry(out, line, s, r, access, false);
            }
        }

        // 4) independent extra drops
        for (String line : s.extraDrops.get()) addEntry(out, line, s, r, access, true);

        return out;
    }

    // ------------------------------------------------------------------ entries

    /**
     * "idOrTag;min;max;X[;enchant]". X is a chance (%) when {@code xIsChance}, otherwise the weight
     * that was already used to pick this line.
     */
    private static void addEntry(List<ItemStack> out, String line, EliteConfig.Tier s, RandomSource r,
                                 RegistryAccess access, boolean xIsChance) {
        String[] p = line.split(";");
        if (p.length == 0) return;
        if (xIsChance && p.length > 3 && !roll(r, parseDouble(p[3], 100.0))) return;
        Item item = item(p[0], r);
        if (item == null) return;
        int min = p.length > 1 ? parseInt(p[1], 1) : 1;
        int max = p.length > 2 ? parseInt(p[2], min) : min;
        int count = Mth.randomBetweenInclusive(r, Math.max(1, Math.min(min, max)), Math.max(1, Math.max(min, max)));
        boolean enchanted = p.length > 4 && p[4].trim().equalsIgnoreCase("enchant");
        if (enchanted) {
            for (int i = 0; i < count; i++) out.add(enchant(new ItemStack(item), s, r, access)); // enchanted items don't stack
        } else {
            int maxStack = new ItemStack(item).getMaxStackSize();
            while (count > 0) {
                int n = Math.min(count, maxStack);
                out.add(new ItemStack(item, n));
                count -= n;
            }
        }
    }

    private static boolean roll(RandomSource r, double percent) {
        return r.nextDouble() * 100.0 < percent;
    }

    /** "itemId" or "#tag": a tag resolves to a random item inside it. */
    private static Item item(String entry, RandomSource r) {
        Item item = RegistryFilter.pickOne(BuiltInRegistries.ITEM, entry, r).orElse(null);
        return item == Items.AIR ? null : item;
    }

    /** Lines "idOrTag" or "idOrTag;weight". Re-rolls a few times if an entry is missing / empty. */
    private static Item pickWeighted(List<? extends String> lines, RandomSource r) {
        for (int attempt = 0; attempt < 4; attempt++) {
            String line = pickWeightedLine(lines, 1, r);
            if (line == null) return null;
            Item item = item(line.split(";")[0], r);
            if (item != null) return item;
        }
        return null;
    }

    /** Pick a line using the number at {@code weightIndex} (after splitting on ';') as its weight, default 1. */
    private static String pickWeightedLine(List<? extends String> lines, int weightIndex, RandomSource r) {
        if (lines.isEmpty()) return null;
        double[] w = new double[lines.size()];
        double total = 0;
        for (int i = 0; i < lines.size(); i++) {
            String[] p = lines.get(i).split(";");
            w[i] = p.length > weightIndex ? Math.max(0, parseDouble(p[weightIndex], 1.0)) : 1.0;
            total += w[i];
        }
        if (total <= 0) return null;
        double roll = r.nextDouble() * total;
        for (int i = 0; i < w.length; i++) {
            roll -= w[i];
            if (roll < 0) return lines.get(i);
        }
        return lines.get(lines.size() - 1);
    }

    // ------------------------------------------------------------------ enchant / damage

    private static ItemStack enchant(ItemStack stack, EliteConfig.Tier s, RandomSource r, RegistryAccess access) {
        int lo = Math.min(s.enchantLevelMin.get(), s.enchantLevelMax.get());
        int hi = Math.max(s.enchantLevelMin.get(), s.enchantLevelMax.get());
        if (hi <= 0) return stack;
        int level = Mth.randomBetweenInclusive(r, Math.max(1, lo), hi);
        try {
            Optional<HolderSet.Named<Enchantment>> set = EliteConfig.ONLY_LOOT_ENCHANTS.get()
                    ? access.registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.ON_RANDOM_LOOT)
                    : Optional.empty();
            return EnchantmentHelper.enchantItem(r, stack, level, access, set);
        } catch (Exception e) {
            LunaticEliteMobs.LOGGER.debug("Could not enchant {}", stack, e);
            return stack;
        }
    }

    private static void damage(ItemStack stack, RandomSource r) {
        if (!stack.isDamageableItem()) return;
        double lo = Math.min(EliteConfig.GEAR_MIN_DURABILITY.get(), EliteConfig.GEAR_MAX_DURABILITY.get());
        double hi = Math.max(EliteConfig.GEAR_MIN_DURABILITY.get(), EliteConfig.GEAR_MAX_DURABILITY.get());
        double keep = lo + r.nextDouble() * (hi - lo);
        int max = stack.getMaxDamage();
        stack.setDamageValue(Mth.clamp((int) Math.round(max * (1.0 - keep)), 0, max - 1));
    }

    // ------------------------------------------------------------------ RPG pool

    private static ItemStack pickRpg(EliteConfig.Tier s, RandomSource r) {
        Pool pool = rpgPool();
        if (pool.isEmpty()) return ItemStack.EMPTY;
        boolean wantArmor = roll(r, s.rpgArmorChancePercent.get());
        List<Scored> list = wantArmor ? pool.armor() : pool.other();
        if (list.isEmpty()) list = wantArmor ? pool.other() : pool.armor();
        int n = list.size();
        double a = Math.min(s.rpgPowerMin.get(), s.rpgPowerMax.get());
        double b = Math.max(s.rpgPowerMin.get(), s.rpgPowerMax.get());
        int lo = Mth.clamp((int) Math.floor(a * n), 0, n - 1);
        int hi = Mth.clamp((int) Math.ceil(b * n) - 1, lo, n - 1);
        return new ItemStack(list.get(Mth.randomBetweenInclusive(r, lo, hi)).item());
    }

    private static Pool rpgPool() {
        Pool pool = rpgPool;
        if (pool != null) return pool;
        Set<String> namespaces = EliteConfig.lower(EliteConfig.RPG_NAMESPACES.get());
        RegistryFilter<Item> blacklist = RegistryFilter.parse(EliteConfig.RPG_ITEM_BLACKLIST.get(), Registries.ITEM);
        RegistryFilter<Item> extra = RegistryFilter.parse(EliteConfig.RPG_EXTRA_ITEMS.get(), Registries.ITEM);
        List<Scored> armor = new ArrayList<>();
        List<Scored> other = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            boolean listed = extra.matches(BuiltInRegistries.ITEM, item);
            if (!listed && !namespaces.contains(id.getNamespace())) continue;
            if (blacklist.matches(BuiltInRegistries.ITEM, item)) continue;
            ItemStack stack = new ItemStack(item);
            ItemAttributeModifiers mods = modifiers(stack);
            if (!listed) { // auto-scanned mods: equipment only
                if (item instanceof BlockItem || item instanceof SpawnEggItem) continue;
                if (!stack.isDamageableItem() && mods.modifiers().isEmpty()) continue;
            }
            Scored sc = new Scored(item, power(stack, mods));
            if (item instanceof ArmorItem) armor.add(sc);
            else other.add(sc);
        }
        armor.sort(Comparator.comparingDouble(Scored::power));
        other.sort(Comparator.comparingDouble(Scored::power));
        pool = new Pool(armor, other);
        LunaticEliteMobs.LOGGER.info("[LunaticEliteMobs] RPG drop pool: {} armor + {} other items from {}",
                armor.size(), other.size(), namespaces);
        rpgPool = pool;
        return pool;
    }

    @SuppressWarnings("deprecation")
    private static ItemAttributeModifiers modifiers(ItemStack stack) {
        ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (mods.modifiers().isEmpty()) mods = stack.getItem().getDefaultAttributeModifiers();
        return mods == null ? ItemAttributeModifiers.EMPTY : mods;
    }

    /** Rough "how strong is this item" score from its stats, durability and rarity. */
    private static double power(ItemStack stack, ItemAttributeModifiers mods) {
        double p = 0;
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            AttributeModifier m = e.modifier();
            double amt = m.amount();
            if (m.operation() != AttributeModifier.Operation.ADD_VALUE) amt *= 10.0; // percent-style bonuses
            if (amt <= 0) continue;
            if (e.attribute().equals(Attributes.ATTACK_DAMAGE)) p += amt;
            else if (e.attribute().equals(Attributes.ARMOR)) p += amt * 1.5;
            else if (e.attribute().equals(Attributes.ARMOR_TOUGHNESS)) p += amt;
            else if (e.attribute().equals(Attributes.ATTACK_SPEED)) p += 0;
            else p += amt * 0.5; // spell power, crit, etc.
        }
        if (stack.isDamageableItem()) p += stack.getMaxDamage() / 250.0;
        p += stack.getRarity().ordinal() * 2.0;
        return p;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static double parseDouble(String s, double def) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
