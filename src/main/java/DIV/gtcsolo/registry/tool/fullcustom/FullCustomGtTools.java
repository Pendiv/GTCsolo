package DIV.gtcsolo.registry.tool.fullcustom;

import DIV.gtcsolo.Gtcsolo;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.registry.MaterialRegistry;
import com.gregtechceu.gtceu.api.data.tag.TagUtil;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry for arbitrary-ID GT tools that are not generated from a registered Material.
 */
public final class FullCustomGtTools {

    private static final Map<ResourceLocation, RegisteredTool> TOOLS = new LinkedHashMap<>();
    private static final Set<ResourceLocation> CLAIMED_IDS = new HashSet<>();
    private static boolean bootstrapped;

    private FullCustomGtTools() {}

    public static void bootstrapAndRegister() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        // First end-to-end fixture: arbitrary ID/name/color/stats with the exact GT soft-mallet function.
        register(FullCustomGtToolDeclaration.builder(
                new ResourceLocation(Gtcsolo.MODID, "freeform_test_mallet"),
                GTToolType.SOFT_MALLET)
                .name("en_us", "Freeform Test Mallet")
                .name("ja_jp", "自由設計テストマレット")
                .colors(0x30D5C8, 0x8A5CFF)
                .stats(8.0F, 2.0F, 0.0F, 800, 3, 16)
                .addTags(TagUtil.createItemTag("tools/freeform_test_mallets", false))
                .build());

        MaCrystalTools.register();
    }

    public static ItemProviderEntry<IGTTool> register(FullCustomGtToolDeclaration declaration) {
        return register(declaration, declaration.function().constructor);
    }

    public static ItemProviderEntry<IGTTool> register(FullCustomGtToolDeclaration declaration,
                                                     GTToolType.ToolConstructor constructor) {
        ResourceLocation id = declaration.id();
        if (!CLAIMED_IDS.add(id)) {
            throw new IllegalArgumentException("Duplicate full custom GT tool ID: " + id);
        }
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            throw new IllegalArgumentException("Item ID is already registered: " + id);
        }

        FreeformToolMaterial material = new FreeformToolMaterial(declaration);
        GTToolType function = declaration.function();
        GTRegistrate registrate = registrateFor(id);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        ItemProviderEntry<IGTTool> entry = (ItemProviderEntry<IGTTool>) (ItemProviderEntry) registrate
                .item(id.getPath(), properties -> constructor.apply(
                        function,
                        material.getToolTier(),
                        material,
                        declaration.toolDefinition(),
                        properties).asItem())
                .properties(properties -> properties.craftRemainder(Items.AIR))
                .setData(ProviderType.LANG, NonNullBiConsumer.noop())
                .model(NonNullBiConsumer.noop())
                .color(() -> IGTTool::tintColor)
                .register();

        TOOLS.put(id, new RegisteredTool(declaration, material, entry));
        Gtcsolo.LOGGER.info(
                "[FullCustomGTTool] registered {} function={} colors=[#{}, #{}] stats=[speed={}, damage={}, " +
                        "attackSpeed={}, durability={}, harvest={}, enchantability={}] tags={}",
                id,
                function.name,
                hex(declaration.primaryColor()),
                hex(declaration.secondaryColor()),
                declaration.toolProperty().getHarvestSpeed(),
                declaration.toolProperty().getAttackDamage(),
                declaration.toolProperty().getAttackSpeed(),
                declaration.toolProperty().getDurability(),
                declaration.toolProperty().getHarvestLevel(),
                declaration.toolProperty().getEnchantability(),
                declaration.itemTags().stream().map(tag -> tag.location().toString()).toList());
        return entry;
    }

    public static void addToCreativeTab(CreativeModeTab.Output output) {
        // IGTTool#get() initializes durability, default enchantments, behavior NBT, and AoE NBT.
        TOOLS.values().forEach(tool -> output.accept(tool.entry().get().get()));
    }

    static String localizedName(FreeformToolMaterial material) {
        String fallback = material.declaration().fallbackName();
        java.util.concurrent.atomic.AtomicReference<String> result =
                new java.util.concurrent.atomic.AtomicReference<>(fallback);
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> result.set(FullCustomGtToolClientNames.resolve(
                        material.declaration().localizedNames(), fallback)));
        return result.get();
    }

    public static String localizedName(IGTTool tool) {
        if (tool.getMaterial() instanceof FreeformToolMaterial material) {
            return localizedName(material);
        }
        throw new IllegalArgumentException("Not a full custom GT tool: " + tool.asItem());
    }

    public static void injectItemTags(Map<ResourceLocation, List<TagLoader.EntryWithSource>> tagMap) {
        int additions = 0;
        for (RegisteredTool tool : TOOLS.values()) {
            for (TagKey<Item> tag : tool.declaration().itemTags()) {
                TagLoader.EntryWithSource entry = new TagLoader.EntryWithSource(
                        net.minecraft.tags.TagEntry.element(tool.declaration().id()),
                        "gtcsolo full custom GT tool");
                tagMap.computeIfAbsent(tag.location(), ignored -> new ArrayList<>()).add(entry);
                additions++;
            }
        }
        if (additions > 0) {
            Gtcsolo.LOGGER.info("[FullCustomGTTool] injected {} dynamic item-tag membership(s)", additions);
        }
    }

    public static FullCustomGtToolDeclaration declarationFor(IGTTool tool) {
        if (tool.getMaterial() instanceof FreeformToolMaterial material) {
            return material.declaration();
        }
        return null;
    }

    private static GTRegistrate registrateFor(ResourceLocation id) {
        if (!Gtcsolo.MODID.equals(id.getNamespace()) && !"gtceu".equals(id.getNamespace())) {
            throw new IllegalArgumentException("No supported GTRegistrate for namespace: " + id.getNamespace());
        }
        MaterialRegistry registry = GTCEuAPI.materialManager.getRegistry(id.getNamespace());
        return registry.getRegistrate();
    }

    private static String hex(int color) {
        return color == -1 ? "none" : String.format(java.util.Locale.ROOT, "%06X", color & 0xFFFFFF);
    }

    private record RegisteredTool(
            FullCustomGtToolDeclaration declaration,
            FreeformToolMaterial material,
            ItemProviderEntry<IGTTool> entry) {}
}
