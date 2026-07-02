package DIV.gtcsolo.manual;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.TextBoxWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.xkmc.l2hostility.content.config.TraitConfig;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * マニュアル (解説書) UI — LDLib ModularUI 製。 左=章リスト、 右=本文の古風な 2 ペイン構成。
 *
 * <p><b>章の追加方法</b>: {@link #CHAPTERS} に id を足し、 lang に 2 キーを足すだけ:
 * {@code manual.gtcsolo.chapter.<id>} (章タイトル) / {@code manual.gtcsolo.chapter.<id>.body} (本文)。
 *
 * <p><b>特性章</b>は特別扱いで、 さらにサブリスト (全体について + 個別特性) + 詳細ペインに分かれる。
 * 個別特性は L2Hostility の trait レジストリ ({@code LHTraits.TRAITS}) から<b>自動生成</b>するため、
 * 特性を追加すれば勝手に図鑑へ載る。 各詳細は 特性色付きの日英併記タイトル / 数値データ
 * (出現Lv・最大ランク・コスト・重み = {@link TraitConfig}) / 説明文 ({@code addDetail} で %s 充填済み)。
 *
 * <p>表示文字列はクライアント側でのみ組み立てる (server 側は構造一致のため key を入れるだけ)。
 * ペイン切替は全ペインを同位置に重ねて可視切替 (動的 add/remove の同期回避)。 ボタン押下は
 * LDLib が両 side で呼ぶため client/server の可視状態は自然に一致する。
 */
public final class ManualUI {

    /** 章 id (表示順)。 traits は特性図鑑として特別扱い。 */
    private static final String[] CHAPTERS = {
            "traits",   // 特性について (図鑑)
            "world",    // この世界について
            "wen",      // WENについて
    };

    private static final int BASE_WIDTH = 384;
    private static final int BASE_HEIGHT = 230;
    private static final int CHAPTER_W = 72;   // 左: 章リスト幅
    private static final int TRAIT_LIST_W = 112; // 特性章: サブリスト幅
    private static final int PAD = 8;

    private ManualUI() {}

    public static ModularUI create(IUIHolder holder, Player player) {
        boolean remote = player.level().isClientSide();

        // 開いた時点の画面サイズに追従して拡大する (クライアントのみ実サイズ、 サーバは名目値。
        // 位置/サイズは side 間で同期されないので構造さえ一致すればずれて良い)。
        int width = BASE_WIDTH;
        int height = BASE_HEIGHT;
        if (remote) {
            var window = net.minecraft.client.Minecraft.getInstance().getWindow();
            width = Math.max(BASE_WIDTH, Math.min(window.getGuiScaledWidth() - 60, 560));
            height = Math.max(BASE_HEIGHT, Math.min(window.getGuiScaledHeight() - 40, 340));
        }

        WidgetGroup root = new WidgetGroup(0, 0, width, height);
        root.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);

        int paneX = PAD + CHAPTER_W + 4;
        int paneW = width - paneX - PAD;
        int paneH = height - PAD * 2;

        // 章ごとの本文ペイン (同位置に重ね、 選択章のみ可視化)
        Map<String, WidgetGroup> panes = new LinkedHashMap<>();
        for (String id : CHAPTERS) {
            if (id.equals("traits")) {
                panes.put(id, buildTraitsPane(paneX, PAD, paneW, paneH, remote));
            } else {
                DraggableScrollableWidgetGroup pane = scrollPane(paneX, PAD, paneW, paneH);
                pane.addWidget(new TextBoxWidget(4, 4, paneW - 14,
                        List.of("manual.gtcsolo.chapter." + id + ".body"))
                        .setFontColor(0xffdddddd));
                panes.put(id, pane);
            }
        }

        // 左: 章ボタン (縦並び)
        List<WidgetGroup> paneList = new ArrayList<>(panes.values());
        int i = 0;
        for (String id : panes.keySet()) {
            final int index = i++;
            root.addWidget(new ButtonWidget(PAD, PAD + index * 22, CHAPTER_W, 18,
                    new GuiTextureGroup(
                            ResourceBorderTexture.BUTTON_COMMON,
                            new TextTexture("manual.gtcsolo.chapter." + id, -1)),
                    click -> selectOnly(paneList, index)));
        }

        for (WidgetGroup pane : paneList) {
            root.addWidget(pane);
        }
        selectOnly(paneList, 0);

        return new ModularUI(root, holder, player);
    }

    // ---- 特性図鑑 -----------------------------------------------------------

    /** 特性章: 左=サブリスト (全体 + 全特性)、 右=詳細。 trait レジストリから自動生成。 */
    private static WidgetGroup buildTraitsPane(int x, int y, int w, int h, boolean remote) {
        WidgetGroup group = new WidgetGroup(x, y, w, h);

        // 出現レベル昇順 → id で安定ソート (client/server で同一順序になること)
        List<MobTrait> traits = new ArrayList<>(LHTraits.TRAITS.get().getValues());
        traits.sort(Comparator
                .comparingInt((MobTrait t) -> config(t).min_level)
                .thenComparing(t -> t.getRegistryName().toString()));

        if (remote) {
            Set<String> namespaces = new HashSet<>();
            namespaces.add("gtcsolo");
            for (MobTrait t : traits) namespaces.add(t.getRegistryName().getNamespace());
            ManualLangs.ensureLoaded(namespaces);
        }

        int detailX = TRAIT_LIST_W + 4;
        int detailW = w - detailX;

        // 詳細ペイン群 (index 0 = 全体について)
        List<WidgetGroup> details = new ArrayList<>();
        DraggableScrollableWidgetGroup overview = scrollPane(detailX, 0, detailW, h);
        overview.addWidget(new TextBoxWidget(4, 4, detailW - 14,
                List.of("manual.gtcsolo.chapter.traits.body")).setFontColor(0xffdddddd));
        details.add(overview);
        for (MobTrait trait : traits) {
            details.add(buildTraitDetail(detailX, detailW, h, trait, remote));
        }

        // サブリスト (スクロール)
        DraggableScrollableWidgetGroup list = scrollPane(0, 0, TRAIT_LIST_W, h);
        list.addWidget(listButton(0, "manual.gtcsolo.trait.overview", -1, details));
        for (int i = 0; i < traits.size(); i++) {
            MobTrait trait = traits.get(i);
            list.addWidget(listButton(i + 1, trait.getDescriptionId(),
                    0xff000000 | trait.getColor(), details));
        }

        group.addWidget(list);
        for (WidgetGroup detail : details) {
            group.addWidget(detail);
        }
        selectOnly(details, 0);
        return group;
    }

    /**
     * サブリストの 1 ボタン (特性色付きラベル)。
     * 不透明ボタン地に色文字は醜いので、 薄い半透明の行地 + 色文字のフラットな見た目にする。
     */
    private static Widget listButton(int index, String labelKey, int color, List<WidgetGroup> details) {
        return new ButtonWidget(2, 2 + index * 18, TRAIT_LIST_W - 12, 16,
                new GuiTextureGroup(
                        new ColorRectTexture(0x20ffffff),
                        new TextTexture(labelKey, color)),
                click -> selectOnly(details, index));
    }

    /** 特性 1 件の詳細ペイン: 色付き日英タイトル / 数値データ / 説明。 */
    private static WidgetGroup buildTraitDetail(int x, int w, int h, MobTrait trait, boolean remote) {
        DraggableScrollableWidgetGroup pane = scrollPane(x, 0, w, h);
        int textW = w - 14;
        int cursor = 4;

        // タイトル (特性色、 日英併記)。 表示文字列は client でのみ実体化する。
        String title = remote ? esc(ManualLangs.dual(trait.getDescriptionId())) : trait.getDescriptionId();
        TextBoxWidget titleBox = new TextBoxWidget(4, cursor, textW, List.of(title));
        titleBox.setFontColor(0xff000000 | trait.getColor());
        pane.addWidget(titleBox);
        cursor += Math.max(titleBox.getSize().height, 10) + 6;

        // 数値データ行 (出現Lv / 最大ランク / コスト / 重み)
        TraitConfig cfg = config(trait);
        String data = remote
                ? esc(I18n.get("manual.gtcsolo.trait.data", cfg.min_level, cfg.max_rank, cfg.cost, cfg.weight))
                : "manual.gtcsolo.trait.data";
        TextBoxWidget dataBox = new TextBoxWidget(4, cursor, textW, List.of(data));
        dataBox.setFontColor(0xffe0c060);
        pane.addWidget(dataBox);
        cursor += Math.max(dataBox.getSize().height, 10) + 8;

        // 説明 (addDetail 経由 = %s 充填済み)。 client のみ実体化。
        List<String> desc = remote ? resolveDesc(trait) : List.of(trait.getDescriptionId() + ".desc");
        pane.addWidget(new TextBoxWidget(4, cursor, textW, desc).setFontColor(0xffd0d0d0));
        return pane;
    }

    /** 説明文の解決: addDetail (引数充填済み) → 無ければ desc キー → それも無ければ未執筆表記。 */
    private static List<String> resolveDesc(MobTrait trait) {
        try {
            List<Component> lines = new ArrayList<>();
            trait.addDetail(lines);
            List<String> result = lines.stream().map(Component::getString)
                    .filter(s -> !s.isBlank()).map(ManualUI::esc).toList();
            if (!result.isEmpty()) return result;
        } catch (Exception ignored) {
            // addDetail が side 依存などで落ちる trait は素の desc キーへフォールバック
        }
        String descKey = trait.getDescriptionId() + ".desc";
        return List.of(I18n.exists(descKey) ? descKey : "manual.gtcsolo.trait.nodesc");
    }

    /**
     * TextBoxWidget は渡した文字列を再度 String.format に通すため、 解決済み文字列に裸の {@code %}
     * (例: 博打の「20%」) が残っていると IllegalFormat → "Format error:" 表示になる。 {@code %%} に逃がす。
     */
    private static String esc(String s) {
        return s.replace("%", "%%");
    }

    private static TraitConfig config(MobTrait trait) {
        TraitConfig cfg = trait.getConfig();
        return cfg != null ? cfg : TraitConfig.DEFAULT;
    }

    // ---- 共通部品 -----------------------------------------------------------

    private static DraggableScrollableWidgetGroup scrollPane(int x, int y, int w, int h) {
        DraggableScrollableWidgetGroup pane = new DraggableScrollableWidgetGroup(x, y, w, h);
        // 半透明の暗色地 (= 特性色の文字が映える)。 不透明の bordered 地は色付きと喧嘩するのでやめた。
        pane.setBackground(new ColorRectTexture(0x40000000));
        pane.setYScrollBarWidth(4);
        return pane;
    }

    /** 指定 index のペインのみ可視化・有効化する。 */
    private static void selectOnly(List<WidgetGroup> panes, int index) {
        for (int i = 0; i < panes.size(); i++) {
            boolean selected = i == index;
            panes.get(i).setVisible(selected);
            panes.get(i).setActive(selected);
        }
    }
}
