package DIV.gtcsolo.audit;

import DIV.gtcsolo.block.EntroniumBlock;
import DIV.gtcsolo.render.material.EntroniumPipeModels;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.pipelike.cable.Insulation;
import com.gregtechceu.gtceu.common.pipelike.fluidpipe.FluidPipeType;
import com.gregtechceu.gtceu.common.pipelike.item.ItemPipeType;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.forgespi.language.IConfigurable;
import net.minecraftforge.forgespi.language.IModInfo;
import java.lang.reflect.Proxy;
import java.util.Optional;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.api.distmarker.Dist;

/** Native block-state and pipe-model contracts, without a game world or GL context. */
public final class EntroniumVisualRegressionTest {
    private static final class FixtureMaterial extends Material {
        private FixtureMaterial(String id) { super(new ResourceLocation(id)); }
    }

    private static final class FixtureContainer extends ModContainer {
        private FixtureContainer() {
            super((IModInfo) Proxy.newProxyInstance(IModInfo.class.getClassLoader(), new Class[]{IModInfo.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getModId" -> "gtceu";
                        case "getConfig" -> Proxy.newProxyInstance(IConfigurable.class.getClassLoader(),
                                new Class[]{IConfigurable.class}, (p, m, a) -> Optional.of("NONE"));
                        default -> throw new UnsupportedOperationException(method.getName());
                    }));
            contextExtension = () -> null;
        }
        @Override public boolean matches(Object object) { return false; }
        @Override public Object getMod() { return this; }
    }

    public static void main(String[] args) throws Throwable {
        var stderr = System.err;
        try { check(); }
        catch (Throwable failure) { failure.printStackTrace(stderr); throw failure; }
    }

    private static void check() throws ReflectiveOperationException {
        // Provide the launch side before FMLEnvironment snapshots it; no Forge launcher is started here.
        var dist = FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        dist.set(null, Dist.DEDICATED_SERVER);
        // GT's registries require an active owner even in a standalone test; no MOD discovery is run.
        ModLoadingContext.get().setActiveContainer(new FixtureContainer());
        SharedConstants.tryDetectVersion();
        try { Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0
                    || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
            System.out.println("Standalone fixture: Forge network transformation is not active.");
        }
        ((MappedRegistry<?>) BuiltInRegistries.BLOCK).unfreeze();
        var material = new FixtureMaterial("gtcsolo:entronium");
        assert EntroniumBlock.matches(material);
        assert !EntroniumBlock.matches(new FixtureMaterial("gtceu:entronium"));
        assert !EntroniumBlock.matches(new FixtureMaterial("gtcsolo:infinity"));
        assert !EntroniumBlock.matches(null);
        for (var prefix : new TagPrefix[]{TagPrefix.block, TagPrefix.frameGt}) {
            var block = new EntroniumBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK), prefix, material);
            block.getStateDefinition().getPossibleStates().forEach(state -> state.initCache());
            var state = block.defaultBlockState();
            assert !state.canOcclude();
            assert state.getLightBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) == 0;
            assert state.propagatesSkylightDown(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            assert state.getShadeBrightness(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) == 1.0f;
            assert !state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            assert !state.isViewBlocking(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            assert !state.isSuffocating(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            for (Direction side : Direction.values()) {
                assert block.skipRendering(state, state, side);
                assert !block.skipRendering(state, Blocks.IRON_BLOCK.defaultBlockState(), side);
            }
        }
        for (var type : FluidPipeType.values()) {
            var model = EntroniumPipeModels.create(type);
            assert model.thickness == type.thickness;
            assert model.sideTexture.get().toString().equals("gtcsolo:block/pipe/entronium/pipe_side");
            assert model.endTexture.get().toString().equals("gtcsolo:block/pipe/entronium/pipe_" + type.name + "_in");
            assert model.sideOverlayTexture == null && model.endOverlayTexture == null;
        }
        for (var type : ItemPipeType.values()) {
            var model = EntroniumPipeModels.create(type);
            assert model.thickness == type.getThickness();
            assert model.endTexture.get().toString().equals("gtcsolo:block/pipe/entronium/pipe_" + type.getSizeForTexture() + "_in");
            assert (model.sideOverlayTexture != null) == type.isRestrictive();
            if (type.isRestrictive()) assert model.sideOverlayTexture.toString().equals("gtceu:block/pipe/pipe_restrictive");
        }
        for (var type : Insulation.values()) {
            var model = EntroniumPipeModels.create(type);
            assert model.thickness == type.thickness;
            assert model.sideTexture.get().toString().equals(type.isCable ?
                    "gtceu:block/cable/insulation_5" : "gtceu:block/material_sets/entronium/wire_side");
            assert model.endTexture.get().toString().equals("gtceu:block/material_sets/entronium/wire_end");
            assert (model.endOverlayTexture != null) == type.isCable;
            if (type.isCable) assert model.endOverlayTexture.toString().equals("gtceu:block/cable/insulation_" + type.insulationLevel);
            assert model.secondarySideTexture == null && model.secondaryEndTexture == null;
        }
        System.out.println("PASS: two transparent native block states; all 25 pipe/wire/cable variants; material isolation; native insulation and restriction overlays.");
    }
}
