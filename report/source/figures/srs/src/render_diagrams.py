#!/usr/bin/env python3
from __future__ import annotations

import math
import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent
OUT = ROOT.parent
FONT_REGULAR = os.environ.get("SRS_FONT_REGULAR", "/mnt/c/Windows/Fonts/msyh.ttc")
FONT_BOLD = os.environ.get("SRS_FONT_BOLD", "/mnt/c/Windows/Fonts/msyhbd.ttc")
BLACK = "#000000"
WHITE = "#FFFFFF"


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REGULAR, size)


F30 = font(30)
F32 = font(32)
F34 = font(34)
F36 = font(36)
F38 = font(38)
F40 = font(40)
F42 = font(42)
F44 = font(44)
F46 = font(46)
F48 = font(48)
F50 = font(50)
F52 = font(52)
F54 = font(54)
F56 = font(56, True)
F58 = font(58)
F60 = font(60)
F62 = font(62)
F64 = font(64, True)


def canvas(width: int, height: int) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (width, height), WHITE)
    return image, ImageDraw.Draw(image)


def centered_text(draw, box, text: str, text_font=F40, spacing=4):
    left, top, right, bottom = box
    bbox = draw.multiline_textbbox((0, 0), text, font=text_font, spacing=spacing, align="center")
    width = bbox[2] - bbox[0]
    height = bbox[3] - bbox[1]
    draw.multiline_text(
        ((left + right - width) / 2, (top + bottom - height) / 2 - bbox[1]),
        text,
        font=text_font,
        fill=BLACK,
        spacing=spacing,
        align="center",
    )


def title(draw, width: int, text: str):
    centered_text(draw, (0, 12, width, 88), text, F56)


def rectangle(draw, box, width=3):
    draw.rectangle(box, fill=WHITE, outline=BLACK, width=width)


def floating_label(draw, box, text: str, text_font=F34):
    draw.rectangle(box, fill=WHITE)
    centered_text(draw, box, text, text_font, spacing=1)


def oval(draw, box, text: str, text_font=F40, width=3, spacing=3):
    draw.ellipse(box, fill=WHITE, outline=BLACK, width=width)
    centered_text(draw, box, text, text_font, spacing)


def actor_box(
    draw,
    box,
    label: str,
    external=False,
    text_font=F46,
    stereotype_font=F30,
    stereotype=None,
    width=3,
):
    rectangle(draw, box, width=width)
    if stereotype is None:
        stereotype = "«external actor»" if external else "«actor»"
    left, top, right, bottom = box
    stereo_bottom = top + max(38, (bottom - top) // 3)
    centered_text(draw, (left, top + 5, right, stereo_bottom), stereotype, stereotype_font, spacing=1)
    centered_text(draw, (left + 5, stereo_bottom - 2, right - 5, bottom - 5), label, text_font, spacing=1)
    return {
        "left": (left, (top + bottom) // 2),
        "right": (right, (top + bottom) // 2),
        "top": ((left + right) // 2, top),
        "bottom": ((left + right) // 2, bottom),
    }


def polyline(draw, points, width=3):
    draw.line(points, fill=BLACK, width=width, joint="curve")


def filled_arrow(draw, start, end, width=3, size=14):
    draw.line((start, end), fill=BLACK, width=width)
    angle = math.atan2(end[1] - start[1], end[0] - start[0])
    left = (end[0] - size * math.cos(angle - 0.55), end[1] - size * math.sin(angle - 0.55))
    right = (end[0] - size * math.cos(angle + 0.55), end[1] - size * math.sin(angle + 0.55))
    draw.polygon((end, left, right), fill=BLACK)


def hollow_triangle(draw, tip, angle, size=24, width=3):
    base = (tip[0] - size * math.cos(angle), tip[1] - size * math.sin(angle))
    left = (
        base[0] + size * 0.65 * math.cos(angle + math.pi / 2),
        base[1] + size * 0.65 * math.sin(angle + math.pi / 2),
    )
    right = (
        base[0] + size * 0.65 * math.cos(angle - math.pi / 2),
        base[1] + size * 0.65 * math.sin(angle - math.pi / 2),
    )
    draw.polygon((tip, left, right), fill=WHITE, outline=BLACK)
    draw.line((tip, left, right, tip), fill=BLACK, width=width)


def generalization(draw, start, tip, width=3, size=24):
    angle = math.atan2(tip[1] - start[1], tip[0] - start[0])
    base = (tip[0] - size * math.cos(angle), tip[1] - size * math.sin(angle))
    draw.line((start, base), fill=BLACK, width=width)
    hollow_triangle(draw, tip, angle, size, width)


def generalization_path(draw, points, tip, angle, width=3, size=24):
    base = (tip[0] - size * math.cos(angle), tip[1] - size * math.sin(angle))
    polyline(draw, [*points, base], width=width)
    hollow_triangle(draw, tip, angle, size, width)


def save(image: Image.Image, filename: str):
    image.save(OUT / filename, optimize=True)


def render_09():
    image, draw = canvas(1800, 1400)
    title(draw, 1800, "系统执行者分析图")
    centered_text(draw, (70, 110, 1160, 180), "人员执行者", F48)
    centered_text(draw, (1190, 110, 1750, 180), "支撑执行者", F48)

    actor_box(draw, (40, 520, 320, 680), "游客", text_font=F50, stereotype_font=F36, width=4)
    actor_box(draw, (500, 220, 900, 390), "已登录用户", text_font=F50, stereotype_font=F36, width=4)
    roles = [
        ((150, 915, 375, 1080), "顾客", (570, 390)),
        ((395, 915, 620, 1080), "商家", (650, 390)),
        ((640, 915, 865, 1080), "骑手", (750, 390)),
        ((885, 915, 1110, 1080), "管理员", (830, 390)),
    ]
    for box, label, tip in roles:
        ports = actor_box(draw, box, label, text_font=F48, stereotype_font=F36, width=4)
        generalization(draw, ports["top"], tip, width=4, size=32)

    actor_box(
        draw,
        (1290, 220, 1750, 390),
        "外部服务",
        external=True,
        text_font=F48,
        stereotype_font=F36,
        width=4,
    )
    services = [
        ((1160, 915, 1360, 1105), "AI / 识图 /\n语音服务", (1370, 390)),
        ((1375, 915, 1575, 1105), "地图服务", (1520, 390)),
        ((1590, 915, 1790, 1105), "对象存储", (1670, 390)),
    ]
    for box, label, tip in services:
        ports = actor_box(
            draw,
            box,
            label,
            external=True,
            text_font=F42,
            stereotype_font=F36,
            stereotype="«external»",
            width=4,
        )
        generalization(draw, ports["top"], tip, width=4, size=32)

    save(image, "09-actor-analysis.png")


def render_10():
    image, draw = canvas(2000, 2500)
    title(draw, 2000, "系统总用例图")

    login = actor_box(draw, (800, 100, 1200, 270), "已登录用户", text_font=F52, stereotype_font=F40, width=4)
    role_boxes = {
        "顾客": ((20, 610, 245, 780), (900, 270), [(132, 610), (132, 350), (900, 350)]),
        "商家": ((20, 930, 245, 1100), (965, 270), [(132, 930), (132, 400), (965, 400)]),
        "骑手": ((1755, 610, 1980, 780), (1100, 270), [(1868, 610), (1868, 350), (1100, 350)]),
        "管理员": ((1755, 930, 1980, 1100), (1035, 270), [(1868, 930), (1868, 400), (1035, 400)]),
    }
    role_ports = {}
    for label, (box, tip, path) in role_boxes.items():
        role_ports[label] = actor_box(draw, box, label, text_font=F50, stereotype_font=F40, width=4)
        generalization_path(draw, path, tip, -math.pi / 2, width=4, size=30)

    boundary = (270, 485, 1730, 2425)
    rectangle(draw, boundary, width=4)
    centered_text(draw, (295, 495, 900, 565), "轻量级外卖服务平台", F50)

    role_cases = [
        ((340, 600, 915, 875), "UC-CUS-01～06\nUC-AI-01～02\nUC-MKT-01 / UC-PER-01\n顾客交易、履约\n及个人能力", "顾客"),
        ((340, 940, 915, 1130), "UC-MER-01～04\n商家经营管理", "商家"),
        ((1085, 620, 1660, 810), "UC-RID-01～04\n骑手配送管理", "骑手"),
        ((1085, 940, 1660, 1130), "UC-ADM-01～03\n平台审核、治理与统计", "管理员"),
    ]
    for box, label, role in role_cases:
        oval(draw, box, label, F46 if role == "顾客" else F48, width=4, spacing=3)
        target = (box[0], (box[1] + box[3]) // 2) if role in ("顾客", "商家") else (box[2], (box[1] + box[3]) // 2)
        source = role_ports[role]["right"] if role in ("顾客", "商家") else role_ports[role]["left"]
        polyline(draw, [source, target], width=4)

    visitor = actor_box(draw, (20, 1570, 245, 1750), "游客", text_font=F50, stereotype_font=F40, width=4)
    public_cases = [
        ((340, 1300, 915, 1475), "UC-COM-01\n注册与登录"),
        ((340, 1570, 915, 1745), "UC-CUS-01\n浏览与搜索店铺商品"),
        ((340, 1840, 915, 2015), "UC-AI-01（公开部分）\n公开规则问答"),
    ]
    for box, label in public_cases:
        oval(draw, box, label, F48, width=4)
        polyline(draw, [visitor["right"], (box[0], (box[1] + box[3]) // 2)], width=4)

    profile = (715, 2180, 1285, 2370)
    oval(draw, profile, "UC-COM-02～03\n个人资料、地址\n角色与通知", F48, width=4)
    polyline(draw, [login["bottom"], (1000, profile[1])], width=4)

    external = actor_box(
        draw,
        (1755, 1570, 1980, 1765),
        "外部服务",
        external=True,
        text_font=F46,
        stereotype_font=F38,
        stereotype="«external»",
        width=4,
    )
    external_cases = [
        ((1085, 1300, 1660, 1475), "UC-AI-01～02\nAI 与多模态识别服务"),
        ((1085, 1570, 1660, 1745), "UC-RID-04\n地图导航服务"),
        ((1085, 1840, 1660, 2015), "对象存储服务\n头像、商品和评价图片"),
    ]
    for box, label in external_cases:
        oval(draw, box, label, F48, width=4)
        polyline(draw, [external["left"], (box[2], (box[1] + box[3]) // 2)], width=4)

    save(image, "10-system-use-case.png")


def role_cluster(draw, y: int, cluster_h: int, parent_text: str, children: list[str], diagram_width: int):
    parent_h = 220 if parent_text.count("\n") >= 2 else 180
    parent_y = y + (cluster_h - parent_h) // 2
    boundary_left = int(diagram_width * 0.17)
    parent = (boundary_left + 30, parent_y, int(diagram_width * 0.55), parent_y + parent_h)
    oval(draw, parent, f"«abstract»\n{parent_text}", F52 if parent_h > 180 else F56, width=4, spacing=2)

    child_h = 120
    gap_x = 30
    gap_y = 20
    child_start = int(diagram_width * 0.59)
    child_w = (diagram_width - 45 - child_start - gap_x) // 2
    rows = math.ceil(len(children) / 2)
    total_h = rows * child_h + (rows - 1) * gap_y
    start_y = y + max(0, (cluster_h - total_h) // 2)
    boxes = []
    for index, child in enumerate(children):
        row, col = divmod(index, 2)
        x1 = child_start + col * (child_w + gap_x)
        y1 = start_y + row * (child_h + gap_y)
        box = (x1, y1, x1 + child_w, y1 + child_h)
        oval(draw, box, child, F52, width=4, spacing=2)
        boxes.append(box)

    centers = [(box[1] + box[3]) // 2 for box in boxes]
    spine_x = (parent[2] + child_start) // 2
    if len(centers) > 1:
        polyline(draw, [(spine_x, min(centers)), (spine_x, max(centers))], width=4)
    for box, cy in zip(boxes, centers):
        polyline(draw, [(box[0], cy), (spine_x, cy)], width=4)
    parent_cy = (parent[1] + parent[3]) // 2
    generalization_path(draw, [(spine_x, parent_cy)], (parent[2], parent_cy), math.pi, width=4, size=30)
    return parent


def render_role_diagram(filename: str, diagram_title: str, actor_label: str, groups, height: int, width: int = 2200):
    image, draw = canvas(width, height)
    title(draw, width, diagram_title)
    boundary_left = int(width * 0.17)
    boundary = (boundary_left, 140, width - 25, height - 55)
    rectangle(draw, boundary, width=4)
    centered_text(draw, (boundary_left + 25, 150, width - 45, 220), f"{actor_label}业务用例", F54)

    row_top = 235
    available = height - row_top - 90
    cluster_heights = []
    for _, children in groups:
        rows = math.ceil(len(children) / 2)
        cluster_heights.append(rows * 120 + (rows - 1) * 20 + 30)
    remaining = available - sum(cluster_heights)
    gap = max(0, remaining / max(1, len(groups) - 1))
    actor = actor_box(
        draw,
        (20, height // 2 - 105, boundary_left - 35, height // 2 + 105),
        actor_label,
        text_font=F60,
        stereotype_font=F40,
        width=4,
    )
    y = row_top
    for cluster_h, (parent_text, children) in zip(cluster_heights, groups):
        parent = role_cluster(draw, int(y), cluster_h, parent_text, children, width)
        parent_cy = (parent[1] + parent[3]) // 2
        polyline(draw, [actor["right"], (parent[0], parent_cy)], width=4)
        y += cluster_h + gap
    save(image, filename)


def render_11():
    groups = [
        ("UC-CUS-01\n浏览与搜索店铺商品", ["UC-CUS-01.1\n店铺列表", "UC-CUS-01.2\n搜索筛选", "UC-CUS-01.3\n店铺商品", "UC-CUS-01.4\n商品详情"]),
        ("UC-CUS-02\n管理购物车", ["UC-CUS-02.1\n加入购物车", "UC-CUS-02.2\n修改数量", "UC-CUS-02.3\n删除单项", "UC-CUS-02.4\n选中结算", "UC-CUS-02.5\n清空 / 切店"]),
        ("UC-CUS-03\n订单试算与创建", ["UC-CUS-03.1\n选择结算信息", "UC-CUS-03.2\n订单试算", "UC-CUS-03.3\n创建订单"]),
        ("UC-CUS-04\n支付与取消订单", ["UC-CUS-04.1\n支付订单", "UC-CUS-04.2\n取消订单"]),
        ("UC-CUS-05\n履约进度与确认收货", ["UC-CUS-05.1\n查看订单", "UC-CUS-05.2\n跟踪履约", "UC-CUS-05.3\n确认收货"]),
        ("UC-CUS-06\n评价与消费分析", ["UC-CUS-06.1\n提交评价", "UC-CUS-06.2\n查看回复", "UC-CUS-06.3\n消费分析"]),
        ("UC-AI-01\nAI 客服与智能点餐", ["UC-AI-01.1\n规则问答", "UC-AI-01.2\n订单查询", "UC-AI-01.3\n点餐建议", "UC-AI-01.4\n确认加购"]),
        ("UC-AI-02\n图片识菜与语音输入", ["UC-AI-02.1\n图片识菜", "UC-AI-02.2\n语音转写", "UC-AI-02.3\n确认结果"]),
        ("UC-MKT-01\n钱包、积分、优惠券\n与会员", ["UC-MKT-01.1\n查看资产", "UC-MKT-01.2\n充值 / 会员", "UC-MKT-01.3\n结算使用"]),
        ("UC-PER-01\n个性化偏好与主题", ["UC-PER-01.1\n切换主题", "UC-PER-01.2\n口味 / 忌口", "UC-PER-01.3\n清除偏好", "UC-PER-01.4\n每日主题"]),
    ]
    render_role_diagram("11-customer-use-case.png", "顾客用例图", "顾客", groups, 3400)


def render_12():
    groups = [
        ("UC-MER-01\n商家申请与店铺状态", ["UC-MER-01.1\n提交申请", "UC-MER-01.2\n审核结果", "UC-MER-01.3\n店铺资料", "UC-MER-01.4\n营业状态"]),
        ("UC-MER-02\n分类、商品与库存管理", ["UC-MER-02.1\n查看分类商品", "UC-MER-02.2\n维护分类", "UC-MER-02.3\n新增商品", "UC-MER-02.4\n修改商品", "UC-MER-02.5\n商品上下架", "UC-MER-02.6\n库存与限购"]),
        ("UC-MER-03\n商家处理订单", ["UC-MER-03.1\n查看订单", "UC-MER-03.2\n接单", "UC-MER-03.3\n拒单", "UC-MER-03.4\n确认出餐"]),
        ("UC-MER-04\n评价回复与经营看板", ["UC-MER-04.1\n查看评价", "UC-MER-04.2\n回复评价", "UC-MER-04.3\n经营看板", "UC-MER-04.4\nAI 经营摘要"]),
    ]
    render_role_diagram("12-merchant-use-case.png", "商家用例图", "商家", groups, 2700)


def render_13():
    groups = [
        ("UC-RID-01\n骑手申请与工作状态", ["UC-RID-01.1\n提交申请", "UC-RID-01.2\n审核结果", "UC-RID-01.3\n上线 / 下线"]),
        ("UC-RID-02\n查看任务与并发接单", ["UC-RID-02.1\n可接任务", "UC-RID-02.2\n提交接单"]),
        ("UC-RID-03\n到店、取餐与送达", ["UC-RID-03.1\n记录到店", "UC-RID-03.2\n确认取餐", "UC-RID-03.3\n开始配送", "UC-RID-03.4\n确认送达"]),
        ("UC-RID-04\n异常、历史与导航", ["UC-RID-04.1\n上报异常", "UC-RID-04.2\n处理结果", "UC-RID-04.3\n历史任务", "UC-RID-04.4\n地图导航"]),
    ]
    render_role_diagram("13-rider-use-case.png", "骑手用例图", "骑手", groups, 2650)


def render_14():
    groups = [
        ("UC-ADM-01\n商家、店铺与骑手审核", ["UC-ADM-01.1\n查看申请", "UC-ADM-01.2\n商家审核", "UC-ADM-01.3\n店铺审核", "UC-ADM-01.4\n骑手审核"]),
        ("UC-ADM-02\n账号、评价与异常治理", ["UC-ADM-02.1\n账号治理", "UC-ADM-02.2\n评价治理", "UC-ADM-02.3\n配送异常"]),
        ("UC-ADM-03\n平台数据看板", ["UC-ADM-03.1\n平台概览", "UC-ADM-03.2\n交易统计", "UC-ADM-03.3\n配送统计"]),
    ]
    render_role_diagram("14-admin-use-case.png", "管理员用例图", "管理员", groups, 2600)


def ia_panel(draw, box, role, entries):
    rectangle(draw, box, width=3)
    left, top, right, _ = box
    centered_text(draw, (left, top + 12, right, top + 82), role, F52)
    root = (left + 175, top + 105, right - 175, top + 245)
    rectangle(draw, root, width=4)
    centered_text(draw, root, "工作台 / 主导航", F44)

    entry_w = 350
    entry_h = 145
    x_positions = [left + 40, right - 40 - entry_w]
    entry_boxes = []
    for index, entry in enumerate(entries):
        row, col = divmod(index, 2)
        x1 = x_positions[col]
        y1 = top + 365 + row * 195
        entry_box = (x1, y1, x1 + entry_w, y1 + entry_h)
        rectangle(draw, entry_box, width=4)
        centered_text(draw, entry_box, entry, F42, spacing=3)
        entry_boxes.append(entry_box)

    root_x = (root[0] + root[2]) // 2
    split_y = top + 305
    left_trunk = left + 20
    right_trunk = right - 20
    last_left_y = max((box[1] + box[3]) // 2 for box in entry_boxes[0::2])
    right_boxes = entry_boxes[1::2]
    last_right_y = max((box[1] + box[3]) // 2 for box in right_boxes) if right_boxes else split_y
    polyline(draw, [(root_x, root[3]), (root_x, split_y), (left_trunk, split_y), (left_trunk, last_left_y)], width=4)
    polyline(draw, [(root_x, split_y), (right_trunk, split_y), (right_trunk, last_right_y)], width=4)
    for index, entry_box in enumerate(entry_boxes):
        cy = (entry_box[1] + entry_box[3]) // 2
        if index % 2 == 0:
            filled_arrow(draw, (left_trunk, cy), (entry_box[0], cy), width=4, size=14)
        else:
            filled_arrow(draw, (right_trunk, cy), (entry_box[2], cy), width=4, size=14)


def render_15():
    image, draw = canvas(1800, 2350)
    title(draw, 1800, "四端页面信息架构图")
    ia_panel(draw, (35, 120, 875, 1120), "顾客端", ["首页 /\n店铺搜索", "店铺与\n商品详情", "购物车 /\n确认订单", "订单列表 /\n履约详情", "AI 点餐", "个人中心"])
    ia_panel(draw, (925, 120, 1765, 1120), "商家端", ["店铺资料 /\n营业状态", "分类 / 商品 /\n库存", "订单工作台", "评价回复", "经营看板"])
    ia_panel(draw, (35, 1190, 875, 2190), "骑手端", ["资格申请 /\n工作状态", "可接任务", "当前任务 /\n导航", "异常上报", "历史任务"])
    ia_panel(draw, (925, 1190, 1765, 2190), "管理端", ["商家 / 店铺\n审核", "骑手审核", "账号 / 评价\n治理", "配送异常\n处理", "平台数据\n看板"])
    save(image, "15-page-information-architecture.png")


def wireframe(draw, box, name):
    rectangle(draw, box, width=4)
    left, top, right, bottom = box
    centered_text(draw, (left, top + 8, right, top + 78), name, F54)
    screen = (left + 24, top + 95, right - 24, bottom - 24)
    rectangle(draw, screen, width=4)
    return screen


def wf_box(draw, box, text, text_font=F38, width=3):
    rectangle(draw, box, width=width)
    centered_text(draw, box, text, text_font, spacing=2)


def render_16():
    image, draw = canvas(2100, 3200)
    title(draw, 2100, "关键页面低保真原型组")
    screens = [
        wireframe(draw, (50, 130, 1010, 1040), "顾客 AI 点餐页"),
        wireframe(draw, (1090, 130, 2050, 1040), "订单履约详情页"),
        wireframe(draw, (50, 1110, 1010, 2020), "商家订单工作台"),
        wireframe(draw, (1090, 1110, 2050, 2020), "骑手当前任务页"),
        wireframe(draw, (570, 2090, 1530, 3000), "管理员审核 / 看板页"),
    ]

    s = screens[0]
    wf_box(draw, (s[0] + 18, s[1] + 18, s[2] - 18, s[1] + 105), "返回        AI 点餐        历史", F50, 4)
    wf_box(draw, (s[0] + 30, s[1] + 150, s[2] - 30, s[1] + 390), "对话区\n用户：想吃清淡的面\nAI：已按营业状态与库存筛选", F50, 4)
    wf_box(draw, (s[0] + 30, s[1] + 435, s[2] - 30, s[1] + 635), "候选商品卡\n名称 / 价格 / 库存        [加入]", F50, 4)
    wf_box(draw, (s[0] + 18, s[3] - 100, s[2] - 18, s[3] - 18), "语音  图片    输入需求...    [发送]", F50, 4)

    s = screens[1]
    wf_box(draw, (s[0] + 18, s[1] + 18, s[2] - 18, s[1] + 105), "订单 #1001                 配送中", F50, 4)
    wf_box(draw, (s[0] + 30, s[1] + 150, s[2] - 30, s[1] + 310), "已接单 → 已出餐 → 配送中 → 待确认", F50, 4)
    wf_box(draw, (s[0] + 30, s[1] + 355, s[2] - 30, s[1] + 620), "骑手信息 / 必要联系方式\n取餐地址 / 收货地址\n关键时间线", F50, 4)
    wf_box(draw, (s[0] + 18, s[3] - 100, s[2] - 18, s[3] - 18), "[地图]    [联系骑手]    [确认收货]", F50, 4)

    s = screens[2]
    wf_box(draw, (s[0] + 18, s[1] + 18, s[2] - 18, s[1] + 105), "全部    待接单    制作中    待取餐", F50, 4)
    wf_box(draw, (s[0] + 30, s[1] + 150, s[2] - 30, s[1] + 405), "订单卡 #1001\n商品明细 / 地址摘要 / 应付金额\n[拒单]                         [接单]", F50, 4)
    wf_box(draw, (s[0] + 30, s[1] + 455, s[2] - 30, s[1] + 685), "订单卡 #1002\n制作计时 / 顾客备注\n[确认出餐]", F50, 4)

    s = screens[3]
    wf_box(draw, (s[0] + 18, s[1] + 18, s[2] - 18, s[1] + 105), "当前任务 #D1001              配送中", F50, 4)
    wf_box(draw, (s[0] + 30, s[1] + 150, s[2] - 30, s[1] + 430), "店铺地址\n收货地址\n距离与文本路线提示", F52, 4)
    wf_box(draw, (s[0] + 30, s[1] + 475, s[2] - 30, s[1] + 635), "到店 → 取餐 → 配送 → 送达", F50, 4)
    wf_box(draw, (s[0] + 18, s[3] - 100, s[2] - 18, s[3] - 18), "[打开导航]  [上报异常]  [确认送达]", F50, 4)

    s = screens[4]
    wf_box(draw, (s[0] + 18, s[1] + 18, s[2] - 18, s[1] + 105), "待审核申请                  平台看板", F50, 4)
    wf_box(draw, (s[0] + 30, s[1] + 145, s[2] - 30, s[1] + 285), "筛选：类型 / 状态 / 日期        [查询]", F50, 4)
    wf_box(draw, (s[0] + 30, s[1] + 330, s[2] - 30, s[1] + 550), "申请列表\n对象    类型    提交时间    状态    [查看]", F50, 4)
    wf_box(draw, (s[0] + 30, s[1] + 595, s[2] - 30, s[1] + 805), "概览：订单量 / 营业额 / 异常数\n趋势图占位            排名列表占位", F50, 4)
    save(image, "16-key-page-wireframes.png")


def render_17():
    image, draw = canvas(1800, 2150)
    title(draw, 1800, "系统部署及外部依赖图")

    client = (30, 285, 410, 930)
    server = (485, 120, 1305, 1820)
    external = (1390, 260, 1770, 1660)
    test = (30, 1090, 410, 1470)
    for box in (client, server, external, test):
        rectangle(draw, box, width=4)
    centered_text(draw, (30, 295, 410, 370), "用户设备", F50)
    centered_text(draw, (485, 130, 1305, 210), "应用服务器", F54)
    centered_text(draw, (1390, 270, 1770, 345), "外部依赖", F50)
    centered_text(draw, (30, 1100, 410, 1180), "自动化测试环境", F46)

    browser = (75, 500, 365, 770)
    test_box = (75, 1260, 365, 1400)
    api = (610, 270, 1180, 445)
    auth = (560, 610, 1230, 765)
    domain = (560, 915, 1230, 1095)
    adapter = (560, 1250, 1230, 1405)
    db = (640, 1580, 1150, 1760)
    wf_box(draw, browser, "Chrome / Edge\nVue 3\n单页应用", F42, 4)
    wf_box(draw, test_box, "Mock / Stub", F44, 4)
    wf_box(draw, api, "Spring Boot REST API", F52, 4)
    wf_box(draw, auth, "认证与权限校验", F48, 4)
    wf_box(draw, domain, "订单 / 商品 / 配送 / 资产服务", F46, 4)
    wf_box(draw, adapter, "外部服务适配层", F48, 4)
    draw.ellipse(db, fill=WHITE, outline=BLACK, width=4)
    centered_text(draw, db, "MySQL\n业务数据与审计流水", F46)

    ext_boxes = [
        (1430, 420, 1730, 690, "AI / 识图 /\n语音服务\nHTTPS /\n脱敏数据"),
        (1430, 835, 1730, 1070, "地图服务\nHTTPS /\n必要位置"),
        (1430, 1220, 1730, 1455, "对象存储\nHTTPS /\n受检媒体"),
    ]
    for x1, y1, x2, y2, label in ext_boxes:
        wf_box(draw, (x1, y1, x2, y2), label, F42 if y1 == 420 else F44, 4)

    polyline(draw, [(browser[2], 635), (455, 635), (455, 330), (api[0], 330)], width=4)
    filled_arrow(draw, (455, 330), (api[0], 330), width=4, size=18)
    floating_label(draw, (320, 405, 535, 475), "HTTPS / JSON", F42)

    polyline(draw, [(test_box[2], 1330), (445, 1330), (445, 395), (api[0], 395)], width=4)
    filled_arrow(draw, (445, 395), (api[0], 395), width=4, size=18)
    floating_label(draw, (305, 1165, 475, 1235), "测试调用", F42)

    center_x = (api[0] + api[2]) // 2
    filled_arrow(draw, (center_x, api[3]), (center_x, auth[1]), width=4, size=18)
    filled_arrow(draw, (center_x, auth[3]), (center_x, domain[1]), width=4, size=18)
    filled_arrow(draw, (center_x, domain[3]), (center_x, adapter[1]), width=4, size=18)
    domain_cy = (domain[1] + domain[3]) // 2
    db_cy = (db[1] + db[3]) // 2
    polyline(draw, [(domain[2], domain_cy), (1270, domain_cy), (1270, db_cy), (db[2], db_cy)], width=4)
    filled_arrow(draw, (1270, db_cy), (db[2], db_cy), width=4, size=18)
    floating_label(draw, (1040, 1460, 1260, 1530), "JDBC / 事务", F42)

    for index, ext in enumerate(ext_boxes):
        start_y = adapter[1] + 25 + index * 48
        end_y = (ext[1] + ext[3]) // 2
        lane_x = 1315 + index * 28
        polyline(draw, [(adapter[2], start_y), (lane_x, start_y), (lane_x, end_y), (ext[0], end_y)], width=4)
        filled_arrow(draw, (lane_x, end_y), (ext[0], end_y), width=4, size=18)

    annotation = (380, 1920, 1420, 2085)
    rectangle(draw, annotation, width=4)
    centered_text(draw, annotation, "前端不直连数据库、不持有外部密钥；外部服务失败时，\n核心交易与文本地址流程仍可用。", F42)
    save(image, "17-system-deployment.png")


def main():
    render_09()
    render_10()
    render_11()
    render_12()
    render_13()
    render_14()
    render_15()
    render_16()
    render_17()


if __name__ == "__main__":
    main()
