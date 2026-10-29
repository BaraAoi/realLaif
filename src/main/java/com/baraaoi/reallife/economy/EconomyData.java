package com.baraaoi.reallife.economy;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Хранилище всех счетов (сохраняется вместе с миром, в измерении overworld).
 */
public class EconomyData extends WorldSavedData {
    public static final String NAME = "reallife_economy";

    private final Map<UUID, Account> accounts = new HashMap<>();
    public long lastDay = -1;
    public boolean jailBuilt;
    public BlockPos jailPos = BlockPos.ZERO;
    public boolean pitBuilt;
    public BlockPos pitPos = BlockPos.ZERO;

    public EconomyData() {
        super(NAME);
    }

    public static ServerWorld overworld(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD);
    }

    public static EconomyData get(MinecraftServer server) {
        return overworld(server).getSavedData().getOrCreate(EconomyData::new, NAME);
    }

    public Account get(PlayerEntity player) {
        Account a = accounts.computeIfAbsent(player.getUniqueID(), Account::new);
        a.name = player.getGameProfile().getName();
        if (a.createdDay < 0 && player.world != null) {
            a.createdDay = player.world.getDayTime() / 24000L;
        }
        return a;
    }

    public Account get(UUID id) {
        return accounts.get(id);
    }

    public Account getOrCreate(UUID id) {
        return accounts.computeIfAbsent(id, Account::new);
    }

    public Collection<Account> all() {
        return accounts.values();
    }

    public void reset(UUID id) {
        accounts.remove(id);
        markDirty();
    }

    @Override
    public void read(CompoundNBT nbt) {
        accounts.clear();
        ListNBT list = nbt.getList("accounts", 10);
        for (INBT n : list) {
            Account a = Account.read((CompoundNBT) n);
            accounts.put(a.id, a);
        }
        lastDay = nbt.getLong("lastDay");
        jailBuilt = nbt.getBoolean("jailBuilt");
        jailPos = BlockPos.fromLong(nbt.getLong("jailPos"));
        pitBuilt = nbt.getBoolean("pitBuilt");
        pitPos = BlockPos.fromLong(nbt.getLong("pitPos"));
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        ListNBT list = new ListNBT();
        for (Account a : accounts.values()) list.add(a.write());
        nbt.put("accounts", list);
        nbt.putLong("lastDay", lastDay);
        nbt.putBoolean("jailBuilt", jailBuilt);
        nbt.putLong("jailPos", jailPos.toLong());
        nbt.putBoolean("pitBuilt", pitBuilt);
        nbt.putLong("pitPos", pitPos.toLong());
        return nbt;
    }
}
