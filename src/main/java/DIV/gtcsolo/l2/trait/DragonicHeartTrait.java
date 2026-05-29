package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.util.L2EntityUtil;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * [07] Dragonic Heart — 周囲にエンドクリスタル生成、 全て破壊されるまで本体無敵。
 *
 * <p>クリスタル親子リンク: 各 EndCrystal の {@code persistentData} に
 *  {@code gtcsolo.dragonic_parent} (UUID) を書き込み、 EntityLeaveLevelEvent で
 *  「親 mob の crystals カウントを減算」 する形で寿命連動。
 *
 * <p>仕様 (2026-05-18 改修):
 * <ul>
 *   <li>配置半径 = 8 (= 旧 6 から +2)、 発光 tag 付与</li>
 *   <li>owner が unload/discard → 紐づくクリスタル全消去 (= リーシュ)</li>
 *   <li>{@link #REPOSITION_INTERVAL_TICKS} (= 300t) ごとに生存クリスタルを再配置</li>
 *   <li>配置先がブロック内なら半径 {@link #SAFE_SEARCH_RADIUS} ブロックで空間を探索</li>
 * </ul>
 */
public class DragonicHeartTrait extends MobTrait {

    public static final String NBT_PARENT_KEY = "gtcsolo.dragonic_parent";

    /** クリスタル残存中の被ダメ軽減率 (= 0.9 = 90% カット、 残ダメ 10%)。 旧仕様は完全無敵 */
    private static final float DAMAGE_REDUCTION = 0.9f;
    /** 攻撃力 buff (= +(25 + 25n)%、 MULTIPLY_BASE)。 常時付与 (= クリスタルと独立) */
    private static final UUID MOD_DRAGONIC_ATK = UUID.fromString("f4a7c6e5-6f90-91c2-ef54-6f7081920314");

    /** 親 UUID → 残存クリスタル UUID 集合 (= サーバ起動中のキャッシュ、 永続は cap.remaining のみ) */
    private static final Map<UUID, Set<UUID>> LIVE_CRYSTALS = new ConcurrentHashMap<>();
    /**
     * Pending owner-discard queue。 EntityLeaveLevelEvent は
     * {@code PersistentEntitySectionManager.updateChunkStatus} 内の section.forEach から呼ばれるため、
     * その場で {@code ec.discard()} すると同 section の ArrayList を iteration 中に modify → CME。
     * よって owner UUID を queue に積み、 次の ServerTickEvent で safe に discard する。
     */
    private static final Deque<UUID> PENDING_OWNER_DISCARD = new ConcurrentLinkedDeque<>();

    private static final int CRYSTAL_COUNT = 4;
    private static final double CRYSTAL_RADIUS = 8.0;
    private static final long REPOSITION_INTERVAL_TICKS = 300L;
    private static final int SAFE_SEARCH_RADIUS = 10;
    /** 200t 毎に owner 生存を検査 — leave event 取りこぼし対策の safety net */
    private static final long OWNER_VALIDATE_INTERVAL_TICKS = 200L;

    public DragonicHeartTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        if (mob.level().isClientSide()) return;
        // 攻撃力 +50% (= spawned チェック前、 reload でも再付与、 多重は uuid で防ぐ)
        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null && atk.getModifier(MOD_DRAGONIC_ATK) == null) {
            atk.addPermanentModifier(new AttributeModifier(
                    MOD_DRAGONIC_ATK, "gtcsolo.dragonic_heart.attack", 0.25 + 0.25 * lv,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
        if (!MobTraitCap.HOLDER.isProper(mob)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (data.spawned) return;
        data.spawned = true;

        Level world = mob.level();
        int count = CRYSTAL_COUNT;  // level 非依存で固定 4 個
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double bx = mob.getX() + Math.cos(angle) * CRYSTAL_RADIUS;
            double by = mob.getY() + 1;
            double bz = mob.getZ() + Math.sin(angle) * CRYSTAL_RADIUS;
            BlockPos safe = findSafeSpot(world, bx, by, bz);
            EndCrystal crystal = new EndCrystal(world, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
            crystal.setShowBottom(false);
            crystal.setGlowingTag(true);
            CompoundTag tag = crystal.getPersistentData();
            tag.putUUID(NBT_PARENT_KEY, mob.getUUID());
            world.addFreshEntity(crystal);
            ids.add(crystal.getUUID());
        }
        data.remaining = count;
        LIVE_CRYSTALS.put(mob.getUUID(), ids);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (!MobTraitCap.HOLDER.isProper(mob)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        // クリスタルがある限り毎秒 maxHP 0.5% を回復
        if (data.remaining > 0 && mob.tickCount % 20 == 0 && mob.getHealth() < mob.getMaxHealth()) {
            mob.heal(mob.getMaxHealth() * 0.005f);
        }
        if (mob.level().getGameTime() % REPOSITION_INTERVAL_TICKS != 0) return;
        if (data.remaining <= 0) return;
        if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;

        Set<UUID> ids = LIVE_CRYSTALS.get(mob.getUUID());
        if (ids == null || ids.isEmpty()) return;

        // 現在 loaded chunk に居る crystal だけ集める。 sl.getEntity(uuid)==null は
        // 「死亡」 ではなく 「unload 中」 の可能性があるので UUID を tracking から外さない
        // (= 旧コードでは unload を死亡と誤判定して LIVE_CRYSTALS から削除 → reload 後に
        //  「あるはずの crystal が見当たらず本体無敵維持」 になっていた)
        java.util.List<EndCrystal> alive = new java.util.ArrayList<>();
        for (UUID cid : ids) {
            Entity e = sl.getEntity(cid);
            if (e instanceof EndCrystal ec && ec.isAlive()) {
                alive.add(ec);
            }
        }
        if (alive.isEmpty()) return;

        // 全体に微小オフセット回転を与えて再配置
        int n = alive.size();
        double rotOffset = mob.getRandom().nextDouble() * 2 * Math.PI;
        for (int i = 0; i < n; i++) {
            double angle = (2 * Math.PI * i) / n + rotOffset;
            double bx = mob.getX() + Math.cos(angle) * CRYSTAL_RADIUS;
            double by = mob.getY() + 1;
            double bz = mob.getZ() + Math.sin(angle) * CRYSTAL_RADIUS;
            BlockPos safe = findSafeSpot(mob.level(), bx, by, bz);
            EndCrystal ec = alive.get(i);
            ec.moveTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
        }
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        // /kill (= GENERIC_KILL / BYPASSES_INVULNERABILITY) は無敵を貫通させる (= 管理用コマンド優先)
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        // クリスタルが残ってる間は被弾を cancel
        if (!MobTraitCap.HOLDER.isProper(entity)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (data.remaining > 0) {
            // 旧: 完全無敵 (setCanceled)。 新: 90% 軽減
            event.setAmount(event.getAmount() * (1.0f - DAMAGE_REDUCTION));
        }
    }

    /** クリスタル または owner mob が leave した時の dispatcher (= 既存ハンドラから呼ばれる) */
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        Entity e = event.getEntity();
        if (e instanceof EndCrystal) {
            onCrystalLeave(event);
        } else if (e instanceof LivingEntity le && LIVE_CRYSTALS.containsKey(le.getUUID())) {
            onOwnerLeave(le, event);
        }
    }

    /**
     * EndCrystal が leave した時: 親 mob の remaining を 1 減らす + LIVE_CRYSTALS から除去。
     * <p>{@code RemovalReason} が KILLED/DISCARDED の時だけ「死亡」 と扱う。
     * UNLOADED_TO_CHUNK / UNLOADED_WITH_PLAYER / CHANGED_DIMENSION は一時退避なので無視
     * (= 旧コードでは全 leave を死亡扱いして、 reposition で別 chunk に飛んだ crystal が
     *   unload された瞬間 「壊された」 と勘違いしていた)。
     */
    public static void onCrystalLeave(EntityLeaveLevelEvent event) {
        Entity e = event.getEntity();
        if (!(e instanceof EndCrystal)) return;
        Entity.RemovalReason reason = e.getRemovalReason();
        if (reason == null) return;
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) return;
        CompoundTag tag = e.getPersistentData();
        if (!tag.hasUUID(NBT_PARENT_KEY)) return;
        UUID parent = tag.getUUID(NBT_PARENT_KEY);
        Set<UUID> ids = LIVE_CRYSTALS.get(parent);
        if (ids != null) ids.remove(e.getUUID());
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        Entity parentE = sl.getEntity(parent);
        if (!(parentE instanceof LivingEntity le)) return;
        if (!MobTraitCap.HOLDER.isProper(le)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(le);
        if (!cap.hasTrait(DIV.gtcsolo.l2.ModL2Traits.DRAGONIC_HEART.get())) return;
        Data data = cap.getOrCreateData(
                DIV.gtcsolo.l2.ModL2Traits.DRAGONIC_HEART.get().getRegistryName(), Data::new);
        data.remaining = Math.max(0, data.remaining - 1);
    }

    /**
     * 毎 server tick で呼ばれる main entry。
     * <ol>
     *   <li>PENDING_OWNER_DISCARD を drain (= EntityLeaveLevelEvent で積まれた owner の crystals を discard)</li>
     *   <li>{@link #OWNER_VALIDATE_INTERVAL_TICKS} ごとに全 owner 生存検査 (= safety net)</li>
     * </ol>
     */
    public static void serverTick(net.minecraft.server.MinecraftServer server) {
        drainPendingOwnerDiscards(server);
        if (server.getTickCount() % OWNER_VALIDATE_INTERVAL_TICKS == 0) {
            validateOwners(server);
        }
    }

    /** EntityLeaveLevelEvent で積まれた owner の crystals を一気に discard */
    private static void drainPendingOwnerDiscards(net.minecraft.server.MinecraftServer server) {
        UUID ownerUUID;
        while ((ownerUUID = PENDING_OWNER_DISCARD.poll()) != null) {
            discardCrystalsFor(ownerUUID, server);
        }
    }

    /** 200t safety net: 全 owner の存在を server 全 dimension で確認、 居なければ enqueue */
    private static void validateOwners(net.minecraft.server.MinecraftServer server) {
        Iterator<Map.Entry<UUID, Set<UUID>>> it = LIVE_CRYSTALS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Set<UUID>> entry = it.next();
            UUID ownerUUID = entry.getKey();
            Entity owner = null;
            for (net.minecraft.server.level.ServerLevel sl : server.getAllLevels()) {
                owner = sl.getEntity(ownerUUID);
                if (owner != null) break;
            }
            if (owner != null && owner.isAlive() && !owner.isRemoved()) continue;
            // owner not present → 同 tick 内で iterating 中だが PENDING に積むだけなので安全
            PENDING_OWNER_DISCARD.add(ownerUUID);
        }
        // PENDING に積まれた分はこの後 drain される (次 tick の serverTick 冒頭)
    }

    /** owner UUID の crystals 全消去 (= server thread かつ section iteration 外で呼ぶこと) */
    private static void discardCrystalsFor(UUID ownerUUID, net.minecraft.server.MinecraftServer server) {
        Set<UUID> ids = LIVE_CRYSTALS.remove(ownerUUID);
        if (ids == null || ids.isEmpty()) return;
        for (UUID cid : ids) {
            for (net.minecraft.server.level.ServerLevel sl : server.getAllLevels()) {
                Entity c = sl.getEntity(cid);
                if (c instanceof EndCrystal ec && ec.isAlive()) {
                    ec.discard();
                    break;
                }
            }
        }
    }

    /**
     * owner mob が unload/discard した時: 即時 discard は CME を起こすため、 queue に積むだけ。
     * 実 discard は次 serverTick の drainPendingOwnerDiscards で行う。
     */
    private static void onOwnerLeave(LivingEntity owner, EntityLeaveLevelEvent event) {
        if (LIVE_CRYSTALS.containsKey(owner.getUUID())) {
            PENDING_OWNER_DISCARD.add(owner.getUUID());
        }
    }

    /** 配置先がブロック内なら、 同心球状に半径 {@link #SAFE_SEARCH_RADIUS} まで探索して空間を見つける */
    private static BlockPos findSafeSpot(Level world, double x, double y, double z) {
        BlockPos start = BlockPos.containing(x, y, z);
        if (isPlaceable(world, start)) return start;
        for (int radius = 1; radius <= SAFE_SEARCH_RADIUS; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        // 殻だけ走査 (= 半径ちょうど)
                        if (Math.abs(dx) != radius && Math.abs(dy) != radius && Math.abs(dz) != radius) continue;
                        BlockPos p = start.offset(dx, dy, dz);
                        if (isPlaceable(world, p)) return p;
                    }
                }
            }
        }
        return start;
    }

    /** クリスタル設置可能 = 自位置と上 1 マスが air (= 1×1×2 の空間) */
    private static boolean isPlaceable(Level world, BlockPos pos) {
        return world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir();
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public boolean spawned = false;
        @SerialClass.SerialField
        public int remaining = 0;
    }
}
