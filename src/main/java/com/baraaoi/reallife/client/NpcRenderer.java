package com.baraaoi.reallife.client;

import com.baraaoi.reallife.RealLife;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

import java.util.function.Function;

/** Все люди мода рисуются моделью игрока со своим скином (64×64). */
public class NpcRenderer<T extends Mob> extends HumanoidMobRenderer<T, AvatarRenderState, PlayerModel> {
    public static class State extends AvatarRenderState {
        public Identifier texture;
    }

    private final Function<T, Identifier> texture;

    public NpcRenderer(EntityRendererProvider.Context ctx, Function<T, Identifier> texture) {
        super(ctx, new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.texture = texture;
    }

    public NpcRenderer(EntityRendererProvider.Context ctx, String name) {
        this(ctx, fixed(name));
    }

    private static <T> Function<T, Identifier> fixed(String name) {
        Identifier id = tex(name);
        return e -> id;
    }

    public static Identifier tex(String path) {
        return Identifier.fromNamespaceAndPath(RealLife.MODID, "textures/entity/" + path + ".png");
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, AvatarRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof State s) s.texture = texture.apply(entity);
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state instanceof State s && s.texture != null ? s.texture : tex("zhora");
    }
}
