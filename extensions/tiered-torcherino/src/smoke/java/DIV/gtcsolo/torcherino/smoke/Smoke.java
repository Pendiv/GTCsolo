package DIV.gtcsolo.torcherino.smoke;

import DIV.gtcsolo.torcherino.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import torcherino.api.TorcherinoAPI;
import torcherino.block.entity.TorcherinoBlockEntity;

@Mod("tiered_torch_smoke")
public final class Smoke {
    private static BlockPos last;
    private ServerPlayer uiPlayer;
    private int uiPhase;
    private int uiWait;
    public Smoke() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::login);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private void started(ServerStartedEvent event) {
        try {
            ServerLevel level = event.getServer().overworld();
            for (Profile profile : Profile.values()) {
                var tier = TorcherinoAPI.INSTANCE.getTiers().get(profile.id());
                check(tier != null && tier.xzRange() == 4 && tier.yRange() == 1 && tier.maxSpeed() == profile.defaultPower + 1,
                        "Native tier synchronization data: " + profile);
                check(TieredTorcherino.FLOOR.get(profile).get().defaultBlockState().hasBlockEntity(), "Entity block missing");
                check(TorcherinoAPI.INSTANCE.isBlockBlacklisted(TieredTorcherino.FLOOR.get(profile).get()), "Floor cannot accelerate itself: " + profile);
                check(TorcherinoAPI.INSTANCE.isBlockBlacklisted(TieredTorcherino.WALL.get(profile).get()), "Wall cannot accelerate itself: " + profile);
                for (var form : java.util.List.of(TieredTorcherino.FLOOR, TieredTorcherino.WALL,
                        TieredTorcherino.LANTERNS, TieredTorcherino.PUMPKINS)) {
                    var block = form.get(profile).get();
                    check(TorcherinoAPI.INSTANCE.isBlockBlacklisted(block), "Every form must exclude self-acceleration: " + profile);
                    var entity = ((net.minecraft.world.level.block.EntityBlock) block).newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                    check(entity instanceof TorcherinoBlockEntity torch && entity.getType().isValid(block.defaultBlockState())
                            && torch.getTier().equals(profile.id()), "Native type and shared tier for every form: " + profile);
                }
            }
            BlockPos pos = new BlockPos(0, level.getMaxBuildHeight() - 6, 0);
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, TieredTorcherino.FLOOR.get(Profile.LV).get().defaultBlockState());
            TorcherinoBlockEntity be = (TorcherinoBlockEntity) level.getBlockEntity(pos);
            check(be != null && be.getType().isValid(be.getBlockState()), "Native entity type must accept tiered floor blocks");
            check(be.getType().isValid(TieredTorcherino.WALL.get(Profile.MAX).get().defaultBlockState()), "Wall block must be valid");
            check(((BudgetEntity) be).tiered$getState().equals(new Budget.State(0, 0, 0, 1)), "Initial +100%, width 1");
            check(be.readClientData(1, 0, 0, 2, 2), "LV two-point allocation accepted");
            Budget.State accepted = ((BudgetEntity) be).tiered$getState();
            check(!be.readClientData(1, 1, 0, 2, 2), "Over-budget network edit rejected");
            check(((BudgetEntity) be).tiered$getState().equals(accepted), "Rejected edit leaves accepted allocation unchanged");
            check(!be.readClientData(-1, 0, 0, 1, 2), "Negative network range rejected");
            // Redstone mode 2 is always active. Exercise the real native ticker with
            // a nonempty area: state validation alone missed self-acceleration recursion.
            be.setPoweredByRedstone(false);
            BlockPos furnacePos = pos.east();
            // setBlock with the same state retains the previous run's inventory/progress.
            level.setBlockAndUpdate(furnacePos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(furnacePos, Blocks.FURNACE.defaultBlockState());
            var furnace = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) level.getBlockEntity(furnacePos);
            furnace.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON));
            furnace.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL));
            TorcherinoBlockEntity.tick(level, pos, be.getBlockState(), be);
            check(((BudgetEntity) be).tiered$getState().equals(accepted), "Active native tick completes without recursion");
            check(furnace.saveWithoutMetadata().getShort("CookTime") == 2, "Native engine delivers two extra furnace ticks");
            // All forms share native behavior, but each registered ID must be safe in a real area.
            for (var form : java.util.List.of(TieredTorcherino.LANTERNS, TieredTorcherino.PUMPKINS, TieredTorcherino.WALL)) {
                BlockPos shapePos = pos.offset(0, 0, 4);
                level.setBlockAndUpdate(shapePos.below(), Blocks.STONE.defaultBlockState());
                // Default wall-torch facing NORTH attaches to its SOUTH neighbor.
                level.setBlockAndUpdate(shapePos.south(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(shapePos, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(shapePos, form.get(Profile.LV).get().defaultBlockState());
                TorcherinoBlockEntity shape = (TorcherinoBlockEntity) level.getBlockEntity(shapePos);
                check(shape != null && shape.readClientData(1, 0, 0, 2, 2), "Form accepts LV budget");
                shape.setPoweredByRedstone(false);
                BlockPos target = shapePos.east();
                level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(target, Blocks.FURNACE.defaultBlockState());
                var shapeFurnace = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) level.getBlockEntity(target);
                shapeFurnace.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON));
                shapeFurnace.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL));
                TorcherinoBlockEntity.tick(level, shapePos, shape.getBlockState(), shape);
                check(shapeFurnace.saveWithoutMetadata().getShort("CookTime") == 2, "Every form accelerates without recursion");
            }
            CompoundTag tampered = be.saveWithoutMetadata();
            for (String key : new String[]{"Speed", "XRange", "YRange", "ZRange"}) tampered.putInt(key, Integer.MAX_VALUE);
            be.load(tampered);
            check(Budget.valid(((BudgetEntity) be).tiered$getState(), 2), "Tampered save is normalized");
            ((BudgetEntity) be).tiered$setState(new Budget.State(1000, 1000, 1000, 1000));
            TorcherinoBlockEntity.tick(level, pos, be.getBlockState(), be);
            check(Budget.valid(((BudgetEntity) be).tiered$getState(), 2), "Per-tick guard repairs bypassed state");
            BlockPos maxPos = pos.offset(6, 0, 0);
            level.setBlockAndUpdate(maxPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(maxPos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(maxPos, TieredTorcherino.FLOOR.get(Profile.MAX).get().defaultBlockState());
            TorcherinoBlockEntity maxTorch = (TorcherinoBlockEntity) level.getBlockEntity(maxPos);
            check(maxTorch.readClientData(4, 4, 1, 2, 2), "High tier accepts 9x3x9 and still has speed points");
            check(!maxTorch.readClientData(5, 0, 0, 1, 2), "High tier rejects X width above nine despite spare points");
            check(!maxTorch.readClientData(0, 5, 0, 1, 2), "High tier rejects Z width above nine");
            check(!maxTorch.readClientData(0, 0, 2, 1, 2), "High tier rejects height above three");
            CompoundTag oldWideSave = maxTorch.saveWithoutMetadata();
            oldWideSave.putInt("XRange", 20); oldWideSave.putInt("YRange", 12); oldWideSave.putInt("ZRange", 20);
            oldWideSave.putInt("Speed", 3);
            maxTorch.load(oldWideSave);
            check(((BudgetEntity) maxTorch).tiered$getState().equals(new Budget.State(4, 1, 4, 3)),
                    "Old wide save shrinks to 9x3x9 without changing acceleration");
            ((BudgetEntity) maxTorch).tiered$setState(new Budget.State(5, 2, 5, 1));
            TorcherinoBlockEntity.tick(level, maxPos, maxTorch.getBlockState(), maxTorch);
            check(((BudgetEntity) maxTorch).tiered$getState().equals(new Budget.State(4, 1, 4, 1)),
                    "Tick guard caps old range even when it fits the point budget");
            var nativeBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation("torcherino", "torcherino"));
            BlockPos nativePos = pos.offset(3, 0, 0);
            level.setBlockAndUpdate(nativePos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(nativePos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(nativePos, nativeBlock.defaultBlockState());
            TorcherinoBlockEntity original = (TorcherinoBlockEntity) level.getBlockEntity(nativePos);
            check(original.readClientData(4, 4, 1, 1, 2), "Original Torcherino remains outside tiered allocation policy");
            System.out.println("TIERED_TORCH_SERVER_PASS tiers=14 items=42 forms=4 rangeCap=9x3x9 nativeType=true blacklist=true activeTick=true validPacket=true overBudgetRejected=true nbtSanitized=true tickGuard=true originalUnchanged=true");
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.out.println("TIERED_TORCH_SERVER_FAIL " + failure);
        }
        if (!Boolean.getBoolean("tieredTorch.clientSmoke")) event.getServer().halt(false);
    }
    private void login(PlayerEvent.PlayerLoggedInEvent event) {
        // Manual testers own the screen. Only opt-in automation may open/change a GUI.
        if (Boolean.getBoolean("tieredTorch.autoUi") && event.getEntity() instanceof ServerPlayer player) {
            uiPlayer = player; uiPhase = 1; uiWait = 120;
        }
    }
    private void openFor(ServerPlayer player, Profile profile) {
        ServerLevel level = player.serverLevel();
        int y = level.getMaxBuildHeight() - 12;
        BlockPos platform = new BlockPos(player.getBlockX(), y - 1, player.getBlockZ());
        for (int x = -1; x <= 3; x++) for (int z = -1; z <= 1; z++) {
            BlockPos floor = platform.offset(x, 0, z);
            level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(floor.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(floor.above(2), Blocks.AIR.defaultBlockState());
        }
        player.teleportTo(platform.getX() + 0.5, y, platform.getZ() + 0.5);
        last = platform.above().offset(2, 0, 0);
        level.setBlockAndUpdate(last.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(last, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(last, TieredTorcherino.FLOOR.get(profile).get().defaultBlockState());
        TorcherinoBlockEntity be = (TorcherinoBlockEntity) level.getBlockEntity(last);
        be.setOwner(player.getStringUUID());
        be.openTorcherinoScreen(player);
    }
    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || uiPlayer == null || uiPhase == 5) return;
        if (uiWait > 0) { uiWait--; return; }
        if (uiPhase == 1) { openFor(uiPlayer, Profile.LV); uiPhase = 2; return; }
        if (uiPhase == 3) { openFor(uiPlayer, Profile.MV); uiPhase = 4; return; }
        var blockEntity = uiPlayer.serverLevel().getBlockEntity(last);
        if (!(blockEntity instanceof BudgetEntity access)) return;
        Budget.State state = access.tiered$getState();
        if (uiPhase == 2 && state.equals(new Budget.State(1, 0, 0, 2))) {
            System.out.println("TIERED_TORCH_UI_LV_PERSISTED " + state);
            uiPhase = 3; uiWait = 20;
        } else if (uiPhase == 4 && state.equals(new Budget.State(1, 1, 1, 1))) {
            System.out.println("TIERED_TORCH_UI_PASS LV=2 MV=3 nativeNetworkPersisted=true");
            uiPhase = 5;
        }
    }
    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tieredtorch_smoke").requires(source -> source.hasPermission(2))
                .then(Commands.literal("open").then(Commands.argument("tier", StringArgumentType.word()).executes(context -> {
                    Profile profile = Profile.valueOf(StringArgumentType.getString(context, "tier").toUpperCase(java.util.Locale.ROOT));
                    var player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    last = player.blockPosition().offset(2, 0, 0);
                    level.setBlockAndUpdate(last.below(), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(last, TieredTorcherino.FLOOR.get(profile).get().defaultBlockState());
                    ((TorcherinoBlockEntity) level.getBlockEntity(last)).openTorcherinoScreen(player);
                    return 1;
                })))
                .then(Commands.literal("inspect").executes(context -> {
                    var be = context.getSource().getLevel().getBlockEntity(last);
                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            ((BudgetEntity) be).tiered$getState().toString()), false);
                    return 1;
                })));
    }
}
