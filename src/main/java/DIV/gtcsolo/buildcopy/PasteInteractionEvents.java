package DIV.gtcsolo.buildcopy;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.registry.ModBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gtcsolo.MODID)
public final class PasteInteractionEvents {
    private PasteInteractionEvents() {}
    // Vanilla skips block.use while sneaking with an item. Allow only our two explicit gestures.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown()
                || event.getUseBlock() == Event.Result.DENY || !event.getLevel().getBlockState(event.getPos()).is(ModBlocks.BLOCK10.get())) return;
        var held = event.getItemStack();
        if (held.isEmpty() || held.is(GTItems.TERMINAL.get())) {
            event.setUseBlock(Event.Result.ALLOW);
            event.setUseItem(Event.Result.DENY);
        }
    }
}
