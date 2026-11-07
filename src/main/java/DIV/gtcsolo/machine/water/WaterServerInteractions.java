package DIV.gtcsolo.machine.water;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.fluid.potion.PotionFluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = Gtcsolo.MODID)
public final class WaterServerInteractions {
    private WaterServerInteractions() {}

    public static boolean acceptsWater(ItemStack held) {
        if (held.isEmpty()) return false;
        if (held.is(Items.GLASS_BOTTLE)) return true;
        if (!held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) return false;
        return FluidUtil.getFluidHandler(held.copyWithCount(1)).map(handler ->
                handler.fill(new FluidStack(Fluids.WATER, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE) > 0).orElse(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fillFromWall(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getUseBlock() == Event.Result.DENY || event.getUseItem() == Event.Result.DENY) return;
        Level level = event.getLevel();
        BlockPos position = event.getPos();
        boolean woodenWall = level.getBlockState(position).is(GTBlocks.CASING_WOOD_WALL.get());
        WaterServerMachine core = MetaMachine.getMachine(level, position) instanceof WaterServerMachine water ? water : null;
        if ((!woodenWall && core == null) || !acceptsWater(event.getItemStack())) return;
        List<WaterServerMachine> owners = core == null ? owners(level, position) : List.of(core);
        if (owners.isEmpty()) return;
        // Predict the same consumed block interaction on the client, preventing a second bucket-use packet.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (!level.isClientSide) {
            for (WaterServerMachine owner : owners) {
                if (fill(owner, event.getEntity(), event.getHand())) break;
            }
        }
    }

    private static List<WaterServerMachine> owners(Level level, BlockPos wall) {
        var result = new ArrayList<WaterServerMachine>();
        if (level instanceof ServerLevel server) {
            // Reuse GregTech's formed-structure index; do not keep another wall-to-controller registry.
            for (var state : MultiblockWorldSavedData.getOrCreate(server).getControllerInChunk(new ChunkPos(wall))) {
                if (!state.isPosInCache(wall) || !level.isLoaded(state.controllerPos)) continue;
                if (state.getController() instanceof WaterServerMachine machine && machine.canServeWalls() && machine.containsWoodWall(wall))
                    result.add(machine);
            }
            result.sort(Comparator.comparingInt(WaterServerMachine::getStoredWater).reversed()
                    .thenComparingLong(machine -> machine.getPos().asLong()));
        } else {
            // Client prediction only, on a container click: at most 175 nearby positions, no ticking scan.
            for (BlockPos candidate : BlockPos.betweenClosed(wall.offset(-2, -3, -2), wall.offset(2, 3, 2))) {
                if (!level.isLoaded(candidate)) continue;
                if (MetaMachine.getMachine(level, candidate) instanceof WaterServerMachine machine
                        && machine.canServeWalls() && machine.containsWoodWall(wall)) {
                    result.add(machine);
                    break;
                }
            }
        }
        return result;
    }

    private static boolean fill(WaterServerMachine machine, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack filled;
        if (held.is(Items.GLASS_BOTTLE)) {
            // Native potion volume is 250 mB. The potion-only glass-bottle capability does not accept plain water.
            int amount = PotionFluidHelper.BOTTLE_AMOUNT;
            if (machine.getStoredWater() < amount) return false;
            filled = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
            machine.getWaterTank().drain(amount, IFluidHandler.FluidAction.EXECUTE);
            player.level().playSound(null, machine.getPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1, 1);
        } else {
            var action = FluidUtil.tryFillContainer(held.copyWithCount(1), machine.getWaterTank(), Integer.MAX_VALUE, player, true);
            if (!action.isSuccess()) return false;
            filled = action.getResult();
        }
        // Fill the held container in both game modes; vanilla handles stacks, full inventories and dropped remainders.
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled, false));
        player.containerMenu.broadcastChanges();
        return true;
    }
}
