package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.wen.WENMonitorData;
import DIV.gtcsolo.machine.wen.WENNetworkData;
import DIV.gtcsolo.network.wen.WENAdminPacket;
import DIV.gtcsolo.network.wen.WENMonitorUpdatePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import java.math.BigInteger;
import java.util.*;

/** Persistence, precise monitor values and bounded wire decoding, using the actual network classes. */
public final class WENNetworkRegressionTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void rejects(Runnable action){boolean rejected=false;try{action.run();}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Invalid view accepted");}
    public static void main(String[] args) {
        var data=new WENNetworkData();
        BigInteger big=BigInteger.ONE.shiftLeft(180);
        check(data.registerNetwork("Alpha","minecraft:overworld"),"register");
        data.updateCapacity("Alpha",big.multiply(BigInteger.TWO));data.setEnergy("Alpha",big);
        var sorted=data.getSortedNetworkIds();
        data.addEnergy("Alpha",123);check(sorted==data.getSortedNetworkIds(),"energy update sorted registry again");
        check(data.renameNetwork("Alpha","Beta","minecraft:overworld"),"rename");
        check(data.getNetwork("Beta").storedEnergy.equals(big.add(BigInteger.valueOf(123))),"rename lost energy");
        check(!data.getSortedNetworkIds().contains("Alpha")&&data.getSortedNetworkIds().contains("Beta"),"stale name index");
        check(data.accessibleNetwork("Beta","minecraft:the_nether")==null,"dimension gate");
        data.getNetwork("Beta").crossDimensionEnabled=true;
        check(data.accessibleNetwork("Beta","minecraft:the_nether")!=null,"cross dimension access");
        data.updateDimension("Beta","minecraft:the_nether");data.getNetwork("Beta").crossDimensionEnabled=false;
        check(data.accessibleNetwork("Beta","minecraft:overworld")==null,"relocated dimension");
        data.recordHistory(0);data.recordHistory(1);data.recordHistory(19);
        check(data.getNetwork("Beta").energyHistory.size()==1,"duplicate history point");
        for(int time=20;time<=2000;time+=20)data.recordHistory(time);
        check(data.getNetwork("Beta").energyHistory.size()==60,"history not bounded");
        CompoundTag encoded=data.save(new CompoundTag());var restored=WENNetworkData.load(encoded);
        check(restored.getNetwork("Beta").storedEnergy.equals(data.getNetwork("Beta").storedEnergy),"BigInteger save/load");
        check(restored.getNetwork("Beta").dimension.equals("minecraft:the_nether"),"dimension save/load");
        restored.updateCapacity("Beta",BigInteger.ZERO);
        check(restored.getNetwork("Beta").storedEnergy.equals(data.getNetwork("Beta").storedEnergy),"invalidation lost energy");
        check(restored.accessibleNetwork("Beta","minecraft:the_nether")==null,"inactive access allowed");
        var legacy=new CompoundTag();var list=new ListTag();var entry=new CompoundTag();
        entry.putString("id","Legacy");entry.putString("dimension","minecraft:overworld");entry.putLong("stored",42);entry.putLong("capacity",100);list.add(entry);legacy.put("networks",list);
        check(WENNetworkData.load(legacy).getNetwork("Legacy").storedEnergy.equals(BigInteger.valueOf(42)),"legacy long read");

        var mutable=new ArrayList<BigInteger>();mutable.add(big);mutable.add(big.multiply(BigInteger.TWO));
        var monitor=new WENMonitorData("Beta",big,big.multiply(BigInteger.TWO),true,123,456,mutable,
                List.of(new WENMonitorData.NetworkInfo("Beta",true,big,big.multiply(BigInteger.TWO),"minecraft:the_nether")),
                10,100,true,true,36,74,3);
        mutable.clear();check(monitor.history().size()==2,"mutable history snapshot");
        check(WENMonitorData.fraction(big,big.multiply(BigInteger.TWO))==0.5,"large ratio overflow");
        var packet=new WENMonitorUpdatePacket(57,new BlockPos(3,60,-5),monitor);
        FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{
            WENMonitorUpdatePacket.encode(packet,buffer);int bytes=buffer.readableBytes();
            check(packet.equals(WENMonitorUpdatePacket.decode(buffer)),"monitor packet lost precision");
            check(bytes<8192&&buffer.readableBytes()==0,"unexpected wire size");
        }finally{buffer.release();}
        var admin=new WENAdminPacket(BlockPos.ZERO,WENAdminPacket.Action.SET_CAPACITY,"Beta",big);
        buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{WENAdminPacket.encode(admin,buffer);check(admin.equals(WENAdminPacket.decode(buffer)),"admin value truncated");}finally{buffer.release();}
        var bad=new FriendlyByteBuf(Unpooled.buffer());
        try{bad.writeUtf("-1");rejects(()->WENMonitorUpdatePacket.readNumber(bad));}finally{bad.release();}
        var tooMany=new FriendlyByteBuf(Unpooled.buffer());
        try{
            tooMany.writeUtf("");WENMonitorUpdatePacket.writeNumber(tooMany,BigInteger.ZERO);WENMonitorUpdatePacket.writeNumber(tooMany,BigInteger.ZERO);
            tooMany.writeBoolean(false);tooMany.writeLong(0);tooMany.writeLong(0);tooMany.writeBoolean(false);tooMany.writeBoolean(false);
            tooMany.writeVarInt(0);tooMany.writeVarInt(2);tooMany.writeVarInt(2);tooMany.writeVarInt(0);tooMany.writeVarInt(0);tooMany.writeVarInt(61);
            rejects(()->WENMonitorUpdatePacket.readData(tooMany));
        }finally{tooMany.release();}
        rejects(()->new WENMonitorData("",BigInteger.valueOf(-1),big,true,0,0,List.of(),List.of(),0,0,false,false,0,2,0));
        System.out.println("WEN_NETWORK_PASS persistence=true legacyNbt=true rename=true dimensionalAccess=true historyBound=60 fullPrecision=true boundedPacket=true sortedIndex=true");
    }
}
