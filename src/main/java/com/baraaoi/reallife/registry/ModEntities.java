package com.baraaoi.reallife.registry;

import com.baraaoi.reallife.entity.CashierEntity;
import com.baraaoi.reallife.entity.AmbalEntity;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.entity.*;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RealLife.MODID);

    public static final RegistryObject<EntityType<ZhoraEntity>> ZHORA = ENTITIES.register("zhora",
            () -> EntityType.Builder.<ZhoraEntity>of(ZhoraEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).build(ENTITIES.key("zhora")));
    public static final RegistryObject<EntityType<AlexanderEntity>> ALEXANDER = ENTITIES.register("alexander",
            () -> EntityType.Builder.<AlexanderEntity>of(AlexanderEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).build(ENTITIES.key("alexander")));
    public static final RegistryObject<EntityType<PoliceEntity>> POLICE = ENTITIES.register("police",
            () -> EntityType.Builder.<PoliceEntity>of(PoliceEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).build(ENTITIES.key("police")));
    public static final RegistryObject<EntityType<VovanEntity>> VOVAN = ENTITIES.register("vovan",
            () -> EntityType.Builder.<VovanEntity>of(VovanEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).build(ENTITIES.key("vovan")));
    public static final RegistryObject<EntityType<BratokEntity>> BRATOK = ENTITIES.register("bratok",
            () -> EntityType.Builder.<BratokEntity>of(BratokEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).build(ENTITIES.key("bratok")));

    public static final RegistryObject<EntityType<InspectorEntity>> INSPECTOR = ENTITIES.register("inspector",
            () -> EntityType.Builder.<InspectorEntity>of(InspectorEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).build(ENTITIES.key("inspector")));
    public static final RegistryObject<EntityType<SellerEntity>> SELLER = ENTITIES.register("seller",
            () -> EntityType.Builder.<SellerEntity>of(SellerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).build(ENTITIES.key("seller")));

    public static final RegistryObject<EntityType<CashierEntity>> CASHIER = ENTITIES.register("cashier",
            () -> EntityType.Builder.<CashierEntity>of(CashierEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).build(ENTITIES.key("cashier")));
    public static final RegistryObject<EntityType<AmbalEntity>> AMBAL = ENTITIES.register("ambal",
            () -> EntityType.Builder.<AmbalEntity>of(AmbalEntity::new, MobCategory.MISC)
                    .sized(0.7F, 1.95F).build(ENTITIES.key("ambal")));

    /** Случайный гость (налоговая, сантехник, цыганка...). Вид — в данных сущности. */
    public static final RegistryObject<EntityType<VisitorEntity>> VISITOR = ENTITIES.register("visitor",
            () -> EntityType.Builder.<VisitorEntity>of(VisitorEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).build(ENTITIES.key("visitor")));

    /** Работник легального заведения. */
    public static final RegistryObject<EntityType<com.baraaoi.reallife.entity.WorkerEntity>> WORKER = ENTITIES.register("worker",
            () -> EntityType.Builder.<com.baraaoi.reallife.entity.WorkerEntity>of(com.baraaoi.reallife.entity.WorkerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).build(ENTITIES.key("worker")));

    /** Подозрительный тип — самостоятельный NPC. */
    public static final RegistryObject<EntityType<com.baraaoi.reallife.entity.SnoopEntity>> SNOOP = ENTITIES.register("snoop",
            () -> EntityType.Builder.<com.baraaoi.reallife.entity.SnoopEntity>of(com.baraaoi.reallife.entity.SnoopEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).build(ENTITIES.key("snoop")));

    /** Покупатель заведения (v1.7) — косметика, «живость». */
    public static final RegistryObject<EntityType<com.baraaoi.reallife.entity.CustomerEntity>> CUSTOMER = ENTITIES.register("customer",
            () -> EntityType.Builder.<com.baraaoi.reallife.entity.CustomerEntity>of(com.baraaoi.reallife.entity.CustomerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).build(ENTITIES.key("customer")));

    private static AttributeSupplier attrs(double health, double speed, double damage) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .build();
    }

    public static void registerAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ZHORA.get(), attrs(20, 0.32, 1));
        event.put(ALEXANDER.get(), attrs(60, 0.33, 4));
        event.put(POLICE.get(), attrs(30, 0.36, 5));
        event.put(VOVAN.get(), attrs(24, 0.34, 2));
        event.put(BRATOK.get(), attrs(34, 0.35, 5));
        event.put(INSPECTOR.get(), attrs(20, 0.34, 1));
        event.put(AMBAL.get(), attrs(80, 0.38, 6));
        event.put(VISITOR.get(), attrs(20, 0.32, 2));
        event.put(SNOOP.get(), attrs(16, 0.3, 1));
        event.put(CUSTOMER.get(), attrs(16, 0.28, 1));
        event.put(WORKER.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .build());
        event.put(CASHIER.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .build());
        event.put(SELLER.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .build());
    }
}
