package DIV.gtcsolo.apoth.gem;

public record GemSkin(int color, String iconset) {
    public GemSkin {
        if (iconset == null || iconset.isEmpty()) {
            throw new IllegalArgumentException("iconset must not be empty");
        }
    }
}
