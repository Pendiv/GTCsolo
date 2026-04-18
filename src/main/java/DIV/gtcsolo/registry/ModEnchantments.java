package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Gtcsolo.MODID);

    public static final RegistryObject<Enchantment> ABSOLUTE_KILL =
            ENCHANTMENTS.register("absolute_kill", () -> new AbsoluteKillEnchantment(
                    Enchantment.Rarity.VERY_RARE,
                    EnchantmentCategory.WEAPON,
                    new EquipmentSlot[]{EquipmentSlot.MAINHAND}
            ));

    private static class AbsoluteKillEnchantment extends Enchantment {

        protected AbsoluteKillEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] slots) {
            super(rarity, category, slots);
        }

        @Override
        public int getMaxLevel() {
            return 1;
        }

        @Override
        public boolean isTreasureOnly() {
            return true;
        }

        @Override
        public boolean isTradeable() {
            return false;
        }

        @Override
        public boolean isDiscoverable() {
            return false;
        }

        @Override
        public boolean canApplyAtEnchantingTable(net.minecraft.world.item.ItemStack stack) {
            return false;
        }

        @Override
        public boolean isAllowedOnBooks() {
            return false;
        }
    }
}