package com.baraaoi.reallife.network;

import com.baraaoi.reallife.economy.BankService;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент -> сервер: нажата кнопка в банке / МФО. */
public class ActionPacket {
    private final int action, arg1, arg2;

    public ActionPacket(int action, int arg1, int arg2) {
        this.action = action;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public static void encode(ActionPacket m, PacketBuffer buf) {
        buf.writeVarInt(m.action);
        buf.writeInt(m.arg1);
        buf.writeInt(m.arg2);
    }

    public static ActionPacket decode(PacketBuffer buf) {
        return new ActionPacket(buf.readVarInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(ActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity p = ctx.get().getSender();
            if (p != null) BankService.handle(p, m.action, m.arg1, m.arg2);
        });
        ctx.get().setPacketHandled(true);
    }
}
