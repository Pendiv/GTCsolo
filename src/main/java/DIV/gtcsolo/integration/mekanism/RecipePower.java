package DIV.gtcsolo.integration.mekanism;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.gregtechceu.gtceu.api.GTValues;

public record RecipePower(long voltage, int amperage, int duration) {
    private static final String VOLTAGE = "gtcsolo:required_voltage";
    private static final String AMPERAGE = "gtcsolo:amperage";
    private static final String DURATION = "gtcsolo:duration";

    public RecipePower {
        if (voltage <= 0 || amperage <= 0 || duration < 0) throw new IllegalArgumentException("Invalid recipe power");
    }

    /** A missing duration uses the machine's normal processing time. Durations are in ticks. */
    public static RecipePower read(JsonObject json) {
        JsonElement value = json.get(VOLTAGE);
        if (value == null && !json.has(AMPERAGE) && !json.has(DURATION)) return null;
        if (value == null || !value.isJsonPrimitive()) throw new JsonSyntaxException("Expected " + VOLTAGE);
        long voltage = value.getAsJsonPrimitive().isNumber() ? positiveInteger(value, VOLTAGE) : namedVoltage(value.getAsString());
        int amps = json.has(AMPERAGE) ? Math.toIntExact(positiveInteger(json.get(AMPERAGE), AMPERAGE)) : 1;
        int ticks = json.has(DURATION) ? Math.toIntExact(positiveInteger(json.get(DURATION), DURATION)) : 0;
        return new RecipePower(voltage, amps, ticks);
    }

    private static long namedVoltage(String name) {
        for (int i = 0; i < GTValues.VN.length; i++) {
            if (GTValues.VN[i].equalsIgnoreCase(name)) return GTValues.V[i];
        }
        throw new JsonSyntaxException("Unknown GT voltage tier: " + name);
    }

    private static long positiveInteger(JsonElement value, String key) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new JsonSyntaxException("Expected an integer for " + key);
        long number = value.getAsBigDecimal().longValueExact();
        if (number <= 0) throw new JsonSyntaxException("Expected a positive " + key);
        return number;
    }

    public int overclocks(long rating, int ticks) {
        int stages = 0;
        for (long v = Math.max(voltage, GTValues.V[GTValues.ULV]); v <= rating / 4 && ticks > 1; v *= 4) {
            stages++;
            ticks = (ticks >>> 1) + (ticks & 1);
        }
        return stages;
    }

    public int operatingTicks(int ticks, int stages) {
        return (int) (1 + ((Math.max(1, ticks) - 1L) >> stages));
    }
}
