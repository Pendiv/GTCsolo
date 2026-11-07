package DIV.gtcsolo.registry.tool.fullcustom;

import com.gregtechceu.gtceu.api.data.chemical.material.properties.ToolProperty;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.IGTToolDefinition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Material registration and GT's material-derived ID convention are deliberately not part of this declaration.
 * The selected {@link GTToolType} is used only as the item's function/model/sound/tag template.
 */
public final class FullCustomGtToolDeclaration {

    private final ResourceLocation id;
    private final GTToolType function;
    private final Map<String, String> localizedNames;
    private final int primaryColor;
    private final int secondaryColor;
    private final ToolProperty toolProperty;
    private final IGTToolDefinition toolDefinition;
    private final Set<TagKey<Item>> itemTags;

    private FullCustomGtToolDeclaration(Builder builder) {
        this.id = builder.id;
        this.function = Objects.requireNonNull(builder.function, "function");
        this.localizedNames = Map.copyOf(builder.localizedNames);
        this.primaryColor = builder.primaryColor;
        this.secondaryColor = builder.secondaryColor;
        this.toolProperty = builder.buildToolProperty();
        this.toolDefinition = builder.toolDefinition == null ? function.toolDefinition : builder.toolDefinition;

        LinkedHashSet<TagKey<Item>> tags = new LinkedHashSet<>();
        if (builder.copyFunctionTags) {
            tags.addAll(function.itemTags);
        }
        tags.addAll(builder.additionalTags);
        tags.removeAll(builder.excludedTags);
        this.itemTags = Set.copyOf(tags);
    }

    public ResourceLocation id() {
        return id;
    }

    public GTToolType function() {
        return function;
    }

    public Map<String, String> localizedNames() {
        return localizedNames;
    }

    public int primaryColor() {
        return primaryColor;
    }

    public int secondaryColor() {
        return secondaryColor;
    }

    public ToolProperty toolProperty() {
        return toolProperty;
    }

    public IGTToolDefinition toolDefinition() {
        return toolDefinition;
    }

    public Set<TagKey<Item>> itemTags() {
        return itemTags;
    }

    public String fallbackName() {
        return localizedNames.get("en_us");
    }

    public static Builder builder(ResourceLocation id, GTToolType function) {
        return new Builder(id, function);
    }

    public static final class Builder {

        private final ResourceLocation id;
        private final GTToolType function;
        private final Map<String, String> localizedNames = new LinkedHashMap<>();
        private final List<TagKey<Item>> additionalTags = new ArrayList<>();
        private final List<TagKey<Item>> excludedTags = new ArrayList<>();

        private int primaryColor = 0xFFFFFF;
        private int secondaryColor = -1;
        private float speed = 1.0F;
        private float attackDamage = 1.0F;
        private float attackSpeed = 0.0F;
        private int durability = 100;
        private int harvestLevel = 0;
        private int enchantability = 10;
        private int durabilityMultiplier = 1;
        private boolean unbreakable;
        private boolean magnetic;
        private boolean copyFunctionTags = true;
        private IGTToolDefinition toolDefinition;
        private final Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();

        private Builder(ResourceLocation id, GTToolType function) {
            this.id = Objects.requireNonNull(id, "id");
            this.function = Objects.requireNonNull(function, "function");
        }

        public Builder name(String locale, String displayName) {
            String normalizedLocale = Objects.requireNonNull(locale, "locale")
                    .trim()
                    .toLowerCase(java.util.Locale.ROOT)
                    .replace('-', '_');
            if (!normalizedLocale.matches("[a-z]{2}_[a-z]{2}")) {
                throw new IllegalArgumentException("Locale must look like en_us or ja_jp: " + locale);
            }
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Display name cannot be blank for " + normalizedLocale);
            }
            localizedNames.put(normalizedLocale, displayName);
            return this;
        }

        public Builder color(int primaryColor) {
            this.primaryColor = primaryColor;
            this.secondaryColor = -1;
            return this;
        }

        public Builder colors(int primaryColor, int secondaryColor) {
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            return this;
        }

        /**
         * Sets the explicit material-side stats. The selected function can still add its own damage, speed,
         * durability, quality, or efficiency modifiers exactly as a normal GT-generated tool does.
         */
        public Builder stats(float speed, float attackDamage, float attackSpeed, int durability,
                             int harvestLevel, int enchantability) {
            if (!Float.isFinite(speed) || speed < 0.0F) {
                throw new IllegalArgumentException("speed must be finite and >= 0");
            }
            if (!Float.isFinite(attackDamage)) {
                throw new IllegalArgumentException("attackDamage must be finite");
            }
            if (!Float.isFinite(attackSpeed)) {
                throw new IllegalArgumentException("attackSpeed must be finite");
            }
            if (durability < 1) {
                throw new IllegalArgumentException("durability must be >= 1");
            }
            if (harvestLevel < 0) {
                throw new IllegalArgumentException("harvestLevel must be >= 0");
            }
            if (enchantability < 0) {
                throw new IllegalArgumentException("enchantability must be >= 0");
            }
            this.speed = speed;
            this.attackDamage = attackDamage;
            this.attackSpeed = attackSpeed;
            this.durability = durability;
            this.harvestLevel = harvestLevel;
            this.enchantability = enchantability;
            return this;
        }

        public Builder durabilityMultiplier(int multiplier) {
            if (multiplier < 1) {
                throw new IllegalArgumentException("durability multiplier must be >= 1");
            }
            this.durabilityMultiplier = multiplier;
            return this;
        }

        public Builder unbreakable() {
            this.unbreakable = true;
            return this;
        }

        public Builder magnetic() {
            this.magnetic = true;
            return this;
        }

        public Builder enchantment(Enchantment enchantment, int level) {
            if (level < 1) {
                throw new IllegalArgumentException("enchantment level must be >= 1");
            }
            enchantments.put(Objects.requireNonNull(enchantment, "enchantment"), level);
            return this;
        }

        /**
         * Replaces only the function definition. The chosen GTToolType still supplies item class, model,
         * sound, tool classes, and (by default) tags.
         */
        public Builder toolDefinition(IGTToolDefinition definition) {
            this.toolDefinition = Objects.requireNonNull(definition, "definition");
            return this;
        }

        public Builder copyFunctionTags(boolean copyFunctionTags) {
            this.copyFunctionTags = copyFunctionTags;
            return this;
        }

        @SafeVarargs
        public final Builder addTags(TagKey<Item>... tags) {
            additionalTags.addAll(List.of(tags));
            return this;
        }

        @SafeVarargs
        public final Builder excludeTags(TagKey<Item>... tags) {
            excludedTags.addAll(List.of(tags));
            return this;
        }

        public FullCustomGtToolDeclaration build() {
            if (!"gtcsolo".equals(id.getNamespace()) && !"gtceu".equals(id.getNamespace())) {
                throw new IllegalArgumentException(
                        "Full custom GT tools currently support the gtcsolo and gtceu namespaces: " + id);
            }
            if (!localizedNames.containsKey("en_us")) {
                throw new IllegalStateException("An en_us display name is required for " + id);
            }
            return new FullCustomGtToolDeclaration(this);
        }

        private ToolProperty buildToolProperty() {
            ToolProperty.Builder builder = ToolProperty.Builder
                    .of(speed, attackDamage, durability, harvestLevel, new GTToolType[0])
                    .attackSpeed(attackSpeed)
                    .enchantability(enchantability)
                    .durabilityMultiplier(durabilityMultiplier);
            if (unbreakable) {
                builder.unbreakable();
            }
            if (magnetic) {
                builder.magnetic();
            }
            enchantments.forEach(builder::enchantment);
            return builder.build();
        }
    }
}
