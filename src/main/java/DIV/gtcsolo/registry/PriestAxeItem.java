package DIV.gtcsolo.registry;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.ForgeTier;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;

public class PriestAxeItem extends AxeItem {

    // ★ 追加：CUSTOM_TIER
    public static final Tier CUSTOM_TIER = new ForgeTier(
            3,              // ダイヤ相当
            33560336,         // 耐久
            138.0F,           // 採掘速度
            -0.8F,           // 攻撃力補正（AxeItemで別に足す）
            15,
            BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(Items.DIAMOND)
    );

    private static final double ATTACK_DAMAGE = 22.0D;
    private static final double ATTACK_SPEED = 0.3D; // 表示2.4

    public PriestAxeItem(Tier tier, Properties props) {
        super(tier, (float) ATTACK_DAMAGE, (float) ATTACK_SPEED, props);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return ToolActions.DEFAULT_AXE_ACTIONS.contains(action)
                || ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(action)
                || ToolActions.DEFAULT_SHOVEL_ACTIONS.contains(action)
                || ToolActions.DEFAULT_HOE_ACTIONS.contains(action);
    }

    private static boolean isMultiToolBlock(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.MINEABLE_WITH_HOE);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!isMultiToolBlock(state)) {
            return super.isCorrectToolForDrops(stack, state);
        }

        if (!state.requiresCorrectToolForDrops()) {
            return true;
        }

        return TierSortingRegistry.isCorrectTierForDrops(this.getTier(), state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (isMultiToolBlock(state)) {
            return this.speed;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            builder.put(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                            ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION));

            builder.put(Attributes.ATTACK_SPEED,
                    new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                            ATTACK_SPEED, AttributeModifier.Operation.ADDITION));

            return builder.build();
        }
        return super.getDefaultAttributeModifiers(slot);
    }
}