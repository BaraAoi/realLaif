package com.baraaoi.reallife.client;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.entity.CustomerEntity;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;

/** Покупатель: несколько скинов (textures/entity/customer/c0..c5.png). */
public class CustomerRenderer extends BipedRenderer<CustomerEntity, PlayerModel<CustomerEntity>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[CustomerEntity.VARIANTS];

    static {
        for (int i = 0; i < TEXTURES.length; i++) TEXTURES[i] = new ResourceLocation(RealLife.MODID, "textures/entity/customer/c" + i + ".png");
    }

    public CustomerRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    @Override
    public ResourceLocation getEntityTexture(CustomerEntity entity) {
        return TEXTURES[entity.getVariant()];
    }
}
