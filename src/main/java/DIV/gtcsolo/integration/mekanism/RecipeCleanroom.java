package DIV.gtcsolo.integration.mekanism;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.gregtechceu.gtceu.api.capability.ICleanroomReceiver;
import com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CleanroomMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;
import org.jetbrains.annotations.Nullable;

public interface RecipeCleanroom {
    @Nullable CleanroomType gtcsolo$getRequiredCleanroom();
    void gtcsolo$setRequiredCleanroom(@Nullable CleanroomType type);

    static @Nullable CleanroomType read(JsonObject json) {
        JsonElement value = json.get("gtcsolo:cleanroom");
        if (value == null) return null;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new JsonSyntaxException("Expected a GT cleanroom type name for gtcsolo:cleanroom");
        }
        CleanroomType type = CleanroomType.getByName(value.getAsString());
        if (type == null) throw new JsonSyntaxException("Unknown GT cleanroom type: " + value.getAsString());
        return type;
    }

    static boolean matches(Object holder, CleanroomType required) {
        if (!ConfigHolder.INSTANCE.machines.enableCleanroom) return true;
        if (!(holder instanceof ICleanroomReceiver receiver)) return false;
        ICleanroomProvider room = receiver.getCleanroom();
        if (room instanceof CleanroomMachine controller && (controller.isInValid() || !controller.isFormed())) return false;
        return room != null && room.isClean() && room.getTypes().contains(required);
    }
}
