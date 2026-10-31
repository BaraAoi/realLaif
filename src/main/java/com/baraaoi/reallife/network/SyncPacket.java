package com.baraaoi.reallife.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Сервер → клиент: снимок данных счёта для интерфейсов. */
public class SyncPacket {
    private final CompoundTag data;

    public SyncPacket(CompoundTag data) {
        this.data = data;
    }

    public static void encode(SyncPacket m, FriendlyByteBuf buf) {
        buf.writeNbt(m.data);
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        CompoundTag t = buf.readNbt();
        return new SyncPacket(t == null ? new CompoundTag() : t);
    }

    public static void handle(SyncPacket m, CustomPayloadEvent.Context ctx) {
        com.baraaoi.reallife.client.ClientAccountData.set(m.data);
        ctx.setPacketHandled(true);
    }
}
