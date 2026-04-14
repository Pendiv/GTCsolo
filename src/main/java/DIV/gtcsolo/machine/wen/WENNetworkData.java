package DIV.gtcsolo.machine.wen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;

/**
 * WEN ネットワークIDのグローバルレジストリ。
 * エネルギーはBigIntegerで一元管理（上限なし）。
 */
public class WENNetworkData extends SavedData {

    private static final String DATA_NAME = "gtcsolo_wen_networks";
    private static final Pattern VALID_ID = Pattern.compile("[a-zA-Z0-9]{1,32}");
    public static final int HISTORY_SIZE = 60;

    private final Map<String, WENEntry> networks = new HashMap<>();

    public static class WENEntry {
        public BigInteger storedEnergy;
        public BigInteger maxCapacity;
        public String dimension;
        public final LinkedList<BigInteger> energyHistory = new LinkedList<>();
        public long inputThisTick = 0;
        public long outputThisTick = 0;
        public BigInteger totalInput = BigInteger.ZERO;
        public BigInteger totalOutput = BigInteger.ZERO;
        public boolean crossDimensionEnabled = false;

        public WENEntry(BigInteger storedEnergy, BigInteger maxCapacity, String dimension) {
            this.storedEnergy = storedEnergy;
            this.maxCapacity = maxCapacity;
            this.dimension = dimension;
        }

        public boolean isFormed() { return maxCapacity.signum() > 0; }
    }

    // =========================================================================
    //  ID管理
    // =========================================================================

    public static boolean isValidId(String id) {
        return id != null && VALID_ID.matcher(id).matches();
    }

    public boolean hasNetwork(String id) { return networks.containsKey(id); }

    public boolean registerNetwork(String id, String dimension) {
        if (!isValidId(id) || networks.containsKey(id)) return false;
        networks.put(id, new WENEntry(BigInteger.ZERO, BigInteger.ZERO, dimension));
        setDirty();
        return true;
    }

    public void removeNetwork(String id) {
        if (networks.remove(id) != null) setDirty();
    }

    public WENEntry getNetwork(String id) { return networks.get(id); }

    public Set<String> getAllNetworkIds() { return networks.keySet(); }

    public List<String> getFormedNetworkIds() {
        List<String> result = new ArrayList<>();
        for (var e : networks.entrySet()) {
            if (e.getValue().isFormed()) result.add(e.getKey());
        }
        Collections.sort(result);
        return result;
    }

    // =========================================================================
    //  エネルギー操作 (long入力 → BigInteger内部)
    // =========================================================================

    public long addEnergy(String id, long amount) {
        WENEntry entry = networks.get(id);
        if (entry == null || amount <= 0 || !entry.isFormed()) return 0;
        BigInteger space = entry.maxCapacity.subtract(entry.storedEnergy);
        if (space.signum() <= 0) return 0;
        BigInteger toAdd = BigInteger.valueOf(amount);
        BigInteger added = toAdd.min(space);
        entry.storedEnergy = entry.storedEnergy.add(added);
        entry.inputThisTick += added.longValueExact() <= Long.MAX_VALUE ? added.longValue() : Long.MAX_VALUE;
        entry.totalInput = entry.totalInput.add(added);
        setDirty();
        return added.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    public long removeEnergy(String id, long amount) {
        WENEntry entry = networks.get(id);
        if (entry == null || amount <= 0) return 0;
        BigInteger toRemove = BigInteger.valueOf(amount);
        BigInteger removed = toRemove.min(entry.storedEnergy);
        entry.storedEnergy = entry.storedEnergy.subtract(removed);
        entry.outputThisTick += removed.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
        entry.totalOutput = entry.totalOutput.add(removed);
        setDirty();
        return removed.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    /** 容量をBigIntegerで設定 */
    public void updateCapacity(String id, BigInteger capacity) {
        WENEntry entry = networks.get(id);
        if (entry == null) return;
        entry.maxCapacity = capacity;
        if (entry.storedEnergy.compareTo(capacity) > 0 && capacity.signum() > 0) {
            entry.storedEnergy = capacity;
        }
        setDirty();
    }

    /** long版の容量更新（後方互換） */
    public void updateCapacity(String id, long capacity) {
        updateCapacity(id, BigInteger.valueOf(capacity));
    }

    /** 管理者用: エネルギーを直接設定 */
    public void setEnergy(String id, long energy) {
        WENEntry entry = networks.get(id);
        if (entry == null) return;
        BigInteger val = BigInteger.valueOf(Math.max(0, energy));
        entry.storedEnergy = val.min(entry.maxCapacity);
        setDirty();
    }

    /** 管理者用: 容量を直接設定 */
    public void setCapacity(String id, long capacity) {
        updateCapacity(id, BigInteger.valueOf(Math.max(0, capacity)));
    }

    // =========================================================================
    //  履歴記録
    // =========================================================================

    public void recordHistory() {
        for (WENEntry entry : networks.values()) {
            entry.energyHistory.addLast(entry.storedEnergy);
            if (entry.energyHistory.size() > HISTORY_SIZE) {
                entry.energyHistory.removeFirst();
            }
            entry.inputThisTick = 0;
            entry.outputThisTick = 0;
        }
    }

    // =========================================================================
    //  SavedData (BigIntegerはbyteArrayで保存)
    // =========================================================================

    public static WENNetworkData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                WENNetworkData::load, WENNetworkData::new, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, WENEntry> e : networks.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", e.getKey());
            entry.putByteArray("stored", e.getValue().storedEnergy.toByteArray());
            entry.putByteArray("capacity", e.getValue().maxCapacity.toByteArray());
            entry.putString("dimension", e.getValue().dimension);
            entry.putByteArray("totalIn", e.getValue().totalInput.toByteArray());
            entry.putByteArray("totalOut", e.getValue().totalOutput.toByteArray());
            entry.putBoolean("crossDim", e.getValue().crossDimensionEnabled);
            list.add(entry);
        }
        tag.put("networks", list);
        return tag;
    }

    public static WENNetworkData load(CompoundTag tag) {
        WENNetworkData data = new WENNetworkData();
        ListTag list = tag.getList("networks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String id = entry.getString("id");
            BigInteger stored = readBigInt(entry, "stored");
            BigInteger capacity = readBigInt(entry, "capacity");
            WENEntry e = new WENEntry(stored, capacity, entry.getString("dimension"));
            e.totalInput = readBigInt(entry, "totalIn");
            e.totalOutput = readBigInt(entry, "totalOut");
            e.crossDimensionEnabled = entry.getBoolean("crossDim");
            data.networks.put(id, e);
        }
        return data;
    }

    /** NBTからBigIntegerを読む（byteArray or long の後方互換） */
    private static BigInteger readBigInt(CompoundTag tag, String key) {
        if (tag.contains(key, Tag.TAG_BYTE_ARRAY)) {
            byte[] bytes = tag.getByteArray(key);
            return bytes.length > 0 ? new BigInteger(bytes) : BigInteger.ZERO;
        }
        if (tag.contains(key, Tag.TAG_LONG)) {
            return BigInteger.valueOf(tag.getLong(key));
        }
        return BigInteger.ZERO;
    }
}
