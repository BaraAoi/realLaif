package com.baraaoi.reallife.network;

import com.baraaoi.reallife.RealLife;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class Network {
    private static final int VERSION = 2;
    public static SimpleChannel CHANNEL;

    public static void register() {
        if (CHANNEL != null) return;
        CHANNEL = ChannelBuilder.named(Identifier.fromNamespaceAndPath(RealLife.MODID, "main"))
                .networkProtocolVersion(VERSION)
                .simpleChannel()
                .messageBuilder(ActionPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActionPacket::encode).decoder(ActionPacket::decode).consumerMainThread(ActionPacket::handle).add()
                .messageBuilder(SyncPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPacket::encode).decoder(SyncPacket::decode).consumerMainThread(SyncPacket::handle).add()
                .build();
    }

    public static void toServer(Object msg) {
        CHANNEL.send(msg, PacketDistributor.SERVER.noArg());
    }

    public static void toPlayer(ServerPlayer p, Object msg) {
        CHANNEL.send(msg, PacketDistributor.PLAYER.with(p));
    }
}
