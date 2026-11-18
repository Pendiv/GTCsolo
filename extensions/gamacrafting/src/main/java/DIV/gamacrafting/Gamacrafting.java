package DIV.gamacrafting;

import DIV.gamacrafting.content.crafting.desk.DeskBlock;
import DIV.gamacrafting.content.crafting.desk.DeskBlockEntity;
import DIV.gamacrafting.content.crafting.desk.DeskMenu;
import DIV.gamacrafting.content.crafting.desk.DeskShapedRecipe;
import DIV.gamacrafting.content.crafting.desk.DeskShapelessRecipe;
import DIV.gamacrafting.content.crafting.desk.IDeskRecipe;
import DIV.gamacrafting.network.DeskNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Gamacrafting.MOD_ID)
public final class Gamacrafting {
    public static final String MOD_ID = "gamacrafting";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);

    public static final RegistryObject<DeskBlock> DESK_BLOCK = BLOCKS.register("desk", () -> new DeskBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_PURPLE).sound(SoundType.BONE_BLOCK).strength(1.5F, 0)));
    public static final RegistryObject<BlockItem> DESK_ITEM = ITEMS.register("desk",
            () -> new BlockItem(DESK_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<DeskBlockEntity>> DESK_BLOCK_ENTITY = BLOCK_ENTITIES.register("desk",
            () -> BlockEntityType.Builder.of(DeskBlockEntity::new, DESK_BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<DeskMenu>> DESK_MENU = MENUS.register("desk", () -> IForgeMenuType.create(DeskMenu::new));
    public static final RegistryObject<RecipeType<IDeskRecipe>> DESK_TYPE = RECIPE_TYPES.register("desk",
            () -> RecipeType.simple(id("desk")));
    public static final RegistryObject<DeskShapedRecipe.Serializer> SHAPED_DESK =
            SERIALIZERS.register("shaped_desk", DeskShapedRecipe.Serializer::new);
    public static final RegistryObject<DeskShapelessRecipe.Serializer> SHAPELESS_DESK =
            SERIALIZERS.register("shapeless_desk", DeskShapelessRecipe.Serializer::new);

    public static final RegistryObject<SoundEvent> DESK_POP = sound("desk_pop");
    public static final RegistryObject<SoundEvent> DESK_RESULT = sound("desk_result");
    public static final RegistryObject<SoundEvent> DESK_MINING = sound("desk_mining");
    public static final RegistryObject<SoundEvent> DESK_MINECRAFT_START = sound("desk_minecraft_start");
    public static final RegistryObject<SoundEvent> DESK_MINECRAFT_FLAP = sound("desk_minecraft_flap");
    public static final RegistryObject<SoundEvent> DESK_MINECRAFT_FINISH = sound("desk_minecraft_finish");

    public Gamacrafting() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        RECIPE_TYPES.register(bus);
        SERIALIZERS.register(bus);
        SOUNDS.register(bus);
        bus.addListener(Gamacrafting::addCreativeItem);
        DeskNetwork.register();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    private static RegistryObject<SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(id(path)));
    }

    private static void addCreativeItem(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) event.accept(DESK_ITEM);
    }
}
