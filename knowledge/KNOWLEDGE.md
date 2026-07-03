# KNOWLEDGE — 保存記憶 (セッションを跨ぐ確定知識・決定事項の積み上げ)

> **これは何**: プロジェクトの「確定した知識・教主の決定・覆してはいけない仕様」を日付付きで**追記していく**台帳。
> 日記 (`refs/claude/YYYY-MM-DD_diary.txt` = セッションの生の発見ログ) とは別物。ここには**煮詰まった結論だけ**を書く。
>
> **運用ルール**:
> 1. 追記式。過去エントリは書き換えず、覆った時は新エントリで上書き宣言する
> 2. 詳細は docs/ や refs/ へのリンクで済ませ、ここには結論と根拠の所在だけ
> 3. 大きな決定・仕様確定・「二度と間違えたくない事実」が生まれたらその場で追記
>
> **迷子になったらまず読む3枚**: 本ファイル → `refs/WORKSPACE_MAP.md` (どこに何があるか) → `refs/REFS_MAP.md` (refs資料の索引)

---

## 2026-07-02 — 初期エントリ (メモリ全面刷新・コード全域監査の日)

### プロジェクトの現在形
- **GTCsoloEU = 工業MOD (gtcsolo)**。戦闘は 2026-06-18 に **allthegregtechbattle (ATGB, `:battle`)** へ分割済み。合体 runClient が通常運用 (battle 単体起動は不可)。分離の全記録 = `refs/ops/battle_migration_status.md`
- 自作 modpack **AllTheGregTecj (ATG)** 専用。汎用配布は考慮しない (教主宣言)
- GTCEu **1.6.4 固定**。API確認は `MODsUnComplessed/old/GregTech-Modern` と `libs/gtceu-1.20.1-1.6.4.jar`

### 覆してはいけない仕様・決定 (教主確定)
- **StarForge BH の過消費はエネルギー実消費を検証しない** — 「異常消費がサインとして出ている間に電源を切れ」というタイミングゲーム。バグとして修正しないこと (2026-07-02 教主明言)
- **NBT キーは `gtcsolo_` 据置** (`gtcsolo_arrow` 等)。battle への rebrand でも触らない
- **MediatorField**: 罠機能は battle 側のみ。gtcsolo の `gtcsolo:unknown` は無害な通常ブロック (SINGULARITY_MAKER 構造材) — 2026-07-02 分離実施 (commit `78f0a42`)
- 両 mod 合体時に機能が一切削がれないこと (分離時の厳命)
- `libs/midnightlib-1.4.2-forge.jar` を消すな (dev クラッシュ再発)

### 場所の要点
- KubeJS 実コード = `run/kubejs/` / サブプロ = `MODs/` (battle/cmdex/credit/DumpAll ほか) / 他MODソース = `MODsUnComplessed` (modsu、綴り注意)
- Claude メモリ: gtcsolo 側 `~/.claude/projects/C--MODs-GTCsoloEU/memory/`、**battle 側は独立メモリを持つ** (戦闘知識はそちらが正)
- 稼働中ゲームの観測/操作 = Synapse (`http://127.0.0.1:25599`、token は `~/.synapse/token`、まず `GET /manifest`)

### 2026-07-02 に確定した事実
- メモリ全面刷新 (旧状態は `memory-backup-20260702/`)。呼称 = 教主、略称 ATG=modpack / ATGB=battle MOD
- gtcsolo 全 135 ファイル精読レビュー実施 → devログ一掃 `4617fa2` / 小バグ4件修正 `1f7ca4d` / WEN・レシピ表の重複整理 `b28980b`。残項目はメモリ [Open Threads]
- 戦闘分離 cleanup コミット `3fb5442` (373 files)。credit 並行作業終了・編集禁止解除
- カスタム元素 56 種が陽子110/中性子160 で同一 → GT 自動生成レシピの加工時間が均一になるだけ (実害なし、調整は教主判断待ち)
- refs/ 区画整理: `code/` (GT_/ST_/fec_/decompiled/Tier) と `ops/` (運用台帳) 新設。本 knowledge/ フォルダと CLAUDE.md も同日新設
