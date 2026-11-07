package DIV.gtcsolo.registry.tool;

import DIV.gtcsolo.Gtcsolo;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.ToolProperty;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.common.data.GTMaterialItems;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 値を持てない {@link MaterialFlag} の代わりに、既知 Material へGTツール生成宣言と性能値を付与する。
 *
 * <p>適用先は {@code PostMaterialEvent}。完全に新しい Item を登録するものではなく、
 * GTCEu 標準の {@code Material × GTToolType} 生成経路へ設定を渡す。</p>
 */
public final class GtcToolMaterialDeclaration {

    private static final Set<GTToolType> PLATE_RECIPE_TYPES = Set.of(
            GTToolType.MINING_HAMMER,
            GTToolType.SPADE,
            GTToolType.SAW,
            GTToolType.AXE,
            GTToolType.HOE,
            GTToolType.PICKAXE,
            GTToolType.SCYTHE,
            GTToolType.SHOVEL,
            GTToolType.SWORD,
            GTToolType.HARD_HAMMER,
            GTToolType.FILE,
            GTToolType.KNIFE,
            GTToolType.WRENCH,
            GTToolType.DRILL_LV,
            GTToolType.DRILL_MV,
            GTToolType.DRILL_HV,
            GTToolType.DRILL_EV,
            GTToolType.DRILL_IV,
            GTToolType.CHAINSAW_LV,
            GTToolType.WRENCH_LV,
            GTToolType.WRENCH_HV,
            GTToolType.WRENCH_IV,
            GTToolType.WIRE_CUTTER_LV,
            GTToolType.WIRE_CUTTER_HV,
            GTToolType.WIRE_CUTTER_IV,
            GTToolType.BUZZSAW);

    private static final Set<GTToolType> ROD_RECIPE_TYPES = Set.of(
            GTToolType.BUTCHERY_KNIFE,
            GTToolType.WIRE_CUTTER,
            GTToolType.SCREWDRIVER,
            GTToolType.CROWBAR);

    private final String name;
    private final Supplier<Material> materialSupplier;
    private final Supplier<Material> inheritedStatsSupplier;
    private final GTToolType[] toolTypes;
    private final GtcToolGroup[] toolGroups;
    private final Set<GTToolType> excludedToolTypes;
    private final Map<GTToolType, GTToolType> replacedToolTypes;
    private final boolean addToolTypes;
    private final boolean standardRecipes;
    private final Set<MaterialFlag> extraFlags;

    private final Float abilityCount;
    private final Float harvestSpeed;
    private final Float attackDamage;
    private final Float attackSpeed;
    private final Integer durability;
    private final Integer harvestLevel;
    private final Integer enchantability;
    private final Integer materialDurabilityMultiplier;

    private final float performanceMultiplier;
    private final float harvestSpeedMultiplier;
    private final float attackDamageMultiplier;
    private final float durabilityMultiplier;

    private final Boolean unbreakable;
    private final Boolean magnetic;
    private final Boolean ignoreCraftingTools;

    private Material appliedMaterial;
    private GTToolType[] appliedToolTypes = new GTToolType[0];

    private GtcToolMaterialDeclaration(Builder builder) {
        this.name = builder.name;
        this.materialSupplier = builder.materialSupplier;
        this.inheritedStatsSupplier = builder.inheritedStatsSupplier;
        this.toolTypes = builder.toolTypes == null ? null : builder.toolTypes.clone();
        this.toolGroups = builder.toolGroups.toArray(GtcToolGroup[]::new);
        this.excludedToolTypes = Set.copyOf(builder.excludedToolTypes);
        this.replacedToolTypes = Map.copyOf(builder.replacedToolTypes);
        this.addToolTypes = builder.addToolTypes;
        this.standardRecipes = builder.standardRecipes;
        this.extraFlags = Set.copyOf(builder.extraFlags);
        this.abilityCount = builder.abilityCount;
        this.harvestSpeed = builder.harvestSpeed;
        this.attackDamage = builder.attackDamage;
        this.attackSpeed = builder.attackSpeed;
        this.durability = builder.durability;
        this.harvestLevel = builder.harvestLevel;
        this.enchantability = builder.enchantability;
        this.materialDurabilityMultiplier = builder.materialDurabilityMultiplier;
        this.performanceMultiplier = builder.performanceMultiplier;
        this.harvestSpeedMultiplier = builder.harvestSpeedMultiplier;
        this.attackDamageMultiplier = builder.attackDamageMultiplier;
        this.durabilityMultiplier = builder.durabilityMultiplier;
        this.unbreakable = builder.unbreakable;
        this.magnetic = builder.magnetic;
        this.ignoreCraftingTools = builder.ignoreCraftingTools;
    }

    public static Builder builder(String name, Supplier<Material> materialSupplier) {
        return new Builder(name, materialSupplier);
    }

    public String name() {
        return name;
    }

    public void apply() {
        Material material = requireMaterial(materialSupplier, "target");
        if (!extraFlags.isEmpty()) {
            material.addFlags(extraFlags.toArray(MaterialFlag[]::new));
        }

        boolean modifiesTools = inheritedStatsSupplier != null || hasToolSelectionChanges() ||
                standardRecipes || hasStatChanges();
        if (!modifiesTools) {
            Gtcsolo.LOGGER.info("[GtcToolMaterial] Applied form-only declaration {} to {}", name, material);
            return;
        }

        ToolProperty property = material.getProperty(PropertyKey.TOOL);
        if (property == null) {
            if (inheritedStatsSupplier != null) {
                Material inheritedMaterial = requireMaterial(inheritedStatsSupplier, "inherited stats");
                ToolProperty inherited = inheritedMaterial.getProperty(PropertyKey.TOOL);
                if (inherited == null) {
                    throw new IllegalStateException("Inherited material " + inheritedMaterial + " has no ToolProperty");
                }
                property = copyOf(inherited);
            } else {
                property = createFromDeclaredStats(material);
            }
            material.setProperty(PropertyKey.TOOL, property);
        } else if (inheritedStatsSupplier != null) {
            Material inheritedMaterial = requireMaterial(inheritedStatsSupplier, "inherited stats");
            ToolProperty inherited = inheritedMaterial.getProperty(PropertyKey.TOOL);
            if (inherited == null) {
                throw new IllegalStateException("Inherited material " + inheritedMaterial + " has no ToolProperty");
            }
            copyInto(inherited, property);
        }

        if (hasToolSelectionChanges()) {
            property.setTypes(resolveToolTypes(property.getTypes()));
        }

        float speed = abilityCount != null ? abilityCount : property.getHarvestSpeed();
        float damage = abilityCount != null ? abilityCount / 2.0F : property.getAttackDamage();
        int durabilityValue = abilityCount != null ?
                Math.max(1, Math.round(abilityCount * 100.0F)) :
                property.getDurability();

        property.setHarvestSpeed(speed * performanceMultiplier * harvestSpeedMultiplier);
        property.setAttackDamage(damage * performanceMultiplier * attackDamageMultiplier);
        property.setDurability(Math.max(1,
                Math.round(durabilityValue * performanceMultiplier * durabilityMultiplier)));

        if (abilityCount != null) {
            property.setAttackSpeed(0.0F);
            property.setHarvestLevel(abilityHarvestLevel(abilityCount));
            property.setEnchantability(Math.max(0, Math.round(abilityCount * 2.0F)));
            property.setDurabilityMultiplier(1);
        }

        // 具体値は abilityCount と各種倍率より強い、最終上書き。
        if (harvestSpeed != null) property.setHarvestSpeed(harvestSpeed);
        if (attackDamage != null) property.setAttackDamage(attackDamage);
        if (durability != null) property.setDurability(durability);
        if (attackSpeed != null) property.setAttackSpeed(attackSpeed);
        if (harvestLevel != null) property.setHarvestLevel(harvestLevel);
        if (enchantability != null) property.setEnchantability(enchantability);
        if (materialDurabilityMultiplier != null) {
            property.setDurabilityMultiplier(materialDurabilityMultiplier);
        }
        if (unbreakable != null) property.setUnbreakable(unbreakable);
        if (magnetic != null) property.setMagnetic(magnetic);
        if (ignoreCraftingTools != null) property.setIgnoreCraftingTools(ignoreCraftingTools);

        if (standardRecipes) {
            addStandardRecipeFlags(material, property.getTypes());
        }

        appliedMaterial = material;
        appliedToolTypes = property.getTypes().clone();

        Gtcsolo.LOGGER.info(
                "[GtcToolMaterial] Applied {} to {}: ability={}, types={}, speed={}, damage={}, durability={}, harvest={}, enchantability={}",
                name,
                material,
                abilityCount,
                Arrays.stream(property.getTypes()).map(type -> type.name).toList(),
                property.getHarvestSpeed(),
                property.getAttackDamage(),
                property.getDurability() * property.getDurabilityMultiplier(),
                property.getHarvestLevel(),
                property.getEnchantability());
    }

    public void addGeneratedToolsToCreativeTab(CreativeModeTab.Output output) {
        if (appliedMaterial == null) {
            return;
        }
        for (GTToolType type : appliedToolTypes) {
            var entry = GTMaterialItems.TOOL_ITEMS.get(appliedMaterial, type);
            if (entry != null) {
                output.accept(entry.get().get());
            }
        }
    }

    public ItemStack firstGeneratedToolStack() {
        if (appliedMaterial == null) {
            return ItemStack.EMPTY;
        }
        for (GTToolType type : appliedToolTypes) {
            var entry = GTMaterialItems.TOOL_ITEMS.get(appliedMaterial, type);
            if (entry != null) {
                return entry.get().get();
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean hasStatChanges() {
        return abilityCount != null || harvestSpeed != null || attackDamage != null ||
                attackSpeed != null || durability != null ||
                harvestLevel != null || enchantability != null || materialDurabilityMultiplier != null ||
                performanceMultiplier != 1.0F || harvestSpeedMultiplier != 1.0F ||
                attackDamageMultiplier != 1.0F || durabilityMultiplier != 1.0F ||
                unbreakable != null || magnetic != null || ignoreCraftingTools != null;
    }

    private boolean hasToolSelectionChanges() {
        return toolTypes != null || toolGroups.length > 0 ||
                !excludedToolTypes.isEmpty() || !replacedToolTypes.isEmpty();
    }

    private GTToolType[] resolveToolTypes(GTToolType[] existingTypes) {
        boolean hasPositiveSelection = toolTypes != null || toolGroups.length > 0;
        LinkedHashSet<GTToolType> resolved = new LinkedHashSet<>();

        if (!hasPositiveSelection || addToolTypes) {
            resolved.addAll(Arrays.asList(existingTypes));
        }
        for (GtcToolGroup group : toolGroups) {
            resolved.addAll(group.resolve());
        }
        if (toolTypes != null) {
            resolved.addAll(Arrays.asList(toolTypes));
        }

        for (var replacement : replacedToolTypes.entrySet()) {
            if (resolved.remove(replacement.getKey())) {
                resolved.add(replacement.getValue());
            }
        }
        resolved.removeAll(excludedToolTypes);

        if (resolved.isEmpty()) {
            throw new IllegalStateException("Declaration " + name + " resolved an empty GTToolType selection");
        }
        return resolved.toArray(GTToolType[]::new);
    }

    private static int abilityHarvestLevel(float count) {
        return Math.max(0, (int) Math.floor(count / 3.0F) + 1);
    }

    private static void addStandardRecipeFlags(Material material, GTToolType[] types) {
        LinkedHashSet<MaterialFlag> flags = new LinkedHashSet<>();
        Set<GTToolType> selected = new LinkedHashSet<>(Arrays.asList(types));

        if (selected.stream().anyMatch(PLATE_RECIPE_TYPES::contains)) {
            flags.add(MaterialFlags.GENERATE_PLATE);
        }
        if (selected.stream().anyMatch(ROD_RECIPE_TYPES::contains)) {
            flags.add(MaterialFlags.GENERATE_ROD);
        }
        if (selected.contains(GTToolType.BUTCHERY_KNIFE) || selected.contains(GTToolType.WIRE_CUTTER)) {
            flags.add(MaterialFlags.GENERATE_PLATE);
        }
        if (selected.contains(GTToolType.WIRE_CUTTER)) {
            flags.add(MaterialFlags.GENERATE_BOLT_SCREW);
        }
        if (selected.contains(GTToolType.SCREWDRIVER_LV)) {
            flags.add(MaterialFlags.GENERATE_LONG_ROD);
        }

        if (!flags.isEmpty()) {
            material.addFlags(flags.toArray(MaterialFlag[]::new));
        }
    }

    private ToolProperty createFromDeclaredStats(Material material) {
        boolean hasPositiveSelection = toolTypes != null || toolGroups.length > 0;
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        if (abilityCount == null) {
            if (harvestSpeed == null) missing.add("harvestSpeed");
            if (attackDamage == null) missing.add("attackDamage");
            if (durability == null) missing.add("durability");
            if (harvestLevel == null) missing.add("harvestLevel");
        }
        if (!hasPositiveSelection) missing.add("toolTypes or toolGroups");
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Declaration " + name + " cannot create ToolProperty for " + material +
                    "; use inheritToolStats(...), abilityCount(...), or provide: " + String.join(", ", missing));
        }

        GTToolType[] initialTypes = resolveToolTypes(new GTToolType[0]);
        if (abilityCount != null) {
            return new ToolProperty(
                    abilityCount,
                    abilityCount / 2.0F,
                    Math.max(1, Math.round(abilityCount * 100.0F)),
                    abilityHarvestLevel(abilityCount),
                    initialTypes);
        }
        return new ToolProperty(
                harvestSpeed,
                attackDamage,
                durability,
                harvestLevel,
                initialTypes);
    }

    private static Material requireMaterial(Supplier<Material> supplier, String role) {
        Material material = supplier.get();
        if (material == null) {
            throw new IllegalStateException("Tool material declaration resolved null " + role + " Material");
        }
        return material;
    }

    private static ToolProperty copyOf(ToolProperty source) {
        ToolProperty result = new ToolProperty(
                source.getHarvestSpeed(),
                source.getAttackDamage(),
                source.getDurability(),
                source.getHarvestLevel(),
                source.getTypes().clone());
        copyInto(source, result);
        return result;
    }

    private static void copyInto(ToolProperty source, ToolProperty target) {
        // Self-inheritance already has the requested stats; clearing its enchantment map loses them.
        if (source == target) return;
        target.setHarvestSpeed(source.getHarvestSpeed());
        target.setAttackDamage(source.getAttackDamage());
        target.setAttackSpeed(source.getAttackSpeed());
        target.setDurability(source.getDurability());
        target.setHarvestLevel(source.getHarvestLevel());
        target.setEnchantability(source.getEnchantability());
        target.setDurabilityMultiplier(source.getDurabilityMultiplier());
        target.setIgnoreCraftingTools(source.isIgnoreCraftingTools());
        target.setUnbreakable(source.isUnbreakable());
        target.setMagnetic(source.isMagnetic());
        target.setTypes(source.getTypes().clone());
        target.getEnchantments().clear();
        for (var entry : source.getEnchantments().object2IntEntrySet()) {
            Enchantment enchantment = entry.getKey();
            target.getEnchantments().put(enchantment, entry.getIntValue());
        }
    }

    public static final class Builder {

        private final String name;
        private final Supplier<Material> materialSupplier;
        private Supplier<Material> inheritedStatsSupplier;
        private GTToolType[] toolTypes;
        private final Set<GtcToolGroup> toolGroups = new LinkedHashSet<>();
        private final Set<GTToolType> excludedToolTypes = new LinkedHashSet<>();
        private final Map<GTToolType, GTToolType> replacedToolTypes = new LinkedHashMap<>();
        private boolean addToolTypes;
        private boolean standardRecipes;
        private final Set<MaterialFlag> extraFlags = new LinkedHashSet<>();

        private Float abilityCount;
        private Float harvestSpeed;
        private Float attackDamage;
        private Float attackSpeed;
        private Integer durability;
        private Integer harvestLevel;
        private Integer enchantability;
        private Integer materialDurabilityMultiplier;

        private float performanceMultiplier = 1.0F;
        private float harvestSpeedMultiplier = 1.0F;
        private float attackDamageMultiplier = 1.0F;
        private float durabilityMultiplier = 1.0F;

        private Boolean unbreakable;
        private Boolean magnetic;
        private Boolean ignoreCraftingTools;

        private Builder(String name, Supplier<Material> materialSupplier) {
            this.name = Objects.requireNonNull(name, "name");
            this.materialSupplier = Objects.requireNonNull(materialSupplier, "materialSupplier");
        }

        public Builder inheritToolStats(Supplier<Material> inheritedStatsSupplier) {
            this.inheritedStatsSupplier = Objects.requireNonNull(inheritedStatsSupplier, "inheritedStatsSupplier");
            return this;
        }

        /** 生成ツール一覧を完全に置き換える。 */
        public Builder toolTypes(GTToolType... toolTypes) {
            this.toolTypes = requireToolTypes(toolTypes);
            this.toolGroups.clear();
            this.addToolTypes = false;
            return this;
        }

        /** 既存の生成ツール一覧を残して追加する。 */
        public Builder addToolTypes(GTToolType... toolTypes) {
            boolean hasSelection = this.toolTypes != null || !this.toolGroups.isEmpty();
            LinkedHashSet<GTToolType> merged = new LinkedHashSet<>();
            if (this.toolTypes != null) merged.addAll(Arrays.asList(this.toolTypes));
            merged.addAll(Arrays.asList(requireToolTypes(toolTypes)));
            this.toolTypes = merged.toArray(GTToolType[]::new);
            if (!hasSelection) this.addToolTypes = true;
            return this;
        }

        /** 指定グループの和で生成ツール一覧を完全に置き換える。 */
        public Builder toolGroups(GtcToolGroup... groups) {
            this.toolGroups.clear();
            this.toolGroups.addAll(requireToolGroups(groups));
            this.toolTypes = null;
            this.addToolTypes = false;
            return this;
        }

        /** 現在の選択、または継承元の生成ツール一覧へグループを追加する。 */
        public Builder addToolGroups(GtcToolGroup... groups) {
            boolean hasSelection = this.toolTypes != null || !this.toolGroups.isEmpty();
            this.toolGroups.addAll(requireToolGroups(groups));
            if (!hasSelection) this.addToolTypes = true;
            return this;
        }

        /** 選択結果から指定ツールを除外する。 */
        public Builder excludeToolTypes(GTToolType... toolTypes) {
            this.excludedToolTypes.addAll(Arrays.asList(requireToolTypes(toolTypes)));
            return this;
        }

        /** 選択結果にfromが含まれる場合だけtoへ差し替える。 */
        public Builder replaceToolType(GTToolType from, GTToolType to) {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            if (from == to) throw new IllegalArgumentException("Tool replacement must change the type");
            this.replacedToolTypes.put(from, to);
            return this;
        }

        /**
         * GT標準レシピ生成へ乗せる。選択ツールに必要な plate / rod 等のMaterialFlagも補う。
         */
        public Builder standardRecipes() {
            this.standardRecipes = true;
            return this;
        }

        public Builder materialFlags(MaterialFlag... flags) {
            extraFlags.addAll(Arrays.asList(flags));
            return this;
        }

        /**
         * 素材の統合性能値。
         * speed=A, damage=A/2, durability=A*100, harvest=floor(A/3)+1, enchantability=A*2。
         */
        public Builder abilityCount(float value) {
            this.abilityCount = positiveFinite(value, "abilityCount");
            return this;
        }

        public Builder performanceMultiplier(float multiplier) {
            this.performanceMultiplier = positiveFinite(multiplier, "performanceMultiplier");
            return this;
        }

        public Builder harvestSpeedMultiplier(float multiplier) {
            this.harvestSpeedMultiplier = positiveFinite(multiplier, "harvestSpeedMultiplier");
            return this;
        }

        public Builder attackDamageMultiplier(float multiplier) {
            this.attackDamageMultiplier = positiveFinite(multiplier, "attackDamageMultiplier");
            return this;
        }

        public Builder durabilityMultiplier(float multiplier) {
            this.durabilityMultiplier = positiveFinite(multiplier, "durabilityMultiplier");
            return this;
        }

        public Builder harvestSpeed(float value) {
            this.harvestSpeed = nonNegativeFinite(value, "harvestSpeed");
            return this;
        }

        public Builder attackDamage(float value) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("attackDamage must be finite");
            this.attackDamage = value;
            return this;
        }

        public Builder attackSpeed(float value) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("attackSpeed must be finite");
            this.attackSpeed = value;
            return this;
        }

        public Builder durability(int value) {
            if (value < 1) throw new IllegalArgumentException("durability must be positive");
            this.durability = value;
            return this;
        }

        public Builder harvestLevel(int value) {
            if (value < 0) throw new IllegalArgumentException("harvestLevel must not be negative");
            this.harvestLevel = value;
            return this;
        }

        public Builder enchantability(int value) {
            if (value < 0) throw new IllegalArgumentException("enchantability must not be negative");
            this.enchantability = value;
            return this;
        }

        /** GT ToolProperty 自身が持つ整数の耐久倍率を直接指定する。 */
        public Builder materialDurabilityMultiplier(int value) {
            if (value < 1) throw new IllegalArgumentException("materialDurabilityMultiplier must be positive");
            this.materialDurabilityMultiplier = value;
            return this;
        }

        public Builder unbreakable(boolean value) {
            this.unbreakable = value;
            return this;
        }

        public Builder magnetic(boolean value) {
            this.magnetic = value;
            return this;
        }

        public Builder ignoreCraftingTools(boolean value) {
            this.ignoreCraftingTools = value;
            return this;
        }

        public GtcToolMaterialDeclaration build() {
            if (name.isBlank()) throw new IllegalArgumentException("Declaration name must not be blank");
            return new GtcToolMaterialDeclaration(this);
        }

        private static GTToolType[] requireToolTypes(GTToolType[] types) {
            if (types.length == 0) throw new IllegalArgumentException("At least one GTToolType is required");
            if (Arrays.stream(types).anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("GTToolType must not be null");
            }
            return new LinkedHashSet<>(Arrays.asList(types)).toArray(GTToolType[]::new);
        }

        private static Set<GtcToolGroup> requireToolGroups(GtcToolGroup[] groups) {
            if (groups.length == 0) throw new IllegalArgumentException("At least one GtcToolGroup is required");
            if (Arrays.stream(groups).anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("GtcToolGroup must not be null");
            }
            return new LinkedHashSet<>(Arrays.asList(groups));
        }

        private static float positiveFinite(float value, String name) {
            if (!Float.isFinite(value) || value <= 0) {
                throw new IllegalArgumentException(name + " must be finite and positive");
            }
            return value;
        }

        private static float nonNegativeFinite(float value, String name) {
            if (!Float.isFinite(value) || value < 0) {
                throw new IllegalArgumentException(name + " must be finite and non-negative");
            }
            return value;
        }
    }
}
