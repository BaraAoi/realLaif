package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.item.BanknoteItem;
import com.baraaoi.reallife.registry.ModItems;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Оптовые закупки «для магазинов» через телефон — только для владельцев легальных заведений.
 * Дешевле розницы (продавцы в банке, автоматы), но дороже, чем банк даёт за товар, — перепродавать невыгодно.
 */
public final class Wholesale {
    private Wholesale() {}

    public static final class Lot {
        public final Supplier<Item> item;
        public final int count;
        /** Цена партии. */
        public final int price;
        /** Цена той же партии в рознице (0 — в рознице не продаётся). */
        public final int retail;
        /** Сырьё для заведения этого вида (доставляется прямо в заведение), иначе null. */
        public final BizType supply;

        Lot(Supplier<Item> item, int count, int price, int retail) {
            this(item, count, price, retail, null);
        }

        Lot(Supplier<Item> item, int count, int price, int retail, BizType supply) {
            this.item = item;
            this.count = count;
            this.price = price;
            this.retail = retail;
            this.supply = supply;
        }

        public String name() {
            return new ItemStack(item.get()).getDisplayName().getString();
        }
    }

    private static List<Lot> lots;
    public static final int SUPPLY_LOT = 16;

    private static int roundUp10(double v) {
        return (int) (Math.ceil(v / 10.0) * 10);
    }

    /** Розничная цена за штуку по ассортименту продавцов и автоматов (0 — не продаётся). */
    public static Map<Item, Double> retailPrices() {
        Map<Item, Double> m = new HashMap<>();
        List<MerchantOffer> all = new ArrayList<>();
        for (int v = 0; v < 2; v++) all.addAll(Trades.seller(v));
        all.addAll(Trades.vending(Trades.VENDING_FOOD));
        all.addAll(Trades.vending(Trades.VENDING_RESOURCES));
        for (MerchantOffer o : all) {
            ItemStack buy = o.getBuyingStackFirst();
            ItemStack sell = o.getSellingStack();
            if (!(buy.getItem() instanceof BanknoteItem) || sell.isEmpty() || !o.getBuyingStackSecond().isEmpty()) continue;
            double unit = ItemValues.stackValue(buy) / sell.getCount();
            Double old = m.get(sell.getItem());
            if (old == null || unit < old) m.put(sell.getItem(), unit);
        }
        return m;
    }

    public static synchronized List<Lot> lots() {
        if (lots != null) return lots;
        List<Lot> l = new ArrayList<>();
        // сырьё для заведений: партия 16 шт., доставка на полки/в холодильники заведения
        for (BizType t : BizType.values()) {
            final BizType bt = t;
            l.add(new Lot(() -> ModItems.SUPPLY.get(bt).get(), SUPPLY_LOT, t.supplyPrice() * SUPPLY_LOT, 0, t));
        }
        Map<Item, Double> retail = retailPrices();
        Object[][] goods = {
                {(Supplier<Item>) () -> Items.COOKED_BEEF, 32}, {(Supplier<Item>) () -> Items.BREAD, 48},
                {(Supplier<Item>) () -> Items.GOLDEN_CARROT, 16}, {(Supplier<Item>) () -> Items.GOLDEN_APPLE, 4},
                {(Supplier<Item>) () -> Items.TORCH, 64}, {(Supplier<Item>) () -> Items.OAK_LOG, 64},
                {(Supplier<Item>) () -> Items.COBBLESTONE, 128}, {(Supplier<Item>) () -> Items.COAL, 64},
                {(Supplier<Item>) () -> Items.IRON_INGOT, 64}, {(Supplier<Item>) () -> Items.DIAMOND, 8}};
        for (Object[] g : goods) {
            @SuppressWarnings("unchecked") Supplier<Item> it = (Supplier<Item>) g[0];
            int count = (Integer) g[1];
            Double unit = retail.get(it.get());
            if (unit == null) continue;
            int r = (int) Math.round(unit * count);
            double bank = ItemValues.valueOf(new ItemStack(it.get())) * count;
            int price = roundUp10(Math.max(r * 0.75, bank * 1.1));
            if (price < r) l.add(new Lot(it, count, price, r));
        }
        // комплектующие для майнинга: в рознице не продаются (только крафт)
        l.add(new Lot(() -> ModItems.GPU.get(), 1, 1600, 0));
        l.add(new Lot(() -> ModItems.CPU.get(), 1, 700, 0));
        l.add(new Lot(() -> ModItems.MOTHERBOARD.get(), 1, 600, 0));
        l.add(new Lot(() -> ModItems.RAM.get(), 1, 300, 0));
        l.add(new Lot(() -> ModItems.PSU.get(), 1, 400, 0));
        l.add(new Lot(() -> ModItems.COOLER.get(), 1, 200, 0));
        l.add(new Lot(() -> ModItems.CRYPTO_RIG.get(), 1, 500, 0));
        lots = l;
        return l;
    }

    static void buy(ServerPlayerEntity p, Account a, EconomyData data, int idx) {
        List<Lot> l = lots();
        if (idx < 0 || idx >= l.size()) return;
        if (a.captivity != Account.FREE) return;
        if (!data.ownsBusiness(p.getUniqueID())) {
            BankService.msg(p, "§3[Опт] §cОптовые закупки — только для владельцев заведений. Купи заведение: телефон → Бизнес.");
            return;
        }
        Lot lot = l.get(idx);
        LegalBusiness target = null;
        if (lot.supply != null) {
            for (LegalBusiness b : data.bizOf(p.getUniqueID())) if (b.type == lot.supply) { target = b; break; }
            if (target == null) {
                BankService.msg(p, "§3[Опт] §c«" + lot.supply.supplyName + "» — сырьё для «" + lot.supply.title + "». У вас нет такого заведения.");
                return;
            }
        }
        if (!BankService.charge(p, a, lot.price)) {
            BankService.msg(p, "§3[Опт] §cПартия стоит " + Finance.money(lot.price) + ". Счёт + наличка — не хватает.");
            return;
        }
        Item item = lot.item.get();
        if (target != null) {
            net.minecraft.world.server.ServerWorld ow = EconomyData.overworld(p.getServer());
            ItemStack rest = LegalBiz.deliver(ow, target, new ItemStack(item, lot.count));
            int placed = lot.count - rest.getCount();
            if (!rest.isEmpty()) BankService.give(p, rest);
            BankService.msg(p, "§3[Опт] §fГазель приехала в «" + target.type.title + "»: §e" + lot.count + "× " + lot.supply.supplyName
                    + "§f за §a" + Finance.money(lot.price) + "§f. " + (placed > 0 ? "Разложено по полкам/холодильникам: §a" + placed + "§f. " : "")
                    + (rest.isEmpty() ? "" : "§eНе влезло " + rest.getCount() + " — отдали вам в руки (поставь полки/стеллажи в зоне)."));
            return;
        }
        int left = lot.count;
        int max = new ItemStack(item).getMaxStackSize();
        while (left > 0) {
            int n = Math.min(max, left);
            BankService.give(p, new ItemStack(item, n));
            left -= n;
        }
        p.sendStatusMessage(new net.minecraft.util.text.StringTextComponent("§3[Опт] §fПартия доставлена: §e" + lot.count + "× ")
                .append(new net.minecraft.util.text.TranslationTextComponent(item.getTranslationKey()))
                .appendString("§f за §a" + Finance.money(lot.price)
                        + (lot.retail > 0 ? "§f (в рознице " + Finance.money(lot.retail) + ")" : "") + "."), false);
    }
}
