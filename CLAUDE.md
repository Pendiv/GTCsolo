# GTCsoloEU — 入口ガイド

GTCEu 1.6.4 (MC 1.20.1 Forge) の大型アドオン **gtcsolo (工業)** + 親Gradle。
戦闘は別MOD **allthegregtechbattle** (`MODs/allthegregtechbattle`, サブプロ `:battle`) に分割済みで、常に合体ロードする。

## 何もかも忘れていたら、この順に読む

1. **`knowledge/KNOWLEDGE.md`** — 保存記憶。確定知識・覆してはいけない決定の積み上げ台帳
2. **`refs/WORKSPACE_MAP.md`** — ワークスペース全域の「どこに何があるか」
3. **`refs/REFS_MAP.md`** — refs/ 配下の参考資料の内容索引

## 最低限の掟

- GTCEu API は **1.6.4** で確認 (`MODsUnComplessed/old/GregTech-Modern` / `libs/gtceu-1.20.1-1.6.4.jar`)
- KubeJS の実コードは `run/kubejs/`
- 戦闘系 (trait/arrow/progression/Fantasy装備) の作業は battle 側レーン。battle は独自の refs と独自の Claude メモリを持つ
- 大きな決定・仕様確定が生まれたら `knowledge/KNOWLEDGE.md` に追記する
- ゲーム (ATGパック) 起動中に rebuild しない
