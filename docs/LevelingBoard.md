# レベリングボード (Leveling Board) — システム仕様 & 引き継ぎ資料

> 旧称「progression / プレイヤー恒久強化」。以後この機能群を **レベリングボード** と呼ぶ。
> パッケージ: `DIV.gtcsolo.progression`(+ `.client` / `.network`)。modID `gtcsolo`。
> 本書は ver.1.0 (2026-06-02 時点)。ノード追加・調整のたびに追記すること。

---

## 1. 概要

プレイヤーが経験値を稼ぐと **恒久レベル** が上がり、**スキルポイント / ステータスポイント** が貯まる。
これを **レベリングボード**（フルスクリーンのパン可能な盤面 UI）でノードに投資し、永続的に強化する。

設計思想（作者談）:
- 元来プレイヤー強化は「きつい本編を補う」もの。**ちょっと追い越すくらい**を許容し、プレイヤー独自のビルドを成立させる。
- 「強くなりすぎ」の主因は個々のノードではなく**強ノードの同時積み上げ**。よって個々の効果は残しつつ、**同時有効化数の上限**で制御する（→ 上級スキル）。

---

## 2. コア: XP → 恒久レベル → ポイント

`PlayerProgression`（プレイヤー1人分のデータ層）が担う。

- **lifetimeXp**: 獲得 XP の累積。**死亡・消費で減らない**。これを起点に `permanentLevel` を算出。
- **恒久レベル曲線**: バニラのレベル曲線（`totalXpToReach`、整数厳密）。`lifetimeXp` が次レベル必要量に達するたび `permanentLevel++`。
- **ポイント付与（レベルアップ毎）**: `award = ceil(level / 10)` = 端数切り上げ（Lv1〜10 で 1、11〜20 で 2 …）。
  - **節目ボーナス**: `level % 100 == 0` → ×10、`level % 10 == 0` → ×2。
  - スキルポイント・ステータスポイントを **それぞれ満額** 付与（2プールは独立）。
- **既存キャラ**: 初回ログイン時、現在の累計 XP を一度だけ `seedIfNeeded` で流し込む（`initialized` フラグ）。

XP の入手は通常プレイの経験値全般 +（後述の）実績達成ボーナス。

---

## 3. ツリー / タブ・プール・解放条件

`ProgressionTree`（= UI のタブ）。

| ツリー | プール | 解放条件 | 備考 |
|---|---|---|---|
| **STAT**（初級ステータス） | statPoints | 最初から | attribute 直接強化 |
| **ADVANCED**（上級ステータス） | statPoints（STATと共有） | STAT 累計 **25** レベル | ％乗算系 |
| **SKILL**（初級スキル） | skillPoints | 最初から | 挙動付与（handler/mixin 駆動） |
| **SKILL_ADVANCED**（上級スキル） | skillPoints（SKILLと共有） | SKILL 累計 **45** レベル | 強力なビルド定義系 |

- `usesSkillPool()`: SKILL系=skillPoints / STAT系=statPoints。
- `baseTree()` / `unlockLevels()`: 上級タブは基準ツリーの累計レベルで解放（ADVANCED←STAT25 / SKILL_ADVANCED←SKILL45）。

### 上級スキルの「同時 3 つ」上限
- `ProgressionManager.MAX_ACTIVE_ADVANCED_SKILLS = 3`。
- **SKILL_ADVANCED で level≥1 のノードは同時に 3 つまで**。新規有効化（level 0→1）のときだけ枠を消費（既存ノードのレベル上げは枠を食わない）。
- 4つ目を有効化しようとすると弾き、アクションバーに `message.gtcsolo.advanced_skill_full` を表示。
- **無効化 = 失効 + ポイント返却**: 別途トグルは持たず、`active = level≥1` 設計。枠を空けるには対象を **撤回（refund）** する＝費やしたポイントは全額プールへ戻る。

---

## 4. リスペック / 撤回（無効化）

`ProgressionManager`（サーバ権威ロジック）。

- **購入 `tryPurchase`**: max到達 / 上級ロック / 同時3上限 / 排他グループ / 前提(全AND) / コスト を検証。成功で `setNodeLevel(+1)`、STAT系は attribute 貼り直し、状態同期。
  - `Result`: SUCCESS / UNKNOWN_NODE / NO_DATA / MAX_LEVEL / REQUIREMENTS_NOT_MET / NOT_ENOUGH_POINTS / LOCKED / EXCLUSIVE / ADVANCED_FULL。
- **撤回 `refund`**（ノード1つ）: そのノードに費やした総ポイント(`totalCostUpTo`)を **100% 返却**し level→0。= 上級スキルの「無効化」。
- **リスペック `respec`**: 全ノード解除＋ポイント全額返却（**基本無料・何度でも**）。`available = earned` に戻すだけで全返却が成立。
- 排他: `exclusiveGroup` が同じノードを1つでも持っていれば他は購入不可。

---

## 5. 実績達成 XP（`AdvancementXpHandler`）

実績達成で XP を付与（`giveExperiencePoints` 経由 → バニラ XP と lifetimeXp の両方に反映）。

- 序盤の実績ラッシュ対策で **達成数 n でランプ**（n は capability に保存＝死亡/次元移動でリセットされない）:
  - 1個目 → **0**
  - 2個目以降 → 本来 XP の **`1/(20-(n-1))`** 倍（n=1 で 1/20=5%、増えるほど上昇、**1以上で天井=満額**、以降不変。天井は 21個目=n20）
- **高レアリティ（challenge 枠）はランプ無視で常に満額**。
- レシピ等の **display 無し実績は対象外**。
- 本来 XP（満額時）: バニラ **challenge=1000 / goal=300 / task=100**、**他MOD=400**。

---

## 6. 永続化 / 同期 / 引き継ぎ

- **Capability**: `ProgressionCapability.PLAYER`（`PlayerProgressionProvider` で attach、NBT save/load）。
- **`ProgressionEvents`**: AttachCapabilities / **Clone**(`now.load(old.save())` で死亡・次元移動を引き継ぎ) / XpChange(→addXp) / Login(seed+同期) / Respawn(attribute貼り直し) / AddReloadListener。
- **同期**: `ProgressionSync.sendState`（S→C 状態）、`ProgressionDefsPacket`（S→C ノード定義）。クライアントは `ClientProgressionData` に保持し、`ProgressionQuery.nodeLevel` が dist 安全に解決。
- attribute は **transient modifier**（`ProgressionAttributes.reapply`、UUID は id 由来で固定）。リログ/リスポーンで貼り直す。

---

## 7. 操作

- **キーバインド**: `K`（`key.gtcsolo.progression`、カテゴリ `key.categories.gtcsolo`）で UI を開く。
- **コマンド** `/gtcsolo progression …`:
  | サブ | 機能 |
  |---|---|
  | `info` | 自分のレベル/ポイント等を表示 |
  | `nodes` | 読み込み済みノード一覧 |
  | `addxp <amount>` | XP 付与（テスト用） |
  | `buy <node>` | ノード購入（テスト用） |
  | `respec` | リスペック |
  | `open` | UI を開く |
  | `edit` | **編集モード** トグル（OP lv3。盤面の手動編集用） |
- **UI**（`ProgressionScreen` / `ProgressionCanvasWidget`、LDLib フルスクリーン）:
  - タブは **上部**（JEI 風、上バーをやや明色化）。ポイント表示は **右上固定**。
  - パン可能な盤面にノードを配置。**左クリックで購入**。撤回は右上の**削除トグル(delete/deleted)アイコン**（ポイント表記の左）をクリックで ON にし、ON 中はノードをクリックで即撤回（ポイント全返却）。トグルは表裏でクリックごとに反転。
  - **下帯**: 有効化中(level>0)の上級スキルを通常ノードとして並べる（`updateScreen` で有効集合の変化をリアルタイム追従）。削除モード中はここのノードをクリックでも撤回でき、撤回すると下帯から消える。「有効化 n/3」表示。
  - **ノードにカーソルを合わせると説明（`<title>.desc` lang、全62ノード ja/en）+ 効果範囲 + コスト + 前提**をツールチップ表示。
  - **編集モード**（`ProgressionEdit` + EditState）: ホイールクリックでノード移動、Shift+右クリックで親子リンク作成/線削除。位置・前提は config 側へ永続化。

---

## 8. ノード JSON

配置: `data/gtcsolo/progression/node/<id>.json`（datapack）。config 上書き: `config/gtcsolo/progression/node/<ns>/*.json`。
ローダ: `ProgressionNodes`（`SimpleJsonResourceReloadListener`）。

```jsonc
{
  "tree": "stat | advanced | skill | skill_advanced",   // 必須
  "title": "progression.gtcsolo.<id>",                  // lang キー（必須）
  "icon": "minecraft:iron_sword",                       // アイテム or テクスチャ
  "pos": { "x": 0, "y": 0 },                            // 省略時は「迷子」自動タイル配置
  "max_level": 5,                                        // 必須
  "cost": [1, 1, 2, 3, 5],                               // 省略時は既定フィボナッチ。要素数 = max_level
  "requirements": [ { "node": "gtcsolo:foo", "min_level": 1 } ],  // 全AND
  "exclusive_group": "combat_stance",                    // 同グループは排他
  // STAT / ADVANCED のみ effect 必須。SKILL / SKILL_ADVANCED は effect 禁止（挙動は handler 駆動）
  "effect": {
    "attribute": "minecraft:generic.attack_damage",
    "operation": "addition | multiply_base | multiply_total",
    "scaling": "linear | fib | triangular",
    "amount": 1.0
  }
}
```

### スケーリングとコスト（`ProgressionNode`）
- **effect.scaling**（`valueAt(level)`）:
  - `LINEAR` = amount × level
  - `FIB` = amount × 標準フィボナッチ**総和** … 累計列 **1, 2, 4, 7, 12**（L1..5）
  - `TRIANGULAR` = amount × n(n-1)/2 … **0, 1, 3, 6, 10**（L1..5）。⚠ **L1 で 0**（max_health が該当）
- **cost**（`costForLevel` / `totalCostUpTo`）: 省略時は `fibCost`（1,1,2,3,5,8…）。

---

## 9. ノード一覧（全 65）

### STAT（statPoints・13）— attribute 直接
| id | 名称 | Lv | scaling | 効果 | 前提 |
|---|---|---|---|---|---|
| `attack_base` | 攻撃力 | 5 | linear | attack_damage +1/lv | — |
| `attack_percent` | 攻撃力強化(%) | 5 | fib | attack_damage ×(1+0.03·fib) | attack_base≥1 |
| `armor_base` | 防御力 | 5 | linear | armor +1/lv | — |
| `armor_percent` | 防御力強化(%) | 5 | fib | armor ×(1+0.03·fib) | armor_base≥1 |
| `armor_toughness_base` | 防具強度 | 5 | linear | toughness +0.5/lv | — |
| `armor_toughness_percent` | 防具強度強化(%) | 5 | fib | toughness ×(1+0.03·fib) | toughness_base≥1 |
| `max_health` | 体力 | 5 | triangular | max_health +n(n-1)/2（L5で+10=5♥、**L1は+0**） | — |
| `movement_speed` | 移動速度 | 5 | linear | movement_speed +0.01/lv | — |
| `mining_speed` | 採掘速度上昇 | 15 | linear(cost fib×0.65) | `attributeslib:mining_speed` +0.105/lv（Apotheosis、上限10.0） | — |
| `lucky_hit_rate` | ラッキーヒット率 | 5 | linear | lucky_hit_rate +0.01/lv | — |
| `lucky_hit_rate_percent` | 同強化 | 5 | fib | lucky_hit_rate +0.03·fib | lucky_hit_rate≥1 |
| `lucky_hit_damage` | ラッキーヒットダメージ | 5 | linear | lucky_hit_damage +2/lv | — |
| `lucky_hit_damage_percent` | 同強化 | 5 | fib | lucky_hit_damage +6·fib | lucky_hit_damage≥1 |

> `lucky_hit_rate` / `lucky_hit_damage` は gtcsolo 独自 attribute。実発火は `DIV.gtcsolo.combat.LuckyHitHandler`（LivingHurtEvent・LOWEST で `nextDouble() >= rate` 判定 → 倍率適用）。

### ADVANCED（statPoints・STAT25解放・5）— ％乗算
| id | 名称 | Lv | 効果 |
|---|---|---|---|
| `attack_mult` | 攻撃力乗算 | 5 | attack_damage ×(1+0.06·lv) |
| `armor_mult` | 防御力乗算 | 5 | armor ×(1+0.06·lv) |
| `health_mult` | 体力乗算 | 5 | max_health ×(1+0.04·lv) |
| `damage_reduction` | ダメージ軽減 | 5 | `l2damagetracker:damage_reduction` −0.02·lv（=2n%軽減） |
| `knockback_resist` | ノックバック耐性 | 10 | knockback_resistance +0.1·lv |

### SKILL（skillPoints・24）— handler / mixin 駆動
| id | 名称 | Lv | cost(総) | 効果 | 前提/排他 |
|---|---|---|---|---|---|
| `eating_speed` | 食事速度上昇 | 10 | fib | 食事時間 −10%/lv（最大100%=1tick） | — |
| `better_eating` | よりよい食事 | 1 | fib | 食べ切ると +1HP | — |
| `actually_fine` | 実は平気… | 1 | fib | 10秒毎に隠し満腹度(saturation) +1（上限=現food） | — |
| `water_movement` | 水中移動 | 1 | 5 | 水中で移動速度 ×1.5 | — |
| `permanent_jump` | 永続跳躍 | 2 | 5,5(10) | 跳躍力上昇 lv1→I / lv2→II 常時 | — |
| `magic_reduction` | 魔法ダメージ軽減 | 5 | fib | 魔法ダメージ −2n% | — |
| `explosion_reduction` | 爆発ダメージ軽減 | 5 | fib | 爆発ダメージ −2n% | — |
| `axe_mastery` | 斧強化 | 5 | fib | 斧装備中 攻撃力+10 & +2n%、攻撃速度−12% | — |
| `prepared` | 準備万端 | 3 | fib | HP割合1%毎に攻撃力+(0.02+0.01n)%、満タンで更に+5% | — |
| `enchant_lapis` | エンチャントコストカット | 1 | 5 | エンチャント時ラピス1個還元（mixin） | — |
| `torch_launcher` | 暗がりの灯火 | 1 | 5 | ピッケル右クリで目線先に松明設置（耐久−1） | — |
| `lucky_heal` | 幸運の癒し | 4 | fib | ラッキーヒット時 nHP(0.5n♥)回復 | — |
| `finisher` | 追い打ち | 5 | fib | 対象HP≤40%で与ダメ ×(1.15+0.1n) | 排他:combat_stance |
| `superiority` | 優勢 | 5 | fib | 対象HP≥60%で与ダメ ×(1.115+0.01n) | 排他:combat_stance |
| `moon_trip` | 月旅行 | 1 | 5 | 落下速度−50%（重力×0.5、落下ダメも減）effect | 排他:celestial_trip |
| `sun_trip` | 太陽旅行 | 1 | 5 | 落下速度+50%（重力×1.5、落下ダメも増）effect | 排他:celestial_trip |
| `veil_of_illusion` | 幻惑の帳 | 4 | 5,5,8,8(26) | 敵を常にデバフ(3+N)個所持と見なす（祟りの呼応のMに加算） | — |
| `flight_magic` | 飛行魔法<初級> | 1 | 15 | クリエ飛行可。代わりに透明無効・防御/防具強度0・敵の攻撃で即死 | — |
| `mining_origin` | 採掘の原点 | 1 | 5 | 真下1ブロックも同時採掘（CTなし） | — |
| `mining_industrialization` | 採掘の工業化 | 5 | 1,11,31,61,101(205) | 掘った面に垂直な平面 (2n+1)²（CT1秒） | mining_origin≥1 |
| `mass_industrialization` | 採掘の大規模工業化 | 5 | 1,13,37,73,121(245) | 立方体 (2n+1)³ 相当・面から奥へ（**CT=Lv秒**, L1=1s〜L5=5s） | mining_industrialization≥1 |
| `mining_essence` | 採掘の真髄 | 5 | 5×5(25) | 範囲破壊の耐久消費 −20%/lv（確率スキップ、lv5=0） | mass_industrialization≥1 |
| `vein_mining` | 一括破壊 | 1 | 30 | 同種ブロックを最大256連結破壊（地形系=石/深層岩/ネザラク/エンド石等タグ除外）CT1秒 | — |
| `break_cooldown` | 破壊スキルのCT減少 | 1 | 10 | 全破壊スキル（範囲＋一括）のCTを −1秒（上級と合算） | — |

> 範囲破壊共通（`MiningBreakHandler`）: しゃがみで無効 / ピッケル必須（`minecraft:pickaxes` or PICKAXE_DIG）/ `isCorrectToolForDrops`（tier 尊重）/ ドロップあり。**向きはプレイヤー視線の主軸**（`Direction.getNearest(getLookAngle())`、掘った面ではない）→ 真ん前=垂直平面で高さ維持・真下=水平平面。さらに垂直平面/立方体の**縦の底=プレイヤー足元Y**にアンカー（origin中心ではない）ので、前進採掘で床が下がらない。上位ティア優先（大規模>工業化>原点）。
> **一括破壊**は地形でないブロックを優先処理（範囲破壊より先）。**CT減少**: 初級`break_cooldown`(-1s)=全破壊スキル、上級`break_cooldown_large`(-1s/Lv)=範囲破壊のみ、合算・下限0。

### SKILL_ADVANCED（skillPoints・SKILL45解放・**同時3まで**・23）
| id | 名称 | Lv | cost(総) | 効果 | 排他 |
|---|---|---|---|---|---|
| `sleep_heal` | 安眠回復 | 1 | 25 | 起床時 HP 全快 | — |
| `auto_mace` | オートメイス | 1 | 40 | 全近接にメイス挙動（落下距離スケール追撃＋音、攻撃者の落下ダメ相殺） | — |
| `equivalent_exchange` | 等価交換 | 1 | 50 | 最大HP×0.8、減少した実数値を攻撃力へ転換（動的・reapply特殊） | — |
| `magic_amplify` | 魔導増幅 | 1 | 25 | 魔法ダメージ ×1.2（`l2damagetracker:magic_damage` +0.2）effect | — |
| `pure_endurance` | 純粋耐久 | 4 | 20,25,30,35(110) | 被ダメ −(10+10n)% | — |
| `pure_firepower` | 純粋火力増加 | 4 | 20,25,30,35(110) | 与ダメ +(20+20n)% | — |
| `gluttony` | 暴食 | 4 | 20,25,30,35(110) | 食事速度+50%固定、食べ切る毎に最大HPの(10+2n)%＋(2+n)♥回復＋デバフ1除去 | — |
| `abundance` | 豊穣 | 4 | 15,20,25,30(90) | 鍬でsneak右クリ→周囲3に骨粉効果、CT(10−2(n−1))秒 | — |
| `curse_resonance` | 祟りの呼応 | 5 | 20,25,30,35,40(150) | 与ダメ ×(1+n(4+M)/100)、M=対象デバフ数(＋幻惑の帳) | — |
| `train_legs_plus` | 足腰を鍛える+ | 1 | 30 | 落下ダメ無効、魔法/通常ダメ ×0.8 | — |
| `loose_ground` | 地盤緩くてビル立たず | 4 | 20,25,30,35(110) | 回復量 ×(0.3+0.03n)、最大HP +80%（固定・reapply特殊） | — |
| `executioner` | 処刑人 | 1 | 40 | 対象HP≤50%で与ダメ ×1.4 | — |
| `break_cooldown_large` | 範囲破壊のCT減少-大 | 4 | 20,25,30,35(110) | 範囲破壊（工業化/大規模）のCTを −1秒/Lv | — |
| `flight_magic_advanced` | 飛行魔法<上級> | 1 | 50 | クリエ飛行＋水平速度2倍。即死無効・防御/防具強度は本来の80%（初級デバフ軽減・初級より優先） | — |
| `soul_offering` | 魂を捧げる | 3 | 25,35,45(105) | オフハンド武器をsneakで破壊→60+30n秒その攻撃力/速度を獲得（効果中再使用不可） | — |
| `immortal` | 不死 | 1 | 60 | HP0時に最大HP10%で復活+10%緩衝体力（CT60秒） | — |
| `critical_accel` | 臨界点超越[加速] | 5 | 20,25,30,35,40(150) | 移動速度0.4超過分を−(30−2n)%層で0.4までキャップ | 排他:critical_point |
| `critical_attack` | 臨界点超越[攻撃] | 5 | 〃(150) | 攻撃速度2超過分を同様に2までキャップ | 排他:critical_point |
| `critical_miracle` | 臨界点超越[奇跡] | 5 | 〃(150) | ラッキーヒット率100%超過分を1%毎に(2.5+0.025n)%ラッキーダメージへ転換 | 排他:critical_point |
| `turning_point_l2h` | 転換点[L2Hostility] | 1 | 30 | 会心↔ラッキーヒットの相互変換（率/ダメージ。詳細`TurningPointHandler`） | — |
| `turning_point_apoth` | 転換点[Apotheosis] | 1 | 30 | 〃 | — |
| `turning_point_to_l2h` | 転換点[→L2Hostility] | 1 | 30 | 〃 | — |
| `turning_point_to_apoth` | 転換点[→Apotheosis] | 1 | 30 | 〃 | — |

> **臨界点超越スタック**: 1層 = 与ダメ ×1.2（累乗、ダメージ計算上は最大10層）。毎tick自分のmodifierを剥がして自然値から再計算するので、敵デバフ等で現ステータスが変われば層も追従（常時観測）。デバフ層数とリンク。

---

## 10. コードマップ（`DIV.gtcsolo.progression`）

**データ/ロジック**
- `PlayerProgression` — データ層（lifetimeXp/permanentLevel/2プール/earned/nodeLevels/advXpCount、NBT）
- `ProgressionCapability` / `PlayerProgressionProvider` / `ProgressionEvents` — capability 配線・イベント
- `ProgressionManager` — 購入/撤回/リスペック/同時3上限/前提/排他（サーバ権威）
- `ProgressionNode` / `ProgressionNodes` / `ProgressionTree` — ノード定義・ローダ・ツリー
- `ProgressionAttributes` — transient modifier 適用。**effect を持つノードは tree 問わず適用**（月旅行/太陽旅行/魔導増幅）。加えて (a) **危険度の隠し効果**＝有効上級スキルの総Lv×5 を `l2hostility:extra_difficulty` に加算、(b) **等価交換**（最大HP×0.8→減少実数値を攻撃力へ・動的）、(c) **地盤緩く**（最大HP+80%固定）の特殊処理。
- `ProgressionQuery` — dist 安全な nodeLevel 取得
- `ProgressionEdit` / `ProgressionEditState` — 盤面編集モード

**効果ハンドラ**（node id → 実装）
- `EatingSkillHandler`(eating_speed/better_eating) / `ActuallyFineHandler` / `UtilityHandler`(water_movement/permanent_jump)
- `CombatReductionHandler`(magic/explosion) / `CombatSkillHandler`(axe_mastery/prepared) / `CombatHpHandler`(finisher/superiority)
- `MiningBreakHandler`(origin/industrialization/mass/essence/**vein_mining/break_cooldown/break_cooldown_large**) / `TorchLauncherHandler`
- `SleepHealHandler` / `AdvancedSkillHandler`(soul_offering/immortal) / `CriticalPointHandler`(臨界点超越) / `TurningPointHandler`(転換点)
- `MaceHandler`(auto_mace ＋ `gtcsolo:mace` エンチャント) / `AdvancedCombatHandler`(pure_firepower/executioner/curse_resonance/pure_endurance/train_legs_plus ＋ loose_ground の回復減少) / `AbundanceHandler`(abundance) / `EatingSkillHandler`(＋gluttony)
- `FlightMagicHandler`(flight_magic/flight_magic_advanced: 飛行能力付与・透明阻害・初級即死。 防御/防具強度倍率は `ProgressionAttributes`)
- `AdvancementXpHandler`(実績XP)
- `DIV.gtcsolo.combat.LuckyHitHandler`(lucky_hit_rate/damage 発火 + lucky_heal)
- `DIV.gtcsolo.mixin.EnchantLapisMixin`(enchant_lapis)

**エンチャント**: `DIV.gtcsolo.registry.ModEnchantments.MACE`（`gtcsolo:mace`、カテゴリ`MELEE_TOOL`＝有階ツール/採掘具、Lv1、効果は `MaceHandler` 共用）

**ネットワーク**（`network/`、`ModNetwork.CHANNEL`）
- S→C: `ProgressionStatePacket` / `ProgressionDefsPacket` / `ProgressionSync`
- C→S: `ProgressionBuyPacket`(ADVANCED_FULL時にアクションバー通知) / `ProgressionRefundPacket` / `ProgressionRespecPacket` / `ProgressionRequestPacket` / `ProgressionOpenPacket`
- 編集: `ProgressionEditEnablePacket` / `ProgressionEditPosPacket` / `ProgressionEditReqPacket`

**クライアント UI**（`client/`）
- `ProgressionScreen` / `ProgressionCanvasWidget` / `ProgressionNodeWidget` / `ProgressionIcons` / `ProgressionEditState` / `ClientProgressionData` / `ProgressionKeybind`(K)

**コマンド**: `GtcSoloCommand`（`/gtcsolo progression …`）

---

## 11. 既知の TODO / 要確認

- `skill_dash`（ダッシュ）/ `acrobatic`（アクロバティック）— **削除済み**（2026-06-04）。落下無効は `train_legs_plus` が担当。
- ローダ: **存在しない親への前提は読込時に自動除去**（子の永久ロックを防ぐ）＋ WARN ログ（親子リンクの正否確認用）。孤立 config override（datapack に無いノード）は無視。
- `max_health`（STAT, triangular）— **L1 で +0**。意図確認（初級1で効果0は不自然なら scaling/amount 調整）。
- 一括破壊／範囲破壊の CT を 0 まで下げると毎ブロックで大量破壊が走り得る（大規模cube特に重い）。負荷が問題なら下限tickを設ける余地。
- 上級スキルの「3/3」枠使用数を UI に表示していない（現状アクションバー通知のみ）。必要なら追加。
- 転換点群の変換単位は handler コメント上「仮」。バランス確認の余地あり。
- 実績XPのランプ係数・本来XP値（1000/300/100/400）は実環境調整値。要に応じ再調整。
