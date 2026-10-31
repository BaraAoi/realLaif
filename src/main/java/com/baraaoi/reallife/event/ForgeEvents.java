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
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.event.server.ServerStartedEvent;

public class ForgeEvents {

    public static void register() {
        TickEvent.ServerTickEvent.Post.BUS.addListener(ForgeEvents::onServerTick);
        net.minecraftforge.event.server.ServerStoppingEvent.BUS.addListener(ForgeEvents::onServerStopping);
        ServerStartedEvent.BUS.addListener(Priority.HIGH, ForgeEvents::onServerStarted);
        net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck.BUS.addListener(ForgeEvents::onCheckSpawn);
        BlockEvent.EntityPlaceEvent.BUS.addListener(Priority.HIGH, e -> { return onPlace(e); });
        PlayerInteractEvent.RightClickBlock.BUS.addListener(e -> { return onRightClickBlock(e); });
        net.minecraftforge.event.level.ExplosionEvent.Detonate.BUS.addListener(ForgeEvents::onExplosion);
        RegisterCommandsEvent.BUS.addListener(ForgeEvents::onCommands);
        LivingDeathEvent.BUS.addListener(Priority.HIGH, ForgeEvents::onDeath);
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(ForgeEvents::onRespawn);
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(ForgeEvents::onLogin);
        BlockEvent.BreakEvent.BUS.addListener(e -> { return onBreak(e); });
        net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish.BUS.addListener(ForgeEvents::onFinishUsing);
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(e -> {
            boolean cancel = onInteract(e);
            if (cancel) e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return cancel;
        });
    }


    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        MinecraftServer server = event.server();
        if (server != null) {
            DebtManager.serverTick(server);
            Visitors.tick(server);
            Raids.tick(server);
            com.baraaoi.reallife.economy.LegalBiz.tickCustomers(server);
            com.baraaoi.reallife.economy.Crypto.tick(server);
            com.baraaoi.reallife.economy.Rivals.tick(server);
        }
    }

    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        Raids.onStop(event.getServer());
        Visitors.onStop();
    }

    public static void onServerStarted(ServerStartedEvent event) {
        DebtManager.onServerStarted(event.getServer());
    }

    /** v1.9: в городских постройках (казино, банк, МФО) монстры не спавнятся. */
    public static void onCheckSpawn(net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.Enemy)) return;
        if (cityProtected(event.getLevel(), net.minecraft.core.BlockPos.containing(event.getX(), event.getY(), event.getZ())))
            event.setResult(net.minecraftforge.common.util.Result.DENY);
    }

    /** Блок внутри неразрушимой городской постройки (банк/МФО на спавне, казино). */
    private static boolean cityProtected(net.minecraft.world.level.LevelAccessor world, net.minecraft.core.BlockPos pos) {
        if (!(world instanceof net.minecraft.server.level.ServerLevel)) return false;
        net.minecraft.server.level.ServerLevel w = (net.minecraft.server.level.ServerLevel) world;
        if (w.dimension() != net.minecraft.world.level.Level.OVERWORLD) return false;
        return EconomyData.get(w.getServer()).isProtected(pos);
    }

    public static boolean onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer)) return false;
        ServerPlayer p = (ServerPlayer) event.getEntity();
        if (!p.isCreative() && cityProtected(event.getLevel(), event.getPos())) {
            BankService.actionBar(p, "§cГородская собственность — строить здесь нельзя.");
            return true;
        }
        if (!p.isCreative() && EconomyData.get(p.level().getServer()).get(p).captivity != Account.FREE) {
            BankService.actionBar(p, "§cВ камере строить нельзя.");
            return true;
        }
        String mallDeny = p.isCreative() ? null : com.baraaoi.reallife.economy.Malls.editDenied(EconomyData.get(p.level().getServer()), p.getUUID(), event.getPos());
        if (mallDeny != null) {
            BankService.actionBar(p, "§cТЦ: " + mallDeny);
            return true;
        }
        DebtManager.onPlace(p, event.getPos());
        com.baraaoi.reallife.economy.Business.onPlaced(p, event.getPos(), event.getPlacedBlock().getBlock());
        return false;
    }

    /** v1.11: ПКМ по табличке помещения ТЦ или вывеске конкурента. */
    public static boolean onRightClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return false;
        if (!(event.getEntity() instanceof ServerPlayer)) return false;
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof net.minecraft.world.level.block.SignBlock)) return false;
        ServerPlayer p = (ServerPlayer) event.getEntity();
        if (com.baraaoi.reallife.economy.Malls.onSignClick(p, event.getPos()) || com.baraaoi.reallife.economy.Rivals.onSignClick(p, event.getPos())) {
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return true;
        }
        return false;
    }

    public static void onExplosion(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) return;
        event.getAffectedBlocks().removeIf(pos -> cityProtected(event.getLevel(), pos));
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel) {
            EconomyData d = EconomyData.get(((net.minecraft.server.level.ServerLevel) event.getLevel()).getServer());
            event.getAffectedBlocks().removeIf(pos -> d.bizAtCounter(pos) != null); // главная касса заведения не взрывается
        }
    }

    public static void onCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof ServerPlayer) {
            DebtManager.onPlayerDeath((ServerPlayer) event.getEntity());
        } else if (event.getEntity() instanceof DebtNpcEntity) {
            Entity killer = event.getSource().getEntity();
            if (killer instanceof ServerPlayer) {
                DebtManager.onNpcKilled((DebtNpcEntity) event.getEntity(), (ServerPlayer) killer);
            }
        }
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = (ServerPlayer) event.getEntity();
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(p);
        if (a.captivity != Account.FREE) DebtManager.sendToCaptivity(server, data, p, a);
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = (ServerPlayer) event.getEntity();
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(p);
        data.setDirty();
        if (a.captivity != Account.FREE) {
            DebtManager.sendToCaptivity(server, data, p, a);
        } else if (a.arrears > 0.01) {
            BankService.msg(p, BankService.BANK + "§cНапоминаем: у вас просрочка " + Finance.money(a.arrears) + ".");
        }
        if (a.hasMfo()) BankService.msg(p, BankService.MFO + "С возвращением! Твой долг: §c" + Finance.money(a.mfoDebt));
    }

    public static boolean onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide() || !(event.getPlayer() instanceof ServerPlayer)) return false;
        if (!event.getPlayer().isCreative() && cityProtected(event.getLevel(), event.getPos())) {
            BankService.actionBar(event.getPlayer(), "§cГородская собственность под охраной — ломать нельзя.");
            return true;
        }
        String mallDeny = event.getPlayer().isCreative() ? null
                : com.baraaoi.reallife.economy.Malls.editDenied(EconomyData.get(event.getPlayer().level().getServer()), event.getPlayer().getUUID(), event.getPos());
        if (mallDeny != null) {
            BankService.actionBar(event.getPlayer(), "§cТЦ: " + mallDeny);
            return true;
        }
        ServerPlayer bp = (ServerPlayer) event.getPlayer();
        if (DebtManager.onBreak(bp, event.getPos(), event.getState())) return true;
        if (event.getState().getBlock() == com.baraaoi.reallife.registry.ModBlocks.BIZ_COUNTER.get()
                && com.baraaoi.reallife.economy.LegalBiz.onCounterBreak(bp, event.getPos())) return true;
        DebtManager.onPlayerBreak(bp, event.getPos());
        com.baraaoi.reallife.economy.Business.onBroken(bp.level().getServer(), event.getPos(), event.getState());
        Visitors.onBreak(bp, event.getPos(), event.getState());
        return false;
    }

    /** Съеденный пирожок бабушки Зины может оказаться с сюрпризом. */
    public static void onFinishUsing(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide()) return;
        Visitors.onEaten(event.getEntity(), event.getItem());
    }

    /** ПКМ по Жоре / Вовану — заплатить прямо им из инвентаря. */
    public static boolean onInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer)) return false;
        Entity t = event.getTarget();
        if (t instanceof com.baraaoi.reallife.entity.VisitorEntity && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            Visitors.onInteract((ServerPlayer) event.getEntity(), (com.baraaoi.reallife.entity.VisitorEntity) t);
            return true;
        }
        if (t instanceof com.baraaoi.reallife.entity.CustomerEntity && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            ((com.baraaoi.reallife.entity.CustomerEntity) t).talkTo(event.getEntity());
            return true;
        }
        if (t instanceof com.baraaoi.reallife.entity.WorkerEntity && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            com.baraaoi.reallife.economy.LegalBiz.workerTalk((ServerPlayer) event.getEntity(), (com.baraaoi.reallife.entity.WorkerEntity) t);
            return true;
        }
        if (t instanceof com.baraaoi.reallife.entity.CashierEntity && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            ServerPlayer p = (ServerPlayer) event.getEntity();
            net.minecraft.core.BlockPos pos = t.blockPosition();
            p.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, pl) -> new com.baraaoi.reallife.container.FinanceContainer(id, inv, pos, false),
                    net.minecraft.network.chat.Component.literal("Касса «Реал Банка»")), pos);
            BankService.sync(p);
            return true;
        }
        if (t instanceof InspectorEntity && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            ServerPlayer p = (ServerPlayer) event.getEntity();
            InspectorEntity ins = (InspectorEntity) t;
            if (!p.getUUID().equals(ins.getTargetId())) {
                ins.say(p, "Я не к вам, гражданин.");
            } else {
                EconomyData data = EconomyData.get(p.level().getServer());
                Account a = data.get(p);
                if (a.landCase && DebtManager.buyLand(p, a, a.landChunk)) {
                    ins.say(p, "§aВот, другое дело. Документы в порядке, участок ваш. Всего доброго!");
                }
            }
            return true;
        }
        if (!(t instanceof ZhoraEntity) && !(t instanceof VovanEntity)) return false;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return false;
        ServerPlayer p = (ServerPlayer) event.getEntity();
        DebtNpcEntity npc = (DebtNpcEntity) t;
        if (!p.getUUID().equals(npc.getTargetId())) {
            npc.say(p, "Я не к вам.");
            return false;
        }
        EconomyData data = EconomyData.get(p.level().getServer());
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
        data.setDirty();
        return true;
    }
}
