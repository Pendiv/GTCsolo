package DIV.gtcsolo.network.wen;

import DIV.gtcsolo.block.wen.WENDataMonitorMenu;
import DIV.gtcsolo.block.wen.WENDataMonitorScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class WENMonitorUpdatePacket {

    final BlockPos pos;
    final String networkId, storedStr, capacityStr;
    final long stored, capacity, inputPerSec, outputPerSec;
    final boolean formed, isOp, crossDim;
    final String[] upgradeNames;
    final List<Long> history;
    final List<WENDataMonitorMenu.NetworkInfo> allNetworks;
    final int storageLevel, nextUpgradeCost;

    public WENMonitorUpdatePacket(BlockPos pos, String networkId, long stored, long capacity,
                                   boolean formed, long inputPerSec, long outputPerSec,
                                   List<Long> history, List<WENDataMonitorMenu.NetworkInfo> allNetworks,
                                   boolean isOp, boolean crossDim, String[] upgradeNames,
                                   String storedStr, String capacityStr,
                                   int storageLevel, int nextUpgradeCost) {
        this.pos = pos; this.networkId = networkId;
        this.stored = stored; this.capacity = capacity;
        this.formed = formed; this.inputPerSec = inputPerSec; this.outputPerSec = outputPerSec;
        this.history = history; this.allNetworks = allNetworks;
        this.isOp = isOp; this.crossDim = crossDim; this.upgradeNames = upgradeNames;
        this.storedStr = storedStr; this.capacityStr = capacityStr;
        this.storageLevel = storageLevel; this.nextUpgradeCost = nextUpgradeCost;
    }

    public static void encode(WENMonitorUpdatePacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeUtf(p.networkId, 64);
        buf.writeLong(p.stored); buf.writeLong(p.capacity);
        buf.writeBoolean(p.formed);
        buf.writeLong(p.inputPerSec); buf.writeLong(p.outputPerSec);
        buf.writeBoolean(p.isOp); buf.writeBoolean(p.crossDim);
        for (String n : p.upgradeNames) buf.writeUtf(n, 64);
        buf.writeUtf(p.storedStr, 256); buf.writeUtf(p.capacityStr, 256);
        buf.writeVarInt(p.storageLevel); buf.writeVarInt(p.nextUpgradeCost);
        buf.writeVarInt(p.history.size());
        for (long v : p.history) buf.writeLong(v);
        buf.writeVarInt(p.allNetworks.size());
        for (var n : p.allNetworks) {
            buf.writeUtf(n.id(), 64); buf.writeBoolean(n.formed());
            buf.writeLong(n.stored()); buf.writeLong(n.capacity()); buf.writeUtf(n.dimension(), 128);
        }
    }

    public static WENMonitorUpdatePacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String netId = buf.readUtf(64);
        long stored = buf.readLong(); long cap = buf.readLong();
        boolean formed = buf.readBoolean();
        long inps = buf.readLong(); long outps = buf.readLong();
        boolean isOp = buf.readBoolean(); boolean crossDim = buf.readBoolean();
        String[] un = new String[5];
        for (int i = 0; i < 5; i++) un[i] = buf.readUtf(64);
        String ss = buf.readUtf(256); String cs = buf.readUtf(256);
        int sl = buf.readVarInt(); int nuc = buf.readVarInt();
        int hSize = buf.readVarInt();
        List<Long> hist = new ArrayList<>(hSize);
        for (int i = 0; i < hSize; i++) hist.add(buf.readLong());
        int nSize = buf.readVarInt();
        List<WENDataMonitorMenu.NetworkInfo> nets = new ArrayList<>(nSize);
        for (int i = 0; i < nSize; i++) {
            nets.add(new WENDataMonitorMenu.NetworkInfo(
                    buf.readUtf(64), buf.readBoolean(), buf.readLong(), buf.readLong(), buf.readUtf(128)));
        }
        return new WENMonitorUpdatePacket(pos, netId, stored, cap, formed, inps, outps,
                hist, nets, isOp, crossDim, un, ss, cs, sl, nuc);
    }

    public static void handle(WENMonitorUpdatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(pkt)));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(WENMonitorUpdatePacket p) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof WENDataMonitorScreen screen
                && screen.getMenu().getMonitorPos().equals(p.pos)) {
            screen.getMenu().updateData(p.networkId, p.stored, p.capacity, p.formed,
                    p.inputPerSec, p.outputPerSec, p.history, p.allNetworks, p.isOp,
                    p.crossDim, p.upgradeNames, p.storedStr, p.capacityStr,
                    p.storageLevel, p.nextUpgradeCost);
        }
    }
}
