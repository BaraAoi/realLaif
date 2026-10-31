package com.baraaoi.reallife.client;

import net.minecraft.nbt.CompoundTag;

/** Последние данные счёта, присланные сервером (без клиентских импортов — безопасно на сервере). */
public class ClientAccountData {
    private static CompoundTag data = new CompoundTag();

    public static void set(CompoundTag t) {
        data = t == null ? new CompoundTag() : t;
    }

    public static CompoundTag get() {
        return data;
    }
}
