package DIV.gtcsolo.common.framealtar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class FrameAltarHandler {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getTarget() instanceof ItemFrame clickedFrame)) return;

        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;
        if (!player.getMainHandItem().isEmpty()) return;

        Level level = event.getLevel();
        BlockPos pos = clickedFrame.getPos();

        List<ItemFrame> frames = getFramesAt(level, pos);
        if (frames.isEmpty()) return;

        List<ItemStack> items = new ArrayList<>();
        for (ItemFrame frame : frames) {
            ItemStack item = frame.getItem();
            if (!item.isEmpty()) {
                items.add(item);
            }
        }

        if (items.isEmpty()) return;

        FrameAltarRecipe recipe = FrameAltarRegistry.findMatchingRecipe(items);
        if (recipe == null) return;

        FrameAltarRecipe.MatchResult result = recipe.match(items);
        if (!result.isSuccess()) return;

        event.setCanceled(true);

        recipe.apply(result.target);

        // 素材の額縁を消費（ターゲット以外）
        for (ItemFrame frame : frames) {
            ItemStack frameItem = frame.getItem();
            if (frameItem.isEmpty()) continue;
            if (frameItem == result.target) continue;
            frame.setItem(ItemStack.EMPTY);
        }

        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private List<ItemFrame> getFramesAt(Level level, BlockPos pos) {
        AABB searchArea = new AABB(pos).inflate(0.5);
        return level.getEntitiesOfClass(ItemFrame.class, searchArea,
                frame -> frame.getPos().equals(pos));
    }
}