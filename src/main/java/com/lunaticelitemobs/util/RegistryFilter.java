package com.lunaticelitemobs.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * A list of registry entries written in config. Each line may be:
 * <ul>
 *   <li>{@code minecraft:zombie} – one exact id</li>
 *   <li>{@code #minecraft:raiders} – every entry in a tag (datapack / mod tags work)</li>
 *   <li>{@code @alexsmobs} or {@code alexsmobs:*} – everything from one mod</li>
 * </ul>
 */
public final class RegistryFilter<T> {
    private final Set<ResourceLocation> ids = new HashSet<>();
    private final Set<String> mods = new HashSet<>();
    private final List<TagKey<T>> tags = new ArrayList<>();

    public static <T> RegistryFilter<T> parse(List<? extends String> lines, ResourceKey<? extends Registry<T>> registry) {
        RegistryFilter<T> f = new RegistryFilter<>();
        for (String raw : lines) f.add(raw, registry);
        return f;
    }

    public void add(String raw, ResourceKey<? extends Registry<T>> registry) {
        String s = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return;
        if (s.startsWith("@")) {
            mods.add(s.substring(1));
        } else if (s.endsWith(":*")) {
            mods.add(s.substring(0, s.length() - 2));
        } else if (s.startsWith("#")) {
            ResourceLocation rl = ResourceLocation.tryParse(s.substring(1));
            if (rl != null) tags.add(TagKey.create(registry, rl));
        } else {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) ids.add(rl);
        }
    }

    public boolean isEmpty() {
        return ids.isEmpty() && mods.isEmpty() && tags.isEmpty();
    }

    public boolean matches(Registry<T> registry, T value) {
        if (isEmpty()) return false;
        ResourceLocation id = registry.getKey(value);
        if (id != null && (ids.contains(id) || mods.contains(id.getNamespace()))) return true;
        if (tags.isEmpty()) return false;
        Holder<T> holder = registry.wrapAsHolder(value);
        for (TagKey<T> tag : tags) if (holder.is(tag)) return true;
        return false;
    }

    // ------------------------------------------------------------------ single entry resolution (drops)

    /**
     * Resolve one config entry ("id" or "#tag") to a random value. Returns empty if the id/tag
     * does not exist (mod not installed, typo, empty tag).
     */
    public static <T> Optional<T> pickOne(Registry<T> registry, String entry, RandomSource r) {
        String s = entry == null ? "" : entry.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return Optional.empty();
        if (s.startsWith("#")) {
            ResourceLocation rl = ResourceLocation.tryParse(s.substring(1));
            if (rl == null) return Optional.empty();
            Optional<HolderSet.Named<T>> set = registry.getTag(TagKey.create(registry.key(), rl));
            if (set.isEmpty() || set.get().size() == 0) return Optional.empty();
            return Optional.of(set.get().get(r.nextInt(set.get().size())).value());
        }
        ResourceLocation rl = ResourceLocation.tryParse(s);
        if (rl == null) return Optional.empty();
        return registry.getOptional(rl);
    }
}
