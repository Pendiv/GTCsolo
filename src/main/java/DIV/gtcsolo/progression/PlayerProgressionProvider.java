package DIV.gtcsolo.progression;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * player へ {@link PlayerProgression} を attach する capability provider。
 * NBT シリアライズ込み (= player データに保存され、 死亡時は {@code PlayerEvent.Clone} で引き継ぐ)。
 */
public class PlayerProgressionProvider implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation ID = new ResourceLocation(Gtcsolo.MODID, "progression");

    private final PlayerProgression data = new PlayerProgression();
    private final LazyOptional<PlayerProgression> opt = LazyOptional.of(() -> data);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ProgressionCapability.PLAYER ? opt.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        opt.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.save();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.load(tag);
    }
}
