package com.baraaoi.reallife.client;

import net.minecraft.client.Minecraft;

/** Клиентские вызовы из общего кода (через DistExecutor). */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openPhone() {
        Minecraft.getInstance().displayGuiScreen(new PhoneScreen());
    }
}
