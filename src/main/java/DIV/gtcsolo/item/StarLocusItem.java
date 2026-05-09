package DIV.gtcsolo.item;

/**
 * StarForge で星を構築するための軌跡アイテム。
 * 単一アイテム ID + NBT "Trace" で 8 種類の軌跡を識別する (DataStick 方式)。
 * Trace 未設定 = 空の軌跡。観測機構 (locus_simulation_builder) で書き込まれる。
 * NBT 仕様の詳細は {@link AbstractLocusItem} を参照。
 */
public class StarLocusItem extends AbstractLocusItem {

    public StarLocusItem(Properties props) {
        super(props);
    }

    @Override
    protected String getTranslationKeyPrefix() {
        return "item.gtcsolo.star_locus";
    }
}
