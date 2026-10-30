package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.Trades;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * Продавец в банке. Стоит на месте, торгует за купюры. Неуязвим.
 * variant 0 — продукты, 1 — хозтовары и инструменты.
 */
public class SellerEntity extends WanderingTrader {
    private int variant;

    public SellerEntity(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
    }

    public void setVariant(int v) {
        this.variant = v;
        this.offers = null;
        this.setCustomName(Component.literal(v == Trades.SELLER_FOOD ? "§aПродавец: продукты"
                : v == Trades.SELLER_CASINO ? "§5Бармен казино" : "§6Продавец: хозтовары"));
        this.setCustomNameVisible(true);
    }

    public int getVariant() {
        return variant;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(2, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    protected void updateTrades(net.minecraft.server.level.ServerLevel level) {
        MerchantOffers mine = this.getOffers();
        mine.addAll(Trades.seller(variant));
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("SellerVariant", variant);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        this.variant = nbt.getIntOr("SellerVariant", 0);
        this.setDespawnDelay(0);
    }
}
