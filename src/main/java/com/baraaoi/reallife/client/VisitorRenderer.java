package com.baraaoi.reallife.client;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.entity.VisitorEntity;
import com.baraaoi.reallife.entity.VisitorKind;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;

/** Гость: скин зависит от вида (textures/entity/visitor/&lt;вид&gt;.png). */
public class VisitorRenderer extends BipedRenderer<VisitorEntity, PlayerModel<VisitorEntity>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[VisitorKind.values().length];

    static {
        for (VisitorKind k : VisitorKind.values()) {
            TEXTURES[k.ordinal()] = new ResourceLocation(RealLife.MODID, "textures/entity/visitor/" + k.id() + ".png");
        }
    }

    public VisitorRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    @Override
    public ResourceLocation getEntityTexture(VisitorEntity entity) {
        return TEXTURES[entity.getKind().ordinal()];
    }
}
