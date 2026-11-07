package DIV.gtcsolo.torcherino;

import DIV.gtcsolo.torcherino.mixin.BlockEntityTypeAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.MinecraftForge;
import torcherino.TorcherinoImpl;
import torcherino.api.Tier;
import torcherino.api.TorcherinoAPI;
import torcherino.block.TorcherinoBlock;
import torcherino.block.WallTorcherinoBlock;
import torcherino.block.LanterinoBlock;
import torcherino.block.JackoLanterinoBlock;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

@Mod(TieredTorcherino.ID)
public final class TieredTorcherino {
    public static final String ID = "gtcsolo_torcherino";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final EnumMap<Profile, RegistryObject<? extends Block>> FLOOR = new EnumMap<>(Profile.class);
    public static final EnumMap<Profile, RegistryObject<? extends Block>> WALL = new EnumMap<>(Profile.class);
    public static final EnumMap<Profile, RegistryObject<Item>> TORCHES = new EnumMap<>(Profile.class);
    public static final EnumMap<Profile, RegistryObject<? extends Block>> LANTERNS = new EnumMap<>(Profile.class);
    public static final EnumMap<Profile, RegistryObject<? extends Block>> PUMPKINS = new EnumMap<>(Profile.class);
    public static final EnumMap<Profile, RegistryObject<Item>> LANTERN_ITEMS = new EnumMap<>(Profile.class);
    public static final EnumMap<Profile, RegistryObject<Item>> PUMPKIN_ITEMS = new EnumMap<>(Profile.class);
    static {
        for (Profile profile : Profile.values()) {
            var floor = BLOCKS.register(profile.path(), () -> new TorcherinoBlock(
                    BlockBehaviour.Properties.copy(Blocks.TORCH), profile.id(), ParticleTypes.FLAME));
            var wall = BLOCKS.register("wall_" + profile.path(), () -> new WallTorcherinoBlock(
                    BlockBehaviour.Properties.copy(Blocks.WALL_TORCH), profile.id(), ParticleTypes.FLAME));
            FLOOR.put(profile, floor); WALL.put(profile, wall);
            TORCHES.put(profile, ITEMS.register(profile.path(), () -> new StandingAndWallBlockItem(
                    floor.get(), wall.get(), new Item.Properties(), net.minecraft.core.Direction.DOWN)));
            var lantern = BLOCKS.register(profile.lanternPath(), () -> new LanterinoBlock(
                    BlockBehaviour.Properties.copy(Blocks.LANTERN), profile.id()));
            var pumpkin = BLOCKS.register(profile.pumpkinPath(), () -> new JackoLanterinoBlock(
                    BlockBehaviour.Properties.copy(Blocks.JACK_O_LANTERN), profile.id()));
            LANTERNS.put(profile, lantern); PUMPKINS.put(profile, pumpkin);
            LANTERN_ITEMS.put(profile, ITEMS.register(profile.lanternPath(), () -> new BlockItem(lantern.get(), new Item.Properties())));
            PUMPKIN_ITEMS.put(profile, ITEMS.register(profile.pumpkinPath(), () -> new BlockItem(pumpkin.get(), new Item.Properties())));
        }
        TABS.register("tiered_torcherino", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.gtcsolo_torcherino"))
                .icon(() -> TORCHES.get(Profile.LV).get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    for (Profile profile : Profile.values()) {
                        output.accept(TORCHES.get(profile).get());
                        output.accept(LANTERN_ITEMS.get(profile).get());
                        output.accept(PUMPKIN_ITEMS.get(profile).get());
                    }
                }).build());
    }
    public TieredTorcherino() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus); ITEMS.register(bus); TABS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Settings.SPEC, ID + ".toml");
        bus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(TieredTorcherino::tooltip);
    }
    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            TorcherinoImpl api = (TorcherinoImpl) TorcherinoAPI.INSTANCE;
            for (Profile profile : Profile.values()) {
                int power = Settings.power(profile);
                api.registerTier(profile.id(), power + 1, Budget.MAX_XZ_RADIUS, Budget.MAX_Y_RADIUS);
                Policies.registered(profile, power);
                // Native Torcherino excludes its own IDs, not every TorcherinoBlock.
                // Exclude both new forms too, otherwise a torch can recursively tick itself.
                api.blacklistBlock(FLOOR.get(profile).get());
                api.blacklistBlock(WALL.get(profile).get());
                api.blacklistBlock(LANTERNS.get(profile).get());
                api.blacklistBlock(PUMPKINS.get(profile).get());
            }
            BlockEntityType<?> nativeType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(new ResourceLocation("torcherino", "torcherino"));
            if (nativeType == null) throw new IllegalStateException("Torcherino block entity type missing");
            BlockEntityTypeAccess access = (BlockEntityTypeAccess) nativeType;
            Set<Block> valid = new HashSet<>(access.tiered$getValidBlocks());
            FLOOR.values().forEach(block -> valid.add(block.get()));
            WALL.values().forEach(block -> valid.add(block.get()));
            LANTERNS.values().forEach(block -> valid.add(block.get()));
            PUMPKINS.values().forEach(block -> valid.add(block.get()));
            access.tiered$setValidBlocks(Set.copyOf(valid));
        });
    }
    private static void tooltip(ItemTooltipEvent event) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (itemId == null) return;
        Profile profile = Profile.from(itemId);
        if (profile == null) return;
        Tier tier = TorcherinoAPI.INSTANCE.getTier(profile.id());
        if (tier == null) tier = TorcherinoAPI.INSTANCE.getTiers().get(profile.id());
        int power = tier == null ? profile.defaultPower : tier.maxSpeed() - 1;
        event.getToolTip().add(Component.translatable("gtcsolo_torcherino.power", power));
        event.getToolTip().add(Component.translatable("gtcsolo_torcherino.allocation"));
    }
}
