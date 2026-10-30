package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.item.DirtyMoneyItem;
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
 * Подпольный бизнес v4: станки покупаются в МФО и ставятся самим игроком — в своём доме,
 * на своём участке (или в старом подвале цеха). Все станки печатают в один «Сундук цеха»,
 * который выдаётся один раз при первой покупке.
 */
public final class Business {
    private Business() {}

    public static final int OK = 0, NOT_OWNER = 1, LIMIT = 2, ALREADY = 3;

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
        if (a.printerList.size() >= Config.MAX_PRINTERS.get()) return LIMIT;
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
        return Math.min(n, Config.MAX_PRINTERS.get());
    }

    /** Старый мир: станки из подвала цеха перенести в список. */
    public static void migrateLegacy(ServerWorld ow, Account a) {
        if (a.businessMin.equals(BlockPos.ZERO) || a.businessMax.equals(BlockPos.ZERO)) return;
        long volume = (long) (a.businessMax.getX() - a.businessMin.getX() + 1) * (a.businessMax.getY() - a.businessMin.getY() + 1)
                * (a.businessMax.getZ() - a.businessMin.getZ() + 1);
        if (volume <= 0 || volume > 4000) return;
        for (BlockPos p : BlockPos.getAllInBoxMutable(a.businessMin, a.businessMax)) {
            if (ow.getBlockState(p).getBlock() == ModBlocks.MONEY_PRINTER.get() && !a.printerList.contains(p)
                    && a.printerList.size() < Config.MAX_PRINTERS.get()) a.printerList.add(p.toImmutable());
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
            if (r == OK) {
                BankService.msg(p, BankService.MFO + "§aСтанок подключён (" + xyz(pos) + "). §fРаботает станков: §e" + a.printerList.size()
                        + "§f → §2" + Finance.money(a.printerList.size() * Config.PRINTER_DAILY.get()) + "§f/день"
                        + (a.businessChest.equals(BlockPos.ZERO) ? "§c. Поставь «Сундук цеха» — без него печатать некуда!" : " в сундук цеха."));
            } else if (r == NOT_OWNER) {
                BankService.msg(p, BankService.MFO + "§cСтанок не подключён: ставь его в своём доме или на своём участке (/reallife land buy). Тут спалят.");
            } else if (r == LIMIT) {
                BankService.msg(p, BankService.MFO + "§cБольше " + Config.MAX_PRINTERS.get() + " станков — соседи услышат. Этот работать не будет.");
            }
        } else if (block == ModBlocks.BUSINESS_CHEST.get()) {
            if (!a.hasBusiness) return;
            a.businessChest = pos.toImmutable();
            data.markDirty();
            BankService.msg(p, BankService.MFO + "§aСундук цеха привязан (" + xyz(pos) + "). §fВсе твои станки будут складывать сюда грязные. "
                    + "Станков подключено: §e" + a.printerList.size());
        } else if (block == ModBlocks.LAUNDER_MACHINE.get()) {
            if (!a.hasLaundering) {
                BankService.msg(p, "§8[ООО «Отмыв денег»] §7Аппарат не зарегистрирован на тебя — это просто железный ящик.");
                return;
            }
            if (!inOwnProperty(data, a, pos)) {
                BankService.msg(p, "§8[ООО «Отмыв денег»] §cЮрадрес ООО — только твой дом или участок. Тут аппарат работать не будет.");
                return;
            }
            a.launderPos = pos.toImmutable();
            data.markDirty();
            BankService.msg(p, "§8[ООО «Отмыв денег»] §aАппарат установлен (" + xyz(pos) + "). §fПКМ — отмыть все грязные из карманов, комиссия "
                    + Finance.percent(Config.LAUNDER_COMMISSION.get()) + ".");
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
        int max = Config.MAX_PRINTERS.get();
        if (a.printers >= max) { BankService.msg(p, BankService.MFO + "§cБольше " + max + " станков не продаём — соседи услышат."); return; }
        int price = Config.PRINTER_PRICE.get();
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
                + "§fСтавь у себя дома или на своём участке: каждый печатает §2" + Finance.money(Config.PRINTER_DAILY.get())
                + "§f грязными в день, всё падает в сундук цеха. Куплено станков: §e" + a.printers + "/" + max);
        if (first) BankService.msg(p, BankService.MFO + "§7В банке грязные палят в " + Finance.percent(Config.DIRTY_CAUGHT_CHANCE.get())
                + " случаев. Отмыть: у нас (−" + Finance.percent(Config.MFO_LAUNDER_COMMISSION.get())
                + ") или своим ООО (−" + Finance.percent(Config.LAUNDER_COMMISSION.get()) + ").");
    }

    static void buyLaunder(ServerPlayerEntity p, Account a) {
        ServerWorld ow = EconomyData.overworld(p.getServer());
        boolean placed = !a.launderPos.equals(BlockPos.ZERO) && ow.getBlockState(a.launderPos).getBlock() == ModBlocks.LAUNDER_MACHINE.get();
        if (a.hasLaundering && placed) {
            BankService.msg(p, BankService.MFO + "ООО уже на тебя оформлено — аппарат стоит (" + xyz(a.launderPos) + ").");
            return;
        }
        if (a.hasLaundering) {
            // ООО есть, аппарата нет — новый аппарат за четверть цены
            int price = Math.max(0, Config.LAUNDER_PRICE.get() / 4);
            if (!BankService.charge(p, a, price)) { BankService.msg(p, BankService.MFO + "§cНовый аппарат — " + Finance.money(price) + ". Не хватает."); return; }
            give(p, new ItemStack(ModItems.LAUNDER_MACHINE.get()));
            BankService.msg(p, BankService.MFO + "Новый аппарат ООО за " + Finance.money(price) + ". Поставь у себя дома.");
            return;
        }
        int price = Config.LAUNDER_PRICE.get();
        if (!BankService.charge(p, a, price)) { BankService.msg(p, BankService.MFO + "§cООО стоит " + Finance.money(price) + ". Не хватает."); return; }
        a.hasLaundering = true;
        give(p, new ItemStack(ModItems.LAUNDER_MACHINE.get()));
        EconomyData.get(p.getServer()).markDirty();
        BankService.msg(p, BankService.MFO + "§aООО «Отмыв денег» зарегистрировано! §fДиректор — твоя бабушка. Держи аппарат: поставь его у себя "
                + "дома или на участке, ПКМ — отмывает все грязные из карманов, комиссия " + Finance.percent(Config.LAUNDER_COMMISSION.get()) + ".");
    }

    // ------------------------------------------------------------------ производство

    /** Положить грязные деньги в сундук цеха. Возвращает, сколько пачек влезло. */
    public static int produceDirty(MinecraftServer server, Account a) {
        if (!a.hasBusiness) return 0;
        ServerWorld ow = EconomyData.overworld(server);
        int working = workingPrinters(ow, a);
        int packs = working * Config.PRINTER_DAILY.get() / DirtyMoneyItem.VALUE;
        if (packs <= 0 || a.businessChest.equals(BlockPos.ZERO)) return 0;
        TileEntity te = ow.getTileEntity(a.businessChest);
        if (!(te instanceof IInventory)) return 0;
        IInventory inv = (IInventory) te;
        int left = packs;
        for (int i = 0; i < inv.getSizeInventory() && left > 0; i++) {
            ItemStack cur = inv.getStackInSlot(i);
            if (cur.isEmpty()) {
                int n = Math.min(64, left);
                inv.setInventorySlotContents(i, new ItemStack(ModItems.DIRTY_MONEY.get(), n));
                left -= n;
            } else if (cur.getItem() == ModItems.DIRTY_MONEY.get() && cur.getCount() < 64) {
                int n = Math.min(64 - cur.getCount(), left);
                cur.grow(n);
                left -= n;
            }
        }
        te.markDirty();
        return packs - left;
    }
}
