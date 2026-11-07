package DIV.gtcsolo.block.copy;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.buildcopy.CopyLibrary;
import DIV.gtcsolo.buildcopy.PasteJobs;
import DIV.gtcsolo.network.ModNetwork;
import DIV.gtcsolo.network.CopyPreviewPacket;
import DIV.gtcsolo.registry.ModBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;
import java.util.List;

/** Keeps block10's registry identity and artwork while adding copy-template interaction. */
public final class PasteBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public PasteBlock() {
        super(Properties.of().strength(5.0f, 12.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PasteBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof PasteBlockEntity paste) paste.readItem(stack);
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var stack = new ItemStack(ModBlocks.BLOCK10.get());
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PasteBlockEntity paste) paste.bindItem(stack);
        return List.of(stack);
    }
    @Override public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, BlockGetter level, BlockPos pos, Player player) {
        var stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof PasteBlockEntity paste) paste.bindItem(stack);
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> text, TooltipFlag flag) {
        text.add(Component.translatable("gtcsolo.copy.tooltip.preview"));
        text.add(Component.translatable("gtcsolo.copy.tooltip.paste"));
        text.add(Component.translatable("gtcsolo.copy.tooltip.overwrite"));
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return InteractionResult.PASS;
        var held = player.getItemInHand(hand);
        boolean preview = held.isEmpty();
        boolean paste = held.is(GTItems.TERMINAL.get());
        if (!preview && !paste) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof PasteBlockEntity entity) || entity.copyId() == null) {
            player.sendSystemMessage(Component.translatable("gtcsolo.copy.empty")); return InteractionResult.CONSUME;
        }
        try {
            var layout = CopyLibrary.get(serverLevel).get(entity.copyId());
            if (layout == null) {
                player.sendSystemMessage(Component.translatable("gtcsolo.copy.missing")); return InteractionResult.CONSUME;
            }
            if (preview) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new CopyPreviewPacket(pos, state.getValue(FACING), layout.save(false)));
            } else {
                if (!player.hasPermissions(2) || !player.isCreative()) {
                    player.sendSystemMessage(Component.translatable("gtcsolo.copy.paste_permission")); return InteractionResult.CONSUME;
                }
                PasteJobs.start(serverPlayer, entity, layout);
            }
        } catch (RuntimeException ex) {
            Gtcsolo.LOGGER.error("Could not use paste block at {}", pos, ex);
            player.sendSystemMessage(Component.translatable("gtcsolo.copy.failed"));
        }
        return InteractionResult.CONSUME;
    }
}
