package com.lunaticelitemobs.elite;

import com.lunaticelitemobs.LunaticEliteMobs;
import com.lunaticelitemobs.config.EliteConfig;
import com.lunaticelitemobs.network.EliteNetwork;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = LunaticEliteMobs.MODID)
public final class EliteEvents {
    private EliteEvents() {}

    // ------------------------------------------------------------------ spawning

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        MobSpawnType type = event.getSpawnType();
        if (!EliteConfig.ALLOW_SPAWNERS.get() && (type == MobSpawnType.SPAWNER || type == MobSpawnType.TRIAL_SPAWNER)) {
            event.getEntity().setData(ModAttachments.PROCESSED, true); // never roll
        }
    }

    /** LOWEST so other mods' join-time health / stat changes are already applied before we scale. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide()) {
            EliteNetwork.consumePending(entity);
            return;
        }
        if (event.isCanceled() || !(entity instanceof Mob mob)) return;
        if (mob.hasData(ModAttachments.PROCESSED)) return;
        EliteManager.scheduleRoll(mob, 0);
    }

    /** Runs delayed rolls, and keeps fresh elites at full health for their first ticks unless they got hit. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) return;
        if (mob.hasData(ModAttachments.PENDING_ROLL)) EliteManager.tickPending(mob);
        if (!mob.hasData(ModAttachments.FULL_HEAL_TICKS)) return;
        int left = mob.getData(ModAttachments.FULL_HEAL_TICKS);
        if (mob.hurtTime == 0 && mob.getLastHurtByMob() == null && mob.getHealth() < mob.getMaxHealth()) {
            mob.setHealth(mob.getMaxHealth());
        }
        if (left <= 1) mob.removeData(ModAttachments.FULL_HEAL_TICKS);
        else mob.setData(ModAttachments.FULL_HEAL_TICKS, left - 1);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        int tier = EliteManager.getTier(event.getTarget());
        if (tier > 0 && event.getEntity() instanceof ServerPlayer sp) {
            EliteNetwork.syncTo(sp, event.getTarget(), tier);
        }
    }

    // ------------------------------------------------------------------ combat

    /** Ranged / explosion / magic damage from elites also gets the attack bonus. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof LivingEntity attacker) || attacker.level().isClientSide()) return;
        int tier = EliteManager.getTier(attacker);
        if (tier <= 0) return;
        double factor = EliteConfig.RANGED_BONUS_FACTOR.get();
        if (factor <= 0) return;
        boolean indirect = src.getDirectEntity() != attacker
                || src.is(DamageTypeTags.IS_EXPLOSION)
                || attacker.getAttribute(Attributes.ATTACK_DAMAGE) == null;
        if (indirect) {
            event.setAmount(event.getAmount() + (float) (EliteConfig.tier(tier).attackBonus.get() * factor));
        }
    }

    // ------------------------------------------------------------------ rewards

    private static boolean killedByPlayer(LivingEntity dead, DamageSource src) {
        if (!EliteConfig.REQUIRE_PLAYER_KILL.get()) return true;
        Entity killer = src.getEntity();
        if (!(killer instanceof Player)) killer = dead.getKillCredit();
        if (!(killer instanceof Player)) return false;
        return EliteConfig.ALLOW_FAKE_PLAYERS.get() || !(killer instanceof FakePlayer);
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent event) {
        LivingEntity e = event.getEntity();
        int tier = EliteManager.getTier(e);
        if (tier <= 0) return;
        Player attacker = event.getAttackingPlayer();
        if (EliteConfig.REQUIRE_PLAYER_KILL.get()) {
            if (attacker == null) return;
            if (!EliteConfig.ALLOW_FAKE_PLAYERS.get() && attacker instanceof FakePlayer) return;
        }
        double mul = EliteConfig.tier(tier).xpMultiplier.get();
        event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * mul));
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        int tier = EliteManager.getTier(e);
        if (tier <= 0 || !(e.level() instanceof ServerLevel level)) return;
        if (!killedByPlayer(e, event.getSource())) return;
        for (ItemStack stack : EliteLoot.generate(tier, e.getRandom(), level.registryAccess())) {
            ItemEntity ie = new ItemEntity(level, e.getX(), e.getY() + 0.5, e.getZ(), stack);
            ie.setDefaultPickUpDelay();
            event.getDrops().add(ie);
        }
    }

    /** Spawn {@code count} elites of {@code type}. tier 0 = random (same rules as natural elites). */
    @SuppressWarnings("deprecation")
    private static int summon(CommandSourceStack src, Holder.Reference<EntityType<?>> type, Vec3 pos, int tier, int count) {
        ServerLevel level = src.getLevel();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Entity created = type.value().create(level);
            if (!(created instanceof Mob mob)) {
                if (created != null) created.discard();
                src.sendFailure(Component.literal(type.key().location() + " is not a mob, it can't be an elite"));
                return 0;
            }
            double ox = count > 1 ? (level.random.nextDouble() - 0.5) * 3.0 : 0.0;
            double oz = count > 1 ? (level.random.nextDouble() - 0.5) * 3.0 : 0.0;
            mob.moveTo(pos.x + ox, pos.y, pos.z + oz, level.random.nextFloat() * 360f, 0f);
            // Same hook as vanilla /summon, so FinalizeSpawnEvent fires and mods like In Control apply their spawn rules.
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null);
            // same delayed path as natural elites so In Control & co. apply first; marks PROCESSED
            EliteManager.scheduleRoll(mob, tier > 0 ? tier : -1);
            level.addFreshEntityWithPassengers(mob);
            spawned++;
        }
        int n = spawned;
        String what = tier > 0 ? "tier " + tier : "random tier";
        src.sendSuccess(() -> Component.literal("Summoned " + n + " " + what + " elite " + type.key().location()), true);
        return n;
    }

    /** Item tags may change on /reload, so rebuild the RPG pool next time it is needed. */
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        EliteLoot.invalidate();
    }

    // ------------------------------------------------------------------ commands

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CommandBuildContext buildContext = event.getBuildContext();
        event.getDispatcher().register(Commands.literal("lem")
                .requires(s -> s.hasPermission(2))
                // /lem summon <entity> [tier|random] [pos] [count]
                .then(Commands.literal("summon")
                        .then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes(c -> summon(c.getSource(), ResourceArgument.getSummonableEntityType(c, "entity"),
                                        c.getSource().getPosition(), 0, 1))
                                .then(Commands.literal("random")
                                        .executes(c -> summon(c.getSource(), ResourceArgument.getSummonableEntityType(c, "entity"),
                                                c.getSource().getPosition(), 0, 1))
                                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                                .executes(c -> summon(c.getSource(), ResourceArgument.getSummonableEntityType(c, "entity"),
                                                        Vec3Argument.getVec3(c, "pos"), 0, 1))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                                                        .executes(c -> summon(c.getSource(), ResourceArgument.getSummonableEntityType(c, "entity"),
                                                                Vec3Argument.getVec3(c, "pos"), 0, IntegerArgumentType.getInteger(c, "count"))))))
                                .then(Commands.argument("tier", IntegerArgumentType.integer(1, EliteConfig.TIER_COUNT))
                                        .executes(c -> summon(c.getSource(), ResourceArgument.getSummonableEntityType(c, "entity"),
                                                c.getSource().getPosition(), IntegerArgumentType.getInteger(c, "tier"), 1))
                                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                                .executes(c -> summon(c.getSource(), ResourceArgument.getSummonableEntityType(c, "entity"),
                                                        Vec3Argument.getVec3(c, "pos"), IntegerArgumentType.getInteger(c, "tier"), 1))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                                                        .executes(c -> summon(c.getSource(), ResourceArgument.getSummonableEntityType(c, "entity"),
                                                                Vec3Argument.getVec3(c, "pos"), IntegerArgumentType.getInteger(c, "tier"),
                                                                IntegerArgumentType.getInteger(c, "count"))))))))
                .then(Commands.literal("make")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("tier", IntegerArgumentType.integer(1, EliteConfig.TIER_COUNT))
                                        .executes(ctx -> {
                                            int tier = IntegerArgumentType.getInteger(ctx, "tier");
                                            int n = 0;
                                            for (Entity t : EntityArgument.getEntities(ctx, "targets")) {
                                                if (t instanceof Mob m) {
                                                    EliteManager.makeElite(m, tier);
                                                    n++;
                                                }
                                            }
                                            int count = n;
                                            ctx.getSource().sendSuccess(() -> Component.literal("Made " + count + " mob(s) tier " + tier + " elite"), true);
                                            return n;
                                        }))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> {
                                    int n = 0;
                                    for (Entity t : EntityArgument.getEntities(ctx, "targets")) {
                                        if (t instanceof Mob m && EliteManager.getTier(m) > 0) {
                                            EliteManager.removeElite(m);
                                            n++;
                                        }
                                    }
                                    int count = n;
                                    ctx.getSource().sendSuccess(() -> Component.literal("Removed elite from " + count + " mob(s)"), true);
                                    return n;
                                })))
                .then(Commands.literal("info")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> {
                                    Entity t = EntityArgument.getEntity(ctx, "target");
                                    int tier = EliteManager.getTier(t);
                                    String msg;
                                    if (t instanceof Mob m) {
                                        StringBuilder sb = new StringBuilder();
                                        sb.append(t.getName().getString()).append(": ").append(tier > 0 ? "elite tier " + tier : "not elite")
                                                .append(", hp ").append(Math.round(m.getHealth())).append("/").append(Math.round(m.getMaxHealth()))
                                                .append(", eligible=").append(EliteManager.isEligible(m))
                                                .append(", healthTier=").append(EliteManager.healthTier(m));
                                        // health breakdown, to see what other mods (In Control...) changed
                                        AttributeInstance hp = m.getAttribute(Attributes.MAX_HEALTH);
                                        if (hp != null) {
                                            sb.append("\nmax_health base=").append(Math.round(hp.getBaseValue()));
                                            for (AttributeModifier mod : hp.getModifiers()) {
                                                sb.append("\n  ").append(mod.id()).append(" ").append(mod.operation().name())
                                                        .append(" ").append(Math.round(mod.amount() * 1000) / 1000.0);
                                            }
                                        }
                                        if (!t.getTags().isEmpty()) sb.append("\ntags=").append(t.getTags());
                                        msg = sb.toString();
                                    } else {
                                        msg = t.getName().getString() + " is not a mob";
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                    return tier;
                                })))
                .then(Commands.literal("reloadloot")
                        .executes(ctx -> {
                            EliteLoot.invalidate();
                            ctx.getSource().sendSuccess(() -> Component.literal("RPG loot pool will be rebuilt on next drop"), true);
                            return 1;
                        })));
    }
}
