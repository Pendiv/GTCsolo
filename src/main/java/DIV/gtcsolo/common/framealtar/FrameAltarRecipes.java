package DIV.gtcsolo.common.framealtar;

import DIV.gtcsolo.registry.ModEnchantments;
import DIV.gtcsolo.registry.ModMaterials;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;
import java.util.HashMap;

public class FrameAltarRecipes {

    public static void init() {
        registerAbsoluteKill();
    }

    private static void registerAbsoluteKill() {
        FrameAltarRegistry.register(
                FrameAltarRecipe.builder()
                        .target(stack -> stack.getItem().canBeDepleted() || stack.isDamageableItem())
                        .ingredient(FrameAltarRecipes::isInfinityBlock, 4)
                        .action(stack -> {
                            Map<Enchantment, Integer> existing = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
                            existing.put(ModEnchantments.ABSOLUTE_KILL.get(), 1);
                            EnchantmentHelper.setEnchantments(existing, stack);
                        })
                        .build()
        );
    }

    private static boolean isInfinityBlock(ItemStack stack) {
        return ChemicalHelper.get(TagPrefix.block, ModMaterials.INFINITY).getItem() == stack.getItem();
    }
}