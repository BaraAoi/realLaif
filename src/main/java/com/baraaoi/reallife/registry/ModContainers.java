package com.baraaoi.reallife.registry;

import com.baraaoi.reallife.container.CasinoContainer;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.container.FinanceContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModContainers {
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RealLife.MODID);

    public static final RegistryObject<MenuType<FinanceContainer>> BANK = CONTAINERS.register("bank",
            () -> IForgeMenuType.create((id, inv, buf) -> new FinanceContainer(id, inv, buf.readBlockPos(), false)));

    public static final RegistryObject<MenuType<FinanceContainer>> MFO = CONTAINERS.register("mfo",
            () -> IForgeMenuType.create((id, inv, buf) -> new FinanceContainer(id, inv, buf.readBlockPos(), true)));

    public static final RegistryObject<MenuType<CasinoContainer>> CASINO = CONTAINERS.register("casino",
            () -> IForgeMenuType.create((id, inv, buf) -> new CasinoContainer(id, inv, buf.readBlockPos(), buf.readVarInt())));

    public static final RegistryObject<MenuType<com.baraaoi.reallife.container.PrinterContainer>> PRINTER = CONTAINERS.register("money_printer",
            () -> IForgeMenuType.create((id, inv, buf) -> new com.baraaoi.reallife.container.PrinterContainer(id, inv, buf.readBlockPos())));

    public static final RegistryObject<MenuType<com.baraaoi.reallife.container.BizContainer>> BIZ = CONTAINERS.register("biz_counter",
            () -> IForgeMenuType.create((id, inv, buf) -> new com.baraaoi.reallife.container.BizContainer(id, inv, buf.readBlockPos())));

    public static final RegistryObject<MenuType<com.baraaoi.reallife.container.CryptoContainer>> CRYPTO = CONTAINERS.register("crypto_rig",
            () -> IForgeMenuType.create((id, inv, buf) -> new com.baraaoi.reallife.container.CryptoContainer(id, inv, buf.readBlockPos())));
}
