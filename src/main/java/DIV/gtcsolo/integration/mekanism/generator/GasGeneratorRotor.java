package DIV.gtcsolo.integration.mekanism.generator;

import com.gregtechceu.gtceu.common.item.TurbineRotorBehaviour;
import mekanism.api.IContentsListener;
import mekanism.api.math.FloatingLong;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.ITileComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** The replaceable rotor uses GregTech's material properties, part damage and breakage. */
public final class GasGeneratorRotor implements ITileComponent {
    public static final String SAVE_KEY = "gtcsolo_rotor";
    // Ultimet at normal speed: ceil(256 * 1.6) * 1.3 * 0.48 / 256 = 0.999375 of native output.
    public static final int BASE_GENERATION_PERCENT = 48;
    private final BasicInventorySlot slot;
    private final IContentsListener listener;
    private int workingTicks;

    public GasGeneratorRotor(TileEntityMekanism tile, IContentsListener listener) {
        this.listener = listener;
        slot = new RotorSlot(listener);
        tile.addComponent(this);
    }

    public static @Nullable GasGeneratorRotor of(TileEntityMekanism tile) {
        return tile instanceof RotorDrivenGenerator generator ? generator.gtcsolo$getRotor() : null;
    }

    public BasicInventorySlot slot() { return slot; }

    public static boolean isUsable(ItemStack stack) {
        var behavior = TurbineRotorBehaviour.getBehaviour(stack);
        return !stack.isEmpty() && behavior != null && behavior.getRotorPower(stack) > 0
                && behavior.getRotorEfficiency(stack) > 0 && behavior.getPartMaxDurability(stack) > 0
                && behavior.getPartDamage(stack) < behavior.getPartMaxDurability(stack);
    }

    public boolean isReady() { return isUsable(slot.getStack()); }

    public double powerMultiplier() {
        if (!isReady()) return 0;
        ItemStack stack = slot.getStack();
        return TurbineRotorBehaviour.getBehaviour(stack).getRotorPower(stack) / 100.0;
    }

    public FloatingLong generationRate(FloatingLong fuelRate) {
        if (!isReady()) return FloatingLong.ZERO;
        ItemStack stack = slot.getStack();
        int efficiency = TurbineRotorBehaviour.getBehaviour(stack).getRotorEfficiency(stack);
        var multiplier = FloatingLong.create((long) BASE_GENERATION_PERCENT * efficiency).divide(10_000);
        return fuelRate.multiply(multiplier);
    }

    /** One point per 20 productive ticks, matching a maintained native turbine's continuous wear. */
    public void onBurn() {
        if (!isReady()) return;
        if (++workingTicks >= 20) {
            workingTicks = 0;
            ItemStack rotor = slot.getStack().copy();
            TurbineRotorBehaviour.getBehaviour(rotor).applyRotorDamage(rotor, 1);
            slot.setStack(rotor);
        }
        listener.onContentsChanged();
    }

    @Override public void read(CompoundTag data) {
        workingTicks = Math.max(0, Math.min(19, data.getCompound(SAVE_KEY).getInt("working_ticks")));
    }

    @Override public void write(CompoundTag data) {
        CompoundTag saved = new CompoundTag();
        saved.putInt("working_ticks", workingTicks);
        data.put(SAVE_KEY, saved);
    }

    private static final class RotorSlot extends BasicInventorySlot {
        private RotorSlot(IContentsListener listener) {
            super(1, alwaysTrueBi, alwaysTrueBi, GasGeneratorRotor::isUsable, listener, 77, 35);
        }
    }
}
