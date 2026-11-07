package DIV.gtcsolo.audit;

import DIV.gtcsolo.buildcopy.CopyLayout;
import DIV.gtcsolo.buildcopy.CopyLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import java.util.*;

/** Actual Minecraft block-state/NBT round trips; no client or world is launched. */
public final class CopyLayoutRegressionTest {
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            // Same narrow standalone harness boundary as audit/2026-10-12/LargeStorageRegression:
            // vanilla registries are ready before Forge's final, transformed network-event hook.
            if (failure.getStackTrace().length == 0
                    || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
            System.out.println("Standalone test: vanilla registries ready; Forge network transformation is not active.");
        }
        var size = new BlockPos(3, 2, 4);
        var anchor = new BlockPos(2, 1, 1);
        int[] indices = new int[24];
        indices[0] = 1;
        indices[23] = 2;
        var contents = new CompoundTag();
        contents.putString("id", "minecraft:chest");
        contents.putString("LootTable", "minecraft:chests/simple_dungeon");
        var source = new CopyLayout("test", size, anchor, Direction.NORTH,
                List.of(Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.CHEST.defaultBlockState()),
                indices, Map.of(23, contents));
        indices[0] = 0;
        contents.putString("LootTable", "changed");
        assert source.state(0).is(Blocks.STONE) : "capture must detach caller arrays";
        assert !source.entityData(23).getString("LootTable").equals("changed") : "capture must detach BE data";
        var read = source.entityData(23);
        read.putString("LootTable", "changed again");
        assert !source.entityData(23).getString("LootTable").equals("changed again");
        var loaded = CopyLayout.load(source.save(true), true);
        assert loaded.size.equals(size) && loaded.anchor.equals(anchor) && loaded.name.equals("test");
        assert loaded.state(0).is(Blocks.STONE) && loaded.state(1).isAir();
        assert loaded.entityData(23).equals(source.entityData(23));
        var preview = source.save(false);
        assert !preview.contains("Entities") : "preview must not transmit inventory/config NBT";
        assert CopyLayout.load(preview, false).entityData(23) == null;
        var origin = new BlockPos(-23, 80, 71);
        for (Rotation rotation : Rotation.values()) {
            Set<BlockPos> targets = new HashSet<>();
            for (int i = 0; i < source.volume(); i++) {
                assert source.index(source.local(i)) == i;
                targets.add(source.target(i, origin, rotation));
            }
            assert targets.size() == 24 : "rotated mapping must be bijective";
            assert source.target(source.index(anchor), origin, rotation).equals(origin);
            assert source.rotationTo(rotation.rotate(Direction.NORTH)) == rotation;
            assert source.rotatedState(23, rotation).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                    == rotation.rotate(Direction.NORTH) : "state facing and geometry must rotate together";
        }
        assert source.target(0, origin, Rotation.CLOCKWISE_90).equals(origin.offset(1, -1, -2));
        assert CopyLayout.checkedVolume(new BlockPos(32, 32, 32)) == 32768;
        rejects(() -> CopyLayout.checkedVolume(new BlockPos(129, 1, 1)));
        rejects(() -> CopyLayout.checkedVolume(new BlockPos(128, 128, 128)));
        rejects(() -> CopyLayout.checkedVolume(BlockPos.ZERO));
        var corrupt = source.save(true);
        corrupt.putIntArray("Blocks", new int[]{0});
        var wrongLength = corrupt;
        rejects(() -> CopyLayout.load(wrongLength, true));
        corrupt = source.save(true);
        corrupt.getList("Palette", 10).getCompound(0).putString("Name", "missingmod:missing_block");
        var missing = corrupt;
        rejects(() -> CopyLayout.load(missing, true));
        var library = new CopyLibrary();
        var id = library.add(source);
        var saved = library.save(new CompoundTag());
        var method = CopyLibrary.class.getDeclaredMethod("load", CompoundTag.class);
        method.setAccessible(true);
        var reloaded = (CopyLibrary) method.invoke(null, saved);
        assert reloaded.get(id).save(true).equals(source.save(true)) : "world library persistence";
        assert reloaded.get(UUID.randomUUID()) == null;
        System.out.println("Copy regressions passed: native block/NBT round trip, detached contents, air, rotation/anchor bijection, preview privacy, limits, missing-block rejection, world-library persistence.");
    }
    private static void rejects(Runnable action) {
        try { action.run(); throw new AssertionError("Invalid copy was accepted"); }
        catch (IllegalArgumentException expected) { }
    }
}
