package DIV.gtcsolo.machine.mega.dismantle;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTItems;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** Deliberate terminal gesture; bounded, persistent, no-drop removal of matching structural blocks only. */
@Mod.EventBusSubscriber(modid=Gtcsolo.MODID)
public final class MegaDismantlingEvents {
    private static final Set<String> SUPPORTED=Set.of("mega_mixer","ore_processing_factory","mega_alloy_blast_furnace","mega_fusion");
    private static final Map<MinecraftServer,List<Pending>> PREPARING=new IdentityHashMap<>();
    private static final Map<MinecraftServer,Map<UUID,Lease>> LEASES=new IdentityHashMap<>();
    private static final ThreadLocal<Boolean> PROBE=ThreadLocal.withInitial(()->false);
    private static final ThreadLocal<ServerLevel> NO_DROPS=new ThreadLocal<>();
    private static final TicketType<Long> TICKET=TicketType.create("gtcsolo_mega_dismantle",Long::compareTo,40);
    private static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger(MegaDismantlingEvents.class);
    private MegaDismantlingEvents() {}

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void breakCore(BlockEvent.BreakEvent event) {
        if(PROBE.get() || event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !player.getAbilities().mayBuild
                || !(player.getMainHandItem().is(GTItems.TERMINAL.get()) || player.getOffhandItem().is(GTItems.TERMINAL.get()))) return;
        var machine=MetaMachine.getMachine(level,event.getPos());
        if(!(machine instanceof IMultiController controller)) return;
        var id=machine.getDefinition().getId();
        if(!id.getNamespace().equals(Gtcsolo.MODID) || !SUPPORTED.contains(id.getPath())) return;
        var server=level.getServer();var data=MegaDismantleData.get(server);
        var pending=PREPARING.computeIfAbsent(server,key->new ArrayList<>());
        boolean duplicate=pending.stream().anyMatch(p->p.level==level && p.core.equals(event.getPos()))
                || data.jobs.stream().anyMatch(j->j.dimension.equals(level.dimension().location().toString()) && j.core==event.getPos().asLong());
        // The actual deletion happens after a fresh uncancelled protection probe, on the server tick.
        event.setCanceled(true);event.setExpToDrop(0);
        if(duplicate) { message(player,"busy");return; }
        pending.add(new Pending(level,player,controller,event.getPos()));
        message(player,"preparing");
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void noDrops(EntityJoinLevelEvent event) {
        if(event.getLevel()==NO_DROPS.get() && (event.getEntity() instanceof ItemEntity || event.getEntity() instanceof ExperienceOrb))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        var server=event.getServer();var data=MegaDismantleData.get(server);
        long deadline=System.nanoTime()+5_000_000L;
        var pending=PREPARING.get(server);
        if(pending!=null) {
            var iterator=pending.iterator();
            while(iterator.hasNext() && System.nanoTime()<deadline) {
                var task=iterator.next();
                try {
                    DismantleJob ready=task.step(deadline);
                    if(ready!=null) {
                        data.jobs.add(ready);data.setDirty();iterator.remove();
                        message(task.player,"started",ready.positions.length);
                    }
                } catch(RuntimeException failure) {
                    iterator.remove();message(task.player,"aborted");
                    LOGGER.warn("Mega dismantling preparation stopped at {}: {}",task.core,failure.getMessage());
                }
            }
        }
        var iterator=data.jobs.iterator();
        while(iterator.hasNext() && System.nanoTime()<deadline) {
            var job=iterator.next();var player=server.getPlayerList().getPlayer(job.owner);
            var level=server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation(job.dimension)));
            if(player==null || level==null || player.serverLevel()!=level || !player.getAbilities().mayBuild) { release(server,job.id);continue; }
            int cursor=job.cursor;
            job.step(new DismantleJob.Access() {
                public boolean loaded(long position) { return ticket(server,job,level,BlockPos.of(position)); }
                public String block(long position) { return blockId(level.getBlockState(BlockPos.of(position)).getBlock()); }
                public boolean permitted(long position) {
                    BlockPos pos=BlockPos.of(position);
                    var machine=MetaMachine.getMachine(level,pos);
                    if(sharedWithOtherController(machine,BlockPos.of(job.core))) return false;
                    return mayBreak(level,player,pos);
                }
                public boolean remove(long position) {
                    BlockPos pos=BlockPos.of(position);NO_DROPS.set(level);
                    try {
                        var original=level.getBlockState(pos).getBlock();
                        clearLocalContents(level,pos);
                        if(!level.getBlockState(pos).is(original)) return false;
                        // Normal onRemove/onMachineRemoved still run, disconnecting covers and networks.
                        return level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS);
                    } catch(RuntimeException failure) {
                        LOGGER.error("Could not dismantle mega block at {}",pos,failure);return false;
                    } finally { NO_DROPS.remove(); }
                }
            },512,deadline);
            if(job.cursor!=cursor) data.setDirty();
            if(!job.done() && server.getTickCount()%100==0)
                player.displayClientMessage(Component.translatable("gtcsolo.mega_dismantle.progress",job.cursor,job.positions.length),true);
            if(job.done()) {
                release(server,job.id);iterator.remove();data.setDirty();message(player,"finished",job.removed,job.skipped);
            }
        }
    }

    private static boolean mayBreak(ServerLevel level,ServerPlayer player,BlockPos pos) {
        if(!player.getAbilities().mayBuild || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player,pos)) return false;
        boolean previous=PROBE.get();PROBE.set(true);
        try { return !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,level.getBlockState(pos),player)); }
        finally { PROBE.set(previous); }
    }

    private static boolean sharedWithOtherController(MetaMachine machine,BlockPos core) {
        return machine instanceof IMultiPart part && part.getControllers().stream().anyMatch(c->!c.self().getPos().equals(core));
    }

    private static void clearLocalContents(ServerLevel level,BlockPos pos) {
        var machine=MetaMachine.getMachine(level,pos);
        if(machine!=null) for(var trait:machine.getTraits()) if(trait instanceof NotifiableItemStackHandler items)
            items.storage.clear();
        // Never extract through ITEM_HANDLER: it can be a proxy for an external ME/storage network.
        if("minecraft".equals(ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()).getNamespace())
                && level.getBlockEntity(pos) instanceof Container container) container.clearContent();
    }

    private static String blockId(Block block) { return Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(block)).toString(); }
    private static void message(ServerPlayer player,String key,Object... values) {
        player.sendSystemMessage(Component.translatable("gtcsolo.mega_dismantle."+key,values));
    }

    private record Lease(ServerLevel level,ChunkPos chunk,long token) {}
    private static boolean ticket(MinecraftServer server,DismantleJob job,ServerLevel level,BlockPos pos) {
        var map=LEASES.computeIfAbsent(server,key->new HashMap<>());var chunk=new ChunkPos(pos);var old=map.get(job.id);
        if(old!=null && (old.level!=level || !old.chunk.equals(chunk))) release(server,job.id);
        long token=job.id.getLeastSignificantBits();level.getChunkSource().addRegionTicket(TICKET,chunk,0,token);
        map.put(job.id,new Lease(level,chunk,token));return level.hasChunkAt(pos);
    }
    private static void release(MinecraftServer server,UUID id) {
        var map=LEASES.get(server);if(map==null)return;var lease=map.remove(id);
        if(lease!=null) lease.level.getChunkSource().removeRegionTicket(TICKET,lease.chunk,0,lease.token);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        PREPARING.remove(event.getServer());var leases=LEASES.remove(event.getServer());
        if(leases!=null) for(var lease:leases.values()) lease.level.getChunkSource().removeRegionTicket(TICKET,lease.chunk,0,lease.token);
    }

    private static final class Pending {
        final ServerLevel level;final ServerPlayer player;final IMultiController controller;final BlockPos core;
        final Direction front,up;final boolean flipped;
        DismantleShape shape;int cursor;
        final Long2IntOpenHashMap targets=new Long2IntOpenHashMap();
        final Map<Block,Integer> palette=new IdentityHashMap<>();final List<String> names=new ArrayList<>();
        Pending(ServerLevel level,ServerPlayer player,IMultiController controller,BlockPos core) {
            this.level=level;this.player=player;this.controller=controller;this.core=core.immutable();
            front=controller.self().getFrontFacing();up=controller.self().getUpwardsFacing();flipped=controller.self().isFlipped();
        }
        DismantleJob step(long deadline) {
            if(player.isRemoved() || player.serverLevel()!=level || MetaMachine.getMachine(level,core)!=controller.self()
                    || controller.self().getFrontFacing()!=front || controller.self().getUpwardsFacing()!=up || controller.self().isFlipped()!=flipped)
                throw new IllegalStateException("Core moved, rotated or removed");
            if(shape==null) {
                if(!mayBreak(level,player,core)) throw new IllegalStateException("Core is protected");
                shape=new DismantleShape(controller.getPattern());
            }
            for(int scanned=0;cursor<shape.volume() && scanned<32768 && System.nanoTime()<deadline;cursor++,scanned++) {
                var allowed=shape.allowed(cursor);if(allowed.isEmpty())continue;
                var pos=shape.position(cursor,core,front,up,flipped);
                if(!level.hasChunkAt(pos)) throw new IllegalStateException("Required chunk is not loaded; core retained");
                Block block=level.getBlockState(pos).getBlock();if(!allowed.contains(block))continue;
                if(sharedWithOtherController(MetaMachine.getMachine(level,pos),core))continue;
                int material=palette.computeIfAbsent(block,key->{names.add(blockId(key));return names.size()-1;});
                targets.put(pos.asLong(),material);
            }
            if(cursor<shape.volume())return null;
            if(!targets.containsKey(core.asLong())) throw new IllegalStateException("Core is not a structural target");
            long[] positions=targets.keySet().toLongArray();
            LongArrays.quickSort(positions,(a,b)-> {
                if(a==core.asLong())return b==a?0:-1;if(b==core.asLong())return 1;
                long ac=ChunkPos.asLong(BlockPos.getX(a)>>4,BlockPos.getZ(a)>>4),bc=ChunkPos.asLong(BlockPos.getX(b)>>4,BlockPos.getZ(b)>>4);
                int chunk=Long.compare(ac,bc);return chunk==0?Long.compare(a,b):chunk;
            });
            int[] indices=new int[positions.length];for(int i=0;i<positions.length;i++)indices[i]=targets.get(positions[i]);
            return new DismantleJob(UUID.randomUUID(),player.getUUID(),level.dimension().location().toString(),core.asLong(),positions,indices,names);
        }
    }
}
