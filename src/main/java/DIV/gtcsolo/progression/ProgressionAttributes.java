package DIV.gtcsolo.progression;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * ステータスノードの attribute 反映。
 *
 * <p>player は永続 entity のため、 modifier は<b>transient</b>（バニラの attribute NBT に保存しない）で付与し、
 * {@link PlayerProgression} を唯一の真実源とする。 ログイン/リスポーン/購入/リスペックのたびに
 * {@link #reapply} で「全 stat ノードの modifier を一旦除去 → 現レベル分を再付与」して整合させる。
 */
public final class ProgressionAttributes {

    private ProgressionAttributes() {}

    /** ノードごとに安定した modifier UUID (= id から決定的に導出)。 */
    public static UUID modifierId(net.minecraft.resources.ResourceLocation nodeId) {
        return UUID.nameUUIDFromBytes(("gtcsolo.progression.node:" + nodeId).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 現在の購入状況に合わせて全 stat ノードの attribute modifier を貼り直す。
     * 未購入(レベル0)のノードは除去のみ (= リスペック後も確実に剥がれる)。
     */
    public static void reapply(LivingEntity entity, PlayerProgression data) {
        for (ProgressionNode node : ProgressionNodes.INSTANCE.all()) {
            if (node.tree() != ProgressionTree.STAT || node.effect() == null) continue;

            AttributeInstance inst = entity.getAttribute(node.effect().attribute());
            if (inst == null) continue;

            UUID id = modifierId(node.id());
            inst.removeModifier(id);   // 冪等: 無ければ no-op

            int level = data.getNodeLevel(node.id());
            if (level <= 0) continue;

            double amount = node.effect().perLevel() * level;
            inst.addTransientModifier(new AttributeModifier(
                    id, "gtcsolo.progression." + node.id().getPath(), amount, node.effect().operation()));
        }
    }
}
