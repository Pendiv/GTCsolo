package DIV.gtcsolo.registry.genericiteminjection;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.registry.ModItems;
import DIV.gtcsolo.registry.iteminjection.KnownItemInjection;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static DIV.gtcsolo.util.ColorMath.adjustLightness;
import static DIV.gtcsolo.registry.iteminjection.KnownItemInjection.RecolorMethod;
import static DIV.gtcsolo.registry.iteminjection.KnownItemInjection.Stage;

/**
 * Generic counterpart to the GT-owned known-item injection system.
 *
 * <p>The source texture may belong to any loaded Forge mod. Generated items remain under the
 * {@code gtcsolo} namespace, so this system does not claim or mutate another mod's registry space.</p>
 */
public final class GenericItemInjections {

    private static final int PURPLE = 0x9B5DE5;
    private static final int PINK = 0xF34FAE;
    private static final int DEEP_BLUE = 0x183B8F;
    private static final int BLUE = 0x337DFF;
    private static final int RUBY = 0xC52F65;
    private static final int DESATURATED_SILVER = 0xAEB9C8;
    private static final int SOFT_BLACK = 0x494552;
    private static final int GOLD = 0xE7B83F;
    private static final int POLISHED_SILVER = 0xE8EEF7;
    private static final List<Shade> STANDARD_SHADES = List.of(
            new Shade("deep", -0.20F),
            new Shade("dark", -0.10F),
            new Shade("base", 0.0F),
            new Shade("light", 0.10F),
            new Shade("pale", 0.20F));
    private static final List<Shade> WIDE_SHADES = List.of(
            new Shade("extreme_deep", -0.35F),
            new Shade("very_deep", -0.25F),
            new Shade("deep", -0.15F),
            new Shade("dark", -0.075F),
            new Shade("base", 0.0F),
            new Shade("light", 0.075F),
            new Shade("pale", 0.15F),
            new Shade("very_pale", 0.25F),
            new Shade("extreme_pale", 0.35F));

    /**
     * Generic source numbers start at 101 so selections cannot be confused with the GT-only 01-09 range.
     */
    private static final Map<ResourceLocation, Integer> SOURCE_NUMBERS = Map.ofEntries(
            sourceNumber(101, "mekanism", "alloy_atomic"),
            sourceNumber(102, "l2hostility", "miracle_ingot"),
            sourceNumber(103, "l2hostility", "miracle_powder"),
            sourceNumber(104, "evolvedmekanism", "alloy_exoversal"),
            sourceNumber(105, "l2hostility", "hostility_orb"),
            sourceNumber(106, "gtceu", "chocolate_coin"),
            sourceNumber(107, "gtceu", "raw_crystal_chip"),
            sourceNumber(108, "gtceu", "quantum_star"),
            sourceNumber(109, "gtceu", "good_electronic_circuit"),
            sourceNumber(110, "gtceu", "good_integrated_circuit"),
            sourceNumber(111, "gtceu", "micro_processor"),
            sourceNumber(112, "gtceu", "advanced_integrated_circuit"),
            sourceNumber(113, "gtceu", "micro_processor_assembly"),
            sourceNumber(114, "gtceu", "nano_processor"),
            sourceNumber(115, "gtceu", "micro_processor_computer"),
            sourceNumber(116, "gtceu", "nano_processor_assembly"),
            sourceNumber(117, "gtceu", "quantum_processor"),
            sourceNumber(118, "gtceu", "crystal_processor"),
            sourceNumber(119, "gtceu", "micro_processor_mainframe"),
            sourceNumber(120, "gtceu", "nano_processor_computer"),
            sourceNumber(121, "gtceu", "quantum_processor_assembly"),
            sourceNumber(122, "gtceu", "crystal_processor_assembly"),
            sourceNumber(123, "gtceu", "nano_processor_mainframe"),
            sourceNumber(124, "gtceu", "quantum_processor_computer"),
            sourceNumber(125, "gtceu", "wetware_processor"),
            sourceNumber(126, "gtceu", "crystal_processor_computer"),
            sourceNumber(127, "gtceu", "quantum_processor_mainframe"),
            sourceNumber(128, "gtceu", "wetware_processor_assembly"),
            sourceNumber(129, "kubejs", "singularity_processor"),
            sourceNumber(130, "gtceu", "crystal_processor_mainframe"),
            sourceNumber(131, "gtceu", "wetware_processor_computer"),
            sourceNumber(132, "kubejs", "singularity_processor_assembly"),
            sourceNumber(133, "gtceu", "wetware_processor_mainframe"),
            sourceNumber(134, "kubejs", "singularity_processor_computer"),
            sourceNumber(135, "kubejs", "singularity_processor_mainframe"),
            sourceNumber(136, "l2complements", "eternium_pickaxe"));

    private static final List<CircuitSource> MV_PLUS_CIRCUITS = List.of(
            circuit("MV", "gtceu", "good_electronic_circuit", "改良型電子回路"),
            circuit("MV", "gtceu", "good_integrated_circuit", "改良型集積回路"),
            circuit("MV", "gtceu", "micro_processor", "マイクロプロセッサー"),
            circuit("HV", "gtceu", "advanced_integrated_circuit", "発展型集積回路"),
            circuit("HV", "gtceu", "micro_processor_assembly", "マイクロ処理演算装置"),
            circuit("HV", "gtceu", "nano_processor", "ナノプロセッサー"),
            circuit("EV", "gtceu", "micro_processor_computer", "マイクロプロセッサーコンピューター"),
            circuit("EV", "gtceu", "nano_processor_assembly", "ナノ演算処理装置"),
            circuit("EV", "gtceu", "quantum_processor", "量子処理装置"),
            circuit("IV", "gtceu", "crystal_processor", "クリスタル処理装置"),
            circuit("IV", "gtceu", "micro_processor_mainframe", "マイクロプロセッサーメインフレーム"),
            circuit("IV", "gtceu", "nano_processor_computer", "ナノスーパーコンピュータ"),
            circuit("IV", "gtceu", "quantum_processor_assembly", "量子演算処理装置"),
            circuit("LuV", "gtceu", "crystal_processor_assembly", "クリスタル演算処理装置"),
            circuit("LuV", "gtceu", "nano_processor_mainframe", "ナノプロセッサーメインフレーム"),
            circuit("LuV", "gtceu", "quantum_processor_computer", "量子スーパーコンピュータ"),
            circuit("LuV", "gtceu", "wetware_processor", "ウェットウェア処理装置"),
            circuit("ZPM", "gtceu", "crystal_processor_computer", "クリスタルスーパーコンピュータ"),
            circuit("ZPM", "gtceu", "quantum_processor_mainframe", "量子処理メインフレーム"),
            circuit("ZPM", "gtceu", "wetware_processor_assembly", "ウェットウェア演算装置"),
            circuit("ZPM", "kubejs", "singularity_processor", "シンギュラリティ処理装置"),
            circuit("UV", "gtceu", "crystal_processor_mainframe", "クリスタル処理メインフレーム"),
            circuit("UV", "gtceu", "wetware_processor_computer", "ウェットウェアスーパーコンピュータ"),
            circuit("UV", "kubejs", "singularity_processor_assembly", "シンギュラリティ演算装置"),
            circuit("UHV", "gtceu", "wetware_processor_mainframe", "ウェットウェアメインフレーム"),
            circuit("UHV", "kubejs", "singularity_processor_computer", "シンギュラリティスーパーコンピューター"),
            circuit("UEV", "kubejs", "singularity_processor_mainframe", "シンギュラリティ処理メインフレーム"));

    private static final List<ColorFamily> ETERNIUM_RED_TO_PURPLE = List.of(
            new ColorFamily("red_rose", 0xE84667),
            new ColorFamily("rose", 0xE84183),
            new ColorFamily("hot_pink", 0xDF3D9F),
            new ColorFamily("magenta", 0xD43FBA),
            new ColorFamily("red_orchid", 0xC648D1),
            new ColorFamily("orchid", 0xB34BE0),
            new ColorFamily("violet", 0xA04DE8));

    private static final List<RegisteredInjection> REGISTERED = new ArrayList<>();
    private static final Set<ResourceLocation> IDS = new HashSet<>();
    private static final Map<ResourceLocation, Integer> SOURCE_COUNTERS = new java.util.LinkedHashMap<>();
    private static final Map<Item, GenericItemInjection> DECLARATION_BY_ITEM =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static boolean bootstrapped;

    private GenericItemInjections() {}

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        // Minimal cross-mod proof: Mekanism source PNG + animation metadata -> gtcsolo-owned items.
        registerPrototypeFamily(
                "purple",
                new ResourceLocation("mekanism", "alloy_atomic"),
                "原子合金",
                PURPLE,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));

        registerAllMethodsFamily(
                "pink",
                new ResourceLocation("l2hostility", "miracle_ingot"),
                "Miracle Ingot",
                PINK,
                STANDARD_SHADES);
        registerAllMethodsFamily(
                "deep_blue",
                new ResourceLocation("l2hostility", "miracle_ingot"),
                "Miracle Ingot",
                DEEP_BLUE,
                STANDARD_SHADES);
        registerAllMethodsFamily(
                "desaturated_silver",
                new ResourceLocation("l2hostility", "miracle_ingot"),
                "Miracle Ingot",
                DESATURATED_SILVER,
                STANDARD_SHADES);

        registerAllMethodsFamily(
                "pink",
                new ResourceLocation("l2hostility", "miracle_powder"),
                "Miracle Powder",
                PINK,
                STANDARD_SHADES);
        registerAllMethodsFamily(
                "deep_blue",
                new ResourceLocation("l2hostility", "miracle_powder"),
                "Miracle Powder",
                DEEP_BLUE,
                STANDARD_SHADES);
        registerAllMethodsFamily(
                "desaturated_silver",
                new ResourceLocation("l2hostility", "miracle_powder"),
                "Miracle Powder",
                DESATURATED_SILVER,
                STANDARD_SHADES);

        ResourceLocation exoversalAlloy =
                new ResourceLocation("evolvedmekanism", "alloy_exoversal");
        registerAllMethodsFamily(
                "blue", exoversalAlloy, "超次元合金", BLUE, STANDARD_SHADES);
        registerAllMethodsFamily(
                "ruby", exoversalAlloy, "超次元合金", RUBY, STANDARD_SHADES);
        registerAllMethodsFamily(
                "purple", exoversalAlloy, "超次元合金", PURPLE, STANDARD_SHADES);

        registerAllMethodsFamily(
                "blue",
                new ResourceLocation("l2hostility", "hostility_orb"),
                "Hostility Orb",
                BLUE,
                STANDARD_SHADES);

        ResourceLocation chocolateCoin = new ResourceLocation("gtceu", "chocolate_coin");
        registerAllMethodsFamily(
                "soft_black", chocolateCoin, "チョコレートコイン", SOFT_BLACK, STANDARD_SHADES);
        registerAllMethodsFamily(
                "gold", chocolateCoin, "チョコレートコイン", GOLD, STANDARD_SHADES);

        registerAllMethodsFamily(
                "blue",
                new ResourceLocation("gtceu", "raw_crystal_chip"),
                "粗クリスタルチップ",
                BLUE,
                STANDARD_SHADES);

        ResourceLocation quantumStar = new ResourceLocation("gtceu", "quantum_star");
        registerAllMethodsFamily(
                "gold", quantumStar, "クアンタムスター", GOLD, STANDARD_SHADES);
        registerAllMethodsFamily(
                "black", quantumStar, "クアンタムスター", SOFT_BLACK, STANDARD_SHADES);
        registerAllMethodsFamily(
                "polished_silver",
                quantumStar,
                "クアンタムスター",
                POLISHED_SILVER,
                STANDARD_SHADES);
        registerAllMethodsFamily(
                "purple", quantumStar, "クアンタムスター", PURPLE, STANDARD_SHADES);

        for (CircuitSource circuit : MV_PLUS_CIRCUITS) {
            registerAllMethodsFamily(
                    "purple_" + circuit.tier().toLowerCase(Locale.ROOT),
                    circuit.itemId(),
                    circuit.displayName(),
                    PURPLE,
                    WIDE_SHADES);
        }

        ResourceLocation eterniumPickaxe =
                new ResourceLocation("l2complements", "eternium_pickaxe");
        ResourceLocation eterniumPickaxeTexture =
                new ResourceLocation("l2complements", "item/generated/eternium/pickaxe");
        for (ColorFamily color : ETERNIUM_RED_TO_PURPLE) {
            registerPrototypeFamily(
                    color.id(),
                    eterniumPickaxe,
                    eterniumPickaxeTexture,
                    "エターニウムのツルハシ",
                    color.color(),
                    STANDARD_SHADES,
                    EnumSet.allOf(RecolorMethod.class));
        }

        Gtcsolo.LOGGER.info(
                "[GenericItemInjection] registered {} prototype declaration(s) across {} external source item(s)",
                REGISTERED.size(),
                SOURCE_COUNTERS.size());
    }

    public static void registerPrototypeFamily(
            String familyId,
            ResourceLocation sourceItemId,
            String displayBase,
            int baseColor,
            List<Shade> shades,
            Set<RecolorMethod> methods) {
        registerPrototypeFamily(
                familyId,
                sourceItemId,
                inferItemTexture(sourceItemId),
                displayBase,
                baseColor,
                shades,
                methods);
    }

    public static void registerPrototypeFamily(
            String familyId,
            ResourceLocation sourceItemId,
            ResourceLocation sourceTexture,
            String displayBase,
            int baseColor,
            List<Shade> shades,
            Set<RecolorMethod> methods) {
        requirePath(familyId, "familyId");
        if (shades.isEmpty()) {
            throw new IllegalArgumentException("At least one shade is required for " + familyId);
        }
        if (methods.isEmpty()) {
            throw new IllegalArgumentException("At least one recolor method is required for " + familyId);
        }

        int sourceNumber = sourceNumber(sourceItemId);
        for (Shade shade : shades) {
            int targetColor = adjustLightness(baseColor, shade.lightnessOffset());
            for (RecolorMethod method : RecolorMethod.values()) {
                if (!methods.contains(method)) {
                    continue;
                }
                int candidateNumber = SOURCE_COUNTERS.merge(sourceItemId, 1, Integer::sum);
                String suffix = String.format(Locale.ROOT, "%03d", candidateNumber);
                registerVariant(new GenericItemInjection(
                        new ResourceLocation(
                                Gtcsolo.MODID,
                                String.format(Locale.ROOT, "generic_%03d_%s", sourceNumber, suffix)),
                        sourceItemId,
                        sourceTexture,
                        String.format(Locale.ROOT, "%03d_%s", sourceNumber, suffix),
                        String.format(
                                Locale.ROOT,
                                "%03d = %s / %s / %s",
                                sourceNumber,
                                displayBase,
                                familyId,
                                shade.id()),
                        targetColor,
                        method,
                        Stage.PROTOTYPE));
            }
        }
    }

    public static RegistryObject<GenericItemInjectionItem> registerFinishedFromPrototype(
            String idPath,
            String displayName,
            int sourceNumber,
            int candidateNumber) {
        ResourceLocation sourceItemId = sourceItemId(sourceNumber);
        String prototypeId = String.format(
                Locale.ROOT,
                "generic_%03d_%03d",
                sourceNumber,
                candidateNumber);
        GenericItemInjection prototype = REGISTERED.stream()
                .map(RegisteredInjection::declaration)
                .filter(declaration -> declaration.stage() == Stage.PROTOTYPE)
                .filter(declaration -> declaration.id().getPath().equals(prototypeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown generic prototype selection: " +
                                String.format(Locale.ROOT, "%03d_%03d", sourceNumber, candidateNumber)));

        return registerVariant(new GenericItemInjection(
                new ResourceLocation(Gtcsolo.MODID, idPath),
                sourceItemId,
                prototype.sourceTexture(),
                displayName,
                "選定元 " + String.format(Locale.ROOT, "%03d_%03d", sourceNumber, candidateNumber),
                prototype.targetColor(),
                prototype.method(),
                Stage.FINISHED));
    }

    public static RegistryObject<GenericItemInjectionItem> registerVariant(GenericItemInjection declaration) {
        if (!Gtcsolo.MODID.equals(declaration.id().getNamespace())) {
            throw new IllegalArgumentException(
                    "Generic item injections must be owned by gtcsolo: " + declaration.id());
        }
        if (!IDS.add(declaration.id())) {
            throw new IllegalArgumentException("Duplicate generic item injection ID: " + declaration.id());
        }

        RegistryObject<GenericItemInjectionItem> entry = ModItems.ITEMS.register(
                declaration.id().getPath(),
                () -> {
                    GenericItemInjectionItem item =
                            new GenericItemInjectionItem(new Item.Properties().stacksTo(64), declaration);
                    DECLARATION_BY_ITEM.put(item, declaration);
                    return item;
                });
        REGISTERED.add(new RegisteredInjection(declaration, entry));
        return entry;
    }

    public static List<GenericItemInjection> declarations() {
        return REGISTERED.stream().map(RegisteredInjection::declaration).toList();
    }

    public static GenericItemInjection declarationFor(ItemStack stack) {
        return DECLARATION_BY_ITEM.get(stack.getItem());
    }

    public static boolean isInjectionOf(ItemStack stack, ResourceLocation sourceItemId) {
        GenericItemInjection declaration = declarationFor(stack);
        return declaration != null && declaration.sourceItemId().equals(sourceItemId);
    }

    public static void addPrototypesToCreativeTab(CreativeModeTab.Output output) {
        addStageToCreativeTab(Stage.PROTOTYPE, output);
    }

    public static void addFinishedToCreativeTab(CreativeModeTab.Output output) {
        addStageToCreativeTab(Stage.FINISHED, output);
    }

    public static ItemStack prototypeIcon() {
        return iconFor(Stage.PROTOTYPE);
    }

    public static ItemStack finishedIcon() {
        ItemStack finished = iconFor(Stage.FINISHED);
        return finished.isEmpty() ? prototypeIcon() : finished;
    }

    private static void addStageToCreativeTab(Stage stage, CreativeModeTab.Output output) {
        REGISTERED.stream()
                .filter(registered -> registered.declaration().stage() == stage)
                .forEach(registered -> output.accept(registered.entry().get()));
    }

    private static ItemStack iconFor(Stage stage) {
        return REGISTERED.stream()
                .filter(registered -> registered.declaration().stage() == stage)
                .findFirst()
                .map(registered -> new ItemStack(registered.entry().get()))
                .orElseGet(() -> new ItemStack(Items.PAPER));
    }

    private static ResourceLocation inferItemTexture(ResourceLocation sourceItemId) {
        return new ResourceLocation(
                sourceItemId.getNamespace(),
                "item/" + sourceItemId.getPath());
    }

    private static void registerAllMethodsFamily(
            String familyId,
            ResourceLocation sourceItemId,
            String displayBase,
            int baseColor,
            List<Shade> shades) {
        registerPrototypeFamily(
                familyId,
                sourceItemId,
                displayBase,
                baseColor,
                shades,
                EnumSet.allOf(RecolorMethod.class));
    }

    private static Map.Entry<ResourceLocation, Integer> sourceNumber(
            int number,
            String namespace,
            String path) {
        return Map.entry(new ResourceLocation(namespace, path), number);
    }

    private static CircuitSource circuit(
            String tier,
            String namespace,
            String path,
            String displayName) {
        return new CircuitSource(tier, new ResourceLocation(namespace, path), displayName);
    }

    private static int sourceNumber(ResourceLocation sourceItemId) {
        Integer number = SOURCE_NUMBERS.get(sourceItemId);
        if (number == null) {
            throw new IllegalArgumentException(
                    "Assign a stable generic source number before registering family: " + sourceItemId);
        }
        return number;
    }

    private static ResourceLocation sourceItemId(int sourceNumber) {
        return SOURCE_NUMBERS.entrySet().stream()
                .filter(entry -> entry.getValue() == sourceNumber)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown generic source number: " + sourceNumber));
    }

    private static void requirePath(String value, String name) {
        if (value == null || !value.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException(name + " must be a valid registry path: " + value);
        }
    }

    public record Shade(String id, float lightnessOffset) {
        public Shade {
            requirePath(id, "shade id");
            if (!Float.isFinite(lightnessOffset) || lightnessOffset < -1.0F || lightnessOffset > 1.0F) {
                throw new IllegalArgumentException("lightnessOffset must be finite and between -1 and 1");
            }
        }
    }

    private record CircuitSource(
            String tier,
            ResourceLocation itemId,
            String displayName) {}

    private record ColorFamily(String id, int color) {}

    private record RegisteredInjection(
            GenericItemInjection declaration,
            RegistryObject<GenericItemInjectionItem> entry) {}
}
