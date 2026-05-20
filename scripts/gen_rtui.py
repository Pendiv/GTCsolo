"""Generate rtui binary NBT files for gtcsolo recipe types.

Resources block is copied verbatim from a working sample (qce) to avoid silent
LDLib failures from missing editor resource defaults.
"""
import re
import math
import nbtlib
import os
from nbtlib import Compound, List, String, Byte, Int, Float
from nbtlib.tag import End


def parse_sample_snbt(path):
    """Parse a samples-style SNBT (newline-separated, no commas) into nbtlib."""
    with open(path) as f:
        text = f.read()
    text = "{" + text + "}"
    lines = text.split("\n")
    out = []
    for i, line in enumerate(lines):
        s = line.rstrip()
        if not s:
            continue
        nxt = ""
        for j in range(i + 1, len(lines)):
            ss = lines[j].strip()
            if ss:
                nxt = ss[0]
                break
        last = s[-1]
        if last in "{[,:":
            out.append(s)
        elif nxt in "}]":
            out.append(s)
        else:
            out.append(s + ",")
    txt = "\n".join(out)
    txt = re.sub(r",(\s*[}\]])", r"\1", txt)
    txt = re.sub(
        r"^(\s*)([a-zA-Z_][a-zA-Z0-9_.]*(?:\s+[a-zA-Z_][a-zA-Z0-9_.!]*)+)(\s*):",
        lambda m: f'{m.group(1)}"{m.group(2)}":',
        txt,
        flags=re.M,
    )
    txt = re.sub(r":\s*(Hello [^,\n}]*)", lambda m: ': "' + m.group(1).strip() + '"', txt)
    return nbtlib.parse_nbt(txt)


# resources は空でも動くことを実機確認済み (no sample copy)
sample_resources = Compound({})


def slot_bg_tex(image_path, img_w=18, img_h=18, border=1):
    return Compound({
        "data": Compound({
            "offsetX": Float(0.0), "imageWidth": Float(1.0),
            "yOffset": Float(0.0), "xOffset": Float(0.0),
            "offsetY": Float(0.0), "color": Int(-1),
            "rotation": Float(0.0),
            "borderSize": Compound({"width": Int(border), "height": Int(border)}),
            "scale": Float(1.0),
            "imageSize": Compound({"width": Int(img_w), "height": Int(img_h)}),
            "imageHeight": Float(1.0),
            "imageLocation": String(image_path),
        }),
        "type": String("border_texture"),
    })


def item_slot(x, y, slot_id, w=18, h=18):
    return Compound({
        "data": Compound({
            "selfPosition": Compound({"x": Int(x), "y": Int(y)}),
            "canPutItems": Byte(1),
            "size": Compound({"width": Int(w), "height": Int(h)}),
            "drawHoverOverlay": Byte(1),
            "tooltipTexts": List[End](),
            "backgroundTexture": slot_bg_tex("gtceu:textures/gui/base/slot.png"),
            "canTakeItems": Byte(1),
            "id": String(slot_id),
            "drawBackgroundWhenHover": Byte(1),
            "align": String("NONE"),
            "drawHoverTips": Byte(1),
        }),
        "type": String("item_slot"),
    })


def fluid_slot(x, y, slot_id, w=18, h=18, border=1):
    return Compound({
        "data": Compound({
            "selfPosition": Compound({"x": Int(x), "y": Int(y)}),
            "fillDirection": String("ALWAYS_FULL"),
            "tooltipTexts": List[End](),
            "showAmount": Byte(1),
            "allowClickDrained": Byte(1),
            "align": String("NONE"),
            "allowClickFilled": Byte(1),
            "drawHoverTips": Byte(1),
            "size": Compound({"width": Int(w), "height": Int(h)}),
            "drawHoverOverlay": Byte(1),
            "backgroundTexture": slot_bg_tex(
                "gtceu:textures/gui/base/fluid_slot.png", border=border),
            "id": String(slot_id),
            "drawBackgroundWhenHover": Byte(1),
        }),
        "type": String("fluid_slot"),
    })


def progress_widget(x, y, w=20, h=20):
    img = "gtceu:textures/gui/progress_bar/progress_bar_arrow.png"
    progress_tex = Compound({
        "data": Compound({
            "yOffset": Float(0.0), "xOffset": Float(0.0),
            "fillDirection": String("LEFT_TO_RIGHT"),
            "rotation": Float(0.0), "scale": Float(1.0),
            "filledBarArea": Compound({
                "data": Compound({
                    "offsetX": Float(0.0), "imageWidth": Float(1.0),
                    "yOffset": Float(0.0), "xOffset": Float(0.0),
                    "offsetY": Float(0.5), "color": Int(-1),
                    "rotation": Float(0.0), "scale": Float(1.0),
                    "imageHeight": Float(0.5),
                    "imageLocation": String(img),
                }),
                "type": String("resource_texture"),
            }),
            "emptyBarArea": Compound({
                "data": Compound({
                    "offsetX": Float(0.0), "imageWidth": Float(1.0),
                    "yOffset": Float(0.0), "xOffset": Float(0.0),
                    "offsetY": Float(0.0), "color": Int(-1),
                    "rotation": Float(0.0), "scale": Float(1.0),
                    "imageHeight": Float(0.5),
                    "imageLocation": String(img),
                }),
                "type": String("resource_texture"),
            }),
        }),
        "type": String("progress_texture"),
    })
    return Compound({
        "data": Compound({
            "selfPosition": Compound({"x": Int(x), "y": Int(y)}),
            "size": Compound({"width": Int(w), "height": Int(h)}),
            "progressTexture": progress_tex,
            "tooltipTexts": List[End](),
            "id": String("progress"),
            "drawBackgroundWhenHover": Byte(1),
            "align": String("NONE"),
        }),
        "type": String("progress"),
    })


def build_data(children, width, height, recipe_type):
    root = Compound({
        "layout": String("NONE"),
        "selfPosition": Compound({"x": Int(56), "y": Int(22)}),
        "size": Compound({"width": Int(width), "height": Int(height)}),
        "isDynamicSized": Byte(0),
        "allowXEIIngredientOverMouse": Byte(1),
        "children": List[Compound](children),
        "tooltipTexts": List[End](),
        "backgroundTexture": Compound({"type": String("empty")}),
        "layoutPadding": Int(0),
        "id": String(""),
        "drawBackgroundWhenHover": Byte(1),
        "align": String("NONE"),
    })
    return Compound({
        "recipe_type": String(recipe_type),
        "root": root,
        "resources": sample_resources,
    })


def save(data, name):
    p = f"src/main/resources/assets/gtcsolo/ui/recipe_type/{name}.rtui"
    nbtlib.File(data).save(p, gzipped=False)
    print(f"Wrote {name}: {os.path.getsize(p)} bytes")


# starforge: 6 in / 16 out items / 8 fluid out
children = []
for i in range(6):
    row = i // 2
    col = i % 2
    children.append(item_slot(4 + col * 27, 4 + row * 27, f"item_in_{i}"))
children.append(progress_widget(60, 29))
for i in range(16):
    row = i // 4
    col = i % 4
    children.append(item_slot(90 + col * 27, 4 + row * 27, f"item_out_{i}"))
for i in range(8):
    row = i // 4
    col = i % 4
    children.append(fluid_slot(90 + col * 27, 112 + row * 27, f"fluid_out_{i}"))
save(build_data(children, 200, 165, "gtcsolo:starforge"), "starforge")


# locus_simulation_builder: 3 in / 1 out items, middle input above progress
children = []
children.append(item_slot(4, 4, "item_in_0"))
children.append(item_slot(4, 31, "item_in_2"))
children.append(item_slot(43, 4, "item_in_1"))  # 進捗バーの上 (+3px 右寄せ)
children.append(progress_widget(40, 31))
children.append(item_slot(85, 31, "item_out_0"))
save(build_data(children, 110, 60, "gtcsolo:locus_simulation_builder"),
     "locus_simulation_builder")


# wen_nexus_assembler: 9 in / 2 out items, 3 fluid in (tall), 0 fluid out
children = []
for i in range(3):
    children.append(fluid_slot(4 + i * 11, 4, f"fluid_in_{i}",
                               w=9, h=130, border=4))
cx, cy, R = 130, 80, 70
for i in range(9):
    a = math.radians(-90 + i * 40)
    x = int(round(cx + R * math.cos(a) - 9))
    y = int(round(cy + R * math.sin(a) - 9))
    children.append(item_slot(x, y, f"item_in_{i}"))
# 円内: progress / item_out_0 / item_out_1 を横一列に隣接配置 (= 中央高さ)
# 列幅 = 20 + 18 + 18 = 56、 列を cx 中心にする → 左端 = cx - 28
row_left = cx - 28
children.append(progress_widget(row_left, cy - 10))             # x = 102, y = 70
children.append(item_slot(row_left + 20, cy - 9, "item_out_0"))  # x = 122
children.append(item_slot(row_left + 20 + 18, cy - 9, "item_out_1"))  # x = 140
save(build_data(children, 220, 165, "gtcsolo:wen_nexus_assembler"),
     "wen_nexus_assembler")
