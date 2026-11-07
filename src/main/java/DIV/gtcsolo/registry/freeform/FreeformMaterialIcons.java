package DIV.gtcsolo.registry.freeform;

import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.common.registry.GTRegistration;

import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import java.util.HashSet;
import java.util.Set;

/**
 * Material を作らずに、任意 ID・任意色で GTCEu の MaterialIcon モデルを使う登録口。
 *
 * <p>このクラスは GTCEu 本体の {@link GTRegistration#REGISTRATE} を使うため、
 * 引数 {@code idPath} は {@code gtceu:idPath} として登録される。</p>
 */
public final class FreeformMaterialIcons {

    private static final Set<String> IDS = new HashSet<>();

    /**
     * 第一段階の動作確認用。ID は Material 名にも TagPrefix の命名規則にも依存しない。
     */
    public static final ItemEntry<FreeformMaterialIconItem> TEST_INGOT = registerItem(
            "gtcsolo_freeform_test_ingot",
            "GTCsolo Freeform Test Ingot",
            MaterialIconType.ingot,
            MaterialIconSet.METALLIC,
            64,
            0xFF30D5C8);

    /**
     * Item と同じ仕組みが Block にも通ることを確認する試験ブロック。
     */
    public static final BlockEntry<FreeformMaterialIconBlock> TEST_BLOCK = registerBlock(
            "gtcsolo_freeform_test_block",
            "GTCsolo Freeform Test Block",
            MaterialIconType.block,
            MaterialIconSet.METALLIC,
            0xFF8A5CFF);

    private FreeformMaterialIcons() {}

    public static ItemEntry<FreeformMaterialIconItem> registerItem(
            String idPath,
            String englishName,
            MaterialIconType iconType,
            MaterialIconSet iconSet,
            int maxStackSize,
            int... layerColors) {
        claimId(idPath);
        if (maxStackSize < 1 || maxStackSize > 64) {
            throw new IllegalArgumentException("maxStackSize must be between 1 and 64: " + maxStackSize);
        }
        return GTRegistration.REGISTRATE
                .item(idPath, properties -> new FreeformMaterialIconItem(
                        properties.stacksTo(maxStackSize), iconType, iconSet, layerColors))
                .lang(englishName)
                .model(NonNullBiConsumer.noop())
                .color(() -> FreeformMaterialIconItem::tintColor)
                .register();
    }

    public static BlockEntry<FreeformMaterialIconBlock> registerBlock(
            String idPath,
            String englishName,
            MaterialIconType iconType,
            MaterialIconSet iconSet,
            int... layerColors) {
        claimId(idPath);
        return GTRegistration.REGISTRATE
                .block(idPath, properties -> new FreeformMaterialIconBlock(
                        properties, iconType, iconSet, layerColors))
                .lang(englishName)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .setData(ProviderType.BLOCKSTATE, NonNullBiConsumer.noop())
                .color(() -> FreeformMaterialIconBlock::blockTintColor)
                .item(FreeformMaterialIconBlock::createItem)
                .model(NonNullBiConsumer.noop())
                .color(() -> FreeformMaterialIconBlock::itemTintColor)
                .build()
                .register();
    }

    /**
     * クラス初期化を明示する入口。登録自体は static field 初期化時に完了する。
     */
    public static void init() {}

    private static void claimId(String idPath) {
        if (!idPath.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid registry path: " + idPath);
        }
        if (!IDS.add(idPath)) {
            throw new IllegalArgumentException("Duplicate freeform MaterialIcon ID: gtceu:" + idPath);
        }
    }
}
