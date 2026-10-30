package com.baraaoi.reallife.network;

import com.baraaoi.reallife.economy.BankService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Клиент → сервер: действие в интерфейсе (код действия + 2 аргумента). */
public class ActionPacket {
    private final int action, arg1, arg2;

    public ActionPacket(int action, int arg1, int arg2) {
        this.action = action;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public static void encode(ActionPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.action);
        buf.writeInt(m.arg1);
        buf.writeInt(m.arg2);
    }

    public static ActionPacket decode(FriendlyByteBuf buf) {
        return new ActionPacket(buf.readVarInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(ActionPacket m, CustomPayloadEvent.Context ctx) {
        ServerPlayer p = ctx.getSender();
        if (p != null) BankService.handle(p, m.action, m.arg1, m.arg2);
        ctx.setPacketHandled(true);
    }
}
