package DIV.gtcsolo.progression;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 恒久強化ノードの購入/強化/リスペックのサーバ権威ロジック。
 * コスト (初項1フィボナッチ or 個別指定)・前提 (全AND)・ポイントプール (stat/skill 別) を検証し、
 * 成功時に {@link ProgressionAttributes#reapply} で attribute を貼り直す。
 */
public final class ProgressionManager {

    private ProgressionManager() {}

    public enum Result {
        SUCCESS,
        UNKNOWN_NODE,
        NO_DATA,
        MAX_LEVEL,
        REQUIREMENTS_NOT_MET,
        NOT_ENOUGH_POINTS
    }

    /** 1 レベル分の購入/強化を試みる。 */
    public static Result tryPurchase(ServerPlayer player, ResourceLocation nodeId) {
        ProgressionNode node = ProgressionNodes.INSTANCE.get(nodeId);
        if (node == null) return Result.UNKNOWN_NODE;

        PlayerProgression d = player.getCapability(ProgressionCapability.PLAYER).orElse(null);
        if (d == null) return Result.NO_DATA;

        int current = d.getNodeLevel(nodeId);
        if (current >= node.maxLevel()) return Result.MAX_LEVEL;
        if (!requirementsMet(d, node)) return Result.REQUIREMENTS_NOT_MET;

        long cost = node.costForLevel(current + 1);
        if (cost > Integer.MAX_VALUE) return Result.NOT_ENOUGH_POINTS;

        boolean paid = node.tree() == ProgressionTree.STAT
                ? d.trySpendStatPoints((int) cost)
                : d.trySpendSkillPoints((int) cost);
        if (!paid) return Result.NOT_ENOUGH_POINTS;

        d.setNodeLevel(nodeId, current + 1);
        if (node.tree() == ProgressionTree.STAT) {
            ProgressionAttributes.reapply(player, d);
        }
        return Result.SUCCESS;
    }

    /** 前提 (全AND) を満たすか。 */
    public static boolean requirementsMet(PlayerProgression d, ProgressionNode node) {
        for (ProgressionNode.Requirement req : node.requirements()) {
            if (d.getNodeLevel(req.node()) < req.minLevel()) return false;
        }
        return true;
    }

    /** 全ノード解除＋ポイント全返却し、 attribute を貼り直す (基本無料)。 */
    public static void respec(ServerPlayer player) {
        player.getCapability(ProgressionCapability.PLAYER).ifPresent(d -> {
            d.respecAll();
            ProgressionAttributes.reapply(player, d);
        });
    }
}
