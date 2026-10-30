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
        if (server != null) DebtManager.serverTick(server);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onServerStarted(FMLServerStartedEvent event) {
        DebtManager.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getWorld().isRemote() || !(event.getEntity() instanceof ServerPlayerEntity)) return;
        DebtManager.onPlace((ServerPlayerEntity) event.getEntity(), event.getPos());
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
        if (DebtManager.onBreak((ServerPlayerEntity) event.getPlayer(), event.getPos(), event.getState())) {
            event.setCanceled(true);
        }
        if (!event.isCanceled()) DebtManager.onPlayerBreak((ServerPlayerEntity) event.getPlayer(), event.getPos());
    }

    /** ПКМ по Жоре / Вовану — заплатить прямо им из инвентаря. */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote || !(event.getPlayer() instanceof ServerPlayerEntity)) return;
        Entity t = event.getTarget();
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
