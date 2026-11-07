package DIV.gtcsolo.audit;

import com.google.gson.JsonParser;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

/** Native GT preview of the complete exported array, using vanilla material stand-ins.
 * Does not assert Forge registry availability, hatch formation, rendering, or gameplay.
 */
public final class MegaBlastPatternRegressionTest {
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0
                    || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
            System.out.println("Standalone fixture: Forge network transformation is not active.");
        }
        try (var stream = MegaBlastPatternRegressionTest.class.getResourceAsStream(
                "/data/gtcsolo/multiblocks/mega_alloy_blast_furnace.json.gz")) {
            assert stream != null : "pattern must be packaged";
            var layout = JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(stream), StandardCharsets.UTF_8)).getAsJsonObject();
            var builder = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK);
            for (var section : layout.getAsJsonArray("aisles")) {
                var array = section.getAsJsonArray();
                String[] rows = new String[array.size()];
                for (int y=0; y<rows.length; y++) rows[y] = array.get(y).getAsString();
                builder.aisle(rows);
            }
            for (var material : layout.getAsJsonObject("palette").entrySet()) {
                var predicate = Predicates.blocks(Blocks.IRON_BLOCK);
                if (material.getValue().getAsJsonObject().has("optional")) predicate = predicate.or(Predicates.air());
                builder.where(material.getKey().charAt(0), predicate);
            }
            builder.where('%', Predicates.blocks(Blocks.GOLD_BLOCK))
                    .where('E', Predicates.blocks(Blocks.REDSTONE_BLOCK))
                    .where('K', Predicates.controller(Predicates.blocks(Blocks.DIAMOND_BLOCK)))
                    .where('#', Predicates.any()).where(' ', Predicates.any());
            var pattern = builder.build();
            assert pattern.aisleRepetitions.length == 182;
            assert Arrays.stream(pattern.aisleRepetitions).allMatch(v -> v[0] == 1 && v[1] == 1);
            int[] repetitions = new int[182]; Arrays.fill(repetitions, 1);
            var preview = pattern.getPreview(repetitions);
            assert preview.length == 189 && preview[0].length == 225 && preview[0][0].length == 182;
            int occupied = 0, ports = 0, controllers = 0, coils = 0;
            for (var plane : preview) for (var row : plane) for (var block : row) {
                if (!block.getBlockState().isAir()) occupied++;
                if (block.getBlockState().is(Blocks.GOLD_BLOCK)) ports++;
                if (block.getBlockState().is(Blocks.REDSTONE_BLOCK)) coils++;
                if (block.getBlockState().is(Blocks.DIAMOND_BLOCK)) controllers++;
            }
            assert occupied == 429782 : occupied;
            assert ports == 48 && controllers == 1;
            assert coils == 62797 : coils;
            assert preview[59][6][11].getBlockState().is(Blocks.DIAMOND_BLOCK) : "controller anchor changed";
            System.out.println("PASS: native GT 1.6.4 full-size preview 189x225x182, 429782 non-air cells, 48 service ports, one controller at 59,6,11; vanilla material stand-ins.");
        }
    }
}
