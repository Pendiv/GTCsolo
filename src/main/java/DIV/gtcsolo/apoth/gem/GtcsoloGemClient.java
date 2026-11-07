package DIV.gtcsolo.apoth.gem;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "gtcsolo", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GtcsoloGemClient {

    private GtcsoloGemClient() {}

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional e) {
        Set<String> iconsets = new HashSet<>();
        for (GemSkin skin : GemSkinRegistry.all().values()) {
            iconsets.add(skin.iconset());
        }
        for (String iconset : iconsets) {
            e.register(new ResourceLocation("gtceu", "item/material_sets/" + iconset + "/gem"));
        }
    }

    @SubscribeEvent
    public static void onRegisterColors(RegisterColorHandlersEvent.Item e) {
        if (!Apotheosis.enableAdventure) return;
        e.register((stack, tintIndex) -> {
            DynamicHolder<Gem> gem = GemItem.getGem(stack);
            if (!gem.isBound()) return 0xFFFFFFFF;
            GemSkin skin = GemSkinRegistry.get(gem.getId());
            if (skin == null) return 0xFFFFFFFF;
            return (skin.color() & 0x00FFFFFF) | 0xFF000000;
        }, Adventure.Items.GEM.get());
    }
}
