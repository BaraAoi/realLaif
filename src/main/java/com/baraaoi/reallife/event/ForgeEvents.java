package com.baraaoi.reallife.event;

import com.baraaoi.reallife.command.ModCommands;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.entity.DebtNpcEntity;
import com.baraaoi.reallife.entity.InspectorEntity;
import com.baraaoi.reallife.entity.VovanEntity;
import com.baraaoi.reallife.entity.ZhoraEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class ForgeEvents {

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            DebtManager.serverTick(server);
            Visitors.tick(server);
            Raids.tick(server);
            com.baraaoi.reallife.economy.LegalBiz.tickCustomers(server);
            com.baraaoi.reallife.economy.Crypto.tick(server);
            com.baraaoi.reallife.economy.Rivals.tick(server);
        }
    }

    @SubscribeEvent
    public void onServerStopping(net.minecraftforge.fml.event.server.FMLServerStoppingEvent event) {
        Raids.onStop(event.getServer());
        Visitors.onStop();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onServerStarted(FMLServerStartedEvent event) {
        DebtManager.onServerStarted(event.getServer());
    }

    /** v1.9: в городских постройках (казино, банк, МФО) монстры не спавнятся. */
    @SubscribeEvent
    public void onCheckSpawn(net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn event) {
        if (!(event.getEntityLiving() instanceof net.minecraft.entity.monster.IMob)) return;
        if (cityProtected(event.getWorld(), new net.minecraft.util.math.BlockPos(event.getX(), event.getY(), event.getZ())))
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
    }

    /** Блок внутри неразрушимой городской постройки (банк/МФО на спавне, казино). */
    private static boolean cityProtected(net.minecraft.world.IWorld world, net.minecraft.util.math.BlockPos pos) {
        if (!(world instanceof net.minecraft.world.server.ServerWorld)) return false;
        net.minecraft.world.server.ServerWorld w = (net.minecraft.world.server.ServerWorld) world;
        if (w.getDimensionKey() != net.minecraft.world.World.OVERWORLD) return false;
        return EconomyData.get(w.getServer()).isProtected(pos);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getWorld().isRemote() || !(event.getEntity() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) event.getEntity();
        if (!p.isCreative() && cityProtected(event.getWorld(), event.getPos())) {
            BankService.actionBar(p, "§cГородская собственность — строить здесь нельзя.");
            event.setCanceled(true);
            return;
        }
        if (!p.isCreative() && EconomyData.get(p.getServer()).get(p).captivity != Account.FREE) {
            BankService.actionBar(p, "§cВ камере строить нельзя.");
            event.setCanceled(true);
            return;
        }
        String mallDeny = p.isCreative() ? null : com.baraaoi.reallife.economy.Malls.editDenied(EconomyData.get(p.getServer()), p.getUniqueID(), event.getPos());
        if (mallDeny != null) {
            BankService.actionBar(p, "§cТЦ: " + mallDeny);
            event.setCanceled(true);
            return;
        }
        DebtManager.onPlace(p, event.getPos());
        com.baraaoi.reallife.economy.Business.onPlaced(p, event.getPos(), event.getPlacedBlock().getBlock());
    }

    /** v1.11: ПКМ по табличке помещения ТЦ или вывеске конкурента. */
    @SubscribeEvent
    public void onRightClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote() || event.getHand() != net.minecraft.util.Hand.MAIN_HAND) return;
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;
        if (!(event.getWorld().getBlockState(event.getPos()).getBlock() instanceof net.minecraft.block.AbstractSignBlock)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
        if (com.baraaoi.reallife.economy.Malls.onSignClick(p, event.getPos()) || com.baraaoi.reallife.economy.Rivals.onSignClick(p, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onExplosion(net.minecraftforge.event.world.ExplosionEvent.Detonate event) {
        if (event.getWorld().isRemote) return;
        event.getAffectedBlocks().removeIf(pos -> cityProtected(event.getWorld(), pos));
        if (event.getWorld() instanceof net.minecraft.world.server.ServerWorld) {
            EconomyData d = EconomyData.get(((net.minecraft.world.server.ServerWorld) event.getWorld()).getServer());
            event.getAffectedBlocks().removeIf(pos -> d.bizAtCounter(pos) != null); // главная касса заведения не взрывается
        }
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntityLiving().world.isRemote) return;
        if (event.getEntityLiving() instanceof ServerPlayerEntity) {
            DebtManager.onPlayerDeath((ServerPlayerEntity) event.getEntityLiving());
        } else if (event.getEntityLiving() instanceof DebtNpcEntity) {
            Entity killer = event.getSource().getTrueSource();
            if (killer instanceof ServerPlayerEntity) {
                DebtManager.onNpcKilled((DebtNpcEntity) event.getEntityLiving(), (ServerPlayerEntity) killer);
            }
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
        MinecraftServer server = p.getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(p);
        if (a.captivity != Account.FREE) DebtManager.sendToCaptivity(server, data, p, a);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
        MinecraftServer server = p.getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(p);
        data.markDirty();
        if (a.captivity != Account.FREE) {
            DebtManager.sendToCaptivity(server, data, p, a);
        } else if (a.arrears > 0.01) {
            BankService.msg(p, BankService.BANK + "§cНапоминаем: у вас просрочка " + Finance.money(a.arrears) + ".");
        }
        if (a.hasMfo()) BankService.msg(p, BankService.MFO + "С возвращением! Твой долг: §c" + Finance.money(a.mfoDebt));
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote() || !(event.getPlayer() instanceof ServerPlayerEntity)) return;
        if (!event.getPlayer().isCreative() && cityProtected(event.getWorld(), event.getPos())) {
            BankService.actionBar(event.getPlayer(), "§cГородская собственность под охраной — ломать нельзя.");
            event.setCanceled(true);
            return;
        }
        String mallDeny = event.getPlayer().isCreative() ? null
                : com.baraaoi.reallife.economy.Malls.editDenied(EconomyData.get(event.getPlayer().getServer()), event.getPlayer().getUniqueID(), event.getPos());
        if (mallDeny != null) {
            BankService.actionBar(event.getPlayer(), "§cТЦ: " + mallDeny);
            event.setCanceled(true);
            return;
        }
        if (DebtManager.onBreak((ServerPlayerEntity) event.getPlayer(), event.getPos(), event.getState())) {
            event.setCanceled(true);
        }
        if (!event.isCanceled() && event.getState().getBlock() == com.baraaoi.reallife.registry.ModBlocks.BIZ_COUNTER.get()
                && com.baraaoi.reallife.economy.LegalBiz.onCounterBreak((ServerPlayerEntity) event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
        if (!event.isCanceled()) {
            DebtManager.onPlayerBreak((ServerPlayerEntity) event.getPlayer(), event.getPos());
            com.baraaoi.reallife.economy.Business.onBroken(event.getPlayer().getServer(), event.getPos(), event.getState());
            Visitors.onBreak((ServerPlayerEntity) event.getPlayer(), event.getPos(), event.getState());
        }
    }

    /** Съеденный пирожок бабушки Зины может оказаться с сюрпризом. */
    @SubscribeEvent
    public void onFinishUsing(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (event.getEntityLiving().world.isRemote) return;
        Visitors.onEaten(event.getEntityLiving(), event.getItem());
    }

    /** ПКМ по Жоре / Вовану — заплатить прямо им из инвентаря. */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote || !(event.getPlayer() instanceof ServerPlayerEntity)) return;
        Entity t = event.getTarget();
        if (t instanceof com.baraaoi.reallife.entity.VisitorEntity && event.getHand() == net.minecraft.util.Hand.MAIN_HAND) {
            Visitors.onInteract((ServerPlayerEntity) event.getPlayer(), (com.baraaoi.reallife.entity.VisitorEntity) t);
            event.setCanceled(true);
            return;
        }
        if (t instanceof com.baraaoi.reallife.entity.CustomerEntity && event.getHand() == net.minecraft.util.Hand.MAIN_HAND) {
            ((com.baraaoi.reallife.entity.CustomerEntity) t).talkTo(event.getPlayer());
            event.setCanceled(true);
            return;
        }
        if (t instanceof com.baraaoi.reallife.entity.WorkerEntity && event.getHand() == net.minecraft.util.Hand.MAIN_HAND) {
            com.baraaoi.reallife.economy.LegalBiz.workerTalk((ServerPlayerEntity) event.getPlayer(), (com.baraaoi.reallife.entity.WorkerEntity) t);
            event.setCanceled(true);
            return;
        }
        if (t instanceof com.baraaoi.reallife.entity.CashierEntity && event.getHand() == net.minecraft.util.Hand.MAIN_HAND) {
            ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
            net.minecraft.util.math.BlockPos pos = t.getPosition();
            net.minecraftforge.fml.network.NetworkHooks.openGui(p, new net.minecraft.inventory.container.SimpleNamedContainerProvider(
                    (id, inv, pl) -> new com.baraaoi.reallife.container.FinanceContainer(id, inv, pos, false),
                    new net.minecraft.util.text.StringTextComponent("Касса «Реал Банка»")), pos);
            BankService.sync(p);
            event.setCanceled(true);
            return;
        }
        if (t instanceof InspectorEntity && event.getHand() == net.minecraft.util.Hand.MAIN_HAND) {
            ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
            InspectorEntity ins = (InspectorEntity) t;
            if (!p.getUniqueID().equals(ins.getTargetId())) {
                ins.say(p, "Я не к вам, гражданин.");
            } else {
                EconomyData data = EconomyData.get(p.getServer());
                Account a = data.get(p);
                if (a.landCase && DebtManager.buyLand(p, a, a.landChunk)) {
                    ins.say(p, "§aВот, другое дело. Документы в порядке, участок ваш. Всего доброго!");
                }
            }
            event.setCanceled(true);
            return;
        }
        if (!(t instanceof ZhoraEntity) && !(t instanceof VovanEntity)) return;
        if (event.getHand() != net.minecraft.util.Hand.MAIN_HAND) return;
        ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
        DebtNpcEntity npc = (DebtNpcEntity) t;
        if (!p.getUniqueID().equals(npc.getTargetId())) {
            npc.say(p, "Я не к вам.");
            return;
        }
        EconomyData data = EconomyData.get(p.getServer());
        Account a = data.get(p);
        if (t instanceof ZhoraEntity) {
            double got = BankService.takeValuables(p, a.arrears, 1.0);
            if (got <= 0) {
                npc.say(p, "У вас нет ни денег, ни ресурсов... Ох. Ну, вы постарайтесь, пожалуйста.");
            } else {
                a.balance += got;
                npc.say(p, "Принял " + Finance.money(got) + ". Спасибо за понимание!");
                BankService.autoPayArrears(p, a);
            }
        } else {
            double got = BankService.takeValuables(p, a.mfoDebt, 0.5);
            if (got <= 0) {
                npc.say(p, "Ты мне фантики суёшь? Пустой — значит, будем говорить по-другому.");
            } else {
                npc.say(p, "Так, это я беру. Твоё барахло — по полцены, сам понимаешь.");
                BankService.payMfo(p, a, got, " (через Вована)");
            }
        }
        data.markDirty();
        event.setCanceled(true);
    }
}
