package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.fusion.FusionVariantSettings;
import com.google.gson.JsonParser;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Actual GT pattern/preview coordinates, with vanilla block stand-ins (not a Forge formation test). */
public final class FusionVariantRegressionTest {
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length==0 || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
            System.out.println("Standalone fixture: Forge network transformation is not active.");
        }
        int compared=0;
        MegaFusionRegressionTest.run();
        MegaDismantleRegressionTest.run();
        assert Arrays.stream(FusionVariantSettings.values()).filter(s->s.family.equals("giant")).count()==1;
        assert FusionVariantSettings.GIANT_MK4.mk==4;
        for (var settings:FusionVariantSettings.values()) {
            var header=settings.pattern().layout();
            if (settings.isMega()) {
                assert header.width==333 && header.height==20 && header.depth==333 && header.radius==156.5;
                assert settings.maximumStartupCapacity()==128_000_000_000L;
                assert settings.maximumEnergyHatches()==128 && settings.minimumEnergyHatches()==32;
                continue; // Its million-plus empty cells are never expanded into a JEI native scene.
            }
            if (settings==FusionVariantSettings.GIANT_MK4)
                assert header.width==45 && header.height==5 && header.depth==45 && header.radius==20;
            try (var input=FusionVariantRegressionTest.class.getResourceAsStream("/data/gtcsolo/multiblocks/fusion/"+settings.id()+".json")) {
                assert input!=null;
                var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
                var builder=FactoryBlockPattern.start(RelativeDirection.RIGHT,RelativeDirection.UP,RelativeDirection.BACK);
                String[][] rows=new String[header.depth][header.height];
                int availableEnergy=0,availableInputs=0;
                for (int z=0;z<header.depth;z++) {
                    for (int y=0;y<header.height;y++) rows[z][y]=json.getAsJsonArray("aisles").get(z).getAsJsonArray().get(y).getAsString();
                    builder.aisle(rows[z]);
                }
                for (int z=0;z<header.depth;z++) for (int y=0;y<header.height;y++) for (int x=0;x<header.width;x++) {
                    char c=rows[z][y].charAt(x);
                    boolean exposed=false;
                    for (var direction:net.minecraft.core.Direction.values()) {
                        int nx=x+direction.getStepX(),ny=y+direction.getStepY(),nz=z+direction.getStepZ();
                        exposed|=nx<0 || ny<0 || nz<0 || nx>=header.width || ny>=header.height || nz>=header.depth
                                || rows[nz][ny].charAt(nx)=='#';
                    }
                    if (exposed && c=='E') availableEnergy++;
                    if (exposed && Character.toUpperCase(c)=='I') availableInputs++;
                }
                assert availableEnergy>=settings.maximumEnergyHatches();
                assert availableInputs>=2;
                for (char c:"CGKEIiOo".toCharArray()) builder.where(c,Predicates.blocks(Blocks.IRON_BLOCK));
                builder.where('S',Predicates.controller(Predicates.blocks(Blocks.DIAMOND_BLOCK)))
                        .where('.',Predicates.any()).where('#',Predicates.any());
                var pattern=builder.build(); int[] repetitions=new int[header.depth]; Arrays.fill(repetitions,1);
                var preview=pattern.getPreview(repetitions);
                assert preview.length==header.width && preview[0].length==header.height && preview[0][0].length==header.depth;
                for (int x=0;x<header.width;x++) for (int y=0;y<header.height;y++) for (int z=0;z<header.depth;z++) {
                    char c=rows[z][y].charAt(x); var state=preview[x][y][z].getBlockState();
                    assert c=='S' ? state.is(Blocks.DIAMOND_BLOCK) : c=='.'||c=='#' ? state.isAir() : state.is(Blocks.IRON_BLOCK);
                    compared++;
                }
                assert preview[header.anchorX][header.anchorY][header.anchorZ].getBlockState().is(Blocks.DIAMOND_BLOCK);
                assert settings.maximumStartupCapacity()==160_000_000L*(1L<<(settings.mk-1));
                assert settings.fusionOverclocking()==(settings.mk<4);
                assert settings.maximumEnergyHatches()==(settings.mk==4?32:16);
                System.out.println("PASS "+settings.id()+" "+header.width+"x"+header.height+"x"+header.depth+"; exterior energy positions="+availableEnergy);
            }
        }
        System.out.println("PASS: "+compared+" native GT preview cells; "+FusionVariantSettings.values().length+" resources, anchors and native capacity limits. Forge registry/formed recipes not exercised.");
    }
}
