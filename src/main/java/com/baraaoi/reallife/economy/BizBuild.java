package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.item.BizPermitItem;
import com.baraaoi.reallife.registry.ModItems;
import com.baraaoi.reallife.structure.Blueprint;
import com.baraaoi.reallife.structure.BizBuildings;
import com.baraaoi.reallife.structure.Buildings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * v1.10/1.11: стройка «под ключ» (лицензия + бригада строителей + всё оборудование) и торговые центры.
 */
public final class BizBuild {
    private BizBuild() {}

    public static final String BUILD = "§6[Стройбригада] §f";

    // ------------------------------------------------------------------ покупка в телефоне

    public static void buyTurnkey(ServerPlayer p, Account a, int typeIdx) {
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
                + "§fПКМ договором по земле — бригада возведёт здание 15×15 перед вами со своей планировкой (" + layoutHint(t) + "), расставит оборудование на "
                + t.maxWorkers + " работников, свет, вывеску" + (com.baraaoi.reallife.event.Inspections.food(t) ? ", раковину" : "")
                + ". Останется закупить сырьё (Опт) и нанять людей в кассе.");
    }

    public static void buyMallPermit(ServerPlayer p, Account a) {
        if (a.captivity != Account.FREE) return;
        if (!BankService.charge(p, a, Mall.PRICE)) {
            BankService.msg(p, BUILD + "§cРазрешение на торговый центр со стройкой стоит " + Finance.money(Mall.PRICE) + ". Не хватает.");
            return;
        }
        BankService.give(p, BizPermitItem.createMall(ModItems.BIZ_PERMIT.get()));
        BankService.msg(p, BUILD + "§a§lРазрешение на ТЦ оформлено! §r§fНужна ровная свободная площадка примерно §e73×62§f блоков "
                + "(вся земля — ваша или ничья). ПКМ разрешением по земле — зал вырастет перед вами.");
        BankService.msg(p, BUILD + "В ТЦ " + Mall.LAYOUT.length + " помещений разного размера. Занимайте сами (что угодно: жильё, любое заведение) "
                + "или продавайте другим игрокам — они ещё и платят вам аренду. Свободные помещения иногда занимают боты-конкуренты: "
                + "аренду платят, но отбивают покупателей. В ТЦ §aприбыль +50%§f и безопасно отмывать §a25%§f прибыли.");
    }

    // ------------------------------------------------------------------ проверка площадки

    /** Свободна ли коробка под стройку. null — можно, иначе причина. */
    
    public static String boxProblem(Level w, Blueprint b, int x1, int y1, int z1, int x2, int y2, int z2, EconomyData data, UUID owner) {
        if (w.dimension() != Level.OVERWORLD) return "строить можно только в обычном мире";
        BlockPos p1 = b.pos(x1, y1, z1), p2 = b.pos(x2, y2, z2);
        BlockPos min = new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
        BlockPos max = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));
        if (min.getY() < w.getMinY() + 3 || max.getY() > w.getMaxY() - 5) return "слишком высоко или низко";
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
        for (BlockPos p : BlockPos.betweenClosed(min.above(), max)) {
            if (w.getBlockEntity(p) != null) {
                return "на месте стройки чьё-то имущество (" + new net.minecraft.world.item.ItemStack(w.getBlockState(p).getBlock()).getHoverName().getString()
                        + " на " + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
            }
        }
        return null;
    }

    
    public static String turnkeyProblem(Level w, BlockPos origin, Direction facing, EconomyData data, UUID owner) {
        Blueprint b = new Blueprint(w, origin, facing);
        String box = boxProblem(w, b, -BizBuildings.SHOP_X, -1, BizBuildings.SHOP_FRONT_MIN, BizBuildings.SHOP_X, BizBuildings.SHOP_H + 2, BizBuildings.SHOP_Z, data, owner);
        if (box != null) return box;
        return LegalBiz.checkSite(w, BizBuildings.shopCounter(b), data, owner);
    }

    
    public static String mallProblem(Level w, BlockPos origin, Direction facing, EconomyData data, UUID owner) {
        Blueprint b = new Blueprint(w, origin, facing);
        String box = boxProblem(w, b, Mall.X1, -1, Mall.Z1 - 5, Mall.X2, Mall.H + 1, Mall.Z2, data, owner);
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
    public static LegalBusiness buildTurnkeyAt(EconomyData data, Level w, BlockPos origin, Direction facing, BizType type, UUID owner, String ownerName) {
        Blueprint b = BizBuildings.buildShop(w, origin, facing, type, ownerName);
        BlockPos c = BizBuildings.shopCounter(b);
        LegalBusiness lb = LegalBiz.register(data, w, c, b.dir(Direction.NORTH), type, owner, ownerName);
        Buildings.claimArea(data, owner, b.min(), b.max(), true);
        data.setDirty();
        return lb;
    }

    /** ПКМ договором «под ключ». */
    public static boolean buildTurnkey(ServerPlayer p, BlockPos clicked, Direction facing, BizType type) {
        if (p.level().getServer() == null) return false;
        EconomyData data = EconomyData.get(p.level().getServer());
        BlockPos origin = clicked.above().relative(facing, 4);
        String problem = turnkeyProblem(p.level(), origin, facing, data, p.getUUID());
        if (problem != null) {
            BankService.msg(p, BUILD + "§cБригада отказалась строить: §f" + problem + ". §7Здание 15×18 встаёт перед вами, вход — к вам.");
            return false;
        }
        LegalBusiness lb = buildTurnkeyAt(data, p.level(), origin, facing, type, p.getUUID(), p.getGameProfile().name());
        LegalBiz.Scan s = LegalBiz.scan(p.level(), lb);
        BankService.msg(p, BUILD + "§a§lГотово! §r§f«" + type.title + "» построено и оборудовано: комплектов §e" + s.sets + "§f из "
                + type.maxWorkers + ", склад " + (s.warehouse ? "§aесть" : "§cнет") + "§f. Касса — в центре зала.");
        BankService.msg(p, LegalBiz.BIZ + "Дальше: сырьё в телефоне (§eОпт → «" + type.supplyName + "»§f) — положите на склад, "
                + "потом ПКМ по кассе → «Нанять» и оформите документы работникам.");
        return true;
    }

    // ------------------------------------------------------------------ торговый центр

    public static Mall buildMallAt(EconomyData data, Level w, BlockPos origin, Direction facing, UUID owner, String ownerName) {
        Blueprint b = BizBuildings.buildMall(w, origin, facing, ownerName);
        Mall m = new Mall();
        m.id = data.nextMallId++;
        m.owner = owner;
        m.ownerName = ownerName == null ? "" : ownerName;
        m.min = box(b, Mall.X1, 0, Mall.Z1, Mall.X2, Mall.H, Mall.Z2)[0];
        m.max = box(b, Mall.X1, 0, Mall.Z1, Mall.X2, Mall.H, Mall.Z2)[1];
        for (int i = 0; i < Mall.LAYOUT.length; i++) {
            int[] l = Mall.LAYOUT[i];
            BlockPos[] bx = box(b, l[0], 0, l[1], l[2], Mall.H - 1, l[3]);
            BizBuildings.UnitGeo g = BizBuildings.geo(l);
            Mall.Unit u = new Mall.Unit();
            u.idx = i;
            u.min = bx[0];
            u.max = bx[1];
            u.entrance = b.pos(g.ex, 1, g.ez).immutable();
            u.facing = b.dir(g.into);
            u.sign = b.pos(g.sx, 5, g.sz).immutable();
            u.width = g.width;
            u.depth = g.depth;
            u.price = Mall.unitPrice(u.area());
            u.rent = Mall.unitRent(u.area());
            m.units.add(u);
        }
        data.malls.add(m);
        Buildings.claimArea(data, owner, b.min().offset(-2, 0, -2), b.max().offset(2, 0, 2), true);
        for (Mall.Unit u : m.units) Malls.updateSign(w, data, m, u);
        data.setDirty();
        return m;
    }

    static BlockPos[] box(Blueprint b, int x1, int y1, int z1, int x2, int y2, int z2) {
        BlockPos p1 = b.pos(x1, y1, z1), p2 = b.pos(x2, y2, z2);
        return new BlockPos[]{
                new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ())),
                new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()))};
    }

    public static boolean buildMall(ServerPlayer p, BlockPos clicked, Direction facing) {
        if (p.level().getServer() == null) return false;
        EconomyData data = EconomyData.get(p.level().getServer());
        BlockPos origin = clicked.above().relative(facing, 6);
        String problem = mallProblem(p.level(), origin, facing, data, p.getUUID());
        if (problem != null) {
            BankService.msg(p, BUILD + "§cТЦ здесь не построить: §f" + problem
                    + ". §7Здание 73×57 встаёт перед вами (вход к вам, по 36 блоков влево и вправо, 57 вглубь).");
            return false;
        }
        Mall m = buildMallAt(data, p.level(), origin, facing, p.getUUID(), p.getGameProfile().name());
        BankService.msg(p, BUILD + "§a§lТорговый центр построен! §r§fАтриум с фонтаном и §e" + m.units.size()
                + "§f помещений разного размера — от киоска до большого зала.");
        BankService.msg(p, BUILD + "Над входом каждого помещения табличка: §eПКМ§f — цена, аренда, кнопки. Своё помещение можно §aзанять бесплатно§f, "
                + "остальные покупают другие игроки (деньги — вам) и платят вам §aаренду каждый день§f. "
                + "Внутри помещения — что угодно: жильё или любое заведение (лицензия любого вида, ПКМ по полу помещения). "
                + "Заведения ТЦ: §aприбыль +50%§f, безопасный отмыв §a25%§f.");
        return m != null;
    }

    static String layoutHint(BizType t) {
        switch (t) {
            case SHAWARMA: return "зал со столиками и стульями, кухня за раздачей, склад";
            case MINIMARKET: return "торговый зал с полками и холодильниками, кассы, подсобка и склад";
            case CAR_WASH: return "два бокса с воротами и сливами, офис с кассами, склад автохимии";
            case QUARRY: return "ангар с воротами, дробилки, будка диспетчера, сарай-склад";
            case PAWNSHOP: return "зал с витринами, окно оценщика за решёткой, хранилище с сейфами, склад";
            case GAS_STATION: return "колонки под навесом, магазин с кассами, склад";
            default: return "витрины-острова, кассы и сервис, мастерская и склад";
        }
    }

    public static String shortTitle(BizType t) {
        int i = t.title.indexOf(" «");
        return i > 0 ? t.title.substring(0, i) : t.title;
    }
}
