#!/usr/bin/env python3
"""Sweaty Feet 贴图生成：汗液瓶物品 + 汗脚/真菌效果图标（16x16 像素画）。"""
import os
from PIL import Image

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                    "src", "main", "resources", "assets", "sweatyfeet")


def new_img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def rect(img, x0, y0, x1, y1, color):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), color)


def save(img, rel):
    path = os.path.join(BASE, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("saved", path)


# ---- 汗液瓶：黄绿色药水瓶 ----
bottle = new_img()
CAP = (0x6B, 0x8E, 0x23, 255)
LIQUID = (0x9A, 0xCD, 0x32, 255)
DARK = (0x7A, 0xA8, 0x28, 255)
HILIGHT = (0xC8, 0xE0, 0x6A, 255)
rect(bottle, 6, 1, 9, 3, CAP)              # 瓶盖
rect(bottle, 7, 4, 8, 5, DARK)             # 瓶颈
rect(bottle, 5, 5, 10, 11, LIQUID)         # 瓶身
rect(bottle, 4, 7, 5, 9, LIQUID)           # 瓶身两侧微凸
rect(bottle, 10, 7, 11, 9, LIQUID)
for p in [(5, 5), (10, 5), (4, 7), (5, 7), (11, 7), (10, 7), (4, 9), (5, 11), (10, 11), (11, 9)]:
    bottle.putpixel(p, DARK)               # 轮廓
rect(bottle, 6, 6, 6, 9, HILIGHT)          # 高光
save(bottle, os.path.join("textures", "item", "sweat_bottle.png"))

# ---- 汗脚图标：蓝色汗滴 ----
sweat = new_img()
BLUE = (0x41, 0xA8, 0xEF, 255)
BLUE_DARK = (0x2E, 0x7F, 0xC7, 255)
BLUE_LIGHT = (0x9C, 0xDD, 0xFA, 255)
rect(sweat, 6, 3, 9, 10, BLUE)             # 滴身
sweat.putpixel((7, 2), BLUE)
sweat.putpixel((8, 2), BLUE)               # 圆顶
sweat.putpixel((7, 11), BLUE)
sweat.putpixel((8, 12), BLUE)              # 尖底
sweat.putpixel((8, 13), BLUE)
for p in [(6, 3), (9, 3), (6, 10), (9, 10), (7, 12), (7, 13)]:
    sweat.putpixel(p, BLUE_DARK)           # 轮廓
sweat.putpixel((6, 4), BLUE_LIGHT)
sweat.putpixel((7, 3), BLUE_LIGHT)         # 高光
save(sweat, os.path.join("textures", "mob_effect", "sweaty_feet.png"))

# ---- 真菌图标：墨绿菌团 ----
fungus = new_img()
GREEN = (0x22, 0x8B, 0x22, 255)
GREEN_DARK = (0x17, 0x64, 0x17, 255)
GREEN_LIGHT = (0x3C, 0xA8, 0x3C, 255)
rect(fungus, 5, 5, 10, 11, GREEN)          # 主体
fungus.putpixel((6, 4), GREEN)
fungus.putpixel((9, 4), GREEN)             # 顶角
fungus.putpixel((4, 8), GREEN)
fungus.putpixel((11, 8), GREEN)            # 侧角
for p in [(6, 4), (9, 4), (4, 8), (11, 8), (5, 5), (10, 5), (5, 11), (10, 11)]:
    fungus.putpixel(p, GREEN_DARK)         # 轮廓
for p in [(6, 7), (7, 6), (9, 9), (8, 10), (6, 9)]:
    fungus.putpixel(p, GREEN_LIGHT)        # 菌斑
save(fungus, os.path.join("textures", "mob_effect", "foot_fungus.png"))
