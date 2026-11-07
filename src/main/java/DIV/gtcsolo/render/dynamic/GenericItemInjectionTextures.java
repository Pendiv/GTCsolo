package DIV.gtcsolo.render.dynamic;

import DIV.gtcsolo.registry.genericiteminjection.GenericItemInjections;

import java.util.List;

/**
 * Adapts generic cross-mod declarations to the shared deterministic recolor engine.
 */
public final class GenericItemInjectionTextures {

    private GenericItemInjectionTextures() {}

    public static void generateAll() {
        List<KnownItemInjectionTextures.Request> requests =
                GenericItemInjections.declarations().stream()
                        .map(declaration -> new KnownItemInjectionTextures.Request(
                                declaration.id(),
                                declaration.sourceTexture(),
                                declaration.targetColor(),
                                declaration.method()))
                        .toList();
        KnownItemInjectionTextures.generate("GenericItemInjection", requests);
    }
}
