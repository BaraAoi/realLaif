package com.baraaoi.reallife.economy;

import net.minecraft.entity.merchant.IMerchant;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.MerchantOffers;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** «Торговец» без сущности — для торговых автоматов. */
public class VendingMerchant implements IMerchant {
    private final World world;
    @Nullable
    private PlayerEntity customer;
    private MerchantOffers offers;
    private int xp;

    public VendingMerchant(PlayerEntity player, MerchantOffers offers) {
        this.world = player.world;
        this.offers = offers;
    }

    @Override
    public void setCustomer(@Nullable PlayerEntity player) {
        this.customer = player;
    }

    @Nullable
    @Override
    public PlayerEntity getCustomer() {
        return customer;
    }

    @Override
    public MerchantOffers getOffers() {
        return offers;
    }

    @Override
    public void setClientSideOffers(@Nullable MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void onTrade(MerchantOffer offer) {
        offer.increaseUses();
    }

    @Override
    public void verifySellingItem(ItemStack stack) {
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public int getXp() {
        return xp;
    }

    @Override
    public void setXP(int xpIn) {
        this.xp = xpIn;
    }

    @Override
    public boolean hasXPBar() {
        return false;
    }

    @Override
    public SoundEvent getYesSound() {
        return SoundEvents.BLOCK_NOTE_BLOCK_CHIME;
    }
}
