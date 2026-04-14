package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public enum ModArmorMaterial implements ArmorMaterial {

    PLACEHOLDER("gtcsolo:refined_netherstar",
            new int[]{3, 6, 8, 3}, 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0f, 0.1f, () -> Ingredient.EMPTY);

    private final String name;
    private final int[] defense;
    private final int enchantability;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    ModArmorMaterial(String name, int[] defense, int enchantability,
                     SoundEvent equipSound, float toughness, float knockbackResistance,
                     Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.defense = defense;
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override public int getDurabilityForType(ArmorItem.@NotNull Type type) {
        return new int[]{13, 15, 16, 11}[type.ordinal()] * 40;
    }

    @Override public int getDefenseForType(ArmorItem.@NotNull Type type) {
        return defense[type.ordinal()];
    }

    @Override public int getEnchantmentValue() { return enchantability; }
    @Override public @NotNull SoundEvent getEquipSound() { return equipSound; }
    @Override public @NotNull Ingredient getRepairIngredient() { return repairIngredient.get(); }
    @Override public @NotNull String getName() { return name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}
