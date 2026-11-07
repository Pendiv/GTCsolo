package DIV.gtcsolo.client;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class ImeSearchTerms {
    private static final int MAX_ROMANIZATIONS = 8;
    private static final Map<String, List<String>> READINGS = readings();

    private ImeSearchTerms() {}

    /** Fold the fullwidth counterparts of printable ASCII and space. */
    private static String foldWidth(String text) {
        StringBuilder folded = null;
        for (int i = 0; i < text.length(); i++) {
            char original = text.charAt(i);
            char character = original;
            if (original >= '\uff01' && original <= '\uff5e') character = (char) (original - 0xfee0);
            if (original == '\u3000') character = ' ';
            if (folded == null && original != character) {
                folded = new StringBuilder(text.length()).append(text, 0, i);
            }
            if (folded != null) folded.append(character);
        }
        return folded == null ? text : folded.toString();
    }

    /** Alternatives to a single parsed search term. The caller retains the original term. */
    public static List<String> alternatives(String term) {
        String widthFolded = foldWidth(term);
        if (!hasKanaWithoutKanji(widthFolded)) {
            return widthFolded.equals(term) ? List.of() : List.of(widthFolded);
        }

        String kana = normalizeKana(widthFolded);
        var result = new LinkedHashSet<String>();
        result.add(widthFolded);
        result.add(kana);
        var variants = new ArrayList<StringBuilder>();
        variants.add(new StringBuilder(kana.length()));
        for (int i = 0; i < kana.length();) {
            char character = kana.charAt(i);
            if (character == 'ー') {
                appendLongVowel(variants);
                i++;
                continue;
            }

            boolean doubled = character == 'っ';
            int start = doubled ? i + 1 : i;
            int end = Math.min(kana.length(), start + 2);
            List<String> options = end - start == 2 ? READINGS.get(kana.substring(start, end)) : null;
            if (options == null && start < kana.length()) {
                end = start + 1;
                options = READINGS.get(kana.substring(start, end));
            }

            if (doubled) {
                var combinations = doubleConsonant(options);
                if (combinations.isEmpty()) {
                    options = READINGS.get("っ");
                    end = i + 1;
                } else options = combinations;
            }
            if (options == null) {
                for (var variant : variants) variant.append(character);
                i++;
            } else {
                appendReadings(variants, options);
                i = end;
            }
        }
        for (var variant : variants) result.add(variant.toString());
        result.remove(term);
        return List.copyOf(result);
    }

    private static boolean hasKanaWithoutKanji(String term) {
        boolean hasKana = false;
        for (int i = 0; i < term.length();) {
            int cp = term.codePointAt(i);
            if (cp > 127 && Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) return false;
            if ((cp >= 0x3041 && cp <= 0x30fa) || cp == 'ー' || (cp >= 0xff66 && cp <= 0xff9f)) hasKana = true;
            i += Character.charCount(cp);
        }
        return hasKana;
    }

    private static String normalizeKana(String text) {
        var result = new StringBuilder(text.length());
        for (int i = 0; i < text.length();) {
            char character = text.charAt(i);
            if (character >= '\uff66' && character <= '\uff9f') {
                int end = i + 1;
                while (end < text.length() && text.charAt(end) >= '\uff66' && text.charAt(end) <= '\uff9f') end++;
                result.append(Normalizer.normalize(text.substring(i, end), Normalizer.Form.NFKC));
                i = end;
            } else {
                result.append(character);
                i++;
            }
        }
        String composed = Normalizer.normalize(result, Normalizer.Form.NFC);
        result.setLength(0);
        for (int i = 0; i < composed.length(); i++) {
            char character = composed.charAt(i);
            if (character >= 'ァ' && character <= 'ヶ') character -= 0x60;
            result.append(character);
        }
        return result.toString();
    }

    private static void appendReadings(List<StringBuilder> variants, List<String> options) {
        int originals = variants.size();
        for (int option = 1; option < options.size() && variants.size() < MAX_ROMANIZATIONS; option++) {
            for (int i = 0; i < originals && variants.size() < MAX_ROMANIZATIONS; i++) {
                variants.add(new StringBuilder(variants.get(i)).append(options.get(option)));
            }
        }
        for (int i = 0; i < originals; i++) variants.get(i).append(options.get(0));
    }

    private static void appendLongVowel(List<StringBuilder> variants) {
        int originals = variants.size();
        for (int i = 0; i < originals && variants.size() < MAX_ROMANIZATIONS; i++) {
            var variant = variants.get(i);
            if (variant.isEmpty()) continue;
            char previous = variant.charAt(variant.length() - 1);
            if ("aeiou".indexOf(previous) >= 0) variants.add(new StringBuilder(variant).append(previous));
        }
        for (int i = 0; i < originals; i++) variants.get(i).append('-');
    }

    private static List<String> doubleConsonant(List<String> options) {
        if (options == null) return List.of();
        var result = new ArrayList<String>();
        for (String reading : options) {
            char first = reading.charAt(0);
            if ("aeiounxl".indexOf(first) < 0) result.add(first + reading);
        }
        if (!result.isEmpty() && options.get(0).startsWith("ch")) result.add("t" + options.get(0));
        return result;
    }

    private static Map<String, List<String>> readings() {
        String table = """
                あ:a い:i う:u え:e お:o
                か:ka,ca き:ki く:ku,cu,qu け:ke こ:ko,co
                さ:sa し:shi,si,ci す:su せ:se,ce そ:so
                た:ta ち:chi,ti つ:tsu,tu て:te と:to
                な:na に:ni ぬ:nu ね:ne の:no
                は:ha ひ:hi ふ:fu,hu へ:he ほ:ho
                ま:ma み:mi む:mu め:me も:mo
                や:ya ゆ:yu よ:yo
                ら:ra り:ri る:ru れ:re ろ:ro
                わ:wa ゐ:wi ゑ:we を:wo,o ん:n,nn,n'
                が:ga ぎ:gi ぐ:gu げ:ge ご:go
                ざ:za じ:ji,zi ず:zu ぜ:ze ぞ:zo
                だ:da ぢ:di,ji づ:du,zu で:de ど:do
                ば:ba び:bi ぶ:bu べ:be ぼ:bo
                ぱ:pa ぴ:pi ぷ:pu ぺ:pe ぽ:po ゔ:vu
                ぁ:xa,la ぃ:xi,li ぅ:xu,lu ぇ:xe,le ぉ:xo,lo
                ゃ:xya,lya ゅ:xyu,lyu ょ:xyo,lyo っ:xtu,ltu,xtsu
                ゎ:xwa,lwa ゕ:xka ゖ:xke
                きゃ:kya きゅ:kyu きょ:kyo きぇ:kye
                ぎゃ:gya ぎゅ:gyu ぎょ:gyo ぎぇ:gye
                しゃ:sha,sya しゅ:shu,syu しょ:sho,syo しぇ:she,sye
                じゃ:ja,jya,zya じゅ:ju,jyu,zyu じょ:jo,jyo,zyo じぇ:je,jye,zye
                ちゃ:cha,tya,cya ちゅ:chu,tyu,cyu ちょ:cho,tyo,cyo ちぇ:che,tye,cye
                にゃ:nya にゅ:nyu にょ:nyo にぇ:nye
                ひゃ:hya ひゅ:hyu ひょ:hyo ひぇ:hye
                びゃ:bya びゅ:byu びょ:byo びぇ:bye
                ぴゃ:pya ぴゅ:pyu ぴょ:pyo ぴぇ:pye
                みゃ:mya みゅ:myu みょ:myo みぇ:mye
                りゃ:rya りゅ:ryu りょ:ryo りぇ:rye
                ぢゃ:dya ぢゅ:dyu ぢょ:dyo
                ふぁ:fa ふぃ:fi ふぇ:fe ふぉ:fo ふゅ:fyu
                ゔぁ:va ゔぃ:vi ゔぇ:ve ゔぉ:vo ゔゅ:vyu
                うぃ:wi,whi うぇ:we,whe うぉ:wo,who いぇ:ye
                くぁ:qa,kwa くぃ:qi,kwi くぇ:qe,kwe くぉ:qo,kwo
                ぐぁ:gwa ぐぃ:gwi ぐぇ:gwe ぐぉ:gwo
                すぃ:swi ずぃ:zwi
                てぃ:ti,thi てゅ:thu でぃ:di,dhi でゅ:dhu
                とぅ:tu,twu どぅ:du,dwu
                つぁ:tsa つぃ:tsi つぇ:tse つぉ:tso
                """;
        var result = new HashMap<String, List<String>>();
        for (String entry : table.strip().split("\\s+")) {
            int separator = entry.indexOf(':');
            result.put(entry.substring(0, separator), List.of(entry.substring(separator + 1).split(",")));
        }
        return Map.copyOf(result);
    }
}
