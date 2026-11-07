package DIV.gtcsolo.client;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.EntroniumBlock;
import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.block.MaterialPipeBlock;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EntroniumClientEvents {
    private EntroniumClientEvents() {}

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // LDLib's wrapped models use the block's registered layer, not just model JSON render_type.
            for (var block : ForgeRegistries.BLOCKS) {
                boolean matches = block instanceof MaterialBlock material && EntroniumBlock.matches(material.material)
                        || block instanceof MaterialPipeBlock<?, ?, ?> pipe && EntroniumBlock.matches(pipe.material);
                if (matches) ItemBlockRenderTypes.setRenderLayer(block, RenderType.translucent());
            }
        });
    }
}
