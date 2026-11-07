package DIV.gtcsolo.item;

/**
 * StarForge で星を崩壊させたときに出力される副産物。
 * locus_simulation_builder のレシピで「次 tier の軌跡」を作るための触媒として使用 (消費されない)。
 * NBT "Trace" で 8 種類を識別する (StarLocusItem と同じ仕様)。
 */
public class DecayingStarLocusItem extends AbstractLocusItem {

    public DecayingStarLocusItem(Properties props) {
        super(props);
    }

    @Override
    protected String getTranslationKeyPrefix() {
        return "item.gtcsolo.decaying_star_locus";
    }
}
