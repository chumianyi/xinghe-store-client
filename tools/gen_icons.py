#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成星河软件站客户端图标（PIL）"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = "/root/.codebuddy/artifact/2f8a7d30-9c21-4f0b-1d53-ce3ed8e6b000/app/XingheStore/res"

C1 = (35, 32, 82)      # 深蓝紫
C2 = (99, 102, 241)    # 靛蓝
STAR = (255, 201, 77)  # 星星金


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def star_points(cx, cy, r_out, r_in, points=5, rot=-90):
    import math
    pts = []
    for i in range(points * 2):
        r = r_out if i % 2 == 0 else r_in
        ang = math.radians(rot + i * 180.0 / points)
        pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
    return pts


def make_icon(size, star_color=STAR, radius_ratio=0.28):
    S = size * 4  # 超采样抗锯齿
    img = Image.new("RGB", (S, S))
    d = ImageDraw.Draw(img)
    for y in range(S):
        d.line([(0, y), (S, y)], fill=lerp(C1, C2, y / float(S)))
    # 星河点点
    seed = 20261022
    for i in range(9):
        seed = (seed * 1103515245 + 12345) & 0x7fffffff
        px = seed % S
        seed = (seed * 1103515245 + 12345) & 0x7fffffff
        py = seed % (S // 2)
        r = S // 90 + (i % 3)
        d.ellipse([px - r, py - r, px + r, py + r], fill=(255, 255, 255))
    # 主星
    d.polygon(star_points(S / 2, S * 0.46, S * 0.30, S * 0.125), fill=star_color)
    # 小星
    d.polygon(star_points(S * 0.80, S * 0.20, S * 0.075, S * 0.03), fill=(255, 255, 255))
    d.polygon(star_points(S * 0.18, S * 0.74, S * 0.055, S * 0.022), fill=(255, 255, 255))
    # 圆角
    radius = int(S * radius_ratio)
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], radius=radius, fill=255)
    img.putalpha(mask)
    img = img.resize((size, size), Image.LANCZOS)
    return img


def make_default(size):
    """应用默认占位图标：灰蓝渐变 + 虚框风车"""
    S = size * 4
    img = Image.new("RGB", (S, S), (58, 62, 92))
    d = ImageDraw.Draw(img)
    for y in range(S):
        d.line([(0, y), (S, y)], fill=lerp((52, 56, 86), (86, 92, 130), y / float(S)))
    # 简单 "APP" 示意：圆角方框 + 中心圆点
    m = int(S * 0.22)
    d.rounded_rectangle([m, m, S - m, S - m], radius=int(S * 0.12),
                        outline=(255, 255, 255), width=max(2, S // 48))
    r = int(S * 0.10)
    d.ellipse([S / 2 - r, S / 2 - r, S / 2 + r, S / 2 + r], fill=(255, 255, 255))
    radius = int(S * 0.28)
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], radius=radius, fill=255)
    img.putalpha(mask)
    img = img.resize((size, size), Image.LANCZOS)
    return img


jobs = [
    ("drawable-ldpi/ic_launcher.png", make_icon(36)),
    ("drawable-mdpi/ic_launcher.png", make_icon(48)),
    ("drawable-hdpi/ic_launcher.png", make_icon(72)),
    ("drawable/ic_launcher.png", make_icon(48)),
    ("drawable/ic_logo.png", make_icon(72)),
    ("drawable/ic_app_default.png", make_default(48)),
]
for rel, img in jobs:
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "PNG")
    print("written", path, img.size)
