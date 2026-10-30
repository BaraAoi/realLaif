package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.tile.PrinterTileEntity;
import com.baraaoi.reallife.registry.ModBlocks;
import com.baraaoi.reallife.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Iterator;

/**
 * Подпольный бизнес v5: станки покупаются в МФО (без лимита) и ставятся самим игроком — в своём доме,
 * на своём участке (или в старом подвале цеха). Каждый станок печатает купюры выбранного номинала
 * в «Сундук цеха» (или в свой лоток). Засвеченные станки ищет ОБЭП — см. {@link com.baraaoi.reallife.event.Raids}.
 */
public final class Business {
    private Business() {}

    public static final int OK = 0, NOT_OWNER = 1, ALREADY = 3;

    private static String xyz(BlockPos p) {
        return p.getX() + ", " + p.getY() + ", " + p.getZ();
    }

    /** Точка внутри своей собственности: дом (с запасом в 1 блок), свой чанк или старый подвал цеха. */
    public static boolean inOwnProperty(EconomyData data, Account a, BlockPos pos) {
        if (a.hasHouse && !a.houseMin.equals(BlockPos.ZERO)) {
            if (pos.getX() >= a.houseMin.getX() - 1 && pos.getX() <= a.houseMax.getX() + 1
                    && pos.getZ() >= a.houseMin.getZ() - 1 && pos.getZ() <= a.houseMax.getZ() + 1
                    && pos.getY() >= a.houseMin.getY() - 8 && pos.getY() <= a.houseMax.getY() + 2) return true;
        }
        if (!a.businessMin.equals(BlockPos.ZERO) && !a.businessMax.equals(BlockPos.ZERO)
                && pos.getX() >= a.businessMin.getX() && pos.getX() <= a.businessMax.getX()
                && pos.getY() >= a.businessMin.getY() && pos.getY() <= a.businessMax.getY()
                && pos.getZ() >= a.businessMin.getZ() && pos.getZ() <= a.businessMax.getZ()) return true;
        return a.id.equals(data.owner(EconomyData.chunkKey(pos)));
    }

    /** Зарегистрировать станок за игроком. */
    public static int registerPrinter(EconomyData data, Account a, BlockPos pos) {
        BlockPos ip = pos.toImmutable();
        if (a.printerList.contains(ip)) return ALREADY;
        if (!inOwnProperty(data, a, ip)) return NOT_OWNER;
        a.printerList.add(ip);
        data.markDirty();
        return OK;
    }

    /** Сколько зарегистрированных станков реально стоит на месте (лишние записи вычищаются). */
    public static int workingPrinters(ServerWorld ow, Account a) {
        Iterator<BlockPos> it = a.printerList.iterator();
        int n = 0;
        while (it.hasNext()) {
            BlockPos p = it.next();
            if (!ow.isBlockLoaded(p)) { n++; continue; } // не загружен — считаем, что стоит
            if (ow.getBlockState(p).getBlock() != ModBlocks.MONEY_PRINTER.get()) it.remove();
            else n++;
        }
        return n;
    }

    /** Старый мир: станки из подвала цеха перенести в список. */
    public static void migrateLegacy(ServerWorld ow, Account a) {
        if (a.businessMin.equals(BlockPos.ZERO) || a.businessMax.equals(BlockPos.ZERO)) return;
        long volume = (long) (a.businessMax.getX() - a.businessMin.getX() + 1) * (a.businessMax.getY() - a.businessMin.getY() + 1)
                * (a.businessMax.getZ() - a.businessMin.getZ() + 1);
        if (volume <= 0 || volume > 4000) return;
        for (BlockPos p : BlockPos.getAllInBoxMutable(a.businessMin, a.businessMax)) {
            if (ow.getBlockState(p).getBlock() == ModBlocks.MONEY_PRINTER.get() && !a.printerList.contains(p)) {
                BlockPos ip = p.toImmutable();
                a.printerList.add(ip);
                TileEntity te = ow.getTileEntity(ip);
                if (te instanceof PrinterTileEntity && ((PrinterTileEntity) te).getOwner() == null) ((PrinterTileEntity) te).setOwner(a.id);
            }
        }
    }

    static void give(PlayerEntity p, ItemStack st) {
        if (!p.inventory.addItemStackToInventory(st)) p.dropItem(st, false);
    }

    // ------------------------------------------------------------------ события блоков

    /** Игрок поставил блок (вызывается после всех проверок). */
    public static void onPlaced(ServerPlayerEntity p, BlockPos pos, Block block) {
        MinecraftServer server = p.getServer();
        if (server == null || p.world.getDimensionKey() != World.OVERWORLD) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(p);
        if (block == ModBlocks.MONEY_PRINTER.get()) {
            if (!a.hasBusiness) {
                BankService.msg(p, BankService.MFO + "§7Станок без хозяина — просто железка. Оформи цех: купи свой первый станок у нас.");
                return;
            }
            int r = registerPrinter(data, a, pos);
            if (r == OK || r == ALREADY) {
                TileEntity te = p.world.getTileEntity(pos);
                if (te instanceof PrinterTileEntity) ((PrinterTileEntity) te).setOwner(a.id);
            }
            if (r == OK) {
                BankService.msg(p, BankService.MFO + "§aСтанок подключён (" + xyz(pos) + "). §fРаботает станков: §e" + a.printerList.size()
                        + "§f. ПКМ по станку — выбрать номинал."
                        + (a.businessChest.equals(BlockPos.ZERO) ? "§e Сундука цеха нет — купюры копятся в лотке станка." : " Купюры падают в сундук цеха."));
                BankService.msg(p, BankService.MFO + "§7Прячь его: если станок увидит посторонний (гость, коллектор, житель), "
                        + "он стуканёт — и придёт ОБЭП. Они раскопают даже замурованный станок.");
            } else if (r == NOT_OWNER) {
                BankService.msg(p, BankService.MFO + "§cСтанок не подключён: ставь его в своём доме или на своём участке (/reallife land buy). Тут спалят.");
            }
        } else if (block == ModBlocks.BUSINESS_CHEST.get()) {
            if (!a.hasBusiness) return;
            a.businessChest = pos.toImmutable();
            data.markDirty();
            BankService.msg(p, BankService.MFO + "§aСундук цеха привязан (" + xyz(pos) + "). §fВсе твои станки будут складывать сюда грязные. "
                    + "Станков подключено: §e" + a.printerList.size());
        } else if (block == ModBlocks.LAUNDER_MACHINE.get()) {
            BankService.msg(p, "§8[ООО «Отмыв денег»] §7ООО ликвидировано налоговой. Отмывать теперь можно через кассу своего заведения (телефон → Бизнес).");
        }
    }

    /** Кто-то сломал блок. */
    public static void onBroken(MinecraftServer server, BlockPos pos, BlockState state) {
        Block b = state.getBlock();
        if (b != ModBlocks.MONEY_PRINTER.get() && b != ModBlocks.BUSINESS_CHEST.get() && b != ModBlocks.LAUNDER_MACHINE.get()) return;
        EconomyData data = EconomyData.get(server);
        for (Account a : data.all()) {
            boolean changed = false;
            if (b == ModBlocks.MONEY_PRINTER.get()) changed = a.printerList.remove(pos);
            if (b == ModBlocks.BUSINESS_CHEST.get() && pos.equals(a.businessChest)) { a.businessChest = BlockPos.ZERO; changed = true; }
            if (b == ModBlocks.LAUNDER_MACHINE.get() && pos.equals(a.launderPos)) { a.launderPos = BlockPos.ZERO; changed = true; }
            if (changed) {
                data.markDirty();
                PlayerEntity owner = server.getPlayerList().getPlayerByUUID(a.id);
                if (owner != null) {
                    String what = b == ModBlocks.MONEY_PRINTER.get() ? "Станок снят (осталось " + a.printerList.size() + "). Поставь его снова у себя — подключится."
                            : b == ModBlocks.BUSINESS_CHEST.get() ? "Сундук цеха снят — станки встанут, пока не поставишь его снова."
                            : "Аппарат ООО снят — поставь его снова у себя дома.";
                    BankService.msg(owner, BankService.MFO + "§e" + what);
                }
            }
        }
    }

    // ------------------------------------------------------------------ покупки в МФО

    static void buyPrinter(ServerPlayerEntity p, Account a, boolean checkFree) {
        int price = Config.PRINTER_COST.get();
        if (!BankService.charge(p, a, price)) { BankService.msg(p, BankService.MFO + "§cСтанок стоит " + Finance.money(price) + ". Счёт + наличка — не хватает."); return; }
        boolean first = !a.hasBusiness;
        a.hasBusiness = true;
        a.printers++;
        give(p, new ItemStack(ModItems.MONEY_PRINTER.get()));
        if (!a.chestGiven) {
            a.chestGiven = true;
            give(p, new ItemStack(ModItems.BUSINESS_CHEST.get()));
        }
        EconomyData.get(p.getServer()).markDirty();
        BankService.msg(p, BankService.MFO + "§aДержи станок" + (first ? " и «Сундук цеха» (выдаём один раз — не потеряй!)" : "") + ". "
                + "§fСтавь у себя дома или на своём участке. Номинал выбираешь сам: 10₽ за 5 с … 5000₽ за 50 мин. Куплено станков: §e" + a.printers);
        BankService.msg(p, BankService.MFO + "§7Чем больше станков — тем чаще к тебе заглядывают подозрительные личности и проверки. Не свети станки.");
        if (first) BankService.msg(p, BankService.MFO + "§7В банке грязные палят в " + Finance.percent(Config.DIRTY_CAUGHT_CHANCE.get())
                + " случаев. Отмыть: у нас (−" + Finance.percent(Config.MFO_LAUNDER_COMMISSION.get())
                + ") или через кассу своего легального заведения.");
    }

    static void buyLaunder(ServerPlayerEntity p, Account a) {
        BankService.msg(p, BankService.MFO + "§cООО «Отмыв» больше не регистрируем — налоговая всех накрыла. "
                + "Купи легальное заведение (телефон → Бизнес) и проводи грязные через его кассу.");
    }
}
