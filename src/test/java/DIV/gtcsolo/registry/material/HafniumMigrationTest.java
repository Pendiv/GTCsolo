package DIV.gtcsolo.registry.material;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import java.lang.reflect.Proxy;
import java.util.List;

/** Uses Forge's actual missing-mapping event and action; no live game or world is loaded. */
public final class HafniumMigrationTest {
    private static ResourceLocation id(String name) { return new ResourceLocation(name); }
    private static void check(boolean test, String why) { if (!test) throw new AssertionError(why); }

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        try { Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0
                    || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
            System.out.println("Standalone fixture: Forge network transformation is not active.");
        }
        String[][] pairs = {
                {"hafhnium", "hafnium"}, {"flowing_hafhnium", "flowing_hafnium"},
                {"hafhnium_dust", "hafnium_dust"}, {"small_hafhnium_dust", "small_hafnium_dust"},
                {"raw_hafhnium_block", "raw_hafnium_block"}, {"marble_hafhnium_ore", "marble_hafnium_ore"},
                {"big_hafhnium_plate", "big_hafnium_plate"}, {"hafhnium_bucket", "hafnium_bucket"}};
        for (var pair : pairs) check(HafniumMigration.replacement(id("gtcsolo:" + pair[0])).equals(id("gtceu:" + pair[1])),pair[0]);
        for (String unchanged : new String[]{"gtceu:hafhnium_dust", "gtcsolo:hafhnium_carbide_ingot",
                "gtcsolo:hafhnium_diboride_dust", "gtcsolo:raw_hafhnium_carbide", "gtcsolo:technetium_dust",
                "gtcsolo:enigma_dust", "gtcsolo:almosthafhnium_dust", "gtcsolo:hafhniumlike_dust"})
            check(HafniumMigration.replacement(id(unchanged)) == null, "Unaffected " + unchanged);
        ResourceKey<Registry<Object>> key = ResourceKey.createRegistryKey(id("minecraft:item"));
        ResourceLocation targetId = id("gtceu:hafnium_dust");
        Object target = new Object();
        @SuppressWarnings("unchecked")
        IForgeRegistry<Object> registry = (IForgeRegistry<Object>) Proxy.newProxyInstance(
                IForgeRegistry.class.getClassLoader(), new Class[]{IForgeRegistry.class}, (proxy, method, values) -> switch (method.getName()) {
                    case "getRegistryKey" -> key;
                    case "containsKey" -> targetId.equals(values[0]);
                    case "getValue" -> targetId.equals(values[0]) ? target : null;
                    case "getKey" -> values[0] == target ? targetId : null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var old = new MissingMappingsEvent.Mapping<>(registry, registry, id("gtcsolo:hafhnium_dust"), 1);
        var compound = new MissingMappingsEvent.Mapping<>(registry, registry, id("gtcsolo:hafhnium_carbide_ingot"), 2);
        var absent = new MissingMappingsEvent.Mapping<>(registry, registry, id("gtcsolo:hafhnium_unregistered_form"), 3);
        var event = new MissingMappingsEvent(key, registry, List.of(old, compound, absent));
        var remap = HafniumMigration.class.getDeclaredMethod("remap", MissingMappingsEvent.class, IForgeRegistry.class);
        remap.setAccessible(true);
        remap.invoke(null, event, registry);
        var action = MissingMappingsEvent.Mapping.class.getDeclaredField("action"); action.setAccessible(true);
        check(action.get(old) == MissingMappingsEvent.Action.REMAP, "Forge accepts native replacement");
        check(action.get(compound) == MissingMappingsEvent.Action.DEFAULT, "Compound kept");
        check(action.get(absent) == MissingMappingsEvent.Action.DEFAULT, "Missing target never discarded");
        System.out.println("PASS: 8 old IDs, 8 exclusions and actual Forge mapping actions; no game/world loaded");
    }
}
