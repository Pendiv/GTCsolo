package DIV.gtcsolo.machine.starforge;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

/**
 * StarForge 本体マシン (恒星鍛造炉)。
 *
 * <p>27³ 球状マルチブロックの controller。レシピは {@code gtcsolo:starforge}。
 *
 * <p>仕様詳細: {@code docs/StarForge_spec.md} (ver.0.4)
 *
 * <p><b>未実装 (枠のみ、TODO で順次実装)</b>:
 * <ul>
 *   <li>フェイズ管理 (構築 → 成熟 → 崩壊) state machine</li>
 *   <li>軌跡別ロジック分岐 ({@link StarForgeTraceData.Kind}: NORMAL / MATURITY_SUN / MATURITY_BLACK_HOLE)</li>
 *   <li>通常型: 要求アイテム搬入トリガー → 所定アイテム累計消費 → 履行</li>
 *   <li>太陽: 成熟フェイズで Solar Panel 搬入カウント → 消費から発電に切替 (ダイソンキューブ)</li>
 *   <li>ブラックホール: 崩壊度 100→0 (毎秒1%) + 内部閾値ランダム値 + 超消費 160tick + 電源 OFF で singularity</li>
 *   <li>StarForgeEnergyHatch (WEN 接続専用) との連携</li>
 * </ul>
 */
public class StarForgeMachine extends WorkableElectricMultiblockMachine {

    public StarForgeMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    // TODO: フェイズ管理 (managed sync field で server↔client 同期)
    // TODO: 軌跡 NBT 取得ヘルパ (input bus から star_locus を取り出して Trace 取得)
    // TODO: 軌跡別 Strategy (StarForgeTraceData.Kind ごとに分岐)
}
