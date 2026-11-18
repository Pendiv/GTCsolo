package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import dev.shadowsoffire.attributeslib.api.AttributeChangedValueEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Gtcsolo.MODID)
public final class FlightAttributes {
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Gtcsolo.MODID);
    public static final RegistryObject<Attribute> CAN_CREATIVE_FLY = ATTRIBUTES.register("can_creative_fly", () ->
            new RangedAttribute("attribute.name.gtcsolo.can_creative_fly", 0, 0, 1) {
                @Override
                public double sanitizeValue(double value) {
                    return value >= 1.0 ? 1.0 : 0.0;
                }
            }.setSyncable(true));

    private static final AttributeModifier CREATIVE_FLIGHT_GRANT = new AttributeModifier(
            UUID.fromString("84141963-b7c7-49a2-ac70-9b8a81b2265d"),
            "gtcsolo:can_creative_fly", 1, AttributeModifier.Operation.ADDITION);

    private FlightAttributes() {}

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        modBus.addListener(FlightAttributes::addPlayerAttribute);
    }

    private static void addPlayerAttribute(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, CAN_CREATIVE_FLY.get());
    }

    @SubscribeEvent
    public static void onAttributeChanged(AttributeChangedValueEvent event) {
        if (event.getAttributeInstance().getAttribute() == CAN_CREATIVE_FLY.get()
                && event.getEntity() instanceof ServerPlayer player) {
            syncGrant(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncGrant(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncGrant(player);
    }

    private static void syncGrant(ServerPlayer player) {
        // Apothic Attributes combines the modifier with other grants and syncs vanilla player abilities.
        AttributeInstance flight = player.getAttribute(ALObjects.Attributes.CREATIVE_FLIGHT.get());
        if (flight == null) return;

        boolean enabled = player.getAttributeValue(CAN_CREATIVE_FLY.get()) == 1.0;
        boolean granted = flight.getModifier(CREATIVE_FLIGHT_GRANT.getId()) != null;
        if (enabled && !granted) {
            flight.addTransientModifier(CREATIVE_FLIGHT_GRANT);
        } else if (!enabled && granted) {
            flight.removeModifier(CREATIVE_FLIGHT_GRANT.getId());
        }
    }
}
