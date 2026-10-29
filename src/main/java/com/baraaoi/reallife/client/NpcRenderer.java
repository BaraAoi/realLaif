package com.baraaoi.reallife.client;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.entity.DebtNpcEntity;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;

public class NpcRenderer<T extends DebtNpcEntity> extends BipedRenderer<T, PlayerModel<T>> {
    private final ResourceLocation texture;

    public NpcRenderer(EntityRendererManager manager, String name) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
        this.texture = new ResourceLocation(RealLife.MODID, "textures/entity/" + name + ".png");
    }

    @Override
    public ResourceLocation getEntityTexture(T entity) {
        return texture;
    }
}
