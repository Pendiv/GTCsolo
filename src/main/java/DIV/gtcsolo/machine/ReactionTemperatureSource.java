package DIV.gtcsolo.machine;

public interface ReactionTemperatureSource {
    /** Current chamber temperature in kelvin, or 0 if unavailable. Read on the server at recipe start. */
    int getReactionTemperatureKelvin();
}
