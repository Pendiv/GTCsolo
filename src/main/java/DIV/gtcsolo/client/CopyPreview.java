package DIV.gtcsolo.client;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.buildcopy.CopyLayout;
import DIV.gtcsolo.mixin.CopyPreviewRendererAccess;
import DIV.gtcsolo.network.CopyPreviewPacket;
import DIV.gtcsolo.registry.ModBlocks;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;

@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT)
public final class CopyPreview {
    private static BlockPos lastOrigin;
    private static String lastName;
    private static ResourceKey<Level> dimension;
    private static Level previewLevel;
    private static TrackedDummyWorld activeWorld;
    private static int layer = -1, remaining;
    private CopyPreview() {}

    public static void show(CopyPreviewPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.level.getBlockState(packet.origin()).is(ModBlocks.BLOCK10.get())) return;
        try {
            var layout = CopyLayout.load(packet.layout(), false);
            if (packet.origin().equals(lastOrigin) && layout.name.equals(lastName) && mc.level == previewLevel) {
                if (++layer >= layout.size.getY()) layer = -1;
            } else layer = -1;
            lastOrigin = packet.origin(); lastName = layout.name;
            dimension = mc.level.dimension(); previewLevel = mc.level;
            var rotation = layout.rotationTo(packet.facing());
            var blocks = new HashMap<BlockPos, BlockInfo>();
            for (int i = 0; i < layout.volume(); i++) {
                if (layer >= 0 && layout.local(i).getY() != layer) continue;
                if (i == layout.index(layout.anchor)) continue;
                var state = layout.rotatedState(i, rotation);
                if (state.isAir()) continue;
                // Empty Mek multiblock BEs may crash their BER (missing formed-world data).
                // Keep their block models, but never create/execute their BER in a dummy world.
                var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                var info = id.getNamespace().startsWith("mekanism") ? new BlockInfo(state, false) : BlockInfo.fromBlockState(state);
                blocks.put(layout.target(i, packet.origin(), rotation), info);
            }
            var world = new TrackedDummyWorld();
            world.addBlocks(blocks);
            MultiblockInWorldPreviewRenderer.cleanPreview();
            activeWorld = world;
            CopyPreviewRendererAccess.gtcsolo$setPreviewLevel(world);
            remaining = Math.max(20, ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20);
            CopyPreviewRendererAccess.gtcsolo$prepareBuffers(world, blocks.keySet(), remaining);
            mc.player.displayClientMessage(Component.translatable(layer < 0 ? "gtcsolo.copy.preview_all" : "gtcsolo.copy.preview_layer",
                    layout.name, layer + 1, layout.size.getY()), true);
        } catch (RuntimeException ex) {
            Gtcsolo.LOGGER.error("Could not preview copied structure", ex);
            clear();
            mc.player.sendSystemMessage(Component.translatable("gtcsolo.copy.failed"));
        }
    }
    private static void clear() {
        if (activeWorld != null && CopyPreviewRendererAccess.gtcsolo$getPreviewLevel() == activeWorld)
            MultiblockInWorldPreviewRenderer.cleanPreview();
        activeWorld = null;
        lastOrigin = null; lastName = null; dimension = null; previewLevel = null; layer = -1; remaining = 0;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || lastOrigin == null) return;
        var mc = Minecraft.getInstance();
        if (CopyPreviewRendererAccess.gtcsolo$getPreviewLevel() != activeWorld) {
            clear(); return;
        }
        if (mc.level == null || mc.level != previewLevel || !mc.level.dimension().equals(dimension)
                || !mc.level.getBlockState(lastOrigin).is(ModBlocks.BLOCK10.get()) || --remaining <= 0) clear();
    }
}
