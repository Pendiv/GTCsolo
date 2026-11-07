package DIV.gtcsolo.integration.mekanism.generator;

import mekanism.api.IContentsListener;
import mekanism.api.NBTConstants;
import mekanism.api.Upgrade;
import mekanism.api.math.FloatingLong;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.interfaces.ITypeBlock;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.IUpgradeTile;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/** Integration limited to the original Mekanism gas-burning generator. */
public final class GasGeneratorUpgrades {
    public static final ResourceLocation GENERATOR = new ResourceLocation("mekanismgenerators", "gas_burning_generator");

    private GasGeneratorUpgrades() {}

    public static void init() {
        Block block = ForgeRegistries.BLOCKS.getValue(GENERATOR);
        if (block != null) enableUpgrades(block);
    }

    public static boolean enableUpgrades(Block block) {
        if (!GENERATOR.equals(ForgeRegistries.BLOCKS.getKey(block)) || !(block instanceof ITypeBlock typed)) return false;
        var supported = EnumSet.noneOf(Upgrade.class);
        var previous = typed.getType().get(AttributeUpgradeSupport.class);
        if (previous != null) supported.addAll(previous.supportedUpgrades());
        supported.add(Upgrade.SPEED);
        supported.add(Upgrade.ENERGY);
        typed.getType().add(new AttributeUpgradeSupport(supported));
        return true;
    }

    public static boolean isTarget(TileEntityMekanism tile) {
        return GENERATOR.equals(ForgeRegistries.BLOCKS.getKey(tile.getBlockState().getBlock()));
    }

    public static boolean isTarget(ItemStack stack) {
        return !stack.isEmpty() && GENERATOR.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public static int installed(IUpgradeTile tile, Upgrade upgrade) {
        var component = tile.getComponent();
        return component == null ? 0 : Math.max(0, Math.min(component.getUpgrades(upgrade), upgrade.getMax()));
    }

    public static FloatingLong capacity(IUpgradeTile tile, FloatingLong base) {
        return base.multiply(GasGeneratorUpgradeRules.capacityMultiplier(installed(tile, Upgrade.ENERGY)));
    }

    public static FloatingLong itemCapacity(ItemStack stack, FloatingLong base) {
        int count = 0;
        if (ItemDataUtils.hasData(stack, NBTConstants.COMPONENT_UPGRADE, Tag.TAG_COMPOUND)) {
            count = Upgrade.buildMap(ItemDataUtils.getCompound(stack, NBTConstants.COMPONENT_UPGRADE))
                    .getOrDefault(Upgrade.ENERGY, 0);
        }
        return base.multiply(GasGeneratorUpgradeRules.capacityMultiplier(Math.min(count, Upgrade.ENERGY.getMax())));
    }

    public static BasicEnergyContainer createEnergyContainer(TileEntityMekanism tile, FloatingLong base,
                                                             @Nullable IContentsListener listener) {
        return isTarget(tile) ? new GasGeneratorEnergyContainer(tile, base, listener)
                : BasicEnergyContainer.output(base, listener);
    }

    public static double burnLimit(TileEntityMekanism tile, double nativeLimit) {
        return burnLimit(tile, nativeLimit, FloatingLong.ZERO);
    }

    public static double burnLimit(TileEntityMekanism tile, double nativeLimit, FloatingLong rate) {
        if (!isTarget(tile)) return Math.ceil(nativeLimit);
        var rotor = GasGeneratorRotor.of(tile);
        double power = rotor == null ? 0 : rotor.powerMultiplier();
        long baseline = (long) Math.ceil(Math.ceil(nativeLimit) * power);
        long requested = (long) GasGeneratorUpgradeRules.burnLimit(baseline,
                installed(tile, Upgrade.SPEED), Upgrade.SPEED.getMax());
        var support = GasGeneratorSupportGas.of(tile);
        return support == null ? baseline : support.limit(baseline, requested, rate);
    }

    public static List<Component> upgradeInfo(TileEntityMekanism tile, Upgrade upgrade) {
        if (!isTarget(tile)) return UpgradeUtils.getMultScaledInfo(tile, upgrade);
        double multiplier;
        String key;
        if (upgrade == Upgrade.SPEED) {
            multiplier = GasGeneratorUpgradeRules.speedMultiplier(installed(tile, upgrade), upgrade.getMax());
            key = "gtcsolo.gas_generator.upgrade.speed";
        } else if (upgrade == Upgrade.ENERGY) {
            multiplier = GasGeneratorUpgradeRules.capacityMultiplier(installed(tile, upgrade));
            key = "gtcsolo.gas_generator.upgrade.capacity";
        } else return UpgradeUtils.getMultScaledInfo(tile, upgrade);
        var amount = Component.translatable(key, Math.round(multiplier * 100) / 100F);
        return upgrade == Upgrade.SPEED ? List.of(amount,
                Component.translatable("gtcsolo.gas_generator.support_requirement")) : List.of(amount);
    }
}
