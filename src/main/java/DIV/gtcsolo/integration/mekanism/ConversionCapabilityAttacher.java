package DIV.gtcsolo.integration.mekanism;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.machine.ConversionFluidHatchMachine;
import DIV.gtcsolo.machine.ConversionFluidOutputHatchMachine;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * GT MetaMachineBlockEntity に Mekanism の Gas/Infusion Capability を付与する。
 * ConversionFluidHatchMachine を内包している場合のみ有効。
 */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConversionCapabilityAttacher {

    private static final ResourceLocation ID = new ResourceLocation(Gtcsolo.MODID, "conversion_chemical_caps");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof MetaMachineBlockEntity mbe)) return;

        // MetaMachine はまだ初期化されていない可能性がある。
        // 遅延評価で、初回 getCapability 時にマシンを取得する。
        event.addCapability(ID, new ICapabilityProvider() {
            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                MetaMachine machine = mbe.getMetaMachine();

                if (machine instanceof ConversionFluidHatchMachine conv) {
                    if (cap == Capabilities.GAS_HANDLER) return conv.getGasCapability().cast();
                    if (cap == Capabilities.INFUSION_HANDLER) return conv.getInfusionCapability().cast();
                }
                if (machine instanceof ConversionFluidOutputHatchMachine conv) {
                    if (cap == Capabilities.GAS_HANDLER) return conv.getGasCapability().cast();
                    if (cap == Capabilities.INFUSION_HANDLER) return conv.getInfusionCapability().cast();
                }
                return LazyOptional.empty();
            }
        });
    }
}