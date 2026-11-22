package com.lunaticelitemobs.elite;

import com.lunaticelitemobs.LunaticEliteMobs;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LunaticEliteMobs.MODID);

    /** Elite tier 1-4 (absent = not elite). Saved with the entity. */
    public static final Supplier<AttachmentType<Integer>> ELITE_TIER = ATTACHMENTS.register("elite_tier",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    /** Set once the mob has had its elite roll, so reloading a chunk never re-rolls it. */
    public static final Supplier<AttachmentType<Boolean>> PROCESSED = ATTACHMENTS.register("processed",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    /** Not saved: remaining ticks during which a fresh elite is topped up to full health. */
    public static final Supplier<AttachmentType<Integer>> FULL_HEAL_TICKS = ATTACHMENTS.register("full_heal_ticks",
            () -> AttachmentType.builder(() -> 0).build());

    /** Not saved: ticks left before a scheduled elite roll (see rollDelayTicks). */
    public static final Supplier<AttachmentType<Integer>> PENDING_ROLL = ATTACHMENTS.register("pending_roll",
            () -> AttachmentType.builder(() -> 0).build());

    /** Not saved: -1 = guaranteed random tier, 1-4 = guaranteed tier for the scheduled roll. */
    public static final Supplier<AttachmentType<Integer>> FORCED_TIER = ATTACHMENTS.register("forced_tier",
            () -> AttachmentType.builder(() -> 0).build());

    /** The mob's name was changed to carry an elite prefix. */
    public static final Supplier<AttachmentType<Boolean>> NAMED = ATTACHMENTS.register("named",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    /** Plain-text custom name the mob had before it was prefixed. */
    public static final Supplier<AttachmentType<String>> ORIGINAL_NAME = ATTACHMENTS.register("original_name",
            () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).build());

    private ModAttachments() {}
}
