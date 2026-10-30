package com.baraaoi.reallife.registry;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.entity.*;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.MobEntity;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, RealLife.MODID);

    public static final RegistryObject<EntityType<ZhoraEntity>> ZHORA = ENTITIES.register("zhora",
            () -> EntityType.Builder.<ZhoraEntity>create(ZhoraEntity::new, EntityClassification.MISC)
                    .size(0.6F, 1.8F).build(RealLife.MODID + ":zhora"));
    public static final RegistryObject<EntityType<AlexanderEntity>> ALEXANDER = ENTITIES.register("alexander",
            () -> EntityType.Builder.<AlexanderEntity>create(AlexanderEntity::new, EntityClassification.MISC)
                    .size(0.6F, 1.8F).build(RealLife.MODID + ":alexander"));
    public static final RegistryObject<EntityType<PoliceEntity>> POLICE = ENTITIES.register("police",
            () -> EntityType.Builder.<PoliceEntity>create(PoliceEntity::new, EntityClassification.MISC)
                    .size(0.6F, 1.8F).build(RealLife.MODID + ":police"));
    public static final RegistryObject<EntityType<VovanEntity>> VOVAN = ENTITIES.register("vovan",
            () -> EntityType.Builder.<VovanEntity>create(VovanEntity::new, EntityClassification.MISC)
                    .size(0.6F, 1.8F).build(RealLife.MODID + ":vovan"));
    public static final RegistryObject<EntityType<BratokEntity>> BRATOK = ENTITIES.register("bratok",
            () -> EntityType.Builder.<BratokEntity>create(BratokEntity::new, EntityClassification.MISC)
                    .size(0.6F, 1.8F).build(RealLife.MODID + ":bratok"));

    public static final RegistryObject<EntityType<InspectorEntity>> INSPECTOR = ENTITIES.register("inspector",
            () -> EntityType.Builder.<InspectorEntity>create(InspectorEntity::new, EntityClassification.MISC)
                    .size(0.6F, 1.8F).build(RealLife.MODID + ":inspector"));
    public static final RegistryObject<EntityType<SellerEntity>> SELLER = ENTITIES.register("seller",
            () -> EntityType.Builder.<SellerEntity>create(SellerEntity::new, EntityClassification.MISC)
                    .size(0.6F, 1.95F).build(RealLife.MODID + ":seller"));

    private static AttributeModifierMap attrs(double health, double speed, double damage) {
        return MobEntity.func_233666_p_()
                .createMutableAttribute(Attributes.MAX_HEALTH, health)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, speed)
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, damage)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 64.0D)
                .create();
    }

    public static void registerAttributes() {
        GlobalEntityTypeAttributes.put(ZHORA.get(), attrs(20, 0.32, 1));
        GlobalEntityTypeAttributes.put(ALEXANDER.get(), attrs(60, 0.33, 4));
        GlobalEntityTypeAttributes.put(POLICE.get(), attrs(30, 0.36, 5));
        GlobalEntityTypeAttributes.put(VOVAN.get(), attrs(24, 0.34, 2));
        GlobalEntityTypeAttributes.put(BRATOK.get(), attrs(34, 0.35, 5));
        GlobalEntityTypeAttributes.put(INSPECTOR.get(), attrs(20, 0.34, 1));
        GlobalEntityTypeAttributes.put(SELLER.get(), MobEntity.func_233666_p_()
                .createMutableAttribute(Attributes.MAX_HEALTH, 20)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.0D)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 16.0D)
                .create());
    }
}
