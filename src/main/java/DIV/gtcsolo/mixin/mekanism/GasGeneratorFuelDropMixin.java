package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorUpgrades;
import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorRotor;
import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorSupportGas;
import DIV.gtcsolo.integration.mekanism.generator.LiquidFuelGenerator;
import mekanism.api.DataHandlerUtils;
import mekanism.api.NBTConstants;
import mekanism.common.block.BlockMekanism;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.ItemDataUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Extend native drops without replacing Mekanism's loot table or other mods' loot changes. */
@Mixin(BlockMekanism.class)
public abstract class GasGeneratorFuelDropMixin {
    @Inject(method = "getDrops", at = @At("RETURN"))
    private void gtcsolo$preserveLiquidFuel(BlockState state, LootParams.Builder params,
                                          CallbackInfoReturnable<List<ItemStack>> callback) {
        var entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(entity instanceof LiquidFuelGenerator fueled) || !(entity instanceof TileEntityMekanism tile)
                || !GasGeneratorUpgrades.isTarget(tile)) return;
        for (ItemStack drop : callback.getReturnValue()) {
            if (!GasGeneratorUpgrades.isTarget(drop)) continue;
            var data = ItemDataUtils.getDataMap(drop);
            data.put(NBTConstants.FLUID_TANKS, DataHandlerUtils.writeContainers(tile.getFluidTanks(null)));
            fueled.gtcsolo$getLiquidFuel().write(data);
            var rotor = GasGeneratorRotor.of(tile);
            if (rotor != null) rotor.write(data);
            var support = GasGeneratorSupportGas.of(tile);
            if (support != null) support.write(data);
        }
    }
}
