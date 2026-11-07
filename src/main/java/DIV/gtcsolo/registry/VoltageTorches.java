package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.torch.VoltageTorchBlock;
import DIV.gtcsolo.block.torch.VoltageTorchBlockEntity;
import DIV.gtcsolo.block.torch.VoltageTorchItem;
import DIV.gtcsolo.block.torch.VoltageTorchTier;
import DIV.gtcsolo.block.torch.VoltageWallTorchBlock;
import DIV.gtcsolo.block.torch.VoltageWardLight;
import DIV.gtcsolo.block.torch.VoltageLanternBlock;
import DIV.gtcsolo.block.torch.VoltageJackOLanternBlock;
import DIV.gtcsolo.block.torch.VoltageLightItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class VoltageTorches {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Gtcsolo.MODID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Gtcsolo.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Gtcsolo.MODID);

    public record Entry(VoltageTorchTier tier, RegistryObject<VoltageTorchBlock> standing,
                        RegistryObject<VoltageWallTorchBlock> wall, RegistryObject<VoltageTorchItem> item) {}

    public static final List<Entry> ENTRIES = registerTiers();
    public record LightEntry(VoltageTorchTier tier, RegistryObject<Block> block, RegistryObject<VoltageLightItem> item) {}
    public static final List<LightEntry> LIGHTS = registerLights();
    public static final RegistryObject<BlockEntityType<VoltageTorchBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("voltage_torch", () -> BlockEntityType.Builder
                    .of(VoltageTorchBlockEntity::new, Stream.concat(ENTRIES.stream()
                            .flatMap(entry -> Stream.of(entry.standing().get(), entry.wall().get())),
                            LIGHTS.stream().map(entry -> entry.block().get()))
                            .toArray(Block[]::new))
                    .build(null));

    private VoltageTorches() {}

    private static List<Entry> registerTiers() {
        List<Entry> entries = new ArrayList<>();
        for (VoltageTorchTier tier : VoltageTorchTier.values()) {
            RegistryObject<VoltageTorchBlock> standing = BLOCKS.register("voltage_torch_" + tier.id(),
                    () -> new VoltageTorchBlock(BlockBehaviour.Properties.copy(Blocks.TORCH)
                            .lightLevel(state -> VoltageTorchTier.LIGHT_LEVEL), tier));
            RegistryObject<VoltageWallTorchBlock> wall = BLOCKS.register("voltage_wall_torch_" + tier.id(),
                    () -> new VoltageWallTorchBlock(BlockBehaviour.Properties.copy(Blocks.WALL_TORCH)
                            .lightLevel(state -> VoltageTorchTier.LIGHT_LEVEL)
                            .dropsLike(standing.get()), tier));
            RegistryObject<VoltageTorchItem> item = ITEMS.register("voltage_torch_" + tier.id(),
                    () -> new VoltageTorchItem(standing.get(), wall.get(), new Item.Properties(), tier));
            entries.add(new Entry(tier, standing, wall, item));
        }
        return List.copyOf(entries);
    }

    private static List<LightEntry> registerLights() {
        List<LightEntry> entries = new ArrayList<>();
        for (VoltageTorchTier tier : VoltageTorchTier.values()) {
            RegistryObject<Block> lantern = BLOCKS.register("voltage_lantern_" + tier.id(),
                    () -> new VoltageLanternBlock(BlockBehaviour.Properties.copy(Blocks.LANTERN)
                            .lightLevel(state -> 15), tier));
            RegistryObject<VoltageLightItem> lanternItem = ITEMS.register("voltage_lantern_" + tier.id(),
                    () -> new VoltageLightItem(lantern.get(), new Item.Properties(), tier));
            entries.add(new LightEntry(tier, lantern, lanternItem));
            RegistryObject<Block> pumpkin = BLOCKS.register("voltage_jack_o_lantern_" + tier.id(),
                    () -> new VoltageJackOLanternBlock(BlockBehaviour.Properties.copy(Blocks.JACK_O_LANTERN)
                            .lightLevel(state -> 15), tier));
            RegistryObject<VoltageLightItem> pumpkinItem = ITEMS.register("voltage_jack_o_lantern_" + tier.id(),
                    () -> new VoltageLightItem(pumpkin.get(), new Item.Properties(), tier));
            entries.add(new LightEntry(tier, pumpkin, pumpkinItem));
        }
        return List.copyOf(entries);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    public static int tier(BlockState state) {
        if (state.getBlock() instanceof VoltageWardLight light) return light.tier().level();
        return 0;
    }

    public static void addToCreativeTab(CreativeModeTab.Output output) {
        ENTRIES.forEach(entry -> output.accept(entry.item().get()));
        LIGHTS.forEach(entry -> output.accept(entry.item().get()));
    }
}
