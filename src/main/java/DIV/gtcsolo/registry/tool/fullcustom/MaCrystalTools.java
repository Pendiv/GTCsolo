package DIV.gtcsolo.registry.tool.fullcustom;

import DIV.gtcsolo.Gtcsolo;

import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolDefinitionBuilder;
import com.gregtechceu.gtceu.api.item.tool.behavior.IToolBehavior;
import com.gregtechceu.gtceu.common.item.tool.behavior.TreeFellingBehavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

/** Growing hand tools using native area mining and supplied MA crystal artwork. */
public final class MaCrystalTools {
    private MaCrystalTools() {}

    static void register() {
        register("pickaxe", "ピッケル", "Pickaxe", GTToolType.PICKAXE);
        register("axe", "斧", "Axe", GTToolType.AXE);
        register("shovel", "シャベル", "Shovel", GTToolType.SHOVEL);
    }

    static FullCustomGtToolDeclaration declaration(
            String suffix, String japanese, String english, GTToolType function) {
        var nativeStats = function.toolDefinition;
        var definition =
                new ToolDefinitionBuilder()
                        .blockBreaking()
                        .aoe(MaToolGrowth.INITIAL_AREA)
                        .damagePerAction(0)
                        .damagePerCraftingAction(0)
                        .baseEfficiency(0)
                        .attackDamage(nativeStats.getBaseDamage(ItemStack.EMPTY))
                        .attackSpeed(nativeStats.getAttackSpeed(ItemStack.EMPTY))
                        .canApplyEnchantment(
                                (stack, enchantment) ->
                                        nativeStats.isEnchantable(stack)
                                                && nativeStats.canApplyEnchantment(
                                                        stack, enchantment))
                        // Tree felling can extend outside the requested mining area.
                        .behaviors(
                                nativeStats.getBehaviors().stream()
                                        .filter(
                                                behavior ->
                                                        !(behavior instanceof TreeFellingBehavior))
                                        .toArray(IToolBehavior[]::new))
                        .build();
        var tier = Tiers.NETHERITE;
        return FullCustomGtToolDeclaration.builder(
                        new ResourceLocation(Gtcsolo.MODID, "ma_crystal_" + suffix), function)
                .name("ja_jp", "MA結晶製" + japanese)
                .name("en_us", "MA Crystal " + english)
                .color(0xFFFFFF)
                .stats(
                        tier.getSpeed(),
                        tier.getAttackDamageBonus(),
                        0,
                        tier.getUses(),
                        tier.getLevel(),
                        tier.getEnchantmentValue())
                .unbreakable()
                .toolDefinition(definition)
                .build();
    }

    private static void register(
            String suffix, String japanese, String english, GTToolType function) {
        FullCustomGtTools.register(
                declaration(suffix, japanese, english, function), MaCrystalToolItem::new);
    }
}
