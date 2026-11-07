package DIV.gtcsolo.render.material.portal;

import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.item.TagPrefixItem;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Galactica inventory and world models. Material IDs and recipe generation stay native. */
@Mod.EventBusSubscriber(modid = "gtcsolo", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GalacticaItemModels {
    private static final ResourceLocation MATERIAL = new ResourceLocation("gtcsolo", "galactica");

    private GalacticaItemModels() {}

    private static boolean matches(Item item) {
        if (item instanceof TagPrefixItem form) return MATERIAL.equals(form.material.getResourceLocation());
        return item instanceof BlockItem block && block.getBlock() instanceof MaterialBlock form
                && MATERIAL.equals(form.material.getResourceLocation());
    }

    @SubscribeEvent
    public static void creativeForms(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().equals(new ResourceLocation("gtcsolo", "gtcsolo_material"))) return;
        // The shared material tab intentionally lists only selected prefixes. Include small dusts and rotor too.
        for (Item item : ForgeRegistries.ITEMS) {
            if (matches(item) && !event.getEntries().contains(new ItemStack(item))) event.accept(item);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void bake(ModelEvent.ModifyBakingResult event) {
        int count = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            if (!matches(item)) continue;
            var id = new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(item), "inventory");
            var original = event.getModels().get(id);
            if (original == null || original instanceof PortalItemModel) continue;
            try {
                event.getModels().put(id, new PortalItemModel(original));
                count++;
            } catch (RuntimeException failure) {
                LogUtils.getLogger().error("Could not prepare Galactica portal model {}", id, failure);
            }
        }
        LogUtils.getLogger().info("Prepared {} Galactica portal item models", count);
        var worldModels = new java.util.HashMap<net.minecraft.world.level.block.state.BlockState, GalacticaBlockModel>();
        int worldCount = 0;
        for (var block : ForgeRegistries.BLOCKS) {
            if (!(block instanceof MaterialBlock form) || !MATERIAL.equals(form.material.getResourceLocation())) continue;
            boolean frame = form.tagPrefix == TagPrefix.frameGt;
            if (!frame && form.tagPrefix != TagPrefix.block) continue;
            for (var state : block.getStateDefinition().getPossibleStates()) {
                var id = BlockModelShaper.stateToModelLocation(state);
                var original = event.getModels().get(id);
                if (original == null || original instanceof GalacticaBlockModel) continue;
                try {
                    var model = new GalacticaBlockModel(original, state, frame);
                    event.getModels().put(id, model);
                    worldModels.put(state, model);
                    worldCount++;
                } catch (RuntimeException failure) {
                    LogUtils.getLogger().error("Could not prepare Galactica world model {}", id, failure);
                }
            }
        }
        GalacticaWorldRenderer.installModels(worldModels);
        LogUtils.getLogger().info("Prepared {} Galactica world models", worldCount);
    }
}
