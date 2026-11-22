package com.lunaticelitemobs.network;

import com.lunaticelitemobs.elite.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EliteNetwork {
    /** Tiers received before the entity existed on the client; applied when it joins. */
    private static final Map<Integer, Integer> PENDING = new ConcurrentHashMap<>();

    private EliteNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(EliteSyncPayload.TYPE, EliteSyncPayload.STREAM_CODEC, EliteNetwork::handle);
    }

    private static void handle(EliteSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (player == null) return;
            Entity e = player.level().getEntity(payload.entityId());
            if (e != null) {
                apply(e, payload.tier());
            } else {
                if (PENDING.size() > 1024) PENDING.clear();
                PENDING.put(payload.entityId(), payload.tier());
            }
        });
    }

    /** Called on the client when an entity joins the level. */
    public static void consumePending(Entity e) {
        Integer tier = PENDING.remove(e.getId());
        if (tier != null) apply(e, tier);
    }

    private static void apply(Entity e, int tier) {
        if (tier > 0) e.setData(ModAttachments.ELITE_TIER, tier);
        else if (e.hasData(ModAttachments.ELITE_TIER)) e.removeData(ModAttachments.ELITE_TIER);
    }

    public static void syncToTrackers(Entity e, int tier) {
        PacketDistributor.sendToPlayersTrackingEntity(e, new EliteSyncPayload(e.getId(), tier));
    }

    public static void syncTo(ServerPlayer player, Entity e, int tier) {
        PacketDistributor.sendToPlayer(player, new EliteSyncPayload(e.getId(), tier));
    }
}
