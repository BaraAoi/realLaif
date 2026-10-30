package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.item.BizPermitItem;
import com.baraaoi.reallife.registry.ModItems;
import com.baraaoi.reallife.structure.Blueprint;
import com.baraaoi.reallife.structure.BizBuildings;
import com.baraaoi.reallife.structure.Buildings;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * v1.10: стройка «под ключ» (лицензия + бригада строителей + всё оборудование) и торговые центры.
 */
public final class BizBuild {
    private BizBuild() {}

    public static final String BUILD = "§6[Стройбригада] §f";

    // ------------------------------------------------------------------ покупка в телефоне

    public static void buyTurnkey(ServerPlayerEntity p, Account a, int typeIdx) {
        BizType t = BizType.byId(typeIdx);
        if (t == null) return;
        if (a.captivity != Account.FREE) return;
        int price = t.turnkeyPrice();
        if (!BankService.charge(p, a, price)) {
            BankService.msg(p, BUILD + "§c«" + t.title + "» под ключ стоит " + Finance.money(price) + " (лицензия " + Finance.money(t.price)
                    + " + оборудование " + Finance.money(t.fullEquipPrice()) + " + работа бригады " + Finance.money(BizType.BUILD_FEE) + "). Не хватает.");
            return;
        }
        BankService.give(p, BizPermitItem.createTurnkey(ModItems.BIZ_PERMIT.get(), t));
        BankService.msg(p, BUILD + "§aДоговор подписан: «" + t.title + "» под ключ за §c" + Finance.money(price) + "§a. "
                + "§fПКМ договором по земле — бригада за минуту возведёт здание 13×13 перед вами, расставит оборудование на "
                + t.maxWorkers + " работников, свет, вывеску" + (com.baraaoi.reallife.event.Inspections.food(t) ? ", раковину" : "")
                + ". Останется закупить сырьё (Опт) и нанять людей в кассе.");
    }

    public static void buyMallPermit(ServerPlayerEntity p, Account a) {
        if (a.captivity != Account.FREE) return;
        if (!BankService.charge(p, a, Mall.PRICE)) {
            BankService.msg(p, BUILD + "§cРазрешение на торговый центр со стройкой стоит " + Finance.money(Mall.PRICE) + ". Не хватает.");
            return;
        }
        BankService.give(p, BizPermitItem.createMall(ModItems.BIZ_PERMIT.get()));
        BankService.msg(p, BUILD + "§a§lРазрешение на ТЦ оформлено! §r§fНужна ровная свободная площадка примерно §e71×45§f блоков "
                + "(вся земля — ваша или ничья). ПКМ разрешением по земле — зал вырастет перед вами.");
        BankService.msg(p, BUILD + "В зале 8 размеченных мест: шаурмичные, мини-маркеты, ломбарды, магазины техники (по 2). "
                + "ПКМ обычной лицензией по центру своего места. В ТЦ §aприбыль +50%§f и безопасно отмывать §a25%§f прибыли.");
    }

    // ------------------------------------------------------------------ проверка площадки

    /** Свободна ли коробка под стройку. null — можно, иначе причина. */
    @Nullable
    public static String boxProblem(World w, Blueprint b, int x1, int y1, int z1, int x2, int y2, int z2, EconomyData data, UUID owner) {
        if (w.getDimensionKey() != World.OVERWORLD) return "строить можно только в обычном мире";
        BlockPos p1 = b.pos(x1, y1, z1), p2 = b.pos(x2, y2, z2);
        BlockPos min = new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
        BlockPos max = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));
        if (min.getY() < 3 || max.getY() > 250) return "слишком высоко или низко";
        for (Mall m : data.malls) {
            if (min.getX() <= m.max.getX() + 1 && max.getX() >= m.min.getX() - 1 && min.getZ() <= m.max.getZ() + 1
                    && max.getZ() >= m.min.getZ() - 1 && min.getY() <= m.max.getY() + 1 && max.getY() >= m.min.getY() - 1) {
                return "здесь торговый центр" + (owner.equals(m.owner) ? " (ваш): в ТЦ открывайте заведения обычной лицензией на размеченных местах" : "");
            }
        }
        for (LegalBusiness lb : data.businesses) {
            BlockPos c = lb.counter;
            if (c.getX() >= min.getX() - 1 && c.getX() <= max.getX() + 1 && c.getZ() >= min.getZ() - 1 && c.getZ() <= max.getZ() + 1
                    && c.getY() >= min.getY() - 3 && c.getY() <= max.getY() + 3) {
                return "здесь стоит заведение «" + lb.type.title + "»" + (owner.equals(lb.owner) ? " (ваше)" : "");
            }
        }
        String occ = Buildings.checkOccupied(min, max, data, owner);
        if (occ != null) return occ;
        for (BlockPos p : BlockPos.getAllInBoxMutable(min.up(), max)) {
            if (w.getTileEntity(p) != null) {
                return "на месте стройки чьё-то имущество (" + new net.minecraft.item.ItemStack(w.getBlockState(p).getBlock()).getDisplayName().getString()
                        + " на " + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
            }
        }
        return null;
    }

    @Nullable
    public static String turnkeyProblem(World w, BlockPos origin, Direction facing, EconomyData data, UUID owner) {
        Blueprint b = new Blueprint(w, origin, facing);
        String box = boxProblem(w, b, -BizBuildings.SHOP_X, -1, -2, BizBuildings.SHOP_X, BizBuildings.SHOP_H + 2, BizBuildings.SHOP_Z, data, owner);
        if (box != null) return box;
        return LegalBiz.checkSite(w, BizBuildings.shopCounter(b), data, owner);
    }

    @Nullable
    public static String mallProblem(World w, BlockPos origin, Direction facing, EconomyData data, UUID owner) {
        Blueprint b = new Blueprint(w, origin, facing);
        String box = boxProblem(w, b, Mall.X1, -1, Mall.Z1 - 4, Mall.X2, Mall.H + 1, Mall.Z2, data, owner);
        if (box != null) return box;
        for (LegalBusiness lb : data.businesses) {
            // зоны чужих заведений не должны залезать в зал
            BlockPos p1 = b.pos(Mall.X1, 0, Mall.Z1), p2 = b.pos(Mall.X2, Mall.H, Mall.Z2);
            int r = LegalBusiness.R_XZ;
            if (lb.counter.getX() >= Math.min(p1.getX(), p2.getX()) - r && lb.counter.getX() <= Math.max(p1.getX(), p2.getX()) + r
                    && lb.counter.getZ() >= Math.min(p1.getZ(), p2.getZ()) - r && lb.counter.getZ() <= Math.max(p1.getZ(), p2.getZ()) + r
                    && Math.abs(lb.counter.getY() - origin.getY()) <= LegalBusiness.R_UP + Mall.H) {
                return "слишком близко к заведению «" + lb.type.title + "»";
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ стройка «под ключ»

    /** Построить здание и открыть в нём заведение (без проверок и денег — их делает вызывающий). */
    public static LegalBusiness buildTurnkeyAt(EconomyData data, World w, BlockPos origin, Direction facing, BizType type, UUID owner, String ownerName) {
        Blueprint b = BizBuildings.buildShop(w, origin, facing, type, ownerName);
        BlockPos c = BizBuildings.shopCounter(b);
        LegalBusiness lb = LegalBiz.register(data, w, c, b.dir(Direction.NORTH), type, owner, ownerName);
        Buildings.claimArea(data, owner, b.min(), b.max(), true);
        data.markDirty();
        return lb;
    }

    /** ПКМ договором «под ключ». */
    public static boolean buildTurnkey(ServerPlayerEntity p, BlockPos clicked, Direction facing, BizType type) {
        if (p.getServer() == null) return false;
        EconomyData data = EconomyData.get(p.getServer());
        BlockPos origin = clicked.up().offset(facing, 3);
        String problem = turnkeyProblem(p.world, origin, facing, data, p.getUniqueID());
        if (problem != null) {
            BankService.msg(p, BUILD + "§cБригада отказалась строить: §f" + problem + ". §7Здание 13×13 встаёт перед вами, вход — к вам.");
            return false;
        }
        LegalBusiness lb = buildTurnkeyAt(data, p.world, origin, facing, type, p.getUniqueID(), p.getGameProfile().getName());
        LegalBiz.Scan s = LegalBiz.scan(p.world, lb);
        BankService.msg(p, BUILD + "§a§lГотово! §r§f«" + type.title + "» построено и оборудовано: комплектов §e" + s.sets + "§f из "
                + type.maxWorkers + ", склад " + (s.warehouse ? "§aесть" : "§cнет") + "§f. Касса — в центре зала.");
        BankService.msg(p, LegalBiz.BIZ + "Дальше: сырьё в телефоне (§eОпт → «" + type.supplyName + "»§f) — положите на склад, "
                + "потом ПКМ по кассе → «Нанять» и оформите документы работникам.");
        return true;
    }

    // ------------------------------------------------------------------ торговый центр

    public static Mall buildMallAt(EconomyData data, World w, BlockPos origin, Direction facing, UUID owner, String ownerName) {
        Blueprint b = BizBuildings.buildMall(w, origin, facing, ownerName);
        Mall m = new Mall();
        m.id = data.nextMallId++;
        m.owner = owner;
        m.ownerName = ownerName == null ? "" : ownerName;
        BlockPos p1 = b.pos(Mall.X1, 0, Mall.Z1), p2 = b.pos(Mall.X2, Mall.H, Mall.Z2);
        m.min = new BlockPos(Math.min(p1.getX(), p2.getX()), p1.getY(), Math.min(p1.getZ(), p2.getZ()));
        m.max = new BlockPos(Math.max(p1.getX(), p2.getX()), p2.getY(), Math.max(p1.getZ(), p2.getZ()));
        for (int i = 0; i < Mall.LAYOUT.length; i++) {
            m.slots.add(b.pos(Mall.SLOT_X[i], 1, Mall.SLOT_Z[i]).toImmutable());
            m.types.add(Mall.LAYOUT[i]);
        }
        data.malls.add(m);
        Buildings.claimArea(data, owner, b.min().add(-2, 0, -2), b.max().add(2, 0, 2), true);
        data.markDirty();
        return m;
    }

    public static boolean buildMall(ServerPlayerEntity p, BlockPos clicked, Direction facing) {
        if (p.getServer() == null) return false;
        EconomyData data = EconomyData.get(p.getServer());
        BlockPos origin = clicked.up().offset(facing, 5);
        String problem = mallProblem(p.world, origin, facing, data, p.getUniqueID());
        if (problem != null) {
            BankService.msg(p, BUILD + "§cТЦ здесь не построить: §f" + problem
                    + ". §7Зал 71×45 встаёт перед вами (вход к вам, по 35 блоков влево и вправо).");
            return false;
        }
        Mall m = buildMallAt(data, p.world, origin, facing, p.getUniqueID(), p.getGameProfile().getName());
        BankService.msg(p, BUILD + "§a§lТорговый центр построен! §r§fВ зале 8 мест — цветные площадки с табличкой и меткой в центре.");
        BankService.msg(p, BUILD + "Купи лицензию нужного вида (телефон → Бизнес) и ПКМ ею по центру площадки — касса встанет ровно в центр. "
                + "Заведения ТЦ: §aприбыль +50%§f, безопасный отмыв §a25%§f вчерашней прибыли. Оборудование — в пределах площадки.");
        return m != null;
    }

    /** Открыть заведение на месте ТЦ (без проверок и денег). Используется и самотестом. */
    public static LegalBusiness openInMall(EconomyData data, World w, Mall m, int slot, Direction facing, UUID owner, String ownerName) {
        LegalBusiness b = LegalBiz.register(data, w, m.slots.get(slot), facing, m.types.get(slot), owner, ownerName);
        b.mallId = m.id;
        data.markDirty();
        return b;
    }

    /** Для телефона: ТЦ игрока и заполненность. */
    public static ListNBT mallList(EconomyData data, UUID owner) {
        ListNBT l = new ListNBT();
        for (Mall m : data.mallsOf(owner)) {
            CompoundNBT t = new CompoundNBT();
            t.putInt("id", m.id);
            t.putInt("x", (m.min.getX() + m.max.getX()) / 2);
            t.putInt("y", m.min.getY());
            t.putInt("z", (m.min.getZ() + m.max.getZ()) / 2);
            t.putInt("used", m.used(data));
            t.putInt("total", m.slots.size());
            StringBuilder free = new StringBuilder();
            for (int i = 0; i < m.slots.size(); i++) {
                if (m.occupant(data, i) != null) continue;
                if (free.length() > 0) free.append(", ");
                free.append(shortTitle(m.types.get(i)));
            }
            t.putString("free", free.toString());
            l.add(t);
        }
        return l;
    }

    public static String shortTitle(BizType t) {
        int i = t.title.indexOf(" «");
        return i > 0 ? t.title.substring(0, i) : t.title;
    }
}
