package com.baraaoi.reallife.client;

import net.minecraft.nbt.CompoundNBT;

/** Последние данные счёта, присланные сервером (без клиентских импортов — безопасно на сервере). */
public class ClientAccountData {
    private static CompoundNBT data = new CompoundNBT();

    public static void set(CompoundNBT t) {
        data = t == null ? new CompoundNBT() : t;
    }

    public static CompoundNBT get() {
        return data;
    }
}
