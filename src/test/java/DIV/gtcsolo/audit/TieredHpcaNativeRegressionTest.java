package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.hpca.HpcaTierSpec;
import DIV.gtcsolo.machine.hpca.TieredHpcaActiveCoolerMachine;
import DIV.gtcsolo.machine.hpca.TieredHpcaComputationMachine;
import DIV.gtcsolo.machine.hpca.TieredHpcaEmptyMachine;
import com.gregtechceu.gtceu.api.capability.IHPCAComponentHatch;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.HPCAMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.hpca.HPCABridgePartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.hpca.HPCACoolerPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.hpca.HPCAComputationPartMachine;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.managed.MultiManagedStorage;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Runs real GT 1.6.4 grid allocation, cooling and power logic with the new machines.
 * No Forge world or machine registration is simulated. Water is only a stand-in
 * for the PCB fluid identity, so the grid can drain a real Forge fluid tank
 * without loading GregTech's material registration.
 */
public final class TieredHpcaNativeRegressionTest {
    public static void main(String[] args) throws Throwable {
        if (!TieredHpcaNativeRegressionTest.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Run this standalone regression with -ea");
        }
        var stderr = System.err;
        try {
            bootstrap();
            checkNode(HpcaTierSpec.UHV, 256, 32, 128, 2_096_640L);
            checkNode(HpcaTierSpec.UEV, 1024, 128, 512, 8_363_520L);
            checkNativeMix();
            System.out.println("Tiered HPCA passed: native allocation and tick reset, full/partial/missing coolant,"
                    + " load-dependent EU, native mixed parts and damage persistence for both tiers.");
        } catch (Throwable failure) {
            failure.printStackTrace(stderr);
            throw failure;
        }
    }

    private static void bootstrap() {
        SharedConstants.tryDetectVersion();
        try { Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0
                    || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) {
                throw failure;
            }
            System.out.println("Standalone fixture: Forge network transformation is not active.");
        }
        TypedPayloadRegistries.init();
        TypedPayloadRegistries.postInit();
    }

    private static void checkNode(HpcaTierSpec tier, int cwu, int heat, int coolant, long maximumEUt) {
        List<IHPCAComponentHatch> parts = new ArrayList<>();
        List<Fixture<TieredHpcaComputationMachine>> cpus = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            var cpu = fixture(holder -> new TieredHpcaComputationMachine(holder, tier));
            cpus.add(cpu);
            parts.add(cpu.machine);
            parts.add(fixture(holder -> new TieredHpcaActiveCoolerMachine(holder, tier)).machine);
        }
        parts.add(fixture(HPCABridgePartMachine::new).machine);
        var grid = new TestGrid();
        grid.onStructureForm(parts);
        assert parts.size() == 9;
        assert grid.hasHPCABridge() && grid.hasActiveCoolers();
        assert grid.getMaxCWUt() == cwu : tier + " CWU";
        assert grid.getMaxCoolingDemand() == heat : tier + " heat";
        assert grid.getMaxCoolingAmount() == heat : tier + " cooling";
        assert grid.getMaxCoolantDemand() == coolant : tier + " coolant demand";
        assert grid.getMaxEUt() == maximumEUt : tier + " maximum power";
        long upkeep = grid.getUpkeepEUt();
        assert upkeep > 0 && upkeep < maximumEUt;
        assert grid.getCurrentEUt() == upkeep;

        assert grid.allocateCWUt(Integer.MAX_VALUE, true) == cwu;
        assert grid.getAllocatedCWUt() == 0 : "simulation must not reserve computation";
        assert grid.allocateCWUt(Integer.MAX_VALUE, false) == cwu;
        assert grid.allocateCWUt(1, false) == 0 : "same-tick requests must share one finite budget";
        assert grid.getCurrentEUt() == maximumEUt;

        FluidTank tank = new FluidTank(coolant * 2);
        fill(tank, coolant);
        assertClose(grid.calculateTemperatureChange(tank, false), 0, tier + " fully cooled");
        assert tank.getFluidAmount() == 0;

        fill(tank, coolant / 2);
        assertClose(grid.calculateTemperatureChange(tank, false), heat / 2.0,
                tier + " half coolant must leave positive heat");
        assert tank.getFluidAmount() == 0;
        assertClose(grid.calculateTemperatureChange(tank, false), heat,
                tier + " depleted coolant must stop cooling");

        grid.tick();
        assert grid.getAllocatedCWUt() == 0 : "native tick must release the allocation budget";
        assert grid.allocateCWUt(cwu / 2, false) == cwu / 2;
        assert grid.getCurrentEUt() == upkeep + (maximumEUt - upkeep) / 2;
        fill(tank, coolant);
        assertClose(grid.calculateTemperatureChange(tank, false), 0, tier + " half load");
        assert tank.getFluidAmount() == coolant / 2 : "coolant must track actual allocated work";

        var damaged = cpus.get(0);
        var intactIcon = damaged.machine.getComponentIcon();
        damaged.machine.setDamaged(true);
        assert damaged.machine.isDamaged();
        assert damaged.machine.getCWUPerTick() == 0;
        assert damaged.machine.getComponentIcon() != intactIcon;
        assert grid.getMaxCWUt() == cwu * 3 / 4 : "native grid must immediately lose the damaged CPU's output";

        CompoundTag saved = new CompoundTag();
        damaged.storage.init();
        damaged.holder.saveManagedPersistentData(saved, false);
        var restored = fixture(holder -> new TieredHpcaComputationMachine(holder, tier));
        restored.storage.init();
        restored.holder.loadManagedPersistentData(saved);
        assert restored.machine.isDamaged() && restored.machine.getCWUPerTick() == 0
                : "inherited native damaged state must survive LDLib's actual save/load path";
        assert restored.machine.getTierSpec() == tier;
        restored.machine.setDamaged(false);
        assert !restored.machine.isDamaged() && restored.machine.getCWUPerTick() == cwu / 4;

        var empty = fixture(holder -> new TieredHpcaEmptyMachine(holder, tier)).machine;
        var cooler = fixture(holder -> new TieredHpcaActiveCoolerMachine(holder, tier)).machine;
        empty.setDamaged(true);
        cooler.setDamaged(true);
        assert !empty.canBeDamaged() && !empty.isDamaged() && empty.getMaxEUt() == 0;
        assert !cooler.canBeDamaged() && !cooler.isDamaged();
    }

    private static void checkNativeMix() {
        // Nine real components: mixed CPU generations, cooler generations, a native bridge and an empty part.
        List<IHPCAComponentHatch> parts = new ArrayList<>();
        parts.add(fixture(holder -> new TieredHpcaComputationMachine(holder, HpcaTierSpec.UHV)).machine);
        parts.add(fixture(holder -> new HPCAComputationPartMachine(holder, true)).machine);
        parts.add(fixture(holder -> new HPCAComputationPartMachine(holder, false)).machine);
        parts.add(fixture(holder -> new TieredHpcaActiveCoolerMachine(holder, HpcaTierSpec.UHV)).machine);
        for (int i = 0; i < 3; i++) parts.add(fixture(holder -> new HPCACoolerPartMachine(holder, true)).machine);
        parts.add(fixture(HPCABridgePartMachine::new).machine);
        parts.add(fixture(holder -> new TieredHpcaEmptyMachine(holder, HpcaTierSpec.UEV)).machine);
        var grid = new TestGrid();
        grid.onStructureForm(parts);
        assert parts.size() == 9;
        assert grid.hasHPCABridge();
        assert grid.getMaxCWUt() == 84;
        assert grid.getMaxCoolingDemand() == 14 && grid.getMaxCoolingAmount() == 14;
        assert grid.getMaxCoolantDemand() == 56;
        assert grid.getMaxEUt() == 706_560L;
        grid.allocateCWUt(84, false);
        FluidTank tank = new FluidTank(56);
        fill(tank, 56);
        assertClose(grid.calculateTemperatureChange(tank, false), 0, "mixed native components");
        assert tank.getFluidAmount() == 0;

        // Native lifecycle reset must not retain an earlier component's budget or bridge.
        grid.onStructureForm(List.of(parts.get(0), parts.get(3)));
        assert grid.getMaxCWUt() == 64 && grid.getAllocatedCWUt() == 0;
        assert !grid.hasHPCABridge();
        assert grid.getMaxCoolingAmount() == 8 && grid.getMaxCoolantDemand() == 32;
    }

    private static void fill(FluidTank tank, int amount) {
        assert tank.fill(new FluidStack(Fluids.WATER, amount), IFluidHandler.FluidAction.EXECUTE) == amount;
    }

    private static void assertClose(double actual, double expected, String message) {
        assert Math.abs(actual - expected) < 0.000001 : message + ": " + actual + " != " + expected;
    }

    private static final class TestGrid extends HPCAMachine.HPCAGridHandler {
        private TestGrid() { super(null); }
        @Override public FluidStack getCoolantStack(int amount) {
            return new FluidStack(Fluids.WATER, amount);
        }
    }

    private static <T extends MetaMachine> Fixture<T> fixture(Function<IMachineBlockEntity, T> factory) {
        var binding = new HolderBinding();
        var holder = (IMachineBlockEntity) Proxy.newProxyInstance(
                IMachineBlockEntity.class.getClassLoader(), new Class<?>[]{IMachineBlockEntity.class}, binding);
        T machine = factory.apply(holder);
        binding.machine = machine;
        return new Fixture<>(machine, holder, binding.storage);
    }

    private record Fixture<T extends MetaMachine>(T machine, IMachineBlockEntity holder, MultiManagedStorage storage) {}

    /** Only supplies a host and storage; all HPCA behavior comes from the actual machine classes. */
    private static final class HolderBinding implements InvocationHandler {
        private final MultiManagedStorage storage = new MultiManagedStorage();
        private final BlockEntity self = new BlockEntity(null, BlockPos.ZERO, Blocks.AIR.defaultBlockState()) {};
        private MetaMachine machine;

        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "getRootStorage" -> storage;
                case "getSelf", "self" -> self;
                case "level" -> null;
                case "pos" -> BlockPos.ZERO;
                case "getMetaMachine" -> machine;
                case "getOffset" -> 0L;
                case "toString" -> "Standalone HPCA holder";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> {
                    if (method.isDefault()) yield InvocationHandler.invokeDefault(proxy, method, args);
                    throw new UnsupportedOperationException("Unexpected holder operation: " + method);
                }
            };
        }
    }
}
