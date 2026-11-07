package DIV.gtcsolo.audit;

import DIV.gtcsolo.client.performance.MegaWorldPreviewTemplate;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Compares the sparse port sequence against native previews with sites spread over multiple slices. */
public final class MegaWorldPreviewTemplateRegressionTest {
    private static int assertions;

    public static void main(String[] arguments) {
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0
                    || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
            System.out.println("Standalone fixture: Forge network transformation is not active.");
        }
        for (int count : new int[]{1, 2, 43, 48, 67}) for (boolean coil : new boolean[]{false, true}) compare(count, coil);
        rejectUnsupported();
        System.out.println("PASS MegaWorldPreviewTemplateRegressionTest: " + assertions + " assertions");
    }

    private static TraceabilityPredicate ports() {
        return Predicates.blocks(Blocks.STONE)
                .or(Predicates.blocks(Blocks.GOLD_BLOCK).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
                .or(Predicates.blocks(Blocks.IRON_BLOCK).setPreviewCount(1))
                .or(Predicates.blocks(Blocks.COPPER_BLOCK).setPreviewCount(12))
                .or(Predicates.blocks(Blocks.EMERALD_BLOCK).setPreviewCount(9));
    }

    private static void compare(int count, boolean coil) {
        var core = Predicates.controller(Predicates.blocks(Blocks.DIAMOND_BLOCK));
        var port = ports();
        var coilPredicate = Predicates.blocks(Blocks.NETHER_BRICKS, Blocks.OBSIDIAN);
        var template = MegaWorldPreviewTemplate.create(core, port, coil ? coilPredicate : null, count);
        var builder = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK);
        builder.aisle(coil ? "K E" : "K  ", "###");
        for (int index = 0; index < count; index++) builder.aisle(index % 2 == 0 ? " P " : "###", index % 2 == 0 ? "###" : " P ");
        builder.where('K', core).where('P', port).where('#', Predicates.air()).where(' ', Predicates.any());
        if (coil) builder.where('E', coilPredicate);
        var pattern = builder.build();
        int[] repetitions = new int[count + 1]; java.util.Arrays.fill(repetitions, 1);
        var preview = pattern.getPreview(repetitions);
        List<BlockState> expected = new ArrayList<>();
        for (int index = 0; index < count; index++) expected.add(preview[1][index % 2][index + 1].getBlockState());
        require(template.ports().equals(expected), "first native candidate/count/order across slices, count=" + count);
        require(template.ports().size() == count, "one example per actual service site");
        require(template.ports().get(0).is(Blocks.GOLD_BLOCK), "required energy candidate is first");
        if (count >= 24) {
            require(template.ports().stream().filter(state -> state.is(Blocks.GOLD_BLOCK)).count() == 1, "global preview maximum");
            require(template.ports().stream().filter(state -> state.is(Blocks.COPPER_BLOCK)).count() == 12, "fluid input example count");
            require(template.ports().stream().filter(state -> state.is(Blocks.EMERALD_BLOCK)).count() == 9, "fluid output example count");
        }
        require(coil ? template.coil().equals(preview[2][0][0].getBlockState()) : template.coil() == null, "native first coil candidate");
    }

    private static void rejectUnsupported() {
        for (int count : new int[]{0, 513}) {
            boolean failed = false;
            try { MegaWorldPreviewTemplate.create(Predicates.controller(Predicates.blocks(Blocks.DIAMOND_BLOCK)), ports(), null, count); }
            catch (IllegalArgumentException expected) { failed = true; }
            require(failed, "malformed port count bounded");
        }
        boolean failed = false;
        try {
            MegaWorldPreviewTemplate.create(Predicates.controller(Predicates.blocks(Blocks.DIAMOND_BLOCK)),
                    Predicates.blocks(Blocks.IRON_BLOCK).setMinLayerLimited(1), null, 3);
        } catch (IllegalArgumentException expected) { failed = true; }
        require(failed, "future layer-limited port predicates must not silently use a sequence-only template");
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
