# 地雷原 (PITFALLS) — 不測の事態が起きたらまずここを見る

> **これは何**: 過去に実際に踏んだ地雷の「症状 → 原因 → 対処」集。
> 変なエラー・原因不明の挙動・クラッシュに遭ったら、**調査を始める前にここを一巡**する。
> 新しい地雷を踏んだら (解決したら) その場で追記する。仕様の意図的な罠は §5。

---

## 1. ビルド / 実行環境

| 症状 | 原因 | 対処 |
|---|---|---|
| Bash から gradlew を打つと不発・変な引数エラー | MSYS が `/c` 等をパス変換する | **gradle は PowerShell で打つ**: `.\gradlew.bat compileJava` / `:battle:compileJava` / `:credit:devJar` |
| runClient が稀に mod loading で落ちる (`LHKJSPlugin.clearCaches:44` ConcurrentModificationException) | L2Hostility×KubeJS init のレース。gtcsolo 無関係 | **再起動すれば通る**。コードを疑わない |
| dev で `NoSuchMethodError` (SRG 名) | **本番用 (reobf) jar を run/mods に入れた**。dev は official mappings | dev に入れてよいのは `devJar` (official 名) のみ。かつサブプロ相乗りと併用しない (二重登録) |
| dev で設定ボタン押すとクラッシュ (`NoSuchFieldError: f_96543_`) | weapon-leveling 同梱 MidnightLib の mixin が dev 非対応 | `libs/midnightlib-1.4.2-forge.jar` が回避策。**このjarを消すと再発** |
| mixin が丸ごと効かない | Forge 1.20.1 は mods.toml の mixin 宣言を dev で読まない | `Gtcsolo` コンストラクタの `Mixins.addConfiguration("gtcsolo.mixins.json")` が正規経路。消さない |
| battle 単体で runClient できない | gtceu が AE2 等を実行時要求、battle 依存に工業 dep なし | **常に gtcsolo と合体ロード** |
| ゲームが変な壊れ方 / jar ロック | ATG パック起動中に rebuild した | **ゲーム起動中は rebuild しない** |

## 2. GTCEu (1.6.4)

| 症状 | 原因 | 対処 |
|---|---|---|
| 起動時 `Registry entry not present` クラッシュ | `GTBlocks.X.get()` 等を **init 時に即時評価**した | ブロック解決は **pattern lambda / Supplier 内で遅延**。既存マシン定義に倣う (fantasy_builder で実績あり) |
| 使った API メソッドが存在しない | 別バージョンの GT の記憶で書いた (例: 7.4.1 の `workableCasingModel()` ≠ 1.6.4 の `workableCasingRenderer()`) | API 確認は **必ず 1.6.4** (`MODsUnComplessed/old/GregTech-Modern` / `libs/gtceu-1.20.1-1.6.4.jar` を javap) |
| static final Map に ModMaterials を入れたら null | Material は registry 凍結後に生成される。**クラス初期化時の static 捕捉が早すぎる** | 素材マップは **メソッド内で構築** (GtcSoloAddon.wenWireMaterials が手本) |
| レシピは正しいのに JEI に出ない / 1件だけ出る | **KubeJS の広域 wipe (`output:'@mod'`) に自前 GT レシピが巻き添え** | `run/logs/latest.log` の "Debug output of all removed recipes" を確認 → wipe に `not: { type: ... }` 除外。詳細=メモリ [KubeJS Wipe Collision] |
| rtui を置いたら UI が丸ごと死ぬ | 構造の一部省略・型アノテーション欠落・二重 `""` ラップ。例外は握り潰されサイレント死 | `refs/claude/2026-05-14_rtui_authoring.txt` 参照。`setMaxIOSize`/progressbar とスロット整合も必須 |
| 後付け chemical cap のスロットが JEI に出ない | custom rtui が item/fluid/EU しか知らない | `GtcSoloAddon.disableCustomUIForChemicalCompatibleTypes` (reflection で customUICache 無効化) が対処している。**GT 更新時に折れやすい箇所** |

## 3. KubeJS / Rhino

| 症状 | 原因 | 対処 |
|---|---|---|
| `redeclaration of var json` | Rhino では `json` 識別子が `recipe.json` と衝突 | **`json` という変数名を使わない** (`data`/`parsed` 等に) |
| 文字列が `mirror_Function: ConsString` みたいに壊れる | ConsString に **template literal** を使った | **string concat (`'a' + x`) 一択**。Java 由来文字列は `String(x)` で包む |

## 4. 戦闘分離 (gtcsolo ↔ ATGB) まわり

| 症状 | 原因 | 対処 |
|---|---|---|
| mob 特性が一切発動しない | **ATGB の trait 登録が現在コメントアウト中** (`Allthegregtechbattle.java:55`、Registrate クラッシュの応急処置) | 既知。本修正は battle レーンの宿題 (メモリ [Open Threads]) |
| NBT キーを allthegregtechbattle_ に直したくなる | rebrand の誘惑 | **NBT キーは `gtcsolo_` 据置が仕様** (`gtcsolo_arrow` 等)。既存ワールド互換のため触らない |
| 旧ワールドで unknown ブロックの BE 警告 | gtcsolo 側 MediatorField BE を廃止した (2026-07-02) | 無害。gtcsolo:unknown は通常ブロック、罠は battle 側のみ |

## 5. 仕様であってバグではない (直すな)

- **StarForge BH の過消費はエネルギー実消費を検証しない** — 「異常消費がサインとして出ている間に電源を切れ」のタイミングゲーム (2026-07-02 教主確定)
- **ExtendEnergyCube の FE 表示が常に Integer.MAX_VALUE clamp** — 内部 long の割り切り (変更は教主判断待ち)
- **疑似プラズマが Material Flag でなく別素材+`forge:plasma` タグ** — MaterialFlag.plasma() の不具合回避

## 6. battle 管轄の地雷 (合体運用で踏み得る)

L2Hostility 系 (postInit 内 setTrait 無限ループ / `MobTraitCap.HOLDER.get` の NoSuchElementException 等) は **battle 側の refs / Claude メモリが正**。戦闘の異常はまず battle 側を引く。

---

**迷ったら**: それでも原因不明なら、憶測で直す前に教主に一言確認 (これがこのファイルの本旨)。
