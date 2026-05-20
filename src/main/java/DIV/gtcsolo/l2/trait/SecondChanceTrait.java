package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * [37] Second Chance (再チャレンジ) — クリーパー専用。 爆発死後 1 度のみ、 同地点に復活する。
 *
 * <p>state: cap data の triggered フラグ (= 1 体生涯 1 回限り)。
 * <p>排他: 復活系 ([17] Endure / [56] Endless Tale / 本特性) は 1 体に 1 つ推奨。
 * <p>実装: onDeath で triggered=false なら同 type の新個体を spawn (= 親子関係なし、 trait 継承なし)。
 */
public class SecondChanceTrait extends TypedMobTrait {

    public SecondChanceTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    protected void onValidDeath(int level, LivingEntity entity, LivingDeathEvent event) {
        if (entity.level().isClientSide()) return;
        if (!(entity.level() instanceof ServerLevel sl)) return;
        if (!MobTraitCap.HOLDER.isProper(entity)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (data.triggered) return;
        data.triggered = true;

        Entity revived = EntityType.CREEPER.spawn(sl, entity.blockPosition(), MobSpawnType.MOB_SUMMONED);
        if (revived == null) return;
        // L2H natural init は EntityJoinLevelEvent で自動付与される — trait 継承は L2 任せ
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public boolean triggered = false;
    }
}
