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
F52 = font(52, True)
F56 = font(56, True)


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
    image, draw = canvas(1800, 1260)
    title(draw, 1800, "系统执行者分析图")
    centered_text(draw, (80, 105, 1160, 165), "人员执行者", F44)
    centered_text(draw, (1200, 105, 1740, 165), "支撑执行者", F44)

    actor_box(draw, (45, 455, 305, 585), "游客", text_font=F46)
    actor_box(draw, (510, 205, 880, 345), "已登录用户", text_font=F46)
    roles = [
        ((190, 800, 425, 930), "顾客", (570, 345)),
        ((445, 800, 680, 930), "商家", (650, 345)),
        ((700, 800, 935, 930), "骑手", (740, 345)),
        ((955, 800, 1190, 930), "管理员", (820, 345)),
    ]
    for box, label, tip in roles:
        ports = actor_box(draw, box, label, text_font=F44)
        generalization(draw, ports["top"], tip, width=3, size=26)

    actor_box(draw, (1300, 205, 1725, 345), "外部服务", external=True, text_font=F42)
    services = [
        ((1195, 800, 1375, 955), "AI / 识图 /\n语音服务", (1375, 345)),
        ((1390, 800, 1570, 955), "地图服务", (1510, 345)),
        ((1585, 800, 1765, 955), "对象存储", (1650, 345)),
    ]
    for box, label, tip in services:
        ports = actor_box(
            draw,
            box,
            label,
            external=True,
            text_font=F34,
            stereotype_font=F30,
            stereotype="«external»",
        )
        generalization(draw, ports["top"], tip, width=3, size=26)

    save(image, "09-actor-analysis.png")


def render_10():
    image, draw = canvas(1800, 2200)
    title(draw, 1800, "系统总用例图")

    login = actor_box(draw, (720, 95, 1080, 225), "已登录用户", text_font=F42)
    role_boxes = {
        "顾客": ((10, 510, 220, 650), (790, 225), [(115, 510), (235, 315)]),
        "商家": ((10, 790, 220, 930), (860, 225), [(115, 790), (250, 345)]),
        "骑手": ((1580, 510, 1790, 650), (940, 225), [(1685, 510), (1565, 315)]),
        "管理员": ((1580, 790, 1790, 930), (1010, 225), [(1685, 790), (1550, 345)]),
    }
    role_ports = {}
    for label, (box, tip, path) in role_boxes.items():
        role_ports[label] = actor_box(draw, box, label, text_font=F42)
        generalization_path(draw, path, tip, -math.pi / 2, width=3, size=24)

    boundary = (245, 410, 1555, 2135)
    rectangle(draw, boundary, width=4)
    centered_text(draw, (265, 415, 765, 485), "轻量级外卖服务平台", F42)

    role_cases = [
        ((315, 500, 805, 730), "UC-CUS-01～06\nUC-AI-01～02\nUC-MKT-01 / UC-PER-01\n顾客交易、履约\n及个人能力", "顾客"),
        ((315, 790, 805, 950), "UC-MER-01～04\n商家经营管理", "商家"),
        ((995, 520, 1485, 680), "UC-RID-01～04\n骑手配送管理", "骑手"),
        ((995, 790, 1485, 950), "UC-ADM-01～03\n平台审核、治理与统计", "管理员"),
    ]
    for box, label, role in role_cases:
        oval(draw, box, label, F36 if role == "顾客" else F38, width=3, spacing=2)
        target = (box[0], (box[1] + box[3]) // 2) if role in ("顾客", "商家") else (box[2], (box[1] + box[3]) // 2)
        source = role_ports[role]["right"] if role in ("顾客", "商家") else role_ports[role]["left"]
        polyline(draw, [source, target], width=3)

    visitor = actor_box(draw, (10, 1360, 220, 1510), "游客", text_font=F42)
    public_cases = [
        ((315, 1110, 805, 1250), "UC-COM-01\n注册与登录"),
        ((315, 1350, 805, 1490), "UC-CUS-01\n浏览与搜索店铺商品"),
        ((315, 1590, 805, 1730), "UC-AI-01（公开部分）\n公开规则问答"),
    ]
    for box, label in public_cases:
        oval(draw, box, label, F40, width=3)
        polyline(draw, [visitor["right"], (box[0], (box[1] + box[3]) // 2)], width=3)

    profile = (655, 1900, 1145, 2045)
    oval(draw, profile, "UC-COM-02～03\n个人资料、地址\n角色与通知", F38, width=3)
    polyline(draw, [login["bottom"], (900, profile[1])], width=3)

    external = actor_box(
        draw,
        (1580, 1360, 1790, 1535),
        "外部服务",
        external=True,
        text_font=F36,
        stereotype_font=F30,
        stereotype="«external»",
    )
    external_cases = [
        ((995, 1110, 1485, 1250), "UC-AI-01～02\nAI 与多模态识别服务"),
        ((995, 1350, 1485, 1490), "UC-RID-04\n地图导航服务"),
        ((995, 1590, 1485, 1730), "对象存储服务\n头像、商品和评价图片"),
    ]
    for box, label in external_cases:
        oval(draw, box, label, F38, width=3)
        polyline(draw, [external["left"], (box[2], (box[1] + box[3]) // 2)], width=3)

    save(image, "10-system-use-case.png")


def role_cluster(draw, y: int, cluster_h: int, parent_text: str, children: list[str]):
    parent_h = 190 if parent_text.count("\n") >= 2 else 155
    parent_y = y + (cluster_h - parent_h) // 2
    parent = (350, parent_y, 1000, parent_y + parent_h)
    oval(draw, parent, f"«abstract»\n{parent_text}", F38 if parent_h > 155 else F42, width=3, spacing=1)

    child_w = 320
    child_h = 98
    gap_x = 25
    gap_y = 10
    rows = math.ceil(len(children) / 2)
    total_h = rows * child_h + (rows - 1) * gap_y
    start_y = y + max(0, (cluster_h - total_h) // 2)
    boxes = []
    for index, child in enumerate(children):
        row, col = divmod(index, 2)
        x1 = 1080 + col * (child_w + gap_x)
        y1 = start_y + row * (child_h + gap_y)
        box = (x1, y1, x1 + child_w, y1 + child_h)
        oval(draw, box, child, F42, width=3, spacing=1)
        boxes.append(box)

    centers = [(box[1] + box[3]) // 2 for box in boxes]
    spine_x = 1040
    if len(centers) > 1:
        polyline(draw, [(spine_x, min(centers)), (spine_x, max(centers))], width=3)
    for box, cy in zip(boxes, centers):
        polyline(draw, [(box[0], cy), (spine_x, cy)], width=3)
    parent_cy = (parent[1] + parent[3]) // 2
    generalization_path(draw, [(spine_x, parent_cy)], (parent[2], parent_cy), math.pi, width=3, size=24)
    return parent


def render_role_diagram(filename: str, diagram_title: str, actor_label: str, groups, height: int):
    image, draw = canvas(1800, height)
    title(draw, 1800, diagram_title)
    boundary = (330, 125, 1780, height - 45)
    rectangle(draw, boundary, width=4)
    centered_text(draw, (350, 130, 1760, 195), f"{actor_label}业务用例", F44)

    row_top = 205
    available = height - row_top - 75
    cluster_heights = []
    for _, children in groups:
        rows = math.ceil(len(children) / 2)
        cluster_heights.append(max(210, rows * 98 + (rows - 1) * 10 + 12))
    remaining = available - sum(cluster_heights)
    gap = max(0, remaining / max(1, len(groups) - 1))
    actor = actor_box(draw, (20, height // 2 - 80, 285, height // 2 + 80), actor_label, text_font=F46)
    y = row_top
    for cluster_h, (parent_text, children) in zip(cluster_heights, groups):
        parent = role_cluster(draw, int(y), cluster_h, parent_text, children)
        parent_cy = (parent[1] + parent[3]) // 2
        polyline(draw, [actor["right"], (parent[0], parent_cy)], width=3)
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
    render_role_diagram("11-customer-use-case.png", "顾客用例图", "顾客", groups, 2600)


def render_12():
    groups = [
        ("UC-MER-01\n商家申请与店铺状态", ["UC-MER-01.1\n提交申请", "UC-MER-01.2\n审核结果", "UC-MER-01.3\n店铺资料", "UC-MER-01.4\n营业状态"]),
        ("UC-MER-02\n分类、商品与库存管理", ["UC-MER-02.1\n查看分类商品", "UC-MER-02.2\n维护分类", "UC-MER-02.3\n新增商品", "UC-MER-02.4\n修改商品", "UC-MER-02.5\n商品上下架", "UC-MER-02.6\n库存与限购"]),
        ("UC-MER-03\n商家处理订单", ["UC-MER-03.1\n查看订单", "UC-MER-03.2\n接单", "UC-MER-03.3\n拒单", "UC-MER-03.4\n确认出餐"]),
        ("UC-MER-04\n评价回复与经营看板", ["UC-MER-04.1\n查看评价", "UC-MER-04.2\n回复评价", "UC-MER-04.3\n经营看板", "UC-MER-04.4\nAI 经营摘要"]),
    ]
    render_role_diagram("12-merchant-use-case.png", "商家用例图", "商家", groups, 1900)


def render_13():
    groups = [
        ("UC-RID-01\n骑手申请与工作状态", ["UC-RID-01.1\n提交申请", "UC-RID-01.2\n审核结果", "UC-RID-01.3\n上线 / 下线"]),
        ("UC-RID-02\n查看任务与并发接单", ["UC-RID-02.1\n可接任务", "UC-RID-02.2\n提交接单"]),
        ("UC-RID-03\n到店、取餐与送达", ["UC-RID-03.1\n记录到店", "UC-RID-03.2\n确认取餐", "UC-RID-03.3\n开始配送", "UC-RID-03.4\n确认送达"]),
        ("UC-RID-04\n异常、历史与导航", ["UC-RID-04.1\n上报异常", "UC-RID-04.2\n处理结果", "UC-RID-04.3\n历史任务", "UC-RID-04.4\n地图导航"]),
    ]
    render_role_diagram("13-rider-use-case.png", "骑手用例图", "骑手", groups, 1900)


def render_14():
    groups = [
        ("UC-ADM-01\n商家、店铺与骑手审核", ["UC-ADM-01.1\n查看申请", "UC-ADM-01.2\n商家审核", "UC-ADM-01.3\n店铺审核", "UC-ADM-01.4\n骑手审核"]),
        ("UC-ADM-02\n账号、评价与异常治理", ["UC-ADM-02.1\n账号治理", "UC-ADM-02.2\n评价治理", "UC-ADM-02.3\n配送异常"]),
        ("UC-ADM-03\n平台数据看板", ["UC-ADM-03.1\n平台概览", "UC-ADM-03.2\n交易统计", "UC-ADM-03.3\n配送统计"]),
    ]
    render_role_diagram("14-admin-use-case.png", "管理员用例图", "管理员", groups, 1750)


def ia_panel(draw, box, role, entries):
    rectangle(draw, box, width=3)
    left, top, right, _ = box
    centered_text(draw, (left, top + 12, right, top + 75), role, F48)
    root = (left + 190, top + 95, right - 190, top + 215)
    rectangle(draw, root, width=3)
    centered_text(draw, root, "工作台 / 主导航", F40)

    entry_w = 350
    entry_h = 115
    x_positions = [left + 40, right - 40 - entry_w]
    entry_boxes = []
    for index, entry in enumerate(entries):
        row, col = divmod(index, 2)
        x1 = x_positions[col]
        y1 = top + 330 + row * 165
        entry_box = (x1, y1, x1 + entry_w, y1 + entry_h)
        rectangle(draw, entry_box, width=3)
        centered_text(draw, entry_box, entry, F38, spacing=2)
        entry_boxes.append(entry_box)

    root_x = (root[0] + root[2]) // 2
    split_y = top + 270
    left_trunk = left + 20
    right_trunk = right - 20
    last_left_y = max((box[1] + box[3]) // 2 for box in entry_boxes[0::2])
    right_boxes = entry_boxes[1::2]
    last_right_y = max((box[1] + box[3]) // 2 for box in right_boxes) if right_boxes else split_y
    polyline(draw, [(root_x, root[3]), (root_x, split_y), (left_trunk, split_y), (left_trunk, last_left_y)], width=3)
    polyline(draw, [(root_x, split_y), (right_trunk, split_y), (right_trunk, last_right_y)], width=3)
    for index, entry_box in enumerate(entry_boxes):
        cy = (entry_box[1] + entry_box[3]) // 2
        if index % 2 == 0:
            filled_arrow(draw, (left_trunk, cy), (entry_box[0], cy), width=3, size=12)
        else:
            filled_arrow(draw, (right_trunk, cy), (entry_box[2], cy), width=3, size=12)


def render_15():
    image, draw = canvas(1800, 2100)
    title(draw, 1800, "四端页面信息架构图")
    ia_panel(draw, (35, 115, 875, 1025), "顾客端", ["首页 /\n店铺搜索", "店铺与\n商品详情", "购物车 /\n确认订单", "订单列表 /\n履约详情", "AI 点餐", "个人中心"])
    ia_panel(draw, (925, 115, 1765, 1025), "商家端", ["店铺资料 /\n营业状态", "分类 / 商品 /\n库存", "订单工作台", "评价回复", "经营看板"])
    ia_panel(draw, (35, 1075, 875, 1985), "骑手端", ["资格申请 /\n工作状态", "可接任务", "当前任务 /\n导航", "异常上报", "历史任务"])
    ia_panel(draw, (925, 1075, 1765, 1985), "管理端", ["商家 / 店铺\n审核", "骑手审核", "账号 / 评价\n治理", "配送异常\n处理", "平台数据\n看板"])
    save(image, "15-page-information-architecture.png")


def wireframe(draw, box, name):
    rectangle(draw, box, width=4)
    left, top, right, bottom = box
    centered_text(draw, (left, top + 8, right, top + 68), name, F46)
    screen = (left + 20, top + 82, right - 20, bottom - 20)
    rectangle(draw, screen, width=3)
    return screen


def wf_box(draw, box, text, text_font=F38, width=3):
    rectangle(draw, box, width=width)
    centered_text(draw, box, text, text_font, spacing=2)


def render_16():
    image, draw = canvas(1800, 2600)
    title(draw, 1800, "关键页面低保真原型组")
    screens = [
        wireframe(draw, (45, 115, 865, 880), "顾客 AI 点餐页"),
        wireframe(draw, (935, 115, 1755, 880), "订单履约详情页"),
        wireframe(draw, (45, 945, 865, 1710), "商家订单工作台"),
        wireframe(draw, (935, 945, 1755, 1710), "骑手当前任务页"),
        wireframe(draw, (490, 1775, 1310, 2540), "管理员审核 / 看板页"),
    ]

    s = screens[0]
    wf_box(draw, (s[0] + 15, s[1] + 15, s[2] - 15, s[1] + 80), "返回        AI 点餐        历史", F38)
    wf_box(draw, (s[0] + 25, s[1] + 115, s[2] - 25, s[1] + 300), "对话区\n用户：想吃清淡的面\nAI：已按营业状态与库存筛选", F38)
    wf_box(draw, (s[0] + 25, s[1] + 335, s[2] - 25, s[1] + 500), "候选商品卡\n名称 / 价格 / 库存        [加入]", F38)
    wf_box(draw, (s[0] + 15, s[3] - 80, s[2] - 15, s[3] - 15), "语音  图片    输入需求...    [发送]", F36)

    s = screens[1]
    wf_box(draw, (s[0] + 15, s[1] + 15, s[2] - 15, s[1] + 80), "订单 #1001                 配送中", F38)
    wf_box(draw, (s[0] + 25, s[1] + 120, s[2] - 25, s[1] + 250), "已接单 → 已出餐 → 配送中 → 待确认", F36)
    wf_box(draw, (s[0] + 25, s[1] + 290, s[2] - 25, s[1] + 500), "骑手信息 / 必要联系方式\n取餐地址 / 收货地址\n关键时间线", F38)
    wf_box(draw, (s[0] + 15, s[3] - 80, s[2] - 15, s[3] - 15), "[地图]    [联系骑手]    [确认收货]", F36)

    s = screens[2]
    wf_box(draw, (s[0] + 15, s[1] + 15, s[2] - 15, s[1] + 80), "全部    待接单    制作中    待取餐", F38)
    wf_box(draw, (s[0] + 25, s[1] + 120, s[2] - 25, s[1] + 315), "订单卡 #1001\n商品明细 / 地址摘要 / 应付金额\n[拒单]                         [接单]", F36)
    wf_box(draw, (s[0] + 25, s[1] + 355, s[2] - 25, s[1] + 535), "订单卡 #1002\n制作计时 / 顾客备注\n[确认出餐]", F38)

    s = screens[3]
    wf_box(draw, (s[0] + 15, s[1] + 15, s[2] - 15, s[1] + 80), "当前任务 #D1001              配送中", F38)
    wf_box(draw, (s[0] + 25, s[1] + 120, s[2] - 25, s[1] + 330), "店铺地址\n收货地址\n距离与文本路线提示", F40)
    wf_box(draw, (s[0] + 25, s[1] + 370, s[2] - 25, s[1] + 500), "到店 → 取餐 → 配送 → 送达", F38)
    wf_box(draw, (s[0] + 15, s[3] - 80, s[2] - 15, s[3] - 15), "[打开导航]  [上报异常]  [确认送达]", F36)

    s = screens[4]
    wf_box(draw, (s[0] + 15, s[1] + 15, s[2] - 15, s[1] + 80), "待审核申请                  平台看板", F38)
    wf_box(draw, (s[0] + 25, s[1] + 115, s[2] - 25, s[1] + 215), "筛选：类型 / 状态 / 日期        [查询]", F36)
    wf_box(draw, (s[0] + 25, s[1] + 250, s[2] - 25, s[1] + 410), "申请列表\n对象    类型    提交时间    状态    [查看]", F36)
    wf_box(draw, (s[0] + 25, s[1] + 450, s[2] - 25, s[1] + 625), "概览：订单量 / 营业额 / 异常数\n趋势图占位            排名列表占位", F36)
    save(image, "16-key-page-wireframes.png")


def render_17():
    image, draw = canvas(1800, 1950)
    title(draw, 1800, "系统部署及外部依赖图")

    client = (30, 260, 410, 850)
    server = (485, 120, 1305, 1635)
    external = (1390, 240, 1770, 1510)
    test = (30, 1030, 410, 1340)
    for box in (client, server, external, test):
        rectangle(draw, box, width=4)
    centered_text(draw, (30, 270, 410, 340), "用户设备", F46)
    centered_text(draw, (485, 130, 1305, 205), "应用服务器", F48)
    centered_text(draw, (1390, 250, 1770, 320), "外部依赖", F46)
    centered_text(draw, (30, 1040, 410, 1110), "自动化测试环境", F40)

    browser = (75, 450, 365, 690)
    test_box = (75, 1160, 365, 1260)
    api = (615, 245, 1175, 390)
    auth = (570, 530, 1220, 660)
    domain = (570, 790, 1220, 945)
    adapter = (570, 1080, 1220, 1215)
    db = (650, 1370, 1140, 1530)
    wf_box(draw, browser, "Chrome / Edge\nVue 3\n单页应用", F36, 3)
    wf_box(draw, test_box, "Mock / Stub", F38, 3)
    wf_box(draw, api, "Spring Boot REST API", F46, 3)
    wf_box(draw, auth, "认证与权限校验", F42, 3)
    wf_box(draw, domain, "订单 / 商品 / 配送 / 资产服务", F40, 3)
    wf_box(draw, adapter, "外部服务适配层", F42, 3)
    draw.ellipse(db, fill=WHITE, outline=BLACK, width=3)
    centered_text(draw, db, "MySQL\n业务数据与审计流水", F40)

    ext_boxes = [
        (1435, 390, 1725, 590, "AI / 识图 /\n语音服务\nHTTPS /\n脱敏数据"),
        (1435, 755, 1725, 940, "地图服务\nHTTPS /\n必要位置"),
        (1435, 1105, 1725, 1290, "对象存储\nHTTPS /\n受检媒体"),
    ]
    for x1, y1, x2, y2, label in ext_boxes:
        wf_box(draw, (x1, y1, x2, y2), label, F34, 3)

    polyline(draw, [(browser[2], 570), (455, 570), (455, 315), (api[0], 315)], width=3)
    filled_arrow(draw, (455, 315), (api[0], 315), width=3, size=15)
    floating_label(draw, (330, 375, 525, 435), "HTTPS / JSON", F32)

    polyline(draw, [(test_box[2], 1210), (450, 1210), (450, 350), (api[0], 350)], width=3)
    filled_arrow(draw, (450, 350), (api[0], 350), width=3, size=15)
    floating_label(draw, (315, 1085, 470, 1145), "测试调用", F32)

    center_x = (api[0] + api[2]) // 2
    filled_arrow(draw, (center_x, api[3]), (center_x, auth[1]), width=3, size=15)
    filled_arrow(draw, (center_x, auth[3]), (center_x, domain[1]), width=3, size=15)
    filled_arrow(draw, (center_x, domain[3]), (center_x, adapter[1]), width=3, size=15)
    domain_cy = (domain[1] + domain[3]) // 2
    db_cy = (db[1] + db[3]) // 2
    polyline(draw, [(domain[2], domain_cy), (1270, domain_cy), (1270, db_cy), (db[2], db_cy)], width=3)
    filled_arrow(draw, (1270, db_cy), (db[2], db_cy), width=3, size=15)
    floating_label(draw, (1060, 1260, 1260, 1320), "JDBC / 事务", F32)

    for index, ext in enumerate(ext_boxes):
        start_y = adapter[1] + 20 + index * 42
        end_y = (ext[1] + ext[3]) // 2
        lane_x = 1330 + index * 24
        polyline(draw, [(adapter[2], start_y), (lane_x, start_y), (lane_x, end_y), (ext[0], end_y)], width=3)
        filled_arrow(draw, (lane_x, end_y), (ext[0], end_y), width=3, size=15)

    annotation = (420, 1720, 1380, 1870)
    rectangle(draw, annotation, width=3)
    centered_text(draw, annotation, "前端不直连数据库、不持有外部密钥；外部服务失败时，\n核心交易与文本地址流程仍可用。", F36)
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
