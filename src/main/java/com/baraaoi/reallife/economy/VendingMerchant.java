package com.baraaoi.reallife.economy;

import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;


/** «Торговец» без сущности — для торговых автоматов. */
public class VendingMerchant implements Merchant {
    private final Level world;
    private Player customer;
    private MerchantOffers offers;
    private int xp;

    public VendingMerchant(Player player, MerchantOffers offers) {
        this.world = player.level();
        this.offers = offers;
    }

    @Override
    public void setTradingPlayer(Player player) {
        this.customer = player;
    }

    @Override
    public Player getTradingPlayer() {
        return customer;
    }

    @Override
    public MerchantOffers getOffers() {
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    public Level getWorld() {
        return world;
    }

    @Override
    public int getVillagerXp() {
        return xp;
    }

    @Override
    public void overrideXp(int xpIn) {
        this.xp = xpIn;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.NOTE_BLOCK_CHIME.value();
    }

    @Override
    public boolean isClientSide() {
        return world.isClientSide();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.customer == player;
    }
}
