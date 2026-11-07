package DIV.gtcsolo.registry.iteminjection;

import DIV.gtcsolo.Gtcsolo;

import com.gregtechceu.gtceu.common.registry.GTRegistration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import java.util.ArrayList;
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
 * Declarative registry for bulk variants made from known GT item textures.
 *
 * <p>One family declaration expands shade steps × recolor methods. Pruning means reducing either list;
 * promoting a selected appearance means registering that concrete variant as {@link Stage#FINISHED}.</p>
 */
public final class KnownItemInjections {

    private static final int PURPLE = 0x9B5DE5;
    private static final int VIVID_PINK = 0xFF3F9F;
    private static final int VIVID_GREEN = 0x35D16F;
    private static final int VIVID_BLUE = 0x3288FF;
    private static final int BLACK_TRACE = 0x302B38;
    private static final int BLUE_VIOLET = 0x625BFF;
    private static final int VIVID_YELLOW = 0xFFD84A;
    private static final Map<ResourceLocation, Integer> SOURCE_NUMBERS = Map.of(
            new ResourceLocation("gtceu", "wetware_processor_assembly"), 1,
            new ResourceLocation("gtceu", "data_stick"), 2,
            new ResourceLocation("gtceu", "lv_voltage_coil"), 3,
            new ResourceLocation("gtceu", "gravitation_engine_unit"), 4,
            new ResourceLocation("gtceu", "blacklight"), 5,
            new ResourceLocation("gtceu", "silicon_boule"), 6,
            new ResourceLocation("gtceu", "silicon_wafer"), 7,
            new ResourceLocation("gtceu", "nano_processor_mainframe"), 8,
            new ResourceLocation("gtceu", "empty_mold"), 9);
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

    private static final List<RegisteredInjection> REGISTERED = new ArrayList<>();
    private static final Set<ResourceLocation> IDS = new HashSet<>();
    private static final Map<ResourceLocation, Integer> SOURCE_COUNTERS = new java.util.LinkedHashMap<>();
    private static final Map<Item, KnownItemInjection> DECLARATION_BY_ITEM =
            java.util.Collections.synchronizedMap(new IdentityHashMap<>());
    private static boolean bootstrapped;

    private KnownItemInjections() {}

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        // Current visual laboratory: 5 purple lightness steps × 5 deterministic methods × 2 source items.
        registerPrototypeFamily(
                "wetware_zpm",
                new ResourceLocation("gtceu", "wetware_processor_assembly"),
                "ウェットウェア演算装置",
                PURPLE,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));
        registerPrototypeFamily(
                "data_stick",
                new ResourceLocation("gtceu", "data_stick"),
                "データスティック",
                PURPLE,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));

        registerPrototypeFamily(
                "pink",
                new ResourceLocation("gtceu", "lv_voltage_coil"),
                "Low Voltage コイル",
                VIVID_PINK,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));

        ResourceLocation gravitationEngine = new ResourceLocation("gtceu", "gravitation_engine_unit");
        registerPrototypeFamily(
                "green",
                gravitationEngine,
                "重力エンジンユニット",
                VIVID_GREEN,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));
        registerPrototypeFamily(
                "blue",
                gravitationEngine,
                "重力エンジンユニット",
                VIVID_BLUE,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));
        registerPrototypeFamily(
                "black_trace",
                gravitationEngine,
                "重力エンジンユニット",
                BLACK_TRACE,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));
        registerPrototypeFamily(
                "blue_violet",
                gravitationEngine,
                "重力エンジンユニット",
                BLUE_VIOLET,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));

        ResourceLocation blacklight = new ResourceLocation("gtceu", "blacklight");
        registerPrototypeFamily(
                "pink",
                blacklight,
                "紫外線",
                VIVID_PINK,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));
        registerPrototypeFamily(
                "blue",
                blacklight,
                "紫外線",
                VIVID_BLUE,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));
        registerPrototypeFamily(
                "yellow",
                blacklight,
                "紫外線",
                VIVID_YELLOW,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));

        registerPrototypeFamily(
                "purple",
                new ResourceLocation("gtceu", "silicon_boule"),
                "シリコン単結晶",
                PURPLE,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));
        registerPrototypeFamily(
                "purple",
                new ResourceLocation("gtceu", "silicon_wafer"),
                "シリコンウェハー",
                PURPLE,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));
        registerPrototypeFamily(
                "purple_wide",
                new ResourceLocation("gtceu", "nano_processor_mainframe"),
                "ナノプロセッサーメインフレーム",
                PURPLE,
                WIDE_SHADES,
                EnumSet.allOf(RecolorMethod.class));
        registerPrototypeFamily(
                "purple",
                new ResourceLocation("gtceu", "empty_mold"),
                "空の金型",
                PURPLE,
                STANDARD_SHADES,
                EnumSet.allOf(RecolorMethod.class));

        Gtcsolo.LOGGER.info(
                "[KnownItemInjection] registered {} numbered prototype declaration(s) across {} source item(s)",
                REGISTERED.size(),
                SOURCE_COUNTERS.size());

        registerFinishedFromPrototype("gtcsolo_selected_001", "名前未定_001", 4, 22);
        registerFinishedFromPrototype("gtcsolo_selected_002", "名前未定_002", 4, 28);
        registerFinishedFromPrototype("gtcsolo_selected_003", "名前未定_003", 4, 32);
        registerFinishedFromPrototype("gtcsolo_selected_004", "名前未定_004", 4, 74);
        registerFinishedFromPrototype("gtcsolo_selected_005", "名前未定_005", 5, 30);
        registerFinishedFromPrototype("gtcsolo_selected_006", "名前未定_006", 7, 4);
        registerFinishedFromPrototype("gtcsolo_selected_007", "名前未定_007", 7, 1);
        registerFinishedFromPrototype("gtcsolo_selected_008", "名前未定_008", 6, 2);
        registerFinishedFromPrototype("gtcsolo_selected_009", "名前未定_009", 6, 6);
        registerFinishedFromPrototype("gtcsolo_selected_010", "名前未定_010", 2, 7);
        registerFinishedFromPrototype("gtcsolo_selected_011", "名前未定_011", 2, 24);
        registerFinishedFromPrototype("gtcsolo_selected_012", "名前未定_012", 1, 10);
        registerFinishedFromPrototype("gtcsolo_selected_013", "名前未定_013", 8, 16);
        registerFinishedFromPrototype("gtcsolo_selected_014", "名前未定_014", 8, 21);
        registerFinishedFromPrototype("gtcsolo_selected_015", "名前未定_015", 8, 26);
        registerFinishedFromPrototype("gtcsolo_selected_016", "名前未定_016", 8, 31);
        registerFinishedFromPrototype("gtcsolo_selected_017", "名前未定_017", 3, 14);

        Gtcsolo.LOGGER.info(
                "[KnownItemInjection] promoted {} selected appearance(s) to finished items",
                REGISTERED.stream()
                        .filter(registered -> registered.declaration().stage() == Stage.FINISHED)
                        .count());
    }

    public static void registerPrototypeFamily(
            String familyId,
            ResourceLocation sourceItemId,
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

        for (Shade shade : shades) {
            int targetColor = adjustLightness(baseColor, shade.lightnessOffset());
            for (RecolorMethod method : RecolorMethod.values()) {
                if (!methods.contains(method)) {
                    continue;
                }
                int number = SOURCE_COUNTERS.merge(sourceItemId, 1, Integer::sum);
                int sourceNumber = sourceNumber(sourceItemId);
                String suffix = String.format(Locale.ROOT, "%03d", number);
                String path = sourceItemId.getPath() + "_" + suffix;
                String displayName = String.format(Locale.ROOT, "%02d_%s", sourceNumber, suffix);
                registerVariant(new KnownItemInjection(
                        new ResourceLocation("gtceu", path),
                        sourceItemId,
                        inferItemTexture(sourceItemId),
                        displayName,
                        String.format(
                                Locale.ROOT,
                                "%02d = %s / %s / %s",
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

    /**
     * Stable finished-item entry point. A selected preview can be reproduced under a permanent ID without
     * retaining the whole prototype family.
     */
    public static ItemEntry<KnownItemInjectionItem> registerFinished(
            String idPath,
            ResourceLocation sourceItemId,
            String displayName,
            int targetColor,
            RecolorMethod method) {
        requirePath(idPath, "idPath");
        return registerVariant(new KnownItemInjection(
                new ResourceLocation("gtceu", idPath),
                sourceItemId,
                inferItemTexture(sourceItemId),
                displayName,
                "Finished",
                targetColor,
                method,
                Stage.FINISHED));
    }

    public static ItemEntry<KnownItemInjectionItem> registerFinishedFromPrototype(
            String idPath,
            String displayName,
            int sourceNumber,
            int candidateNumber) {
        if (candidateNumber < 1 || candidateNumber > 999) {
            throw new IllegalArgumentException("candidateNumber must be between 1 and 999");
        }

        ResourceLocation sourceItemId = sourceItemId(sourceNumber);
        String candidatePath = sourceItemId.getPath() + "_" +
                String.format(Locale.ROOT, "%03d", candidateNumber);
        KnownItemInjection prototype = REGISTERED.stream()
                .map(RegisteredInjection::declaration)
                .filter(declaration -> declaration.stage() == Stage.PROTOTYPE)
                .filter(declaration -> declaration.id().getPath().equals(candidatePath))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown prototype selection: " +
                                String.format(Locale.ROOT, "%02d_%03d", sourceNumber, candidateNumber)));

        return registerVariant(new KnownItemInjection(
                new ResourceLocation("gtceu", idPath),
                prototype.sourceItemId(),
                prototype.sourceTexture(),
                displayName,
                "選定元 " + String.format(Locale.ROOT, "%02d_%03d", sourceNumber, candidateNumber),
                prototype.targetColor(),
                prototype.method(),
                Stage.FINISHED));
    }

    public static ItemEntry<KnownItemInjectionItem> registerVariant(KnownItemInjection declaration) {
        if (!"gtceu".equals(declaration.id().getNamespace())) {
            throw new IllegalArgumentException("Known item injections currently register through gtceu: " +
                    declaration.id());
        }
        if (!IDS.add(declaration.id())) {
            throw new IllegalArgumentException("Duplicate known item injection ID: " + declaration.id());
        }

        ItemEntry<KnownItemInjectionItem> entry = GTRegistration.REGISTRATE
                .item(declaration.id().getPath(), properties ->
                        new KnownItemInjectionItem(properties.stacksTo(64), declaration))
                .onRegister(item -> DECLARATION_BY_ITEM.put(item, declaration))
                .setData(ProviderType.LANG, NonNullBiConsumer.noop())
                .model(NonNullBiConsumer.noop())
                .register();
        REGISTERED.add(new RegisteredInjection(declaration, entry));
        return entry;
    }

    public static List<KnownItemInjection> declarations() {
        return REGISTERED.stream().map(RegisteredInjection::declaration).toList();
    }

    /**
     * Entry point for custom machines/capabilities that want to select injected items without hard-coding
     * every numbered registry ID.
     */
    public static KnownItemInjection declarationFor(ItemStack stack) {
        return DECLARATION_BY_ITEM.get(stack.getItem());
    }

    public static boolean isInjectionOf(ItemStack stack, ResourceLocation sourceItemId) {
        KnownItemInjection declaration = declarationFor(stack);
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
                .forEach(registered -> output.accept(registered.entry().asStack()));
    }

    private static ItemStack iconFor(Stage stage) {
        return REGISTERED.stream()
                .filter(registered -> registered.declaration().stage() == stage)
                .findFirst()
                .map(registered -> registered.entry().asStack())
                .orElseGet(() -> new ItemStack(Items.PAPER));
    }

    private static ResourceLocation inferItemTexture(ResourceLocation sourceItemId) {
        return new ResourceLocation(
                sourceItemId.getNamespace(),
                "item/" + sourceItemId.getPath());
    }

    private static int sourceNumber(ResourceLocation sourceItemId) {
        Integer number = SOURCE_NUMBERS.get(sourceItemId);
        if (number == null) {
            throw new IllegalArgumentException(
                    "Assign a stable source number before registering prototype family: " + sourceItemId);
        }
        return number;
    }

    private static ResourceLocation sourceItemId(int sourceNumber) {
        return SOURCE_NUMBERS.entrySet().stream()
                .filter(entry -> entry.getValue() == sourceNumber)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown source number: " + String.format(Locale.ROOT, "%02d", sourceNumber)));
    }

    private static void requirePath(String value, String name) {
        if (value == null || !value.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException(name + " must be a valid registry path: " + value);
        }
    }

    private static String hex(int color) {
        return String.format(Locale.ROOT, "%06X", color & 0xFFFFFF);
    }

    public record Shade(String id, float lightnessOffset) {
        public Shade {
            requirePath(id, "shade id");
            if (!Float.isFinite(lightnessOffset) || lightnessOffset < -1.0F || lightnessOffset > 1.0F) {
                throw new IllegalArgumentException("lightnessOffset must be finite and between -1 and 1");
            }
        }
    }

    private record RegisteredInjection(
            KnownItemInjection declaration,
            ItemEntry<KnownItemInjectionItem> entry) {}
}
