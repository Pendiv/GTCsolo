package DIV.gtcsolo.mixin.jei;

import DIV.gtcsolo.client.ImeSearchTerms;
import mezz.jei.gui.ingredients.IListElement;
import mezz.jei.gui.search.ElementPrefixParser.TokenInfo;
import mezz.jei.gui.search.IElementSearch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Function;

@Pseudo
@Mixin(targets = "mezz.jei.gui.ingredients.IngredientFilter", remap = false)
public abstract class JeiImeSearchMixin {
    @ModifyArg(method = "getSearchResults(Lmezz/jei/gui/ingredients/IngredientFilter$SearchTokens;)Ljava/util/Set;",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"),
            index = 0)
    private Function<TokenInfo, Set<IListElement<?>>> gtcsolo$includeImeTerms(
            Function<TokenInfo, Set<IListElement<?>>> lookup) {
        return token -> gtcsolo$searchToken(token, lookup);
    }

    @Redirect(method = "getSearchResults(Lmezz/jei/gui/ingredients/IngredientFilter$SearchTokens;)Ljava/util/Set;",
            at = @At(value = "INVOKE", target = "Lmezz/jei/gui/search/IElementSearch;getSearchResults(Lmezz/jei/gui/search/ElementPrefixParser$TokenInfo;)Ljava/util/Set;"))
    private Set<IListElement<?>> gtcsolo$excludeImeTerms(IElementSearch search, TokenInfo token) {
        return gtcsolo$searchToken(token, search::getSearchResults);
    }

    @Unique
    private static Set<IListElement<?>> gtcsolo$searchToken(TokenInfo token,
            Function<TokenInfo, Set<IListElement<?>>> lookup) {
        var alternatives = ImeSearchTerms.alternatives(token.token());
        var original = lookup.apply(token);
        if (alternatives.isEmpty()) return original;

        Set<IListElement<?>> combined = Collections.newSetFromMap(new IdentityHashMap<>());
        combined.addAll(original);
        for (String alternative : alternatives) {
            combined.addAll(lookup.apply(new TokenInfo(alternative, token.prefixInfo())));
        }
        return combined;
    }
}
