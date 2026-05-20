# StarForge 設計書 ver.0.6

> 継続更新: 仕様確定/変更があったらこのファイルを更新する。

---

## 1. 基本情報 (ver.0.1 から継承)

- **マシン名**: StarForge / SF / 恒星鍛造炉
- **外形**: 27³ の真球形マルチブロック (草案: `run/cmdex/export/StarForge.txt`)
- **コンセプト**: 星の一生をシミュレーションする機械
- **現行最強指標**:
  - 最大エネルギー消費 / 最大エネルギー生成
  - 最も加工時間の長いレシピを保有
  - 最大の出力アイテム種類数 / 出力数

---

## 2. レシピタイプ

- レシピタイプ: `gtcsolo:starforge` (1 つ)
- **スロット仕様** (実態):
  - アイテム入力 1 = 軌跡 (star_locus) のみ。 起動アイテム / 所定アイテム継続消費は別ハッチ経由 (実態ロジック側で扱う)
  - アイテム出力 16 / 液体入力 0 / 液体出力 8
- **GT recipe type 上の `setMaxIOSize`** (ver.0.6 更新): 入力 **6** / 出力 **16** / 流体 **0/8**
  - 入力 6 は JEI ダミー表示専用 (構築フェイズの進捗テーブル全 item を並べるため)
  - 実態 machine は GT recipe processor を使わず自前 tick で動作するので、 maxIO 値は JEI 表示上限にのみ作用する
- 入力: NBT 付き `gtcsolo:star_locus` (Trace で 8 軌跡識別、 レシピ稼働で消費)
- 軌跡ごとにロジック固有 (= レシピ稼働中の挙動が分岐する、 GT 標準のレシピデータには収まらない)
- 出力アイテム: **未確定** (後回し)

---

## 3. 軌跡分類

| 軌跡 | フェイズ構成 | 区分 | 特殊事項 |
|---|---|---|---|
| brown_dwarf  | 構築 → 崩壊        | 通常型     | — |
| koi74        | 構築 → 崩壊        | 通常型     | — |
| r_andromedae | 構築 → 崩壊        | 通常型     | — |
| hd101065     | 構築 → 崩壊        | 通常型     | — |
| cemp_r       | 構築 → 崩壊        | 通常型     | — |
| **sun**      | 構築 → **成熟** → 崩壊 | 成熟型 | ダイソンキューブ化 (継続発電) |
| neutron_star | 構築 → 崩壊        | 通常型     | — |
| **black_hole** | 構築 → **成熟** → 崩壊 | 成熟型 | 特殊: 崩壊度 + 過消費 + singularity 獲得チャンス |

---

## 4. 通常型レシピ (brown_dwarf / koi74 / r_andromedae / hd101065 / cemp_r / neutron_star)

### 4.1 構築フェイズ
- レシピは未稼働 (idle、 **進捗カウンタ**ゼロ状態)
- プレイヤーが対象アイテム群を搬入 → カウント加算
- 累計が **構築要求 count** に達した瞬間 **崩壊フェイズへ移行**

### 4.2 崩壊フェイズ
- レシピ稼働開始
- 対象アイテム群を継続供給して**さらに**カウントを加算
- 供給が止まると機械電源 OFF
- 累計が **崩壊要求 count** に達するまで稼働
- 累計達成 → レシピ履行完了 → 出力アイテム搬出

### 4.3 進捗テーブル (PhaseProgressionTable)
- 各フェイズに進捗テーブルを持たせ、 投入アイテム → count 加算値を判定
- **default 群**: 指定 Item ID 全部 = `+1`
- **Effective 群**: Predicate (NBT/Tag 等の特殊条件) で判定し、 任意値 `+N` を返す
- 評価順: Effective → default

#### 4.3.1 通常型 6 軌跡 共通テーブル (ver.0.5 初期値)
| 区分 | アイテム | count 加算 |
|---|---|---|
| default | `minecraft:stone` / `minecraft:cobblestone` / `minecraft:dirt` / `gtceu:stone_dust` | **+1** |
| Effective | **空** の `gtcsolo:star_locus` (NBT `Trace` 未設定) | **+1,296** |

- **構築要求 count**: **12,960**
- **崩壊要求 count**: **65,536** (構築完了後に追加で)
- 構築フェイズと崩壊フェイズで**同じテーブル**を使う (= 「親切表記なしで JEI に同じ表記を繰り返す」 設計判断)

### 4.4 JEI 表示 (= 自作 StarForgeInfoCategory)
- INPUT slot 群として default アイテム + Effective アイテムを並列表示
- Effective slot にカーソル合わせると tooltip に「Effective」 + 「count: +N」 が出る
- 親切な注釈 (「崩壊フェイズも同じ」 等) は付けず、 同じ表記を構築/崩壊で 2 回繰り返す

---

## 5. 成熟型: 太陽 (sun)

### 5.1 構築フェイズ
- 通常型と同様: 要求アイテム搬入で**成熟フェイズ**へ
- 進捗テーブルは未確定 (= 通常型同テーブル流用 or 専用テーブル) — 後日決定

### 5.2 成熟フェイズ (ダイソンキューブ化)
- **duration = 1200 秒 (= 24,000 tick)** で固定
- 全期間 **EU 消費 0** (レシピ動作にエネルギー不要)
- 期間中、 **UV Solar Panel** の搬入を受け付ける
- **搬入条件**: UV Solar Panel **328 個** 搬入完了
- **搬入完了後**: 完了時点 → 成熟終了 (= 1200 秒経過) まで **4,194,304 EU/t (= UHV 32A)** を継続放出
  - 搬入が遅いほど放出期間が短くなる (= player 操作速度で発電量変動)
  - 放出先は StarForge 専用 EnergyOutputHatch (§7.2 参照)
- 1200 秒経過 → 崩壊フェイズへ自動遷移

### 5.3 崩壊フェイズ
- 既存 outputItems (太陽分解物。 `StarForgeTraceData` 参照) を搬出
- レシピ外で確定出力:
  - **UV Solar Panel × 65 個** (= 搬入総数 328 の 2 割還元)
  - **decaying_star_locus (sun trace)** × 1

---

## 6. 成熟型: ブラックホール (black_hole) — 特殊

### 6.1 構築フェイズ
- 通常型と同様
- 進捗テーブル未確定 — 後日決定

### 6.2 成熟フェイズ
- **duration = 1200 tick (= 60 秒)** で固定
- EU 消費 0
- 期間中、 **物体投入**を受け付ける
- **物体投入 1 個**ごとに **8.59 GEU/t (= MAX+1V × 2A、 2^33) × 20 tick** 放出 (= 1 回放出セッション、 総 171.8G EU)
  - 複数投入で**連鎖**: N 個投入なら N × 20 tick の放出ウィンドウ (重なる場合は同時加算)
  - 1200 tick 内に最大 60 個まで物理的に放出可能 (60 × 20 = 1200)
- **物体**: `gtceu:neutronium_block` (= Neutronium Block、 重さ概念で最重量を採用) — ver.0.6 末で確定
- 1200 tick 経過 → 崩壊フェイズへ自動遷移

### 6.3 崩壊フェイズ — 崩壊度 + 過消費

#### 崩壊度 (Decay %)
- UI 表示パラメータ
- 初期値: **100%**
- 減衰: **毎秒 1% 低下**
- 0% に達した時点:
  - **過消費を見逃した場合**: **失敗判定** — 出力 0、 投入された星の軌跡 (= 構築フェイズで投入されたもの) も返却なし、 レシピ即終了
  - **過消費中の電源 OFF で singularity 獲得済の場合**: 上述処理で既終了済

#### 内部閾値 (ロジック内、 UI 非表示)
- ブラックホールレシピ稼働ごとに**ランダム生成**
- 値域: **1% 〜 92%** (= 0% 除外、 ver.0.6 訂正)
- 崩壊度が**内部値に達したとき**、 **過消費開始** (ver.0.6 訂正: 旧 spec の「+8%」 は内部値の意味を変えて簡略化)

#### 過消費 (Surge Consumption) — ver.0.6 大幅改訂
- 持続: **160 tick (= 8 秒)** = 8% 崩壊度減少分
- 電力内訳:
  - **最初 20 tick**: **MAX+14V × 2A** (= **2^62 ≈ 4.61 × 10^18 EU/t**)
  - **その後 140 tick**: **MAX+1V × 2A** (= **2^34 ≈ 1.72 × 10^10 EU/t**)
- 消費の挙動 (StarForge 専用 EnergyHatch 経由、 §7 参照):
  - 接続電線/WEN から**取れるだけエネルギー**を消費し続ける
  - 不足してもレシピは進行 (= player 側の電源確保責務)
- **過消費中にプレイヤーがマシン電源を切る** (controller の active OFF):
  - 即座にレシピ稼働停止
  - **gtcsolo:star_singularity 搬出** + 崩壊フェイズ即終了 (= 成功獲得)
- 過消費中に電源を切らずに 160 tick 経過 → 崩壊度はそのまま減衰継続、 0% で失敗判定

---

## 7. StarForge 専用 EnergyHatch

### 7.1 StarForgeEnergyHatch (Input)
- **WEN (Wireless Energy Network) 接続専用**
- 容量: **2,147,483,648 EU (= 2^31 = MAX 1A × 1 tick 分)**
- 通常時: 容量内で受け取り、 レシピへ供給
- 過消費時 (BH 崩壊フェイズ):
  - 接続電線から最大限消費 (容量無視で吸い上げ続ける、 特殊処理)
  - WEN ネットワークから tick あたり 容量分 (2.14G EU) 消費
- 既存 `gtcsolo:spaceforge_energy_hatch` 系の流儀を参考に新規実装

### 7.2 StarForgeEnergyOutputHatch (Output) — ver.0.6 追加、 実装可否考慮中
- 用途: SUN ダイソンキューブ放出 / BH 過消費前の蓄電 (未整理)
- 容量上限: **922 京 EU (= 9.22 × 10^18 ≈ 約 2^63 / Long.MAX_VALUE オーダー)**
- 出力制限: **2,147,483,648 EU/t (= 2^31)**
- 独自 PartAbility 化の要否は実装着手時に判定 (= 既存 IO Hatch 拡張で済むなら不要)

---

## 8. 設計書 ver.0.1 (前回) との整合性

- ver.0.1 §7「80tick」→ **160tick に訂正** (8% 崩壊度 = 160tick で確実性確保)
- ver.0.5 の「MAX+14V × 1A = 2^61」 → ver.0.6 で「MAX+14V × 2A」 に **2A 化** (前半 20 tick 限定、 残り 140 tick は MAX+1V × 2A)
- ver.0.5 の内部閾値「0〜92%、 +8% で発火」 → ver.0.6 で「**1〜92%、 直接発火**」 に簡略化

---

## 9. 設計判断 (実装方針)

- マルチブロック 1 つ + レシピタイプ 1 つ
- 軌跡ごとのロジック分岐: 通常型ベースクラス + 太陽サブクラス + ブラックホールサブクラス の 3 系
- 軌跡 8 種は recipe data に Trace を持たせて分岐
- StarForge 用 EnergyHatch (入出力 2 系統) は別マシン (新規実装、 WEN 接続必須)

---

## 9.5 JEI 設計 (二層構造)

### 9.5.1 自作 JEI ページ (メイン情報源)
- 独自カテゴリ `gtcsolo:starforge_info` を実装 (GTCEu の `MultiblockInfoCategory` をお手本)
- 軌跡ごとに 1 ページ (合計 8 ページ)
- 表示内容:
  - 軌跡名 + 区分 (通常型 / 成熟型)
  - **初期要求アイテム** (構築フェイズ)
  - **総消費アイテム** (崩壊フェイズの累計、 通常型のみ)
  - **フェイズ説明** (構築 → (成熟) → 崩壊 の流れ)
  - 太陽/ブラックホールは「特殊な稼働 (詳細は仕様書/ゲーム内挙動参照)」と注記
  - 出力アイテム (一覧)
- 実装層: `StarForgeInfoCategory` + `StarForgeInfoWrapper` + `StarForgeInfoWidget`

### 9.5.2 GT 標準レシピ JEI ページ (誘導用)
- レシピタイプ `gtcsolo:starforge` のレシピデータを 8 件登録 (ダミー)
- 表示内容:
  - 入力: Trace 付き star_locus
  - 出力: 各軌跡の出力アイテム (未確定だが入る)
  - EUt / duration:
    - 通常型: 「エネルギー消費なし」 と表記 (実態は所定アイテム継続消費)
    - 太陽 / BH: 「特殊なレシピ稼働が行われる」と表記
  - **自作 JEI ページへの誘導テキスト必須** (例: tooltip や description で「詳細は星の軌跡を右クリック」)

### 9.5.3 右クリックリンク (JEI 起動)
- ターゲット:
  - **StarForge コアブロック** (controller) を空手で右クリック → 該当 JEI ページ (どれを開く? 候補: 全カテゴリ表示)
  - **star_locus アイテム** を右クリック → 該当 Trace の自作 JEI ページ
- 実装:
  - `IModPlugin.onRuntimeAvailable()` で `IJeiRuntime` を static field に保存
  - `Item.use()` / `Block.useWithoutItem()` から `runtime.getRecipesGui().showTypes(List.of(RECIPE_TYPE))` 呼び出し
  - 既存 credit `CraftPatternJeiPlugin` + `JeiNavigation` を流儀の参考に (編集不可、 コピー流儀は OK)

---

## 10. 解決済み / 未確定事項

### 解決済み (Q1〜Q11)

- ✅ (Q1) 要求アイテムと所定アイテムは**別物**
- ✅ (Q2) 内部閾値 = レシピ稼働ごとのランダム値 (**1〜92%**)、 ver.0.6 で値域訂正 + 「+8%」 機構撤去
- ✅ (Q3) ver.0.1 の 80tick は誤記、 正しくは 160tick
- ✅ (Q4) EnergyHatch = WEN 専用、 過消費時は電線吸い尽くし + WEN 容量分
- ✅ (Q5) ソーラー = GTCEu UV Solar Panel
- ✅ (Q6) 太陽 成熟→崩壊 移行 = レシピ duration 経過 (= 1200 秒、 ver.0.6 確定)
- ✅ (Q7) 通常型「履行」 = 所定アイテムの累計消費量が要求値に到達
- ✅ (Q8) スロット仕様 = items 6/16, fluids 0/8 (ver.0.6 更新)、 JEI 二層 (自作 + GT 標準ダミー)、 右クリックリンク (controller / star_locus)
- ✅ (Q9) SUN 成熟 = 1200 秒、 UV Solar Panel × 328 搬入、 完了後 4.19M EU/t (UHV 32A) を成熟終了まで放出 (ver.0.6 確定)
- ✅ (Q10) BH 成熟 = 1200 tick、 物体 1 個 = 8.59 GEU/t × 20 tick × 1 回連鎖可、 物体は仮 cobble (ver.0.6 確定)
- ✅ (Q11) BH 過消費 = 20 tick MAX+14V × 2A + 140 tick MAX+1V × 2A、 失敗時は出力 0/星の軌跡返却なし (ver.0.6 確定)

### 未解決

- ✅ BH 成熟フェイズ「物体」 = `gtceu:neutronium_block` (Q12、 ver.0.6 末で確定)
- [ ] 出力アイテム (各軌跡通常型 — 既存 StarForgeTraceData の値を踏襲予定だが要確認)
- [ ] SUN/BH の構築フェイズ進捗テーブル (= 通常型と同テーブル流用 or 専用テーブル)
- [ ] starforge_energy_output_hatch を独自 PartAbility 化するかどうか
- [ ] 通常型: 所定アイテム + 累計消費量 + tick あたり消費レート (= 構築/崩壊で同一でいいか、 詳細値)

---

## 改訂履歴

- **ver.0.1**: 基本骨格、 3 フェイズ、 シンギュラリティ獲得、 tier 解放
- **ver.0.2** (2026-05-10): 通常型/成熟型分類、 太陽ダイソンキューブ、 ブラックホール崩壊度、 専用 EnergyHatch 概念
- **ver.0.3** (2026-05-10): Q1〜Q7 解決を反映 — フェイズ流れ明確化、 要求/所定アイテム別、 内部閾値ランダム値、 過消費 160tick、 EnergyHatch 詳細、 ver.0.1 80tick→160tick 訂正
- **ver.0.4** (2026-05-10): スロット仕様確定 (items 1/15, fluids 0/12)、 JEI 二層構造 (自作カテゴリ + GT 標準ダミー)、 右クリックリンク (controller / star_locus)、 §9.5 追加
- **ver.0.5** (2026-05-14): 通常型 6 軌跡の進捗テーブル仕様確定 (§4.3) — PhaseProgressionTable (default +1 / Effective +N)、 構築 12,960 / 崩壊 65,536 count、 共通テーブル (stone/cobble/dirt/gtceu:stone_dust + 空 star_locus +1,296)、 JEI Effective tooltip 仕様 (§4.4)、 BH 専用 singularity を gtcsolo:star_singularity 化
- **ver.0.6** (2026-05-14): SUN/BH 成熟フェイズ確定 (Q9/Q10) — SUN 1200 秒/UV Solar 328 個/UHV 32A 放出、 BH 1200 tick/物体投入連鎖式/MAX+1V × 2A × 20 tick × N、 BH 崩壊過消費の電力内訳訂正 (Q11) — 20 tick MAX+14V × 2A + 140 tick MAX+1V × 2A、 内部閾値値域 1〜92% に訂正 + 「+8%」 機構撤去、 失敗判定の挙動明文化、 starforge_energy_output_hatch (§7.2) 追加 (容量 922京/出力 2^31)、 setMaxIOSize を 6/16/0/8 に更新
