package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.container.BizContainer;
import com.baraaoi.reallife.entity.WorkerEntity;
import com.baraaoi.reallife.event.DebtManager;
import com.baraaoi.reallife.event.Visitors;
import com.baraaoi.reallife.item.BizPermitItem;
import com.baraaoi.reallife.item.DirtyMoneyItem;
import com.baraaoi.reallife.item.SupplyItem;
import com.baraaoi.reallife.registry.ModBlocks;
import com.baraaoi.reallife.registry.ModEntities;
import com.baraaoi.reallife.registry.ModItems;
import com.baraaoi.reallife.tile.FridgeTileEntity;
import com.baraaoi.reallife.tile.ShelfTileEntity;
import com.baraaoi.reallife.tile.PrinterTileEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Легальный бизнес v6: игрок САМ строит заведение. Лицензия ставит главную кассу на своей земле, вокруг неё —
 * зона 17×10×17, где игрок расставляет купленное оборудование (полки, холодильники, гриль, мойки...).
 * Сколько полных комплектов оборудования — столько работников можно нанять (но не больше потолка вида).
 * Каждый работник расходует сырьё (закупка в телефоне → «Опт», доставляется прямо на полки/в холодильники зоны).
 * Отмыв через кассу — в пределах вчерашней прибыли.
 */
public final class LegalBiz {
    private LegalBiz() {}

    public static final String BIZ = "§a[Бизнес] §f";
    public static final String TAX = "§6§l[ФНС] §r§f";
    private static final Random R = new Random();

    // ------------------------------------------------------------------ удача

    /** Итог дня: множитель базовой прибыли и описание. */
    public static final class Roll {
        public final double mult;
        public final String note;
        public final int kind;

        Roll(double mult, String note, int kind) {
            this.mult = mult;
            this.note = note;
            this.kind = kind;
        }
    }

    public static final int BAD = 0, NORMAL = 1, GOOD = 2, JACKPOT = 3;

    /** v1.8: плохие дни реже (5%), разнообразнее и не идут два дня подряд; «отключили свет» — лишь одна из многих причин. */
    private static final String[] BAD_DAYS = {"СЭС закрыла на полдня", "сломался кассовый аппарат", "отключили свет на пару часов",
            "ремонт дороги перед входом", "кассир перепутал сдачу", "пожарная проверка", "залило соседи сверху",
            "лёг интернет — терминал не принимал карты", "поставщик опоздал на полдня", "кассир заболел, работали без него",
            "прорвало трубу у входа", "налоговая проверка документов", "ливень — улица пустая"};
    public static final double BAD_CHANCE = 0.05;
    private static final String[] GOOD_DAYS = {"рядом открылась остановка", "блогер снял обзор", "праздники — очереди",
            "конкурент закрылся на ремонт", "хорошая погода", "день зарплаты в городе"};
    private static final String[] JACKPOT_DAYS = {"заведение попало в топ на картах", "свадьба сняла всё целиком",
            "ажиотаж: дефицит у конкурентов", "приехал городской фестиваль"};

    /** luckBonus — прибавка к множителю от эффекта «Удача» (минус — от «Невезения»). */
    public static Roll roll(Random r, double luckBonus) {
        return roll(r, luckBonus, false);
    }

    /** lastBad — вчера был плохой день: сегодня плохого не будет (неудачи подряд не идут). */
    public static Roll roll(Random r, double luckBonus, boolean lastBad) {
        double x = r.nextDouble();
        if (lastBad && x < BAD_CHANCE) x = BAD_CHANCE + r.nextDouble() * (0.80 - BAD_CHANCE);
        double m;
        String note;
        int kind;
        if (x < BAD_CHANCE) {
            m = 0.2 + r.nextDouble() * 0.4;
            note = BAD_DAYS[r.nextInt(BAD_DAYS.length)];
            kind = BAD;
        } else if (x < 0.80) {
            m = 0.7 + r.nextDouble() * 0.6;
            note = "обычный день";
            kind = NORMAL;
        } else if (x < 0.95) {
            m = 1.3 + r.nextDouble() * 0.5;
            note = GOOD_DAYS[r.nextInt(GOOD_DAYS.length)];
            kind = GOOD;
        } else {
            m = 2.0 + r.nextDouble();
            note = JACKPOT_DAYS[r.nextInt(JACKPOT_DAYS.length)];
            kind = JACKPOT;
        }
        return new Roll(Math.max(0, m + luckBonus), note, kind);
    }

    public static boolean isBadNote(String note) {
        if (note == null) return false;
        for (String s : BAD_DAYS) if (note.startsWith(s)) return true;
        return false;
    }

    public static double luckBonus(ServerPlayer p) {
        if (p == null) return 0;
        double bonus = 0;
        MobEffectInstance luck = p.getEffect(MobEffects.LUCK);
        if (luck != null) bonus += 0.3 * (luck.getAmplifier() + 1);
        MobEffectInstance bad = p.getEffect(MobEffects.UNLUCK);
        if (bad != null) bonus -= 0.3 * (bad.getAmplifier() + 1);
        return bonus;
    }

    // ------------------------------------------------------------------ отмывание

    /** Безопасный дневной объём отмыва: доля ВЧЕРАШНЕЙ прибыли заведения (убыток/ноль — отмывать нельзя). */
    public static double safeLimit(LegalBusiness b) {
        return Math.max(0, b.lastProfit) * safeShare(b);
    }

    /** v1.10: безопасная доля отмыва — в торговом центре 25%, иначе из конфига (15%). */
    public static double safeShare(LegalBusiness b) {
        return b != null && b.inMall() ? Mall.SAFE_SHARE : Config.LAUNDER_SAFE_SHARE.get();
    }

    /** Шанс, что налоговая заметит отмыв amount, если сегодня уже провели before; base — вчерашняя прибыль. */
    public static double launderChance(double base, double before, double amount) {
        return launderChance(base, before, amount, Config.LAUNDER_SAFE_SHARE.get());
    }

    public static double launderChance(double base, double before, double amount, double share) {
        if (base <= 0) return 0.95;
        double after = before + amount;
        if (after <= base * share + 1e-6) return 0;
        return Math.max(0, Math.min(0.95, (after / base - share) * Config.LAUNDER_RISK_SLOPE.get()));
    }

    // ------------------------------------------------------------------ лицензия и касса

    public static void buyPermit(ServerPlayer p, Account a, int typeIdx) {
        BizType t = BizType.byId(typeIdx);
        if (t == null) return;
        if (a.captivity != Account.FREE) { BankService.msg(p, "§cВы сейчас не можете пользоваться услугами."); return; }
        if (!BankService.charge(p, a, t.price)) {
            BankService.msg(p, BIZ + "§cЛицензия на «" + t.title + "» стоит " + Finance.money(t.price) + ". Счёт + наличка — не хватает.");
            return;
        }
        BankService.give(p, BizPermitItem.create(ModItems.BIZ_PERMIT.get(), t));
        BankService.msg(p, BIZ + "§aЛицензия на «" + t.title + "» оформлена! §fПКМ ею по блоку на СВОЕЙ земле — там встанет главная касса. "
                + "Вокруг неё (±" + LegalBusiness.R_XZ + " блоков) расставь оборудование из телефона (Бизнес → Оборудование) и найми работников в кассе.");
    }

    /** Покупка оборудования через телефон. */
    public static void buyEquip(ServerPlayer p, Account a, int idx, int count) {
        BizEquip e = BizEquip.byId(idx);
        if (e == null) return;
        int n = Math.max(1, Math.min(8, count));
        if (a.captivity != Account.FREE) return;
        int cost = e.price * n;
        if (!BankService.charge(p, a, cost)) {
            BankService.msg(p, BIZ + "§c«" + e.title + "» ×" + n + " стоит " + Finance.money(cost) + ". Не хватает.");
            return;
        }
        BankService.give(p, new ItemStack(e.block.get(), n));
        BankService.msg(p, BIZ + "Доставлено: §e" + e.title + " ×" + n + "§f за §c" + Finance.money(cost) + "§f. Поставь в зоне своего заведения.");
    }

    /** Чем мешает место для главной кассы (null — можно). */
    
    public static String checkSite(Level world, BlockPos pos, EconomyData data, UUID owner) {
        if (world.dimension() != Level.OVERWORLD) return "заведения открывают только в обычном мире";
        if (pos.getY() < world.getMinY() + 2 || pos.getY() > world.getMaxY() - 5) return "слишком высоко или низко";
        if (data.isProtected(pos)) return "это городская постройка";
        for (LegalBusiness b : data.businesses) {
            if (LegalBusiness.zonesOverlap(b.counter, pos)) {
                return "слишком близко к заведению «" + b.type.title + "»" + (owner.equals(b.owner) ? " (вашему)" : "")
                        + " — зоны (±" + LegalBusiness.R_XZ + " блоков) не должны пересекаться";
            }
        }
        for (Account o : data.all()) {
            if (o.id.equals(owner) || !o.hasHouse) continue;
            if (pos.getX() >= o.houseMin.getX() - 1 && pos.getX() <= o.houseMax.getX() + 1 && pos.getZ() >= o.houseMin.getZ() - 1
                    && pos.getZ() <= o.houseMax.getZ() + 1 && pos.getY() >= o.houseMin.getY() - 8 && pos.getY() <= o.houseMax.getY() + 2) {
                return "это дом другого игрока";
            }
        }
        for (Mall m : data.malls) {
            if (m.contains(pos, LegalBusiness.R_XZ + 1)) {
                return "рядом торговый центр — внутри ТЦ заведение открывают в своём помещении (купите его: ПКМ по табличке над входом)";
            }
        }
        Account a = data.getOrCreate(owner);
        if (Business.inOwnProperty(data, a, pos)) return null;
        UUID landOwner = data.owner(EconomyData.chunkKey(pos));
        if (landOwner == null) return null; // ничья земля — оформим на вас
        return landOwner.equals(com.baraaoi.reallife.structure.Buildings.CITY) ? "это городская земля" : "участок принадлежит другому игроку";
    }

    /** Поставить главную кассу по лицензии. true — заведение открыто. */
    public static boolean place(ServerPlayer p, BlockPos pos, Direction facing, BizType type) {
        Level world = p.level();
        MinecraftServer server = p.level().getServer();
        if (server == null) return false;
        EconomyData data = EconomyData.get(server);
        Mall mall = data.mallAt(pos, 0);
        if (mall != null) {
            Mall.Unit u = mall.unitAt(pos);
            if (u == null) {
                BankService.msg(p, BIZ + "§cВ ТЦ заведение открывают внутри своего помещения, а не в общем зале. "
                        + "§fПКМ по табличке над входом помещения — купить/занять.");
                return false;
            }
            if (!p.getUUID().equals(u.tenant)) {
                BankService.msg(p, BIZ + "§cПомещение №" + (u.idx + 1) + " не ваше"
                        + (u.rival != 0 ? " — его занял конкурент" : u.tenant == null ? " — оно продаётся (ПКМ по табличке над входом)" : " (арендатор " + u.tenantName + ")") + ".");
                return false;
            }
            if (mall.business(data, u) != null) {
                BankService.msg(p, BIZ + "§cВ этом помещении уже есть заведение. Одно помещение — одно заведение.");
                return false;
            }
            if (!world.getBlockState(pos).canBeReplaced()) {
                BankService.msg(p, BIZ + "§cМесто под кассу занято.");
                return false;
            }
            LegalBusiness b = Malls.openInUnit(data, world, mall, u, pos, facing, type, p.getUUID(), p.getGameProfile().name());
            BankService.msg(p, BIZ + "§a§lЗаведение «" + type.title + "» открыто в ТЦ (помещение №" + (u.idx + 1) + ")! §r§fПрибыль §a+50%§f, безопасный отмыв §a"
                    + Math.round(Mall.SAFE_SHARE * 100) + "%§f вчерашней прибыли. Зона заведения — всё помещение " + u.width + "×" + u.depth
                    + ". Комплект на работника: §e" + kitText(type) + "§f + склад §e" + baseText(type) + "§f.");
            if (!b.owner.equals(mall.owner)) BankService.msg(p, BIZ + "§7Не забывайте: аренда " + Finance.money(u.rent) + "/день владельцу ТЦ списывается со счёта.");
            return true;
        }
        String problem = checkSite(world, pos, data, p.getUUID());
        if (problem != null) {
            BankService.msg(p, BIZ + "§cЗдесь открыть нельзя: §f" + problem);
            return false;
        }
        BlockState cur = world.getBlockState(pos);
        if (!cur.canBeReplaced()) {
            BankService.msg(p, BIZ + "§cМесто под кассу занято.");
            return false;
        }
        LegalBusiness b = register(data, world, pos, facing, type, p.getUUID(), p.getGameProfile().name());
        int claimed = com.baraaoi.reallife.structure.Buildings.claimArea(data, p.getUUID(), pos, pos, true);
        data.setDirty();
        BankService.msg(p, BIZ + "§aОткрыто заведение «" + type.title + "»! §fЭто главная касса. Зона заведения — §e±" + LegalBusiness.R_XZ
                + "§f блоков вокруг неё. Комплект на 1 работника: §e" + kitText(type) + "§f (≈" + Finance.money(type.kitPrice()) + ")"
                + ", плюс на всё заведение — §e" + baseText(type) + "§f (склад). Больше стеллажей — больше запас сырья.");
        BankService.msg(p, BIZ + "Купи оборудование в телефоне (§eБизнес → Оборудование§f), сырьё — в §eОпт§f; потом ПКМ по кассе → «Нанять».");
        if (claimed > 0) BankService.msg(p, "§3[Администрация] §fУчасток под кассой оформлен на вас.");
        return true;
    }

    /** Зарегистрировать заведение с главной кассой в pos (касса ставится, если её там нет). Используется и самотестом. */
    public static LegalBusiness register(EconomyData data, Level world, BlockPos pos, Direction facing, BizType type, UUID owner, String ownerName) {
        if (world.getBlockState(pos).getBlock() != ModBlocks.BIZ_COUNTER.get()) {
            world.setBlock(pos, ModBlocks.BIZ_COUNTER.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing), 3);
        }
        LegalBusiness b = new LegalBusiness();
        b.id = data.nextBizId++;
        b.owner = owner;
        b.ownerName = ownerName == null ? "" : ownerName;
        b.type = type;
        b.setCounter(pos);
        data.businesses.add(b);
        data.setDirty();
        return b;
    }

    /** Закрыть заведение (главную кассу сломал владелец): уволить всех, вернуть лицензию. */
    public static void close(MinecraftServer server, EconomyData data, LegalBusiness b, ServerPlayer by) {
        ServerLevel ow = EconomyData.overworld(server);
        for (UUID u : b.workers) {
            Entity e = ow.getEntity(u);
            if (e != null) e.discard();
        }
        b.workers.clear();
        data.businesses.remove(b);
        data.setDirty();
        Mall mall = b.inMall() ? data.mallById(b.mallId) : null;
        Mall.Unit mu = mall == null ? null : mall.unitAt(b.counter);
        if (mu != null) Malls.updateSign(ow, data, mall, mu);
        if (by != null) {
            BankService.give(by, BizPermitItem.create(ModItems.BIZ_PERMIT.get(), b.type));
            BankService.msg(by, BIZ + "§eЗаведение «" + b.type.title + "» закрыто, работники распущены. §fЛицензия возвращена — "
                    + "можно открыть заново в другом месте.");
        }
    }

    /** Сломали кассу. true — отменить (чужая главная касса или касса в чужой зоне). */
    public static boolean onCounterBreak(ServerPlayer p, BlockPos pos) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return false;
        EconomyData data = EconomyData.get(server);
        LegalBusiness main = data.bizAtCounter(pos);
        boolean owner = main != null ? p.getUUID().equals(main.owner) : true;
        if (main == null) {
            LegalBusiness z = bizContaining(data, pos);
            if (z != null && !p.getUUID().equals(z.owner) && !p.isCreative()) {
                BankService.actionBar(p, "§cЭто касса чужого заведения.");
                return true;
            }
            return false;
        }
        if (!owner && !p.isCreative()) {
            BankService.actionBar(p, "§cЭто главная касса чужого заведения — ломать нельзя.");
            return true;
        }
        close(server, data, main, owner ? p : null);
        return false;
    }

    
    public static LegalBusiness bizContaining(EconomyData data, BlockPos pos) {
        for (LegalBusiness b : data.businesses) if (b.contains(pos, 0)) return b;
        return null;
    }

    public static String kitText(BizType t) {
        StringBuilder sb = new StringBuilder();
        for (BizEquip e : BizEquip.values()) {
            int n = t.need(e);
            if (n <= 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.title).append(n > 1 ? " ×" + n : "");
        }
        return sb.toString();
    }

    /** Оборудование, нужное один раз на всё заведение (склад). */
    public static String baseText(BizType t) {
        StringBuilder sb = new StringBuilder();
        for (BizEquip e : BizEquip.values()) {
            int n = t.baseNeed(e);
            if (n <= 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.title).append(n > 1 ? " ×" + n : "");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ осмотр зоны

    /** Итог осмотра зоны заведения. */
    public static final class Scan {
        public final int[] count = new int[BizEquip.values().length];
        public final List<Container> stores = new ArrayList<>();
        /** Витринные хранилища (полки, холодильники, витрины, сейфы) — заполняются первыми, расходуются последними. */
        public final List<Container> visible = new ArrayList<>();
        /** Склад: стеллажи, сундуки, бочки — запас; работники берут отсюда в первую очередь. */
        public final List<Container> back = new ArrayList<>();
        /** Есть ли склад (складской стеллаж). Без него заведение не работает. */
        public boolean warehouse;
        public final List<BlockPos> spots = new ArrayList<>();
        public int stock;
        public int sets;
        public int allowed;
        public boolean counterOk;
    }

    static boolean isStore(Block block, BlockEntity te) {
        if (te instanceof ShelfTileEntity || te instanceof ChestBlockEntity || te instanceof BarrelBlockEntity) return true;
        return te instanceof FridgeTileEntity && block != ModBlocks.BUSINESS_CHEST.get();
    }

    public static Scan scan(Level w, LegalBusiness b) {
        Scan s = new Scan();
        s.warehouse = true;
        s.counterOk = w.getBlockState(b.counter).getBlock() == ModBlocks.BIZ_COUNTER.get();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = b.min.getX(); x <= b.max.getX(); x++)
            for (int z = b.min.getZ(); z <= b.max.getZ(); z++)
                for (int y = Math.max(w.getMinY(), b.min.getY()); y <= Math.min(w.getMaxY(), b.max.getY()); y++) {
                    m.set(x, y, z);
                    BlockState st = w.getBlockState(m);
                    if (st.isAir()) continue;
                    Block block = st.getBlock();
                    BizEquip e = BizEquip.of(block);
                    if (e != null) {
                        s.count[e.ordinal()]++;
                        if (e != BizEquip.SHELF && e != BizEquip.RACK) s.spots.add(m.immutable());
                    }
                    if (st.hasBlockEntity()) {
                        BlockEntity te = w.getBlockEntity(m);
                        if (te instanceof Container && isStore(block, te)) {
                            Container inv = (Container) te;
                            s.stores.add(inv);
                            boolean shown = (te instanceof ShelfTileEntity && block != ModBlocks.STORAGE_RACK.get()) || te instanceof FridgeTileEntity;
                            (shown ? s.visible : s.back).add(inv);
                            for (int i = 0; i < inv.getContainerSize(); i++) {
                                ItemStack it = inv.getItem(i);
                                if (SupplyItem.typeOf(it) == b.type) s.stock += it.getCount();
                            }
                        }
                    }
                }
        int sets = Integer.MAX_VALUE;
        for (BizEquip e : BizEquip.values()) {
            int need = b.type.need(e);
            if (need > 0) sets = Math.min(sets, s.count[e.ordinal()] / need);
        }
        for (BizEquip e : BizEquip.values()) if (s.count[e.ordinal()] < b.type.baseNeed(e)) s.warehouse = false;
        if (!s.counterOk || !s.warehouse) sets = 0;
        s.sets = sets == Integer.MAX_VALUE ? 0 : sets;
        s.allowed = Math.min(b.type.maxWorkers, s.sets);
        b.cacheSets = s.sets;
        b.cacheStock = s.stock;
        return s;
    }

    /** Списать до amount единиц сырья из хранилищ зоны. Возвращает, сколько списано. */
    static int consume(Scan s, BizType type, int amount) {
        int left = amount;
        List<Container> order = new ArrayList<>(s.back); // сначала со склада — полки остаются полными, пока есть запас
        order.addAll(s.visible);
        for (Container inv : order) {
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack it = inv.getItem(i);
                if (SupplyItem.typeOf(it) != type) continue;
                int n = Math.min(left, it.getCount());
                it.shrink(n);
                if (it.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
                left -= n;
            }
            inv.setChanged();
            if (left <= 0) break;
        }
        s.stock -= amount - left;
        return amount - left;
    }

    /** Разложить поставку по полкам/холодильникам/сундукам зоны. Возвращает то, что не влезло. */
    public static ItemStack deliver(Level w, LegalBusiness b, ItemStack stack) {
        Scan s = scan(w, b);
        ItemStack rest = stack.copy();
        // сначала — полки и холодильники (их видно), потом склад: стеллажи и сундуки
        List<Container> order = new ArrayList<>(s.visible);
        order.addAll(s.back);
        // раскладываем порциями по 8, по кругу — чтобы заполнялись все полки, а не одна
        boolean progress = true;
        while (!rest.isEmpty() && progress) {
            progress = false;
            for (Container inv : order) {
                if (rest.isEmpty()) break;
                ItemStack piece = rest.split(Math.min(8, rest.getCount()));
                int before = piece.getCount();
                PrinterTileEntity.insert(inv, piece);
                if (piece.getCount() < before) { progress = true; inv.setChanged(); }
                if (!piece.isEmpty()) rest.grow(piece.getCount());
            }
        }
        return rest;
    }

    // ------------------------------------------------------------------ работники

    
    public static BlockPos workerSpot(ServerLevel w, LegalBusiness b, Scan s) {
        List<BlockPos> anchors = new ArrayList<>(s.spots);
        anchors.add(0, b.counter);
        for (int attempt = 0; attempt < 60; attempt++) {
            BlockPos a = attempt < 20 ? anchors.get(Math.min(anchors.size() - 1, attempt % anchors.size())) : anchors.get(R.nextInt(anchors.size()));
            Direction d = Direction.from2DDataValue(R.nextInt(4));
            BlockPos p = a.relative(d);
            if (attempt >= 30) p = p.offset(R.nextInt(3) - 1, R.nextInt(3) - 1, R.nextInt(3) - 1);
            if (!b.contains(p, 0)) continue;
            if (!w.isEmptyBlock(p) || !w.isEmptyBlock(p.above()) || !w.getBlockState(p.below()).isSolid()) continue;
            if (!w.getEntitiesOfClass(WorkerEntity.class, new AABB(p)).isEmpty()) continue;
            return p;
        }
        return null;
    }

    public static WorkerEntity spawnWorker(ServerLevel w, LegalBusiness b, BlockPos at) {
        WorkerEntity e = ModEntities.WORKER.get().create(w, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) return null;
        float yaw = R.nextFloat() * 360F;
        e.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0F);
        e.yHeadRot = yaw;
        e.yBodyRot = yaw;
        e.setBizType(b.type);
        e.bizId = b.id;
        String name = b.workers.isEmpty() ? b.type.workerName : b.type.workerName + " " + (b.workers.size() + 1) + "-й";
        e.setCustomName(Component.literal("§a" + name));
        e.setCustomNameVisible(true);
        w.addFreshEntity(e);
        b.workers.add(e.getUUID());
        return e;
    }

    /** Убрать из списка работников, которых больше нет (только если зона загружена). */
    static void pruneWorkers(ServerLevel w, LegalBusiness b) {
        if (!w.isLoaded(b.counter)) return;
        b.workers.removeIf(u -> w.getEntity(u) == null);
    }

    static void hire(ServerPlayer p, Account a, LegalBusiness b) {
        ServerLevel w = p.level();
        pruneWorkers(w, b);
        Scan s = scan(w, b);
        if (!s.counterOk) { BankService.msg(p, BIZ + "§cГлавная касса пропала."); return; }
        if (b.workers.size() >= s.allowed) {
            if (b.workers.size() >= b.type.maxWorkers) {
                BankService.msg(p, BIZ + "§eБольше " + b.type.maxWorkers + " работников в «" + b.type.title + "» не нужно.");
            } else {
                if (!s.warehouse) BankService.msg(p, BIZ + "§cНет склада: поставь в зоне §e" + baseText(b.type) + "§c — один на всё заведение.");
                else BankService.msg(p, BIZ + "§cНе хватает оборудования на ещё одного работника. Нужен ещё комплект: §e" + kitText(b.type)
                        + "§c (в зоне ±" + LegalBusiness.R_XZ + " блоков от главной кассы).");
            }
            return;
        }
        BlockPos at = workerSpot(w, b, s);
        if (at == null) { BankService.msg(p, BIZ + "§cРаботнику негде встать — освободи место (2 блока воздуха) рядом с оборудованием."); return; }
        int cost = b.type.hireCost();
        if (!BankService.charge(p, a, cost)) {
            BankService.msg(p, BIZ + "§cНайм стоит " + Finance.money(cost) + " (подъёмные). Не хватает.");
            return;
        }
        if (spawnWorker(w, b, at) == null) { a.balance += cost; return; }
        EconomyData.get(p.level().getServer()).setDirty();
        BankService.msg(p, BIZ + "§aНанят работник (" + b.workers.size() + "/" + s.allowed + "). §fЗарплата " + Finance.money(b.type.wage())
                + "/день, расход сырья " + BizType.SUPPLY_PER_WORKER + " коробка/день.");
        BankService.msg(p, BIZ + "§eНе забудьте оформить ему документы (кнопка «Документы» в кассе, "
                + Finance.money(com.baraaoi.reallife.event.Inspections.DOCS_PRICE) + "/чел. на 7 дней) — иначе СЭС и миграционная служба оштрафуют.");
    }

    static void fire(ServerPlayer p, LegalBusiness b) {
        ServerLevel w = p.level();
        pruneWorkers(w, b);
        if (b.workers.isEmpty()) { BankService.msg(p, BIZ + "Увольнять некого."); return; }
        UUID u = b.workers.remove(b.workers.size() - 1);
        Entity e = w.getEntity(u);
        if (e != null) e.discard();
        EconomyData.get(p.level().getServer()).setDirty();
        BankService.msg(p, BIZ + "§eРаботник уволен. §fОсталось: " + b.workers.size() + ".");
    }

    public static void onWorkerDeath(WorkerEntity e) {
        MinecraftServer server = e.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        LegalBusiness b = data.bizById(e.bizId);
        if (b == null || !b.workers.remove(e.getUUID())) return;
        data.setDirty();
        ServerPlayer owner = b.owner == null ? null : server.getPlayerList().getPlayer(b.owner);
        if (owner != null) BankService.msg(owner, BIZ + "§cСМС: «Работник из «" + b.type.title + "» погиб. Осталось работников: "
                + b.workers.size() + ". Нанять нового — ПКМ по кассе.»");
    }

    public static void workerTalk(ServerPlayer p, WorkerEntity e) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        LegalBusiness b = EconomyData.get(server).bizById(e.bizId);
        String[] lines = {"Здравствуйте! Чем могу помочь?", "Касса справа, карты принимаем.", "Акция: второй товар — за полную цену!",
                "Директор сказал улыбаться. Я улыбаюсь.", "Скоро обед, приходите после."};
        String text = lines[R.nextInt(lines.length)];
        if (b != null && p.getUUID().equals(b.owner)) {
            int need = BizType.SUPPLY_PER_WORKER * b.workers.size();
            text = "Шеф! Вчера вышло " + Finance.money(b.lastProfit) + " (" + (b.lastNote.isEmpty() ? "ещё не считали" : b.lastNote) + ")."
                    + (b.cacheStock < need ? " И сырьё кончается: " + b.cacheStock + " из " + need + " на день!" : "");
        }
        p.sendSystemMessage(Component.literal("§7<§a" + e.getBizType().workerName + "§7> §f" + text));
    }

    // ------------------------------------------------------------------ касса

    public static void openCounter(ServerPlayer p, BlockPos pos) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        LegalBusiness b = data.bizAtCounter(pos);
        if (b == null) b = bizContaining(data, pos);
        if (b == null) {
            BankService.msg(p, BIZ + "§7Касса не относится ни к одному заведению. Лицензию покупают в телефоне (Бизнес), "
                    + "лишние кассы ставят в зоне своего заведения.");
            return;
        }
        if (!p.getUUID().equals(b.owner)) {
            BankService.msg(p, BIZ + "«" + b.type.title + "». Владелец: §e" + (b.ownerName.isEmpty() ? "?" : b.ownerName)
                    + "§f. " + (!b.workers.isEmpty() ? "§aОткрыто." : "§cЗакрыто."));
            return;
        }
        final BlockPos at = pos.immutable();
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BizContainer(id, inv, at),
                Component.literal(b.type.title)), at);
        BankService.sync(p);
    }

    /** Заведение, к которому относится открытая касса. */
    
    public static LegalBusiness bizForCounter(EconomyData data, BlockPos pos) {
        LegalBusiness b = data.bizAtCounter(pos);
        return b != null ? b : bizContaining(data, pos);
    }

    static String closedReason(LegalBusiness b, Scan s, int sets, int stock) {
        if (s != null && !s.counterOk) return "нет главной кассы";
        if (s != null && !s.warehouse) return "нет склада (" + baseText(b.type) + ")";
        if (sets <= 0) return "не хватает оборудования";
        if (b.workers.isEmpty()) return "нет работников";
        if (stock <= 0) return "нет сырья";
        return "";
    }

    /** Данные заведения для окон. live — осмотреть зону прямо сейчас (иначе — кэш). */
    public static CompoundTag info(Level w, LegalBusiness b, long today, boolean live) {
        CompoundTag t = new CompoundTag();
        Scan s = live && w != null && w.isLoaded(b.counter) ? scan(w, b) : null;
        int sets = s != null ? s.sets : b.cacheSets;
        int stock = s != null ? s.stock : b.cacheStock;
        t.putInt("id", b.id);
        t.putInt("type", b.type.ordinal());
        t.putInt("workers", b.workers.size());
        t.putInt("sets", sets);
        t.putInt("allowed", Math.min(b.type.maxWorkers, sets));
        t.putInt("maxWorkers", b.type.maxWorkers);
        t.putInt("stock", stock);
        t.putInt("need", BizType.SUPPLY_PER_WORKER * b.workers.size());
        String reason = closedReason(b, s, sets, stock);
        if (com.baraaoi.reallife.event.Inspections.sealed(b, today)) reason = "опечатано до дня " + b.sealedUntil + " (" + b.sealReason + ")";
        t.putString("reason", reason);
        int undoc = com.baraaoi.reallife.event.Inspections.undocumented(b, today);
        t.putInt("undoc", undoc);
        t.putLong("docsUntil", b.docsUntil);
        t.putDouble("lastProfit", b.lastProfit);
        t.putString("lastNote", b.lastNote);
        t.putDouble("total", b.totalProfit);
        t.putInt("days", b.days);
        t.putDouble("laundered", b.launderedOn(today));
        t.putDouble("safe", safeLimit(b));
        t.putDouble("safeShare", safeShare(b));
        t.putBoolean("mall", b.inMall());
        EconomyData rd = w.getServer() == null ? null : EconomyData.get(w.getServer());
        t.putInt("rivals", Rivals.count(rd, b));
        t.putDouble("rivalFactor", Rivals.factor(rd, b));
        t.putInt("x", b.counter.getX());
        t.putInt("y", b.counter.getY());
        t.putInt("z", b.counter.getZ());
        if (s != null) {
            ListTag eq = new ListTag();
            for (BizEquip e : BizEquip.values()) {
                int need = b.type.need(e), base = b.type.baseNeed(e);
                if (need <= 0 && base <= 0) continue;
                CompoundTag q = new CompoundTag();
                q.putString("n", e.title);
                q.putInt("h", s.count[e.ordinal()]);
                q.putInt("q", need);
                q.putInt("b", base);
                eq.add(q);
            }
            t.put("equip", eq);
        }
        return t;
    }

    public static ListTag infoList(EconomyData data, UUID owner, long today) {
        ListTag l = new ListTag();
        for (LegalBusiness b : data.bizOf(owner)) l.add(info(null, b, today, false));
        return l;
    }

    static void counterAction(ServerPlayer p, Account a, BizContainer c, int action) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        LegalBusiness b = bizForCounter(data, c.getPos());
        if (b == null || !p.getUUID().equals(b.owner)) return;
        if (action == Actions.BIZ_HIRE) hire(p, a, b);
        else if (action == Actions.BIZ_FIRE) fire(p, b);
        else if (action == Actions.BIZ_LAUNDER) launder(p, a, b, c);
        else if (action == Actions.BIZ_DOCS) docs(p, a, b);
    }

    /** v1.9: оформить документы (медкнижки + патенты) всем текущим работникам на 7 дней. */
    static void docs(ServerPlayer p, Account a, LegalBusiness b) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        pruneWorkers(p.level(), b);
        int n = b.workers.size();
        if (n == 0) { BankService.msg(p, BIZ + "Сначала наймите работников — оформлять пока некого."); return; }
        long today = BankService.today(server);
        int cost = com.baraaoi.reallife.event.Inspections.DOCS_PRICE * n;
        if (!BankService.charge(p, a, cost)) { BankService.msg(p, BIZ + "§cНужно " + Finance.money(cost) + " (по "
                + Finance.money(com.baraaoi.reallife.event.Inspections.DOCS_PRICE) + " на работника)."); return; }
        b.docsUntil = today + com.baraaoi.reallife.event.Inspections.DOCS_DAYS;
        b.docsWorkers = n;
        EconomyData.get(server).setDirty();
        BankService.msg(p, BIZ + "§aМедкнижки и патенты оформлены на " + n + " чел. до дня " + b.docsUntil + "§f (−" + Finance.money(cost)
                + "). СЭС и миграционная служба претензий не имеют. Наняли нового — оформите и его.");
    }

    static void launder(ServerPlayer p, Account a, LegalBusiness b, BizContainer c) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        if (a.captivity != Account.FREE) return;
        ItemStack st = c.getInput().getItem(0);
        if (st.isEmpty() || !(st.getItem() instanceof DirtyMoneyItem)) { BankService.msg(p, BIZ + "Положи грязные деньги в слот кассы."); return; }
        if (b.workers.isEmpty()) { BankService.msg(p, BIZ + "§cЗаведение закрыто — без работников выручку не проведёшь."); return; }
        if (b.lastProfit <= 0) {
            BankService.msg(p, BIZ + "§cВчера заведение " + (b.days == 0 ? "ещё не работало" : b.lastProfit < 0 ? "ушло в минус" : "ничего не заработало")
                    + " — провести «выручку» не через что. Налоговая сразу заметит. Отмыв доступен в пределах вчерашней прибыли.");
            return;
        }
        EconomyData data = EconomyData.get(server);
        long today = BankService.today(server);
        double amount = ItemValues.stackValue(st);
        double before = b.launderedOn(today);
        double chance = launderChance(b.lastProfit, before, amount, safeShare(b));
        b.launderDay = today;
        b.launderedToday = before + amount;
        c.getInput().setItem(0, ItemStack.EMPTY);
        data.setDirty();
        if (R.nextDouble() < chance) {
            caught(server, data, p, a, b, amount);
            return;
        }
        double tax = Config.LAUNDER_TAX.get();
        double clean = amount * (1 - tax);
        a.balance += clean;
        BankService.msg(p, BIZ + "Выручка проведена через кассу «" + b.type.title + "»: §2" + Finance.money(amount) + "§f → §a+"
                + Finance.money(clean) + "§f на счёт (НДФЛ " + Finance.percent(tax) + "). Сегодня проведено: "
                + Finance.money(b.launderedToday) + " из безопасных " + Finance.money(safeLimit(b)) + " (" + Math.round(safeShare(b) * 100) + "% вчерашней прибыли" + (b.inMall() ? ", льгота ТЦ" : "") + ").");
        if (chance > 0) BankService.msg(p, BIZ + "§eПронесло. Риск был " + Finance.percent(chance) + " — налоговая могла заметить.");
    }

    static void caught(MinecraftServer server, EconomyData data, ServerPlayer p, Account a, LegalBusiness b, double amount) {
        double fine = Config.TAX_FINE_MULT.get() * (Math.max(0, b.lastProfit) + amount);
        p.closeContainer();
        BankService.msg(p, TAX + "§cНалоговая проверка в «" + b.type.title + "»! §fВыручка не сходится с чеками: "
                + Finance.money(amount) + " «из воздуха».");
        BankService.msg(p, TAX + "Грязные деньги изъяты. Штраф 200% от (прибыли заведения + суммы): §c" + Finance.money(fine) + "§f.");
        Visitors.forceCharge(p, a, fine);
        data.setDirty();
        int ticks = Config.TAX_JAIL_TICKS.get();
        if (ticks > 0) {
            DebtManager.jailFor(server, data, p, a, ticks, "отмывание денег через «" + b.type.title + "»");
        }
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other != p) BankService.msg(other, "§9[Новости] §fНалоговая накрыла «" + b.type.title + "» игрока §e"
                    + p.getGameProfile().name() + "§f: отмывание денег.");
        }
    }

    // ------------------------------------------------------------------ покупатели (v1.7, косметика)

    /** Открыто ли заведение по кэшу последнего осмотра. */
    public static boolean isOpen(LegalBusiness b) {
        return !b.workers.isEmpty() && b.cacheSets > 0 && b.cacheStock > 0;
    }

    /** Раз в 2 секунды: к открытым заведениям рядом с игроками заходят покупатели. */
    public static void tickCustomers(MinecraftServer server) {
        ServerLevel ow = EconomyData.overworld(server);
        if (ow == null || ow.getGameTime() % 40 != 7) return;
        EconomyData data = EconomyData.get(server);
        boolean night = ow.getOverworldClockTime() % 24000L >= 13000L && ow.getOverworldClockTime() % 24000L <= 23000L;
        for (LegalBusiness b : data.businesses) {
            if (b.owner == null || !isOpen(b) || !ow.isLoaded(b.counter)
                    || com.baraaoi.reallife.event.Inspections.sealed(b, BankService.today(server))) continue;
            BlockPos c = b.counter;
            if (ow.getNearestPlayer(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 64.0D, false) == null) continue;
            int working = Math.min(b.workers.size(), Math.max(1, b.cacheSets));
            double chance = 0.07 * working * (night ? 0.3 : 1.0);
            if (R.nextDouble() >= chance) continue;
            int cap = Math.min(6, 1 + working);
            int have = ow.getEntitiesOfClass(com.baraaoi.reallife.entity.CustomerEntity.class, new AABB(c).inflate(32.0D),
                    e -> e.bizId == b.id).size();
            if (have >= cap) continue;
            spawnCustomer(ow, b);
        }
    }

    /** Покупатель приходит со стороны (10–18 блоков от кассы), обходит пару единиц оборудования и идёт к кассе. */
    
    public static com.baraaoi.reallife.entity.CustomerEntity spawnCustomer(ServerLevel w, LegalBusiness b) {
        com.baraaoi.reallife.entity.CustomerEntity e = ModEntities.CUSTOMER.get().create(w, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) return null;
        BlockPos at = customerSpawn(w, b);
        if (at == null) return null;
        Scan s = scan(w, b);
        List<BlockPos> equip = new ArrayList<>(s.spots);
        equip.remove(b.counter);
        java.util.Collections.shuffle(equip, R);
        e.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, R.nextFloat() * 360F, 0F);
        e.setVariant(R.nextInt(com.baraaoi.reallife.entity.CustomerEntity.VARIANTS));
        e.bizId = b.id;
        e.counter = b.counter;
        e.life = 2400 + R.nextInt(1200);
        for (int i = 0; i < equip.size() && i < 1 + R.nextInt(2); i++) e.targets.add(equip.get(i));
        e.targets.add(b.counter);
        w.addFreshEntity(e);
        return e;
    }

    /** Точка появления покупателя: снаружи зоны, на уровне кассы ±6, где можно стоять. */
    
    static BlockPos customerSpawn(ServerLevel w, LegalBusiness b) {
        BlockPos c = b.counter;
        for (int i = 0; i < 30; i++) {
            double ang = R.nextDouble() * Math.PI * 2;
            int r = 10 + R.nextInt(9);
            int x = c.getX() + (int) Math.round(Math.cos(ang) * r), z = c.getZ() + (int) Math.round(Math.sin(ang) * r);
            if (!w.isLoaded(new BlockPos(x, c.getY(), z))) continue;
            for (int dy = 0; dy <= 6; dy++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    BlockPos p = new BlockPos(x, c.getY() + dy * sign, z);
                    if (w.isEmptyBlock(p) && w.isEmptyBlock(p.above()) && w.getBlockState(p.below()).isSolid()
                            && !w.getBlockState(p.below()).liquid()) return p;
                }
            }
        }
        // запасной вариант — прямо в зоне, рядом с кассой
        for (int i = 0; i < 30; i++) {
            BlockPos p = c.offset(R.nextInt(9) - 4, R.nextInt(3) - 1, R.nextInt(9) - 4);
            if (w.isEmptyBlock(p) && w.isEmptyBlock(p.above()) && w.getBlockState(p.below()).isSolid()) return p;
        }
        return null;
    }

    // ------------------------------------------------------------------ день

    /** Выручка дня до вычета зарплат. */
    public static double revenue(BizType t, int working, double mult, double supplyFraction) {
        if (working <= 0) return 0;
        return Math.round(t.base * BizType.REVENUE_BOOST * (1 + 0.9 * (working - 1)) * mult * supplyFraction);
    }

    /** v1.10: выручка конкретного заведения — в торговом центре +50%; v1.11: конкуренты рядом отбивают покупателей. */
    public static double dayRevenue(EconomyData data, LegalBusiness b, int working, double mult, double supplyFraction) {
        double r = revenue(b.type, working, mult, supplyFraction);
        double k = (b.inMall() ? Mall.REVENUE_BONUS : 1.0) * Rivals.factor(data, b);
        return k == 1.0 ? r : Math.round(r * k);
    }

    public static void processDay(MinecraftServer server, EconomyData data) {
        ServerLevel ow = EconomyData.overworld(server);
        java.util.Set<UUID> rated = new java.util.HashSet<>();
        for (LegalBusiness b : new ArrayList<>(data.businesses)) {
            if (b.owner == null) continue;
            Account a = data.getOrCreate(b.owner);
            ServerPlayer p = server.getPlayerList().getPlayer(b.owner);
            pruneWorkers(ow, b);
            Scan s = scan(ow, b); // при необходимости подгрузит чанк
            int hired = b.workers.size();
            long day = BankService.today(server) - 1;
            boolean sealedDay = com.baraaoi.reallife.event.Inspections.sealed(b, day);
            int working = sealedDay ? 0 : Math.min(hired, s.allowed);
            int need = BizType.SUPPLY_PER_WORKER * working;
            int used = working > 0 ? consume(s, b.type, need) : 0;
            b.cacheStock = s.stock;
            double fraction = need > 0 ? used / (double) need : 0;
            double wages = b.type.wage() * hired;
            Roll r = roll(R, luckBonus(p), isBadNote(b.lastNote));
            double revenue = dayRevenue(data, b, working, r.mult, fraction);
            int rivals = Rivals.count(data, b);
            double profit = revenue - wages;
            String note;
            if (sealedDay) note = "опечатано: " + b.sealReason;
            else if (hired == 0) note = "закрыто: нет работников";
            else if (working == 0) note = "закрыто: " + closedReason(b, s, s.sets, s.stock + used);
            else if (used == 0) note = "простой: нет сырья";
            else note = r.note + (used < need ? ", сырья хватило на " + Math.round(fraction * 100) + "%" : "");
            a.balance += profit;
            int customers = b.customersToday;
            b.customersToday = 0;
            b.lastProfit = profit;
            b.lastNote = note;
            b.totalProfit += profit;
            if (working > 0) b.days++;
            boolean ratingUp = working > 0 && profit > 0 && rated.add(b.owner);
            if (ratingUp) a.bizDays++;
            if (p != null) {
                String color = working == 0 || used == 0 ? "§c" : r.kind == BAD ? "§c" : r.kind == NORMAL ? "§f" : r.kind == GOOD ? "§a" : "§6§l";
                BankService.msg(p, BIZ + "СМС «" + b.type.title + "»: " + color + note + "§f. Работает " + working + "/" + hired
                        + ", сырья ушло " + used + "/" + need + ". Выручка §a" + Finance.money(revenue) + (b.inMall() && revenue > 0 ? " §d(ТЦ +50%)§f" : "")
                        + (rivals > 0 && revenue > 0 ? " §c(конкуренты рядом: " + rivals + ", −" + Math.round((1 - Rivals.factor(data, b)) * 100) + "%)§f" : "") + "§f, зарплаты §c−"
                        + Finance.money(wages) + "§f → итог " + (profit >= 0 ? "§a+" : "§c") + Finance.money(profit) + "§f на счёт."
                        + (customers > 0 ? " §7Покупателей заходило при вас: " + customers + "." : "")
                        + (ratingUp ? " §aКредитный рейтинг растёт: банк видит рабочий бизнес." : ""));
                if (working > 0 && s.stock < need) BankService.msg(p, BIZ + "§eСырья на завтра мало (" + s.stock + " из " + need
                        + "). Телефон → Опт → «" + b.type.supplyName + "».");
            }
        }
        data.setDirty();
    }
}
