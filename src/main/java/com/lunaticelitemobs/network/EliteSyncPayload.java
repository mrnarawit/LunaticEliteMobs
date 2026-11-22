package com.lunaticelitemobs.network;

import com.lunaticelitemobs.LunaticEliteMobs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: entity {@code entityId} is an elite of {@code tier} (0 = not elite). */
public record EliteSyncPayload(int entityId, int tier) implements CustomPacketPayload {
    public static final Type<EliteSyncPayload> TYPE = new Type<>(LunaticEliteMobs.id("elite_sync"));

    public static final StreamCodec<ByteBuf, EliteSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EliteSyncPayload::entityId,
            ByteBufCodecs.VAR_INT, EliteSyncPayload::tier,
            EliteSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
