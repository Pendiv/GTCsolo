package DIV.gtcsolo.command;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.buildcopy.CopyLayout;
import DIV.gtcsolo.buildcopy.CopyLibrary;
import DIV.gtcsolo.block.copy.PasteBlock;
import DIV.gtcsolo.block.copy.PasteBlockEntity;
import DIV.gtcsolo.registry.ModBlocks;
import DIV.gtcsolo.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class CopyCommand {
    private CopyCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("copy").requires(source -> source.hasPermission(2))
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(CopyCommand::copy)))));
    }
    private static int copy(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        try { return capture(context); }
        catch (RuntimeException | java.io.IOException ex) {
            Gtcsolo.LOGGER.error("Could not copy block region", ex);
            context.getSource().sendFailure(Component.translatable("gtcsolo.copy.failed"));
            return 0;
        }
    }
    private static int capture(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException, java.io.IOException {
        var source = context.getSource();
        var player = source.getPlayerOrException();
        var level = source.getLevel();
        var first = BlockPosArgument.getBlockPos(context, "from");
        var second = BlockPosArgument.getBlockPos(context, "to");
        var min = new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
        var max = new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
        String name = StringArgumentType.getString(context, "name").trim();
        if (name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2) name = name.substring(1, name.length() - 1).trim();
        if (name.isEmpty() || name.length() > 80) {
            source.sendFailure(Component.translatable("gtcsolo.copy.invalid_name")); return 0;
        }
        // Long arithmetic before subtraction prevents malicious extreme coordinates from wrapping.
        long dx = (long) max.getX() - min.getX() + 1, dy = (long) max.getY() - min.getY() + 1, dz = (long) max.getZ() - min.getZ() + 1;
        if (dx > CopyLayout.MAX_SIDE || dy > CopyLayout.MAX_SIDE || dz > CopyLayout.MAX_SIDE || dx * dy * dz > CopyLayout.MAX_BLOCKS) {
            source.sendFailure(Component.translatable("gtcsolo.copy.too_large", CopyLayout.MAX_SIDE, CopyLayout.MAX_BLOCKS)); return 0;
        }
        if (level.isOutsideBuildHeight(min) || level.isOutsideBuildHeight(max)) {
            source.sendFailure(Component.translatable("gtcsolo.copy.outside_world")); return 0;
        }
        // No implicit chunk generation/loading.
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) {
            if (!level.hasChunk(x, z)) {
                source.sendFailure(Component.translatable("gtcsolo.copy.unloaded")); return 0;
            }
        }
        var size = new BlockPos((int) dx, (int) dy, (int) dz);
        var palette = new ArrayList<BlockState>();
        var stateIds = new HashMap<BlockState, Integer>();
        int[] blocks = new int[CopyLayout.checkedVolume(size)];
        var entities = new HashMap<Integer, CompoundTag>();
        BlockPos anchor = first.subtract(min);
        var facing = player.getDirection().getOpposite();
        boolean foundAnchor = false;
        long entityBytes = 0;
        int index = 0;
        for (int y = 0; y < size.getY(); y++) for (int z = 0; z < size.getZ(); z++) for (int x = 0; x < size.getX(); x++) {
            var pos = min.offset(x, y, z);
            var state = level.getBlockState(pos);
            Integer stateId = stateIds.get(state);
            if (stateId == null) {
                stateId = palette.size(); palette.add(state); stateIds.put(state, stateId);
                if (palette.size() > CopyLayout.MAX_PALETTE) {
                    source.sendFailure(Component.translatable("gtcsolo.copy.too_many_states", CopyLayout.MAX_PALETTE)); return 0;
                }
            }
            blocks[index] = stateId;
            if (state.is(ModBlocks.BLOCK10.get())) {
                if (foundAnchor) {
                    source.sendFailure(Component.translatable("gtcsolo.copy.multiple_anchors")); return 0;
                }
                foundAnchor = true; anchor = pos.subtract(min); facing = state.getValue(PasteBlock.FACING);
            } else {
                var entity = level.getBlockEntity(pos);
                if (entity != null) {
                    var data = entity.saveWithFullMetadata();
                    entityBytes += CopyLayout.encodedBytes(data);
                    if (entityBytes > 8 * 1024 * 1024) {
                        source.sendFailure(Component.translatable("gtcsolo.copy.too_much_data")); return 0;
                    }
                    entities.put(index, data);
                }
            }
            index++;
        }
        try {
            var layout = new CopyLayout(name, size, anchor, facing, palette, blocks, entities);
            if (CopyLayout.encodedBytes(layout.save(false)) > 1024 * 1024) {
                source.sendFailure(Component.translatable("gtcsolo.copy.too_much_data")); return 0;
            }
            var id = CopyLibrary.get(level).add(layout);
            var item = new ItemStack(ModItems.BLOCK10_ITEM.get());
            PasteBlockEntity.bind(item, id, name);
            if (!player.getInventory().add(item)) player.drop(item, false);
            source.sendSuccess(() -> Component.translatable("gtcsolo.copy.saved", layout.name, layout.volume(),
                    size.getX(), size.getY(), size.getZ()), false);
            return layout.volume();
        } catch (RuntimeException ex) {
            Gtcsolo.LOGGER.error("Could not copy block region", ex);
            source.sendFailure(Component.translatable("gtcsolo.copy.failed"));
            return 0;
        }
    }
}
