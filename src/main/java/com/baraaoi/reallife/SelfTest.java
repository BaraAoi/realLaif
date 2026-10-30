package com.baraaoi.reallife;

import com.baraaoi.reallife.economy.Structure;
import com.baraaoi.reallife.economy.CasinoService;
import com.baraaoi.reallife.entity.CashierEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.Business;
import com.baraaoi.reallife.entity.VisitorEntity;
import com.baraaoi.reallife.entity.VisitorKind;
import com.baraaoi.reallife.event.Visitors;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.economy.CreditRating;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.economy.HouseType;
import com.baraaoi.reallife.economy.ItemValues;
import com.baraaoi.reallife.economy.Trades;
import com.baraaoi.reallife.entity.SellerEntity;
import com.baraaoi.reallife.registry.ModItems;
import com.baraaoi.reallife.tile.FridgeTileEntity;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import java.util.List;
import com.baraaoi.reallife.entity.DebtNpcEntity;
import com.baraaoi.reallife.event.DebtManager;
import com.baraaoi.reallife.registry.ModBlocks;
import com.baraaoi.reallife.registry.ModEntities;
import com.baraaoi.reallife.structure.Blueprint;
import com.baraaoi.reallife.structure.Buildings;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Интеграционный самотест для CI: запускается только с -Dreallife.selftest=true.
 * Строит все здания, прогоняет экономику по дням и создаёт всех NPC, затем выключает сервер.
 */
public class SelfTest {
    private static final Logger LOG = LogManager.getLogger("RealLifeSelfTest");

    private static void check(boolean cond, String what) {
        if (!cond) throw new IllegalStateException("CHECK FAILED: " + what);
        LOG.info("ok: " + what);
    }

    @SubscribeEvent
    public void onStarted(FMLServerStartedEvent event) {
        if (!Boolean.getBoolean("reallife.selftest")) return;
        MinecraftServer s = event.getServer();
        try {
            run(s);
            LOG.info("REALLIFE SELFTEST PASSED");
        } catch (Throwable t) {
            LOG.error("REALLIFE SELFTEST FAILED", t);
        }
        s.initiateShutdown(false);
    }

    private void run(MinecraftServer s) {
        ServerWorld ow = EconomyData.overworld(s);
        BlockPos base = new BlockPos(ow.getWorldInfo().getSpawnX(), 120, ow.getWorldInfo().getSpawnZ());
        EconomyData data = EconomyData.get(s);

        // ---- банк и МФО на спавне
        check(data.spawnBuilt && data.spawnVersion == 3, "bank and mfo built at spawn (v" + data.spawnVersion + ")");
        check(!data.mfoCenters.isEmpty(), "spawn mfo registered (" + data.mfoCenters + ")");
        int sx0 = ow.getWorldInfo().getSpawnX(), sz0 = ow.getWorldInfo().getSpawnZ();
        Structure spawnBank = null, spawnMfo = null, casino = null;
        for (Structure st : data.structures) {
            LOG.info("structure " + st.type + " " + st.min + " -> " + st.max + " protect=" + st.protect + " cashiers=" + st.cashiers.size());
            if (st.protect && Structure.BANK.equals(st.type)) spawnBank = st;
            if (st.protect && Structure.MFO.equals(st.type)) spawnMfo = st;
            if (Structure.CASINO.equals(st.type)) casino = st;
        }
        check(spawnBank != null && spawnMfo != null, "spawn bank and mfo are registered as protected");
        check(spawnBank.cashiers.size() == 3, "spawn bank has 3 cashier spots");
        BlockPos sbc = new BlockPos(sx0, spawnBank.cashiers.get(0).getY(), sz0 + 12);
        check(ow.getBlockState(sbc).getBlock() == ModBlocks.BANK_COUNTER.get() && data.isProtected(sbc), "spawn bank counter is protected at " + sbc);
        check(!ow.getEntitiesWithinAABB(CashierEntity.class, new AxisAlignedBB(spawnBank.min, spawnBank.max.add(1, 1, 1))).isEmpty(), "spawn bank cashiers present");

        // ---- казино возле спавна
        check(data.casinoBuilt && casino != null && casino.protect, "casino built near spawn");
        double cd = Math.sqrt(Math.pow((casino.min.getX() + casino.max.getX()) / 2.0 - sx0, 2) + Math.pow((casino.min.getZ() + casino.max.getZ()) / 2.0 - sz0, 2));
        check(cd >= 80 && cd <= 192, "casino is not next to spawn but within 12 chunks: " + (int) cd);
        int slots = 0, coins = 0, roul = 0, anarchy = 0;
        for (BlockPos p : BlockPos.getAllInBoxMutable(casino.min, casino.max)) {
            net.minecraft.block.Block b = ow.getBlockState(p).getBlock();
            if (b == ModBlocks.SLOT_MACHINE.get()) slots++;
            else if (b == ModBlocks.COIN_TABLE.get()) coins++;
            else if (b == ModBlocks.ROULETTE_TABLE.get()) roul++;
            else if (b == ModBlocks.ANARCHY_MACHINE.get()) anarchy++;
        }
        check(slots == 24 && coins == 6 && roul == 6 && anarchy == 6, "casino games: slots " + slots + ", coin " + coins + ", roulette " + roul + ", anarchy " + anarchy);
        BlockPos ce = casino.exit;
        boolean outside = ce.getX() < casino.min.getX() || ce.getX() > casino.max.getX() || ce.getZ() < casino.min.getZ() || ce.getZ() > casino.max.getZ();
        boolean near = ce.getX() >= casino.min.getX() - 6 && ce.getX() <= casino.max.getX() + 6 && ce.getZ() >= casino.min.getZ() - 6 && ce.getZ() <= casino.max.getZ() + 6;
        check(outside && near, "casino exit is just outside: " + ce);
        check(!ow.getEntitiesWithinAABB(SellerEntity.class, new AxisAlignedBB(casino.min, casino.max.add(1, 1, 1))).isEmpty(), "casino bartender");
        // v4: шансы подняты, но казино всё ещё (чуть-чуть) в плюсе
        int wheelSum = 0, wheelWins = 0;
        for (int m : CasinoService.WHEEL) { wheelSum += m; if (m >= 2) wheelWins++; }
        check(CasinoService.WHEEL.length == 100 && wheelSum == 99 && wheelWins == 26, "roulette return " + wheelSum + "%, wins " + wheelWins + "%");
        check(Math.abs(CasinoService.slotsRtp() - 0.96) < 1e-9, "slots theoretical return " + CasinoService.slotsRtp());
        java.util.Random rnd = new java.util.Random(4242L);
        int n = 200000, triples = 0, pairs = 0, sevens = 0;
        double paid = 0;
        for (int i = 0; i < n; i++) {
            int[] r = CasinoService.spinSlots(rnd);
            double mlt = CasinoService.slotMult(r);
            paid += mlt;
            if (r[0] == r[1] && r[1] == r[2]) { triples++; if (r[0] == 0) sevens++; }
            else if (r[0] == r[1] || r[1] == r[2] || r[0] == r[2]) pairs++;
        }
        LOG.info("slots sample: triples " + triples + ", 777 " + sevens + ", pairs " + pairs + ", return " + paid / n);
        check(triples > n * 0.065 && triples < n * 0.08 && pairs < n * 0.08 && pairs > triples * 0.8, "slots: more triples (" + triples * 100.0 / n + "%), fewer pairs (" + pairs * 100.0 / n + "%)");
        check(Math.abs(paid / n - 0.96) < 0.03, "slots sampled return " + paid / n);
        check(CasinoService.anarchyRtp() > 0.97 && CasinoService.anarchyRtp() < 1.0, "anarchy return " + CasinoService.anarchyRtp());
        check(CasinoService.COIN_WIN > 0.475 && CasinoService.COIN_WIN < 0.5, "coin chance " + CasinoService.COIN_WIN);
        check(ItemValues.valueOf(new ItemStack(Items.DIAMOND)) >= 300 && ItemValues.valueOf(new ItemStack(ModItems.BANKNOTE_100.get())) == 100, "item values doubled, banknotes at face value");
        check(!s.getRecipeManager().getRecipe(new net.minecraft.util.ResourceLocation("reallife", "money_printer")).isPresent(), "money printer can not be crafted");

        Blueprint bank = Buildings.buildBank(ow, base, Direction.SOUTH);
        check(ow.getBlockState(bank.pos(0, 1, 6)).getBlock() == ModBlocks.BANK_COUNTER.get(), "bank counter placed");
        check(ow.getBlockState(bank.pos(-2, 1, 1)).getBlock() == ModBlocks.ATM.get(), "bank atm placed");
        check(ow.getBlockState(bank.pos(-8, 1, 3)).getBlock() == ModBlocks.VENDING_FOOD.get(), "bank vending placed");
        check(!data.isProtected(bank.pos(0, 1, 6)), "permit-built bank is not protected");
        List<CashierEntity> cashiers = ow.getEntitiesWithinAABB(CashierEntity.class, new AxisAlignedBB(bank.min(), bank.max().add(1, 1, 1)));
        check(cashiers.size() == 3, "3 cashiers in bank (" + cashiers.size() + ")");
        check(cashiers.get(0).getAttributeValue(Attributes.MAX_HEALTH) > 0, "cashier has hp");
        List<SellerEntity> sellers = ow.getEntitiesWithinAABB(SellerEntity.class, new AxisAlignedBB(bank.min(), bank.max().add(1, 1, 1)));
        check(sellers.size() == 2, "2 sellers in bank (" + sellers.size() + ")");
        for (SellerEntity se : sellers) check(se.getOffers().size() >= 10, "seller " + se.getVariant() + " has offers: " + se.getOffers().size());
        check(Trades.vending(Trades.VENDING_FOOD).size() > 5 && Trades.vending(Trades.VENDING_RESOURCES).size() > 5, "vending offers");
        // перекупка невыгодна: каждый товар стоит дороже, чем за него даст банк
        check(Trades.seller(Trades.SELLER_CASINO).size() >= 8, "casino bar offers");
        for (int v = 0; v < 3; v++) {
            for (net.minecraft.item.MerchantOffer o : Trades.seller(v)) {
                double price = ItemValues.stackValue(o.getBuyingStackFirst());
                double back = ItemValues.stackValue(o.getSellingStack());
                check(back < price, "no arbitrage " + o.getSellingStack() + " " + back + " < " + price);
            }
        }

        Blueprint mfo = Buildings.buildMfo(ow, base.east(40), Direction.WEST);
        check(ow.getBlockState(mfo.pos(0, 1, 5)).getBlock() == ModBlocks.MFO_COUNTER.get(), "mfo counter placed (rotated)");
        BlockPos mfoCenter = mfo.pos(0, 0, 3);
        boolean registered = false;
        for (BlockPos m : data.mfoCenters) if (m.equals(mfoCenter)) registered = true;
        check(registered, "mfo center registered");
        check(ow.getBlockState(mfo.pos(-1, 1, 1)).getBlock() == Blocks.LOOM, "mfo has no free printer (loom instead)");

        // ---- дома
        Direction[] dirs = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
        Blueprint stone = null;
        for (HouseType h : HouseType.values()) {
            Blueprint b = Buildings.buildHouse(ow, base.add(h.ordinal() * 40, 0, 80), dirs[h.ordinal() % 4], h);
            LOG.info("built " + h.name() + " box " + b.min() + " -> " + b.max());
            check(!ow.isAirBlock(b.pos(0, -1, 2)), h.name() + " has floor");
            int fridges = 0, full = 0;
            for (BlockPos p : BlockPos.getAllInBoxMutable(b.min(), b.max())) {
                TileEntity te = ow.getTileEntity(p);
                if (te instanceof FridgeTileEntity) fridges++;
                if (te instanceof IInventory && !((IInventory) te).isEmpty()) full++;
            }
            LOG.info(h.name() + ": fridges " + fridges + ", filled containers " + full);
            check(full >= 1, h.name() + " has filled containers");
            if (h.ordinal() >= HouseType.WOODEN_HUT.ordinal()) check(fridges >= 1, h.name() + " has fridge");
            if (h == HouseType.STONE_HOUSE) {
                stone = b;
                check(ow.getBlockState(b.pos(-3, 0, 6)).getBlock() instanceof net.minecraft.block.BedBlock, "stone house bed is whole");
            }
            if (h == HouseType.MANSION) check(ow.getBlockState(b.pos(-6, 0, 1)).getBlock() == ModBlocks.ATM.get(), "mansion atm");
            if (h == HouseType.PENTHOUSE) {
                check(ow.getBlockState(b.pos(5, 0, 5)).getBlock() == ModBlocks.ATM.get(), "penthouse atm");
                check(ow.getBlockState(b.pos(1, 13, -3)).getBlock() == Blocks.WATER && ow.getBlockState(b.pos(1, 11, -3)).getBlock() == Blocks.GLASS,
                        "penthouse cantilevered glass pool");
                check(ow.isAirBlock(b.pos(1, 15, 0)) && ow.isAirBlock(b.pos(1, 16, 0)) && ow.isAirBlock(b.pos(0, 15, 3)), "terrace opens into the pool");
                TileEntity stand = ow.getTileEntity(b.pos(5, 15, 3));
                TileEntity bar = ow.getTileEntity(b.pos(5, 15, 1));
                check(stand instanceof IInventory && ((IInventory) stand).getStackInSlot(0).getItem() == Items.POTION, "bar brewing stand has drinks");
                check(bar instanceof IInventory && ((IInventory) bar).count(Items.POTION) >= 5, "bar barrel has drinks");
            }
        }

        // ---- СИЗО в небе со снимком
        Account jailed = data.getOrCreate(UUID.randomUUID());
        jailed.captivity = Account.JAIL;
        DebtManager.buildCell(s, data, jailed, null);
        BlockPos jc = jailed.cellCenter;
        check(jailed.cellActive && ow.getBlockState(jc).getBlock() == ModBlocks.JAIL_WALL.get(), "sky jail built at " + jc);
        check(jc.getY() > 200, "jail is in the sky");
        Account jailed2 = data.getOrCreate(UUID.randomUUID());
        jailed2.captivity = Account.JAIL;
        DebtManager.buildCell(s, data, jailed2, null);
        check(!jailed2.cellCenter.equals(jc) && jailed2.cellCenter.manhattanDistance(jc) >= 14, "second jail is separate: " + jailed2.cellCenter);
        DebtManager.restoreCell(s, data, jailed);
        DebtManager.restoreCell(s, data, jailed2);
        check(ow.isAirBlock(jc) && ow.isAirBlock(jc.up(5)) && ow.isAirBlock(jailed2.cellCenter), "jails removed, sky restored");

        // ---- подвал под МФО со снимком
        BlockPos probe = mfoCenter.add(1, -11, 0);
        ow.setBlockState(probe, Blocks.GOLD_BLOCK.getDefaultState(), 3);
        Account pit = data.getOrCreate(UUID.randomUUID());
        pit.captivity = Account.PIT;
        DebtManager.buildCell(s, data, pit, mfoCenter);
        BlockPos pc = pit.cellCenter;
        check(ow.getBlockState(pc).getBlock() == ModBlocks.JAIL_WALL.get(), "pit built at " + pc);
        check(ow.getBlockState(pc.add(0, 1, 3)).getBlock() == ModBlocks.PIT_RECEIVER.get(), "pit receiver");
        check(Buildings.pitValue(new ItemStack(Items.DIAMOND)) == 200 && Buildings.pitValue(new ItemStack(Items.DIRT)) == 3
                && Buildings.pitValue(new ItemStack(Items.DIAMOND_SWORD)) == 0, "pit loot values");
        check(pc.getX() == mfoCenter.getX() && pc.getZ() == mfoCenter.getZ() && pc.getY() + 5 < mfoCenter.getY(), "pit is under the mfo");
        Account pit2 = data.getOrCreate(UUID.randomUUID());
        pit2.captivity = Account.PIT;
        DebtManager.buildCell(s, data, pit2, mfoCenter);
        check(Math.abs(pit2.cellCenter.getY() - pc.getY()) >= 8 || Math.abs(pit2.cellCenter.getX() - pc.getX()) >= 12, "second pit does not overlap: " + pit2.cellCenter);
        DebtManager.restoreCell(s, data, pit);
        DebtManager.restoreCell(s, data, pit2);
        check(ow.getBlockState(probe).getBlock() == Blocks.GOLD_BLOCK, "pit removed, original blocks restored");
        check(ow.getBlockState(pc).getBlock() != ModBlocks.JAIL_WALL.get(), "no jail wall left");

        // ---- подпольный бизнес v4: станки ставит сам игрок, все печатают в «Сундук цеха»
        Account biz = data.getOrCreate(UUID.randomUUID());
        biz.hasHouse = true;
        biz.houseMin = stone.min();
        biz.houseMax = stone.max();
        biz.hasBusiness = true;
        List<BlockPos> floor = Visitors.floorSpots(ow, biz.houseMin, biz.houseMax);
        check(floor.size() >= 6, "stone house has free floor (" + floor.size() + ")");
        BlockPos pr1 = floor.get(0), pr2 = floor.get(floor.size() / 2), chestPos = floor.get(floor.size() - 1);
        ow.setBlockState(pr1, ModBlocks.MONEY_PRINTER.get().getDefaultState(), 3);
        ow.setBlockState(pr2, ModBlocks.MONEY_PRINTER.get().getDefaultState(), 3);
        check(Business.registerPrinter(data, biz, pr1) == Business.OK && Business.registerPrinter(data, biz, pr2) == Business.OK, "printers register inside own house");
        check(Business.registerPrinter(data, biz, pr1) == Business.ALREADY, "printer is not registered twice");
        BlockPos outLand = biz.houseMax.add(7, 0, 7);
        check(Business.registerPrinter(data, biz, outLand) == Business.NOT_OWNER, "printer outside own property is refused");
        data.claim(biz.id, EconomyData.chunkKey(outLand));
        check(Business.inOwnProperty(data, biz, outLand) && Business.registerPrinter(data, biz, outLand) == Business.OK, "printer on own claimed land is accepted");
        ow.getChunkAt(outLand);
        check(Business.workingPrinters(ow, biz) == 2 && biz.printerList.size() == 2, "missing printer block is dropped from the list");
        ow.setBlockState(chestPos, ModBlocks.BUSINESS_CHEST.get().getDefaultState(), 3);
        TileEntity bizChest = ow.getTileEntity(chestPos);
        check(bizChest instanceof FridgeTileEntity && "Сундук цеха".equals(((FridgeTileEntity) bizChest).getDisplayName().getString()), "business chest block works");
        biz.businessChest = chestPos;
        int perPrinter = Config.PRINTER_DAILY.get() / 100;
        check(BankService.produceDirty(s, biz) == 2 * perPrinter, "2 printers print into the linked chest");
        ow.setBlockState(pr2, Blocks.AIR.getDefaultState(), 3);
        check(BankService.produceDirty(s, biz) == perPrinter && biz.printerList.size() == 1, "removed printer stops printing");
        check(((IInventory) bizChest).count(ModItems.DIRTY_MONEY.get()) == 3 * perPrinter, "dirty money in the business chest");
        check(ItemValues.valueOf(new ItemStack(ModItems.DIRTY_MONEY.get())) == 100, "dirty money worth 100");
        // старый подвал: станки переносятся в список
        biz.businessMin = base.add(700, -20, 0);
        biz.businessMax = biz.businessMin.add(8, 5, 8);
        BlockPos legacy = biz.businessMin.add(2, 1, 2);
        ow.setBlockState(legacy, ModBlocks.MONEY_PRINTER.get().getDefaultState(), 3);
        Business.migrateLegacy(ow, biz);
        check(biz.printerList.contains(legacy) && Business.inOwnProperty(data, biz, legacy), "legacy basement printer migrated");
        biz.chestGiven = true;
        Account bizCopy = Account.read(biz.write());
        check(bizCopy.printerList.size() == biz.printerList.size() && bizCopy.chestGiven, "printers and chest flag saved");
        CompoundNBT oldBiz = biz.write();
        oldBiz.remove("bizVersion");
        oldBiz.remove("chestGiven");
        Account oldCopy = Account.read(oldBiz);
        check(oldCopy.chestGiven && oldCopy.needPrinters, "old save: chest counts as given, printers will be migrated");
        ow.setBlockState(legacy, Blocks.AIR.getDefaultState(), 3);
        biz.businessMin = BlockPos.ZERO;
        biz.businessMax = BlockPos.ZERO;
        DebtManager.demolishHouse(ow, biz);
        check(biz.hasBusiness && Business.workingPrinters(ow, biz) == 0 && biz.businessChest.equals(BlockPos.ZERO) && !biz.chestGiven,
                "house seizure takes printers and chest, next printer comes with a new chest");
        data.landOwners.remove(EconomyData.chunkKey(outLand));

        // ---- случайные гости
        check(Visitors.eventKinds().size() == VisitorKind.values().length - 3 && Visitors.eventKinds().size() >= 19, "visitor kinds: " + Visitors.eventIds());
        for (VisitorKind k : VisitorKind.values()) {
            java.nio.file.Path tex = net.minecraftforge.fml.ModList.get().getModFileById(RealLife.MODID).getFile()
                    .findResource("assets/reallife/textures/entity/visitor/" + k.id() + ".png");
            check(java.nio.file.Files.exists(tex), "texture for visitor " + k.id());
        }
        VisitorEntity ve = ModEntities.VISITOR.get().create(ow);
        check(ve != null, "create visitor");
        ve.setKind(VisitorKind.GRANNY);
        check(ve.getKind() == VisitorKind.GRANNY && ve.getAttributeValue(Attributes.MAX_HEALTH) > 0, "visitor kind and hp");
        check(VisitorKind.byName("horse_thief") == VisitorKind.HORSE_THIEF, "visitor names");
        check(Visitors.taxLimit(500) == 7000, "tax limit");
        Blueprint vh = Buildings.buildHouse(ow, base.add(0, 0, 300), Direction.SOUTH, HouseType.STONE_HOUSE);
        BlockPos vmin = vh.min(), vmax = vh.max();
        List<BlockPos> vfloor = Visitors.floorSpots(ow, vmin, vmax);
        check(vfloor.size() >= 8, "visitor house floor (" + vfloor.size() + ")");
        int l0 = Visitors.countLights(ow, vmin, vmax);
        ow.setBlockState(vfloor.get(0), Blocks.GLOWSTONE.getDefaultState(), 3);
        check(Visitors.countLights(ow, vmin, vmax) == l0 + 1, "electrician counts lights (" + l0 + " + 1)");
        int f0 = Visitors.countFurnaces(ow, vmin, vmax);
        ow.setBlockState(vfloor.get(1), Blocks.FURNACE.getDefaultState(), 3);
        ow.setBlockState(vfloor.get(2), Blocks.SMOKER.getDefaultState(), 3);
        check(Visitors.countFurnaces(ow, vmin, vmax) == f0 + 2, "fire inspector counts furnaces");
        check(Visitors.findLava(ow, vmin, vmax, 64).isEmpty(), "no lava yet");
        ow.setBlockState(vfloor.get(3), Blocks.LAVA.getDefaultState(), 3);
        check(Visitors.findLava(ow, vmin, vmax, 64).contains(vfloor.get(3)), "fire inspector finds lava");
        ow.setBlockState(vfloor.get(3), Blocks.AIR.getDefaultState(), 3);
        double v0 = Visitors.houseValue(ow, vmin, vmax);
        ow.setBlockState(vfloor.get(4), Blocks.CHEST.getDefaultState(), 3);
        TileEntity vc = ow.getTileEntity(vfloor.get(4));
        check(vc instanceof IInventory, "tax test chest");
        ((IInventory) vc).setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 10));
        double v1 = Visitors.houseValue(ow, vmin, vmax);
        check(Math.abs(v1 - v0 - 10 * ItemValues.valueOf(new ItemStack(Items.DIAMOND))) < 0.01, "tax inspector values house chests (" + v0 + " -> " + v1 + ")");
        ItemStack taken = Visitors.confiscateBest(ow, vmin, vmax);
        check(!taken.isEmpty() && Visitors.houseValue(ow, vmin, vmax) < v1, "tax inspector confiscates the best stack: " + taken);
        // площадка вокруг дома (дома в тесте висят в воздухе)
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(vmin.getX() - 4, vmin.getY(), vmin.getZ() - 4), new BlockPos(vmax.getX() + 5, vmin.getY(), vmax.getZ() + 5))) {
            if (ow.isAirBlock(p)) ow.setBlockState(p.toImmutable(), Blocks.STONE.getDefaultState(), 2);
        }
        int inLeak = Visitors.leak(ow, vmin, vmax, true, 3, new java.util.Random(5));
        int outLeak = Visitors.leak(ow, vmin, vmax, false, 4, new java.util.Random(6));
        check(inLeak == 3 && outLeak == 4, "plumber leaks inside " + inLeak + ", outside " + outLeak);
        BlockPos box = Visitors.buildNeighborBox(ow, data, vmin, vmax);
        check(box != null && ow.getBlockState(box).getBlock() == Blocks.DIRT && ow.isAirBlock(box.add(1, 1, 1))
                && ow.getBlockState(box.add(1, 2, 1)).getBlock() == Blocks.DIRT, "neighbor dirt box at " + box);

        // ---- ипотека: погашение до постройки не аннулирует ордер
        Account mo = data.getOrCreate(UUID.randomUUID());
        mo.mortHouse = HouseType.STONE_HOUSE.ordinal();
        mo.mortPrincipal = 1000;
        mo.mortDays = 10;
        mo.mortRate = 0.01;
        mo.pendingHouse = HouseType.STONE_HOUSE.ordinal();
        mo.houseOrderId = 12345L;
        mo.clearMortgage();
        check(!mo.hasMortgage() && mo.pendingHouse == HouseType.STONE_HOUSE.ordinal() && mo.houseOrderId == 12345L, "paid-off mortgage keeps the house order");
        check(!BankService.mortgageRefusal(mo, HouseType.DIRT_HUT, BankService.today(s)).isEmpty(), "no new mortgage while a house is pending");
        CompoundNBT moSaved = mo.write();
        check(Account.read(moSaved).pendingHouse == HouseType.STONE_HOUSE.ordinal(), "pending house saved");

        // ---- площадка под дом: ровная — можно; на доме, на банке, на склоне — нельзя
        BlockPos pad = base.add(600, 80, 0);
        for (BlockPos p : BlockPos.getAllInBoxMutable(pad.add(-12, -1, -12), pad.add(12, -1, 12))) ow.setBlockState(p.toImmutable(), Blocks.STONE.getDefaultState(), 2);
        UUID owner = UUID.randomUUID();
        String ok = Buildings.checkHouseSite(ow, pad, Direction.SOUTH, HouseType.DIRT_HUT, data, owner);
        check(ok == null, "flat site accepted (" + ok + ")");
        Blueprint hut = Buildings.buildHouse(ow, pad, Direction.SOUTH, HouseType.DIRT_HUT);
        Account ho = data.getOrCreate(owner);
        ho.hasHouse = true;
        ho.houseMin = hut.min();
        ho.houseMax = hut.max();
        String onHouse = Buildings.checkHouseSite(ow, pad.add(1, 5, 1), Direction.SOUTH, HouseType.WOODEN_HUT, data, UUID.randomUUID());
        check(onHouse != null, "house on top of a house refused: " + onHouse);
        String onBank = Buildings.checkHouseSite(ow, bank.pos(0, 8, 4), Direction.SOUTH, HouseType.DIRT_HUT, data, owner);
        check(onBank != null && onBank.contains("банк"), "house on the bank refused: " + onBank);
        String edge = Buildings.checkHouseSite(ow, pad.add(11, 0, 0), Direction.SOUTH, HouseType.STONE_HOUSE, data, owner);
        check(edge != null, "uneven site refused: " + edge);
        data.reset(owner);
        data.reset(mo.id);

        // ---- земля и выселение
        long ck = EconomyData.chunkKey(base.add(400, 0, 0));
        UUID builder = UUID.randomUUID();
        for (int i = 0; i < 50; i++) data.addPlaced(ck, builder, base.add(400, 0, i % 16));
        check(data.placedCount(ck, builder) == 16, "placed blocks counted per position");
        data.claim(builder, ck);
        check(builder.equals(data.owner(ck)) && data.placedCount(ck, builder) == 0, "land claim");
        int cx = (base.getX() + 400) >> 4, cz = base.getZ() >> 4;
        DebtManager.wipeLayers(ow, cx, cz, ow.getHeight() - 1, 0);
        boolean clean = true;
        for (int y = 5; y < 255; y++) if (!ow.isAirBlock(new BlockPos((cx << 4) + 7, y, (cz << 4) + 7))) clean = false;
        check(clean, "evicted chunk wiped");
        check(ow.getBlockState(new BlockPos(cx << 4, 0, cz << 4)).getBlock() == Blocks.BEDROCK, "bedrock remains");

        // ---- экономика
        Account a = data.getOrCreate(UUID.randomUUID());
        long today = BankService.today(s);
        a.createdDay = today;
        int fresh = CreditRating.score(a, today);
        LOG.info("fresh score " + fresh + ", max loan " + CreditRating.maxLoan(a, today));
        check(fresh >= 450 && CreditRating.maxLoan(a, today) > 0, "new client can get a small loan");
        a.balance = 3000;
        a.lifetimeDeposited = 6000;
        int rich = CreditRating.score(a, today);
        check(rich > fresh, "savings and income raise score (" + rich + ")");
        a.ratingBonus = CreditRating.bonusFor(a, today, 777);
        check(CreditRating.score(a, today) == 777, "admin rating command math");
        a.ratingBonus = 0;

        a.loanPrincipal = 1000;
        a.loanRate = 0.01;
        a.loanDays = 10;
        a.balance = 250;
        double pay = a.loanPayment();
        LOG.info("loan payment " + Finance.money(pay));
        BankService.processDay(s, data);
        check(a.arrears < 0.01 && a.onTimePayments == 1, "first payment auto-debited");
        a.balance = 0;
        for (int d = 1; d <= 6; d++) {
            BankService.processDay(s, data);
            LOG.info("day " + d + ": arrears " + Finance.money(a.arrears) + ", overdue " + a.overdueDays + ", stage " + DebtManager.bankStage(a));
        }
        check(DebtManager.bankStage(a) == 3, "bank escalates to police");
        check(CreditRating.score(a, today) < rich, "late payments lower score");

        // ---- МФО
        Account m = data.getOrCreate(UUID.randomUUID());
        m.mfoDebt = 1000;
        m.mfoPrincipal = 1000;
        m.mfoDays = 2;
        for (int d = 1; d <= 6; d++) {
            BankService.processDay(s, data);
            LOG.info("mfo day " + d + ": debt " + Finance.money(m.mfoDebt) + ", stage " + DebtManager.mfoStage(m));
        }
        check(m.mfoDebt > 2000, "mfo compound interest grows");
        check(DebtManager.mfoStage(m) == 3, "mfo escalates to lethal");

        // ---- NPC
        EntityType<?>[] types = {ModEntities.ZHORA.get(), ModEntities.ALEXANDER.get(), ModEntities.POLICE.get(),
                ModEntities.VOVAN.get(), ModEntities.BRATOK.get(), ModEntities.INSPECTOR.get(), ModEntities.AMBAL.get()};
        for (EntityType<?> t : types) {
            DebtNpcEntity e = (DebtNpcEntity) t.create(ow);
            check(e != null, "create " + t.getRegistryName());
            e.setPosition(base.getX(), base.getY() + 2, base.getZ() + 3);
            ow.addEntity(e);
            check(e.getAttributeValue(Attributes.MAX_HEALTH) > 0, e.npcName() + " hp=" + e.getHealth());
            e.remove();
        }
        data.reset(a.id);
        data.reset(m.id);
        for (Account x : new Account[]{jailed, jailed2, pit, pit2, biz}) data.reset(x.id);
        data.landOwners.remove(ck);
        data.structures.removeIf(st -> !st.protect);
        data.markDirty();
    }
}
