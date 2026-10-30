package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.Trades;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtCustomerGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.entity.merchant.villager.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.MerchantOffers;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

/**
 * Продавец в банке. Стоит на месте, торгует за купюры. Неуязвим.
 * variant 0 — продукты, 1 — хозтовары и инструменты.
 */
public class SellerEntity extends WanderingTraderEntity {
    private int variant;

    public SellerEntity(EntityType<? extends WanderingTraderEntity> type, World world) {
        super(type, world);
        this.enablePersistence();
        this.setInvulnerable(true);
    }

    public void setVariant(int v) {
        this.variant = v;
        this.offers = null;
        this.setCustomName(new StringTextComponent(v == Trades.SELLER_FOOD ? "§aПродавец: продукты"
                : v == Trades.SELLER_CASINO ? "§5Бармен казино" : "§6Продавец: хозтовары"));
        this.setCustomNameVisible(true);
    }

    public int getVariant() {
        return variant;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(2, new LookAtCustomerGoal(this));
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 8.0F));
    }

    @Override
    protected void populateTradeData() {
        MerchantOffers mine = this.getOffers();
        mine.addAll(Trades.seller(variant));
    }

    @Override
    protected void onVillagerTrade(MerchantOffer offer) {
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void writeAdditional(CompoundNBT nbt) {
        super.writeAdditional(nbt);
        nbt.putInt("SellerVariant", variant);
    }

    @Override
    public void readAdditional(CompoundNBT nbt) {
        super.readAdditional(nbt);
        this.variant = nbt.getInt("SellerVariant");
        this.setDespawnDelay(0);
    }
}
