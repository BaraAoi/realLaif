package com.baraaoi.reallife.client;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.entity.WorkerEntity;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;

/** Работник заведения: скин по виду заведения (textures/entity/worker/&lt;вид&gt;.png). */
public class WorkerRenderer extends BipedRenderer<WorkerEntity, PlayerModel<WorkerEntity>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[BizType.values().length];

    static {
        for (BizType t : BizType.values()) {
            TEXTURES[t.ordinal()] = new ResourceLocation(RealLife.MODID, "textures/entity/worker/" + t.id() + ".png");
        }
    }

    public WorkerRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    @Override
    public ResourceLocation getEntityTexture(WorkerEntity entity) {
        return TEXTURES[entity.getBizType().ordinal()];
    }
}
