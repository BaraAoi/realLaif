package com.baraaoi.reallife.network;

import com.baraaoi.reallife.client.ClientAccountData;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Сервер -> клиент: состояние счёта для интерфейса. */
public class SyncPacket {
    private final CompoundNBT data;

    public SyncPacket(CompoundNBT data) {
        this.data = data;
    }

    public static void encode(SyncPacket m, PacketBuffer buf) {
        buf.writeCompoundTag(m.data);
    }

    public static SyncPacket decode(PacketBuffer buf) {
        return new SyncPacket(buf.readCompoundTag());
    }

    public static void handle(SyncPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientAccountData.set(m.data));
        ctx.get().setPacketHandled(true);
    }
}
