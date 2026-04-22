package DIV.gtcsolo.integration.mekanism.capability;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * GT MetaMachineBlockEntity に Mekanism chemical capabilities (GAS/INFUSION/PIGMENT/SLURRY HANDLER)
 * を attach する. 対象は ChemicalIOHatchMachine を内包する BE のみ.
 *
 * 旧 ConversionCapabilityAttacher と同じパターンだが、4 種の chemical 型を統一的に扱う.
 */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChemicalCapabilityAttacher {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation ID =
            new ResourceLocation(Gtcsolo.MODID, "chemical_hatch_caps");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof MetaMachineBlockEntity mbe)) return;
        LOGGER.debug("[ChemCap] Attacher: attaching to BE {}",
                mbe.getType() == null ? "?" : mbe.getType().toString());

        event.addCapability(ID, new ICapabilityProvider() {
            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap,
                                                                @Nullable Direction side) {
                MetaMachine machine = mbe.getMetaMachine();
                if (machine instanceof ChemicalIOHatchMachine hatch) {
                    LazyOptional<T> result = hatch.getMekCapability(cap);
                    if (result.isPresent()) {
                        LOGGER.debug("[ChemCap] Attacher: cap HIT cap={} side={} hatchType={}",
                                cap.getName(), side, hatch.getChemType());
                    }
                    return result;
                }
                return LazyOptional.empty();
            }
        });
    }
}