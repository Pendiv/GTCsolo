package DIV.gtcsolo.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * 描画しない renderer。 視覚はサーバー側で発射するパーティクル trail に任せる用 (= AmethystProjectile)。
 */
public class NoOpEntityRenderer<T extends Entity> extends EntityRenderer<T> {

    private static final ResourceLocation MISSING =
            new ResourceLocation("minecraft", "missingno");

    public NoOpEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return MISSING;
    }
}
