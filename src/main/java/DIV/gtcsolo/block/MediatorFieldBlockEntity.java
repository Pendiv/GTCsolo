package DIV.gtcsolo.block;

import DIV.gtcsolo.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 媒介野ブロックの BlockEntity。 「元のブロック」「デバフ 1 種」「付与秒数」「CT」を保持する。
 *
 * <p>踏まれたら: CT 中なら無視。 同種デバフ保持中は効果時間を加算、 未保持なら新規付与。
 * いずれも付与後 CT 10 秒。 付与秒数は生成時の特性レベルから焼き込まれる (= 5+5N 秒)。
 */
public class MediatorFieldBlockEntity extends BlockEntity {

    private static final long COOLDOWN_TICKS = 200L;   // CT 10 秒

    private BlockState original = null;
    private String effectId = "";
    private int durationTicks = 100;
    private long ctUntil = 0L;

    public MediatorFieldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEDIATOR_FIELD.get(), pos, state);
    }

    /** 生成時に元ブロック・デバフ・付与時間を焼き込む。 */
    public void setup(BlockState original, MobEffect effect, int durationTicks) {
        this.original = original;
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        this.effectId = id != null ? id.toString() : "";
        this.durationTicks = durationTicks;
        setChanged();
    }

    public BlockState getOriginal() {
        return original;
    }

    /** 踏まれた時の付与処理。 */
    public void trigger(ServerPlayer player) {
        Level lvl = getLevel();
        if (lvl == null) return;
        long now = lvl.getGameTime();
        if (now < ctUntil) return;
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(effectId));
        if (effect == null) return;

        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null) {
            // 同種デバフ保持中 → 効果時間を加算 (amplifier 維持)
            player.forceAddEffect(new MobEffectInstance(effect,
                    existing.getDuration() + durationTicks, existing.getAmplifier()), null);
        } else {
            player.addEffect(new MobEffectInstance(effect, durationTicks, 0));
        }
        ctUntil = now + COOLDOWN_TICKS;
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("original")) {
            original = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("original"));
        }
        effectId = tag.getString("effect");
        durationTicks = tag.getInt("duration");
        ctUntil = tag.getLong("ct_until");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (original != null) tag.put("original", NbtUtils.writeBlockState(original));
        tag.putString("effect", effectId);
        tag.putInt("duration", durationTicks);
        tag.putLong("ct_until", ctUntil);
    }
}
