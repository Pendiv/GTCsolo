package DIV.gtcsolo.api.ingredient;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.item.AbstractLocusItem;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * star_locus / decaying_star_locus 専用の Ingredient。
 *
 * <p>{@link net.minecraftforge.common.crafting.StrictNBTIngredient} は tag=null と tag={} を
 * 別物として扱うため、 ゲーム内で empty な locus stack の正規化状態によってマッチが破綻する。
 * 当該 ingredient は {@link AbstractLocusItem#isEmpty}/{@link AbstractLocusItem#getTrace}
 * セマンティクスで照合するので、 tag の物理表現に依存しない。
 *
 * <ul>
 *   <li>{@code Mode.EMPTY} — Trace NBT 未設定の locus stack のみ通過</li>
 *   <li>{@code Mode.TRACE} — 指定 Trace と一致する stack のみ通過</li>
 * </ul>
 */
public class LocusIngredient extends AbstractIngredient {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    public static final ResourceLocation ID = new ResourceLocation(Gtcsolo.MODID, "locus");
    public static final IIngredientSerializer<LocusIngredient> SERIALIZER = new Serializer();

    public enum Mode {
        /** Trace NBT 未設定の locus を許可 (= 空の軌跡) */
        EMPTY,
        /** 指定 Trace と完全一致する locus を許可 */
        TRACE
    }

    public final Mode mode;
    public final Item item;
    @Nullable public final String trace;

    public LocusIngredient(Mode mode, Item item, @Nullable String trace) {
        super(Stream.of(new Ingredient.ItemValue(buildDisplayStack(mode, item, trace))));
        this.mode = mode;
        this.item = item;
        this.trace = trace;
    }

    private static ItemStack buildDisplayStack(Mode mode, Item item, @Nullable String trace) {
        ItemStack s = new ItemStack(item);
        if (mode == Mode.TRACE && trace != null) {
            AbstractLocusItem.setTrace(s, trace);
        }
        return s;
    }

    public static LocusIngredient empty(Item item) {
        return new LocusIngredient(Mode.EMPTY, item, null);
    }

    public static LocusIngredient ofTrace(Item item, String trace) {
        return new LocusIngredient(Mode.TRACE, item, trace);
    }

    @Override
    public boolean test(@Nullable ItemStack input) {
        // レシピ照合のホットパスなのでログは置かない
        if (input == null || input.isEmpty()) return false;
        if (!input.is(item)) return false;
        return switch (mode) {
            case EMPTY -> AbstractLocusItem.isEmpty(input);
            case TRACE -> trace != null && trace.equals(AbstractLocusItem.getTrace(input));
        };
    }

    @Override
    public boolean isSimple() {
        // NBT を見るので非 simple = vanilla shortcut の Item-only match を抑止
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.addProperty("mode", mode.name().toLowerCase());
        json.addProperty("item", ForgeRegistries.ITEMS.getKey(item).toString());
        if (trace != null) json.addProperty("trace", trace);
        return json;
    }

    public static final class Serializer implements IIngredientSerializer<LocusIngredient> {
        @Override
        public LocusIngredient parse(FriendlyByteBuf buf) {
            Mode mode = buf.readEnum(Mode.class);
            ResourceLocation itemId = buf.readResourceLocation();
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            String trace = buf.readBoolean() ? buf.readUtf() : null;
            return new LocusIngredient(mode, item, trace);
        }

        @Override
        public LocusIngredient parse(JsonObject json) {
            Mode mode = Mode.valueOf(GsonHelper.getAsString(json, "mode").toUpperCase());
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(json, "item"));
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            String trace = json.has("trace") ? json.get("trace").getAsString() : null;
            LocusIngredient ing = new LocusIngredient(mode, item, trace);
            LOGGER.debug("[LocusIngredient] parsed JSON: mode={} item={} trace={}", mode, itemId, trace);
            return ing;
        }

        @Override
        public void write(FriendlyByteBuf buf, LocusIngredient ing) {
            buf.writeEnum(ing.mode);
            buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(ing.item));
            buf.writeBoolean(ing.trace != null);
            if (ing.trace != null) buf.writeUtf(ing.trace);
        }
    }
}
