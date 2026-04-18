package DIV.gtcsolo.common.framealtar;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FrameAltarRecipe {

    private final Predicate<ItemStack> targetPredicate;
    private final List<IngredientEntry> ingredients;
    private final Consumer<ItemStack> action;

    private FrameAltarRecipe(Predicate<ItemStack> targetPredicate,
                             List<IngredientEntry> ingredients,
                             Consumer<ItemStack> action) {
        this.targetPredicate = targetPredicate;
        this.ingredients = ingredients;
        this.action = action;
    }

    public MatchResult match(List<ItemStack> frameItems) {
        ItemStack target = null;
        int targetIndex = -1;
        List<ItemStack> remaining = new ArrayList<>();

        for (int i = 0; i < frameItems.size(); i++) {
            ItemStack stack = frameItems.get(i);
            if (target == null && targetPredicate.test(stack)) {
                target = stack;
                targetIndex = i;
            } else {
                remaining.add(stack);
            }
        }

        if (target == null) return MatchResult.FAIL;

        List<IngredientEntry> unmatched = new ArrayList<>(ingredients);
        for (ItemStack stack : remaining) {
            boolean found = false;
            for (int i = 0; i < unmatched.size(); i++) {
                if (unmatched.get(i).test(stack)) {
                    unmatched.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return MatchResult.FAIL;
        }

        if (!unmatched.isEmpty()) return MatchResult.FAIL;

        return new MatchResult(target, targetIndex);
    }

    public void apply(ItemStack target) {
        action.accept(target);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class MatchResult {
        public static final MatchResult FAIL = new MatchResult(null, -1);

        public final ItemStack target;
        public final int targetIndex;

        MatchResult(ItemStack target, int targetIndex) {
            this.target = target;
            this.targetIndex = targetIndex;
        }

        public boolean isSuccess() {
            return target != null;
        }
    }

    public record IngredientEntry(Predicate<ItemStack> predicate) {
        public boolean test(ItemStack stack) {
            return predicate.test(stack);
        }
    }

    public static class Builder {
        private Predicate<ItemStack> targetPredicate;
        private final List<IngredientEntry> ingredients = new ArrayList<>();
        private Consumer<ItemStack> action;

        public Builder target(Predicate<ItemStack> predicate) {
            this.targetPredicate = predicate;
            return this;
        }

        public Builder ingredient(Predicate<ItemStack> predicate, int count) {
            for (int i = 0; i < count; i++) {
                ingredients.add(new IngredientEntry(predicate));
            }
            return this;
        }

        public Builder action(Consumer<ItemStack> action) {
            this.action = action;
            return this;
        }

        public FrameAltarRecipe build() {
            return new FrameAltarRecipe(targetPredicate, ingredients, action);
        }
    }
}
