#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成底部导航图标（5 个 x 双态 x 三密度），PIL 4x 超采样抗锯齿"""
import math
import os

from PIL import Image, ImageDraw

OUT = "/root/.codebuddy/artifact/2f8a7d30-9c21-4f0b-1d53-ce3ed8e6b000/app/XingheStore/res"
OFF = (154, 156, 184, 255)   # 未选中 灰紫
ON = (91, 95, 222, 255)      # 选中 主题紫


def canvas(S):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def draw_home(d, S, c):
    # 屋顶
    d.polygon([(S*0.5, S*0.12), (S*0.90, S*0.46), (S*0.10, S*0.46)], fill=c)
    # 房身
    d.rectangle([S*0.22, S*0.46, S*0.78, S*0.88], fill=c)
    # 门（镂空）
    d.rectangle([S*0.43, S*0.62, S*0.57, S*0.88], fill=(0, 0, 0, 0))


def draw_search(d, S, c):
    r = S*0.26
    cx, cy = S*0.46, S*0.46
    d.ellipse([cx-r, cy-r, cx+r, cy+r], outline=c, width=max(2, int(S*0.09)))
    d.line([cx+r*0.72, cy+r*0.72, S*0.82, S*0.82], fill=c, width=max(2, int(S*0.09)))


def draw_download(d, S, c):
    w = max(2, int(S*0.10))
    # 竖杆 + 箭头
    d.line([S*0.5, S*0.14, S*0.5, S*0.56], fill=c, width=w)
    d.polygon([(S*0.5, S*0.72), (S*0.28, S*0.44), (S*0.72, S*0.44)], fill=c)
    # 托盘
    d.rectangle([S*0.20, S*0.80, S*0.80, S*0.88], fill=c)


def draw_fav(d, S, c):
    pts = []
    for i in range(10):
        r = S*0.38 if i % 2 == 0 else S*0.16
        ang = math.radians(-90 + i*36)
        pts.append((S*0.5 + r*math.cos(ang), S*0.5 + r*math.sin(ang)))
    d.polygon(pts, fill=c)


def draw_me(d, S, c):
    r = S*0.20
    d.ellipse([S*0.5-r, S*0.14, S*0.5+r, S*0.14+2*r], fill=c)
    # 肩
    d.pieslice([S*0.18, S*0.46, S*0.82, S*1.10], 180, 360, fill=c)


DRAW = {
    "home": draw_home,
    "search": draw_search,
    "download": draw_download,
    "fav": draw_fav,
    "me": draw_me,
}

SIZES = {"ldpi": 18, "mdpi": 24, "hdpi": 36}

for name, fn in DRAW.items():
    for density, size in SIZES.items():
        for state, color in (("off", OFF), ("on", ON)):
            S = size * 4
            img, d = canvas(S)
            fn(d, S, color)
            img = img.resize((size, size), Image.LANCZOS)
            path = os.path.join(OUT, "drawable-" + density,
                                "nav_%s_%s.png" % (name, state))
            os.makedirs(os.path.dirname(path), exist_ok=True)
            img.save(path, "PNG")
            print("written", path)
