#!/usr/bin/env python3
"""
Generates Minecraft Remix's resource files: textures, block/item models, blockstates, lang, loot tables,
tags, recipes, ponder schematics and ponder text. Run from the repository root:

    python3 tools/gen_assets.py

Everything it writes is plain data and is committed, so the mod builds without running this script.
It owns src/main/resources/assets/mcremix and src/main/resources/data outright and wipes them first: edit this
script, not the files it writes.
Ponder text is read straight out of the scene classes so the English stays next to the scene code.
Needs Pillow (python3 -m pip install pillow).
"""
import colorsys
import gzip
import io
import json
import math
import os
import random
import re
import struct

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
ASSETS = os.path.join(RES, 'assets', 'mcremix')
DATA = os.path.join(RES, 'data')
MASKS = json.load(open(os.path.join(ROOT, 'tools', 'uv_masks.json')))


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def asset(*parts):
    return os.path.join(ASSETS, *parts)


def data(ns, *parts):
    return os.path.join(DATA, ns, *parts)


# ------------------------------------------------------------------------------------------------ variants

STAGES = ['unaffected', 'exposed', 'weathered', 'oxidized']
PARTS = ['shaft', 'cogwheel', 'large_cogwheel', 'gearbox', 'clutch', 'gearshift', 'chain_drive', 'chain_gearshift',
         'bracket']
# parts in a casing, with a model of their own for the item
CASED = ('gearbox', 'clutch', 'gearshift', 'chain_drive', 'chain_gearshift')
CHAINS = ('chain_drive', 'chain_gearshift')
# shafts and cogs, which turn in the open and need a friction point to hold them (FrictionPoints)
BARE = ('shaft', 'cogwheel', 'large_cogwheel')
# brass comes as every part but these (Taran, 0.23.0): its shafts, cogs, gearbox, chain drive and bracket are clockwork
# to look at, and Create's brass-cased blocks do the switching
NO_BRASS = ('clutch', 'gearshift', 'chain_gearshift')
# how far a bare part of each material may reach from the friction point holding it (KineticMaterial.span)
# how far a bare shaft or cog reaches from a support: twice the 0.15.0 spans since 0.27.0 (Taran asked for longer)
SPANS = {'wooden': 2, 'brass': 2, 'copper': 4, 'iron': 4, 'steel': 6, 'aluminium': 6, 'titanium': 6}


# metals that rust, and only where they get wet; copper oxidizes anywhere
RUSTING = ('iron', 'steel')

# the woods wooden parts are cut from, as in WoodSpecies. Each part looks like its wood and nothing else changes,
# except that crimson and warped, being stems, do not burn
WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped']
FIREPROOF_WOODS = ('crimson', 'warped')
WOOD_PARTS = ['shaft', 'gearbox', 'clutch', 'gearshift', 'bracket']


def wood_logs(wood):
    """The tag of a wood's logs, the bark-covered and stripped ones alike."""
    return {'bamboo': 'minecraft:bamboo_blocks', 'crimson': 'minecraft:crimson_stems',
            'warped': 'minecraft:warped_stems'}.get(wood, f'minecraft:{wood}_logs')


def stripped_wood(wood):
    """A wood's stripped log and stripped wood, the casing its gearbox and clutch are built in."""
    if wood == 'bamboo':
        return [item('minecraft:stripped_bamboo_block')]
    if wood in FIREPROOF_WOODS:
        return [item(f'minecraft:stripped_{wood}_stem'), item(f'minecraft:stripped_{wood}_hyphae')]
    return [item(f'minecraft:stripped_{wood}_log'), item(f'minecraft:stripped_{wood}_wood')]


# titanium's temper colours (Tints), kept in the stage slot weathering metals use: grey, then gold, purple and blue
TINT_PREFIXES = {'unaffected': '', 'exposed': 'gold_tinted_', 'weathered': 'purple_tinted_', 'oxidized': 'blue_tinted_'}
TINTED = [prefix + 'titanium' for prefix in TINT_PREFIXES.values()]


def stage_prefix(material, stage):
    if material == 'titanium':
        return TINT_PREFIXES[stage]
    return {'unaffected': '', 'exposed': 'exposed_', 'weathered': 'weathered_',
            'oxidized': 'rusted_' if material in RUSTING else 'oxidized_'}[stage]


def past_last_stage(material, stage):
    """Steel rusts no further than its first stage (Taran, 0.28.0); its later parts stay only for worlds that have them."""
    return material == 'steel' and stage in STAGES[2:]


def holds_only(look):
    """A tint of titanium, which only what holds a part takes: its shafts and cogs never heat, so they stay plain."""
    return look in TINTED[1:]


def variants():
    """(name, material, part, stage, waxed, texture_stage_name) in registration order."""
    out = [(f'{wood}_{part}', 'wooden', part, 'unaffected', False, wood) for wood in WOODS for part in WOOD_PARTS]
    # brass never tarnishes, so there is one of each part it comes as and nothing to wax
    out += [(f'brass_{part}', 'brass', part, 'unaffected', False, 'brass') for part in PARTS if part not in NO_BRASS]
    for material in ['copper', 'iron', 'steel']:
        for part in PARTS:
            for waxed in [False, True]:
                for stage in STAGES:
                    tex = stage_prefix(material, stage) + material
                    name = ('waxed_' if waxed else '') + tex + '_' + part
                    out.append((name, material, part, stage, waxed, tex))
    # aluminium never weathers, so there is one of each part and nothing to wax
    out += [(f'aluminium_{part}', 'aluminium', part, 'unaffected', False, 'aluminium') for part in PARTS]
    # nor does titanium, but what holds a part keeps the temper colour heat gives it, so each holder comes in four
    # tints; its bare shafts and cogs come plain
    out += [(stage_prefix('titanium', stage) + 'titanium_' + part, 'titanium', part, stage, False,
             stage_prefix('titanium', stage) + 'titanium')
            for part in PARTS for stage in (STAGES[:1] if part in BARE else STAGES)]
    return out


VARIANTS = variants()
# a texture's place in this list seeds its noise, so oak (the one wood there once was) comes first, then the other
# woods, aluminium, brass and titanium's tints in the order they came, which keeps every texture made before them as
# it was
TEXTURE_STAGES = ['oak', 'copper', 'exposed_copper', 'weathered_copper', 'oxidized_copper',
                  'iron', 'exposed_iron', 'weathered_iron', 'rusted_iron',
                  'steel', 'exposed_steel', 'weathered_steel', 'rusted_steel'] + WOODS[1:] + ['aluminium', 'brass'] \
    + TINTED

# ------------------------------------------------------------------------------------------------ textures

PALETTES = {
    # base, dark, light, speck (None = no specks). Each wood's colours come from its planks, at oak's contrast
    'oak': ((0xB8, 0x94, 0x5F), (0x8A, 0x68, 0x3C), (0xCF, 0xAE, 0x7A), None),
    'spruce': ((0x80, 0x5E, 0x36), (0x5B, 0x43, 0x27), (0x95, 0x6E, 0x3F), None),
    'birch': ((0xD1, 0xC0, 0x80), (0x96, 0x89, 0x5C), (0xF4, 0xDF, 0x95), None),
    'jungle': ((0xB2, 0x7F, 0x58), (0x7F, 0x5B, 0x3F), (0xD0, 0x94, 0x66), None),
    'acacia': ((0xB6, 0x62, 0x35), (0x82, 0x46, 0x26), (0xD4, 0x72, 0x3D), None),
    'dark_oak': ((0x4C, 0x31, 0x18), (0x36, 0x23, 0x11), (0x58, 0x39, 0x1C), None),
    'mangrove': ((0x7E, 0x3C, 0x37), (0x5A, 0x2B, 0x27), (0x92, 0x46, 0x40), None),
    # cherry planks are pale and low in contrast; at oak's contrast its shadows went grey, so they stay pink
    'cherry': ((0xE6, 0xAE, 0xAA), (0xC0, 0x7E, 0x7E), (0xF6, 0xCE, 0xC8), None),
    'bamboo': ((0xD3, 0xBB, 0x51), (0x97, 0x86, 0x3A), (0xF6, 0xDA, 0x5F), None),
    'crimson': ((0x6E, 0x36, 0x4E), (0x4F, 0x27, 0x38), (0x80, 0x3F, 0x5B), None),
    'warped': ((0x2B, 0x74, 0x6A), (0x1F, 0x53, 0x4C), (0x32, 0x87, 0x7C), None),
    'copper': ((0xE0, 0x79, 0x52), (0xA4, 0x4C, 0x2F), (0xF4, 0xA6, 0x80), None),
    'exposed_copper': ((0xAE, 0x86, 0x6C), (0x7E, 0x60, 0x50), (0xC9, 0xA4, 0x88), (0x7F, 0xA8, 0x8A)),
    'weathered_copper': ((0x6B, 0xA0, 0x7E), (0x4B, 0x76, 0x5D), (0x8D, 0xBE, 0x9C), (0xA4, 0x7A, 0x5E)),
    'oxidized_copper': ((0x52, 0xA3, 0x8B), (0x37, 0x78, 0x66), (0x76, 0xC4, 0xAB), (0x3F, 0x8E, 0x77)),
    'iron': ((0xC4, 0xC4, 0xC4), (0x86, 0x86, 0x86), (0xE6, 0xE6, 0xE6), None),
    'exposed_iron': ((0xB5, 0xA9, 0x9E), (0x80, 0x76, 0x6E), (0xD6, 0xCD, 0xC4), (0xA8, 0x5E, 0x34)),
    'weathered_iron': ((0xA3, 0x6E, 0x4C), (0x72, 0x48, 0x2E), (0xC4, 0x8E, 0x66), (0x8A, 0x8A, 0x86)),
    'rusted_iron': ((0x8C, 0x4B, 0x27), (0x5C, 0x2D, 0x15), (0xB2, 0x68, 0x3A), (0xC2, 0x6A, 0x2C)),
    # steel is a dark gunmetal grey, as Create's girders and industrial iron are, and rusts a deeper brown
    'steel': ((0x52, 0x53, 0x57), (0x33, 0x33, 0x38), (0x7C, 0x7A, 0x7B), None),
    'exposed_steel': ((0x5E, 0x56, 0x52), (0x3A, 0x34, 0x32), (0x84, 0x7A, 0x74), (0x8E, 0x4C, 0x2A)),
    'weathered_steel': ((0x7A, 0x4E, 0x36), (0x50, 0x32, 0x22), (0x9A, 0x6A, 0x4A), (0x52, 0x53, 0x57)),
    'rusted_steel': ((0x76, 0x3E, 0x22), (0x4A, 0x24, 0x12), (0x9A, 0x58, 0x32), (0xB0, 0x60, 0x2A)),
    # aluminium is paler than iron and a little blue, and never weathers
    'aluminium': ((0xCC, 0xD3, 0xDA), (0x92, 0x9B, 0xA6), (0xEE, 0xF2, 0xF6), None),
    # brass, polished like a clock's works, never tarnishes
    'brass': ((0xD9, 0xA9, 0x4C), (0x9C, 0x6B, 0x26), (0xF6, 0xD8, 0x7C), None),
    # titanium is a paler, warmer grey than steel, polished bright (Taran, 0.23.0: shinier), and heat tints what holds
    # its parts gold, then purple, then blue, each flecked with the next as real temper colours run into each other.
    # The tints are faint, the metal's own grey showing through: each colour is two fifths of the way from titanium's
    # grey to the full temper colour
    'titanium': ((0x8E, 0x8A, 0x87), (0x4A, 0x47, 0x46), (0xE2, 0xDE, 0xD9), None),
    'gold_tinted_titanium': ((0xA3, 0x91, 0x6F), (0x62, 0x52, 0x3A), (0xE4, 0xD5, 0xB3), (0x93, 0x7E, 0x8A)),
    'purple_tinted_titanium': ((0x8D, 0x77, 0x90), (0x51, 0x41, 0x54), (0xD0, 0xBB, 0xD0), (0x7A, 0x82, 0x9E)),
    'blue_tinted_titanium': ((0x74, 0x7E, 0x98), (0x3E, 0x45, 0x5A), (0xBA, 0xC4, 0xD9), (0x87, 0x9B, 0xA8)),
}


def clamp(v):
    return max(0, min(255, int(v)))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def material_texture(stage, mask_name, seed):
    """Paints a texture over the opaque area of a UV mask: grain for wood, brushed streaks for metal,
    darker rims at region edges, specks of the next weathering stage. Polished titanium's streaks and catch-lights
    are brighter."""
    rows = MASKS[mask_name]
    h, w = len(rows), len(rows[0])
    base, dark, light, speck = PALETTES[stage]
    rnd = random.Random(seed)
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    wood = stage in WOODS
    polished = stage in TINTED
    streak = [rnd.uniform(-0.12, 0.12) * (1.4 if polished else 1) for _ in range(max(w, h))]

    def inside(x, y):
        return 0 <= x < w and 0 <= y < h and rows[y][x] == '#'

    for y in range(h):
        for x in range(w):
            if not inside(x, y):
                continue
            t = 0.5 + streak[y if not wood else x] + rnd.uniform(-0.08, 0.08)
            if wood and (x + (y // 4)) % 5 == 0:
                t -= 0.25  # grain line
            col = mix(dark, light, max(0.0, min(1.0, t)))
            edge = not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                col = mix(col, dark, 0.55)
            elif not inside(x - 1, y - 1):
                col = mix(col, light, 0.6 if polished else 0.35)  # catch-light on upper-left
            if speck and rnd.random() < 0.13:
                col = mix(col, speck, 0.7)
            img.putpixel((x, y), col + (255,))
    return img


def casing_texture(stage, seed, hole=False, edges='tblr', notches=None):
    """Gearbox casing: a bevelled frame of the material around a sunken panel. Metal frames are riveted at the
    corners around a darker plate; wood is a dark board frame around upright planks. The side texture (hole=True)
    adds the bushing a shaft comes out of. edges names the sides that have a frame (top, bottom, left, right): a
    chain drive's faces leave the frame off where they run on into the next face or the next drive. notches, for a
    connected texture, names the inner corners ('tl', 'tr', 'bl', 'br') that get a corner of frame where the panel runs
    on past both sides but not across the diagonal; a connected texture also only rivets corners that have frame."""
    base, dark, light, speck = PALETTES[stage]
    rnd = random.Random(seed)
    wood = stage in WOODS
    # polished titanium's frame catches more light, and a sheen runs across its panel
    polished = stage in TINTED
    deep = mix(dark, (0, 0, 0), 0.45)
    streak = [rnd.uniform(-0.1, 0.1) for _ in range(16)]
    img = Image.new('RGBA', (16, 16))
    top, bottom, left, right = ('t' in edges), ('b' in edges), ('l' in edges), ('r' in edges)
    corner_px = set()
    for n in (notches or ()):
        xs = range(0, 2) if n[1] == 'l' else range(14, 16)
        ys = range(0, 2) if n[0] == 't' else range(14, 16)
        corner_px |= {(x, y) for x in xs for y in ys}
    for y in range(16):
        for x in range(16):
            if (left and x < 2) or (right and x > 13) or (top and y < 2) or (bottom and y > 13) or (x, y) in corner_px:
                # frame, lit on its top-left outer edge and shadowed on the bottom-right
                t = 0.55 + rnd.uniform(-0.07, 0.07)
                if x == 0 or y == 0:
                    t += 0.4 if polished else 0.25
                if x == 15 or y == 15:
                    t -= 0.4
                col = mix(dark, light, max(0.0, min(1.0, t)))
                if wood:
                    col = mix(col, deep, 0.4)
                    if (x * 3 + y) % 7 == 0:
                        col = mix(col, deep, 0.3)
                if speck and rnd.random() < 0.12:
                    col = mix(col, speck, 0.65)
            else:
                if wood:
                    # upright boards three pixels wide, grain running along them
                    board = (x - 2) // 3
                    t = 0.5 + streak[board] + rnd.uniform(-0.05, 0.05)
                    if (y + board * 3) % 6 == 0:
                        t -= 0.15
                    col = mix(dark, light, max(0.0, min(1.0, t)))
                    if (x - 2) % 3 == 2 and x < 13:
                        col = mix(col, deep, 0.6)
                else:
                    t = 0.45 + streak[x] * 1.2 + rnd.uniform(-0.05, 0.05)
                    if polished:
                        t += 0.9 * math.exp(-((x + y - 15) / 2.6) ** 2)
                    col = mix(mix(deep, dark, 0.7), mix(dark, base, 0.4), max(0.0, min(1.0, t)))
                    if speck and rnd.random() < 0.08:
                        col = mix(col, speck, 0.5)
                # the panel sits below the frame: shadow under the top and left lip, light on the far edges
                if (left and x == 2) or (top and y == 2):
                    col = mix(col, (0, 0, 0), 0.35)
                elif (right and x == 13) or (bottom and y == 13):
                    col = mix(col, light, 0.2)
            if not wood and (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
                framed = ((top if y < 8 else bottom) and (left if x < 8 else right)) or (x, y) in corner_px
                if notches is None or framed:
                    col = mix(dark, (0, 0, 0), 0.3)  # rivets
            img.putpixel((x, y), col + (255,))
    if hole:
        bushing(img, stage)
    return img


def omni_ct_index(up, down, left, right, tl, tr, bl, br):
    """Which tile of an omnidirectional connected-texture sheet a face shows: a port of Create's
    AllCTTypes.OMNIDIRECTIONAL.getTextureIndex. A side is True where the next block is the same casing; a corner is
    True where the diagonal one is too (and both sides beside it)."""
    tx = ty = 0
    borders = (not up) + (not down) + (not left) + (not right)
    tx += (1 if up else 0) + (2 if down else 0)
    ty += (1 if left else 0) + (2 if right else 0)
    if borders == 0:
        tx += (1 if tr else 0) + (2 if tl else 0)
        ty += (2 if br else 0) + (1 if bl else 0)
    if borders == 1:
        if not right and (tl or bl):
            ty, tx = 4, -1 + (1 if bl else 0) + (2 if tl else 0)
        if not left and (tr or br):
            ty, tx = 5, -1 + (1 if br else 0) + (2 if tr else 0)
        if not down and (tl or tr):
            ty, tx = 6, -1 + (1 if tl else 0) + (2 if tr else 0)
        if not up and (bl or br):
            ty, tx = 7, -1 + (1 if bl else 0) + (2 if br else 0)
    if borders == 2 and ((up and left and tl) or (down and left and bl) or (up and right and tr)
                         or (down and right and br)):
        tx += 3
    return tx + 8 * ty


def connected_casing_texture(stage, seed):
    """A casing's 8 by 8 connected-texture sheet, as Create's casings have: the frame only round the outside of a
    wall of casing, with a corner of frame where the wall turns in."""
    sheet = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    for bits in range(256):
        up, down, left, right, tl, tr, bl, br = [(bits >> i) & 1 == 1 for i in range(8)]
        tl, tr, bl, br = tl and up and left, tr and up and right, bl and down and left, br and down and right
        index = omni_ct_index(up, down, left, right, tl, tr, bl, br)
        edges = ('' if up else 't') + ('' if down else 'b') + ('' if left else 'l') + ('' if right else 'r')
        notches = [n for n, a, b, d in (('tl', up, left, tl), ('tr', up, right, tr), ('bl', down, left, bl),
                                        ('br', down, right, br)) if a and b and not d]
        sheet.paste(casing_texture(stage, seed, edges=edges, notches=notches), (index % 8 * 16, index // 8 * 16))
    return sheet


def bushing(img, stage):
    """The bushing in the middle of a casing face that a shaft comes out of: a dark hole in a bright ring."""
    base, dark, light, speck = PALETTES[stage]
    deep = mix(dark, (0, 0, 0), 0.45)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r < 3.1:
                col = mix((0x16, 0x12, 0x10), (0x30, 0x2A, 0x26), max(0.0, min(1.0, (r - 1.5) / 2)))
            elif r < 4.3:
                t = 0.55 - (x - 7.5) * 0.06 - (y - 7.5) * 0.06
                col = mix(dark, light, max(0.0, min(1.0, t)))
                if stage in WOODS:
                    col = mix(col, deep, 0.25)
            else:
                continue
            img.putpixel((x, y), col + (255,))


def redstone(lit, t):
    """A redstone indicator's colour, shaded by t (0 to 1): dull when unpowered, glowing when powered."""
    if lit:
        return mix((0xB4, 0x16, 0x0E), (0xFF, 0x70, 0x4E), t)
    return mix((0x3C, 0x0B, 0x0A), (0x70, 0x18, 0x14), t)


def clutch_texture(stage, seed, lit):
    """A casing side with the split between the two shaft halves drawn as a redstone line across the middle, glowing
    while the clutch is powered and the far half stands still."""
    img = casing_texture(stage, seed)
    for x in range(1, 15):
        img.putpixel((x, 6), mix(img.getpixel((x, 6))[:3], (0, 0, 0), 0.45) + (255,))
        img.putpixel((x, 9), mix(img.getpixel((x, 9))[:3], (0, 0, 0), 0.25) + (255,))
        glow = lit and 5 <= x <= 10
        img.putpixel((x, 7), redstone(lit, 0.85 if glow else 0.6) + (255,))
        img.putpixel((x, 8), redstone(lit, 0.55 if glow else 0.3) + (255,))
    return img


def gearshift_texture(stage, seed, lit):
    """A casing side with redstone strips down both sides of the panel, glowing while the gearshift is powered and
    the far half turns backwards."""
    img = casing_texture(stage, seed)
    for y in range(2, 14):
        glow = lit and 6 <= y <= 9
        for x, t in ((3, 0.75), (4, 0.4), (11, 0.75), (12, 0.4)):
            img.putpixel((x, y), redstone(lit, min(1.0, t + (0.2 if glow else 0))) + (255,))
        for x in (5, 10):
            img.putpixel((x, y), mix(img.getpixel((x, y))[:3], (0, 0, 0), 0.4) + (255,))
    return img


def frame_texture(stage, seed):
    """The casing's frame alone, for the ends a shaft comes out of: the inset plate behind shows through."""
    img = casing_texture(stage, seed)
    for y in range(2, 14):
        for x in range(2, 14):
            img.putpixel((x, y), (0, 0, 0, 0))
    return img


def chain_run(img, stage, x0, x1, top=5):
    """A run of chain in a channel six pixels deep (rows top to top + 5), from x0 up to x1, in the drive's own
    material: links seen flat, rings three pixels long, joined by links seen edge-on. The pattern repeats every four
    pixels, so it runs on unbroken into the next face and the next drive."""
    base, dark, light, speck = PALETTES[stage]
    hollow = mix(dark, (0, 0, 0), 0.75)
    for x in range(x0, x1):
        p = x % 4
        for row in range(6):
            y = top + row
            if row == 0:
                col = mix(dark, (0, 0, 0), 0.6)  # in the shadow of the lip above
            elif row == 5:
                col = mix(mix(dark, (0, 0, 0), 0.5), light, 0.15)  # the lower lip catches a little light
            else:
                col = hollow
                ring_top, ring_bottom = row == 1 and p == 1, row == 4 and p == 1
                ring_end = row in (2, 3) and p in (0, 2)
                edge_on = row in (2, 3) and p == 3
                if ring_top:
                    col = mix(base, light, 0.6)
                elif ring_bottom:
                    col = mix(base, dark, 0.55)
                elif ring_end:
                    col = mix(base, light, 0.3) if row == 2 else mix(base, dark, 0.2)
                elif edge_on:
                    col = mix(dark, base, 0.35) if row == 2 else mix(dark, (0, 0, 0), 0.15)
                if speck and col != hollow and (x * 7 + y * 3) % 5 == 0:
                    col = mix(col, speck, 0.55)
            img.putpixel((x, y), col + (255,))


def chain_drive_texture(stage, seed, kind, lit=False):
    """A chain drive's faces, in the casing of its material. 'side': the faces round the shaft, framed along the
    shaft's ends, with the chain running round the middle in a channel. 'end' and 'middle': the faces the shaft
    comes out of in a row, the chain running from behind the shaft's bushing out to the next drive on one side or on
    both. 'gearshift': a side with a redstone window over the chain, glowing while the gearshift is powered."""
    if kind == 'end':
        img = casing_texture(stage, seed, edges='tbl')
        chain_run(img, stage, 8, 16)
        bushing(img, stage)
    elif kind == 'middle':
        img = casing_texture(stage, seed, edges='tb')
        chain_run(img, stage, 0, 16)
        bushing(img, stage)
    else:
        img = casing_texture(stage, seed, edges='tb')
        chain_run(img, stage, 0, 16)
    if kind == 'gearshift':
        base, dark, light, speck = PALETTES[stage]
        for y in range(4, 12):
            for x in range(5, 11):
                if x in (5, 10) or y in (4, 11):
                    # the window's bezel, raised: lit above and to the left
                    t = 0.7 if (y == 4 or x == 5) else 0.25
                    col = mix(dark, light, t)
                else:
                    # redstone behind glass, brightest in the middle
                    t = 1.0 - (abs(x - 7.5) + abs(y - 7.5)) / 6
                    col = redstone(lit, max(0.0, min(1.0, t)))
                    if (x, y) == (6, 5):
                        col = mix(col, (255, 255, 255), 0.45 if lit else 0.25)  # glint on the glass
                img.putpixel((x, y), col + (255,))
    return img


def save_png(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def stone_inside_texture(seed, glow=None):
    """The furnace's sooty stone lining: rounded stones in dark mortar, like a cobblestone wall that has been burnt
    in for years. It tiles, so the long walls of the chamber show no seams. Lit, embers glow through the joints,
    brightest low down."""
    rnd = random.Random(seed)
    # one stone per cell of a jittered 4x3 grid, wider than tall, wrapped round the edges so the texture tiles
    seeds = []
    for gy in range(3):
        for gx in range(4):
            seeds.append(((gx + rnd.uniform(0.2, 0.8)) * 4, (gy + rnd.uniform(0.25, 0.75)) * 16 / 3))
    shade = [rnd.uniform(0.15, 0.85) for _ in seeds]

    def cell(x, y):
        best, which = None, 0
        for i, (sx, sy) in enumerate(seeds):
            dx = min(abs(x + .5 - sx), 16 - abs(x + .5 - sx))
            dy = min(abs(y + .5 - sy), 16 - abs(y + .5 - sy)) * 1.35
            d = dx * dx + dy * dy
            if best is None or d < best:
                best, which = d, i
        return which

    cells = [[cell(x, y) for x in range(16)] for y in range(16)]
    mortar, dark, light = (0x18, 0x16, 0x16), (0x2C, 0x2A, 0x2A), (0x50, 0x4C, 0x4A)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = cells[y][x]
            right, below = cells[y][(x + 1) % 16], cells[(y + 1) % 16][x]
            left, above = cells[y][(x - 1) % 16], cells[(y - 1) % 16][x]
            is_mortar = right != c or below != c
            if is_mortar:
                col = mortar
            else:
                col = mix(dark, light, shade[c] * 0.7 + rnd.uniform(0, 0.3))
                if left != c or above != c:
                    col = mix(col, light, 0.35)  # the stone's worn top and left edge
            if glow is not None:
                g = max(0.0, (y / 15.0) * 0.8 - 0.1 + rnd.uniform(-0.15, 0.15))
                col = mix(col, glow, g * (0.9 if is_mortar else 0.4))
            img.putpixel((x, y), col + (255,))
    return img


def ember_texture(seed, lit):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            v = rnd.random()
            if lit:
                col = mix((0x5A, 0x1A, 0x08), (0xFF, 0xC2, 0x4A), v ** 1.5)
            else:
                col = mix((0x2A, 0x26, 0x24), (0x8E, 0x88, 0x82), v ** 2)
            img.putpixel((x, y), col + (255,))
    return img


def grate_texture():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if x % 4 in (0, 1):
                shade = 0x5A if x % 4 == 0 else 0x3E
                img.putpixel((x, y), (shade, shade - 4, shade - 8, 255))
            elif y in (0, 15):
                img.putpixel((x, y), (0x46, 0x42, 0x3E, 255))
    return img


def lump_texture(seed, dark, light, accent=None, accent_chance=0.0, glow_center=False):
    """A rough lump for items like coke, blooms and slag."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx, cy = 7.5, 8.5
    radius = [5.2 + rnd.uniform(-1.2, 1.2) for _ in range(16)]
    import math
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            ang = int(((math.atan2(dy, dx) + math.pi) / (2 * math.pi)) * 16) % 16
            r = math.hypot(dx, dy * 1.15)
            if r > radius[ang]:
                continue
            t = 0.65 - (dy / 12.0) - (dx / 18.0) + rnd.uniform(-0.25, 0.25)
            col = mix(dark, light, max(0.0, min(1.0, t)))
            if r > radius[ang] - 1.0:
                col = mix(col, (0, 0, 0), 0.35)
            if accent and rnd.random() < accent_chance:
                col = mix(col, accent, 0.8)
            if glow_center and r < 2.6:
                col = mix(col, (0xFF, 0xD0, 0x60), 0.6 - r / 6.0)
            img.putpixel((x, y), col + (255,))
    return img


def powder_texture(seed, dark, base, light, clumps=0.0):
    """A little heap of powder: a low mound lit from the upper left, its grains speckled, with an occasional clump."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    tops = [13 - 8 * max(0.0, 1 - ((x - 7.5) / 6.5) ** 2) ** 0.7 + rnd.uniform(-0.5, 0.5) for x in range(16)]
    for x in range(16):
        for y in range(16):
            if y < tops[x] or y > 13 or tops[x] > 12.5:
                continue
            t = 0.75 - (x - 7.5) / 20 - (y - tops[x]) / 14 + rnd.uniform(-0.12, 0.12)
            col = mix(dark, base, min(1.0, t * 1.6)) if t < 0.62 else mix(base, light, min(1.0, (t - 0.62) * 2.6))
            if clumps and rnd.random() < clumps:
                col = mix(col, dark, 0.45)
            if y - tops[x] < 1 or y == 13 or x in (0, 15):
                col = mix(col, dark, 0.6)
            img.putpixel((x, y), col + (255,))
    return img


def metal_item_texture(shape, seed, outline, light, top, front, shadow, blisters=None):
    """An ingot (a bar seen from the front and above, its top face narrower), a nugget or a sheet (a plate lying
    tilted, with its front edge showing), shaded from a light upper left. blisters = (light, dark): bumps raised all
    over the bar, each a lit pixel over a shadowed one, the way carbon blisters the skin of cemented steel."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    rows = {}  # y: (x0, x1, face) with the outline drawn round the whole shape
    if shape == 'ingot':
        rows = {6: (4, 12, 't'), 7: (3, 13, 't'), 8: (2, 13, 'f'), 9: (2, 13, 'f'), 10: (2, 13, 's')}
        top_y, bottom_y = 5, 11
    elif shape == 'nugget':
        rows = {7: (6, 8, 't'), 8: (5, 9, 't'), 9: (5, 9, 'f')}
        top_y, bottom_y = 6, 10
    else:  # sheet: a slanted plate, two pixels of edge along the front
        for y in range(4, 11):
            x0 = 6 - (y - 4) // 2
            rows[y] = (x0, x0 + 8, 't')
        rows[11] = (3, 11, 'f')
        top_y, bottom_y = 3, 12
    shade = {'t': top, 'f': front, 's': shadow}
    for y, (x0, x1, kind) in rows.items():
        for x in range(x0, x1 + 1):
            t = rnd.uniform(-0.06, 0.06)
            col = mix(shade[kind], light, max(0.0, t)) if t > 0 else mix(shade[kind], shadow, -t)
            # a catch-light along the upper left edge, a darker lower right
            if kind == 't' and (x == x0 or y == min(rows)):
                col = mix(col, light, 0.6)
            if kind != 't' and x == x1:
                col = mix(col, shadow, 0.5)
            img.putpixel((x, y), col + (255,))
    if blisters:
        bl, bd = blisters
        spots = [(x, y) for y, (x0, x1, kind) in rows.items() for x in range(x0 + 1, x1) if kind != 's']
        rnd.shuffle(spots)
        for x, y in spots[:len(spots) // 5]:
            if img.getpixel((x, y))[3] and (x, y + 1) in [(sx, sy) for sx, sy in spots] + [(xx, max(rows)) for xx in range(16)]:
                img.putpixel((x, y), bl + (255,))
                img.putpixel((x, y + 1), bd + (255,))
    # outline: every empty pixel touching the shape
    shape_px = {(x, y) for y, (x0, x1, kind) in rows.items() for x in range(x0, x1 + 1)}
    for y in range(top_y, bottom_y + 1):
        for x in range(16):
            if (x, y) in shape_px:
                continue
            if any((x + dx, y + dy) in shape_px for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), outline + (255,))
    return img


# Ingots, nuggets and sheets drawn as Create draws its own (Taran, 0.26.0): Create's brass ingot, nugget and sheet, each
# pixel numbered by the shade of brass it is in, darkest 0 to brightest 8, and repainted in another material's nine
# shades, cooler or warmer in the shadows as Create's zinc is
CREATE_ITEM_SHAPES = {
    'ingot': ['................', '................', '..........22....', '.......222551...', '....2225566551..',
              '..2255666666551.', '.27666666776530.', '.25866777522230.', '.25587522111230.', '.24563111111230.',
              '.2446211112300..', '.02452123000....', '..0222000.......', '...000..........', '................',
              '................'],
    'nugget': ['................', '................', '................', '................', '......222.......',
               '.....25552......', '....2666560.....', '....2866720.....', '....2488320.....', '.....24530......',
               '......2520......', '.......00.......', '................', '................', '................',
               '................'],
    'sheet': ['................', '................', '................', '................', '........22......',
              '.....222551.....', '..2225656551....', '.277666656551...', '.0487766656551..', '..0487766666660.',
              '...058776677320.', '....05877322000.', '.....0522000....', '......000.......', '................',
              '................'],
}
hexes = lambda *cs: [((c >> 16) & 255, (c >> 8) & 255, c & 255) for c in cs]
# nine shades each, darkest first: steel dark and cool, aluminium near white, titanium a paler, warmer grey polished
# bright, PVC a soft matt white, Bakelite a glossy red-brown, blister steel a greyer steel
STEEL_SHADES = hexes(0x1A1B21, 0x25262D, 0x33343B, 0x44454C, 0x57585F, 0x6D6E74, 0x89898E, 0xADACAF, 0xD6D4D4)
ALUMINIUM_SHADES = hexes(0x4C5560, 0x66707C, 0x8792A0, 0xA3AEBA, 0xBCC5CF, 0xD3DAE2, 0xE6EBF0, 0xF4F7FA, 0xFFFFFF)
TITANIUM_SHADES = hexes(0x3A3538, 0x4E494B, 0x645F60, 0x7C7776, 0x96918F, 0xB0ACA9, 0xCAC6C2, 0xE2DED9, 0xF6F4F1)
PVC_SHADES = hexes(0x80848A, 0x9EA2A8, 0xB8BCC0, 0xCCCFD2, 0xDCDEE0, 0xE8EAEB, 0xF2F3F3, 0xF7F7F8, 0xFCFCFC)
BAKELITE_SHADES = hexes(0x1E0C06, 0x2C1209, 0x401A0C, 0x562410, 0x6C2E14, 0x823A1A, 0x9C4C24, 0xC07848, 0xE8B488)
BLISTER_STEEL_SHADES = hexes(0x222226, 0x2E2E33, 0x3E3E43, 0x505055, 0x646469, 0x7A7A7F, 0x929297, 0xB0B0B4, 0xD0D0D2)


def create_item_texture(shape, shades, seed=None, blisters=False):
    """An ingot, nugget or sheet in Create's own drawing, in `shades`. A blistered one (blister steel) has bumps raised
    over its lit faces, each a bright pixel over a dark one, the way carbon blisters the skin of cemented steel."""
    rows = CREATE_ITEM_SHAPES[shape]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != '.':
                img.putpixel((x, y), shades[int(c)] + (255,))
    if blisters:
        rnd = random.Random(seed)
        spots = [(x, y) for y, row in enumerate(rows) for x, c in enumerate(row)
                 if c in '56' and y + 1 < 16 and rows[y + 1][x] in '5678']
        rnd.shuffle(spots)
        for x, y in spots[:len(spots) // 4]:
            img.putpixel((x, y), shades[8] + (255,))
            img.putpixel((x, y + 1), shades[3] + (255,))
    return img


# steel, as the Carbon Electrode's stub is drawn: outline, light, top, front, shadow
STEEL_ITEM = ((0x18, 0x19, 0x1C), (0xAE, 0xAC, 0xAE), (0x7A, 0x79, 0x7C), (0x5A, 0x5A, 0x5E), (0x3E, 0x3E, 0x43))
# unfired clay, from the clay ball's colours: outline, light, top, front, shadow
CLAY_ITEM = ((0x37, 0x39, 0x44), (0xC4, 0xCA, 0xDA), (0xA1, 0xA7, 0xB1), (0x8A, 0x90, 0x9E), (0x66, 0x6E, 0x80))


def cementation_pack_texture(seed):
    """Iron bars packed in coal coke in a box of clay, its lid pressed down and sealed: seen like an ingot, from the
    front and above, with coke and the ends of the bars showing at the seam and thumb marks in the clay on top."""
    rnd = random.Random(seed)
    outline, light, top, front, shadow = CLAY_ITEM
    coke, iron = (0x24, 0x24, 0x28), (0xC8, 0xC8, 0xCC)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    rows = {4: (4, 12, 't'), 5: (3, 13, 't'), 6: (2, 13, 't'), 12: (2, 13, 's')}
    rows.update({y: (2, 13, 'f') for y in range(7, 12)})
    shade = {'t': top, 'f': front, 's': shadow}
    for y, (x0, x1, kind) in rows.items():
        for x in range(x0, x1 + 1):
            t = rnd.uniform(-0.08, 0.08)
            col = mix(shade[kind], light, t) if t > 0 else mix(shade[kind], shadow, -t)
            if kind == 't' and (x == x0 or y == min(rows)):
                col = mix(col, light, 0.6)
            if kind != 't' and x == x1:
                col = mix(col, shadow, 0.5)
            img.putpixel((x, y), col + (255,))
    # clay smoothed by hand: blue-grey streaks, as on a clay ball
    for _ in range(7):
        x, y = rnd.randint(3, 12), rnd.randint(9, 11)
        img.putpixel((x, y), mix(img.getpixel((x, y))[:3], (0x5E, 0x6C, 0x8D), 0.5) + (255,))
    for x in range(2, 14):
        img.putpixel((x, 8), mix(front, outline, 0.55) + (255,))
    for x, col in ((3, coke), (4, iron), (6, coke), (7, coke), (9, iron), (11, coke), (12, iron)):
        img.putpixel((x, 8), col + (255,))
    for x in (4, 9, 12):
        img.putpixel((x, 9), mix(iron, shadow, 0.5) + (255,))
    for x, y in ((6, 5), (9, 5)):
        img.putpixel((x, y), mix(top, shadow, 0.45) + (255,))
        img.putpixel((x + 1, y), mix(top, light, 0.3) + (255,))
    shape_px = {(x, y) for y, (x0, x1, kind) in rows.items() for x in range(x0, x1 + 1)}
    for y in range(3, 14):
        for x in range(16):
            if (x, y) not in shape_px and any((x + dx, y + dy) in shape_px
                                              for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), outline + (255,))
    return img


def leather_texture(seed):
    """Folded leather for the bellows bag: a crease every three rows, so squashing the bag squashes the folds."""
    rnd = random.Random(seed)
    # redder than spruce, so the bag stands out between the boards
    dark, mid, light = (0x4A, 0x22, 0x14), (0x82, 0x3E, 0x24), (0xAC, 0x5C, 0x36)
    img = Image.new('RGBA', (16, 16))
    sag = [rnd.uniform(-0.06, 0.06) for _ in range(16)]
    for y in range(16):
        for x in range(16):
            # each fold: lit top, body, shadowed crease; the folds sag a little between the corners
            col = [light, mid, dark][y % 3]
            col = mix(col, (0, 0, 0), max(0.0, 0.08 + sag[x] + rnd.uniform(-0.04, 0.04)))
            if x in (0, 15):
                col = mix(col, dark, 0.5)  # seams at the corners
            img.putpixel((x, y), col + (255,))
    return img


def nozzle_texture(seed):
    """Copper, with the bellows' blowing hole in the middle four pixels (uv 6-10)."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            col = mix((0xA4, 0x52, 0x34), (0xE6, 0x8A, 0x63), rnd.uniform(0.3, 0.8))
            if 6 <= x < 10 and 6 <= y < 10:
                col = (0xF2, 0xA0, 0x7A)  # polished lip
                if 7 <= x < 9 and 7 <= y < 9:
                    col = (0x1C, 0x13, 0x10)
            img.putpixel((x, y), col + (255,))
    return img


STONE_DARK, STONE_MID, STONE_LIGHT = (0x6E, 0x6D, 0x6B), (0x9C, 0x9B, 0x98), (0xBC, 0xBB, 0xB7)
# hand-placed cracks, in texture pixels: each wear stage adds to the last, so the damage reads at a glance
TOP_CRACKS = [
    [(3, 13), (3, 12), (4, 11), (4, 10), (5, 9), (6, 9), (6, 8)],  # chipped: from the edge into the strike zone
    [(12, 2), (12, 3), (11, 4), (11, 5), (10, 6), (10, 7), (9, 7)],  # damaged: one from the far edge...
    [(4, 10), (3, 9), (2, 9), (1, 8)],  # ...and a branch off the first
]
SIDE_CRACKS = [
    [(4, 2), (4, 3), (5, 4), (5, 5), (5, 6)],
    [(11, 2), (11, 3), (10, 4), (10, 5), (11, 6)],
]


def draw_crack(img, pts):
    for x, y in pts:
        img.putpixel((x, y), (0x36, 0x33, 0x31, 255))
    for x, y in pts:
        # a pale lip below the split, so it reads as a crack rather than a stain
        nx, ny = x + 1, y + 1
        if nx < 16 and ny < 16 and (nx, ny) not in pts and (x + 1, y) not in pts:
            r, g, b, _ = img.getpixel((nx, ny))
            img.putpixel((nx, ny), mix((r, g, b), STONE_LIGHT, 0.4) + (255,))


def stone_anvil_top(wear, seed):
    """The working face, smooth stone like the recipe's top row: a darker worn rim, a scorched strike zone flecked
    with iron scale, and cracks as it wears. The face uses rows 2-13 (the top spans z 2-14)."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            col = mix(STONE_MID, STONE_LIGHT, rnd.uniform(0.0, 0.45))
            if x in (0, 15) or y in (2, 13):
                col = mix(col, STONE_DARK, 0.45)
            elif x in (1, 14) or y in (3, 12):
                col = mix(col, STONE_LIGHT, 0.35)
            d = math.hypot((x - 7.5) / 4.6, (y - 7.5) / 3.2)
            if d < 1:  # scorched where hot blooms land
                col = mix(col, (0x55, 0x48, 0x40), (1 - d) * 0.6 + rnd.uniform(-0.06, 0.06))
                if rnd.random() < 0.10 * (1 - d) + 0.02:
                    col = mix(col, (0x9A, 0x55, 0x30), 0.7)
            img.putpixel((x, y), col + (255,))
    for crack in TOP_CRACKS[:[0, 1, 3][wear]]:
        draw_crack(img, crack)
    return img


def stone_anvil_side(wear, seed):
    """Sides of the top slab, which is five pixels deep and uses rows 2-6: a lit upper edge, a chiselled face and a
    shadowed lower edge. The other rows repeat the pattern, for break particles."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            col = mix(STONE_DARK, STONE_MID, rnd.uniform(0.45, 0.9))
            band = (y - 2) % 5
            if band == 0:
                col = mix(col, STONE_LIGHT, 0.55)
            elif band == 4:
                col = mix(col, STONE_DARK, 0.6)
            elif (x * 2 + y) % 7 == 0:
                col = mix(col, STONE_DARK, 0.35)  # chisel marks
            img.putpixel((x, y), col + (255,))
    for crack in SIDE_CRACKS[:wear]:
        draw_crack(img, crack)
    return img


def stone_anvil_broken(seed):
    """Freshly broken stone, where chips have come off a worn anvil: paler and rough."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            col = mix(STONE_MID, STONE_LIGHT, rnd.uniform(0.3, 1.0))
            if rnd.random() < 0.12:
                col = mix(col, STONE_DARK, 0.7)
            img.putpixel((x, y), col + (255,))
    return img


# saw blades, drawn like vanilla's stonecutter blade: the top half of a disc centred on the bottom edge of the
# texture, which Create's blade models show (rows 8-16). Each material has its own edge, and the pattern repeats
# every three teeth so the animation, which turns the blade three teeth, loops.
BLADE_CENTRE, BLADE_BODY, BLADE_TIP = (8.0, 16.0), 5.9, 7.7
BLADE_REPEAT, BLADE_FRAMES = 3, 9
BLADES = {
    # chipped stone: uneven teeth, grit that turns with the disc
    'stone': {'teeth': 18, 'edge': 'chipped', 'hub': (0x4E, 0x4C, 0x49),
              'body': ((0x5A, 0x58, 0x55), (0xA6, 0xA3, 0x9E)), 'tooth': ((0x74, 0x72, 0x6E), (0xCE, 0xCB, 0xC4))},
    # gold: hooked teeth and a polished band
    'gold': {'teeth': 18, 'edge': 'hooked', 'hub': (0x8E, 0x62, 0x14),
             'body': ((0xB8, 0x86, 0x1A), (0xFF, 0xE6, 0x6E)), 'tooth': ((0xC8, 0x92, 0x16), (0xFF, 0xF6, 0xB0))},
    # diamond: a steel disc with diamond segments set into its rim, slots between them
    'diamond': {'teeth': 15, 'edge': 'segmented', 'hub': (0x4A, 0x50, 0x58),
                'body': ((0x5E, 0x64, 0x6C), (0xC2, 0xC9, 0xD0)), 'tooth': ((0x1E, 0x9C, 0x92), (0xB8, 0xFF, 0xF6))},
}


def blade_point(blade, x, y, turn):
    """What the blade is at a point: None outside it, else (part, shade, place within the tooth, which tooth)."""
    dx, dy = x - BLADE_CENTRE[0], BLADE_CENTRE[1] - y
    r = math.hypot(dx, dy)
    if r > BLADE_TIP + .3:
        return None
    a = (math.atan2(dy, dx) + turn) * blade['teeth'] / (2 * math.pi)
    u, tooth = a % 1.0, int(math.floor(a)) % BLADE_REPEAT
    edge = blade['edge']
    if edge == 'hooked':
        tip = BLADE_BODY - .3 + (BLADE_TIP - BLADE_BODY + .3) * u
    elif edge == 'chipped':
        tip = BLADE_BODY - .2 + (BLADE_TIP - BLADE_BODY + .2) * (.6, 1.0, .8)[tooth] * (1 - abs(u - .5) * 2) ** .7
    else:
        tip = BLADE_TIP - .25 if u > .22 else BLADE_BODY - 1.2
    if r > tip:
        return None
    if r < 2.4:
        return 'hub', 0, u, tooth
    if r > BLADE_BODY - .35 and (edge != 'segmented' or u > .22):
        return 'tooth', min(1.0, max(0.0, (r - BLADE_BODY) / (BLADE_TIP - BLADE_BODY))), u, tooth
    return 'body', r / BLADE_BODY, u, tooth


def saw_blade_frame(material, turn, ss=4):
    """One frame of a blade, supersampled: a pixel is blade if most of it is, coloured by the part most of it is."""
    blade = BLADES[material]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for py in range(7, 16):
        for px in range(16):
            hits = [p for sy in range(ss) for sx in range(ss)
                    if (p := blade_point(blade, px + (sx + .5) / ss, py + (sy + .5) / ss, turn)) is not None]
            if len(hits) * 2 < ss * ss:
                continue
            parts = {}
            for p in hits:
                parts.setdefault(p[0], []).append(p)
            part = max(parts, key=lambda k: len(parts[k]))
            ps = parts[part]
            t = sum(p[1] for p in ps) / len(ps)
            u, tooth = ps[len(ps) // 2][2], ps[len(ps) // 2][3]
            if part == 'hub':
                col = blade['hub']
            elif part == 'tooth':
                col = mix(blade['tooth'][0], blade['tooth'][1], .35 + .65 * t)
            else:
                s = .35 + .45 * t
                if blade['edge'] == 'chipped':
                    s += (random.Random(f'grit/{tooth}/{int(u * 2)}/{int(t * 4)}').random() - .5) * .5
                elif blade['edge'] == 'hooked':
                    s += .35 if .62 < t < .8 else 0
                else:
                    s -= .2 if .55 < t < .7 else 0
                col = mix(blade['body'][0], blade['body'][1], max(0.0, min(1.0, s)))
            img.putpixel((px, py), col + (255,))
    # outline: the upper left rim catches the light, every other outer edge is darker
    src = img.load()
    out = img.copy()
    dst = out.load()
    for py in range(7, 16):
        for px in range(16):
            if src[px, py][3] == 0:
                continue
            up = src[px, py - 1][3] == 0
            side = any(not 0 <= px + d < 16 or src[px + d, py][3] == 0 for d in (-1, 1))
            col = src[px, py][:3]
            if up and px < 8:
                col = mix(col, (255, 255, 255), .25)
            elif up or side:
                col = mix(col, (0, 0, 0), .3)
            dst[px, py] = col + (255,)
    return out


def saw_blade_strip(material):
    """The turning blade: BLADE_FRAMES frames, a third of a tooth apart, stacked for a .mcmeta animation."""
    teeth = BLADES[material]['teeth']
    img = Image.new('RGBA', (16, 16 * BLADE_FRAMES), (0, 0, 0, 0))
    for k in range(BLADE_FRAMES):
        img.paste(saw_blade_frame(material, -k / BLADE_FRAMES * BLADE_REPEAT * 2 * math.pi / teeth), (0, 16 * k))
    return img


# item icons for drill bits and saw blades (DrillBits, SawBlades). Bits are a fluted cone pointing up and right like a
# tool, on the iron shank they are set on; a blade is the whole disc, on the iron sheet at its middle
BIT_COLOURS = {
    'stone': ((0x4E, 0x4C, 0x49), (0xB4, 0xB1, 0xAB)),
    'iron': ((0x6A, 0x6A, 0x6A), (0xF0, 0xF0, 0xF0)),
    'gold': ((0x9A, 0x6A, 0x10), (0xFF, 0xF0, 0x8A)),
    'diamond': ((0x16, 0x80, 0x78), (0xC8, 0xFF, 0xF8)),
}
ICON_IRON = ((0x50, 0x50, 0x54), (0xB8, 0xB8, 0xBC))
# Create's iron blade is vanilla's stonecutter blade; its icon gets hooked teeth like gold's
ICON_BLADES = dict(BLADES, iron={'teeth': 16, 'edge': 'hooked', 'hub': (0x5C, 0x5C, 0x5C),
                                 'body': ((0x80, 0x80, 0x80), (0xE2, 0xE2, 0xE2)),
                                 'tooth': ((0x9A, 0x9A, 0x9A), (0xFF, 0xFF, 0xFF))})


def icon_outline(img):
    """An item's rim: dark on the lower right edges, a little dark on the upper left."""
    src = img.load()
    out = img.copy()
    dst = out.load()

    def clear(x, y):
        return not (0 <= x < 16 and 0 <= y < 16) or src[x, y][3] == 0

    for py in range(16):
        for px in range(16):
            if src[px, py][3] == 0:
                continue
            col = src[px, py][:3]
            if clear(px + 1, py) or clear(px, py + 1):
                col = mix(col, (0, 0, 0), .45)
            elif clear(px - 1, py) or clear(px, py - 1):
                col = mix(col, (0, 0, 0), .2)
            dst[px, py] = col + (255,)
    return out


def drill_bits_icon(material, ss=4):
    dark, light = BIT_COLOURS[material]
    base, tip, radius, flutes, band = (5.2, 10.8), (14.4, 1.6), 5.0, 2.2, .45
    length = math.hypot(tip[0] - base[0], tip[1] - base[1])
    ux, uy = (tip[0] - base[0]) / length, (tip[1] - base[1]) / length
    rnd = random.Random('bits/' + material)

    def where(x, y):
        dx, dy = x - base[0], y - base[1]
        u, v = (dx * ux + dy * uy) / length, dx * -uy + dy * ux
        if 0 <= u <= 1 and abs(v) <= radius * (1 - u):
            return 'bit', u, v / max(radius * (1 - u), .01)
        r = 1.5 * (1.4 if u > -.08 else 1)  # a collar where the shank meets the cone
        if -.36 <= u < 0 and abs(v) <= r:
            return 'shank', u, v / r
        return None

    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for py in range(16):
        for px in range(16):
            hits = [h for sy in range(ss) for sx in range(ss) if (h := where(px + (sx + .5) / ss, py + (sy + .5) / ss))]
            if len(hits) * 2 < ss * ss:
                continue
            kinds = [h[0] for h in hits]
            kind = max(set(kinds), key=kinds.count)
            hs = [h for h in hits if h[0] == kind]
            u, across = sum(h[1] for h in hs) / len(hs), sum(h[2] for h in hs) / len(hs)
            if kind == 'shank':
                col = mix(*ICON_IRON, max(0, min(1, .62 - across * .4)))
            else:
                # a flute winding round the cone, in shadow, and the lip of its cutting edge catching the light
                phase = (u * length / flutes - across * .5) % 1.0
                t = .7 - across * .3 + (-.45 if phase < band else .2 if phase < band + .12 else 0)
                if material == 'stone':
                    t += rnd.uniform(-.18, .18)
                col = mix(dark, light, max(0, min(1, t)))
            img.putpixel((px, py), col + (255,))
    return icon_outline(img)


def saw_blade_icon(material, ss=4):
    global BLADE_CENTRE
    blade = ICON_BLADES[material]
    saved, BLADE_CENTRE = BLADE_CENTRE, (8.0, 8.0)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    try:
        for py in range(16):
            for px in range(16):
                hits = [p for sy in range(ss) for sx in range(ss)
                        if (p := blade_point(blade, px + (sx + .5) / ss, py + (sy + .5) / ss, .3)) is not None]
                if len(hits) * 2 < ss * ss:
                    continue
                parts = {}
                for p in hits:
                    parts.setdefault(p[0], []).append(p)
                part = max(parts, key=lambda k: len(parts[k]))
                ps = parts[part]
                t = sum(p[1] for p in ps) / len(ps)
                u, tooth = ps[len(ps) // 2][2], ps[len(ps) // 2][3]
                light = -(px + py - 15) / 40  # from the upper left, across the disc
                if part == 'hub':
                    r = math.hypot(px + .5 - 8, py + .5 - 8)
                    col = (0x30, 0x30, 0x32) if r < 1.0 else mix(*ICON_IRON, .7 + light * 3)
                elif part == 'tooth':
                    col = mix(blade['tooth'][0], blade['tooth'][1], .35 + .65 * t)
                else:
                    s = .35 + .45 * t + light
                    if blade['edge'] == 'chipped':
                        s += (random.Random(f'grit/{tooth}/{int(u * 2)}/{int(t * 4)}').random() - .5) * .5
                    elif blade['edge'] == 'hooked':
                        s += .35 if .62 < t < .8 else 0
                    else:
                        s -= .2 if .55 < t < .7 else 0
                    col = mix(blade['body'][0], blade['body'][1], max(0.0, min(1.0, s)))
                img.putpixel((px, py), col + (255,))
    finally:
        BLADE_CENTRE = saved
    return icon_outline(img)


def gen_textures():
    for i, stage in enumerate(TEXTURE_STAGES):
        if not holds_only(stage):
            save_png(material_texture(stage, 'axis', 100 + i), asset('textures', 'block', stage + '_axis.png'))
            save_png(material_texture(stage, 'axis_top', 200 + i), asset('textures', 'block', stage + '_axis_top.png'))
        # brass's are polished and decorated, for building with (Taran, 0.23.0)
        casing = brass_casing_texture if stage == 'brass' else lambda seed, **k: casing_texture(stage, seed, **k)
        save_png(casing(600 + i), asset('textures', 'block', stage + '_casing.png'))
        save_png(casing(600 + i, hole=True), asset('textures', 'block', stage + '_gearbox.png'))
        # brass has no clutch or gearshift (NO_BRASS), whose frame this is
        if stage != 'brass':
            save_png(frame_texture(stage, 600 + i), asset('textures', 'block', stage + '_frame.png'))
            for lit, state in ((False, 'off'), (True, 'on')):
                save_png(clutch_texture(stage, 600 + i, lit), asset('textures', 'block', f'{stage}_clutch_{state}.png'))
                save_png(gearshift_texture(stage, 600 + i, lit),
                         asset('textures', 'block', f'{stage}_gearshift_{state}.png'))
        # every material's bracket, Create's in its colours; brass's polished and engraved
        # (brass's is a clock bridge in its clockwork brass instead, clock_bridge_models)
        for k, kind in enumerate(('bracket', 'bracket_plate')):
            if stage != 'brass':
                save_png(bracket_texture(kind, stage, 700 + 100 * k + i),
                         asset('textures', 'block', f'{stage}_{kind}.png'))
        if stage in WOODS:
            continue
        chain = brass_chain_drive_texture if stage == 'brass' \
            else lambda seed, kind: chain_drive_texture(stage, seed, kind)
        for kind in ('end', 'middle'):
            save_png(chain(600 + i, kind), asset('textures', 'block', f'{stage}_chain_drive_{kind}.png'))
        save_png(chain(600 + i, 'side'), asset('textures', 'block', f'{stage}_chain_drive.png'))
        for lit, state in ((False, 'off'), (True, 'on')):
            if stage != 'brass':
                save_png(chain_drive_texture(stage, 600 + i, 'gearshift', lit),
                         asset('textures', 'block', f'{stage}_chain_gearshift_{state}.png'))
        if holds_only(stage):
            continue
        save_png(material_texture(stage, 'cogwheel_axis', 300 + i), asset('textures', 'block', stage + '_cogwheel_axis.png'))
        save_png(material_texture(stage, 'cogwheel', 400 + i), asset('textures', 'block', stage + '_cogwheel.png'))
        save_png(material_texture(stage, 'large_cogwheel', 500 + i), asset('textures', 'block', stage + '_large_cogwheel.png'))

    save_png(brass_clockwork_texture(1301), asset('textures', 'block', 'brass_clockwork.png'))

    save_png(stone_inside_texture(7), asset('textures', 'block', 'kiln_inside.png'))
    save_png(stone_inside_texture(7, glow=(0xFF, 0x8A, 0x2A)), asset('textures', 'block', 'kiln_inside_lit.png'))
    save_png(ember_texture(11, True), asset('textures', 'block', 'kiln_embers.png'))
    save_png(ember_texture(11, False), asset('textures', 'block', 'kiln_ash.png'))
    save_png(grate_texture(), asset('textures', 'block', 'kiln_grate.png'))
    save_png(leather_texture(31), asset('textures', 'block', 'bellows_leather.png'))
    save_png(nozzle_texture(32), asset('textures', 'block', 'bellows_nozzle.png'))
    for wear, name in enumerate(STONE_ANVILS):
        save_png(stone_anvil_top(wear, 40), asset('textures', 'block', name + '_top.png'))
        save_png(stone_anvil_side(wear, 41), asset('textures', 'block', name + '_side.png'))
    save_png(stone_anvil_broken(50), asset('textures', 'block', 'stone_anvil_broken.png'))

    for blade in SAWS:
        save_png(saw_blade_strip(blade), asset('textures', 'block', blade_name(blade) + '.png'))
        write_json(asset('textures', 'block', blade_name(blade) + '.png.mcmeta'), {'animation': {'frametime': 1}})
        save_png(saw_blade_frame(blade, 0), asset('textures', 'block', blade_name(blade) + '_static.png'))
    for material in FITTINGS:
        save_png(drill_bits_icon(material), asset('textures', 'item', bits_name(material) + '.png'))
        save_png(saw_blade_icon(material), asset('textures', 'item', blade_name(material) + '.png'))

    # Steel Casing is the steel gearbox's casing texture as a block, connected like Create's casings, and Iron Casing
    # and Aluminium Casing are their gearboxes' the same way (an Iron Casing never rusts, as Create's Copper Casing
    # never weathers)
    for metal in ('iron', 'steel', 'aluminium'):
        save_png(connected_casing_texture(metal, 600 + TEXTURE_STAGES.index(metal)),
                 asset('textures', 'block', f'{metal}_casing_connected.png'))

    save_png(lump_texture(21, (0x2C, 0x2C, 0x30), (0x8A, 0x8C, 0x92), accent=(0x18, 0x18, 0x1A), accent_chance=0.25),
             asset('textures', 'item', 'coal_coke.png'))
    save_png(lump_texture(22, (0x6A, 0x30, 0x18), (0xF0, 0x86, 0x3A), accent=(0x40, 0x36, 0x30), accent_chance=0.2,
                          glow_center=True), asset('textures', 'item', 'iron_bloom.png'))
    save_png(lump_texture(23, (0x1E, 0x26, 0x24), (0x6E, 0x80, 0x76), accent=(0x9C, 0xB4, 0x5A), accent_chance=0.08),
             asset('textures', 'item', 'slag.png'))
    for shape in ('ingot', 'nugget', 'sheet'):
        save_png(create_item_texture(shape, STEEL_SHADES), asset('textures', 'item', f'steel_{shape}.png'))
        save_png(create_item_texture(shape, ALUMINIUM_SHADES), asset('textures', 'item', f'aluminium_{shape}.png'))
    save_png(create_item_texture('sheet', BAKELITE_SHADES), asset('textures', 'item', 'bakelite.png'))
    save_png(lump_texture(1245, (0x5A, 0x22, 0x12), (0xC4, 0x6A, 0x3C), accent=(0xE8, 0xC8, 0x9C), accent_chance=0.1),
             asset('textures', 'item', 'bauxite.png'))
    save_png(powder_texture(1246, (0x8C, 0x94, 0xA0), (0xD4, 0xD8, 0xDE), (0xFA, 0xFC, 0xFF), clumps=0.12),
             asset('textures', 'item', 'aluminium_hydroxide.png'))
    save_png(powder_texture(1247, (0xA8, 0xA8, 0xB0), (0xEC, 0xEC, 0xEE), (0xFF, 0xFF, 0xFF)),
             asset('textures', 'item', 'alumina.png'))
    save_png(create_item_texture('ingot', BLISTER_STEEL_SHADES, 27, blisters=True),
             asset('textures', 'item', 'blister_steel.png'))
    save_png(cementation_pack_texture(28), asset('textures', 'item', 'cementation_pack.png'))


# ------------------------------------------------------------------------------------------------ models

def face(tex, cull=None):
    f = {'texture': tex}
    if cull:
        f['cullface'] = cull
    return f


def box(frm, to, faces):
    return {'from': frm, 'to': to, 'faces': faces}


def all_faces(tex, skip=()):
    return {d: face(tex) for d in ['north', 'south', 'east', 'west', 'up', 'down'] if d not in skip}


def kiln_model(chimney=True):
    """The Furnace (mcremix:kiln; the kiln in code), in vanilla's furnace textures so it reads as a furnace at a
    glance. Its front (north) is cut open: a low firebox opening (y 2-5) and a chamber opening (y 7-12) above a grate,
    with the side texture running on round them, so the stonework matches the other faces. The chimney stub on top
    is left out when something rests on it (KilnBlock.CHIMNEY)."""
    S, T, I, E, G = '#side', '#top', '#inside', '#embers', '#grate'
    elements = [
        box([0, 0, 0], [16, 2, 16], {'north': face(S, 'north'), 'south': face(S, 'south'), 'east': face(S, 'east'),
                                    'west': face(S, 'west'), 'down': face(T, 'down'), 'up': face(I)}),
        box([0, 2, 0], [2, 14, 16], {'north': face(S, 'north'), 'south': face(S, 'south'), 'west': face(S, 'west'),
                                    'east': face(I)}),
        box([14, 2, 0], [16, 14, 16], {'north': face(S, 'north'), 'south': face(S, 'south'), 'east': face(S, 'east'),
                                      'west': face(I)}),
        box([2, 2, 14], [14, 14, 16], {'south': face(S, 'south'), 'north': face(I)}),
        box([0, 14, 0], [16, 16, 16], {'north': face(S, 'north'), 'south': face(S, 'south'), 'east': face(S, 'east'),
                                      'west': face(S, 'west'), 'up': face(T, 'up'), 'down': face(I)}),
        box([2, 5, 0], [14, 6, 2], {'north': face(S, 'north'), 'up': face(T), 'down': face(I), 'south': face(I)}),
        box([2, 12, 0], [14, 14, 2], {'north': face(S, 'north'), 'down': face(I), 'south': face(I)}),
        box([2, 6, 2], [14, 7, 14], {'up': face(G), 'down': face(G), 'north': face(G)}),
        box([2, 2, 1], [14, 2.5, 14], {'up': face(E), 'north': face(E)}),
    ]
    if chimney:
        # a stone flue, its sides cut from the top rows of the side texture where vanilla's furnace has its rim. The
        # stub stands above the block, so its UVs are given: worked out from its position they would run off the
        # texture and pick up whatever lies next to it in the atlas.
        def rim():
            return {'texture': S, 'uv': [6, 0, 10, 4]}
        elements.append(box([6, 16, 10], [10, 20, 14], {'north': rim(), 'south': rim(), 'east': rim(), 'west': rim(),
                                                       'up': face(I)}))
    return {
        'parent': 'block/block',
        'textures': {'particle': 'minecraft:block/furnace_side', 'side': 'minecraft:block/furnace_side',
                     'top': 'minecraft:block/furnace_top', 'inside': 'mcremix:block/kiln_inside',
                     'embers': 'mcremix:block/kiln_ash', 'grate': 'mcremix:block/kiln_grate'},
        'elements': elements,
    }


def bellows_parts():
    """The bellows, nozzle to the north. The static block model holds the base, the casing the air leaves through,
    the copper nozzle and the bearing under the shaft. Three partials move: the lid with the yoke hanging off its back,
    the leather bag (squashed from y 2), and the crank (shaft stub, arm and the pin that rides in the yoke slot). The crank pin sits 2.5 px from the shaft, so the lid travels 5 px; BellowsBlockEntity.STROKE."""
    P, C, N, L, M = '#planks', '#casing', '#nozzle', '#leather', '#metal'

    def boxed(frm, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down'), cull=()):
        return box(frm, to, {d: face(tex, d if d in cull else None) for d in faces})

    static = [
        boxed([0, 0, 0], [16, 2, 16], P, cull=('north', 'south', 'east', 'west', 'down')),
        boxed([3, 2, 1], [13, 8, 4], C, faces=('north', 'east', 'west', 'up')),
        box([6, 3, 0], [10, 7, 1], {'north': {'texture': N, 'uv': [6, 6, 10, 10], 'cullface': 'north'},
                                    'east': {'texture': N, 'uv': [0, 0, 1, 4]},
                                    'west': {'texture': N, 'uv': [0, 0, 1, 4]},
                                    'up': {'texture': N, 'uv': [0, 0, 4, 1]},
                                    'down': {'texture': N, 'uv': [0, 0, 4, 1]}}),
        boxed([5, 2, 14], [11, 6, 16], C, faces=('north', 'south', 'east', 'west', 'up'), cull=('south',)),
    ]
    lid = [
        boxed([1, 13, 3], [15, 15, 13], P),
        boxed([4, 11, 12], [12, 13, 13], M, faces=('south', 'north', 'east', 'west', 'down')),
        boxed([4, 10, 12], [5, 11, 13], M, faces=('south', 'north', 'west', 'east')),
        boxed([11, 10, 12], [12, 11, 13], M, faces=('south', 'north', 'west', 'east')),
        boxed([4, 9, 12], [12, 10, 13], M, faces=('south', 'north', 'east', 'west', 'up', 'down')),
    ]
    bag = [boxed([2, 2, 4], [14, 13, 12], L, faces=('north', 'south', 'east', 'west'))]
    crank = [
        {'from': [6, 6, 14], 'to': [10, 10, 16], 'faces': {
            'north': {'uv': [6, 6, 10, 10], 'rotation': 180, 'texture': '#axis_top'},
            'east': {'uv': [6, 0, 10, 2], 'rotation': 270, 'texture': '#axis'},
            'south': {'uv': [6, 6, 10, 10], 'texture': '#axis_top'},
            'west': {'uv': [6, 0, 10, 2], 'rotation': 90, 'texture': '#axis'},
            'up': {'uv': [6, 0, 10, 2], 'rotation': 180, 'texture': '#axis'},
            'down': {'uv': [6, 0, 10, 2], 'texture': '#axis'}}},
        boxed([7, 7, 13], [9, 12, 14], M),
        boxed([7.5, 10, 12], [8.5, 11, 13], M, faces=('north', 'east', 'west', 'up', 'down')),
    ]
    textures = {'particle': 'minecraft:block/spruce_planks', 'planks': 'minecraft:block/spruce_planks',
                'casing': 'create:block/andesite_casing', 'nozzle': 'mcremix:block/bellows_nozzle',
                'leather': 'mcremix:block/bellows_leather', 'metal': 'create:block/andesite_block',
                'axis': 'create:block/axis', 'axis_top': 'create:block/axis_top'}

    def model(elements):
        used = {'particle'} | {f['texture'][1:] for e in elements for f in e['faces'].values()}
        return {'parent': 'block/block', 'textures': {k: v for k, v in textures.items() if k in used},
                'elements': elements}

    return {'bellows': model(static), 'bellows/lid': model(lid), 'bellows/bag': model(bag),
            'bellows/crank': model(crank), 'item': model(static + bag + lid + crank)}


STONE_ANVILS = ['stone_anvil', 'chipped_stone_anvil', 'damaged_stone_anvil']
# drill bits (DrillBits) and saw blades (SawBlades), items fitted to any drill or saw, Create's included: the texture
# fitted bits give Create's drill head (iron bits keep Create's own head), and the blades with their own texture
# (an iron blade is Create's own)
FITTINGS = ('stone', 'iron', 'gold', 'diamond')
BITS = {'stone': 'minecraft:block/cobblestone', 'gold': 'minecraft:block/gold_block',
        'diamond': 'minecraft:block/diamond_block'}
SAWS = ('stone', 'gold', 'diamond')


# machines that make things (MachineKind): Create's id, and the material Create's own counts as. All but the crafter
# come in every tier below as well, a tier being Create's machine in that material's casing; the mechanical crafter
# comes in Create's brass alone (Taran, 0.23.0)
MACHINES = {'mechanical_press': 'andesite', 'mechanical_mixer': 'andesite', 'millstone': 'andesite',
            'encased_fan': 'andesite', 'mechanical_saw': 'andesite', 'mechanical_drill': 'andesite',
            'deployer': 'andesite', 'mechanical_crafter': 'brass', 'mechanical_arm': 'brass'}
TIERED_MACHINES = [kind for kind in MACHINES if kind != 'mechanical_crafter']
MACHINE_TIERS = ('copper', 'iron', 'steel', 'aluminium', 'titanium')


def machines(*kinds):
    """Every tier of these machines (all of them when none are named), as block ids."""
    return [f'mcremix:{tier}_{kind}' for kind in TIERED_MACHINES if not kinds or kind in kinds
            for tier in MACHINE_TIERS]


def bits_name(material):
    return ('golden' if material == 'gold' else material) + '_drill_bits'


def blade_name(material):
    return ('golden' if material == 'gold' else material) + '_saw_blade'


# each tier's casing (EncasingCasing), which its machines are crafted with and swapped to
TIER_CASINGS = {'copper': 'create:copper_casing', 'iron': 'mcremix:iron_casing',
                'steel': 'mcremix:steel_casing', 'aluminium': 'mcremix:aluminium_casing',
                'titanium': 'mcremix:titanium_casing'}
# the textures of Create's models for each machine that a tier changes, the block's, the item's and those of the arm's
# moving parts (ModPartialModels): a texture of tools/create_machine_textures.json in the tier's colours, or the tier's
# own casing, gearbox face or chain drive. The rest (heads, blades, axles, the arm's cog) stay Create's
MACHINE_MODELS = {
    'mechanical_press': {
        'block': {'4': 'mechanical_press_side', 'particle': 'mechanical_press_side', 'gearbox_top': 'casing',
                  'gearbox': 'gearbox', 'mechanical_press_top': 'mechanical_press_top',
                  'mechanical_press_bottom': 'mechanical_press_bottom'},
        'item': {'8': 'mechanical_press_side', 'particle': 'mechanical_press_side', 'gearbox_top': 'casing',
                 'gearbox': 'gearbox', 'mechanical_press_top': 'mechanical_press_top',
                 'mechanical_press_bottom': 'mechanical_press_bottom'}},
    'mechanical_mixer': {
        'block': {'2': 'casing', '4': 'mixer_base_side', '11': 'mechanical_press_top', 'particle': 'mixer_base_side'},
        'item': {'2': 'casing', '4': 'mixer_base_side', '11': 'mechanical_press_top', 'particle': 'mixer_base_side'}},
    'millstone': {
        'block': {'0': 'gearbox', '5': 'millstone', 'particle': 'casing'},
        'item': {'0': 'gearbox', '5': 'millstone', 'particle': 'casing'}},
    'encased_fan': {
        'block': {'3': 'andesite_funnel_frame', 'back': 'gearbox', 'fan_casing': 'fan_casing', 'particle': 'fan_side',
                  'fan_side': 'fan_side'},
        'item': {'2': 'gearbox', 'fan_casing': 'fan_casing', 'particle': 'fan_side', 'fan_side': 'fan_side'}},
    'mechanical_saw': {
        'horizontal': {'slit': 'mechanical_saw_top_no_slot', 'particle': 'casing', 'gearbox_top': 'casing',
                       'encased_belt': 'chain_drive', 'gearbox': 'gearbox', 'andesite_casing_short': 'andesite_casing_short'},
        'vertical': {'gearbox_top': 'casing', 'gearbox': 'gearbox', 'andesite_casing_short': 'andesite_casing_short',
                     'particle': 'mechanical_saw_top', 'mechanical_saw_top': 'mechanical_saw_top'},
        'item': {'slit': 'mechanical_saw_top_no_slot', 'particle': 'casing', 'gearbox_top': 'casing',
                 'encased_belt': 'chain_drive', 'gearbox': 'gearbox', 'andesite_casing_short': 'andesite_casing_short'}},
    'mechanical_drill': {
        'block': {'4': 'mechanical_drill_top', '10': 'andesite_casing_very_short', 'gearbox_top': 'casing',
                  'particle': 'gearbox', 'gearbox': 'gearbox'},
        'item': {'8': 'mechanical_drill_top', '10': 'andesite_casing_very_short', 'gearbox': 'gearbox'}},
    'deployer': {
        'horizontal': {'5': 'andesite_casing_piston', '6': 'piston_bottom', '7': 'piston_inner', 'particle': 'casing',
                       'gearbox_top': 'casing', 'gearbox': 'gearbox', 'andesite_casing_short': 'andesite_casing_short'},
        'vertical': {'5': 'andesite_casing_piston', '6': 'piston_bottom', '7': 'piston_inner', 'particle': 'casing',
                     'gearbox_top': 'casing', 'gearbox': 'gearbox', 'andesite_casing_short': 'andesite_casing_short'},
        'item': {'10': 'andesite_casing_piston', '6': 'piston_bottom', '7': 'piston_inner', 'gearbox_top': 'casing',
                 'gearbox': 'gearbox', 'andesite_casing_short': 'andesite_casing_short'}},
    'mechanical_arm': {
        'block': {'3': 'mechanical_arm', '7': 'brass_block', 'particle': 'casing'},
        'item': {'5': 'mechanical_arm', '7': 'brass_block', '8': 'casing', 'particle': 'casing'},
        'base': {'3': 'brass_block', '5': 'mechanical_arm'}, 'lower_body': {'5': 'mechanical_arm'},
        'upper_body': {'5': 'mechanical_arm'}, 'claw_base': {'5': 'mechanical_arm'},
        'claw_base_goggles': {'5': 'mechanical_arm'}, 'lower_claw_grip': {'5': 'mechanical_arm'},
        'upper_claw_grip': {'5': 'mechanical_arm'}},
}
# Create's recipe for each machine (its data/create/recipes/crafting/kinetics), with C where its casing goes: a tier's
# takes the tier's casing instead (Taran). Pattern, the other keys, and how many it makes
MACHINE_RECIPES = {
    'mechanical_press': (['S', 'C', 'I'], {'S': {'item': 'create:shaft'},
                                           'I': {'tag': 'forge:storage_blocks/iron'}}, 1),
    'mechanical_mixer': (['S', 'C', 'I'], {'S': {'item': 'create:cogwheel'}, 'I': {'item': 'create:whisk'}}, 1),
    'millstone': (['G', 'C', 'I'], {'G': {'item': 'create:cogwheel'}, 'I': {'tag': 'forge:stone'}}, 1),
    'encased_fan': (['S', 'C', 'P'], {'S': {'item': 'create:shaft'}, 'P': {'item': 'create:propeller'}}, 1),
    'mechanical_saw': ([' A ', 'AIA', ' C '], {'A': {'tag': 'forge:plates/iron'}, 'I': {'tag': 'forge:ingots/iron'}}, 1),
    'mechanical_drill': ([' A ', 'AIA', ' C '], {'A': {'item': 'create:andesite_alloy'},
                                                 'I': {'tag': 'forge:ingots/iron'}}, 1),
    'deployer': (['B', 'C', 'I'], {'B': {'item': 'create:electron_tube'}, 'I': {'item': 'create:brass_hand'}}, 1),
    'mechanical_arm': (['LLA', 'L  ', 'IC '], {'L': {'tag': 'forge:plates/brass'}, 'A': {'item': 'create:andesite_alloy'},
                                               'I': {'item': 'create:precision_mechanism'}}, 1),
}


def machine_states(kind):
    """Create's blockstates for a machine, as the model and its x and y turn for each of Create's variants."""
    facing = {'up': (0, 0), 'down': (180, 0), 'north': (90, 0), 'east': (90, 90), 'south': (90, 180),
              'west': (90, 270)}
    horizontal = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    if kind == 'mechanical_press':
        return {f'facing={f}': ('block', 0, y) for f, y in horizontal.items()}
    if kind in ('encased_fan', 'mechanical_drill'):
        return {f'facing={f}': ('block', x, y) for f, (x, y) in facing.items()}
    if kind == 'mechanical_arm':
        return {'ceiling=false': ('block', 0, 0), 'ceiling=true': ('block', 180, 0)}
    if kind == 'mechanical_saw':
        # SawGenerator: the upright model turned along its axis, the lying one to face
        states = {}
        for along in ('false', 'true'):
            for f in facing:
                for flipped in ('false', 'true'):
                    if f in ('up', 'down'):
                        x, y = (180 if f == 'down' else 0), ((270 if along == 'true' else 0)
                                                             + (180 if flipped == 'true' else 0)) % 360
                        states[f'axis_along_first={along},facing={f},flipped={flipped}'] = ('vertical', x, y)
                    else:
                        y = {'south': 0, 'west': 90, 'north': 180, 'east': 270}[f]
                        states[f'axis_along_first={along},facing={f},flipped={flipped}'] = ('horizontal', 0, y)
        return states
    if kind == 'deployer':
        # DirectionalAxisBlockStateGen: upright when its shaft runs across the face it points out of
        states = {}
        for along in ('false', 'true'):
            for f in facing:
                if f in ('up', 'down'):
                    states[f'axis_along_first={along},facing={f}'] = (
                        'horizontal', 270 if f == 'down' else 90, 0 if along == 'true' else 90)
                else:
                    upright = (along == 'true') == (f in ('east', 'west'))
                    states[f'axis_along_first={along},facing={f}'] = (
                        'vertical' if upright else 'horizontal', 0, {'south': 0, 'west': 90, 'north': 180, 'east': 270}[f])
        return states
    return {'': ('block', 0, 0)}


# corners knocked off the top as a stone anvil wears: (x0, x1, z0, z1), each cut down to y 11
STONE_ANVIL_CHIPS = [[], [(13, 16, 2, 6)], [(13, 16, 2, 6), (0, 4, 10, 14)]]


def stone_anvil_model(wear):
    """A stone anvil with its top along x (facing east): cobblestone foot, stone waist and a smooth stone top, the
    recipe's three rows from the bottom up. StoneAnvilBlock's shapes match. Worn anvils have chips out of the top,
    built as a grid of columns so the chipped corners sit lower and show broken stone."""
    chips = STONE_ANVIL_CHIPS[wear]
    xs = sorted({0, 16} | {c[0] for c in chips} | {c[1] for c in chips})
    zs = sorted({2, 14} | {c[2] for c in chips} | {c[3] for c in chips})

    def height(x0, x1, z0, z1):
        return 11 if any(c[0] <= x0 and x1 <= c[1] and c[2] <= z0 and z1 <= c[3] for c in chips) else 14

    cells = {(i, j): height(xs[i], xs[i + 1], zs[j], zs[j + 1])
             for i in range(len(xs) - 1) for j in range(len(zs) - 1)}
    elements = [
        box([2, 0, 2], [14, 3, 14], dict(all_faces('#foot', skip=('down',)), down=face('#foot', 'down'))),
        box([4, 3, 5], [12, 9, 11], all_faces('#waist', skip=('up', 'down'))),
    ]
    for (i, j), top in cells.items():
        faces = {'up': face('#top' if top == 14 else '#broken'), 'down': face('#side')}
        for d, (di, dj) in {'west': (-1, 0), 'east': (1, 0), 'north': (0, -1), 'south': (0, 1)}.items():
            neighbour = cells.get((i + di, j + dj))
            if neighbour is None:
                faces[d] = face('#side')
            elif neighbour < top:
                faces[d] = face('#broken')  # the wall of a chip
        elements.append(box([xs[i], 9, zs[j]], [xs[i + 1], top, zs[j + 1]], faces))
    name = STONE_ANVILS[wear]
    return {
        'parent': 'block/block',
        'textures': {'particle': f'mcremix:block/{name}_side', 'foot': 'minecraft:block/cobblestone',
                     'waist': 'minecraft:block/stone', 'top': f'mcremix:block/{name}_top',
                     'side': f'mcremix:block/{name}_side', 'broken': 'mcremix:block/stone_anvil_broken'},
        'elements': elements,
    }


def chain_drive_states(model, powered=False):
    """Create's ChainDriveGenerator (MIT) in Python: which of the chain drive models each state of a chain drive
    shows, turned how. A lone drive shows 'single'; one in a row shows an end or a middle, lying or upright."""
    states = {}
    for axis in 'xyz':
        for along_first in (False, True):
            for part in ('start', 'middle', 'end', 'none'):
                if part == 'none':
                    suffix = 'single'
                    x = 90 if axis == 'y' else 0
                    y = 90 if axis == 'x' else 0
                else:
                    suffix = ('middle' if part == 'middle' else 'end') + '_' + (
                        'vertical' if axis == 'y' else 'horizontal')
                    if axis == 'x':
                        x = (90 if along_first else 0) + (180 if part == 'start' else 0)
                    elif axis == 'z':
                        x = 0 if along_first else (270 if part == 'start' else 90)
                    else:
                        x = 0
                    if axis == 'z':
                        y = 270 if along_first and part == 'end' else 90
                    elif axis == 'y':
                        flip = (part == 'end' and not along_first) or (part == 'start' and along_first)
                        y = (90 if along_first else 0) + (180 if flip else 0)
                    else:
                        y = 0
                rot = {k: v % 360 for k, v in (('x', x), ('y', y)) if v % 360}
                key = f'axis={axis},axis_along_first={str(along_first).lower()},part={part}'
                for on in (('false', 'true') if powered else (None,)):
                    full = key + (f',powered={on}' if on else '')
                    states[full] = dict({'model': f'{model}/{suffix}' + ('_powered' if on == 'true' else '')},
                                        **rot)
    return states


# Create's BracketGenerator: for each way a bracket faces, which of its models (lying on the ground or against a wall)
# it shows, turned how (x, y). The same for shaft, cog and pipe brackets
BRACKET_TURNS = {('false', 'down'): (180, 0, 'ground'), ('false', 'east'): (0, 0, 'wall'),
                 ('false', 'north'): (90, 270, 'wall'), ('false', 'south'): (90, 90, 'wall'),
                 ('false', 'up'): (0, 0, 'ground'), ('false', 'west'): (0, 180, 'wall'),
                 ('true', 'down'): (180, 90, 'ground'), ('true', 'east'): (90, 0, 'wall'),
                 ('true', 'north'): (0, 270, 'wall'), ('true', 'south'): (0, 90, 'wall'),
                 ('true', 'up'): (0, 90, 'ground'), ('true', 'west'): (90, 180, 'wall')}


def bracket_states(tex):
    return {f'axis_along_first={first},facing={facing},type={kind}': dict(
                {'model': f'mcremix:block/bracket/{tex}/{kind}_{lie}'}, **({'x': x} if x else {}), **({'y': y} if y else {}))
            for (first, facing), (x, y, lie) in BRACKET_TURNS.items() for kind in ('cog', 'pipe', 'shaft')}


def clockwork_box(frm, to, side=(8, 0), end=None, turn=None, axis='y'):
    """A box of polished brass (brass_clockwork.png). Each face takes a patch of the texture its own size, from the
    corner `side`, or `end` for its top and bottom, so thin parts show the brass at their own scale. `turn` turns it about
    the block's middle round `axis`: round the arbor for a wheel, round the shaft for a bracket's bridge."""
    (x0, y0, z0), (x1, y1, z1) = frm, to
    dx, dy, dz = x1 - x0, y1 - y0, z1 - z0
    end = end or side

    def uv(corner, w, h):
        return [corner[0], corner[1], min(16, corner[0] + w), min(16, corner[1] + h)]
    faces = {'north': uv(side, dx, dy), 'south': uv(side, dx, dy), 'east': uv(side, dz, dy), 'west': uv(side, dz, dy),
             'up': uv(end, dx, dz), 'down': uv(end, dx, dz)}
    element = {'from': list(frm), 'to': list(to), 'faces': {d: {'uv': f, 'texture': '#0'} for d, f in faces.items()}}
    if turn:
        element['rotation'] = {'angle': turn, 'axis': axis, 'origin': [8, 8, 8]}
    return element


def clockwork_arbor():
    """The slender rod brass parts turn on, end to end of the block: three pixels across where Create's shafts are four
    (Taran, 0.26.0: a little thicker than the two it was), with no collars on it."""
    side = {'uv': [0, 0, 3, 16], 'texture': '#0'}
    return {'from': [6.5, 0, 6.5], 'to': [9.5, 16, 9.5],
            'faces': dict({d: dict(side) for d in ('north', 'east', 'south', 'west')},
                          up={'uv': [0, 0, 3, 3], 'texture': '#0'}, down={'uv': [0, 0, 3, 3], 'texture': '#0'})}


def clockwork_wheel(rim, teeth_out, ring_sides, tooth_turns, spoke_width, hoop=None):
    """A thin brass wheel in the XZ plane, two pixels thick: a ring of `ring_sides` flat bars whose outside is `rim`
    pixels from the middle, a tooth two pixels wide reaching out to `teeth_out` at every angle in `tooth_turns` (the
    same places as the teeth on Create's cog of that size, so they mesh), four spokes and a hub on the arbor. A
    `hoop`, for decoration (Taran, 0.23.0), is a slender eight-sided ring across the spokes that far out."""
    inner = rim - 1.5
    half_side = rim * math.tan(math.pi / ring_sides)
    lo, hi = 8 - half_side, 8 + half_side
    bars = [((lo, 7, 8 + inner), (hi, 9, 8 + rim)), ((lo, 7, 8 - rim), (hi, 9, 8 - inner)),
            ((8 - rim, 7, lo), (8 - inner, 9, hi)), ((8 + inner, 7, lo), (8 + rim, 9, hi))]
    ring_turns = [0, 45] if ring_sides == 8 else [0, 22.5, 45, -22.5]
    elements = [clockwork_box([round(c, 3) for c in a], [round(c, 3) for c in b], turn=t)
                for t in ring_turns for a, b in bars]
    out = 8 + teeth_out
    teeth = [((8 + rim, 7, 7), (out, 9, 9)), ((16 - out, 7, 7), (8 - rim, 9, 9)),
             ((7, 7, 8 + rim), (9, 9, out)), ((7, 7, 16 - out), (9, 9, 8 - rim))]
    elements += [clockwork_box(a, b, turn=t) for t in tooth_turns for a, b in teeth]
    w0, w1 = 8 - spoke_width / 2, 8 + spoke_width / 2
    elements += [clockwork_box((8 - inner, 7.5, w0), (8 + inner, 8.5, w1), turn=22.5),
                 clockwork_box((w0, 7.5, 8 - inner), (w1, 8.5, 8 + inner), turn=22.5)]
    if hoop:
        half = hoop * math.tan(math.pi / 8)
        lo, hi = round(8 - half, 3), round(8 + half, 3)
        bars = [((lo, 7.5, 8 + hoop - 1), (hi, 8.5, 8 + hoop)), ((lo, 7.5, 8 - hoop), (hi, 8.5, 9 - hoop)),
                ((8 - hoop, 7.5, lo), (9 - hoop, 8.5, hi)), ((7 + hoop, 7.5, lo), (8 + hoop, 8.5, hi))]
        elements += [clockwork_box(a, b, turn=t) for t in (0, 45) for a, b in bars]
    elements.append(clockwork_box((6, 6, 6), (10, 10, 10), side=(3, 4), end=(3, 0)))
    return elements


def clockwork_models():
    """Brass shafts and cogs look fine and delicate, like a clock's works (Taran): a slender plain arbor, and thin
    spoked wheels with slender teeth, the large one with a hoop across its spokes."""
    textures = {'0': 'mcremix:block/brass_clockwork', 'particle': 'mcremix:block/brass_clockwork'}
    small = clockwork_wheel(6, 9, 8, [0, 45], 1)
    large = clockwork_wheel(11.5, 15, 16, [0, 22.5, 45, -22.5], 1.5, hoop=6)
    half_side = {'uv': [0, 8, 3, 16], 'rotation': 90, 'texture': '#0'}
    return {
        'brass_shaft': {'parent': 'block/block', 'textures': textures, 'elements': [clockwork_arbor()]},
        # the half an arbor that turns in each open side of a gearbox, pointing south as Create's half shaft does
        'brass_shaft_half': {'parent': 'block/block', 'textures': textures, 'elements': [{
            'from': [6.5, 6.5, 8], 'to': [9.5, 9.5, 16],
            'faces': dict({d: dict(half_side) for d in ('east', 'west', 'up', 'down')},
                          north={'uv': [0, 0, 3, 3], 'texture': '#0'}, south={'uv': [0, 0, 3, 3], 'texture': '#0'})}]},
        'brass_cogwheel': {'parent': 'create:block/large_wheels', 'textures': textures,
                           'elements': [clockwork_arbor()] + small},
        'brass_large_cogwheel': {'parent': 'create:block/large_wheels', 'textures': textures,
                                 'elements': [clockwork_arbor()] + large},
        'brass_large_cogwheel_shaftless': {'parent': 'create:block/large_wheels', 'textures': textures,
                                           'elements': large},
        'brass_cogwheel_shaftless': {'parent': 'create:block/large_wheels', 'textures': textures, 'elements': small},
    }


def clock_bridge_model(kind, lie):
    """A brass bracket as a clock bridge (Taran, 0.26.0): a thin pierced brass plate standing over the part, as the
    bridges over a watch's wheels do, a round bearing ring round the shaft at its heart, its two legs screwed down to
    the block by a foot strip. Laid out lying on the ground with the shaft running north to south through the middle,
    as Create's bracket models are; against a wall it is the same turned onto its side. A cog's has two bridges either
    side of the wheel, and a pipe's a wider ring on a single post."""
    boxes = []  # (from, to, turn about the shaft)

    def ring(z0, z1, inner, outer):
        half = outer * math.tan(math.pi / 8)
        lo, hi = 8 - half, 8 + half
        bars = [((lo, 8 + inner, z0), (hi, 8 + outer, z1)), ((lo, 8 - outer, z0), (hi, 8 - inner, z1)),
                ((8 - outer, lo, z0), (8 - inner, hi, z1)), ((8 + inner, lo, z0), (8 + outer, hi, z1))]
        boxes.extend((a, b, t) for t in (0, 45) for a, b in bars)

    def bridge(z0, z1):
        ring(z0, z1, 2.1, 3.6)  # round any shaft, Create's four pixels as well as brass's three
        boxes.append(((3.2, 1, z0), (4.7, 8, z1), 0))  # the legs
        boxes.append(((11.3, 1, z0), (12.8, 8, z1), 0))
        boxes.append(((4.7, 2.5, z0 + 0.25), (11.3, 3.5, z1 - 0.25), 0))  # the arch between them, under the ring
        foot(z0 - 0.5, z1 + 0.5, 1.5, 14.5)

    def foot(z0, z1, x0, x1):
        boxes.append(((x0, 0, z0), (x1, 1, z1), 0))
        zc = (z0 + z1) / 2
        for x in (x0 + 0.5, x1 - 1.5):  # a screw head at each end
            boxes.append(((x, 1, zc - 0.5), (x + 1, 1.5, zc + 0.5), 0))

    if kind == 'shaft':
        bridge(7, 9)
    elif kind == 'cog':
        bridge(3, 5)
        bridge(11, 13)
    else:
        ring(7, 9, 4.1, 5.6)
        boxes.append(((7, 1, 7), (9, 2.6, 9), 0))
        foot(6.5, 9.5, 2, 14)

    def turned(frm, to):
        # onto a wall: (x, y, z) to (y, 16 - x, z), the foot against the west face
        (x0, y0, z0), (x1, y1, z1) = frm, to
        return (y0, 16 - x1, z0), (y1, 16 - x0, z1)
    elements = []
    for frm, to, turn in boxes:
        if lie == 'wall':
            frm, to = turned(frm, to)
        elements.append(clockwork_box([round(c, 3) for c in frm], [round(c, 3) for c in to], turn=turn, axis='z'))
    return {'parent': 'block/block',
            'textures': {'0': 'mcremix:block/brass_clockwork', 'particle': 'mcremix:block/brass_clockwork'},
            'elements': elements}


def gen_models_and_states():
    for capped in ('', '_capped'):
        write_json(asset('models', 'block', f'kiln{capped}.json'), kiln_model(chimney=not capped))
        write_json(asset('models', 'block', f'kiln_lit{capped}.json'), {
            'parent': f'mcremix:block/kiln{capped}',
            'textures': {'inside': 'mcremix:block/kiln_inside_lit', 'embers': 'mcremix:block/kiln_embers'}})
        # heated from underneath with its own fire out: the inside glows over cold ash
        write_json(asset('models', 'block', f'kiln_hot{capped}.json'), {
            'parent': f'mcremix:block/kiln{capped}', 'textures': {'inside': 'mcremix:block/kiln_inside_lit'}})
    variants_kiln = {}
    for facing, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
        for heat in range(5):
            for chimney in ('false', 'true'):
                for lit in ('false', 'true'):
                    look = '_lit' if lit == 'true' else '_hot' if heat > 0 else ''
                    v = {'model': f'mcremix:block/kiln{look}' + ('' if chimney == 'true' else '_capped')}
                    if y:
                        v['y'] = y
                    variants_kiln[f'chimney={chimney},facing={facing},heat={heat},lit={lit}'] = v
    write_json(asset('blockstates', 'kiln.json'), {'variants': variants_kiln})
    write_json(asset('models', 'item', 'kiln.json'), {'parent': 'mcremix:block/kiln'})
    # the Steel Furnace: the Furnace's shape, cased in steel
    steel = 'mcremix:block/steel_casing'
    for capped in ('', '_capped'):
        write_json(asset('models', 'block', f'steel_furnace{capped}.json'), {
            'parent': f'mcremix:block/kiln{capped}', 'textures': {'particle': steel, 'side': steel, 'top': steel}})
        write_json(asset('models', 'block', f'steel_furnace_lit{capped}.json'), {
            'parent': f'mcremix:block/steel_furnace{capped}',
            'textures': {'inside': 'mcremix:block/kiln_inside_lit', 'embers': 'mcremix:block/kiln_embers'}})
        write_json(asset('models', 'block', f'steel_furnace_hot{capped}.json'), {
            'parent': f'mcremix:block/steel_furnace{capped}', 'textures': {'inside': 'mcremix:block/kiln_inside_lit'}})
    write_json(asset('blockstates', 'steel_furnace.json'), {'variants': {
        k: dict(v, model=v['model'].replace('block/kiln', 'block/steel_furnace')) for k, v in variants_kiln.items()}})
    write_json(asset('models', 'item', 'steel_furnace.json'), {'parent': 'mcremix:block/steel_furnace'})

    for tex in TEXTURE_STAGES:
        t = lambda n: f'mcremix:block/{tex}_{n}'
        # what turns in a holder: its own material's shafts, plain titanium in a tinted titanium one
        a = lambda n: f'mcremix:block/{"titanium" if holds_only(tex) else tex}_{n}'
        if not holds_only(tex):
            write_json(asset('models', 'block', f'{tex}_shaft.json'), {
                'parent': 'create:block/shaft',
                'textures': {'0': t('axis'), '1': t('axis_top'), 'particle': t('axis')}})
            # the half shafts a gearbox turns, one per open side
            write_json(asset('models', 'block', f'{tex}_shaft_half.json'), {
                'parent': 'create:block/shaft_half',
                'textures': {'0': t('axis'), '1': t('axis_top'), 'particle': t('axis')}})
        write_json(asset('models', 'block', f'{tex}_gearbox.json'), {
            'parent': 'create:block/gearbox/block',
            'textures': {'0': t('casing'), '1': t('gearbox'), 'particle': t('casing')}})
        write_json(asset('models', 'block', f'{tex}_gearbox_item.json'), {
            'parent': 'create:block/gearbox/item',
            'textures': {'0': t('casing'), '1': t('gearbox'), 'particle': t('casing'), '1_0': a('axis'),
                         '1_1': a('axis_top')}})
        # clutches and gearshifts share Create's gearshift model: indicator sides, framed ends with the plate inset.
        # Brass has neither (NO_BRASS)
        if tex != 'brass':
            for part in ('clutch', 'gearshift'):
                for powered, state in (('', 'off'), ('_powered', 'on')):
                    write_json(asset('models', 'block', f'{tex}_{part}{powered}.json'), {
                        'parent': 'create:block/gearshift/block',
                        'textures': {'0': t(f'{part}_{state}'), '1': t('gearbox'), '2': t('frame'),
                                     'particle': t(f'{part}_{state}')}})
            write_json(asset('models', 'block', f'{tex}_clutch_item.json'), {
                'parent': 'create:block/clutch/item',
                'textures': {'0': t('clutch_off'), '1': t('gearbox'), '4': t('frame'), 'particle': a('axis'),
                             '1_0': a('axis'), '1_1': a('axis_top')}})
            write_json(asset('models', 'block', f'{tex}_gearshift_item.json'), {
                'parent': 'create:block/gearshift/item',
                'textures': {'0': t('gearshift_off'), '1': t('gearbox'), 'particle': a('axis'), '1_0': a('axis'),
                             '1_1': a('axis_top')}})
        # Create's bracket models, lying on the ground or against a wall, for shafts, cogs and pipes, in this material;
        # brass's is a clock bridge of its own
        for kind in ('shaft', 'cog', 'pipe'):
            for lie in ('ground', 'wall'):
                write_json(asset('models', 'block', 'bracket', tex, f'{kind}_{lie}.json'),
                           clock_bridge_model(kind, lie) if tex == 'brass' else {
                    'parent': f'create:block/bracket/{kind}/{lie}',
                    'textures': {'bracket': t('bracket'), 'plate': t('bracket_plate'), 'particle': t('bracket_plate')}})
        if tex in WOODS:
            continue
        if not holds_only(tex):
            write_json(asset('models', 'block', f'{tex}_cogwheel.json'), {
                'parent': 'create:block/cogwheel',
                'textures': {'0': t('cogwheel_axis'), '3': t('axis_top'), '1_2': t('cogwheel'),
                             'particle': t('cogwheel')}})
            write_json(asset('models', 'block', f'{tex}_large_cogwheel.json'), {
                'parent': 'create:block/large_cogwheel',
                'textures': {'0': t('cogwheel_axis'), '3': t('axis_top'), '4': t('large_cogwheel'),
                             'particle': t('large_cogwheel')}})
            write_json(asset('models', 'block', f'{tex}_large_cogwheel_shaftless.json'), {
                'parent': 'create:block/large_cogwheel_shaftless',
                'textures': {'4': t('large_cogwheel'), 'particle': t('large_cogwheel')}})
            # an encased cog turns without its shaft, which comes out of the casing in halves where a shaft meets it
            write_json(asset('models', 'block', f'{tex}_cogwheel_shaftless.json'), {
                'parent': 'create:block/cogwheel_shaftless',
                'textures': {'1_2': t('cogwheel'), 'particle': t('cogwheel')}})
        if tex == 'brass':
            for name, model in clockwork_models().items():
                write_json(asset('models', 'block', name + '.json'), model)
        # chain drives and chain gearshifts take Create's chain drive models: a lone drive shows the gearbox's
        # bushing on its ends, a drive in a row shows the chain running on to its neighbours
        for part, sides in (('chain_drive', {'': t('chain_drive')}),
                            ('chain_gearshift', {'': t('chain_gearshift_off'), '_powered': t('chain_gearshift_on')})):
            if tex == 'brass' and part in NO_BRASS:
                continue
            for powered, side in sides.items():
                for suffix, own in [('single', {'1': t('gearbox')})] + [
                        (f'{section}_{lie}', {'2': t('chain_drive_end')} if section == 'end'
                         else {'1': t('chain_drive_middle')})
                        for section in ('end', 'middle') for lie in ('horizontal', 'vertical')]:
                    write_json(asset('models', 'block', f'{tex}_{part}', f'{suffix}{powered}.json'), {
                        'parent': f'create:block/encased_chain_drive/{suffix}',
                        'textures': dict({'side': side, 'particle': t('casing')}, **own)})
            write_json(asset('models', 'block', f'{tex}_{part}', 'item.json'), {
                'parent': 'create:block/encased_chain_drive/item',
                'textures': {'side': sides[''], '1': t('gearbox'), 'particle': t('casing'), '1_0': a('axis'),
                             '1_1': a('axis_top')}})

    for name, material, part, stage, waxed, tex in VARIANTS:
        if part == 'bracket':
            write_json(asset('blockstates', name + '.json'), {'variants': bracket_states(tex)})
            write_json(asset('models', 'item', name + '.json'), {'parent': 'mcremix:block/bracket/brass/shaft_ground'}
                       if tex == 'brass' else {
                'parent': 'create:block/bracket/item',
                'textures': {'bracket': f'mcremix:block/{tex}_bracket', 'plate': f'mcremix:block/{tex}_bracket_plate'}})
            continue
        model = f'mcremix:block/{tex}_{part}'
        # gearboxes keep their casing texture upright whichever way they lie, as Create's does
        lock = {'uvlock': True} if part == 'gearbox' else {}
        turns = {'x': {'x': 90, 'y': 90}, 'y': {}, 'z': {'x': 90, 'y': 180}}
        if part in ('clutch', 'gearshift'):
            states = {f'axis={axis},powered={powered}': dict(
                          {'model': model + ('_powered' if powered == 'true' else '')}, **rot)
                      for axis, rot in turns.items() for powered in ('false', 'true')}
        elif part in CHAINS:
            states = chain_drive_states(model, powered=part == 'chain_gearshift')
        else:
            states = {f'axis={axis}': dict({'model': model}, **rot, **lock) for axis, rot in turns.items()}
        write_json(asset('blockstates', name + '.json'), {'variants': states})
        write_json(asset('models', 'item', name + '.json'),
                   {'parent': model + '/item' if part in CHAINS else model + '_item' if part in CASED else model})

    parts = bellows_parts()
    for name in ['bellows', 'bellows/lid', 'bellows/bag', 'bellows/crank']:
        write_json(asset('models', 'block', name + '.json'), parts[name])
    write_json(asset('models', 'item', 'bellows.json'), parts['item'])
    write_json(asset('blockstates', 'bellows.json'), {'variants': {
        f'facing={facing}': dict({'model': 'mcremix:block/bellows'}, **({'y': y} if y else {}))
        for facing, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})

    for wear, name in enumerate(STONE_ANVILS):
        write_json(asset('models', 'block', name + '.json'), stone_anvil_model(wear))
        write_json(asset('models', 'item', name + '.json'), {'parent': f'mcremix:block/{name}'})
        # the model's top runs along x; StoneAnvilBlock faces across the placer's view, as vanilla anvils do
        write_json(asset('blockstates', name + '.json'), {'variants': {
            f'facing={facing}': dict({'model': f'mcremix:block/{name}'}, **({'y': y} if y else {}))
            for facing, y in [('east', 0), ('south', 90), ('west', 180), ('north', 270)]}})

    # fitted bits are Create's drill head with their own texture (texture "2", the vanilla anvil on Create's own),
    # which TieredDrillRenderer and TieredDrillVisual draw in place of Create's head
    for material, tex in BITS.items():
        write_json(asset('models', 'block', bits_name(material), 'head.json'), {
            'parent': 'create:block/mechanical_drill/head', 'textures': {'2': tex, 'particle': tex}})
    # fitted blades are Create's blade with their own texture, which TieredSawRenderer draws in place of Create's.
    # Like Create's, a still blade is the turning blade's model with a still texture
    for material in SAWS:
        turning = f'mcremix:block/{blade_name(material)}'
        still = turning + '_static'
        for lie in ('horizontal', 'vertical'):
            for spin, parent, tex in (('active', 'active', turning), ('reversed', 'reversed', turning),
                                      ('inactive', 'active', still)):
                write_json(asset('models', 'block', blade_name(material), f'blade_{lie}_{spin}.json'), {
                    'parent': f'create:block/mechanical_saw/blade_{lie}_{parent}',
                    'textures': {'stonecutter_saw': tex}})
    for material in FITTINGS:
        for name in (bits_name(material), blade_name(material)):
            write_json(asset('models', 'item', name + '.json'),
                       {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})

    for metal in ('iron', 'steel', 'aluminium', 'titanium'):
        write_json(asset('models', 'block', f'{metal}_casing.json'),
                   {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'mcremix:block/{metal}_casing'}})
        write_json(asset('models', 'item', f'{metal}_casing.json'), {'parent': f'mcremix:block/{metal}_casing'})
        write_json(asset('blockstates', f'{metal}_casing.json'),
                   {'variants': {'': {'model': f'mcremix:block/{metal}_casing'}}})

    for item in ['coal_coke', 'iron_bloom', 'slag', 'cementation_pack', 'blister_steel', 'steel_ingot', 'steel_nugget',
                 'steel_sheet', 'aluminium_ingot', 'aluminium_nugget', 'aluminium_sheet', 'bakelite', 'bauxite',
                 'aluminium_hydroxide', 'alumina']:
        write_json(asset('models', 'item', item + '.json'),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{item}'}})


# ------------------------------------------------------------------------------------------------ lang

def title_case(name):
    return ' '.join(w.capitalize() for w in name.split('_'))


def tinted_title(name):
    """A name in title case, a titanium tint hyphenated: Gold-Tinted Titanium."""
    return title_case(name).replace(' Tinted ', '-Tinted ')


# Create's machine, clutch, gearshift, chain drive and mover scenes, shown for ours too (ModPonderPlugin): registered by
# us, they read their text from our namespace, so it is copied here from Create's en_us.json (Create is MIT licensed)
CREATE_PONDER_TEXT = {
    'mechanical_press': ['Processing Items with the Mechanical Press',
                         'The Mechanical Press can process items provided beneath it',
                         'The Input items can be dropped or placed on a Depot under the Press',
                         'When items are provided on a belt...',
                         'The Press will hold and process them automatically'],
    'mechanical_press_compacting': ['Compacting items with the Mechanical Press',
                                    'Pressing items held in a Basin will cause them to be Compacted',
                                    'Compacting includes any filled 2x2 or 3x3 Crafting Recipe, plus a couple extra '
                                    'ones',
                                    'Some of those recipes may require the heat of a Blaze Burner',
                                    'The filter slot can be used in case two recipes are conflicting.'],
    'mechanical_mixer': ['Processing Items with the Mechanical Mixer',
                         'With a Mixer and Basin, some Crafting Recipes can be automated',
                         'Available recipes include any Shapeless Crafting Recipe, plus a couple extra ones',
                         'Some of those recipes may require the heat of a Blaze Burner',
                         'The filter slot can be used in case two recipes are conflicting.'],
    'millstone': ['Processing Items in the Millstone',
                  'Millstones process items by grinding them',
                  'They can be powered from the side using cogwheels',
                  'Throw or Insert items at the top',
                  'After some time, the result can be obtained via Right-click',
                  'The outputs can also be extracted by automation'],
    'fan_direction': ['Air flow of Encased Fans',
                      'Encased Fans use Rotational Force to create an Air Current',
                      'Strength and Direction of Flow depends on the Rotational Input'],
    'fan_processing': ['Processing Items using Encased Fans',
                       'When passing through lava, the Air Flow becomes Heated',
                       'Items caught in the area will be smelted',
                       'Food items thrown here would be incinerated',
                       'Instead, a setup for Smoking using Fire should be used for them',
                       'Air Flows passing through water create a Washing Setup',
                       'Some interesting new processing can be done with it',
                       'The Speed of the Fan does NOT affect the processing speed, only its range',
                       'Fan Processing can also be applied to Items on Depots and Belts'],
    'mechanical_saw_processing': ['Processing Items on the Mechanical Saw',
                                  'Upward facing Mechanical Saws can process a variety of items',
                                  'The processed item always moves against the rotational input to the saw',
                                  'Saws can work in-line with Mechanical Belts',
                                  'When an ingredient has multiple possible outcomes, the filter slot can specify it',
                                  'Without filter, the Saw would cycle through all outcomes instead'],
    'mechanical_saw_breaker': ['Cutting Trees with the Mechanical Saw',
                               'When given Rotational Force, a Mechanical Saw will cut trees directly in front of it',
                               'In order to cut the tree fully, the Saw has to break the last block connecting it to '
                               'the ground'],
    'mechanical_saw_contraption': ['Using Mechanical Saws on Contraptions',
                                   'Whenever Saws are moved as part of an animated Contraption...',
                                   '...they will cut any trees the contraption runs them into'],
    'mechanical_drill': ['Breaking Blocks with the Mechanical Drill',
                         'When given Rotational Force, a Mechanical Drill will break blocks directly in front of it',
                         'Its mining speed depends on the Rotational Input'],
    'mechanical_drill_contraption': ['Using Mechanical Drills on Contraptions',
                                     'Whenever Drills are moved as part of an animated Contraption...',
                                     '...they will break blocks the contraption runs them into'],
    'deployer': ['Using the Deployer',
                 'Given Rotational Force, a Deployer can imitate player interactions',
                 'It will always interact with the position 2 blocks in front of itself',
                 'Blocks directly in front will not obstruct it',
                 'Deployers can:',
                 'Place Blocks,',
                 'Use Items,',
                 'Activate Blocks,',
                 'Harvest blocks',
                 'and Attack Mobs',
                 'Right-click the front to give it an Item to use',
                 'Items can also be inserted automatically',
                 'Deployers carry a filter slot',
                 'When a filter is set, it activates only while holding a matching item',
                 'Only items matching the filter can now be inserted...',
                 '...and only non-matching items will be extracted'],
    'deployer_modes': ['Modes of the Deployer',
                       'By default, a Deployer imitates a Right-click interaction',
                       'Using a Wrench, it can be set to imitate a Left-click instead'],
    'deployer_processing': ['Processing Items using Deployers',
                            'With a fitting held item, Deployers can process items provided beneath them',
                            'The Input items can be dropped or placed on a Depot under the Deployer',
                            'When items are provided on a belt...',
                            'The Deployer will hold and process them automatically'],
    'deployer_redstone': ['Controlling Deployers with Redstone',
                          'When powered by Redstone, Deployers will not activate',
                          'Before stopping, the Deployer will finish any started cycles',
                          'Thus, a negative pulse can be used to trigger exactly one activation cycle'],
    'deployer_contraption': ['Using Deployers on Contraptions',
                             'Whenever Deployers are moved as part of an animated Contraption...',
                             'They activate at each visited location, using items from inventories anywhere on the '
                             'contraption',
                             'The Filter slot can be used to specify which items to pull'],
    'mechanical_crafter': ['Setting up Mechanical Crafters',
                           'An array of Mechanical Crafters can be used to automate any Crafting Recipe',
                           "Using a Wrench, the Crafters' paths can be arranged",
                           'For a valid setup, all paths have to converge into one exit at any side',
                           'The outputs will be placed into the inventory at the exit',
                           'Mechanical Crafters require Rotational Force to operate',
                           'Right-Click the front to insert Items manually',
                           'Once every slot of a path contains an Item, the crafting process will begin',
                           'For recipes not fully occupying the crafter setup, the start can be forced using a '
                           'Redstone Pulse'],
    'mechanical_crafter_connect': ['Connecting Inventories of Crafters',
                                   'Items can be inserted to Crafters automatically',
                                   'Using the Wrench at their backs, Mechanical Crafter inputs can be combined',
                                   'All connected Crafters can now be accessed by the same input location'],
    'mechanical_crafter_covers': ['Covering slots of Mechanical Crafters',
                                  'Some recipes will require additional Crafters to bridge gaps in the path',
                                  'Using Slot Covers, Crafters can be set to act as an Empty Slot in the arrangement',
                                  'Shared Inputs created with the Wrench at the back can also reach across covered '
                                  'Crafters'],
    'mechanical_arm': ['Setting up Mechanical Arms',
                       'Mechanical Arms have to be assigned their in- and outputs before they are placed',
                       'Right-Click inventories while holding the Arm to assign them as Targets',
                       'Right-Click again to toggle between Input (Blue) and Output (Yellow)',
                       'Left-Click components to remove their Selection',
                       'Once placed, the Mechanical Arm will target the blocks selected previously',
                       'They can have any amount of in- and outputs within their range',
                       'However, not every type of Inventory can be interacted with directly',
                       'Funnels and Depots can help to Bridge that gap'],
    'mechanical_arm_filtering': ['Filtering Outputs of the Mechanical Arm',
                                 'Inputs',
                                 'Outputs',
                                 'Sometimes it is desirable to restrict targets of the Arm by matching a filter',
                                 'Mechanical Arms by themselves do not provide any options for filtering',
                                 'Brass Funnels as Targets do however communicate their own filter to the Arm',
                                 "The Arm is smart enough not to pick up items it couldn't distribute"],
    'mechanical_arm_modes': ['Distribution modes of the Mechanical Arm',
                             'Input',
                             'Outputs',
                             'Whenever an Arm has to choose between multiple valid outputs...',
                             '...it will act according to its setting',
                             'The value panel will allow you to configure it',
                             'Round Robin mode simply cycles through all outputs that are available',
                             'If an output is unable to take more items, it will be skipped',
                             'Forced Round Robin mode will never skip outputs, and instead wait until they are free',
                             'Prefer First prioritizes the outputs selected earliest when configuring this Arm'],
    'mechanical_arm_redstone': ['Controlling Mechanical Arms with Redstone',
                                'When powered by Redstone, Mechanical Arms will not activate',
                                'Before stopping, it will finish any started cycles',
                                'Thus, a negative pulse can be used to trigger exactly one activation cycle'],
    'clutch': ['Controlling rotational force using a Clutch',
               'Clutches will relay rotation in a straight line',
               'When powered by Redstone, it breaks the connection'],
    'gearshift': ['Controlling rotational force using a Gearshift',
                  'Gearshifts will relay rotation in a straight line',
                  'When powered by Redstone, it reverses the transmission'],
    'chain_drive': ['Relaying rotational force with Chain Drives',
                    'Chain Drives relay rotation to each other in a row',
                    'All shafts connected like this will rotate in the same direction',
                    'Any part of the row can be rotated by 90 degrees'],
    'chain_gearshift': ['Controlling rotational speed with Chain Gearshifts',
                        'Unpowered Chain Gearshifts behave exactly like Chain Drives',
                        'When Powered, the speed transmitted to other Chain Drives in the row is doubled',
                        'Whenever the Powered Gearshift is not at the source, its speed will be halved instead',
                        'In both cases, Chain Drives in the row always run at 2x the speed of the Powered Gearshift',
                        'Using analog signals, the ratio can be adjusted more precisely between 1 and 2',
                        '12 RPM'],
    'mechanical_piston': ['Moving Structures using Mechanical Pistons',
                          'Mechanical Pistons can move blocks in front of them',
                          'Speed and direction of movement depend on the Rotational Input',
                          'Sticky Mechanical Pistons can pull the attached blocks back'],
    'piston_pole': ['Piston Extension Poles',
                    'Without attached Poles, a Mechanical Piston cannot move',
                    'The Length of pole added at its back determines the Extension Range'],
    'mechanical_piston_modes': ['Movement Modes of the Mechanical Piston',
                                'Whenever Pistons stop moving, the moved structure reverts to blocks',
                                'It can be configured never to revert to solid blocks, or only at the location it '
                                'started at'],
    'rope_pulley': ['Moving Structures using Rope Pulleys',
                    'Rope Pulleys can move blocks vertically when given Rotational Force',
                    'Direction and Speed of movement depend on the Rotational Input'],
    'rope_pulley_modes': ['Movement Modes of the Rope Pulley',
                          'Whenever Pulleys stop moving, the moved structure reverts to blocks',
                          'It can be configured never to revert to solid blocks, or only at the location it started '
                          'at'],
    'rope_pulley_attachment': ['Moving Pulleys as part of a Contraption',
                               'Whenever Pulleys are themselves being moved by a Contraption...',
                               '...its attached structure will be dragged with it',
                               'Mind that pulleys are only movable while stopped'],
    'rope_pulley_multi_rope': ['Synchronised Pulley Movement',
                               'Whenever a pulley assembles a contraption...',
                               '...other pulleys on the same layer will connect to the structure',
                               'They do not require to be powered, the effect is purely cosmetic'],
    'mechanical_bearing': ['Moving Structures using the Mechanical Bearing',
                           'Mechanical Bearings attach to the block in front of them',
                           'Upon receiving Rotational Force, it will assemble it into a Rotating Contraption'],
    'bearing_modes': ['Movement Modes of the Mechanical Bearing',
                      'When Stopped, the Bearing will place the structure at the nearest grid-aligned Angle',
                      'It can be configured never to revert to solid blocks, or only near the angle it started at'],
    'stabilized_bearings': ['Stabilized Contraptions',
                            'Whenever Mechanical Bearings are themselves part of a moving Structure..',
                            '..they will attempt to keep themselves upright',
                            'Once again, the bearing will attach to the block in front of it',
                            'As a result, the entire sub-Contraption will stay upright'],
    'gantry_shaft': ['Using Gantry Shafts',
                     'Gantry Shafts form the basis of a gantry setup. Attached Carriages will move along them.',
                     'Gantry setups can move attached Blocks.'],
    'gantry_carriage': ['Using Gantry Carriages',
                        'Gantry Carriages can mount to and slide along a Gantry Shaft.',
                        'Gantry setups can move attached Blocks.'],
    'gantry_redstone': ['Gantry Power Propagation',
                        'Redstone-powered gantry shafts stop moving their carriages',
                        "Instead, its rotational force is relayed to the carriages' output shaft"],
    'gantry_direction': ['Gantry Movement Direction',
                         'Gantry Shafts can have opposite orientations',
                         "The movement direction of carriages depend on their shafts' orientation",
                         '...as well as the rotation direction of the shaft',
                         'Same rules apply for the propagated rotation'],
    'gantry_cascaded': ['Cascaded Gantries',
                        'Gantry shafts attach to a carriage without the need of super glue',
                        'Same applies for carriages on moved Gantry Shafts',
                        'Thus, a gantry system can be cascaded to cover multiple axes of movement'],
}
CREATE_PONDER_TEXT['steam_engine'] = [
    'Setting up Steam Engines',
    'Steam Engines can be placed on a Fluid Tank of their own metal',
    'Clicking the engine with a Shaft creates the Kinetic Output',
    'With sufficient Heat, Water and Boiler space...',
    '...they will generate Rotational Force',
    'The minimal setup requires 4 Fluid Tanks',
    'With the help of Blaze Burners, the power output can be increased',
    'Higher power levels require more Water, Size and Heat',
    'The boiler\'s current power level can be inspected with Engineer\'s Goggles',
    'With each added power level, an additional Engine can output at full capacity',
    'Lvl 4',
    '4 Engines',
    'Lvl 8',
    '8 Engines']
CREATE_PONDER_SHARED = {
    'behaviour_modify_value_panel': 'This behaviour can be modified using the value panel',
    'movement_anchors': 'With the help of Super Glue, larger structures can be moved.',
    'rpm16': '16 RPM',
    'rpm16_source': 'Source: 16 RPM',
    'rpm32': '32 RPM',
    'rpm8': '8 RPM',
    'storage_on_contraption': 'Inventories attached to the Contraption will pick up their drops automatically',
}


def create_ponder_lang():
    lang = {}
    for scene_id, (header, *texts) in CREATE_PONDER_TEXT.items():
        lang[f'mcremix.ponder.{scene_id}.header'] = header
        for i, text in enumerate(texts, 1):
            lang[f'mcremix.ponder.{scene_id}.text_{i}'] = text
    for key, text in CREATE_PONDER_SHARED.items():
        lang[f'mcremix.ponder.shared.{key}'] = text
    return lang


def ponder_lang():
    """Pulls scene titles and texts out of the scene classes, in order, into ponder lang keys."""
    lang = {}
    ponder_dir = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'mcremix', 'ponder')
    for fn in sorted(os.listdir(ponder_dir)):
        if not fn.endswith('Scenes.java'):
            continue
        src = open(os.path.join(ponder_dir, fn)).read()
        for method in re.split(r'\n\tpublic static void ', src)[1:]:
            m = re.search(r'scene\.title\("([^"]+)",\s*"([^"]+)"\)', method)
            if not m:
                continue
            scene_id = m.group(1)
            lang[f'mcremix.ponder.{scene_id}.header'] = m.group(2)
            texts = re.findall(r'\.text\("((?:[^"\\]|\\.)*)"\)', method)
            for i, text in enumerate(texts, 1):
                lang[f'mcremix.ponder.{scene_id}.text_{i}'] = text
    return lang


def gen_lang():
    lang = {
        'itemGroup.mcremix.main': 'Minecraft Remix',
        # the kiln is the Furnace in game (vanilla's furnace is renamed in its own namespace, below)
        'block.mcremix.kiln': 'Furnace',
        'block.mcremix.kiln.tooltip.summary': 'Fires, smelts and bakes without a menu. Load the _upper chamber_ with things to fire and the _low firebox_ with fuel and _light_ it, or put it on a _heat source_.',
        'block.mcremix.kiln.tooltip.condition1': 'Heat',
        'block.mcremix.kiln.tooltip.behaviour1': 'Wood burns _Warm_, charcoal and coal _Hot_, coke and blaze rods _Searing_. Heat from below counts too, and needs no lighting.',
        'block.mcremix.kiln.tooltip.condition2': 'Lighting',
        'block.mcremix.kiln.tooltip.behaviour2': 'Use _flint and steel_ or a _fire charge_ on it, or fit a _spark plug_. Once lit it burns its fuel whether or not it has work, and goes out when the fuel is gone. A _shovel_ puts it out and leaves the fuel in.',
        'block.mcremix.kiln.tooltip.condition3': 'Under a boiler or basin',
        'block.mcremix.kiln.tooltip.behaviour3': 'Heats it like a Blaze Burner: a _Searing_ fire boils a _steam boiler_ and is hot enough for _heated_ mixing and compacting; a _Warm_ or _Hot_ one only keeps a boiler simmering.',
        'block.mcremix.kiln.tooltip.condition4': 'Coking coal, charring logs',
        'block.mcremix.kiln.tooltip.behaviour4': 'Coal gives off _coal tar_ as it cokes, and logs _wood spirit_ as they char. Draw it off with a _pump_ and pipe, or a _bucket_; once a bucket\'s worth waits in the Furnace, the rest _burns off_ up the chimney.',
        'block.mcremix.bellows': 'Bellows',
        'block.mcremix.bellows.tooltip.summary': 'Blows air into the block its _nozzle_ faces while its shaft turns. Feeds fires: _one heat level hotter_, up to _Searing_.',
        'block.mcremix.bellows.tooltip.condition1': 'Facing a furnace, or a fire with a furnace on it',
        'block.mcremix.bellows.tooltip.behaviour1': 'The furnace burns _one level hotter_. Fuel it fans burns _twice as fast_. It cannot light a furnace with no fire.',
        'block.mcremix.bellows.tooltip.condition2': 'Facing a lit campfire',
        'block.mcremix.bellows.tooltip.behaviour2': 'Food on it cooks _twice as fast_.',
        'block.mcremix.stone_anvil': 'Stone Anvil',
        'block.mcremix.chipped_stone_anvil': 'Chipped Stone Anvil',
        'block.mcremix.damaged_stone_anvil': 'Damaged Stone Anvil',
        'item.mcremix.stone_drill_bits': 'Stone Drill Bits',
        'item.mcremix.stone_drill_bits.tooltip.summary': 'Cut what a _stone pickaxe_ can, a little _slower_ than iron bits, for _half the stress_.',
        'item.mcremix.iron_drill_bits': 'Iron Drill Bits',
        'item.mcremix.iron_drill_bits.tooltip.summary': 'The bits every _Mechanical Drill_ comes with. Cut what an _iron pickaxe_ can.',
        'item.mcremix.golden_drill_bits': 'Golden Drill Bits',
        'item.mcremix.golden_drill_bits.tooltip.summary': 'Cut _twice as fast_ as iron bits for _half the stress_, but only what a _golden pickaxe_ can: no iron, copper or lapis ore.',
        'item.mcremix.diamond_drill_bits': 'Diamond Drill Bits',
        'item.mcremix.diamond_drill_bits.tooltip.summary': 'The only bits that cut _obsidian_ and _ancient debris_. A little faster than iron bits, for _twice the stress_.',
        'item.mcremix.stone_saw_blade': 'Stone Saw Blade',
        'item.mcremix.stone_saw_blade.tooltip.summary': 'Cuts wood and _stone_ a little _slower_ than an iron blade, for _half the stress_.',
        'item.mcremix.iron_saw_blade': 'Iron Saw Blade',
        'item.mcremix.iron_saw_blade.tooltip.summary': 'The blade every _Mechanical Saw_ comes with. Cuts wood and stone.',
        'item.mcremix.golden_saw_blade': 'Golden Saw Blade',
        'item.mcremix.golden_saw_blade.tooltip.summary': 'Cuts wood _twice as fast_ as an iron blade for _half the stress_, but gold is _too soft for stone_: stone skates across it uncut.',
        'item.mcremix.diamond_saw_blade': 'Diamond Saw Blade',
        'item.mcremix.diamond_saw_blade.tooltip.summary': 'Cuts wood and stone _a third faster_ than an iron blade, for _twice the stress_.',
        'item.mcremix.coal_coke': 'Coal Coke',
        'item.mcremix.coal_coke.tooltip.summary': 'Coal baked Hot in a furnace. Burns _Searing_, hot enough for iron.',
        'item.mcremix.iron_bloom': 'Iron Bloom',
        'item.mcremix.iron_bloom.tooltip.summary': 'Spongy iron full of slag. Strike it on an _anvil_ (a _Stone Anvil_ will do) or put it under a _Mechanical Press_.',
        'item.mcremix.iron_bloom.worked': 'Worked %s / %s',
        'item.mcremix.slag': 'Slag',
        'item.mcremix.slag.tooltip.summary': 'Bloomery waste. Works as _fertiliser_ like bone meal, and washing it in front of an _Encased Fan_ recovers _iron nuggets_.',
        'item.mcremix.blister_steel': 'Blister Steel',
        'item.mcremix.blister_steel.tooltip.summary': 'Iron that has soaked up carbon from _coal coke_, its skin _blistered_ and brittle. Only a _Mechanical Press_ can beat it into a _steel ingot_.',
        'item.mcremix.steel_ingot': 'Steel Ingot',
        'item.mcremix.steel_ingot.tooltip.summary': 'Iron hardened with carbon. Its supports turn up to _512 RPM_, and its shafts and cogs take _any load_.',
        'item.mcremix.steel_nugget': 'Steel Nugget',
        'item.mcremix.steel_sheet': 'Steel Sheet',
        'block.mcremix.iron_casing': 'Iron Casing',
        'block.mcremix.iron_casing.tooltip.summary': 'Made like Create\'s casings: apply an _iron sheet_ to a stripped log. Iron gearboxes, clutches, gearshifts, chain drives, encased shafts and cogs and iron machines are built on it. Never rusts.',
        'block.mcremix.steel_casing': 'Steel Casing',
        'block.mcremix.steel_casing.tooltip.summary': 'Made like Create\'s casings: apply a _steel sheet_ to a stripped log. Steel gearboxes, clutches, gearshifts and chain drives are built on it.',
        'item.mcremix.bakelite': 'Bakelite',
        'item.mcremix.bakelite.tooltip.summary': 'The first plastic: _coal tar_ set hard with _wood spirit_ by a _press_ over a _heated_ basin.',
        'item.mcremix.bauxite': 'Bauxite',
        'item.mcremix.bauxite.tooltip.summary': "Aluminium's ore. _Washing red sand_ with a fan turns some up beside the gold.",
        'item.mcremix.aluminium_hydroxide': 'Aluminium Hydroxide',
        'item.mcremix.aluminium_hydroxide.tooltip.summary': 'Settles out of _bauxite liquor_ as it cools. _Fire_ it at _Searing_ into _alumina_.',
        'item.mcremix.alumina': 'Alumina',
        'item.mcremix.alumina.tooltip.summary': 'The white powder _electrolysis_ turns into _aluminium_.',
        'entity.mcremix.chlorine_cloud': 'Chlorine Cloud',
        'entity.mcremix.ethylene_cloud': 'Ethylene Cloud',
        'entity.mcremix.vinyl_chloride_haze': 'Vinyl Chloride Haze',
        'entity.mcremix.hydrogen_chloride_mist': 'Hydrogen Chloride Mist',
        'death.attack.mcremix.chlorine': '%1$s choked on chlorine',
        'death.attack.mcremix.chlorine.player': '%1$s choked on chlorine while fighting %2$s',
        'item.mcremix.aluminium_ingot': 'Aluminium Ingot',
        'item.mcremix.aluminium_ingot.tooltip.summary': 'Light and soft. Its supports turn up to _512 RPM_ with _no drag_ and nothing of it _weathers_, but its shafts and cogs take only _light loads_, and it _melts_ beside anything _Hot_.',
        'item.mcremix.aluminium_nugget': 'Aluminium Nugget',
        'item.mcremix.aluminium_sheet': 'Aluminium Sheet',
        'block.mcremix.aluminium_casing': 'Aluminium Casing',
        'block.mcremix.aluminium_casing.tooltip.summary': 'Crafted from an _aluminium sheet_, a stripped log and _polyethylene_, since every aluminium support needs polyethylene. Aluminium gearboxes, clutches, gearshifts and chain drives are built on it.',
        'item.mcremix.cementation_pack': 'Cementation Pack',
        'item.mcremix.cementation_pack.tooltip.summary': 'Iron ingots packed in _coal coke_ and sealed in clay, the way steel was made in cementation chests. Fired at _Searing_ heat, the iron soaks up carbon and comes out as four _blister steel_.',
        'mcremix.heat.cold': 'Cold',
        'mcremix.heat.warm': 'Warm',
        'mcremix.heat.hot': 'Hot',
        'mcremix.heat.searing': 'Searing',
        'mcremix.heat.blazing': 'Blazing',
        'mcremix.goggles.heat': 'Heat: ',
        'mcremix.goggles.fuel': 'Fuel: %ss left (%s)',
        'mcremix.goggles.needs_heat': 'needs %s',
        'mcremix.goggles.tray_full': 'output full',
        'mcremix.goggles.byproduct': '%s: %s / %s mB',
        'mcremix.goggles.burning_off': 'Full: what it gives off is burning away',
        'mcremix.goggles.burning_off.hint': 'Draw it off with a pump and pipe, or a bucket',
        'mcremix.goggles.cannot_fire': 'cannot be fired',
        'mcremix.goggles.kiln_bellows': 'Fanned by bellows: one level hotter',
        'mcremix.goggles.boiler.none': 'Too cold to heat the boiler above',
        'mcremix.goggles.boiler.passive': 'Keeps the boiler above simmering, as a campfire would',
        'mcremix.goggles.boiler.kindled': 'Boils the boiler above like a fed Blaze Burner',
        'mcremix.goggles.boiler.superheated': 'Boils the boiler above like a superheated Blaze Burner',
        'mcremix.goggles.boiler.hint': 'A Searing fire would boil it: Coal Coke, or charcoal with a Bellows',
        'mcremix.goggles.basin.none': 'Too cold to heat the basin above',
        'mcremix.goggles.basin.warm': 'Only warms the basin above, as a campfire would: not enough for heated recipes',
        'mcremix.goggles.basin.heated': 'Heats the basin above like a fed Blaze Burner',
        'mcremix.goggles.basin.superheated': 'Heats the basin above like a superheated Blaze Burner',
        'mcremix.goggles.basin.hint': 'A Searing fire would heat it: Coal Coke, or charcoal with a Bellows',
        'mcremix.goggles.bellows.fanning': 'Fanning the furnace: one level hotter',
        'mcremix.goggles.bellows.no_fire': 'No fire to fan',
        'mcremix.goggles.bellows.max_heat': 'The furnace cannot burn any hotter',
        'mcremix.goggles.bellows.campfire': 'The campfire cooks twice as fast',
        'mcremix.goggles.bellows.nothing_above': 'No furnace above this fire to heat',
        'mcremix.goggles.bellows.nothing': 'Nothing to blow into',
        'mcremix.goggles.bellows.still': 'Not turning',
        'mcremix.goggles.material.wooden': 'Wooden',
        'mcremix.goggles.material.andesite': 'Andesite',
        'mcremix.goggles.material.brass': 'Brass',
        'mcremix.goggles.material.copper': 'Copper',
        'mcremix.goggles.material.iron': 'Iron',
        'mcremix.goggles.material.steel': 'Steel',
        'mcremix.goggles.material.aluminium': 'Aluminium',
        'mcremix.goggles.material.titanium': 'Titanium',
        'mcremix.goggles.max_speed': 'Stability: up to %s RPM',
        'mcremix.goggles.no_top_speed': 'Turns as fast as the supports holding it allow',
        'mcremix.goggles.max_load': 'Strength: up to %s su',
        'mcremix.goggles.max_load_unlimited': 'Strength: takes any load',
        'mcremix.goggles.friction_point': 'Support',
        'mcremix.goggles.bracket': 'Support: %s bracket',
        'mcremix.goggles.bracket_weathering': 'Bracket: %s',
        'mcremix.goggles.held.1': 'Held by the support beside it',
        'mcremix.goggles.held': 'Held by a support %s blocks along',
        'mcremix.goggles.held.speed_controller': 'Held by the speed controller below',
        'mcremix.goggles.span.1': 'Reaches 1 block from a support',
        'mcremix.goggles.span': 'Reaches %s blocks from a support',
        'mcremix.goggles.wobbling': 'Overloaded: wobbling loose!',
        'mcremix.goggles.grinding': 'Too fast: grinding itself apart!',
        'mcremix.goggles.grinding.bracket': 'Too fast: its bracket is grinding itself apart!',
        'mcremix.goggles.grinding.casing': 'Too fast: its casing is grinding itself apart!',
        'mcremix.goggles.casing': 'Support: %s casing',
        'mcremix.goggles.inside': 'Holds: %s',
        'mcremix.goggles.temperature': 'Temperature: %s °C (melts at %s °C)',
        'mcremix.friction_point.unheld.1': 'Needs a support right beside it along its axis, or a bracket: put one down there first, or hold one in your offhand',
        'mcremix.friction_point.unheld': 'Needs a support within %s blocks along its axis, or a bracket: put one down there first, or hold one in your offhand',
        'mcremix.bracket.nothing_to_bolt_to': 'Nothing solid beside it to bolt a bracket to',
        'mcremix.friction_point.offhand_unbolted': 'Nothing for your bracket to bolt to: it needs a solid block beside the part, across its axis',
        'mcremix.goggles.patina.unaffected': 'Bright',
        'mcremix.goggles.patina.exposed': 'Tarnished',
        'mcremix.goggles.patina.weathered': 'Weathered',
        'mcremix.goggles.patina.oxidized': 'Oxidized',
        'mcremix.goggles.rust.unaffected': 'Clean',
        'mcremix.goggles.rust.exposed': 'Spotted with rust',
        'mcremix.goggles.rust.weathered': 'Rusty',
        'mcremix.goggles.rust.oxidized': 'Rusted through',
        'mcremix.goggles.friction': 'Friction: %s su per RPM',
        'mcremix.goggles.waxed': 'Waxed',
        'mcremix.goggles.chain_gearshift': 'The rest of its row turns %s\u00d7 as fast',
        'mcremix.drill_bits.stone': 'Stone bits: cut like a stone pickaxe',
        'mcremix.drill_bits.iron': 'Iron bits: cut like an iron pickaxe',
        'mcremix.drill_bits.gold': 'Gold bits: cut like a golden pickaxe',
        'mcremix.drill_bits.diamond': 'Diamond bits: cut like a diamond pickaxe',
        'mcremix.drill_bits.speed.stone': 'at two thirds the speed of iron',
        'mcremix.drill_bits.speed.iron': 'at the usual speed',
        'mcremix.drill_bits.speed.gold': 'twice as fast as iron',
        'mcremix.drill_bits.speed.diamond': 'a third faster than iron',
        'mcremix.drill_bits.material.stone': 'stone',
        'mcremix.drill_bits.material.iron': 'iron',
        'mcremix.drill_bits.material.gold': 'gold',
        'mcremix.drill_bits.material.diamond': 'diamond',
        'mcremix.drill_bits.list.1': '%s bits',
        'mcremix.drill_bits.list.2': '%s or %s bits',
        'mcremix.drill_bits.list.3': '%s, %s or %s bits',
        'mcremix.drill_bits.list.4': '%s, %s, %s or %s bits',
        'mcremix.goggles.drill_bits.speed': 'Cuts %s',
        'mcremix.tooltip.drill.too_hard': 'Too hard for %s bits',
        'mcremix.tooltip.drill.needs': '%s needs %s',
        'mcremix.tooltip.drill.none_cut': 'No drill bits cut %s',
        # a shaft or cog's Strength, the load it carries, and a support's Stability, its top speed (Taran, 0.27.0)
        'mcremix.tooltip.strength': 'Strength:',
        'mcremix.tooltip.strength.flimsy': 'Flimsy',
        'mcremix.tooltip.strength.delicate': 'Delicate',
        'mcremix.tooltip.strength.light_duty': 'Light-duty',
        'mcremix.tooltip.strength.sturdy': 'Sturdy',
        'mcremix.tooltip.strength.unbreakable': 'Unbreakable',
        'mcremix.tooltip.stability': 'Stability:',
        'mcremix.tooltip.stability.wobbly': 'Wobbly',
        'mcremix.tooltip.stability.shaky': 'Shaky',
        'mcremix.tooltip.stability.steady': 'Steady',
        'mcremix.tooltip.stability.stable': 'Stable',
        'mcremix.tooltip.stability.very_stable': 'Very Stable',
        'mcremix.tooltip.stability.rock_solid': 'Rock-solid',
        'mcremix.saw_blade.stone': 'Stone blade: cuts wood and stone',
        'mcremix.saw_blade.iron': 'Iron blade: cuts wood and stone',
        'mcremix.saw_blade.gold': 'Gold blade: cuts wood, too soft for stone',
        'mcremix.saw_blade.diamond': 'Diamond blade: cuts wood and stone',
        'mcremix.furnace.cold': 'This furnace is cracked and cold for good. Build a new Furnace to fire things in, and cook food on a Campfire.',
        'mcremix.furnace.workstation': 'It cannot cook any more, but villagers still work at it. Fire things in a Furnace, and cook food on a Campfire.',
        'mcremix.jei.kiln': 'Furnace',
        'mcremix.jei.kiln_heat': 'Furnace Heat',
        'mcremix.jei.bloom_striking': 'Striking Blooms',
        'mcremix.jei.heat_and_time': '%s \u00b7 %ss',
        'mcremix.jei.time': '%ss a batch',
        'mcremix.jei.bonus': '%s: twice as many',
        'mcremix.jei.bonus.short': '%s: \u00d72',
        'mcremix.jei.bonus.note': 'At %s or hotter, twice as many come out',
        'mcremix.jei.byproduct': 'Given off as each one fires. Draw it off the Furnace with a pipe or a bucket; once a bucket\'s worth waits there, the rest burns off.',
        'mcremix.jei.hotter_is_faster': 'Each heat level above this halves the time',
        'mcremix.jei.big_crucible': 'A big crucible works nine times as fast',
        'mcremix.jei.melting': 'Melting',
        'mcremix.jei.alloying': 'Alloying',
        'mcremix.jei.casting': 'Casting',
        'mcremix.jei.casting.sets': 'Sets in %ss',
        'mcremix.jei.casting.how': 'Tip a Crucible to pour over a Sand Mold on a depot or belt. Full, the mold sets, then the casting breaks out and the sand falls away',
        'mcremix.jei.settling': 'Settling',
        'mcremix.jei.settling.time': 'Left to stand %ss',
        'mcremix.jei.settling.basin': 'In a basin with no heat under it and no mixer or press over it, or in a vat',
        'mcremix.jei.settling.vat': 'A vat settles a batch for each bucket it holds, up to 16 at once',
        'mcremix.jei.electrolysis': 'Electrolysis',
        'mcremix.jei.electrolysis.charge': '%s C: %ss at %s A',
        'mcremix.jei.electrolysis.charge.note': 'Charge is amps times seconds: the more current through the Electrolyser, the faster it splits',
        'mcremix.jei.electrolysis.anode': 'Its anode burns away with the power through it, whatever it splits',
        'mcremix.jei.electrolysis.gas': 'A gas: it escapes from a basin, and only a vat keeps it',
        'mcremix.jei.fuel': 'Fuel',
        'mcremix.jei.fuel.lit': 'Burns once the Furnace is lit with flint and steel, a fire charge or a spark plug',
        'mcremix.jei.below': 'Below',
        'mcremix.jei.fanned': 'Fanned',
        'mcremix.jei.fanned.note': 'A Bellows fanning a %s fire',
        'mcremix.jei.below.lava': 'Lava under it',
        'mcremix.jei.below.fire': 'Fire under it',
        'mcremix.jei.below.soul_fire': 'Soul fire under it (light soul soil or soul sand)',
        'mcremix.jei.below.furnace': 'A Furnace burning %s or hotter under it',
        'mcremix.jei.below.hungry_burner': 'A blaze burner with nothing to burn',
        'mcremix.jei.below.fed_burner': 'A blaze burner fed with fuel',
        'mcremix.jei.below.superheated_burner': 'A blaze burner superheated with a Blaze Cake',
        'mcremix.jei.below.electric': 'At %s\u00b0C or hotter',
        'mcremix.jei.strikes': '%s strikes on an anvil',
        'mcremix.jei.info.cold_furnace': 'This has gone cold for good. Fire things in a Furnace instead, and cook food on a Campfire.',
        'mcremix.jei.info.workstation': 'Villagers still work at it, though.',
        'mcremix.jei.info.slag': 'Use it on crops and saplings like bone meal. Washing it in front of an Encased Fan recovers iron nuggets.',
        'mcremix.ponder.ui.pause': 'Pause',
        'mcremix.ponder.ui.play': 'Play',
        'mcremix.ponder.tag.heat': 'Heat',
        'mcremix.ponder.tag.heat.description': 'Furnaces, bellows, crucibles, heat sinks, fuels and heat sources',
        'mcremix.ponder.tag.kinetic_materials': 'Kinetic Materials',
        'mcremix.ponder.tag.kinetic_materials.description': 'Shafts, cogs, gearboxes, clutches, gearshifts, chain drives and brackets made of wood, andesite, brass, copper, iron, steel, aluminium and titanium, and the supports that hold them',
    }
    part_names = {'shaft': 'Shaft', 'cogwheel': 'Cogwheel', 'large_cogwheel': 'Large Cogwheel', 'gearbox': 'Gearbox',
                  'clutch': 'Clutch', 'gearshift': 'Gearshift', 'chain_drive': 'Chain Drive',
                  'chain_gearshift': 'Chain Gearshift', 'bracket': 'Bracket'}
    switched = {'clutch': "_Disconnects_ the far side, like Create's Clutch.",
                'gearshift': "_Reverses_ the far side, like Create's Gearshift.",
                'chain_gearshift': "The rest of its row turns _up to twice as fast_, by signal strength, like Create's "
                                   "Adjustable Chain Gearshift. Its own shaft keeps its speed."}
    # since 0.27.0 (Taran) a shaft or cog has a Strength, the load it carries, and turns as fast as its supports allow;
    # a support has a Stability, its top speed, and takes any load
    part_summaries = {
        'wooden': ('Cheap and weak: it carries the least, _2048 su_, so wood is the first thing to _give way_ when a '
                   'network is overloaded.'),
        'brass': ('Fine and delicate, like clockwork: it carries only _3072 su_, kept low because brass has some '
                  'powerful devices. _Never tarnishes_.'),
        'copper': 'Light-duty: it carries _4096 su_. It _oxidizes_ like copper blocks. Wax with honeycomb.',
        'iron': 'Takes _any load_, but _rusts_ where it gets wet. Wax with honeycomb.',
        'steel': ('Takes _any load_ and reaches far from a support. Where it gets wet it gets _spotted with rust_, but '
                  'never worse. Wax with honeycomb.'),
        'aluminium': ('Light-duty (_8192 su_), but it _never weathers_ and reaches far from a support. _Melts_ beside '
                      'anything _Hot_: lava, fire, a lit Furnace, a Blaze Burner or molten metal.'),
        'titanium': 'Takes _any load_, reaches far from a support and _never weathers_.',
    }
    support_summaries = {
        'wooden': 'Cheap and slow: it turns at _64 RPM_ at most, the slowest support.',
        'brass': ('Fine and delicate, like clockwork: _96 RPM_ at most, kept low because brass has some powerful '
                  'devices. _Never tarnishes_.'),
        'copper': 'Fast: up to _256 RPM_. It _oxidizes_, adding friction. Wax with honeycomb.',
        'iron': ('Fast: up to _256 RPM_, but a little heavy to turn, and it _rusts_ where it gets wet, adding '
                 'friction. Wax with honeycomb.'),
        'steel': ('Faster: up to _512 RPM_, but the _heaviest_ to turn. Where it gets wet it gets _spotted with rust_, '
                  'never worse, adding a little friction. Wax with honeycomb.'),
        'aluminium': ('Fast (_512 RPM_) with _no drag_, and _never weathers_. _Melts_ beside anything _Hot_: lava, '
                      'fire, a lit Furnace, a Blaze Burner or molten metal.'),
        'titanium': ('The fastest of all (_2048 RPM_), with _no drag_, and it _never weathers_. It needs _twice the '
                     'cooling_ aluminium does, keeps the _temper colour_ heat gives it, and _burns_ past _1000 °C_.'),
    }
    holders = 'a gearbox, clutch, gearshift, chain drive, machine, casing or bolted bracket'
    for name, material, part, stage, waxed, tex in VARIANTS:
        lang[f'block.mcremix.{name}'] = tinted_title(name.replace('_' + part, '')) + ' ' + part_names[part]
        lang[f'block.mcremix.{name}.tooltip.summary'] = (
            (part_summaries if part in BARE else support_summaries)[material]
            + (' Cut from a stem, it _does not burn_.' if tex in FIREPROOF_WOODS else ''))
        if part in BARE:
            span = SPANS[material]
            lang[f'block.mcremix.{name}.tooltip.condition1'] = 'Needs holding'
            lang[f'block.mcremix.{name}.tooltip.behaviour1'] = (
                f'Goes only within _{span} block{"s" if span > 1 else ""}_ of a _support_ along its axis '
                f'({holders}), counting through the shafts and cogs between, and _drops off_ when that goes. It turns '
                'as fast as its supports allow. Place it into a _bracket_ standing on its own, or hold one in your '
                'offhand to fit it as you place it.')
            lang[f'block.mcremix.{name}.tooltip.condition2'] = 'When encased'
            lang[f'block.mcremix.{name}.tooltip.behaviour2'] = (
                'Use a _casing_ of any material on it: the casing is a _support_ that sets its _top speed_, whatever '
                'the part is made of, and keeps it out of the weather. A _wrench_ while sneaking takes the casing off '
                'again.')
        if part == 'bracket':
            lang[f'block.mcremix.{name}.tooltip.condition1'] = 'Used on a shaft or cog'
            lang[f'block.mcremix.{name}.tooltip.behaviour1'] = (
                'Bolts to the _solid block_ beside the part and makes it a _support_: it holds itself up, other '
                'shafts and cogs can reach out from it, and it turns as fast as the _bracket_ allows, whatever the '
                'part is made of. It falls off if that block goes. Hold it in your _offhand_ to fit it as you place a '
                'part.')
            lang[f'block.mcremix.{name}.tooltip.condition2'] = 'Placed on its own'
            lang[f'block.mcremix.{name}.tooltip.behaviour2'] = (
                'Goes down against a _solid face_, and a shaft or cog placed into it lies along it and is held by it. '
                'A _wrench_ turns it to take a shaft the other way.')
        if part == 'gearbox':
            lang[f'block.mcremix.{name}.tooltip.condition1'] = 'When placed'
            lang[f'block.mcremix.{name}.tooltip.behaviour1'] = (
                'Stands upright, or _lies on its side_ to meet a shaft above or below it. _Sneak_ to lay it down anywhere.')
        if part == 'chain_drive':
            lang[f'block.mcremix.{name}.tooltip.condition1'] = 'In a row'
            lang[f'block.mcremix.{name}.tooltip.behaviour1'] = (
                "_Relays rotation_ to the chain drives beside it, of any material, like Create's Encased Chain Drive.")
        if part in switched:
            lang[f'block.mcremix.{name}.tooltip.condition1'] = 'When powered by redstone'
            lang[f'block.mcremix.{name}.tooltip.behaviour1'] = switched[part]
    for name in STONE_ANVILS:
        lang[f'block.mcremix.{name}.tooltip.summary'] = (
            'A rough anvil for beating out _iron blooms_ before you have iron. No menu. _Cracks_ as you work it, '
            'and crumbles to _cobblestone_ after about _fifteen_ blooms.')
    for material in FITTINGS:
        lang[f'item.mcremix.{bits_name(material)}.tooltip.condition1'] = 'When used on a Mechanical Drill'
        lang[f'item.mcremix.{bits_name(material)}.tooltip.behaviour1'] = (
            'Fits them to it, and its old bits _pop out_. A _Deployer_ holding them fits them too. A broken drill '
            '_keeps_ its bits.')
        lang[f'item.mcremix.{blade_name(material)}.tooltip.condition1'] = 'When used on a Mechanical Saw'
        lang[f'item.mcremix.{blade_name(material)}.tooltip.behaviour1'] = (
            'Fits it to the saw, and its old blade _pops out_. A _Deployer_ holding it fits it too. A broken saw '
            '_keeps_ its blade.')
    lang.update(ponder_lang())
    lang.update(create_ponder_lang())
    lang.update(fluid_lang())
    lang.update(steam_engine_lang())
    lang.update(electric_pump_lang())
    lang.update(heat_sink_lang())
    lang.update(chemical_lang())
    lang.update(red_mud_lang())
    lang.update(electrolysis_lang())
    lang.update(encased_lang())
    lang.update(plastics_lang())
    lang.update(vat_mixer_lang())
    lang.update(spark_plug_lang())
    lang.update(pressurizer_lang())
    lang.update(machine_lang())
    lang.update(mover_lang())
    lang.update(cooling_lang())
    lang.update(titanium_lang())
    lang.update(titanium_plating_lang())
    lang.update(titanium_chain_lang())
    write_json(asset('lang', 'en_us.json'), lang)
    # Vanilla's furnace no longer works, so it is the Cold Furnace. This has to go in the minecraft namespace's file:
    # the game reads each namespace's lang files in turn, in no fixed order, so a vanilla key in ours could lose to
    # vanilla's own, while within one namespace mod files are read after vanilla's and win.
    write_json(os.path.join(RES, 'assets', 'minecraft', 'lang', 'en_us.json'), {'block.minecraft.furnace': 'Cold Furnace'})


# ------------------------------------------------------------------------------------------------ data

def loot_self(name):
    return {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': f'mcremix:{name}'}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
    }


def tag(values, required=True):
    return {'replace': False, 'values': values if required else [{'id': v, 'required': False} for v in values]}


def gen_data():
    write_json(data('mcremix', 'loot_tables', 'blocks', 'kiln.json'), loot_self('kiln'))
    write_json(data('mcremix', 'loot_tables', 'blocks', 'steel_furnace.json'), loot_self('steel_furnace'))
    write_json(data('mcremix', 'loot_tables', 'blocks', 'bellows.json'), loot_self('bellows'))
    for name, *_ in VARIANTS:
        write_json(data('mcremix', 'loot_tables', 'blocks', name + '.json'), loot_self(name))
    for name in STONE_ANVILS:
        write_json(data('mcremix', 'loot_tables', 'blocks', name + '.json'), loot_self(name))
    write_json(data('mcremix', 'loot_tables', 'blocks', 'iron_casing.json'), loot_self('iron_casing'))
    write_json(data('mcremix', 'loot_tables', 'blocks', 'steel_casing.json'), loot_self('steel_casing'))
    write_json(data('mcremix', 'loot_tables', 'blocks', 'aluminium_casing.json'), loot_self('aluminium_casing'))
    write_json(data('mcremix', 'loot_tables', 'blocks', 'titanium_casing.json'), loot_self('titanium_casing'))
    # every tier of the machines that make things drops itself; a drill or saw keeps its bits or blade (keep_fittings)
    for block in machines():
        name = block.split(':')[1]
        write_json(data('mcremix', 'loot_tables', 'blocks', name + '.json'), loot_self(name))
    write_json(data('minecraft', 'tags', 'blocks', 'mineable', 'shovel.json'), tag(['mcremix:black_sand']))
    # as Create's own: Contraption Controls switch drills, saws and deployers, and every machine but the millstone
    # (a pickaxe's) takes either tool
    write_json(data('create', 'tags', 'items', 'contraption_controlled.json'),
               tag(machines('mechanical_saw', 'mechanical_drill', 'deployer')))

    metal_parts = [f'mcremix:{v[0]}' for v in VARIANTS if v[1] != 'wooden']
    write_json(data('minecraft', 'tags', 'blocks', 'mineable', 'pickaxe.json'),
               tag(['mcremix:kiln', 'mcremix:steel_furnace'] + metal_parts + [f'mcremix:{n}' for n in STONE_ANVILS]
                   + machines()
                   + ['mcremix:iron_casing', 'mcremix:steel_casing', 'mcremix:aluminium_casing',
                      'mcremix:titanium_casing', 'mcremix:heat_sink',
                      'mcremix:electrolyser', 'mcremix:spark_plug', 'mcremix:pressurizer', 'mcremix:pressurizer_vessel',
                      'mcremix:water_cooler',
                      'mcremix:radiator', 'mcremix:arc_furnace']
                   + ['mcremix:red_mud_bricks', 'mcremix:red_mud_brick_stairs', 'mcremix:red_mud_brick_slab',
                      'mcremix:red_mud_brick_wall']
                   + fluid_pickaxe_blocks() + encased_blocks() + vat_mixers_mined_with('pickaxe')
                   + titanium_pickaxe_blocks()))
    wooden_parts = [f'mcremix:{v[0]}' for v in VARIANTS if v[1] == 'wooden']
    write_json(data('minecraft', 'tags', 'blocks', 'mineable', 'axe.json'),
               tag(wooden_parts + ['mcremix:bellows'] + [m for m in machines() if not m.endswith('_millstone')]
                   + ['mcremix:iron_casing', 'mcremix:steel_casing', 'mcremix:aluminium_casing',
                      'mcremix:titanium_casing']
                   + encased_blocks() + vat_mixers_mined_with('axe') + movers()))
    # any wood's shaft does where a recipe wants a wooden one
    write_json(data('mcremix', 'tags', 'items', 'wooden_shafts.json'),
               tag([f'mcremix:{wood}_shaft' for wood in WOODS]))
    # Iron, Steel, Aluminium and Titanium Casing are Create's casings: windows, encasing and the rest look for this tag
    casings = ['mcremix:iron_casing', 'mcremix:steel_casing', 'mcremix:aluminium_casing', 'mcremix:titanium_casing']
    write_json(data('create', 'tags', 'blocks', 'casing.json'), tag(casings))
    write_json(data('create', 'tags', 'items', 'casing.json'), tag(casings))
    # a schematicannon places these with what they hold, as it does Create's own
    write_json(data('create', 'tags', 'blocks', 'safe_nbt.json'),
               tag(movers('mechanical_piston', 'sticky_mechanical_piston', 'mechanical_bearing', 'rope_pulley')))
    # steel under the common names, so other mods' recipes take it
    for kind, name in (('ingots', 'steel_ingot'), ('nuggets', 'steel_nugget'), ('plates', 'steel_sheet')):
        write_json(data('forge', 'tags', 'items', kind, 'steel.json'), tag([f'mcremix:{name}']))
    # aluminium too, under both spellings: other mods' recipes ask for "aluminum", and this mod's own (the crucible's
    # melting among them) take other mods' aluminum as well
    for kind, name in (('ingots', 'aluminium_ingot'), ('nuggets', 'aluminium_nugget'), ('plates', 'aluminium_sheet')):
        write_json(data('forge', 'tags', 'items', kind, 'aluminum.json'), tag([f'mcremix:{name}']))
        write_json(data('forge', 'tags', 'items', kind, 'aluminium.json'),
                   tag([f'mcremix:{name}', {'id': f'#forge:{kind}/aluminum', 'required': False}]))
    for kind, name in (('ingots', 'titanium_ingot'), ('nuggets', 'titanium_nugget'), ('plates', 'titanium_sheet')):
        write_json(data('forge', 'tags', 'items', kind, 'titanium.json'), tag([f'mcremix:{name}']))

    # heat sources: anything beneath a kiln
    write_json(data('mcremix', 'tags', 'blocks', 'heat_sources', 'warm.json'),
               tag(['minecraft:campfire', 'minecraft:magma_block']))
    write_json(data('mcremix', 'tags', 'blocks', 'heat_sources', 'hot.json'), tag(['minecraft:fire', 'minecraft:lava']))
    write_json(data('mcremix', 'tags', 'blocks', 'heat_sources', 'searing.json'),
               tag(['minecraft:soul_campfire', 'minecraft:soul_fire']))
    write_json(data('mcremix', 'tags', 'blocks', 'heat_sources', 'blazing.json'), tag([]))
    # read by temperature instead (HeatSources.fromCelsius); bellows cannot fan these
    write_json(data('mcremix', 'tags', 'blocks', 'heat_sources', 'electric.json'),
               tag(['powergrid:basin_heater', 'powergrid:heating_coil'], required=False))

    # what Create's blocks and the fluid parts are made of (KineticMaterials; this mod's parts know their own). Create's
    # shaft, cogs, gearbox, clutch, gearshift, chain drives, andesite casing and metal bracket are the andesite tier; its
    # brass-cased shafts, cogs, speed controller and sequenced gearshift are brass; its pump is copper. Its own machines
    # are the andesite tier of each (MACHINES), the crafter and arm brass, and its crushing wheel, which comes in
    # andesite alone, andesite too, as are its movers, whose wooden ones are wood. The machine tiers and wooden movers
    # know their own material
    andesite = ['create:shaft', 'create:cogwheel', 'create:large_cogwheel', 'create:andesite_encased_shaft',
                'create:andesite_encased_cogwheel', 'create:andesite_encased_large_cogwheel', 'create:gearbox',
                'create:clutch', 'create:gearshift', 'create:encased_chain_drive', 'create:adjustable_chain_gearshift',
                'create:metal_girder_encased_shaft', 'create:metal_bracket']
    andesite += [f'create:{block}' for block in CREATE_MOVERS['blockstates'] if block in MOVER_FRICTION_POINTS]
    brass = ['create:brass_encased_shaft', 'create:brass_encased_cogwheel', 'create:brass_encased_large_cogwheel',
             'create:rotation_speed_controller', 'create:sequenced_gearshift']
    andesite += [f'create:{m}' for m, base in MACHINES.items() if base == 'andesite'] + ['create:crushing_wheel']
    brass += [f'create:{m}' for m, base in MACHINES.items() if base == 'brass']
    pump = lambda m: [f'mcremix:{pump_name(m)}']
    materials = {'wooden': ['create:wooden_bracket'], 'andesite': andesite, 'brass': brass + pump('brass'),
                 'copper': ['create:mechanical_pump'], 'iron': pump('iron'),
                 'steel': pump('steel') + ['mcremix:pressurizer'], 'aluminium': pump('aluminium'), 'titanium': []}
    for material, blocks in materials.items():
        write_json(data('mcremix', 'tags', 'blocks', 'kinetic_material', material + '.json'),
                   tag(blocks, required=False))
    # friction points beyond the machines, gearboxes and the rest FrictionPoints knows: none of this mod's own now the
    # Steel Spring is gone, but packs can add them. And what turns but holds nothing: Create's flywheel, and its
    # crushing wheel (Taran), which carries any load but turns no faster than andesite's top speed
    # (supports, as players know them since 0.23.0; the tag's old name is kept inside it for packs that used it)
    write_json(data('mcremix', 'tags', 'blocks', 'supports.json'),
               tag([{'id': '#mcremix:friction_points', 'required': False}]))
    write_json(data('mcremix', 'tags', 'blocks', 'friction_points.json'), tag([]))
    write_json(data('mcremix', 'tags', 'blocks', 'holds_no_shafts.json'),
               tag(['create:flywheel', 'create:crushing_wheel'], required=False))
    # what an iron bloom can be struck on (BloomHammering)
    write_json(data('mcremix', 'tags', 'blocks', 'bloom_anvils.json'),
               tag(['#minecraft:anvil'] + [f'mcremix:{n}' for n in STONE_ANVILS]))
    write_json(data('mcremix', 'tags', 'blocks', 'cold_furnaces.json'),
               tag(['minecraft:furnace', 'minecraft:smoker', 'minecraft:blast_furnace']))

    # kiln fuels
    write_json(data('mcremix', 'tags', 'items', 'kiln_fuel', 'warm.json'),
               tag(['#minecraft:logs_that_burn', '#minecraft:planks', 'minecraft:stick']))
    # Immersive Weathering chars wood that fire passes over; charred wood is half way to charcoal, so it burns Hot
    charred = [{'id': f'immersive_weathering:charred_{n}', 'required': False}
               for n in ['log', 'planks', 'slab', 'stairs', 'fence', 'fence_gate']]
    write_json(data('mcremix', 'tags', 'items', 'kiln_fuel', 'hot.json'),
               {'replace': False, 'values': ['minecraft:charcoal', 'minecraft:coal', 'minecraft:coal_block',
                                             'minecraft:lava_bucket'] + charred})
    write_json(data('mcremix', 'tags', 'items', 'kiln_fuel', 'searing.json'),
               tag(['mcremix:coal_coke', 'minecraft:blaze_rod']))
    # what lights a Furnace or Steel Furnace by hand or from a deployer, as they light a campfire
    write_json(data('mcremix', 'tags', 'items', 'furnace_lighters.json'),
               tag(['minecraft:flint_and_steel', 'minecraft:fire_charge']))

    contraband = ['minecraft:diamond', 'minecraft:emerald', 'minecraft:iron_ingot', 'minecraft:gold_ingot',
                  'minecraft:iron_sword', 'minecraft:iron_pickaxe', 'minecraft:iron_shovel', 'minecraft:iron_axe',
                  'minecraft:iron_helmet', 'minecraft:iron_chestplate', 'minecraft:iron_leggings',
                  'minecraft:iron_boots', 'minecraft:chainmail_helmet', 'minecraft:chainmail_chestplate',
                  'minecraft:chainmail_leggings', 'minecraft:chainmail_boots', 'minecraft:diamond_sword',
                  'minecraft:diamond_pickaxe', 'minecraft:diamond_shovel', 'minecraft:diamond_axe',
                  'minecraft:diamond_helmet', 'minecraft:diamond_chestplate', 'minecraft:diamond_leggings',
                  'minecraft:diamond_boots', 'minecraft:saddle', 'minecraft:iron_horse_armor',
                  'minecraft:golden_horse_armor', 'minecraft:diamond_horse_armor', 'minecraft:obsidian',
                  'minecraft:enchanted_book']
    write_json(data('mcremix', 'tags', 'items', 'village_contraband.json'), tag(contraband))
    write_json(data('mcremix', 'loot_modifiers', 'village_contraband.json'), {
        'type': 'mcremix:remove_items', 'conditions': [], 'table_prefix': 'minecraft:chests/village/',
        'items': 'mcremix:village_contraband'})
    # Create's copper pipes, pumps and tanks drop with their patina, so they go back down as they were
    write_json(data('mcremix', 'loot_modifiers', 'keep_patina.json'), {'type': 'mcremix:keep_patina', 'conditions': []})
    # a drill or saw broken with other bits or another blade keeps them in the item (KeepFittingsModifier)
    write_json(data('mcremix', 'loot_modifiers', 'keep_fittings.json'),
               {'type': 'mcremix:keep_fittings', 'conditions': []})
    write_json(data('forge', 'loot_modifiers', 'global_loot_modifiers.json'),
               {'replace': False, 'entries': ['mcremix:village_contraband', 'mcremix:keep_patina',
                                              'mcremix:keep_fittings']})

    gen_fluid_data()
    gen_recipes()
    gen_power_grid_recipes()


def recipe(path, obj):
    write_json(data('mcremix', 'recipes', path + '.json'), obj)


def disabled(ns, path):
    write_json(data(ns, 'recipes', path + '.json'), {'conditions': [{'type': 'forge:false'}]})


def item(i, count=1):
    r = {'item': i}
    if count != 1:
        r['count'] = count
    return r


def firing(path, ingredient, result, heat, time, count=1, bonus=None, byproduct=None):
    obj = {'type': 'mcremix:firing', 'ingredient': ingredient, 'result': item(result, count), 'heat': heat,
           'time': time}
    if bonus:
        obj['bonus_heat'] = bonus
    if byproduct:
        obj['byproduct'] = {'fluid': byproduct[0], 'amount': byproduct[1]}
    recipe('firing/' + path, obj)


def gen_recipes():
    # iron goes through the bloom
    for p in ['iron_ingot_from_smelting_raw_iron',
              'iron_ingot_from_blasting_raw_iron', 'iron_ingot_from_smelting_iron_ore',
              'iron_ingot_from_blasting_iron_ore', 'iron_ingot_from_smelting_deepslate_iron_ore',
              'iron_ingot_from_blasting_deepslate_iron_ore']:
        disabled('minecraft', p)
    disabled('create', 'smelting/iron_ingot_from_crushed')
    disabled('create', 'blasting/iron_ingot_from_crushed')
    # ancient debris needs a Blazing kiln (see firing/netherite_scrap), not a lava fan
    disabled('minecraft', 'netherite_scrap')
    disabled('minecraft', 'netherite_scrap_from_blasting')

    # smokers and blast furnaces are villager workstations only; build them around a kiln or a salvaged furnace
    core = [item('mcremix:kiln'), item('minecraft:furnace')]
    write_json(data('minecraft', 'recipes', 'smoker.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' L ', 'LKL', ' L '],
        'key': {'L': {'tag': 'minecraft:logs'}, 'K': core}, 'result': item('minecraft:smoker')})
    write_json(data('minecraft', 'recipes', 'blast_furnace.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['III', 'IKI', 'SSS'],
        'key': {'I': {'tag': 'forge:ingots/iron'}, 'K': core, 'S': item('minecraft:smooth_stone')},
        'result': item('minecraft:blast_furnace')})

    # the Furnace is crafted exactly as vanilla's furnace is: vanilla's own recipe makes ours now, and keeps its id so
    # the recipe book offers it as soon as a player picks up cobblestone (vanilla's furnace is no longer made)
    write_json(data('minecraft', 'recipes', 'furnace.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['###', '# #', '###'],
        'key': {'#': {'tag': 'minecraft:stone_crafting_materials'}}, 'result': item('mcremix:kiln')})

    # the vanilla anvil's shape in stone: smooth stone face, a stone waist, a cobblestone foot
    recipe('stone_anvil', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['SSS', ' T ', 'CCC'],
        'key': {'S': item('minecraft:smooth_stone'), 'T': {'tag': 'forge:stone'}, 'C': {'tag': 'forge:cobblestone'}},
        'result': item('mcremix:stone_anvil')})

    # drill bits and saw blades, fitted to any drill or saw: three of the material around an iron ingot make bits, and
    # around an iron sheet a blade (Taran's card). Iron ones are what every drill and saw comes with
    fitting_materials = {'stone': {'tag': 'forge:cobblestone'}, 'iron': {'tag': 'forge:ingots/iron'},
                         'gold': {'tag': 'forge:ingots/gold'}, 'diamond': {'tag': 'forge:gems/diamond'}}
    for material, ingredient in fitting_materials.items():
        for name, core in ((bits_name(material), {'tag': 'forge:ingots/iron'}),
                           (blade_name(material), {'tag': 'forge:plates/iron'})):
            recipe(name, {
                'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' B ', 'BIB'],
                'key': {'B': ingredient, 'I': core}, 'result': item(f'mcremix:{name}')})

    # lid of planks, leather bag around the andesite outlet, base with the drive shaft
    recipe('bellows', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['PPP', 'LAL', 'PSP'],
        'key': {'P': {'tag': 'minecraft:planks'}, 'L': {'tag': 'forge:leather'}, 'A': item('create:andesite_alloy'),
                'S': [{'tag': 'mcremix:wooden_shafts'}, item('create:shaft')]},
        'result': item('mcremix:bellows')})

    # Warm: ceramics and charcoal, the things wood fire can do
    # logs give off wood spirit as they char, for pipes to draw off the Furnace
    firing('charcoal', {'tag': 'minecraft:logs_that_burn'}, 'minecraft:charcoal', 'warm', 300,
           byproduct=('mcremix:wood_spirit', 50))
    firing('brick', item('minecraft:clay_ball'), 'minecraft:brick', 'warm', 160)
    firing('terracotta', item('minecraft:clay'), 'minecraft:terracotta', 'warm', 300)
    firing('sponge', item('minecraft:wet_sponge'), 'minecraft:sponge', 'warm', 200)
    firing('green_dye', item('minecraft:cactus'), 'minecraft:green_dye', 'warm', 200)
    firing('lime_dye', item('minecraft:sea_pickle'), 'minecraft:lime_dye', 'warm', 200)
    # Hot
    # coal gives off its tar as it cokes, for pipes to draw off the Furnace
    firing('coal_coke', item('minecraft:coal'), 'mcremix:coal_coke', 'hot', 400, byproduct=('mcremix:coal_tar', 50))
    # Bakelite: the phenol in coal tar set hard with the formaldehyde wood spirit makes, under heat and pressure
    # iron dissolved in chlorine water: the circuit board etchant. It takes a gas, so only a vat can mix it
    recipe('vat_mixing/ferric_chloride', {
        'type': 'mcremix:vat_mixing',
        'ingredients': [{'tag': 'forge:nuggets/iron'}, {'fluid': 'mcremix:chlorine', 'amount': 250},
                        {'fluid': 'minecraft:water', 'amount': 250}],
        'results': [{'fluid': 'mcremix:ferric_chloride', 'amount': 250}]})
    # the Bayer process: bauxite dissolved in hot caustic soda, leaving red mud; the liquor settles out aluminium
    # hydroxide as it cools, which is fired into alumina
    recipe('mixing/bauxite_liquor', {
        'type': 'create:mixing', 'heatRequirement': 'heated',
        'ingredients': [item('mcremix:bauxite'), item('mcremix:bauxite'),
                        {'fluid': 'mcremix:caustic_soda', 'amount': 250}],
        'results': [{'fluid': 'mcremix:bauxite_liquor', 'amount': 250}, item('mcremix:red_mud')]})
    # Create's red sand washing, with bauxite beside its gold nuggets and dead bushes
    write_json(data('create', 'recipes', 'splashing', 'red_sand.json'), {
        'type': 'create:splashing', 'ingredients': [item('minecraft:red_sand')],
        'results': [{'item': 'minecraft:gold_nugget', 'count': 3, 'chance': 0.125},
                    {'item': 'mcremix:bauxite', 'chance': 0.25},
                    {'item': 'minecraft:dead_bush', 'chance': 0.05}]})
    recipe('compacting/bakelite', {
        'type': 'create:compacting', 'heatRequirement': 'heated',
        'ingredients': [{'fluid': 'mcremix:coal_tar', 'amount': 100}, {'fluid': 'mcremix:wood_spirit', 'amount': 100}],
        'results': [item('mcremix:bakelite')]})
    firing('popped_chorus_fruit', item('minecraft:chorus_fruit'), 'minecraft:popped_chorus_fruit', 'hot', 200)
    # Searing: iron blooms
    firing('iron_bloom_from_raw_iron', item('minecraft:raw_iron'), 'mcremix:iron_bloom', 'searing', 300)
    firing('iron_bloom_from_iron_ore', [item('minecraft:iron_ore'), item('minecraft:deepslate_iron_ore')],
           'mcremix:iron_bloom', 'searing', 400)
    firing('iron_bloom_from_crushed_raw_iron', item('create:crushed_raw_iron'), 'mcremix:iron_bloom', 'searing',
           250, bonus='blazing')
    # Searing: iron packed in coke and sealed in clay soaks up carbon, the slowest firing there is
    firing('blister_steel', item('mcremix:cementation_pack'), 'mcremix:blister_steel', 'searing', 800, count=4)
    firing('alumina', item('mcremix:aluminium_hydroxide'), 'mcremix:alumina', 'searing', 300)
    # Blazing
    firing('netherite_scrap', item('minecraft:ancient_debris'), 'minecraft:netherite_scrap', 'blazing', 400)

    # Create processing
    recipe('pressing/iron_bloom', {
        'type': 'create:pressing', 'ingredients': [item('mcremix:iron_bloom')],
        'results': [item('minecraft:iron_ingot'), {'item': 'mcremix:slag', 'chance': 0.5}]})
    recipe('splashing/slag', {
        'type': 'create:splashing', 'ingredients': [item('mcremix:slag')],
        'results': [{'item': 'minecraft:iron_nugget', 'chance': 0.5}, {'item': 'minecraft:iron_nugget', 'chance': 0.25},
                    {'item': 'minecraft:gravel', 'chance': 0.3}]})
    # Power Grid's electromagnet pulls twice the iron out of slag that washing does, but leaves no gravel
    recipe('magnetization/slag', {
        'type': 'powergrid:magnetization', 'ingredients': [item('mcremix:slag')],
        'results': [item('minecraft:iron_nugget'), {'item': 'minecraft:iron_nugget', 'chance': 0.5}]})

    # a cementation pack: four iron ingots in four coal coke, sealed with a clay ball, in any order
    recipe('cementation_pack', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [item('minecraft:iron_ingot')] * 4 + [item('mcremix:coal_coke')] * 4 + [item('minecraft:clay_ball')],
        'result': item('mcremix:cementation_pack')})
    # steel: blister steel only a Mechanical Press can beat into ingots; ingots press into sheets, and a sheet applied to
    # a stripped log makes Steel Casing, as Create makes its casings
    recipe('pressing/steel_ingot', {'type': 'create:pressing', 'ingredients': [item('mcremix:blister_steel')],
                                    'results': [item('mcremix:steel_ingot')]})
    recipe('pressing/steel_sheet', {'type': 'create:pressing', 'ingredients': [item('mcremix:steel_ingot')],
                                    'results': [item('mcremix:steel_sheet')]})
    recipe('steel_nugget', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                            'ingredients': [item('mcremix:steel_ingot')], 'result': item('mcremix:steel_nugget', 9)})
    recipe('steel_ingot_from_nuggets', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['NNN', 'NNN', 'NNN'],
        'key': {'N': item('mcremix:steel_nugget')}, 'result': item('mcremix:steel_ingot')})
    for base, under in (('log', 'forge:stripped_logs'), ('wood', 'forge:stripped_wood')):
        recipe(f'item_application/steel_casing_from_{base}', {
            'type': 'create:item_application', 'ingredients': [{'tag': under}, item('mcremix:steel_sheet')],
            'results': [item('mcremix:steel_casing')]})
        # Iron Casing the same way, from Create's iron sheet: what iron's gearboxes, encased parts and machines are
        # built on (Taran, 0.23.0), in place of Create's Block of Industrial Iron
        recipe(f'item_application/iron_casing_from_{base}', {
            'type': 'create:item_application', 'ingredients': [{'tag': under}, {'tag': 'forge:plates/iron'}],
            'results': [item('mcremix:iron_casing')]})

    # aluminium is made into sheets, nuggets and casing as steel is
    recipe('pressing/aluminium_sheet', {'type': 'create:pressing', 'ingredients': [{'tag': 'forge:ingots/aluminium'}],
                                        'results': [item('mcremix:aluminium_sheet')]})
    recipe('aluminium_nugget', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                'ingredients': [item('mcremix:aluminium_ingot')],
                                'result': item('mcremix:aluminium_nugget', 9)})
    recipe('aluminium_ingot_from_nuggets', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['NNN', 'NNN', 'NNN'],
        'key': {'N': item('mcremix:aluminium_nugget')}, 'result': item('mcremix:aluminium_ingot')})
    # Aluminium Casing takes polyethylene, which locks every aluminium friction point built on it until stage 4 (Taran:
    # aluminium and titanium friction points all use polyethylene), so it is crafted rather than applied to a log
    recipe('aluminium_casing', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [item('mcremix:aluminium_sheet'), [{'tag': 'forge:stripped_logs'}, {'tag': 'forge:stripped_wood'}],
                        {'tag': 'mcremix:polyethylene'}],
        'result': item('mcremix:aluminium_casing')})

    # titanium the same, its casing taking polyethylene as aluminium's does
    recipe('pressing/titanium_sheet', {'type': 'create:pressing', 'ingredients': [{'tag': 'forge:ingots/titanium'}],
                                       'results': [item('mcremix:titanium_sheet')]})
    recipe('titanium_nugget', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                               'ingredients': [item('mcremix:titanium_ingot')],
                               'result': item('mcremix:titanium_nugget', 9)})
    recipe('titanium_ingot_from_nuggets', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['NNN', 'NNN', 'NNN'],
        'key': {'N': item('mcremix:titanium_nugget')}, 'result': item('mcremix:titanium_ingot')})
    recipe('titanium_casing', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [item('mcremix:titanium_sheet'), [{'tag': 'forge:stripped_logs'}, {'tag': 'forge:stripped_wood'}],
                        {'tag': 'mcremix:polyethylene'}],
        'result': item('mcremix:titanium_casing')})

    # kinetic parts. Wooden ones are made of a single wood each: its logs make its shafts, and its stripped log or
    # stripped wood is the casing of its gearbox and clutch
    redstone_dust = {'tag': 'forge:dusts/redstone'}
    for wood in WOODS:
        shaft, casing = item(f'mcremix:{wood}_shaft'), stripped_wood(wood)
        recipe(f'{wood}_shaft', {
            'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['L', 'L'],
            'key': {'L': {'tag': wood_logs(wood)}}, 'result': item(f'mcremix:{wood}_shaft', 6)})
        recipe(f'{wood}_gearbox', {
            'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' C ', 'CBC', ' C '],
            'key': {'C': shaft, 'B': casing}, 'result': item(f'mcremix:{wood}_gearbox')})
        # wood has no cogs, so its gearshift is its gearbox with redstone
        recipe(f'{wood}_clutch', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                  'ingredients': [casing, shaft, redstone_dust],
                                  'result': item(f'mcremix:{wood}_clutch')})
        recipe(f'{wood}_gearshift', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                     'ingredients': [item(f'mcremix:{wood}_gearbox'), redstone_dust],
                                     'result': item(f'mcremix:{wood}_gearshift')})
    for metal, ingot in [('brass', 'create:brass_ingot'), ('copper', 'minecraft:copper_ingot'),
                         ('iron', 'minecraft:iron_ingot'), ('steel', 'mcremix:steel_ingot'),
                         ('aluminium', 'mcremix:aluminium_ingot'), ('titanium', 'mcremix:titanium_ingot')]:
        recipe(f'{metal}_shaft', {
            'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['I', 'I'],
            'key': {'I': item(ingot)}, 'result': item(f'mcremix:{metal}_shaft', 6)})
        recipe(f'{metal}_cogwheel', {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc',
            'ingredients': [item(f'mcremix:{metal}_shaft'), item(ingot)], 'result': item(f'mcremix:{metal}_cogwheel')})
        recipe(f'{metal}_large_cogwheel', {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc',
            'ingredients': [item(f'mcremix:{metal}_shaft'), item(ingot), item(ingot)],
            'result': item(f'mcremix:{metal}_large_cogwheel')})
        recipe(f'{metal}_large_cogwheel_from_little', {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc',
            'ingredients': [item(f'mcremix:{metal}_cogwheel'), item(ingot)],
            'result': item(f'mcremix:{metal}_large_cogwheel')})
    # gearboxes: four parts around a casing, the same plus as Create's gearbox
    for name, rim, core in [
            ('brass_gearbox', item('mcremix:brass_cogwheel'), item('create:brass_casing')),
            ('copper_gearbox', item('mcremix:copper_cogwheel'), item('create:copper_casing')),
            ('iron_gearbox', item('mcremix:iron_cogwheel'), item('mcremix:iron_casing')),
            ('steel_gearbox', item('mcremix:steel_cogwheel'), item('mcremix:steel_casing')),
            ('aluminium_gearbox', item('mcremix:aluminium_cogwheel'), item('mcremix:aluminium_casing')),
            ('titanium_gearbox', item('mcremix:titanium_cogwheel'), item('mcremix:titanium_casing'))]:
        recipe(name, {
            'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' C ', 'CBC', ' C '],
            'key': {'C': rim, 'B': core}, 'result': item(f'mcremix:{name}')})
    # clutches and gearshifts: Create's recipes, a casing, a part and redstone, in each metal
    for name, ingredients in [
            ('copper_clutch', [item('create:copper_casing'), item('mcremix:copper_shaft')]),
            ('copper_gearshift', [item('create:copper_casing'), item('mcremix:copper_cogwheel')]),
            ('iron_clutch', [item('mcremix:iron_casing'), item('mcremix:iron_shaft')]),
            ('iron_gearshift', [item('mcremix:iron_casing'), item('mcremix:iron_cogwheel')]),
            ('steel_clutch', [item('mcremix:steel_casing'), item('mcremix:steel_shaft')]),
            ('steel_gearshift', [item('mcremix:steel_casing'), item('mcremix:steel_cogwheel')]),
            ('aluminium_clutch', [item('mcremix:aluminium_casing'), item('mcremix:aluminium_shaft')]),
            ('aluminium_gearshift', [item('mcremix:aluminium_casing'), item('mcremix:aluminium_cogwheel')]),
            ('titanium_clutch', [item('mcremix:titanium_casing'), item('mcremix:titanium_shaft')]),
            ('titanium_gearshift', [item('mcremix:titanium_casing'), item('mcremix:titanium_cogwheel')])]:
        recipe(name, {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                      'ingredients': ingredients + [redstone_dust], 'result': item(f'mcremix:{name}')})
    # chain drives: Create's, a casing and three nuggets for the chain, in each metal; a chain gearshift is a chain
    # drive with an electron tube
    for metal, casing in [('brass', item('create:brass_casing')), ('copper', item('create:copper_casing')),
                          ('iron', item('mcremix:iron_casing')), ('steel', item('mcremix:steel_casing')),
                          ('aluminium', item('mcremix:aluminium_casing')), ('titanium', item('mcremix:titanium_casing'))]:
        nugget = {'tag': f'forge:nuggets/{metal}'}
        recipe(f'{metal}_chain_drive', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                        'ingredients': [casing, nugget, nugget, nugget],
                                        'result': item(f'mcremix:{metal}_chain_drive')})
        if metal != 'brass':
            recipe(f'{metal}_chain_gearshift', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                                'ingredients': [item(f'mcremix:{metal}_chain_drive'),
                                                                item('create:electron_tube')],
                                                'result': item(f'mcremix:{metal}_chain_gearshift')})
    # brackets in Create's shape, four at a time: a row of nuggets (sticks for wood) over a plate of ingots (planks) either
    # side of a sheet (stripped log); aluminium's and titanium's sheet is polyethylene, as their other friction points
    # take
    def bracket(name, nugget, plate, core):
        recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['SSS', 'PCP'],
                      'key': {'S': nugget, 'P': plate, 'C': core}, 'result': item(f'mcremix:{name}', 4)})
    for wood in WOODS:
        bracket(f'{wood}_bracket', {'tag': 'forge:rods/wooden'}, item(f'minecraft:{wood}_planks'), stripped_wood(wood))
    for metal in ('brass', 'copper', 'iron', 'steel', 'aluminium', 'titanium'):
        core = {'tag': 'mcremix:polyethylene'} if metal in ('aluminium', 'titanium') else {'tag': f'forge:plates/{metal}'}
        bracket(f'{metal}_bracket', {'tag': f'forge:nuggets/{metal}'}, {'tag': f'forge:ingots/{metal}'}, core)
    # Create's metal bracket is the andesite one now that iron has its own: andesite alloy either side, zinc nuggets on
    # top. Its wooden bracket gives way to the eleven woods' own
    write_json(data('create', 'recipes', 'crafting', 'kinetics', 'metal_bracket.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['SSS', 'PCP'],
        'key': {'S': {'tag': 'forge:nuggets/zinc'}, 'P': item('create:andesite_alloy'), 'C': item('create:andesite_alloy')},
        'result': item('create:metal_bracket', 4), 'show_notification': True})
    disabled('create', 'crafting/kinetics/wooden_bracket')

    for name, material, part, stage, waxed, tex in VARIANTS:
        # wood, brass, aluminium and titanium never weather, so there is nothing to wax; nor does steel past its first
        # stage of rust (0.28.0), so its later parts, kept for worlds that have them, are not waxed either
        if waxed or material in ('wooden', 'brass', 'aluminium', 'titanium') or past_last_stage(material, stage):
            continue
        recipe(f'waxing/waxed_{name}', {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc',
            'ingredients': [item(f'mcremix:{name}'), item('minecraft:honeycomb')],
            'result': item(f'mcremix:waxed_{name}')})


# ------------------------------------------------------------------------------------------------ ponder schematics

class NBT:
    """Just enough of the NBT format to write structure files."""

    @staticmethod
    def _str(s):
        b = s.encode('utf-8')
        return struct.pack('>H', len(b)) + b

    @classmethod
    def payload(cls, kind, value):
        if kind == 'int':
            return struct.pack('>i', value)
        if kind == 'string':
            return cls._str(value)
        if kind == 'byte':
            return struct.pack('>b', value)
        if kind == 'float':
            return struct.pack('>f', value)
        if kind == 'compound':
            out = b''
            for name, (k, v) in value.items():
                out += bytes([TAG[k]]) + cls._str(name) + cls.payload(k, v)
            return out + b'\x00'
        if kind == 'list':
            elem_kind, items = value
            out = bytes([TAG[elem_kind] if items else 0]) + struct.pack('>i', len(items))
            for it in items:
                out += cls.payload(elem_kind, it)
            return out
        raise ValueError(kind)

    @classmethod
    def file(cls, root):
        return bytes([TAG['compound']]) + cls._str('') + cls.payload('compound', root)


TAG = {'byte': 1, 'int': 3, 'float': 5, 'string': 8, 'list': 9, 'compound': 10}


def structure(size, blocks):
    """blocks: {(x, y, z): (name, {prop: value})}, or (name, {prop: value}, {block entity tag: (kind, value)})"""
    palette, index, entries = [], {}, []
    for pos, (name, props, *nbt) in sorted(blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
        key = (name, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(palette)
            entry = {'Name': ('string', name)}
            if props:
                entry['Properties'] = ('compound', {k: ('string', str(v)) for k, v in props.items()})
            palette.append(entry)
        block = {'pos': ('list', ('int', list(pos))), 'state': ('int', index[key])}
        if nbt:
            block['nbt'] = ('compound', nbt[0])
        entries.append(block)
    root = {
        'DataVersion': ('int', 3465),
        'size': ('list', ('int', list(size))),
        'palette': ('list', ('compound', palette)),
        'blocks': ('list', ('compound', entries)),
        'entities': ('list', ('compound', [])),
    }
    return NBT.file(root)


def plate(blocks, size=5):
    for x in range(size):
        for z in range(size):
            blocks[(x, 0, z)] = ('minecraft:smooth_stone', {})


def bracketed(part, bracket, axis='x', facing='up'):
    """A shaft or cog with a bracket fitted, which its block entity keeps (Create's BracketedBlockEntityBehaviour). The
    bracket's plate is on the side away from {@code facing}, the face it was fitted on; the axis flag is Create's
    BracketBlock.getSuitableBracket."""
    face_axis = {'up': 'y', 'down': 'y', 'north': 'z', 'south': 'z', 'east': 'x', 'west': 'x'}[facing]
    along_first = axis == 'z' if face_axis != 'z' else axis == 'y'
    kind = 'cog' if 'cogwheel' in part else 'shaft'
    state = {'facing': ('string', facing), 'type': ('string', kind),
             'axis_along_first': ('string', 'false' if along_first else 'true')}
    be = 'create:simple_kinetic' if part.startswith('create:') else 'mcremix:tiered_kinetic'
    return (part, {'axis': axis}, {'id': ('string', be),
                                   'Bracket': ('compound', {'Name': ('string', bracket),
                                                            'Properties': ('compound', state)})})


def save_structure(path, size, blocks):
    full = asset('ponder', path + '.nbt')
    os.makedirs(os.path.dirname(full), exist_ok=True)
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode='wb', mtime=0) as gz:
        gz.write(structure(size, blocks))
    with open(full, 'wb') as f:
        f.write(buf.getvalue())


def kiln_state(facing='west', chimney=True):
    return ('mcremix:kiln', {'chimney': 'true' if chimney else 'false', 'facing': facing, 'heat': 0, 'lit': 'false'})


def power_grid_recipe(path, obj):
    """One of Power Grid's recipes, replaced: this mod loads after Power Grid (mods.toml), so its file wins."""
    write_json(data('powergrid', 'recipes', path + '.json'), obj)


def gen_power_grid_recipes():
    # an electric motor's frame and a transformer's core are steel sheet, where Power Grid has iron
    power_grid_recipe('mechanical_crafting/electric_motor', {
        'type': 'create:mechanical_crafting', 'acceptMirrored': True,
        'key': {'C': {'tag': 'forge:copper_coils'}, 'E': item('powergrid:conductive_casing'),
                'I': {'tag': 'forge:plates/steel'}, 'M': item('powergrid:magnet'), 'S': item('create:shaft')},
        'pattern': [' ICI ', 'CMSMC', ' ICI ', '  E  '], 'result': item('powergrid:electric_motor')})
    core = item('powergrid:incomplete_transformer_core')
    power_grid_recipe('sequenced_assembly/transformer_core', {
        'type': 'create:sequenced_assembly', 'ingredient': {'tag': 'forge:plates/steel'}, 'loops': 3,
        'results': [{'item': 'powergrid:transformer_core', 'chance': 100.0},
                    {'item': 'mcremix:steel_sheet', 'chance': 5.0}, {'item': 'mcremix:steel_nugget', 'chance': 3.0}],
        'sequence': [{'type': 'create:deploying', 'ingredients': [core, {'tag': 'forge:plates/steel'}],
                      'results': [core]},
                     {'type': 'create:pressing', 'ingredients': [core], 'results': [core]}],
        'transitionalItem': core})
    # blank boards are sawn from Bakelite, not wooden slabs, and the small switches, the button and the socket
    # get a Bakelite body
    power_grid_recipe('cutting/empty_circuit_slabs', {
        'type': 'create:cutting', 'ingredients': [item('mcremix:bakelite')], 'processingTime': 50,
        'results': [item('powergrid:empty_circuit', 2)]})
    # circuit boards are etched in ferric chloride, not Blazing Acid (which batteries still take)
    power_grid_recipe('mixing/etched_circuit_board', {
        'type': 'create:mixing', 'heatRequirement': 'heated',
        'ingredients': [item('powergrid:unetched_circuit'), {'fluid': 'mcremix:ferric_chloride', 'amount': 250}],
        'results': [item('powergrid:incomplete_circuit')]})
    # an integrated circuit's chip is cased in polyethylene, two pellets either side of the lapis
    power_grid_recipe('mechanical_crafting/integrated_circuit', {
        'type': 'create:mechanical_crafting', 'acceptMirrored': True,
        'key': {'G': item('minecraft:gold_nugget'), 'L': item('minecraft:lapis_lazuli'),
                'P': item('mcremix:polyethylene_pellets'), 'Q': item('create:polished_rose_quartz'),
                'R': item('minecraft:redstone')},
        'pattern': [' PLP ', 'RRQRR', ' GGG '], 'result': item('powergrid:integrated_circuit')})
    casing, bakelite = item('powergrid:conductive_casing'), item('mcremix:bakelite')
    for name, parts in (('lv_switch', [item('minecraft:lever'), item('powergrid:pins')]),
                        ('mv_switch', [item('minecraft:lever'), {'tag': 'forge:plates/copper'}]),
                        ('lv_button', [item('minecraft:stone_button'), item('powergrid:pins')]),
                        ('socket', [{'tag': 'forge:plates/copper'}, {'tag': 'forge:plates/brass'}])):
        power_grid_recipe(f'crafting/{name}', {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc',
            'ingredients': parts + [casing, bakelite], 'result': item(f'powergrid:{name}')})


def gen_ponder():
    b = {}
    plate(b)
    b[(2, 1, 2)] = kiln_state()
    save_structure('kiln/intro', (5, 3, 5), b)

    b = {}
    plate(b)
    b[(2, 1, 0)] = ('minecraft:campfire', {'lit': 'true', 'facing': 'west'})
    b[(2, 1, 2)] = ('minecraft:lava', {'level': 0})
    b[(2, 1, 4)] = ('create:blaze_burner', {'blaze': 'smouldering', 'facing': 'west'})
    for z in (0, 2, 4):
        b[(2, 2, z)] = kiln_state()
    save_structure('kiln/heat', (5, 4, 5), b)

    b = {}
    plate(b)
    b[(1, 1, 2)] = ('create:blaze_burner', {'blaze': 'kindled', 'facing': 'west'})
    b[(1, 2, 2)] = kiln_state()
    b[(3, 1, 2)] = ('mcremix:stone_anvil', {'facing': 'west'})
    save_structure('kiln/bloom', (5, 4, 5), b)

    b = {}
    plate(b)
    b[(2, 1, 2)] = kiln_state()
    save_structure('kiln/cementation', (5, 3, 5), b)

    b = {}
    plate(b)
    b[(0, 1, 2)] = ('create:creative_motor', {'facing': 'east'})
    b[(1, 1, 2)] = ('mcremix:bellows', {'facing': 'east'})
    b[(2, 1, 2)] = ('minecraft:campfire', {'lit': 'true', 'facing': 'west'})
    b[(2, 2, 2)] = kiln_state()
    save_structure('bellows/intro', (5, 3, 5), b)

    # a line of each material, slowest at the front, held by brackets of its own material bolted to the floor under
    # every other part
    b = {}
    plate(b)
    rows = {0: ('mcremix:oak_shaft', 'mcremix:oak_bracket'), 1: ('mcremix:brass_shaft', 'mcremix:brass_bracket'),
            2: ('create:shaft', 'create:metal_bracket'), 3: ('mcremix:copper_shaft', 'mcremix:copper_bracket'),
            4: ('mcremix:iron_shaft', 'mcremix:iron_bracket')}
    for z, (name, bracket) in rows.items():
        for x in range(5):
            b[(x, 1, z)] = bracketed(name, bracket) if x % 2 == 0 else (name, {'axis': 'x'})
    save_structure('kinetics/tiers', (5, 3, 5), b)

    # an iron line in front of a steel one, in brackets of their own; the scene casts a stripped log into Steel
    # Casing at the back
    b = {}
    plate(b)
    for x in range(5):
        b[(x, 1, 1)] = bracketed('mcremix:iron_shaft', 'mcremix:iron_bracket') if x % 2 == 0 \
            else ('mcremix:iron_shaft', {'axis': 'x'})
        b[(x, 1, 2)] = bracketed('mcremix:steel_shaft', 'mcremix:steel_bracket') if x % 2 == 0 \
            else ('mcremix:steel_shaft', {'axis': 'x'})
    b[(2, 1, 4)] = ('minecraft:stripped_spruce_log', {'axis': 'y'})
    save_structure('kinetics/steel', (5, 3, 5), b)

    # a copper gearbox holding copper shafts to the west, oak ones to the east and steel ones to the north, each as far
    # as its material reaches (4, 2 and 6 since 0.27.0), and the shaft of a motor to the south
    b = {}
    plate(b, 9)
    b[(4, 1, 6)] = ('mcremix:copper_gearbox', {'axis': 'y'})
    for x in range(4):
        b[(x, 1, 6)] = ('mcremix:copper_shaft', {'axis': 'x'})
    for x in (5, 6):
        b[(x, 1, 6)] = ('mcremix:oak_shaft', {'axis': 'x'})
    for z in range(6):
        b[(4, 1, z)] = ('mcremix:steel_shaft', {'axis': 'z'})
    b[(4, 1, 7)] = ('mcremix:copper_shaft', {'axis': 'z'})
    b[(4, 1, 8)] = ('create:creative_motor', {'facing': 'north'})
    save_structure('kinetics/friction_points', (9, 3, 9), b)

    # a copper line on the floor, and an iron one up on a post at the back; the scene fits the brackets
    b = {}
    plate(b)
    for x in range(5):
        b[(x, 1, 2)] = ('mcremix:copper_shaft', {'axis': 'x'})
    b[(2, 1, 0)] = ('minecraft:stone_bricks', {})
    for x in (1, 2, 3):
        b[(x, 2, 0)] = ('mcremix:iron_shaft', {'axis': 'x'})
    save_structure('kinetics/brackets', (5, 4, 5), b)

    # a motor turning an andesite shaft into Create's speed controller, a steel large cog in its top slot and steel
    # shafts along the cog's axis (FrictionPointScenes.speedController)
    b = {}
    plate(b)
    b[(0, 1, 2)] = ('create:creative_motor', {'facing': 'east'})
    b[(1, 1, 2)] = ('create:shaft', {'axis': 'x'})
    b[(2, 1, 2)] = ('create:rotation_speed_controller', {'axis': 'x'})
    b[(2, 2, 2)] = ('mcremix:steel_large_cogwheel', {'axis': 'z'})
    for z in (3, 4):
        b[(2, 2, z)] = ('mcremix:steel_shaft', {'axis': 'z'})
    save_structure('kinetics/speed_controller', (5, 3, 5), b)

    # a motor turning copper shafts either side of a copper gearbox; the scene encases them
    b = {}
    plate(b)
    b[(0, 1, 2)] = ('create:creative_motor', {'facing': 'east'})
    b[(1, 1, 2)] = ('mcremix:copper_shaft', {'axis': 'x'})
    b[(2, 1, 2)] = ('mcremix:copper_gearbox', {'axis': 'y'})
    for x in (3, 4):
        b[(x, 1, 2)] = ('mcremix:copper_shaft', {'axis': 'x'})
    save_structure('kinetics/encasing', (5, 3, 5), b)

    # a wooden shaft between a bracketed copper one on the motor's side and a bracketed andesite one on the fan's
    b = {}
    plate(b)
    b[(0, 1, 2)] = ('create:creative_motor', {'facing': 'east'})
    b[(1, 1, 2)] = bracketed('mcremix:copper_shaft', 'mcremix:copper_bracket')
    b[(2, 1, 2)] = ('mcremix:oak_shaft', {'axis': 'x'})
    b[(3, 1, 2)] = bracketed('create:shaft', 'create:metal_bracket')
    b[(4, 1, 2)] = ('create:encased_fan', {'facing': 'east'})
    save_structure('kinetics/wood_fuse', (5, 3, 5), b)

    # oak parts fed from the west: a shaft into a gearbox, a clutch to the east and a gearshift to the south, each
    # shaft a block from what holds it, as far as wood reaches. The scene cuts them from each wood in turn
    b = {}
    plate(b)
    b[(0, 1, 2)] = ('create:creative_motor', {'facing': 'east'})
    b[(1, 1, 2)] = ('mcremix:oak_shaft', {'axis': 'x'})
    b[(2, 1, 2)] = ('mcremix:oak_gearbox', {'axis': 'y'})
    b[(3, 1, 2)] = ('mcremix:oak_clutch', {'axis': 'x', 'powered': 'false'})
    b[(4, 1, 2)] = ('mcremix:oak_shaft', {'axis': 'x'})
    b[(2, 1, 1)] = ('mcremix:oak_shaft', {'axis': 'z'})
    b[(2, 1, 3)] = ('mcremix:oak_gearshift', {'axis': 'z', 'powered': 'false'})
    b[(2, 1, 4)] = ('mcremix:oak_shaft', {'axis': 'z'})
    save_structure('kinetics/woods', (5, 3, 5), b)

    b = {}
    plate(b)
    b[(0, 1, 2)] = ('create:creative_motor', {'facing': 'east'})
    b[(2, 1, 2)] = ('create:gearbox', {'axis': 'y'})
    for x in (1, 3, 4):
        b[(x, 1, 2)] = ('mcremix:copper_shaft', {'axis': 'x'})
    for z in (0, 1, 3, 4):
        b[(2, 1, z)] = ('mcremix:copper_shaft', {'axis': 'z'})
    for y in (2, 3):
        b[(2, y, 2)] = ('mcremix:copper_shaft', {'axis': 'y'})
    save_structure('kinetics/gearboxes', (5, 4, 5), b)

    # the scene swaps Create's clutch for ours; the lever beside it is shown later
    b = {}
    plate(b)
    b[(0, 1, 2)] = ('create:creative_motor', {'facing': 'east'})
    b[(2, 1, 2)] = ('create:clutch', {'axis': 'x', 'powered': 'false'})
    for x in (1, 3, 4):
        b[(x, 1, 2)] = ('mcremix:copper_shaft', {'axis': 'x'})
    b[(2, 1, 1)] = ('minecraft:lever', {'face': 'floor', 'facing': 'east', 'powered': 'false'})
    save_structure('kinetics/clutches', (5, 3, 5), b)

    # a row of Create's chain drives running east, fed from the north at its west end, driving a shaft south from its
    # east end; the scene swaps in ours, and the lever is shown later
    b = {}
    plate(b)
    b[(1, 1, 0)] = ('create:creative_motor', {'facing': 'south'})
    b[(1, 1, 1)] = ('mcremix:copper_shaft', {'axis': 'z'})
    for x, part in ((1, 'start'), (2, 'middle'), (3, 'end')):
        b[(x, 1, 2)] = ('create:encased_chain_drive', {'axis': 'z', 'axis_along_first': 'true', 'part': part})
    for z in (3, 4):
        b[(3, 1, z)] = ('mcremix:copper_shaft', {'axis': 'z'})
    b[(1, 1, 3)] = ('minecraft:lever', {'face': 'floor', 'facing': 'south', 'powered': 'false'})
    save_structure('kinetics/chain_drives', (5, 3, 5), b)


def gen_drill_ponder():
    """Four of Create's drills facing down onto a block each, a creative motor on top, all with the iron bits a drill
    comes with: the scene fits the others (DrillScenes). They stand on a line of equal depth so none hides another."""
    b = {}
    plate(b)
    for x, z in [(3, 1), (2, 2), (1, 3), (0, 4)]:
        b[(x, 1, z)] = ('minecraft:stone', {})
        b[(x, 2, z)] = ('create:mechanical_drill', {'facing': 'down', 'waterlogged': 'false'})
        b[(x, 3, z)] = ('create:creative_motor', {'facing': 'down'})
    save_structure('drills/bits', (5, 4, 5), b)


def gen_saw_ponder():
    """Four of Create's saws facing up in a row on one shaft (rotation axis x), the motor on the left of the screen,
    all with the iron blade a saw comes with: the scene fits the others (SawScenes). Items cross them towards the
    front (north)."""
    b = {}
    plate(b)
    b[(4, 1, 2)] = ('create:creative_motor', {'facing': 'west'})
    for x in (3, 2, 1, 0):
        b[(x, 1, 2)] = ('create:mechanical_saw', {'axis_along_first': 'true', 'facing': 'up', 'flipped': 'false'})
    save_structure('saws/blades', (5, 3, 5), b)


def gen_machine_ponder():
    """A press of each material in a line along x, Create's andesite one on the left of the screen, then copper, iron,
    steel and aluminium: the scene breaks the andesite one, makes the iron one steel and weathers the copper one
    (MachineScenes)."""
    b = {}
    plate(b)
    b[(4, 1, 2)] = ('create:mechanical_press', {'facing': 'west'})
    for x, material in ((3, 'copper'), (2, 'iron'), (1, 'steel'), (0, 'aluminium')):
        b[(x, 1, 2)] = (f'mcremix:{material}_mechanical_press', {'facing': 'west', 'patina': 'none'})
    save_structure('machines/tiers', (5, 3, 5), b)


def block_pos_nbt(pos):
    return ('compound', {'X': ('int', pos[0]), 'Y': ('int', pos[1]), 'Z': ('int', pos[2])})


def gen_boiler_ponder():
    """A kiln under one corner of a 2x2 boiler, a steam engine on the boiler's north side and its shaft. The block
    entity data joins the four tanks into one boiler with the engine attached, as Create's own steam engine scene
    does, so the boiler's gauge shows and the engine's shaft knows its engine."""
    b = {}
    plate(b)
    b[(3, 1, 3)] = kiln_state(chimney=False)
    controller = (3, 2, 3)
    for x in (3, 4):
        for z in (3, 4):
            pos = (x, 2, z)
            nbt = {'id': ('string', 'create:fluid_tank'), 'LastKnownPos': block_pos_nbt(pos), 'Luminosity': ('int', 0),
                   'Boiler': ('compound', {'Supply': ('float', 0.0), 'ActiveHeat': ('int', 0), 'PassiveHeat': ('byte', 0),
                                           'Engines': ('int', 1 if pos == controller else 0), 'Whistles': ('int', 0),
                                           'Update': ('byte', 0)})}
            if pos == controller:
                nbt.update({'Size': ('int', 2), 'Height': ('int', 1), 'Window': ('byte', 0),
                            'TankContent': ('compound', {'FluidName': ('string', 'minecraft:water'),
                                                         'Amount': ('int', 16000)})})
            else:
                nbt['Controller'] = block_pos_nbt(controller)
            b[pos] = ('create:fluid_tank', {'top': 'true', 'bottom': 'true', 'shape': 'plain'}, nbt)
    # an engine on a wall faces away from its tank, with its shaft two blocks out (Create's SteamEngineBlock)
    b[(3, 2, 2)] = ('create:steam_engine', {'face': 'wall', 'facing': 'north', 'waterlogged': 'false'},
                    {'id': ('string', 'create:steam_engine'), 'ScrollValue': ('int', 0)})
    b[(3, 2, 0)] = ('create:powered_shaft', {'axis': 'x', 'waterlogged': 'false'},
                    {'id': ('string', 'create:powered_shaft'), 'Direction': ('int', 1),
                     'EnginePos': block_pos_nbt((0, 0, -2)), 'EnginePower': ('float', 0.0),
                     'EngineType': ('string', 'create:steam_engine')})
    save_structure('kiln/boiler', (5, 4, 5), b)


def gen_basin_ponder():
    """A basin on a kiln, with a mixer over the basin turned by an encased cogwheel beside it (east, behind the mixer
    from the camera) on a creative motor; encased, the cog is its own support. The basin has no spout, so what
    it makes stays in it."""
    b = {}
    plate(b)
    b[(2, 1, 2)] = kiln_state(chimney=False)
    b[(2, 2, 2)] = ('create:basin', {'facing': 'down'})
    b[(2, 4, 2)] = ('create:mechanical_mixer', {})
    b[(3, 4, 2)] = ('create:andesite_encased_cogwheel', {'axis': 'y', 'top_shaft': 'true', 'bottom_shaft': 'false'})
    b[(3, 5, 2)] = ('create:creative_motor', {'facing': 'down'})
    save_structure('kiln/basin', (5, 6, 5), b)


def gen_crucible_ponder():
    """A crucible over a fed Blaze Burner, its lip to the west, turned by a creative motor through a shaft on its south
    side; a depot under the lip, and rough brass spilt beside it."""
    b = {}
    plate(b)
    b[(2, 1, 2)] = ('create:blaze_burner', {'blaze': 'kindled', 'facing': 'north'})
    b[(2, 2, 2)] = ('mcremix:crucible', {'facing': 'west', 'formed': 'false'})
    b[(2, 2, 3)] = ('create:shaft', {'axis': 'z', 'waterlogged': 'false'})
    b[(2, 2, 4)] = ('create:creative_motor', {'facing': 'north'})
    b[(1, 1, 2)] = ('create:depot', {})
    b[(1, 1, 1)] = ('mcremix:rough_brass', {'layers': 3})
    save_structure('crucible/intro', (5, 4, 5), b)


def lone_tank_nbt(pos):
    """An empty one-block fluid tank, its own controller."""
    return {'id': ('string', 'create:fluid_tank'), 'LastKnownPos': block_pos_nbt(pos), 'Luminosity': ('int', 0),
            'Size': ('int', 1), 'Height': ('int', 1), 'Window': ('byte', 1),
            'TankContent': ('compound', {'FluidName': ('string', 'minecraft:empty'), 'Amount': ('int', 0)}),
            'Boiler': ('compound', {'Supply': ('float', 0.0), 'ActiveHeat': ('int', 0), 'PassiveHeat': ('byte', 0),
                                    'Engines': ('int', 0), 'Whistles': ('int', 0), 'Update': ('byte', 0)})}


def gen_tar_ponder():
    """A Furnace with a mechanical pump drawing off its back into a fluid tank, the pump turned by a cogwheel behind it
    (north), held by a bracket bolted to the floor, on a creative motor. Then a basin on a fed Blaze Burner, under a
    mechanical press turned by a creative motor behind it (east)."""
    b = {}
    plate(b)
    b[(1, 1, 2)] = kiln_state()
    b[(2, 1, 2)] = ('create:mechanical_pump', {'facing': 'east', 'waterlogged': 'false'})
    b[(2, 1, 1)] = bracketed('create:cogwheel', 'create:metal_bracket')
    b[(3, 1, 1)] = ('create:creative_motor', {'facing': 'west'})
    b[(3, 1, 2)] = ('create:fluid_tank', {'top': 'true', 'bottom': 'true', 'shape': 'window'}, lone_tank_nbt((3, 1, 2)))
    save_structure('kiln/byproducts', (5, 3, 5), b)

    b = {}
    plate(b)
    b[(2, 1, 2)] = ('create:blaze_burner', {'blaze': 'kindled', 'facing': 'north'})
    b[(2, 2, 2)] = ('create:basin', {'facing': 'down'})
    b[(2, 4, 2)] = ('create:mechanical_press', {'facing': 'east'})
    b[(3, 4, 2)] = ('create:creative_motor', {'facing': 'west'})
    save_structure('kiln/bakelite', (5, 5, 5), b)


def gen_heat_sink_ponder():
    """A Power Grid electric motor with a heat sink on top, and a fan behind the sink (north) blowing through its fins
    toward the camera, on a creative motor."""
    b = {}
    plate(b)
    b[(2, 1, 2)] = ('powergrid:electric_motor', {'facing': 'east'})
    b[(2, 2, 2)] = ('mcremix:heat_sink', {'facing': 'down'})
    b[(2, 2, 1)] = ('create:encased_fan', {'facing': 'south'})
    b[(2, 2, 0)] = ('create:creative_motor', {'facing': 'south'})
    save_structure('heat_sink/intro', (5, 4, 5), b)


def gen_cooling_ponder():
    """Friction heat: a creative motor turning an aluminium gearbox through aluminium shafts, a heat sink on the
    gearbox, and behind it (north) two fans blowing toward the camera, one at the gearbox and one through the sink, each
    on a creative motor. Water cooling: an aluminium gearbox on a creative motor with a water cooler on top; a pump
    draws its warm water east into a panel of six radiators, three columns two high standing across x with a fan under
    each column on a creative motor set in the floor, and a second pump brings the cold water back over the top into
    the cooler. Each pump is turned by a cogwheel beside it on a creative motor."""
    b = {}
    plate(b)
    b[(0, 1, 2)] = ('create:creative_motor', {'facing': 'east'})
    for x in (1, 3, 4):
        b[(x, 1, 2)] = ('mcremix:aluminium_shaft', {'axis': 'x'})
    b[(2, 1, 2)] = ('mcremix:aluminium_gearbox', {'axis': 'y'})
    b[(2, 2, 2)] = ('mcremix:heat_sink', {'facing': 'down'})
    for y in (1, 2):
        b[(2, y, 1)] = ('create:encased_fan', {'facing': 'south'})
        b[(2, y, 0)] = ('create:creative_motor', {'facing': 'south'})
    save_structure('friction_heat/intro', (5, 4, 5), b)

    def pipe(*sides):
        return ('create:fluid_pipe', dict({d: 'true' if d in sides else 'false'
                                           for d in ('north', 'south', 'east', 'west', 'up', 'down')},
                                          waterlogged='false'))
    b = {}
    plate(b, 7)
    b[(0, 1, 3)] = ('create:creative_motor', {'facing': 'east'})
    b[(1, 1, 3)] = ('mcremix:aluminium_gearbox', {'axis': 'y'})
    b[(1, 2, 3)] = ('mcremix:water_cooler', {'facing': 'down'})
    # warm water out of the cooler's side and east into the panel
    b[(2, 2, 3)] = pipe('east', 'west')
    b[(3, 2, 3)] = ('create:mechanical_pump', {'facing': 'east', 'waterlogged': 'false'})
    b[(3, 2, 2)] = ('create:cogwheel', {'axis': 'x'})
    b[(2, 2, 2)] = ('create:creative_motor', {'facing': 'east'})
    b[(4, 2, 3)] = pipe('east', 'west')
    for y in (2, 3):
        for z in (2, 3, 4):
            b[(5, y, z)] = ('mcremix:radiator', {'axis': 'x'})
    for z in (2, 3, 4):
        b[(5, 1, z)] = ('create:encased_fan', {'facing': 'up'})
        b[(5, 0, z)] = ('create:creative_motor', {'facing': 'up'})
    # cold water back out of the panel's top row and over into the top of the cooler
    b[(4, 3, 3)] = ('create:mechanical_pump', {'facing': 'west', 'waterlogged': 'false'})
    b[(4, 3, 4)] = ('create:cogwheel', {'axis': 'x'})
    b[(3, 3, 4)] = ('create:creative_motor', {'facing': 'east'})
    b[(3, 3, 3)] = pipe('east', 'west')
    b[(2, 3, 3)] = pipe('east', 'west')
    b[(1, 3, 3)] = pipe('east', 'down')
    save_structure('water_cooling/intro', (7, 5, 7), b)


# ------------------------------------------------------------------------------------------------ fluids (stage 2)

# pipes come in brass, iron, glass, steel and aluminium; pumps and tanks in the four metals. Copper ones are Create's
# own. Brass pipes are thin, and iron rusts (FluidPartMaterial)
FLUID_MATERIALS = ['brass', 'iron', 'glass', 'steel', 'aluminium']
PUMP_MATERIALS = ['brass', 'iron', 'steel', 'aluminium']
# Power Grid's electric pump in each metal with a pump but brass, whose went in 0.26.0: the Brass Mechanical Pump sets its
# own flow instead (Taran)
ELECTRIC_PUMP_MATERIALS = [m for m in PUMP_MATERIALS if m != 'brass']
TANK_MATERIALS = ['brass', 'iron', 'steel', 'aluminium']
# iron's rust, a stage at a time, as its pipes, pumps and tanks are drawn (FluidPartModels.rust)
RUST_STAGES = ['exposed', 'weathered', 'rusted']
# the molten metals and how hot each is (FluidHeat); MoltenMetal.java holds the rest
MOLTEN = [('iron', 'blazing'), ('gold', 'searing'), ('copper', 'searing'), ('zinc', 'hot'), ('brass', 'searing'),
          ('steel', 'blazing'), ('netherite', 'blazing'), ('aluminium', 'hot')]
# Create's blocks that hold or carry fluid: copper, so rated Hot like this mod's glass pipe
CREATE_FLUID_BLOCKS = ['create:fluid_pipe', 'create:encased_fluid_pipe', 'create:glass_fluid_pipe',
                       'create:mechanical_pump', 'create:smart_fluid_pipe', 'create:fluid_valve', 'create:fluid_tank',
                       'create:spout', 'create:item_drain', 'powergrid:electric_pump']


def tank_nbt(pos, controller, width, height, fluid=None, amount=0, be='create:fluid_tank', window=True):
    """One block of a fluid tank, Create's or (by its block entity id {@code be}) one of this mod's: the controller
    holds the size, contents and whether it has windows, the others point to it."""
    nbt = {'id': ('string', be), 'LastKnownPos': block_pos_nbt(pos), 'Luminosity': ('int', 0)}
    if pos != controller:
        nbt['Controller'] = block_pos_nbt(controller)
        return nbt
    nbt.update({'Size': ('int', width), 'Height': ('int', height), 'Window': ('byte', 1 if window else 0),
                'TankContent': ('compound', {'FluidName': ('string', fluid or 'minecraft:empty'),
                                             'Amount': ('int', amount)}),
                'Boiler': ('compound', {'Supply': ('float', 0.0), 'ActiveHeat': ('int', 0), 'PassiveHeat': ('byte', 0),
                                        'Engines': ('int', 0), 'Whistles': ('int', 0), 'Update': ('byte', 0)})})
    return nbt


def gen_electrolysis_ponder():
    """An Electrolyser on the west side of a basin (toward the camera), still without its electrodes, a creative
    voltage source north of it to wire it to and a heat sink on top of it. Then a vat: a 2x2 tank two high on a layer of andesite scaffolding, its windows shut as a
    standing vat's are, two Electrolysers on its west side (toward the camera) with a creative voltage source below
    them, a chest beside the scaffolding and two pipes on top."""
    b = {}
    plate(b)
    b[(2, 1, 2)] = ('create:basin', {'facing': 'down'})
    b[(1, 1, 2)] = ('mcremix:electrolyser', {'facing': 'east', 'anode': 'false', 'cathode': 'false'})
    b[(1, 1, 0)] = ('powergrid:creative_voltage_source', {'facing': 'west'})
    b[(1, 2, 2)] = ('mcremix:heat_sink', {'facing': 'down'})
    save_structure('electrolysis/basin', (5, 4, 5), b)

    b = {}
    plate(b)
    controller = (1, 2, 1)
    for x in (1, 2):
        for z in (1, 2):
            b[(x, 1, z)] = ('create:andesite_scaffolding', {'distance': 0, 'bottom': 'false', 'waterlogged': 'false'})
            for y in (2, 3):
                b[(x, y, z)] = ('create:fluid_tank', {'top': 'true' if y == 3 else 'false',
                                                      'bottom': 'true' if y == 2 else 'false', 'shape': 'plain'},
                                tank_nbt((x, y, z), controller, 2, 2, window=False))
    # facing east: their electrodes reach east into the tank
    for z in (1, 2):
        b[(0, 3, z)] = ('mcremix:electrolyser', {'facing': 'east', 'anode': 'true', 'cathode': 'true'})
    b[(0, 1, 0)] = ('powergrid:creative_voltage_source', {'facing': 'west'})
    b[(0, 1, 2)] = ('minecraft:chest', {'facing': 'west', 'type': 'single', 'waterlogged': 'false'})
    # pulling out what the vat makes (0.28.0)
    b[(0, 1, 1)] = ('create:andesite_funnel', {'facing': 'west', 'extracting': 'true', 'powered': 'false',
                                               'waterlogged': 'false'})
    pipe = {'north': 'false', 'south': 'false', 'east': 'false', 'west': 'false', 'up': 'true', 'down': 'true',
            'waterlogged': 'false'}
    b[(1, 4, 1)] = ('create:fluid_pipe', pipe)
    b[(2, 4, 2)] = ('create:fluid_pipe', pipe)
    save_structure('electrolysis/vat', (5, 5, 5), b)


def gen_stage4_ponder():
    """A Furnace with planks in it, a spark plug on its north side (toward the camera) and a creative voltage source to
    wire it to. A Furnace (cased in steel in the scene) on a pipe that turns up into it from a pump behind it, which
    draws from a tank; the pump turned by a bracketed cogwheel on its south side, on a creative motor. A vat of four
    copper tanks two high on andesite scaffolding, its windows shut as a standing vat's are, a copper mixer in its vat
    form on its north side, turned through its cog by a bracketed cogwheel beside it on a creative motor. A Pressurizer over a Blaze Burner on a stand, geared up from a creative motor through a
    steel gearbox and a steel large cogwheel to the small steel cogwheel on its shaft at its west side, the gearbox and
    motor on stands of their own so the large cogwheel clears the floor, and a pipe to its vessel's west side."""
    b = {}
    plate(b)
    b[(2, 1, 2)] = kiln_state()
    b[(2, 1, 1)] = ('mcremix:spark_plug', {'facing': 'south'})
    b[(4, 1, 1)] = ('powergrid:creative_voltage_source', {'facing': 'west'})
    save_structure('spark_plug/intro', (5, 3, 5), b)

    b = {}
    plate(b)
    b[(2, 2, 2)] = kiln_state()
    b[(2, 1, 2)] = ('create:fluid_pipe', {'north': 'false', 'south': 'false', 'east': 'true', 'west': 'false',
                                          'up': 'true', 'down': 'false', 'waterlogged': 'false'})
    b[(3, 1, 2)] = ('create:mechanical_pump', {'facing': 'west', 'waterlogged': 'false'})
    b[(3, 1, 3)] = bracketed('create:cogwheel', 'create:metal_bracket')
    b[(4, 1, 3)] = ('create:creative_motor', {'facing': 'west'})
    b[(4, 1, 2)] = ('create:fluid_tank', {'top': 'true', 'bottom': 'true', 'shape': 'window'}, lone_tank_nbt((4, 1, 2)))
    save_structure('steel_furnace/intro', (5, 4, 5), b)

    b = {}
    plate(b)
    controller = (2, 2, 2)
    for x in (2, 3):
        for z in (2, 3):
            b[(x, 1, z)] = ('create:andesite_scaffolding', {'distance': 0, 'bottom': 'false', 'waterlogged': 'false'})
            for y in (2, 3):
                b[(x, y, z)] = ('create:fluid_tank', {'top': 'true' if y == 3 else 'false',
                                                      'bottom': 'true' if y == 2 else 'false', 'shape': 'plain'},
                                tank_nbt((x, y, z), controller, 2, 2, window=False))
    # the mixer faces the vat; a cog beside it turns its cog, bracketed to a casing under it and driven by a motor on
    # another
    b[(2, 2, 1)] = ('mcremix:copper_vat_mixer', {'facing': 'south', 'patina': 'none'})
    b[(1, 2, 1)] = bracketed('create:cogwheel', 'create:metal_bracket', axis='z')
    b[(1, 2, 0)] = ('create:creative_motor', {'facing': 'south'})
    b[(1, 1, 1)] = ('create:andesite_casing', {})
    b[(1, 1, 0)] = ('create:andesite_casing', {})
    save_structure('vat/mixer', (5, 4, 5), b)

    # the Pressurizer faces north, its dial to the camera, over a Blaze Burner; its shaft is driven from its west side
    # by a small cog, which a large cog behind it turns at twice a creative motor's speed through a gearbox; a pipe
    # comes in at its vessel's west side (the scene puts out the vessel's nozzle to it as it shows the pipe)
    b = {}
    plate(b)
    for stand in ((2, 1, 2), (0, 1, 3), (0, 1, 4)):
        b[stand] = ('create:industrial_iron_block', {})
    b[(2, 2, 2)] = ('create:blaze_burner', {'blaze': 'kindled', 'facing': 'north'})
    b[(2, 3, 2)] = ('mcremix:pressurizer', {'facing': 'north', 'vessel': 'true'})
    b[(2, 4, 2)] = ('mcremix:pressurizer_vessel', {d: 'false' for d in ('north', 'east', 'south', 'west')})
    b[(1, 3, 2)] = ('mcremix:steel_cogwheel', {'axis': 'x'})
    b[(1, 2, 3)] = ('mcremix:steel_large_cogwheel', {'axis': 'x'})
    b[(0, 2, 3)] = ('mcremix:steel_gearbox', {'axis': 'y'})
    b[(0, 2, 4)] = ('create:creative_motor', {'facing': 'north'})
    for x in (0, 1):
        b[(x, 4, 2)] = ('create:fluid_pipe', dict({d: 'true' if d in ('east', 'west') else 'false'
                                                   for d in ('north', 'south', 'east', 'west', 'up', 'down')},
                                                  waterlogged='false'))
    save_structure('pressurizer/intro', (5, 5, 5), b)


def pipe_name(m):
    return 'glass_pipe' if m == 'glass' else f'{m}_fluid_pipe'


def pump_name(m):
    return f'{m}_mechanical_pump'


def electric_pump_name(m):
    return f'{m}_electric_pump'


def tank_name(m):
    return f'{m}_fluid_tank'


def fluid_blocks(material):
    """Every pipe, pump (mechanical and electric) and tank of one material."""
    names = [pipe_name(material)]
    if material in PUMP_MATERIALS:
        names.append(pump_name(material))
    if material in ELECTRIC_PUMP_MATERIALS:
        names.append(electric_pump_name(material))
    if material in TANK_MATERIALS:
        names.append(tank_name(material))
    return [f'mcremix:{n}' for n in names]


def fluid_pickaxe_blocks():
    """The metal pipes, pumps and tanks and the rest of the fluid blocks a pickaxe mines; glass breaks by hand."""
    metal = [b for m in FLUID_MATERIALS if m != 'glass' for b in fluid_blocks(m)]
    metal += [f'mcremix:{engine_name(m)}' for m in ENGINE_MATERIALS]
    return (metal + ['mcremix:steel_block', 'mcremix:aluminium_block', 'mcremix:crucible']
            + [f'mcremix:rough_{m}' for m, _ in MOLTEN] + PVC_BLOCKS)


# PVC pipes and the corner pieces between them
PVC_BLOCKS = ['mcremix:pvc_pipe', 'mcremix:pvc_pipe_corner']



# Create's copper fluid textures, stored as palettes and rows of palette indices (tools/create_fluid_textures.json),
# so pipes, pumps and tanks in other materials are Create's own drawing in other colours
CREATE_FLUID_TEXTURES = json.load(open(os.path.join(ROOT, 'tools', 'create_fluid_textures.json')))
INDEX_CHARS = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'
# Create's copper, darkest to lightest; the shadows inside its pipe ends, darkest last; and vanilla's glass, in the
# tank windows, also darkest to lightest
COPPER_RAMP = [(144, 73, 49), (154, 80, 56), (167, 90, 64), (178, 98, 71), (194, 107, 76), (200, 116, 86),
               (214, 123, 91), (227, 130, 108)]
COPPER_SHADOWS = [(121, 59, 43), (91, 41, 36), (61, 22, 30)]
GLASS = [(123, 174, 183), (139, 193, 205), (168, 208, 217), (208, 234, 233)]
# a tank's sprites; the last is the vat sheet (vat_sheet), drawn here in Create's copper and taken with the rest
TANK_SPRITES = ['', '_top', '_inner', '_window', '_window_single', '_connected', '_top_connected', '_inner_connected',
                '_vat']
# every copper texture of Create's pipes, glass pipe, pump and tank; each weathers (FluidPartModels.COPPER_SPRITES)
COPPER_SPRITES = ['pipes', 'pipes_connected', 'glass_fluid_pipe', 'pump'] + ['fluid_tank' + s for s in TANK_SPRITES]
# Create's andesite and brass machine textures, stored the same way (tools/create_machine_textures.json), which every
# machine tier takes in its own colours
CREATE_MACHINE_TEXTURES = json.load(open(os.path.join(ROOT, 'tools', 'create_machine_textures.json')))
# Power Grid's electric pump texture, stored the same way (tools/powergrid_textures.json), in Create's copper
POWERGRID_TEXTURES = json.load(open(os.path.join(ROOT, 'tools', 'powergrid_textures.json')))


def create_texture(name):
    if name in OWN_COPPER_TEXTURES:
        return OWN_COPPER_TEXTURES[name]()
    t = next(textures[name] for textures in (CREATE_FLUID_TEXTURES, CREATE_MACHINE_TEXTURES, POWERGRID_TEXTURES)
             if name in textures)
    return [[tuple(t['palette'][INDEX_CHARS.index(c)]) for c in row] for row in t['rows']]


def vat_sheet():
    """What a standing vat adds to its tanks, drawn in Create's copper so every tank metal, patina and rust takes it
    in its own colours (VatParts): the manway door's plate in the top left, a disc ten pixels across with its rows as
    wide as the model's, a raised rim lit from the top left and a bolt between each arm of the handle; the legs' strip
    beside it, light to dark across; the handle's hub, a round boss with a nut, under the plate; and down the right the
    single colours of the hinges and flange, the plate's edge, braces and outlet, the lever, the handle and hub, the
    outlet's mouth, the feet and the gasket round the door, each two pixels high so it holds at a distance. The rest
    is dark, which the plate's edge fades into."""
    r, s = COPPER_RAMP, COPPER_SHADOWS
    px = [[r[1]] * 16 for _ in range(16)]

    def inside(x, y):
        if not (0 <= y < 10):
            return False
        across = y + .5 - 5
        half = int(math.floor(math.sqrt(25 - across * across) + .5))  # VatParts.halfWidth, rounding halves up
        return 5 - half <= x < 5 + half
    for y in range(10):
        for x in range(10):
            if not inside(x, y):
                continue
            light = (x + .5 - 5) + (y + .5 - 5)
            if not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[y][x] = r[7] if light < 0 else r[1]
            else:
                px[y][x] = r[5] if light < -3 else r[3] if light > 3 else r[4]
    for x, y in ((2, 2), (7, 2), (2, 7), (7, 7)):
        px[y][x], px[y + 1][x] = r[7], s[0]
    hub = ((4, 7, 7, 4), (7, 6, 5, 5), (7, 5, 3, 3), (4, 4, 3, 2))
    for y, row in enumerate(hub):
        for x, i in enumerate(row):
            px[10 + y][x] = r[i]
    for y in range(16):
        px[y][10:13] = [r[6], r[4], r[2]]
        px[y][13:16] = [(r[1], r[3], r[5], r[7], s[2], r[0], s[1], r[0])[y // 2]] * 3
    return [[c + (255,) for c in row] for row in px]


# copper textures drawn here, which take other metals' colours as Create's own do
OWN_COPPER_TEXTURES = {'fluid_tank_vat': vat_sheet}


def ramp(palette, t):
    """A point along a palette from its dark through its base to its light."""
    base, dark, light = palette[:3]
    t = max(0.0, min(1.0, t))
    return mix(dark, base, t * 2) if t < 0.5 else mix(base, light, t * 2 - 1)


def recolour(name, material, seed, greys=True):
    """Create's copper texture `name` in another metal, glass, a weathered copper or a rusting iron: each copper shade
    moves to the same place along the material's palette, and the shadows in the pipe ends darken its dark. Window
    glass, black and anything else stays as it is, and so does the pump's grey cog in weathered copper; in the other
    metals the cog is the metal's own, darker, unless `greys` is off (the electric pump's grey motor). A palette's
    specks (rust on iron) dot it. Glass pipes are clear, with pale glass where the copper's edges and glints are, so
    the fluid shows through."""
    rows = create_texture(name)
    rnd = random.Random(seed)
    img = Image.new('RGBA', (len(rows[0]), len(rows)), (0, 0, 0, 0))
    palette = None if material == 'glass' else PALETTES[material]
    for y, row in enumerate(rows):
        for x, px in enumerate(row):
            rgb, a = px[:3], px[3]
            col = px
            if a and rgb in COPPER_RAMP:
                i = COPPER_RAMP.index(rgb)
                if palette is None:
                    col = (GLASS[i + 1] + (255,)) if i < 3 else (GLASS[3] + (255,)) if i == 7 else (0, 0, 0, 0)
                else:
                    t = i / (len(COPPER_RAMP) - 1)
                    c = ramp(palette, t)
                    if palette[3] and rnd.random() < 0.1:
                        c = mix(c, palette[3], 0.6)
                    col = c + (255,)
            elif a and rgb in COPPER_SHADOWS:
                k = COPPER_SHADOWS.index(rgb)
                if palette is None:
                    col = (GLASS[0] + (255,)) if k == 0 else (0, 0, 0, 0)
                else:
                    col = mix((0, 0, 0), palette[1], (0.82, 0.6, 0.4)[k]) + (255,)
            elif a and greys and rgb not in GLASS and max(rgb) - min(rgb) < 20 and max(rgb) > 40 \
                    and palette is not None and 'copper' not in material:
                t = (sum(rgb) / 3 - 90) / 90
                col = mix(mix((0, 0, 0), palette[1], 0.7), mix(palette[1], palette[0], 0.5), max(0.0, min(1.0, t))) \
                    + (255,)
            img.putpixel((x, y), col)
    return img


def bracket_texture(kind, stage, seed):
    """Create's bracket ('bracket') or its plate ('bracket_plate') in a wood, a metal or a weathering stage: each shade of
    the wood in Create's wooden bracket, or of the metal in its metal one, moves to the same place along the material's
    palette; the bolts on wooden brackets, and the black of the hole, stay as they are. Wood gets a little grain and
    weathered metal its specks."""
    wood = stage in WOODS
    rows = create_texture(f'{kind}_{"wooden" if wood else "metal"}')
    palette = PALETTES[stage]
    rnd = random.Random(seed)

    def is_material(rgb):
        return max(rgb) - min(rgb) > 20 if wood else max(rgb) > 40 and max(rgb) - min(rgb) < 20

    shades = [sum(px[:3]) for row in rows for px in row if px[3] and is_material(px[:3])]
    low, high = min(shades), max(shades)
    img = Image.new('RGBA', (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, px in enumerate(row):
            col = px
            if px[3] and is_material(px[:3]):
                t = 0.1 + 0.85 * (sum(px[:3]) - low) / max(1, high - low)
                if wood:
                    t += rnd.uniform(-0.05, 0.05) - (0.1 if (x + y // 3) % 7 == 0 else 0)
                c = ramp(palette, t)
                if palette[3] and rnd.random() < 0.12:
                    c = mix(c, palette[3], 0.6)
                col = c + (255,)
            img.putpixel((x, y), col)
    return img


# polished brass, darkest to brightest, which brass shafts, cogs, gearboxes, chain drives and brackets are drawn in:
# shinier than the brass palette's three shades, as a clock's works are kept (Taran, 0.23.0). Drawn clean since 0.26.0
# (Taran): smooth bevels and plain polished plates, no engraving, rosettes or rubies
BRASS_SHINE = [(0x4A, 0x2A, 0x08), (0x7A, 0x4C, 0x12), (0xA6, 0x70, 0x22), (0xCC, 0x98, 0x38), (0xE6, 0xB8, 0x52),
               (0xF6, 0xD8, 0x7E), (0xFF, 0xF2, 0xC0)]


def along(colours, t):
    """A point along a list of colours, darkest first."""
    t = max(0.0, min(1.0, t)) * (len(colours) - 1)
    i = min(int(t), len(colours) - 2)
    return mix(colours[i], colours[i + 1], t - i)


def shine(t):
    return along(BRASS_SHINE, t)


def polish(x, y):
    """Polished brass's soft fall of light across a face, brighter toward its upper left."""
    return 0.06 * (1 - (x + y) / 15)


def brass_bushing(img):
    """A plain polished brass bushing with the arbor's hole through it, where a shaft comes out of a face: a rounded
    square ring lit on its upper left and shaded on its lower right, round a hole four pixels across, with a fine
    dark line round it."""
    for y in range(16):
        for x in range(16):
            ax, ay = abs(x - 7.5), abs(y - 7.5)
            inside = lambda r: ax < r and ay < r and not (ax > r - 1 and ay > r - 1)
            if inside(2):
                col = (0x1A, 0x10, 0x06)
            elif inside(3):
                col = shine(0.82 if x + y < 15 else 0.46)
            elif inside(4):
                col = shine(0.24)
            else:
                continue
            img.putpixel((x, y), col + (255,))


def brass_casing_texture(seed, hole=False, edges='tblr'):
    """A brass gearbox's or chain drive's casing: a smooth polished bevel, bright where the light falls on it, round a
    plain polished plate with one fine line inset in it. A face a shaft comes out of (hole) has a bushing for it.
    edges names the sides that have a frame, as casing_texture's."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    top, bottom, left, right = ('t' in edges), ('b' in edges), ('l' in edges), ('r' in edges)
    x0, x1, y0, y1 = (3 if left else -1), (12 if right else 16), (3 if top else -1), (12 if bottom else 16)
    for y in range(16):
        for x in range(16):
            outer = (left and x == 0) or (right and x == 15) or (top and y == 0) or (bottom and y == 15)
            inner = not outer and ((left and x == 1) or (right and x == 14) or (top and y == 1) or (bottom and y == 14))
            if outer or inner:
                lit = (top and y == (0 if outer else 1)) or (left and x == (0 if outer else 1))
                t = (0.86 if lit else 0.2) if outer else (0.68 if lit else 0.38)
            else:
                t = 0.55 + polish(x, y) + rnd.uniform(-0.012, 0.012)
                if (left and x == 2) or (top and y == 2):
                    t -= 0.12  # just below the bevel, in its shadow
                # the fine inset line, its lower and right edges catching the light
                if (x in (x0, x1) and y0 <= y <= y1) or (y in (y0, y1) and x0 <= x <= x1):
                    t -= 0.16
                elif (x - 1 in (x0, x1) and y0 <= y <= y1) or (y - 1 in (y0, y1) and x0 <= x <= x1):
                    t += 0.07
            img.putpixel((x, y), shine(t) + (255,))
    if hole:
        brass_bushing(img)
    return img


def brass_chain_run(img, x0, x1, top=5):
    """A fine polished chain, as a fusee's, in a channel six pixels deep: flat links three pixels long, joined by
    pins, drawn softly so it reads as a chain without striping the drive. It repeats every three pixels and runs on
    unbroken across 48 pixels, the next face and the next drive."""
    for x in range(x0, x1):
        p = x % 3
        for row in range(6):
            if row == 0:
                t = 0.18  # in the shadow of the lip above
            elif row == 5:
                t = 0.3
            elif row == 2:
                t = (0.8, 0.7, 0.5)[p]
            elif row == 3:
                t = (0.6, 0.52, 0.42)[p]
            else:
                t = 0.42 if p == 1 else 0.26
            img.putpixel((x, top + row), shine(t) + (255,))


def brass_chain_drive_texture(seed, kind):
    """A brass chain drive's faces, as chain_drive_texture's: the chain running round in its channel, and bushings
    where the shaft comes out."""
    img = brass_casing_texture(seed, edges='tbl' if kind == 'end' else 'tb')
    brass_chain_run(img, 8 if kind == 'end' else 0, 16)
    if kind in ('end', 'middle'):
        brass_bushing(img)
    return img


def brass_clockwork_texture(seed):
    """The polished brass of the clockwork shafts, cogs and brackets. Columns 0-2 are the arbor, lit on one side and
    shaded on the other so the slender rod reads as round; 3-6 by 0-3 is a hub's face, a polished boss round the
    arbor; 3-6 by 4-7 a hub's banded side; the rest is smooth polished brass for rims, spokes, teeth and bridges."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shine(0.6 + polish(x, y) + rnd.uniform(-0.015, 0.015)) + (255,))
    for y in range(16):
        for x, t in ((0, 0.92), (1, 0.64), (2, 0.32)):
            img.putpixel((x, y), shine(t + rnd.uniform(-0.01, 0.01)) + (255,))
    for y in range(4):
        for x in range(3, 7):
            img.putpixel((x, y), shine(0.34 if x in (3, 6) or y in (0, 3) else 0.78) + (255,))
    for y in range(4, 8):
        for x in range(3, 7):
            img.putpixel((x, y), shine((0.3, 0.88, 0.66, 0.32)[y - 4]) + (255,))
    return img


# rough metal: base, dark, light. Iron and steel set grey, zinc a little green as Create's is, netherite near black
ROUGH_METAL = {
    'iron': ((0xA8, 0xA8, 0xA8), (0x5E, 0x5E, 0x60), (0xDA, 0xDA, 0xDA)),
    'gold': ((0xE6, 0xB4, 0x2E), (0x96, 0x66, 0x12), (0xFF, 0xE6, 0x78)),
    'copper': ((0xC6, 0x6A, 0x40), (0x7A, 0x38, 0x22), (0xEC, 0x9A, 0x6E)),
    'zinc': ((0xA6, 0xB0, 0xA6), (0x60, 0x6A, 0x64), (0xD0, 0xDA, 0xD2)),
    'brass': ((0xD2, 0xA2, 0x4A), (0x80, 0x58, 0x20), (0xF2, 0xD2, 0x82)),
    'steel': PALETTES['steel'][:3],
    'netherite': ((0x4C, 0x42, 0x46), (0x24, 0x1E, 0x22), (0x74, 0x68, 0x6C)),
    'aluminium': PALETTES['aluminium'][:3],
}


def metal_block_texture(metal, seed):
    """Block of Steel (or of Aluminium, or Titanium): two forged plates one above the other, brushed across, each lit
    along its top and left edges and shadowed along the others. Polished titanium has a sheen across each plate."""
    palette = PALETTES[metal]
    rnd = random.Random(seed)
    streak = [rnd.uniform(-0.1, 0.1) for _ in range(16)]
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            gleam = 0.3 * math.exp(-((x + y % 8 * 2 - 9) / 2.4) ** 2) if metal == 'titanium' else 0
            col = ramp(palette, 0.55 + streak[y] + rnd.uniform(-0.05, 0.05) + gleam)
            if y % 8 == 0 or x == 0:
                col = mix(col, palette[2], 0.45)
            elif y % 8 == 7 or x == 15:
                col = mix(col, palette[1], 0.6)
            elif x in (2, 13) and y % 8 == 3:
                col = mix(col, palette[2], 0.7)  # a rivet head, lit
            elif x in (2, 13) and y % 8 == 4:
                col = mix(col, palette[1], 0.7)  # and its shadow
            img.putpixel((x, y), col + (255,))
    return img


# a model drawn facing down, turned to each facing: x 90 turns its base to the south, then y turns it round
# (DownModelShapes.turn)
DOWN_TURNS = {'down': {}, 'up': {'x': 180}, 'south': {'x': 90}, 'west': {'x': 90, 'y': 90},
              'north': {'x': 90, 'y': 180}, 'east': {'x': 90, 'y': 270}}


def heat_sink_fin_texture(seed):
    """Extruded aluminium fins, ribbed along their length as extrusions are: rows 0 to 5 are a fin's side, lit along
    its rounded edge at the top, then two ribs, each lit over a groove, darkening down to the root where it meets the
    base; column 14 is a fin's sawn end and column 15 its edge, seen from above. The sides repeat on down for the
    particles."""
    palette = PALETTES['aluminium']
    rnd = random.Random(seed)
    depth = (0.96, 0.7, 0.46, 0.62, 0.36, 0.16)
    streak = [rnd.uniform(-0.04, 0.04) for _ in range(16)]
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            n = rnd.uniform(-0.03, 0.03)
            if x == 15:
                t = 0.88 + n
            elif x == 14:
                t = (0.98, 0.8, 0.72, 0.64, 0.52, 0.3)[y % 6] + n
            else:
                t = depth[y % 6] + streak[x] + n
            img.putpixel((x, y), ramp(palette, t) + (255,))
    return img


def heat_sink_base_texture(seed):
    """The Heat Sink's base plate, drawn as its top (columns 2 to 13, rows 2 to 13): a ledge two pixels deep in front
    of the fins and behind them, each with a pair of bolt heads, and between them the shadowed floor of the
    extrusion, seen only in the channels between the fins. Rows 14 and 15 are its edge: a lit chamfer over a grey
    thermal pad where it is bolted to what it cools."""
    palette = PALETTES['aluminium']
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            n = rnd.uniform(-0.03, 0.03)
            if y == 14:
                col = ramp(palette, 0.82 + n)
            elif y == 15:
                col = mix((0x4E, 0x54, 0x5E), (0x6A, 0x70, 0x7A), 0.5 + n * 4)
            elif y in (2, 3, 12, 13):
                col = ramp(palette, (0.74 if y in (2, 12) else 0.58) + n)
            else:
                col = ramp(palette, 0.26 + n)
            img.putpixel((x, y), col + (255,))
    # the bolts: steel heads, a glint on the upper left
    steel = PALETTES['steel']
    for bx, by in ((3, 2), (11, 2), (3, 12), (11, 12)):
        for (dx, dy), col in (((0, 0), palette[2]), ((1, 0), ramp(steel, 0.8)), ((0, 1), ramp(steel, 0.7)),
                              ((1, 1), ramp(steel, 0.25))):
            img.putpixel((bx + dx, by + dy), col + (255,))
    return img


# the Heat Sink's six fins, a pixel thick and a pixel apart across its base, half a pixel off the grid so it is centred
HEAT_SINK_FINS = [2.5 + 2 * i for i in range(6)]


def heat_sink_model():
    """Drawn bolted to the block below: a base plate twelve pixels square and two thick, and six fins standing six
    pixels out of it between a bolted ledge at the front and one at the back, each fin a pixel thick and a pixel from
    the next. HeatSinkBlock's shape is the same boxes."""
    b, f = '#base', '#fin'
    elements = [box([2, 0, 2], [14, 2, 14], dict(
        {'down': {'uv': [2, 2, 14, 14], 'texture': b, 'cullface': 'down'}, 'up': {'uv': [2, 2, 14, 14], 'texture': b}},
        **{side: {'uv': [2, 14, 14, 16], 'texture': b} for side in ('north', 'south', 'east', 'west')}))]
    for x in HEAT_SINK_FINS:
        elements.append(box([x, 2, 4], [x + 1, 8, 12], dict(
            {'up': {'uv': [15, 4, 16, 12], 'texture': f}},
            **{side: {'uv': [4, 0, 12, 6], 'texture': f} for side in ('east', 'west')},
            **{side: {'uv': [14, 0, 15, 6], 'texture': f} for side in ('north', 'south')})))
    return {'parent': 'minecraft:block/block',
            'textures': {'base': 'mcremix:block/heat_sink_base', 'fin': 'mcremix:block/heat_sink_fin',
                         'particle': 'mcremix:block/heat_sink_fin'},
            'elements': elements}


def gen_heat_sink():
    save_png(heat_sink_fin_texture(1250), asset('textures', 'block', 'heat_sink_fin.png'))
    save_png(heat_sink_base_texture(1251), asset('textures', 'block', 'heat_sink_base.png'))
    write_json(asset('models', 'block', 'heat_sink.json'), heat_sink_model())
    write_json(asset('models', 'item', 'heat_sink.json'), {'parent': 'mcremix:block/heat_sink'})
    write_json(asset('blockstates', 'heat_sink.json'), {'variants': {
        f'facing={facing}': dict({'model': 'mcremix:block/heat_sink'}, **rot) for facing, rot in DOWN_TURNS.items()}})
    write_json(data('mcremix', 'loot_tables', 'blocks', 'heat_sink.json'), loot_self('heat_sink'))
    # four sheets for fins on a base of three ingots
    recipe('heat_sink', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['S S', 'S S', 'III'],
        'key': {'S': {'tag': 'forge:plates/aluminium'}, 'I': {'tag': 'forge:ingots/aluminium'}},
        'result': item('mcremix:heat_sink')})


def heat_sink_lang():
    return {
        'block.mcremix.heat_sink': 'Heat Sink',
        'block.mcremix.heat_sink.tooltip.summary': (
            'Finned aluminium. Bolted onto a _Power Grid_ device or an _aluminium_ or _titanium support_, it '
            'draws out the heat and sheds it, so the device takes _more power_ and the support turns _faster_ '
            'before either overheats. A _fan_ blowing through its fins makes it shed _far more_. Past _660 °C_ it '
            '_melts_, as aluminium does: titanium turning fast needs a _water cooler_.'),
        'block.mcremix.heat_sink.tooltip.condition1': 'When placed',
        'block.mcremix.heat_sink.tooltip.behaviour1': (
            'Its base goes against the block you place it on. Put _several_ on one block for more cooling.'),
        'mcremix.goggles.heat_sink.temperature': 'Temperature: %s °C',
        'mcremix.goggles.heat_sink.loose': 'Not bolted to anything that heats up',
    }


# ------------------------------------------------------------------------------------------------ red mud bricks

# red mud fired into bricks is vanilla's mud bricks tinted red: the models draw vanilla's own mud brick texture with a
# tint on every face, which McRemixClient.RED_MUD_TINT gives (multiplied in, as grass is coloured), so a resource
# pack's mud bricks come out red too. The brick item is in the same colours: outline, light, top, front, shadow
RED_MUD_BRICK_ITEM = ((0x2E, 0x14, 0x0E), (0xA6, 0x52, 0x38), (0x8A, 0x40, 0x2A), (0x78, 0x36, 0x24), (0x52, 0x26, 0x1A))


def tint_face(f, uv=None, cull=None):
    """A face of vanilla's block model templates, tinted (tintindex 0)."""
    out = {'texture': f}
    if uv:
        out['uv'] = uv
    if cull:
        out['cullface'] = cull
    out['tintindex'] = 0
    return out


def tinted_templates():
    """Vanilla's cube, stairs, slab and wall templates (minecraft:block/...), every face tinted. A model with one of
    them as its parent and these elements of its own draws them in place of the parent's, keeping its display."""
    t = tint_face
    lower = box([0, 0, 0], [16, 8, 16], {
        'down': t('#bottom', [0, 0, 16, 16], 'down'), 'up': t('#top', [0, 0, 16, 16]),
        **{d: t('#side', [0, 8, 16, 16], d) for d in ('north', 'south', 'west', 'east')}})
    upper_east = box([8, 8, 0], [16, 16, 16], {
        'up': t('#top', [8, 0, 16, 16], 'up'), 'north': t('#side', [0, 0, 8, 8], 'north'),
        'south': t('#side', [8, 0, 16, 8], 'south'), 'west': t('#side', [0, 0, 16, 8]),
        'east': t('#side', [0, 0, 16, 8], 'east')})
    cube = lambda mirrored: [box([0, 0, 0], [16, 16, 16], {
        d: t('#' + d, [16, 0, 0, 16] if mirrored and d in ('north', 'west') else None, d)
        for d in ('down', 'up', 'north', 'south', 'west', 'east')})]
    return {
        'cube_all': cube(False),
        'cube_north_west_mirrored_all': cube(True),
        'stairs': [lower, upper_east],
        'inner_stairs': [lower, upper_east, box([0, 8, 8], [8, 16, 16], {
            'up': t('#top', [0, 8, 8, 16], 'up'), 'north': t('#side', [8, 0, 16, 8]),
            'south': t('#side', [0, 0, 8, 8], 'south'), 'west': t('#side', [8, 0, 16, 8], 'west')})],
        'outer_stairs': [lower, box([8, 8, 8], [16, 16, 16], {
            'up': t('#top', [8, 8, 16, 16], 'up'), 'north': t('#side', [0, 0, 8, 8]),
            'south': t('#side', [8, 0, 16, 8], 'south'), 'west': t('#side', [8, 0, 16, 8]),
            'east': t('#side', [0, 0, 8, 8], 'east')})],
        'slab': [lower],
        'slab_top': [box([0, 8, 0], [16, 16, 16], {
            'down': t('#bottom', [0, 0, 16, 16]), 'up': t('#top', [0, 0, 16, 16], 'up'),
            **{d: t('#side', [0, 0, 16, 8], d) for d in ('north', 'south', 'west', 'east')}})],
        'template_wall_post': [box([4, 0, 4], [12, 16, 12], {
            'down': t('#wall', cull='down'), 'up': t('#wall', cull='up'),
            **{d: t('#wall') for d in ('north', 'south', 'west', 'east')}})],
        'template_wall_side': [box([5, 0, 0], [11, 14, 8], {
            'down': t('#wall', cull='down'), 'up': t('#wall'), 'north': t('#wall', cull='north'),
            'west': t('#wall'), 'east': t('#wall')})],
        'template_wall_side_tall': [box([5, 0, 0], [11, 16, 8], {
            'down': t('#wall', cull='down'), 'up': t('#wall', cull='up'), 'north': t('#wall', cull='north'),
            'west': t('#wall'), 'east': t('#wall')})],
        'wall_inventory': [
            box([4, 0, 4], [12, 16, 12], {
                'down': t('#wall', [4, 4, 12, 12], 'down'), 'up': t('#wall', [4, 4, 12, 12]),
                **{d: t('#wall', [4, 0, 12, 16]) for d in ('north', 'south', 'west', 'east')}}),
            box([5, 0, 0], [11, 13, 16], {
                'down': t('#wall', [5, 0, 11, 16], 'down'), 'up': t('#wall', [5, 0, 11, 16]),
                'north': t('#wall', [5, 3, 11, 16], 'north'), 'south': t('#wall', [5, 3, 11, 16], 'south'),
                'west': t('#wall', [0, 3, 16, 16]), 'east': t('#wall', [0, 3, 16, 16])})],
    }


def mud_ball_texture(seed, dark, base, light, shine):
    """A ball of wet mud: a soft blob sagging a little below its middle, lit from the upper left, with a glossy wet
    highlight."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx, cy = 7.5, 8.0
    radius = [5.6 + rnd.uniform(-0.5, 0.5) for _ in range(12)]
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx / (1.0 + max(0.0, dy) * 0.04), dy * 1.1)
            a = int(((math.atan2(dy, dx) + math.pi) / (2 * math.pi)) * 12) % 12
            if r > radius[a]:
                continue
            t = 0.62 - dy / 11.0 - dx / 16.0 + rnd.uniform(-0.07, 0.07)
            col = (mix(dark, base, max(0.0, min(1.0, t * 1.6))) if t < 0.62
                   else mix(base, light, min(1.0, (t - 0.62) * 2.2)))
            if r > radius[a] - 1.0:
                col = mix(col, dark, 0.5)
            img.putpixel((x, y), col + (255,))
    for (x, y), k in (((5, 5), 0.9), ((6, 5), 0.6), ((5, 6), 0.6), ((4, 6), 0.3)):
        px = img.getpixel((x, y))
        if px[3]:
            img.putpixel((x, y), mix(px[:3], shine, k) + (255,))
    return img


def stairs_states(model):
    """Vanilla's stairs blockstate, for the models `model`, `model`_inner and `model`_outer."""
    variants = {}
    for facing, y in (('east', 0), ('south', 90), ('west', 180), ('north', 270)):
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                # a left corner is the right one turned a quarter back; upside down, left and right swap
                if half == 'bottom':
                    turn = y - 90 if shape.endswith('left') else y
                else:
                    turn = y + 90 if shape.endswith('right') else y
                kind = shape.split('_')[0]
                v = {'model': model if kind == 'straight' else f'{model}_{kind}'}
                if half == 'top':
                    v['x'] = 180
                if turn % 360:
                    v['y'] = turn % 360
                if len(v) > 1:
                    v['uvlock'] = True
                variants[f'facing={facing},half={half},shape={shape}'] = v
    return {'variants': variants}


def wall_states(model):
    """Vanilla's wall blockstate, for the models `model`_post, `model`_side and `model`_side_tall."""
    parts = [{'when': {'up': 'true'}, 'apply': {'model': model + '_post'}}]
    for height, suffix in (('low', '_side'), ('tall', '_side_tall')):
        for side, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            apply = {'model': model + suffix, 'uvlock': True}
            if y:
                apply['y'] = y
            parts.append({'when': {side: height}, 'apply': apply})
    return {'multipart': parts}


def gen_red_mud_bricks():
    """Red mud, left over from dissolving bauxite, fired into bricks: a block of its own, with stairs, slab and wall
    made as vanilla's bricks' are, and drawn as vanilla's mud bricks in red."""
    save_png(mud_ball_texture(1281, (0x3C, 0x14, 0x0C), (0x86, 0x32, 0x1C), (0xBC, 0x58, 0x36), (0xF0, 0xB0, 0x90)),
             asset('textures', 'item', 'red_mud.png'))
    save_png(metal_item_texture('ingot', 1282, *RED_MUD_BRICK_ITEM), asset('textures', 'item', 'red_mud_brick.png'))
    for name in ('red_mud', 'red_mud_brick'):
        write_json(asset('models', 'item', name + '.json'),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})

    tex = 'minecraft:block/mud_bricks'
    faces = {'bottom': tex, 'top': tex, 'side': tex}
    templates = tinted_templates()
    block = lambda name, parent, textures: write_json(asset('models', 'block', name + '.json'), {
        'parent': f'minecraft:block/{parent}', 'textures': textures, 'elements': templates[parent]})
    # placed, its north and west faces are mirrored, as vanilla's mud bricks' are; the item and a double slab are not
    block('red_mud_bricks', 'cube_all', {'all': tex})
    block('red_mud_bricks_north_west_mirrored', 'cube_north_west_mirrored_all', {'all': tex})
    block('red_mud_brick_stairs', 'stairs', faces)
    block('red_mud_brick_stairs_inner', 'inner_stairs', faces)
    block('red_mud_brick_stairs_outer', 'outer_stairs', faces)
    block('red_mud_brick_slab', 'slab', faces)
    block('red_mud_brick_slab_top', 'slab_top', faces)
    for suffix, parent in (('_post', 'template_wall_post'), ('_side', 'template_wall_side'),
                           ('_side_tall', 'template_wall_side_tall'), ('_inventory', 'wall_inventory')):
        block('red_mud_brick_wall' + suffix, parent, {'wall': tex})
    for name, model in (('red_mud_bricks', 'red_mud_bricks'), ('red_mud_brick_stairs', 'red_mud_brick_stairs'),
                        ('red_mud_brick_slab', 'red_mud_brick_slab'),
                        ('red_mud_brick_wall', 'red_mud_brick_wall_inventory')):
        write_json(asset('models', 'item', name + '.json'), {'parent': f'mcremix:block/{model}'})
    write_json(asset('blockstates', 'red_mud_bricks.json'),
               {'variants': {'': {'model': 'mcremix:block/red_mud_bricks_north_west_mirrored'}}})
    write_json(asset('blockstates', 'red_mud_brick_stairs.json'), stairs_states('mcremix:block/red_mud_brick_stairs'))
    write_json(asset('blockstates', 'red_mud_brick_slab.json'), {'variants': {
        'type=bottom': {'model': 'mcremix:block/red_mud_brick_slab'},
        'type=top': {'model': 'mcremix:block/red_mud_brick_slab_top'},
        'type=double': {'model': 'mcremix:block/red_mud_bricks'}}})
    write_json(asset('blockstates', 'red_mud_brick_wall.json'), wall_states('mcremix:block/red_mud_brick_wall'))

    for name in ('red_mud_bricks', 'red_mud_brick_stairs', 'red_mud_brick_wall'):
        write_json(data('mcremix', 'loot_tables', 'blocks', name + '.json'), loot_self(name))
    # a double slab drops both halves
    write_json(data('mcremix', 'loot_tables', 'blocks', 'red_mud_brick_slab.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
            'type': 'minecraft:item', 'name': 'mcremix:red_mud_brick_slab',
            'functions': [{'function': 'minecraft:set_count', 'count': 2, 'add': False,
                           'conditions': [{'condition': 'minecraft:block_state_property',
                                           'block': 'mcremix:red_mud_brick_slab', 'properties': {'type': 'double'}}]},
                          {'function': 'minecraft:explosion_decay'}]}]}]})
    for kind in ('blocks', 'items'):
        for name, family in (('stairs', 'stairs'), ('slab', 'slabs'), ('wall', 'walls')):
            write_json(data('minecraft', 'tags', kind, family + '.json'), tag([f'mcremix:red_mud_brick_{name}']))

    firing('red_mud_brick', item('mcremix:red_mud'), 'mcremix:red_mud_brick', 'warm', 160)
    bricks = item('mcremix:red_mud_bricks')
    shaped = lambda name, pattern, key, count=1: recipe(name, {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': pattern, 'key': key,
        'result': item(f'mcremix:{name}', count)})
    shaped('red_mud_bricks', ['##', '##'], {'#': item('mcremix:red_mud_brick')})
    shaped('red_mud_brick_stairs', ['#  ', '## ', '###'], {'#': bricks}, 4)
    shaped('red_mud_brick_slab', ['###'], {'#': bricks}, 6)
    shaped('red_mud_brick_wall', ['###', '###'], {'#': bricks}, 6)
    for name, count in (('red_mud_brick_stairs', 1), ('red_mud_brick_slab', 2), ('red_mud_brick_wall', 1)):
        recipe(f'stonecutting/{name}', {'type': 'minecraft:stonecutting', 'ingredient': bricks,
                                         'result': f'mcremix:{name}', 'count': count})


def red_mud_lang():
    return {
        'item.mcremix.red_mud': 'Red Mud',
        'item.mcremix.red_mud.tooltip.summary': (
            "What's left when _bauxite_ dissolves: iron-red sludge. _Fire_ it in the Furnace into _red mud bricks_."),
        'item.mcremix.red_mud_brick': 'Red Mud Brick',
        'block.mcremix.red_mud_bricks': 'Red Mud Bricks',
        'block.mcremix.red_mud_brick_stairs': 'Red Mud Brick Stairs',
        'block.mcremix.red_mud_brick_slab': 'Red Mud Brick Slab',
        'block.mcremix.red_mud_brick_wall': 'Red Mud Brick Wall',
    }


# the chemical works' liquids (Chemical.java): dark, base, light, and how opaque (255 solid; less, see-through)
CHEMICALS = {
    # coal tar: black with a brown cast, its slow surface catching the light in thin lines
    'coal_tar': ((0x0C, 0x0A, 0x08), (0x26, 0x20, 0x1B), (0x74, 0x6A, 0x60), 255),
    # wood spirit: thin and clear, a pale straw colour
    'wood_spirit': ((0xA8, 0x9C, 0x7C), (0xD8, 0xCC, 0xA8), (0xFA, 0xF4, 0xE2), 150),
    # ferric chloride: dark rusty brown, opaque
    'ferric_chloride': ((0x2E, 0x14, 0x06), (0x5C, 0x30, 0x10), (0xA8, 0x6A, 0x30), 255),
    # chlorine: a pale yellow-green haze
    'chlorine': ((0x9C, 0xB0, 0x40), (0xC4, 0xD4, 0x6C), (0xE8, 0xF0, 0xB0), 130),
    # caustic soda: clear lye, faintly milky
    'caustic_soda': ((0xB4, 0xC0, 0xC0), (0xE4, 0xEC, 0xEC), (0xFF, 0xFF, 0xFF), 160),
    # bauxite liquor: cloudy orange, the bauxite's red still in it
    'bauxite_liquor': ((0x7C, 0x40, 0x1C), (0xC8, 0x84, 0x4E), (0xF0, 0xC0, 0x90), 255),
    # salt water: the sea's blue-green, a little greyer and clearer than a river's
    'salt_water': ((0x1C, 0x46, 0x78), (0x36, 0x6C, 0xA8), (0x9C, 0xC8, 0xEC), 170),
    # hydrogen: all but invisible, a faint cold shimmer
    'hydrogen': ((0xC8, 0xD8, 0xE8), (0xE4, 0xEC, 0xF8), (0xFF, 0xFF, 0xFF), 70),
    # ethanol: clear as water but colder and brighter, with no blue in it
    'ethanol': ((0xB8, 0xC8, 0xD0), (0xE0, 0xEA, 0xEE), (0xFF, 0xFF, 0xFF), 110),
    # ethylene: a faint warm haze
    'ethylene': ((0xD8, 0xC8, 0xB0), (0xF0, 0xE6, 0xD6), (0xFF, 0xFC, 0xF4), 80),
    # vinyl chloride: a faint lilac haze, heavier than ethylene's
    'vinyl_chloride': ((0xC0, 0xB8, 0xC8), (0xE2, 0xDC, 0xEA), (0xFF, 0xFF, 0xFF), 90),
    # hydrogen chloride: all but colourless, a pale grey fume
    'hydrogen_chloride': ((0xC4, 0xC8, 0xC0), (0xE6, 0xEA, 0xE2), (0xFF, 0xFF, 0xFF), 80),
    # hydrochloric acid: clear, with the faint yellow cast of the acid as it's sold
    'hydrochloric_acid': ((0xBC, 0xC0, 0x94), (0xE6, 0xE8, 0xC4), (0xFF, 0xFF, 0xF0), 130),
    # ethylene dichloride: clear and oily, a faint green sheen on it
    'ethylene_dichloride': ((0xB4, 0xC0, 0xA4), (0xE8, 0xEE, 0xDA), (0xFF, 0xFF, 0xF4), 120),
    # pressurized steam: a white, billowing fog
    'pressurized_steam': ((0xC8, 0xCC, 0xD0), (0xF2, 0xF4, 0xF6), (0xFF, 0xFF, 0xFF), 150),
    # warm water: a river's blue gone paler and a little green, as water does warmed in a pipe
    'warm_water': ((0x2E, 0x5C, 0x88), (0x5A, 0x92, 0xCC), (0xC4, 0xE4, 0xF4), 165),
    # cold water: a deep, clear blue with a bright icy sheen
    'cold_water': ((0x14, 0x34, 0x74), (0x2C, 0x5C, 0xAE), (0xE0, 0xF0, 0xFF), 175),
    # ethylene glycol: a clear, thick syrup with a faint cool tint
    'ethylene_glycol': ((0xA8, 0xBC, 0xB4), (0xDC, 0xEC, 0xE4), (0xFF, 0xFF, 0xFF), 135),
    # coolant: bright antifreeze green, dyed so leaks show
    'coolant': ((0x14, 0x7C, 0x1C), (0x3F, 0xCC, 0x3C), (0xC8, 0xFF, 0xB4), 200),
    # hot coolant: the same green gone paler and steamier
    'hot_coolant': ((0x4C, 0x9C, 0x40), (0x8E, 0xE0, 0x7E), (0xF0, 0xFF, 0xE4), 185),
    # molten salt: clear as glass, but glowing orange-yellow with its heat
    'molten_salt': ((0xC0, 0x5C, 0x14), (0xF2, 0xA0, 0x40), (0xFF, 0xE8, 0xA0), 225),
    # molten sodium: a bright, silvery liquid metal, like mercury
    'molten_sodium': ((0x70, 0x76, 0x80), (0xC0, 0xC4, 0xCA), (0xF4, 0xF6, 0xFA), 255),
    # titanium tetrachloride: a colourless vapour with the faintest yellow in it
    'titanium_tetrachloride': ((0xD0, 0xCC, 0xB8), (0xF4, 0xF2, 0xE4), (0xFF, 0xFF, 0xFF), 90),
}
# gases have no bucket (Chemical.gas)
GASES = {'chlorine', 'hydrogen', 'ethylene', 'vinyl_chloride', 'hydrogen_chloride', 'pressurized_steam',
         'titanium_tetrachloride'}


def liquid_frame(size, phase, waves, palette, shift=0):
    """One frame of a chemical works liquid: its colour, darker in the troughs of slow waves and lit in a thin line
    along each crest, as a glossy liquid catches the light. Waves are whole numbers of cycles across the texture and
    over the loop, so it tiles and loops."""
    dark, base, light, alpha = palette
    img = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            v = sum(a * math.sin(2 * math.pi * (kx * x + ky * (y - shift)) / size + p + 2 * math.pi * m * phase)
                    for a, kx, ky, p, m in waves)
            col = mix(dark, base, 0.5 + 0.5 * math.tanh(v))
            sheen = max(0.0, 1 - abs(v - 1.3) / 0.4)
            col = mix(col, light, 0.7 * sheen)
            # a clear liquid is a little less see-through where it catches the light
            img.putpixel((x, y), col + (min(255, int(alpha + (255 - alpha) * 0.5 * sheen)),))
    return img


def liquid_textures(palette, seed):
    rnd = random.Random(seed)

    def waves(n, moving, k):
        return [(rnd.uniform(0.5, 1.0), rnd.choice(k), rnd.choice(k), rnd.uniform(0, 2 * math.pi),
                 rnd.choice((-1, 1)) if moving else 0) for _ in range(n)]
    frames = 32
    still_waves = waves(5, True, (-2, -1, 1, 2))
    still = Image.new('RGBA', (16, 16 * frames))
    for f in range(frames):
        still.paste(liquid_frame(16, f / frames, still_waves, palette), (0, 16 * f))
    flow_waves = waves(6, False, (-3, -2, -1, 1, 2, 3))
    flow = Image.new('RGBA', (32, 32 * frames))
    for f in range(frames):
        flow.paste(liquid_frame(32, 0, flow_waves, palette, shift=f), (0, 32 * f))
    return still, flow


def gen_chemicals():
    for n, (name, palette) in enumerate(CHEMICALS.items()):
        still, flow = liquid_textures(palette, 1260 + n)
        save_png(still, asset('textures', 'block', f'{name}_still.png'))
        write_json(asset('textures', 'block', f'{name}_still.png.mcmeta'),
                   {'animation': {'frametime': 6, 'interpolate': True}})
        save_png(flow, asset('textures', 'block', f'{name}_flow.png'))
        write_json(asset('textures', 'block', f'{name}_flow.png.mcmeta'), {'animation': {'frametime': 4}})
        # drawn as a fluid; the block model only gives the particles
        write_json(asset('models', 'block', name + '.json'), {'textures': {'particle': f'mcremix:block/{name}_still'}})
        write_json(asset('blockstates', name + '.json'), {'variants': {'': {'model': f'mcremix:block/{name}'}}})
        if name not in GASES:
            write_json(asset('models', 'item', f'{name}_bucket.json'),
                       {'parent': 'forge:item/bucket_drip', 'loader': 'forge:fluid_container',
                        'fluid': f'mcremix:{name}'})


def chemical_lang():
    lang = {}
    for name in CHEMICALS:
        lang[f'fluid.mcremix.{name}'] = title_case(name)
        lang[f'block.mcremix.{name}'] = title_case(name)
        if name not in GASES:
            lang[f'item.mcremix.{name}_bucket'] = title_case(name) + ' Bucket'
    return lang


def rough_metal_texture(metal, seed):
    """Spilled metal set hard: smooth rounded lumps run together, shadowed where they meet, each lit on its upper left
    with a glint. It tiles."""
    base, dark, light = ROUGH_METAL[metal]
    rnd = random.Random(seed)
    lumps = [(gx * 16 / 3 + rnd.uniform(0, 4), gy * 16 / 3 + rnd.uniform(0, 4)) for gx in range(3) for gy in range(3)]
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            near = sorted((math.hypot(dx, dy), dx, dy) for lx, ly in lumps
                          for dx in [(x - lx + 8) % 16 - 8] for dy in [(y - ly + 8) % 16 - 8])
            (d1, dx, dy), (d2, _, _) = near[0], near[1]
            t = 0.58 - (dx + dy) * 0.05 - d1 * 0.03 + rnd.uniform(-0.04, 0.04)
            col = mix(dark, base, t * 2) if t < 0.5 else mix(base, light, min(1.0, t * 2 - 1))
            if d2 - d1 < 1.0:
                col = mix(col, dark, 0.5)
            elif -2.2 < dx < -0.8 and -2.2 < dy < -0.8:
                col = mix(col, light, 0.55)
            img.putpixel((x, y), col + (255,))
    return img


def molten_frame(size, phase, waves, shift=0, skin_width=0.35):
    """One frame of molten metal in grey, for each metal's colour to tint: bright, with thin darker skins of cooling
    metal drifting over it. Waves are whole numbers of cycles across the texture and over the loop, so it tiles and
    loops."""
    img = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            v = sum(a * math.sin(2 * math.pi * (kx * x + ky * (y - shift)) / size + p + 2 * math.pi * m * phase)
                    for a, kx, ky, p, m in waves)
            skin = max(0.0, 1 - abs(v) / skin_width)  # where the waves cancel, a thin crust forms
            g = clamp(250 - 62 * skin - 10 * max(0.0, math.tanh(v)))
            img.putpixel((x, y), (g, g, g, 255))
    return img


def molten_textures(seed):
    rnd = random.Random(seed)

    def waves(n, moving, k):
        return [(rnd.uniform(0.5, 1.0), rnd.choice(k), rnd.choice(k), rnd.uniform(0, 2 * math.pi),
                 rnd.choice((-1, 1)) if moving else 0) for _ in range(n)]
    frames = 32
    still_waves = waves(5, True, (-2, -1, 1, 2))
    still = Image.new('RGBA', (16, 16 * frames))
    for f in range(frames):
        still.paste(molten_frame(16, f / frames, still_waves, skin_width=0.5), (0, 16 * f))
    flow_waves = waves(6, False, (-3, -2, -1, 1, 2, 3))
    flow = Image.new('RGBA', (32, 32 * frames))
    for f in range(frames):
        flow.paste(molten_frame(32, 0, flow_waves, shift=f), (0, 32 * f))
    return still, flow


def clay_texture(seed, base, dark, light, soot=0.0, rim=None, size=16):
    """Clay, fired or not: smooth with faint mottling. Fired, it is sooted towards the bottom, where the fire licks it,
    and the rows `rim` (first, last) are a lighter band round its top."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            t = 0.5 + rnd.uniform(-0.12, 0.12) + (0.08 if (x * 7 + y * 3) % 11 == 0 else 0)
            col = mix(dark, base, t * 2) if t < 0.5 else mix(base, light, t * 2 - 1)
            if soot:
                low = (y / size - 0.375) / 0.625
                col = mix(col, (0x2A, 0x22, 0x20), soot * max(0.0, low) * rnd.uniform(0.6, 1.0))
            if rim and rim[0] <= y <= rim[1]:
                col = mix(col, light, 0.4 if y == rim[0] else 0.15)
            img.putpixel((x, y), col + (255,))
    return img


def andesite_bar_texture(seed):
    """A bar of andesite alloy, as Create's casings are framed in: streaked along its length, lit down its left."""
    shades = [(80, 83, 81), (130, 135, 132), (154, 164, 157), (168, 179, 171), (154, 164, 157), (143, 150, 145),
              (154, 164, 157), (143, 150, 145), (154, 164, 157), (143, 150, 145), (130, 135, 132), (143, 150, 145),
              (130, 135, 132), (112, 117, 112), (96, 99, 95), (64, 69, 67)]
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            col = mix(shades[x], (0, 0, 0), rnd.uniform(0, 0.06)) if 0 < x < 15 else shades[x]
            img.putpixel((x, y), col + (255,))
    return img


SAND = ((0xDA, 0xCF, 0xA3), (0xB8, 0xA2, 0x70), (0xEC, 0xE6, 0xC0))


def sand_mold_textures(block):
    """A sand mold seen from above and in front: rammed sand with the pattern's shape sunk into its top, and that
    shape alone as the metal, in grey for each metal's colour to tint."""
    rnd = random.Random(70 + block)
    base, dark, light = SAND
    hollow = (0x8A, 0x74, 0x4A)
    mold = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    metal = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    top, front, bottom = (4, 11, 13) if not block else (2, 11, 13)
    x0, y0, x1, y1 = (4, 6, 11, 9) if not block else (4, 4, 11, 9)  # the hollow, on the top
    for y in range(top, bottom + 1):
        for x in range(1, 15):
            t = 0.55 + rnd.uniform(-0.15, 0.15)
            col = mix(dark, base, t * 2) if t < 0.5 else mix(base, light, t * 2 - 1)
            if y >= front:
                col = mix(col, dark, 0.35)  # the front face, out of the light
            if y in (top, bottom) or x in (1, 14):
                col = mix(col, (0x5A, 0x4A, 0x30), 0.75)  # outline
            if x0 <= x <= x1 and y0 <= y <= y1:
                # the far wall of the hollow faces the light; its floor is shadowed by the left and far rims
                col = mix(col, light, 0.3) if y == y0 else mix(col, hollow, 0.85 if x == x0 or y == y0 + 1 else 0.6)
                g = 240 if y == y0 else 215 - (20 if x == x1 or y == y1 else 0) + (15 if x == x0 + 1 else 0)
                metal.putpixel((x, y), (g, g, g, 255))
            elif y == y1 + 1 and x0 <= x <= x1:
                col = mix(col, light, 0.35)  # the near rim catches the light
            mold.putpixel((x, y), col + (255,))
    return mold, metal


def gen_fluid_textures():
    """Pipes, pumps and tanks in brass, iron, glass, steel and aluminium, iron's rusting, Create's copper ones
    weathered, and the molten metals, rough metals, steel block, crucible and sand molds."""
    for n, m in enumerate(FLUID_MATERIALS):
        seed = 900 + 10 * n
        save_png(recolour('pipes', m, seed), asset('textures', 'block', m + '_pipes.png'))
        save_png(recolour('pipes_connected', m, seed + 1), asset('textures', 'block', m + '_pipes_connected.png'))
        if m in PUMP_MATERIALS:
            save_png(recolour('pump', m, seed + 2), asset('textures', 'block', m + '_pump.png'))
        if m in TANK_MATERIALS:
            for k, suffix in enumerate(TANK_SPRITES):
                save_png(recolour('fluid_tank' + suffix, m, seed + 3 + k),
                         asset('textures', 'block', tank_name(m) + suffix + '.png'))
    # the vat sheet as drawn, for Create's own copper tank
    sheet = Image.new('RGBA', (16, 16))
    for y, row in enumerate(vat_sheet()):
        for x, col in enumerate(row):
            sheet.putpixel((x, y), col)
    save_png(sheet, asset('textures', 'block', 'fluid_tank_vat.png'))
    for n, stage in enumerate(('exposed', 'weathered', 'oxidized')):
        for k, sprite in enumerate(COPPER_SPRITES):
            save_png(recolour(sprite, stage + '_copper', 1100 + 20 * n + k),
                     asset('textures', 'block', f'{stage}_{sprite}.png'))
    # iron's pipes, pump and tank at each stage of rust; the rusted ones leak (Leaks)
    iron_sprites = [('pipes', 'iron_pipes'), ('pipes_connected', 'iron_pipes_connected'), ('pump', 'iron_pump')]
    iron_sprites += [('fluid_tank' + suffix, tank_name('iron') + suffix) for suffix in TANK_SPRITES]
    for n, stage in enumerate(RUST_STAGES):
        for k, (sprite, name) in enumerate(iron_sprites):
            save_png(recolour(sprite, stage + '_iron', 1150 + 20 * n + k),
                     asset('textures', 'block', f'{stage}_{name}.png'))
    # steel's tank side at each stage of rust, for steel vat mixers, which weather as their mixer does and are drawn as
    # their tank (vat_mixer_tank); its first stage is the steel tank's own as well
    for n, stage in enumerate(RUST_STAGES):
        save_png(recolour('fluid_tank', stage + '_steel', 1182 + n),
                 asset('textures', 'block', f'{stage}_{tank_name("steel")}.png'))
    # steel's pipes, pump and the rest of its tank spotted with rust, as far as steel rusts (Taran, 0.28.0)
    steel_sprites = [('pipes', 'steel_pipes'), ('pipes_connected', 'steel_pipes_connected'), ('pump', 'steel_pump')]
    steel_sprites += [('fluid_tank' + suffix, tank_name('steel') + suffix) for suffix in TANK_SPRITES if suffix]
    for k, (sprite, name) in enumerate(steel_sprites):
        save_png(recolour(sprite, 'exposed_steel', 1240 + k), asset('textures', 'block', f'exposed_{name}.png'))

    still, flow = molten_textures(1200)
    save_png(still, asset('textures', 'block', 'molten_metal_still.png'))
    write_json(asset('textures', 'block', 'molten_metal_still.png.mcmeta'),
               {'animation': {'frametime': 3, 'interpolate': True}})
    save_png(flow, asset('textures', 'block', 'molten_metal_flow.png'))
    write_json(asset('textures', 'block', 'molten_metal_flow.png.mcmeta'), {'animation': {'frametime': 3}})
    for n, (m, _) in enumerate(MOLTEN):
        save_png(rough_metal_texture(m, 1210 + n), asset('textures', 'block', f'rough_{m}.png'))
    save_png(metal_block_texture('steel', 1220), asset('textures', 'block', 'steel_block.png'))
    save_png(metal_block_texture('aluminium', 1221), asset('textures', 'block', 'aluminium_block.png'))

    fired = ((0xB0, 0x6A, 0x48), (0x7E, 0x46, 0x2E), (0xCA, 0x86, 0x60))
    inside = ((0x3E, 0x32, 0x2E), (0x24, 0x1C, 0x1A), (0x5C, 0x4A, 0x42))
    # a pot's walls show rows 2-14 of its texture; a big pot's, rows 4-46 of one three times the size
    save_png(clay_texture(1230, *fired, soot=0.55, rim=(2, 3)), asset('textures', 'block', 'crucible.png'))
    save_png(clay_texture(1231, *inside), asset('textures', 'block', 'crucible_inside.png'))
    save_png(clay_texture(1233, *fired, soot=0.55, rim=(4, 6), size=48),
             asset('textures', 'block', 'crucible_large.png'))
    save_png(clay_texture(1234, *inside, size=48), asset('textures', 'block', 'crucible_large_inside.png'))
    save_png(andesite_bar_texture(1235), asset('textures', 'block', 'crucible_frame.png'))
    save_png(clay_texture(1232, (0xA0, 0xA6, 0xB2), (0x84, 0x8A, 0x98), (0xB6, 0xBC, 0xCA)),
             asset('textures', 'block', 'unfired_crucible.png'))
    for block, name in ((False, 'sand_ingot_mold'), (True, 'sand_block_mold')):
        mold, metal = sand_mold_textures(block)
        save_png(mold, asset('textures', 'item', name + '.png'))
        save_png(metal, asset('textures', 'item', name + '_metal.png'))


# the sides of a pipe a multipart piece of Create's pipe model joins, per axis: left, right, up and down
PIPE_SIDES = {'x': ('south', 'north', 'up', 'down'), 'y': ('east', 'west', 'south', 'north'),
              'z': ('east', 'west', 'up', 'down')}
PIPE_SHAPES = ['lu', 'ru', 'ld', 'rd', 'ud', 'u', 'd', 'lr', 'l', 'r']
FACING_ROTATIONS = {'down': {'x': 180}, 'east': {'x': 90, 'y': 90}, 'north': {'x': 90}, 'south': {'x': 90, 'y': 180},
                    'up': {}, 'west': {'x': 90, 'y': 270}}
TANK_SHAPES = {'plain': '', 'window': '_window', 'window_ne': '_window_ne', 'window_nw': '_window_nw',
               'window_se': '_window_se', 'window_sw': '_window_sw'}


def fluid_particle(m):
    """What breaking one throws up: its metal's block, or glass (FluidPartModels.particle)."""
    return {'glass': 'minecraft:block/glass', 'iron': 'minecraft:block/iron_block',
            'brass': 'create:block/brass_block'}.get(m, f'mcremix:block/{m}_block')


def cell_box(frm, to, textures):
    """A box of any size, as elements no bigger than a block, each face mapping its texture pixel for pixel as vanilla
    maps a face inside one block; faces inside the box are left out. textures: {direction: texture}."""
    elements = []

    def cuts(a, b):
        points = [a] + [c for c in range(int(math.floor(a / 16)) * 16 + 16, int(math.ceil(b)), 16) if a < c < b] + [b]
        return list(zip(points, points[1:]))
    for x0, x1 in cuts(frm[0], to[0]):
        for y0, y1 in cuts(frm[1], to[1]):
            for z0, z1 in cuts(frm[2], to[2]):
                # the block cell this piece is in, and its corners within it
                cx, cy, cz = (math.floor(min(x0, x1 - 1e-6) / 16) * 16, math.floor(min(y0, y1 - 1e-6) / 16) * 16,
                              math.floor(min(z0, z1 - 1e-6) / 16) * 16)
                a, b, c, d, e, f = x0 - cx, x1 - cx, y0 - cy, y1 - cy, z0 - cz, z1 - cz
                uvs = {'north': [16 - b, 16 - d, 16 - a, 16 - c], 'south': [a, 16 - d, b, 16 - c],
                       'west': [e, 16 - d, f, 16 - c], 'east': [16 - f, 16 - d, 16 - e, 16 - c],
                       'up': [a, e, b, f], 'down': [a, 16 - f, b, 16 - e]}
                edge = {'north': z0 == frm[2], 'south': z1 == to[2], 'west': x0 == frm[0], 'east': x1 == to[0],
                        'down': y0 == frm[1], 'up': y1 == to[1]}
                faces = {side: {'uv': uvs[side], 'texture': tex} for side, tex in textures.items() if edge[side]}
                if faces:
                    elements.append({'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': faces})
    return elements


def span_box(frm, to, textures, origin=-16, size=48):
    """A box whose faces map one texture spread over `size` pixels from `origin` on every axis, so that the walls of a
    big pot are one surface. textures: {direction: texture}."""
    k = 16 / size
    a, b, c, d, e, f = [(v - origin) * k for v in (frm[0], to[0], frm[1], to[1], frm[2], to[2])]
    uvs = {'north': [16 - b, 16 - d, 16 - a, 16 - c], 'south': [a, 16 - d, b, 16 - c],
           'west': [e, 16 - d, f, 16 - c], 'east': [16 - f, 16 - d, 16 - e, 16 - c],
           'up': [a, e, b, f], 'down': [a, 16 - f, b, 16 - e]}
    return [{'from': frm, 'to': to, 'faces': {side: {'uv': uvs[side], 'texture': tex}
                                               for side, tex in textures.items()}}]


def pot_elements(big):
    """The crucible's pot, lip to the south, round the middle of its block (a big one's middle block): a floor, four
    walls with a notch in the south one, and a spout out from the notch. The fluid inside is drawn by
    CrucibleRenderer, between the walls and from the floor up to the notch, and the stream falls from the spout's
    end (CrucibleBlockEntity.lipPosition)."""
    if big:
        lo, hi, wall, floor, top, notch, spout, end = -12, 28, 2, -14, 28, (5, 11, 26), (4, 12, 25), 32
    else:
        lo, hi, wall, floor, top, notch, spout, end = 3, 13, 1, 1, 14, (7, 9, 13), (6, 10, 12), 15.5
    shaped = span_box if big else cell_box
    sides = ('north', 'south', 'east', 'west', 'up', 'down')

    def walled(frm, to, inward=None, skip=()):
        """Its face towards the inside of the pot in the inside texture, the rest in the pot's."""
        return shaped(frm, to, {d: '#inside' if d == inward else '#pot' for d in sides if d not in skip})
    inner_lo, inner_hi, base = lo + wall, hi - wall, floor + wall
    n0, n1, ny = notch
    s0, s1, sy = spout
    return (walled([lo, floor, lo], [hi, base, hi], 'up')
            + walled([lo, base, lo], [inner_lo, top, hi], 'east', skip=('down',))
            + walled([inner_hi, base, lo], [hi, top, hi], 'west', skip=('down',))
            + walled([inner_lo, base, lo], [inner_hi, top, inner_lo], 'south', skip=('down', 'east', 'west'))
            + walled([inner_lo, base, inner_hi], [inner_hi, ny, hi], 'north', skip=('down', 'east', 'west'))
            + walled([inner_lo, ny, inner_hi], [n0, top, hi], 'north', skip=('down', 'west'))
            + walled([n1, ny, inner_hi], [inner_hi, top, hi], 'north', skip=('down', 'east'))
            + walled([n0, sy, hi], [n1, ny, end], 'up', skip=('north',))
            + walled([s0, sy, hi], [n0, top, end], skip=('north',))
            + walled([n1, sy, hi], [s1, top, end], skip=('north',)))


def frame_elements(big):
    """The two posts the pot hangs between, on the shaft's sides (east and west for a pot facing south), each with a
    foot before and behind it. The drive shaft runs into them (CrucibleRenderer draws its end)."""
    sides = {d: '#frame' for d in ('north', 'south', 'east', 'west', 'up', 'down')}
    # the posts stand half a pixel in from the sides, so the shaft's end shows proud of them
    if big:
        posts, depth, height, feet, low = ((-15.5, -12.5), (28.5, 31.5)), (3, 13), 12, (-8, 24), -16
    else:
        posts, depth, height, feet, low = ((0.5, 2.5), (13.5, 15.5)), (5, 11), 11, (3, 13), 0
    els = []
    for x0, x1 in posts:
        els += cell_box([x0, low, depth[0]], [x1, height, depth[1]], sides)
        els += cell_box([x0, low, feet[0]], [x1, low + 1, depth[0]], sides)
        els += cell_box([x0, low, depth[1]], [x1, low + 1, feet[1]], sides)
    return els


# Create's pipe is 8 px across with 10 px rims; brass ones are 6 px across (Taran), drawn by squeezing Create's model
# (ThinPipeModel.squeeze), its ends still meeting the next block's
CREATE_PIPE_WALL, THIN_PIPE_WALL = 4, 3


def squeeze(c):
    """A coordinate of Create's pipe model, in pixels, where a thin pipe has it (ThinPipeModel.squeeze)."""
    d = abs(c - 8)
    if d <= CREATE_PIPE_WALL:
        to = d * THIN_PIPE_WALL / CREATE_PIPE_WALL
    elif d <= 8:
        to = THIN_PIPE_WALL + (d - CREATE_PIPE_WALL) * (8 - THIN_PIPE_WALL) / (8 - CREATE_PIPE_WALL)
    else:
        to = d
    return 8 + math.copysign(to, c - 8)


def thin_pipe_item_model(m):
    """Create's pipe item, a length of pipe with a rim at each end, squeezed as a thin pipe's blocks are."""
    rim = {'north': {'uv': [11, 11, 16, 16], 'texture': '#1'}, 'east': {'uv': [10.5, 16, 9.5, 11], 'texture': '#1'},
           'south': {'uv': [11, 11, 16, 16], 'texture': '#1'}, 'west': {'uv': [9.5, 16, 10.5, 11], 'texture': '#1'},
           'up': {'uv': [9.5, 11, 10.5, 16], 'rotation': 90, 'texture': '#1'},
           'down': {'uv': [10.5, 11, 9.5, 16], 'rotation': 90, 'texture': '#1'}}
    elements = [
        ([4, 4, 0], [12, 12, 16], {'east': {'uv': [4, 8, 8, 0], 'rotation': 90, 'texture': '#1'},
                                   'west': {'uv': [4, 0, 8, 8], 'rotation': 90, 'texture': '#1'},
                                   'up': {'uv': [4, 8, 8, 0], 'texture': '#1'},
                                   'down': {'uv': [4, 8, 8, 0], 'rotation': 180, 'texture': '#1'}}),
        ([3, 3, 0], [13, 13, 2], rim), ([3, 3, 14], [13, 13, 16], rim)]
    return {'parent': 'minecraft:block/block',
            'textures': {'1': f'mcremix:block/{m}_pipes', 'particle': fluid_particle(m)},
            'elements': [{'from': [squeeze(c) for c in frm], 'to': [squeeze(c) for c in to], 'faces': faces}
                         for frm, to, faces in elements],
            'display': {'fixed': {'rotation': [0, 90, 0], 'scale': [0.5, 0.5, 0.5]}}}


# Create's pump models (tools/create_pump_models.json): its block's two flanges, its cog, modelled facing south and ten
# pixels deep, and its item, lying along x
CREATE_PUMP_MODELS = json.load(open(os.path.join(ROOT, 'tools', 'create_pump_models.json')))
# the axes (0 x, 1 y, 2 z) a face's texture runs along, u then v, when the face is not turned
FACE_UV_AXES = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}
FACE_OF = {(0, 0, -1): 'north', (0, 0, 1): 'south', (-1, 0, 0): 'west', (1, 0, 0): 'east', (0, 1, 0): 'up',
           (0, -1, 0): 'down'}
# the Brass Mechanical Pump's impeller ends here where Create's cog ends at 13, leaving room for its valve's neck
BRASS_IMPELLER_END = 8


def shortened(elements, axis, end):
    """Create's pump cog, ending at `end` along `axis` where it ended at 13: each face that runs along that axis shows
    as much less of its texture as it is shorter."""
    out = []
    for element in elements:
        element = json.loads(json.dumps(element))
        start, stop = element['from'][axis], element['to'][axis]
        new_stop = end - (13 - stop)
        scale = (new_stop - start) / (stop - start)
        element['to'][axis] = new_stop
        for name, face in element['faces'].items():
            u_axis, v_axis = FACE_UV_AXES[name]
            if face.get('rotation', 0) in (90, 270):
                u_axis, v_axis = v_axis, u_axis
            if axis in (u_axis, v_axis):
                i = 0 if axis == u_axis else 1
                face['uv'][i + 2] = round(face['uv'][i] + (face['uv'][i + 2] - face['uv'][i]) * scale, 4)
        out.append(element)
    return out


def brass_valve(turn=lambda c: c):
    """The Brass Mechanical Pump's valve (Taran, 0.26.0: a handwheel on a stem, as on a throttle valve, since it sets its
    own flow), drawn for a pump facing up as Create's pump models are: a neck of the pump's own banded brass between its
    impeller and its front flange, and out of the neck's south side a stem with a handwheel flat across it, polished, a
    ring with its corners cut and two spokes crossing at its hub. RatedPumpBlock.Throttled has the handwheel's middle and
    outline. `turn` moves a corner, for drawing it some other way round."""
    band = {'uv': [10.5, 9, 14.5, 11.5], 'texture': '#4'}
    neck = {'from': list(turn((4, BRASS_IMPELLER_END, 4))), 'to': list(turn((12, 13, 12))), 'faces': {}}
    # the neck's sides, which way round they face once turned; its ends meet the impeller and the flange
    for side, normal in (('north', (0, 0, -1)), ('south', (0, 0, 1)), ('west', (-1, 0, 0)), ('east', (1, 0, 0))):
        neck['faces'][FACE_OF[turn(normal)]] = dict(band, rotation=0 if normal == turn(normal) else 90)
    boxes = [((7.5, 11.5, 12), (8.5, 12.5, 14.5)), ((7, 11, 14.5), (9, 13, 16)),        # stem and hub
             ((5, 15, 15), (11, 16, 16)), ((5, 8, 15), (11, 9, 16)),                    # the wheel's rim
             ((4, 9, 15), (5, 15, 16)), ((11, 9, 15), (12, 15, 16)),
             ((7.5, 9, 15.25), (8.5, 15, 15.75)), ((5, 11.5, 15.25), (11, 12.5, 15.75))]  # spokes
    return [neck] + [clockwork_box(turn(a), turn(b)) for a, b in boxes]


def brass_pump_models():
    """The Brass Mechanical Pump: Create's flanges, a shallower impeller and the valve, for its block, its cog and its
    item (which lies along x, the valve on top)."""
    textures = {'0': 'mcremix:block/brass_clockwork', '4': 'mcremix:block/brass_pump',
                'particle': fluid_particle('brass')}
    block = {'parent': 'block/block', 'textures': textures,
             'elements': CREATE_PUMP_MODELS['block'] + brass_valve()}
    cog = {'parent': 'block/block', 'texture_size': [32, 32], 'textures': {'2': 'mcremix:block/brass_pump'},
           'elements': shortened(CREATE_PUMP_MODELS['cog'], 2, BRASS_IMPELLER_END)}
    flanges = [e for e in CREATE_PUMP_MODELS['item'] if e['from'][0] in (0, 13)]
    impeller = [e for e in CREATE_PUMP_MODELS['item'] if e['from'][0] not in (0, 13)]
    item = {'parent': 'block/block', 'texture_size': [32, 32], 'textures': textures,
            'elements': flanges + shortened(impeller, 0, BRASS_IMPELLER_END)
            + brass_valve(lambda c: (c[1], c[2], c[0])),
            'display': CREATE_PUMP_MODELS['item_display']}
    return block, cog, item


def gen_fluid_models():
    for m in FLUID_MATERIALS:
        pipe = pipe_name(m)
        parts = []
        for axis, (left, right, up, down) in PIPE_SIDES.items():
            named = {'l': left, 'r': right, 'u': up, 'd': down}
            for shape in PIPE_SHAPES:
                parts.append({'apply': {'model': f'create:block/fluid_pipe/{shape}_{axis}'},
                              'when': {side: str(k in shape).lower() for k, side in named.items()}})
        write_json(asset('blockstates', pipe + '.json'), {'multipart': parts})
        write_json(asset('models', 'item', pipe + '.json'), thin_pipe_item_model(m) if m == 'brass' else {
            'parent': 'create:block/fluid_pipe/item',
            'textures': {'1': f'mcremix:block/{m}_pipes', 'particle': fluid_particle(m)}})
        if m == 'brass':
            pump = pump_name(m)
            block, cog, item = brass_pump_models()
            write_json(asset('blockstates', pump + '.json'), {'variants': {
                f'facing={f}': dict({'model': f'mcremix:block/{pump}/block'}, **r)
                for f, r in FACING_ROTATIONS.items()}})
            write_json(asset('models', 'block', pump, 'block.json'), block)
            write_json(asset('models', 'block', pump, 'cog.json'), cog)
            write_json(asset('models', 'item', pump + '.json'), item)
        elif m in PUMP_MATERIALS:
            pump = pump_name(m)
            write_json(asset('blockstates', pump + '.json'), {'variants': {
                f'facing={f}': dict({'model': 'create:block/mechanical_pump/block'}, **r)
                for f, r in FACING_ROTATIONS.items()}})
            write_json(asset('models', 'item', pump + '.json'), {
                'parent': 'create:block/mechanical_pump/item',
                'textures': {'4': f'mcremix:block/{m}_pump', 'particle': fluid_particle(m)}})
            write_json(asset('models', 'block', pump, 'cog.json'), {
                'parent': 'create:block/mechanical_pump/cog', 'textures': {'2': f'mcremix:block/{m}_pump'}})
        if m in TANK_MATERIALS:
            tank = tank_name(m)
            ends = {('false', 'false'): 'middle', ('false', 'true'): 'top', ('true', 'false'): 'bottom',
                    ('true', 'true'): 'single'}
            write_json(asset('blockstates', tank + '.json'), {'variants': {
                f'bottom={bottom},shape={shape},top={top}': {'model': f'create:block/fluid_tank/block_{end}{suffix}'}
                for (bottom, top), end in ends.items() for shape, suffix in TANK_SHAPES.items()}})
            sprite = f'mcremix:block/{tank}'
            write_json(asset('models', 'item', tank + '.json'), {
                'parent': 'create:block/fluid_tank/block_single_window',
                'textures': {'0': sprite + '_top', '1': sprite, '4': sprite + '_inner', '5': sprite + '_window_single',
                             'particle': sprite}})

    # molten metal is drawn as a fluid; its block model only gives the particles
    write_json(asset('models', 'block', 'molten_metal.json'),
               {'textures': {'particle': 'mcremix:block/molten_metal_still'}})
    for m, _ in MOLTEN:
        write_json(asset('blockstates', f'molten_{m}.json'),
                   {'variants': {'': {'model': 'mcremix:block/molten_metal'}}})
        write_json(asset('models', 'item', f'molten_{m}_bucket.json'),
                   {'parent': 'forge:item/bucket_drip', 'loader': 'forge:fluid_container',
                    'fluid': f'mcremix:molten_{m}'})
        # rough metal stands in layers, as snow does
        variants = {}
        for layers in range(1, 9):
            name = f'rough_{m}' if layers == 8 else f'rough_{m}_height{2 * layers}'
            tex = f'mcremix:block/rough_{m}'
            if layers == 8:
                model = {'parent': 'minecraft:block/cube_all', 'textures': {'all': tex}}
            else:
                h = 2 * layers
                model = {'parent': 'minecraft:block/block', 'textures': {'particle': tex, 'texture': tex},
                         'elements': [{'from': [0, 0, 0], 'to': [16, h, 16], 'faces': {
                             'down': {'uv': [0, 0, 16, 16], 'texture': '#texture', 'cullface': 'down'},
                             'up': {'uv': [0, 0, 16, 16], 'texture': '#texture'},
                             **{side: {'uv': [0, 16 - h, 16, 16], 'texture': '#texture', 'cullface': side}
                                for side in ('north', 'south', 'east', 'west')}}}]}
            write_json(asset('models', 'block', name + '.json'), model)
            variants[f'layers={layers}'] = {'model': f'mcremix:block/{name}'}
        write_json(asset('blockstates', f'rough_{m}.json'), {'variants': variants})

    for metal in ('steel', 'aluminium'):
        write_json(asset('models', 'block', f'{metal}_block.json'),
                   {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'mcremix:block/{metal}_block'}})
        write_json(asset('models', 'item', f'{metal}_block.json'), {'parent': f'mcremix:block/{metal}_block'})
        write_json(asset('blockstates', f'{metal}_block.json'),
                   {'variants': {'': {'model': f'mcremix:block/{metal}_block'}}})

    # the crucible: its frame stands as the block, its pot (which tips) and the shaft's end are partial models drawn by
    # CrucibleRenderer, which also draws all of a big crucible, so a formed block shows nothing of its own
    textures = {'pot': 'mcremix:block/crucible', 'inside': 'mcremix:block/crucible_inside',
                'frame': 'mcremix:block/crucible_frame', 'particle': 'mcremix:block/crucible'}
    big = dict(textures, pot='mcremix:block/crucible_large', inside='mcremix:block/crucible_large_inside')
    write_json(asset('models', 'block', 'crucible', 'frame.json'),
               {'parent': 'minecraft:block/block', 'textures': textures, 'elements': frame_elements(False)})
    write_json(asset('models', 'block', 'crucible', 'frame_large.json'),
               {'textures': big, 'elements': frame_elements(True)})
    write_json(asset('models', 'block', 'crucible', 'pot.json'),
               {'parent': 'minecraft:block/block', 'textures': textures, 'elements': pot_elements(False)})
    write_json(asset('models', 'block', 'crucible', 'pot_large.json'),
               {'textures': big, 'elements': pot_elements(True)})
    write_json(asset('models', 'block', 'crucible', 'formed.json'), {'textures': {'particle': 'mcremix:block/crucible'}})
    write_json(asset('models', 'block', 'crucible', 'shaft.json'), {
        'parent': 'minecraft:block/block',
        'textures': {'0': 'create:block/axis', '1': 'create:block/axis_top', 'particle': 'create:block/axis'},
        'elements': [{'from': [6, 6, 13], 'to': [10, 10, 16], 'faces': {
            'north': {'uv': [6, 6, 10, 10], 'rotation': 180, 'texture': '#1'},
            'east': {'uv': [6, 0, 10, 3], 'rotation': 270, 'texture': '#0'},
            'south': {'uv': [6, 6, 10, 10], 'texture': '#1'},
            'west': {'uv': [6, 0, 10, 3], 'rotation': 90, 'texture': '#0'},
            'up': {'uv': [6, 0, 10, 3], 'rotation': 180, 'texture': '#0'},
            'down': {'uv': [6, 0, 10, 3], 'texture': '#0'}}}]})
    write_json(asset('models', 'item', 'crucible.json'), {
        'parent': 'minecraft:block/block', 'textures': textures,
        'elements': frame_elements(False) + pot_elements(False)})
    write_json(asset('blockstates', 'crucible.json'), {'variants': {
        f'facing={f},formed={formed}': (
            {'model': 'mcremix:block/crucible/formed'} if formed == 'true'
            else dict({'model': 'mcremix:block/crucible/frame'}, **({'y': 90} if f in ('east', 'west') else {})))
        for f in ('north', 'east', 'south', 'west') for formed in ('false', 'true')}})
    write_json(asset('models', 'item', 'unfired_crucible.json'), {
        'parent': 'mcremix:block/crucible/pot',
        'textures': {'pot': 'mcremix:block/unfired_crucible', 'inside': 'mcremix:block/unfired_crucible',
                     'particle': 'mcremix:block/unfired_crucible'}})
    # a mold with metal in it shows the metal, in the metal's colour (McRemixClient)
    for name in ('sand_ingot_mold', 'sand_block_mold'):
        write_json(asset('models', 'item', name + '.json'), {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'},
            'overrides': [{'predicate': {'mcremix:filled': 1}, 'model': f'mcremix:item/{name}_filled'}]})
        write_json(asset('models', 'item', name + '_filled.json'), {
            'parent': 'minecraft:item/generated',
            'textures': {'layer0': f'mcremix:item/{name}', 'layer1': f'mcremix:item/{name}_metal'}})

# what each metal's ingot, nugget, block, sheet, raw ore and crushed ore are called, as tags or items; None where
# there is none. The crucible melts them all, an ingot's worth (90 mB) apiece, a block's nine
METAL_FORMS = {
    'iron': {'nugget': True, 'sheet': True, 'raw': True},
    'gold': {'nugget': True, 'sheet': True, 'raw': True},
    'copper': {'nugget': True, 'sheet': True, 'raw': True},
    'zinc': {'nugget': True, 'sheet': False, 'raw': True},
    'brass': {'nugget': True, 'sheet': True, 'raw': False},
    'steel': {'nugget': True, 'sheet': True, 'raw': False},
    'netherite': {'nugget': False, 'sheet': False, 'raw': False},
    'aluminium': {'nugget': True, 'sheet': True, 'raw': False},
}
# patterns pressed into sand for a mold: anything ingot-shaped, brick included, or block-shaped
INGOT_PATTERNS = ['minecraft:iron_ingot', 'minecraft:gold_ingot', 'minecraft:copper_ingot', 'create:zinc_ingot',
                  'create:brass_ingot', 'mcremix:steel_ingot', 'minecraft:netherite_ingot', 'minecraft:brick',
                  'mcremix:aluminium_ingot']
BLOCK_PATTERNS = ['minecraft:iron_block', 'minecraft:gold_block', 'minecraft:copper_block', 'create:zinc_block',
                  'create:brass_block', 'mcremix:steel_block', 'minecraft:netherite_block', 'minecraft:bricks',
                  'mcremix:aluminium_block']


def melting(path, ingredient, metal, amount):
    recipe('melting/' + path, {'type': 'mcremix:melting', 'ingredient': ingredient,
                               'result': {'fluid': f'mcremix:molten_{metal}', 'amount': amount}})


# the ingots in each piece of gear, as crafted
GEAR_INGOTS = {'helmet': 5, 'chestplate': 8, 'leggings': 7, 'boots': 4,
               'sword': 2, 'pickaxe': 3, 'axe': 3, 'shovel': 1, 'hoe': 2}
# Create's copper diving gear as crafted (the backtank is a block and three ingots)
CREATE_DIVING_GEAR = {'diving_helmet': 5, 'diving_boots': 4, 'backtank': 12}


def gear_melting():
    """Tools and armour melt back into half the metal they were made of (45 mB an ingot), worn or not."""
    def gear(path, ingredient, metal, ingots):
        melting(path, ingredient, metal, 45 * ingots)

    for metal, prefix in (('iron', 'iron'), ('gold', 'golden')):
        for piece, ingots in GEAR_INGOTS.items():
            gear(f'{prefix}_{piece}', item(f'minecraft:{prefix}_{piece}'), metal, ingots)
        # horse armour has no recipe; it holds a chestplate's worth
        gear(f'{prefix}_horse_armor', item(f'minecraft:{prefix}_horse_armor'), metal, GEAR_INGOTS['chestplate'])
    gear('shears', item('minecraft:shears'), 'iron', 2)
    # chainmail has no recipe either, and is as much iron as iron armour
    for piece in ('helmet', 'chestplate', 'leggings', 'boots'):
        gear(f'chainmail_{piece}', item(f'minecraft:chainmail_{piece}'), 'iron', GEAR_INGOTS[piece])
    for piece, ingots in CREATE_DIVING_GEAR.items():
        gear(f'copper_{piece}', item(f'create:copper_{piece}'), 'copper', ingots)
    # netherite gear is made of one netherite ingot; the diamonds or copper under it are lost
    for piece in GEAR_INGOTS:
        gear(f'netherite_{piece}', item(f'minecraft:netherite_{piece}'), 'netherite', 1)
    for piece in CREATE_DIVING_GEAR:
        gear(f'netherite_{piece}', item(f'create:netherite_{piece}'), 'netherite', 1)


def gen_crucible_data():
    write_json(data('mcremix', 'loot_tables', 'blocks', 'crucible.json'), loot_self('crucible'))
    for metal, forms in METAL_FORMS.items():
        melting(f'{metal}_ingot', {'tag': f'forge:ingots/{metal}'}, metal, 90)
        melting(f'{metal}_block', {'tag': f'forge:storage_blocks/{metal}'}, metal, 810)
        if forms['nugget']:
            melting(f'{metal}_nugget', {'tag': f'forge:nuggets/{metal}'}, metal, 10)
        if forms['sheet']:
            melting(f'{metal}_sheet', {'tag': f'forge:plates/{metal}'}, metal, 90)
        if forms['raw']:
            melting(f'raw_{metal}', {'tag': f'forge:raw_materials/{metal}'}, metal, 90)
            melting(f'raw_{metal}_block', {'tag': f'forge:storage_blocks/raw_{metal}'}, metal, 810)
            melting(f'crushed_raw_{metal}', item(f'create:crushed_raw_{metal}'), metal, 90)
    # blister steel only needs the press to beat it into ingots, but melts as well as any steel
    melting('blister_steel', item('mcremix:blister_steel'), 'steel', 90)
    gear_melting()

    def alloying(name, fluids, result, amount, heat, time, items=()):
        recipe('alloying/' + name, {
            'type': 'mcremix:alloying',
            'fluids': [{'fluid': f'mcremix:molten_{m}', 'amount': a} for m, a in fluids],
            'items': [{'ingredient': i, 'count': c} for i, c in items],
            'result': {'fluid': f'mcremix:molten_{result}', 'amount': amount}, 'heat': heat, 'time': time})
    # Create mixes brass from an ingot each of copper and zinc
    alloying('brass', [('copper', 90), ('zinc', 90)], 'brass', 180, 'searing', 40)
    # crucible steel: four ingots of iron and a coke, in a quarter of the time a cementation pack takes
    alloying('steel', [('iron', 360)], 'steel', 360, 'blazing', 200, items=[(item('mcremix:coal_coke'), 1)])
    # netherite as vanilla makes it: four gold ingots and four scrap for an ingot
    alloying('netherite', [('gold', 360)], 'netherite', 90, 'blazing', 200,
             items=[(item('minecraft:netherite_scrap'), 4)])

    # nine steel ingots make a block of steel, as any metal's do, and it breaks back down; aluminium the same
    for metal in ('steel', 'aluminium'):
        recipe(f'{metal}_block', {
            'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['III', 'III', 'III'],
            'key': {'I': {'tag': f'forge:ingots/{metal}'}}, 'result': item(f'mcremix:{metal}_block')})
        recipe(f'{metal}_ingot_from_block', {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [item(f'mcremix:{metal}_block')],
            'result': item(f'mcremix:{metal}_ingot', 9)})

    # a crucible is shaped from clay like a pot and fired Hot
    recipe('unfired_crucible', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['C C', 'C C', 'CCC'],
        'key': {'C': item('minecraft:clay_ball')}, 'result': item('mcremix:unfired_crucible')})
    firing('crucible', item('mcremix:unfired_crucible'), 'mcremix:crucible', 'hot', 400)

    # sand molds: a Mechanical Press rams sand round a pattern in a basin, and the pattern comes back out
    for pattern in INGOT_PATTERNS:
        recipe(f'compacting/sand_ingot_mold_from_{pattern.split(":")[1]}', {
            'type': 'create:compacting', 'ingredients': [item('minecraft:sand'), item(pattern)],
            'results': [item('mcremix:sand_ingot_mold'), item(pattern)]})
    for pattern in BLOCK_PATTERNS:
        recipe(f'compacting/sand_block_mold_from_{pattern.split(":")[1]}', {
            'type': 'create:compacting', 'ingredients': [item('minecraft:sand')] * 4 + [item(pattern)],
            'results': [item('mcremix:sand_block_mold'), item(pattern)]})


# what rust does to this mod's iron pipes, pumps and tanks (Leaks): rusted through, they drip, and leak only with the
# server config's rustedIronLeaks on (0.22.1)
# what rain does to steel ones (Taran, 0.28.0: steel rusts, but only to the first stage)
STEEL_RUST = 'Left wet it gets _spotted with rust_, but never worse, so it never leaks. _Wax_ it to keep it bright.'
IRON_RUST = ('Left wet it _rusts_, and rusted through it _drips_; with the _rustedIronLeaks_ setting on it _leaks_ too, '
             '_1 mB a tick_ for every rusted block{}. Scrape it with an _axe_, clean it with _acid_ or _wax_ it to stop '
             'that.')
# what heat does to aluminium ones (Overheating)
ALUMINIUM_MELTS = ('Never weathers, but holds fluids only up to _Warm_: with anything _Hot_ inside or beside it (lava, '
                   'fire, a lit Furnace, a Blaze Burner, molten metal) it _melts_ into rough aluminium.')


def fluid_lang():
    lang = {
        'mcremix.tooltip.fluid_ceiling': 'Holds fluids up to %s',
        'mcremix.boiler.pressure': 'Pressure',
        'mcremix.boiler.venting': 'Venting spare steam through its whistle',
        'mcremix.boiler.building': 'Steam for %s more engines is building pressure',
        'death.attack.mcremix.scalding': '%1$s was scalded by a bursting boiler',
        'death.attack.mcremix.scalding.player': '%1$s was scalded by a bursting boiler while fighting %2$s',
    }
    for m, _ in MOLTEN:
        name = title_case(m)
        lang[f'fluid.mcremix.molten_{m}'] = f'Molten {name}'
        lang[f'block.mcremix.molten_{m}'] = f'Molten {name}'
        lang[f'item.mcremix.molten_{m}_bucket'] = f'Molten {name} Bucket'
        lang[f'block.mcremix.rough_{m}'] = f'Rough {name}'
        lang[f'block.mcremix.rough_{m}.tooltip.summary'] = (
            f'Molten {m} that _set where it spilled_. Mine it for the {m} back: an ingot for every _90 mB_ in it, '
            'the rest as nuggets.')
    lang['block.mcremix.steel_block'] = 'Block of Steel'
    lang['block.mcremix.aluminium_block'] = 'Block of Aluminium'

    for m in ('brass', 'iron', 'aluminium'):
        name = title_case(m)
        lang[f'block.mcremix.{pipe_name(m)}'] = f'{name} Fluid Pipe'
        lang[f'block.mcremix.{pump_name(m)}'] = f'{name} Mechanical Pump'
        lang[f'block.mcremix.{tank_name(m)}'] = f'{name} Fluid Tank'
    lang['block.mcremix.brass_fluid_pipe.tooltip.summary'] = (
        'Thin and fine, like brass clockwork, and holds fluids up to _Hot_, like lava. A line with a brass pipe in it '
        'carries only _half_ as much.')
    lang['block.mcremix.brass_mechanical_pump.tooltip.summary'] = (
        'Turns no faster than _96 RPM_, like every brass support. Holds fluids up to _Hot_.')
    lang['block.mcremix.brass_mechanical_pump.tooltip.condition1'] = 'Its handwheel'
    lang['block.mcremix.brass_mechanical_pump.tooltip.behaviour1'] = (
        'Sets the _most it lets through_, from _1 to 24 mB a tick_, all it moves at 96 RPM. _Hold right-click_ on the '
        'handwheel to set it.')
    lang['mcremix.pump.max_flow'] = 'Max Flow (mB/t)'
    lang['mcremix.pump.max_flow.row'] = 'mB/t'
    lang['mcremix.pump.max_flow.value'] = '%s mB/t'
    lang['block.mcremix.brass_fluid_tank.tooltip.summary'] = (
        'Holds fluids up to _Hot_ and makes a _boiler_ as Create\'s tanks do, for _Brass Steam Engines_. Brass tanks '
        'make a vat like any other, but _no mixer_ fits one.')
    rust = IRON_RUST
    lang['block.mcremix.iron_fluid_pipe.tooltip.summary'] = (
        'Holds fluids up to _Hot_, like copper. ' + rust.format(' while fluid goes through'))
    lang['block.mcremix.iron_mechanical_pump.tooltip.summary'] = (
        'Turns up to _256 RPM_ like Create\'s, and holds fluids up to _Hot_. ' + rust.format(' while fluid goes through'))
    lang['block.mcremix.iron_fluid_tank.tooltip.summary'] = (
        'Holds fluids up to _Hot_ and makes a _boiler_ as Create\'s tanks do, for _Iron Steam Engines_. '
        + rust.format(' while it holds anything'))
    melts = ALUMINIUM_MELTS
    lang['block.mcremix.aluminium_fluid_pipe.tooltip.summary'] = 'Light and soft. ' + melts
    lang['block.mcremix.aluminium_mechanical_pump.tooltip.summary'] = (
        'Turns up to _512 RPM_ on a _polyethylene bushing_, and _heats up_ turning fast, as aluminium supports '
        'do. ' + melts)
    lang['block.mcremix.aluminium_fluid_tank.tooltip.summary'] = (
        'Makes _no boiler_, so no steam engine goes on it, and a vat of aluminium tanks takes only an _aluminium '
        'mixer_. ' + melts)
    lang['block.mcremix.glass_pipe'] = 'Glass Pipe'
    lang['block.mcremix.glass_pipe.tooltip.summary'] = (
        'Shows the fluid _running through it_ and never corrodes, but carries only _a quarter_ as much as copper. '
        'Holds fluids up to _Hot_, like lava.')
    lang['block.mcremix.steel_fluid_pipe'] = 'Steel Fluid Pipe'
    lang['block.mcremix.steel_fluid_pipe.tooltip.summary'] = (
        'Holds fluids up to _Searing_: molten copper, gold and brass. Iron, steel and netherite are too hot for any '
        'pipe: _pour_ them from a crucible. ' + STEEL_RUST)
    lang['block.mcremix.steel_mechanical_pump'] = 'Steel Mechanical Pump'
    lang['block.mcremix.steel_mechanical_pump.tooltip.summary'] = (
        'Turns up to _512 RPM_, twice what Create\'s pump takes, and holds fluids up to _Searing_. ' + STEEL_RUST)
    lang['block.mcremix.steel_fluid_tank'] = 'Steel Fluid Tank'
    lang['block.mcremix.steel_fluid_tank.tooltip.summary'] = (
        'Holds fluids up to _Searing_. As a boiler for _Steel Steam Engines_ it boils no water and wants no fire: it '
        'runs only on _pressurised steam_ piped in, every _10 mB a tick_ a level. It is _compact_, a level for every '
        '_two_ tanks, but steam its engines do not use builds _pressure_. ' + STEEL_RUST)
    lang['block.mcremix.steel_fluid_tank.tooltip.condition1'] = 'With a steam whistle'
    lang['block.mcremix.steel_fluid_tank.tooltip.behaviour1'] = 'The whistle _vents_ the spare steam, screaming.'
    lang['block.mcremix.steel_fluid_tank.tooltip.condition2'] = 'Without one'
    lang['block.mcremix.steel_fluid_tank.tooltip.behaviour2'] = (
        'It hisses and groans, then _bursts_: its tanks fall, what they held is lost and the steam _scalds_ whoever '
        'is near.')

    lang.update({
        'block.mcremix.crucible': 'Crucible',
        'block.mcremix.crucible.tooltip.summary': (
            'Melts metal, ore, and metal _tools and armour_ (into half their metal) with the heat _underneath_ it, '
            'and _tips over_ as its shaft turns to pour. _27 placed as a cube_ join into one big crucible.'),
        'block.mcremix.crucible.tooltip.condition1': 'Heat',
        'block.mcremix.crucible.tooltip.behaviour1': (
            'Zinc and aluminium melt _Hot_; copper, gold and brass _Searing_; iron, steel and netherite need a '
            '_superheated_ Blaze Burner. A big crucible needs heat under _every column_, and melts nine times as fast.'),
        'block.mcremix.crucible.tooltip.condition2': 'Turning its shaft',
        'block.mcremix.crucible.tooltip.behaviour2': (
            '_A full turn_ lays it on its side, and turning back stands it up. It pours whatever rises _over the lip_ '
            'onto what is in front: a _sand mold_ on a depot or belt, a tank, another crucible, or the ground.'),
        'block.mcremix.crucible.tooltip.condition3': 'Alloys',
        'block.mcremix.crucible.tooltip.behaviour3': (
            '_Copper and zinc_ join as brass; _iron and coal coke_ as steel; _gold and netherite scrap_ as '
            'netherite.'),
        'item.mcremix.unfired_crucible': 'Unfired Crucible',
        'item.mcremix.unfired_crucible.tooltip.summary': 'Clay shaped into a pot. Fire it _Hot_ in a furnace for a '
                                                         '_Crucible_.',
        'item.mcremix.sand_ingot_mold': 'Sand Ingot Mold',
        'item.mcremix.sand_ingot_mold.tooltip.summary': (
            'Sand rammed round an ingot, or a brick, by a _Mechanical Press_ in a basin. Under a crucible\'s stream '
            'it takes _90 mB_ of metal; full, it _sets_, and an _ingot_ breaks out of the sand.'),
        'item.mcremix.sand_block_mold': 'Sand Block Mold',
        'item.mcremix.sand_block_mold.tooltip.summary': (
            'Sand rammed round a block by a _Mechanical Press_ in a basin. Under a crucible\'s stream it takes '
            '_810 mB_ of metal; full, it _sets_, and a _block_ breaks out of the sand.'),
        'mcremix.mold.empty': 'Empty: takes %s mB',
        'mcremix.mold.holds': 'Holds %s of %s mB of %s',
        'mcremix.mold.setting': 'Full of %s, setting',
        'mcremix.goggles.crucible.big': 'Big Crucible',
        'mcremix.goggles.crucible.every_column': 'Every column needs heat beneath it',
        'mcremix.goggles.crucible.holds': 'Holds %s of %s mB',
        'mcremix.goggles.crucible.alloying': 'Making %s: %s%%',
        'mcremix.goggles.crucible.tipped': 'Tipped %s\u00b0',
        'mcremix.goggles.crucible.pouring': 'Tipped %s\u00b0, pouring',
    })

    # Create's copper pipes, pumps and tanks, dropped weathered or waxed, say so under their name; iron ones and iron
    # and steel machines say how rusted they are
    for waxed in ('', 'waxed_'):
        for stage in ([''] if waxed else []) + ['exposed', 'weathered', 'oxidized']:
            key = (waxed + stage).rstrip('_')
            if key:
                lang[f'mcremix.patina.{key}'] = title_case(key)
                lang[f'mcremix.rust.{key}'] = title_case(key.replace('oxidized', 'rusted'))
    return lang


def gen_fluid_part_recipes():
    """Pipes, pumps and tanks made as Create makes its copper ones, in their own metal: a sheet either side of an ingot
    makes four pipes, a pipe and the metal's cog a pump, and a sheet above and below a barrel a tank. The aluminium pump
    takes a polyethylene bushing too, as every aluminium support does. Glass pipes are panes either side of a
    block of glass. Create's chute is iron's sheet, ingot and sheet upright and its item vault iron's sheet, barrel and
    sheet, so iron pipes are made only across and an iron tank has its sheets either side of the barrel."""
    for m in FLUID_MATERIALS:
        pipe = f'mcremix:{pipe_name(m)}'
        if m == 'glass':
            keys = {'S': {'tag': 'forge:glass_panes/colorless'}, 'C': {'tag': 'forge:glass/colorless'}}
        else:
            keys = {'S': {'tag': f'forge:plates/{m}'}, 'C': {'tag': f'forge:ingots/{m}'}}
        patterns = [(pipe_name(m), ['SCS'])]
        if m != 'iron':
            patterns.append((pipe_name(m) + '_vertical', ['S', 'C', 'S']))
        for name, pattern in patterns:
            recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': keys,
                          'result': item(pipe, 4)})
        if m in PUMP_MATERIALS:
            ingredients = [item(f'mcremix:{m}_cogwheel'), item(pipe)]
            if m == 'aluminium':
                ingredients.append({'tag': 'mcremix:polyethylene'})
            recipe(pump_name(m), {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                  'ingredients': ingredients, 'result': item(f'mcremix:{pump_name(m)}')})
        if m in ELECTRIC_PUMP_MATERIALS:
            recipe(electric_pump_name(m), {
                'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                'ingredients': [item('powergrid:electric_motor'), item(pipe)],
                'result': item(f'mcremix:{electric_pump_name(m)}')})
        if m in TANK_MATERIALS:
            pattern = ['SBS'] if m == 'iron' else ['S', 'B', 'S']
            recipe(tank_name(m), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
                                  'key': {'S': {'tag': f'forge:plates/{m}'}, 'B': {'tag': 'forge:barrels/wooden'}},
                                  'result': item(f'mcremix:{tank_name(m)}')})


def gen_fluid_data():
    for m in FLUID_MATERIALS:
        for block in fluid_blocks(m):
            name = block.split(':')[1]
            write_json(data('mcremix', 'loot_tables', 'blocks', name + '.json'), loot_self(name))
    write_json(data('mcremix', 'loot_tables', 'blocks', 'steel_block.json'), loot_self('steel_block'))
    write_json(data('mcremix', 'loot_tables', 'blocks', 'aluminium_block.json'), loot_self('aluminium_block'))
    gen_crucible_data()

    # how hot a fluid each part holds (FluidHeat.ceiling); untagged blocks hold anything
    write_json(data('mcremix', 'tags', 'blocks', 'fluid_ceiling', 'cold.json'), tag(PVC_BLOCKS))
    write_json(data('mcremix', 'tags', 'blocks', 'fluid_ceiling', 'warm.json'), tag(fluid_blocks('aluminium')))
    write_json(data('mcremix', 'tags', 'blocks', 'fluid_ceiling', 'hot.json'),
               tag(CREATE_FLUID_BLOCKS + fluid_blocks('glass') + fluid_blocks('brass') + fluid_blocks('iron')))
    write_json(data('mcremix', 'tags', 'blocks', 'fluid_ceiling', 'searing.json'), tag(fluid_blocks('steel')))
    gen_fluid_part_recipes()
    # how hot each fluid is (FluidHeat.of); fluids with no tag go by their temperature
    for heat in ('warm', 'hot', 'searing', 'blazing'):
        fluids = [f'mcremix:{flow}molten_{m}' for m, h in MOLTEN if h == heat for flow in ('', 'flowing_')]
        if heat == 'hot':
            fluids = ['minecraft:lava', 'minecraft:flowing_lava'] + fluids
        write_json(data('mcremix', 'tags', 'fluids', 'heat', heat + '.json'), tag(fluids))

    # rough metal needs the pickaxe its metal's block needs
    write_json(data('minecraft', 'tags', 'blocks', 'needs_stone_tool.json'),
               tag([f'mcremix:rough_{m}' for m in ('iron', 'copper', 'zinc', 'brass', 'steel', 'aluminium')]
                   + ['mcremix:aluminium_block']))
    write_json(data('minecraft', 'tags', 'blocks', 'needs_iron_tool.json'),
               tag(['mcremix:rough_gold', 'mcremix:steel_block', 'mcremix:titanium_block']
                   + titanium_plating_blocks()))
    write_json(data('minecraft', 'tags', 'blocks', 'needs_diamond_tool.json'), tag(['mcremix:rough_netherite']))
    write_json(data('forge', 'tags', 'blocks', 'storage_blocks', 'steel.json'), tag(['mcremix:steel_block']))
    write_json(data('forge', 'tags', 'items', 'storage_blocks', 'steel.json'), tag(['mcremix:steel_block']))
    for kind in ('blocks', 'items'):
        write_json(data('forge', 'tags', kind, 'storage_blocks', 'aluminum.json'), tag(['mcremix:aluminium_block']))
        write_json(data('forge', 'tags', kind, 'storage_blocks', 'aluminium.json'),
                   tag(['mcremix:aluminium_block', {'id': '#forge:storage_blocks/aluminum', 'required': False}]))
    write_json(data('minecraft', 'tags', 'blocks', 'beacon_base_blocks.json'),
               tag(['mcremix:steel_block', 'mcremix:aluminium_block', 'mcremix:titanium_block']))
    # what a bursting steel boiler does to whoever is near (PressureBoilerData)
    write_json(data('mcremix', 'damage_type', 'scalding.json'),
               {'message_id': 'mcremix.scalding', 'scaling': 'when_caused_by_living_non_player', 'exhaustion': 0.1,
                'effects': 'burning'})
    # chlorine choking whoever is in its cloud (ChlorineCloud); armour does nothing for it
    write_json(data('mcremix', 'damage_type', 'chlorine.json'),
               {'message_id': 'mcremix.chlorine', 'scaling': 'when_caused_by_living_non_player', 'exhaustion': 0.0})
    write_json(data('minecraft', 'tags', 'damage_type', 'bypasses_armor.json'), tag(['mcremix:chlorine']))
    # where water taken by bucket, pump or hose pulley is salt water, and where it is cold water (WildWater)
    write_json(data('mcremix', 'tags', 'worldgen', 'biome', 'salt_water.json'),
               tag(['#minecraft:is_ocean', '#minecraft:is_beach']))
    # every snowy biome, whatever its climate says: WWOO gives snowy plains, taigas and beaches patches where it rains,
    # and ice spikes, groves and frozen rivers rain all over
    write_json(data('mcremix', 'tags', 'worldgen', 'biome', 'cold_water.json'),
               tag(['#forge:is_snowy', 'minecraft:frozen_river']))

# ------------------------------------------------------------------------------------------------ steam engines

# every metal whose tanks make a boiler has a steam engine of its own (FluidPartMaterial.hasEngine); aluminium tanks
# make none
ENGINE_MATERIALS = ['brass', 'iron', 'steel']
# how Create turns its engine for each way it stands, on the floor, a wall or a ceiling, facing away from its tank:
# (x, y) degrees
ENGINE_TURNS = {'ceiling': {'east': (180, 270), 'north': (180, 180), 'south': (180, 0), 'west': (180, 90)},
                'floor': {'east': (0, 90), 'north': (0, 0), 'south': (0, 180), 'west': (0, 270)},
                'wall': {'east': (90, 90), 'north': (90, 0), 'south': (90, 180), 'west': (90, 270)}}


def engine_name(m):
    return f'{m}_steam_engine'


def gen_steam_engines():
    """Steam engines in brass, iron and steel (Taran, 0.24.0), each going on its own metal's tanks alone
    (MetalSteamEngineBlock): Create's engine with its body in the metal's colours. Its piston and linkage, which Create
    draws as it runs, stay as they are. Made as Create makes its own, on a block of the metal instead of copper."""
    for n, m in enumerate(ENGINE_MATERIALS):
        name = engine_name(m)
        sprite = f'mcremix:block/{name}'
        save_png(recolour('engine', m, 1500 + n), asset('textures', 'block', name + '.png'))
        write_json(asset('models', 'block', name + '.json'), {
            'parent': 'create:block/steam_engine/block', 'textures': {'1': sprite, 'particle': sprite}})
        write_json(asset('models', 'item', name + '.json'), {
            'parent': 'create:block/steam_engine/item',
            'textures': {'0': 'create:block/cam_linkage', '1': sprite, 'particle': sprite}})
        variants = {}
        for face, turns in ENGINE_TURNS.items():
            for facing, (x, y) in turns.items():
                variant = {'model': f'mcremix:block/{name}'}
                if x:
                    variant['x'] = x
                if y:
                    variant['y'] = y
                for waterlogged in ('false', 'true'):
                    variants[f'face={face},facing={facing},waterlogged={waterlogged}'] = variant
        write_json(asset('blockstates', name + '.json'), {'variants': variants})
        write_json(data('mcremix', 'loot_tables', 'blocks', name + '.json'), loot_self(name))
        recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['P', 'A', 'B'],
                      'key': {'P': {'tag': 'forge:plates/gold'}, 'A': item('create:andesite_alloy'),
                              'B': {'tag': f'forge:storage_blocks/{m}'}},
                      'result': item(f'mcremix:{name}')})


# the boiler gauges drawn in other metals, by the prefix of their names: each engine metal's, iron's at each stage of
# rust, Create's copper weathered and steel's one stage of rust, each with the palette it is drawn in
BOILER_GAUGES = dict([(f'{m}_', m) for m in ENGINE_MATERIALS]
                     + [(f'{stage}_iron_', f'{stage}_iron') for stage in RUST_STAGES]
                     + [(f'{stage}_', f'{stage}_copper') for stage in ('exposed', 'weathered', 'oxidized')]
                     + [('exposed_steel_', 'exposed_steel')])


# the bright fittings on Create's boiler gauge, darkest first: the post holding its needle and the bands round its
# casing's sides (its dial face has warm shades of its own, which stay)
GAUGE_FITTINGS = [(175, 102, 79), (191, 121, 85), (206, 137, 90), (228, 157, 99), (247, 175, 108), (251, 195, 125),
                  (255, 212, 140)]


def boiler_gauge_texture(palette, seed):
    """Create's boiler gauge in a metal: its copper casing moved along the metal's palette (recolour), and its bright
    fittings, which recolour leaves, along the light end of it, so that only its glass and dial face are as they were."""
    img = recolour('boiler_gauge', palette, seed, greys=False)
    for y in range(img.height):
        for x in range(img.width):
            px = img.getpixel((x, y))
            if px[3] and px[:3] in GAUGE_FITTINGS and (x >= 16 or y < 16):
                t = 0.4 + 0.6 * GAUGE_FITTINGS.index(px[:3]) / (len(GAUGE_FITTINGS) - 1)
                img.putpixel((x, y), ramp(PALETTES[palette], t) + (255,))
    return img


def gen_boiler_gauges():
    """The gauge a boiler shows once an engine is on it, in the metal of its tanks (Taran, 0.26.0;
    ModPartialModels.boilerGauge): Create's copper casing and the bright fittings on it in the metal's colours, rusting
    with iron tanks and weathering with Create's copper ones, and its glass and dial face as they are."""
    for n, (prefix, palette) in enumerate(BOILER_GAUGES.items()):
        sprite = f'mcremix:block/{prefix}boiler_gauge'
        save_png(boiler_gauge_texture(palette, 1600 + n), asset('textures', 'block', f'{prefix}boiler_gauge.png'))
        for part in ('gauge', 'gauge_dial'):
            write_json(asset('models', 'block', 'steam_engine', f'{prefix}{part}.json'), {
                'parent': f'create:block/steam_engine/{part}', 'textures': {'0': sprite, 'particle': sprite}})


def steam_engine_lang():
    lang = {'mcremix.steam_engine.fits': '%s goes only on %s boilers'}
    for m in ENGINE_MATERIALS:
        name = title_case(m)
        lang[f'block.mcremix.{engine_name(m)}'] = f'{name} Steam Engine'
        lang[f'block.mcremix.{engine_name(m)}.tooltip.summary'] = (
            f'Create\'s steam engine in {m}. It runs as Create\'s does, as fast and as strong from each level of its '
            f'boiler, but goes only on _{name} Fluid Tanks_.')
    lang['block.mcremix.steel_steam_engine.tooltip.summary'] += (
        ' A steel boiler runs only on _pressurised steam_ piped in. It is _compact_, but _bursts_ unless a whistle '
        'vents the steam its engines do not use.')
    return lang


# Power Grid's electric pump turned to face each way, as its own blockstate turns it
ELECTRIC_PUMP_TURNS = {'down': {'x': 90}, 'east': {'y': 90}, 'north': {}, 'south': {'y': 180}, 'up': {'x': 270},
                       'west': {'y': 270}}
# the stage of rust an iron part's patina shows, by its patina (Patina): its texture's prefix
PATINA_RUST = {'none': '', 'exposed': 'exposed_', 'weathered': 'weathered_', 'oxidized': 'rusted_'}
# steel rusts only to its first stage (Taran, 0.28.0), and steel that went further before looks as rusty as that
STEEL_PATINA_RUST = {'none': '', 'exposed': 'exposed_', 'weathered': 'exposed_', 'oxidized': 'exposed_'}


def rust_looks(m):
    """How a metal's pipes and pumps look at each patina, by their textures' prefixes, or None if it never rusts."""
    return {'iron': PATINA_RUST, 'steel': STEEL_PATINA_RUST}.get(m)


def gen_electric_pumps():
    """Power Grid's electric pump in iron, steel and aluminium (Taran, 0.24.0; MetalElectricPumpBlock), and brass until
    0.26.0: its
    model, with its copper body in the metal's colours and its grey motor as it is. Iron ones rust a stage at a time,
    wax or not, as iron pumps do, and steel ones to their first stage."""
    for m in ELECTRIC_PUMP_MATERIALS:
        n = PUMP_MATERIALS.index(m)  # seeded as when there was a brass one
        name = electric_pump_name(m)
        looks = rust_looks(m)
        for k, prefix in enumerate(dict.fromkeys((looks or {'none': ''}).values())):
            palette = prefix + m if prefix else m
            save_png(recolour('electric_pump', palette, 1510 + 10 * n + k, greys=False),
                     asset('textures', 'block', prefix + name + '.png'))
            sprite = f'mcremix:block/{prefix}{name}'
            write_json(asset('models', 'block', prefix + name + '.json'), {
                'parent': 'powergrid:block/electric_pump', 'textures': {'0': sprite, 'particle': sprite}})
        write_json(asset('models', 'item', name + '.json'), {'parent': f'mcremix:block/{name}'})
        variants = {}
        for facing, turn in ELECTRIC_PUMP_TURNS.items():
            if not looks:
                variants[f'facing={facing}'] = dict({'model': f'mcremix:block/{name}'}, **turn)
                continue
            for waxed in ('', 'waxed_'):
                for patina, prefix in looks.items():
                    value = waxed + patina if patina != 'none' else waxed.rstrip('_') or 'none'
                    variants[f'facing={facing},patina={value}'] = dict({'model': f'mcremix:block/{prefix}{name}'},
                                                                        **turn)
        write_json(asset('blockstates', name + '.json'), {'variants': variants})


def electric_pump_lang():
    lang = {}
    ceilings = {'iron': 'Hot', 'steel': 'Searing', 'aluminium': 'Warm'}
    speeds = {'iron': 256, 'steel': 512, 'aluminium': 512}
    for m in ELECTRIC_PUMP_MATERIALS:
        name = title_case(m)
        lang[f'block.mcremix.{electric_pump_name(m)}'] = f'{name} Electric Pump'
        lang[f'block.mcremix.{electric_pump_name(m)}.tooltip.summary'] = (
            f'Power Grid\'s _Electric Pump_ in {m}. It pumps as Power Grid\'s does, faster the more _power_ it takes, '
            f'but only up to _{speeds[m]} RPM_, as fast as {m} mechanical pumps turn, and it _overheats_ on much '
            f'more power than that takes.' + ('' if m == 'aluminium' else f' Holds fluids up to _{ceilings[m]}_.'))
    lang['block.mcremix.iron_electric_pump.tooltip.summary'] += ' ' + IRON_RUST.format(' while fluid goes through')
    lang['block.mcremix.steel_electric_pump.tooltip.summary'] += ' ' + STEEL_RUST
    lang['block.mcremix.aluminium_electric_pump.tooltip.summary'] += ' ' + ALUMINIUM_MELTS
    return lang


def gen_steam_engine_ponder():
    """Copper, brass, iron and steel boilers in a row, each two tanks of water high with its own metal's engine on the
    north side (toward the camera) and the engine's shaft two blocks out, and an empty aluminium tank, which makes no
    boiler, at the end. The block entity data makes each tank a boiler with its engine attached, so its gauge shows,
    and tells each shaft its engine."""
    b = {}
    plate(b, 9)
    for x, m in zip((0, 2, 4, 6), ['copper'] + ENGINE_MATERIALS):
        tank = 'create:fluid_tank' if m == 'copper' else f'mcremix:{tank_name(m)}'
        engine = 'create:steam_engine' if m == 'copper' else f'mcremix:{engine_name(m)}'
        controller = (x, 1, 4)
        for y in (1, 2):
            nbt = tank_nbt((x, y, 4), controller, 1, 2, 'minecraft:water', 16000, be=tank, window=False)
            if y == 1:
                nbt['Boiler'][1]['Engines'] = ('int', 1)
            b[(x, y, 4)] = (tank, {'top': str(y == 2).lower(), 'bottom': str(y == 1).lower(), 'shape': 'plain'}, nbt)
        b[(x, 1, 3)] = (engine, {'face': 'wall', 'facing': 'north', 'waterlogged': 'false'},
                        {'id': ('string', 'create:steam_engine' if m == 'copper' else 'mcremix:metal_steam_engine'),
                         'ScrollValue': ('int', 0)})
        b[(x, 1, 1)] = ('create:powered_shaft', {'axis': 'x', 'waterlogged': 'false'},
                        {'id': ('string', 'create:powered_shaft'), 'Direction': ('int', 1),
                         'EnginePos': block_pos_nbt((0, 0, -2)), 'EnginePower': ('float', 0.0),
                         'EngineType': ('string', engine)})
    aluminium = f'mcremix:{tank_name("aluminium")}'
    for y in (1, 2):
        b[(8, y, 4)] = (aluminium, {'top': str(y == 2).lower(), 'bottom': str(y == 1).lower(), 'shape': 'window'},
                        tank_nbt((8, y, 4), (8, 1, 4), 1, 2, be=aluminium))
    save_structure('steam_engine/metals', (9, 3, 9), b)


# ------------------------------------------------------------------------------------------------ electrolysis

# moulded Bakelite, a deep glossy brown: dark, base, light. The Electrolyser's is black, as electrical Bakelite mostly
# was (Taran, 0.26.0: fewer brown pixels on it)
BAKELITE_MOULDED = ((0x3A, 0x1A, 0x0A), (0x62, 0x30, 0x14), (0xB8, 0x74, 0x40))
BLACK_BAKELITE = ((0x0E, 0x0C, 0x0B), (0x20, 0x1C, 0x1A), (0x50, 0x49, 0x44))
# tinned steel, for the clamps an Electrolyser's terminals sit in: dark, base, light
TINNED = ((0x5E, 0x60, 0x66), (0x9C, 0x9E, 0xA4), (0xD8, 0xDA, 0xDE))
# baked carbon, grey-black with a dull sheen; unbaked, browner and matt with the tar still soft in it
CARBON = ((0x1A, 0x1A, 0x1E), (0x38, 0x39, 0x3E), (0x84, 0x86, 0x8E))
GREEN_CARBON = ((0x1C, 0x16, 0x10), (0x36, 0x2D, 0x24), (0x5C, 0x50, 0x42))
# the Electrolyser's terminals: copper studs in the red and blue of Power Grid's + and -. dark, base, light
TERMINAL_COPPER = ((0xA4, 0x4C, 0x2F), (0xD6, 0x7B, 0x5B), (0xF4, 0xA6, 0x80))
TERMINAL_RED = ((0x8A, 0x16, 0x16), (0xC8, 0x2A, 0x2A), (0xF2, 0x64, 0x5A))
TERMINAL_BLUE = ((0x1A, 0x3A, 0x8A), (0x2C, 0x5E, 0xCC), (0x6E, 0xA0, 0xF4))


def shade(palette, t):
    """A colour from dark (0) through base (0.5) to light (1)."""
    dark, base, light = palette
    t = max(0.0, min(1.0, t))
    return mix(dark, base, t * 2) if t < 0.5 else mix(base, light, (t - 0.5) * 2)


def bakelite_texture(seed, palette=BAKELITE_MOULDED):
    """Moulded Bakelite for the Electrolyser's head, faintly mottled where the wood flour in it shows. Rows 0 to 7
    are its top, the moulded edge round it lit; rows 8 to 13 its sides, lit along the rounded top edge and shadowed
    at the foot."""
    rnd = random.Random(seed)
    blobs = [(rnd.uniform(0, 16), rnd.uniform(0, 16), rnd.uniform(1.5, 3.5), rnd.choice((-1, 1))) for _ in range(10)]
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            t = 0.5 + rnd.uniform(-0.03, 0.03)
            for bx, by, r, sign in blobs:
                d = math.hypot(x - bx, y - by)
                if d < r:
                    t += sign * 0.09 * (1 - d / r)
            if y < 8 and (x in (2, 13) or y in (0, 7)):
                t += 0.16
            if y == 8:
                t += 0.22
            if y == 13:
                t -= 0.18
            img.putpixel((x, y), shade(palette, t) + (255,))
    return img


def electrolyser_plate_texture(seed):
    """The steel plate the Electrolyser stands on: brushed along its length. Rows 0 to 11 are its faces, rows 12 and
    13 its edge, lit along the top."""
    base, dark, light, _ = PALETTES['steel']
    rnd = random.Random(seed)
    streak = [rnd.uniform(-0.1, 0.1) for _ in range(16)]
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            t = 0.5 + streak[y] + rnd.uniform(-0.04, 0.04)
            if y == 12:
                t += 0.25
            if y == 13:
                t -= 0.2
            img.putpixel((x, y), shade((dark, base, light), t) + (255,))
    return img


def terminals_texture(seed, copper=TERMINAL_COPPER):
    """The terminals: copper for the studs (rows 0 to 3, the top lit), then the red collar of the + (rows 4 to 7) and
    the blue of the - (rows 8 to 11), each with a lit rim round the top and a darker band for its sides at x 4 to 7.
    The collars' tops have a dark hole in the middle, where an Electrolyser's electrode stands. The studs' copper is
    dark, base and light, as shade() takes it."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            n = rnd.uniform(-0.04, 0.04)
            if y < 4:
                col = shade(copper, (0.78 if y < 2 else 0.42 - 0.1 * (y - 2)) + n)
            elif y < 12:
                palette = TERMINAL_RED if y < 8 else TERMINAL_BLUE
                row = (y - 4) % 4
                if x in (1, 2) and row in (1, 2):
                    col = (0x14, 0x12, 0x12)
                elif x < 4:
                    col = shade(palette, (0.68 if x in (0, 3) or row in (0, 3) else 0.55) + n)
                else:
                    col = shade(palette, 0.36 + n)
            else:
                col = shade(copper, 0.5 + n)
            img.putpixel((x, y), col + (255,))
    return img


def electrolyser_poles_texture(seed):
    """The Electrolyser's terminals, laid out as terminals_texture's (Taran, 0.26.0: fewer red, blue and brown
    pixels): tinned steel clamps (rows 0 to 3 and 12 to 15), and collars of black Bakelite, the + (rows 4 to 7) and
    the - (rows 8 to 11) each marked by a single band of red or blue round the top of its side. The collars' tops
    have a dark hole in the middle, where an electrode stands."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            n = rnd.uniform(-0.03, 0.03)
            if y < 4 or y >= 12:
                col = shade(TINNED, (0.72 if y < 2 else 0.45) + n)
            else:
                mark = TERMINAL_RED if y < 8 else TERMINAL_BLUE
                row = (y - 4) % 4
                if x in (1, 2) and row in (1, 2):
                    col = (0x08, 0x07, 0x07)
                elif x < 4:
                    col = shade(BLACK_BAKELITE, (0.66 if x in (0, 3) or row in (0, 3) else 0.5) + n)
                elif row == 0:
                    col = shade(mark, 0.5 + n)
                else:
                    col = shade(BLACK_BAKELITE, 0.42 + n)
            img.putpixel((x, y), col + (255,))
    return img


def carbon_rod_texture(seed):
    """A Carbon Electrode as it hangs: grey-black carbon in faint lengthwise grain, each even column lit and each odd
    one in shadow, so any two columns are a rod's lit and dark sides. Rows 0 and 1 are the cut end; the bottom rows,
    down in the liquid, are pitted where the carbon is burning away."""
    rnd = random.Random(seed)
    grain = [rnd.uniform(-0.06, 0.06) for _ in range(16)]
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if y < 2:
                t = 0.6 + rnd.uniform(-0.05, 0.05)
            else:
                t = (0.56 if x % 2 == 0 else 0.34) + grain[x] + rnd.uniform(-0.05, 0.05)
                if y >= 12 and rnd.random() < 0.3 + 0.1 * (y - 12):
                    t -= rnd.uniform(0.15, 0.3)
            img.putpixel((x, y), shade(CARBON, t) + (255,))
    return img


def electrode_item_texture(seed, palette, stub):
    """A Carbon Electrode as an item: a thick rod lying corner to corner, lit along its upper left side and outlined.
    A baked one has a steel stub at its upper end to hang it by; an unbaked one is grainy with coke."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    ax, ay, bx, by = 3.5, 12.5, 10.5, 5.5

    def capsule(px, py, x0, y0, x1, y1):
        dx, dy = x1 - x0, y1 - y0
        t = max(0.0, min(1.0, ((px - x0) * dx + (py - y0) * dy) / (dx * dx + dy * dy)))
        cross = (dx * (py - y0) - dy * (px - x0)) / math.hypot(dx, dy)
        return math.hypot(px - x0 - t * dx, py - y0 - t * dy), cross
    steel = (STEEL_ITEM[4], STEEL_ITEM[2], STEEL_ITEM[1])
    shape = set()
    for y in range(16):
        for x in range(16):
            d, cross = capsule(x + 0.5, y + 0.5, ax, ay, bx, by)
            if d < 2.1:
                t = 0.5 - 0.42 * cross / 2.1 + rnd.uniform(-0.06, 0.06)
                if not stub and rnd.random() < 0.15:
                    t += rnd.choice((-0.25, 0.3))
                img.putpixel((x, y), shade(palette, t) + (255,))
                shape.add((x, y))
            elif stub:
                d, cross = capsule(x + 0.5, y + 0.5, bx, by, bx + 2.5, by - 2.5)
                if d < 1.0:
                    img.putpixel((x, y), shade(steel, 0.5 - 0.4 * cross + rnd.uniform(-0.05, 0.05)) + (255,))
                    shape.add((x, y))
    outline = mix(palette[0], (0, 0, 0), 0.5)
    for y in range(16):
        for x in range(16):
            if (x, y) not in shape and any((x + dx, y + dy) in shape for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), outline + (255,))
    return img


def electrolyser_model():
    """Drawn fixed to the block under it, and turned onto the side of a basin or vat: a steel plate, a moulded black
    Bakelite head on it, and on the head the + and - terminals, Bakelite collars banded red and blue, each with a tinned
    clamp at its foot and a hole through it, where an electrode stands. ElectrolyserBlock's shape and terminals are the
    same boxes."""
    b, p, t = '#bakelite', '#plate', '#terminals'
    elements = [
        box([1, 0, 3], [15, 2, 13], {
            'down': {'uv': [1, 1, 15, 11], 'texture': p}, 'up': {'uv': [1, 1, 15, 11], 'texture': p},
            'north': {'uv': [1, 12, 15, 14], 'texture': p}, 'south': {'uv': [1, 12, 15, 14], 'texture': p},
            'east': {'uv': [3, 12, 13, 14], 'texture': p}, 'west': {'uv': [3, 12, 13, 14], 'texture': p}}),
        box([2, 2, 4], [14, 8, 12], {
            'up': {'uv': [2, 0, 14, 8], 'texture': b},
            'north': {'uv': [2, 8, 14, 14], 'texture': b}, 'south': {'uv': [2, 8, 14, 14], 'texture': b},
            'east': {'uv': [4, 8, 12, 14], 'texture': b}, 'west': {'uv': [4, 8, 12, 14], 'texture': b}}),
    ]
    sides = ('north', 'south', 'east', 'west')
    for x, row in ((3, 4), (9, 8)):  # the + in red, the - in blue
        # the copper clamp the collar sits in, then the collar, with the electrode's hole in its top
        elements.append(box([x, 8, 6], [x + 4, 9, 10], dict(
            {'up': {'uv': [0, 12, 4, 16], 'texture': t}},
            **{side: {'uv': [0, 3, 4, 4], 'texture': t} for side in sides})))
        elements.append(box([x, 9, 6], [x + 4, 11, 10], dict(
            {'up': {'uv': [0, row, 4, row + 4], 'texture': t}},
            **{side: {'uv': [4, row, 8, row + 2], 'texture': t} for side in sides})))
    return {'parent': 'minecraft:block/block',
            'textures': {'bakelite': 'mcremix:block/electrolyser_bakelite', 'plate': 'mcremix:block/electrolyser_plate',
                         'terminals': 'mcremix:block/electrolyser_poles',
                         'particle': 'mcremix:block/electrolyser_bakelite'},
            'elements': elements}


def electrode_rod_model(x):
    """A Carbon Electrode through the + (anode) or - (cathode) terminal: its cut end stands four pixels out of the
    collar, so what is fitted shows, and it runs on through the head and plate, twelve pixels into the basin or vat
    the Electrolyser is fixed to."""
    r = '#rod'
    return {'textures': {'rod': 'mcremix:block/carbon_electrode', 'particle': 'mcremix:block/carbon_electrode'},
            'elements': [box([x, -12, 7], [x + 2, 15, 9], dict(
                {'down': {'uv': [x, 0, x + 2, 2], 'texture': r}, 'up': {'uv': [x, 0, x + 2, 2], 'texture': r}},
                **{side: {'uv': [x, 2, x + 2, 16], 'texture': r} for side in ('north', 'south', 'east', 'west')}))]}


def gen_electrolysis():
    """The Electrolyser and its Carbon Electrodes, and the recipes that settle and split in basins and vats."""
    save_png(bakelite_texture(1290, BLACK_BAKELITE), asset('textures', 'block', 'electrolyser_bakelite.png'))
    save_png(electrolyser_plate_texture(1291), asset('textures', 'block', 'electrolyser_plate.png'))
    # the arc furnace's and spark plug's terminals; the Electrolyser has poles of its own
    save_png(terminals_texture(1292), asset('textures', 'block', 'electrolyser_terminals.png'))
    save_png(electrolyser_poles_texture(1292), asset('textures', 'block', 'electrolyser_poles.png'))
    save_png(carbon_rod_texture(1293), asset('textures', 'block', 'carbon_electrode.png'))
    save_png(electrode_item_texture(1294, CARBON, True), asset('textures', 'item', 'carbon_electrode.png'))
    save_png(electrode_item_texture(1295, GREEN_CARBON, False),
             asset('textures', 'item', 'unbaked_carbon_electrode.png'))
    write_json(asset('models', 'block', 'electrolyser.json'), electrolyser_model())
    for side, x in (('anode', 4), ('cathode', 10)):
        write_json(asset('models', 'block', f'electrolyser_{side}.json'), electrode_rod_model(x))
    write_json(asset('models', 'item', 'electrolyser.json'), {'parent': 'mcremix:block/electrolyser'})
    for name in ('carbon_electrode', 'unbaked_carbon_electrode'):
        write_json(asset('models', 'item', name + '.json'),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})
    parts = []
    for facing, rot in DOWN_TURNS.items():
        parts.append({'when': {'facing': facing}, 'apply': dict({'model': 'mcremix:block/electrolyser'}, **rot)})
        for side in ('anode', 'cathode'):
            parts.append({'when': {'facing': facing, side: 'true'},
                          'apply': dict({'model': f'mcremix:block/electrolyser_{side}'}, **rot)})
    write_json(asset('blockstates', 'electrolyser.json'), {'multipart': parts})
    write_json(data('mcremix', 'loot_tables', 'blocks', 'electrolyser.json'), loot_self('electrolyser'))

    # coke bound with tar by a press, then baked hard in the Furnace
    recipe('compacting/unbaked_carbon_electrode', {
        'type': 'create:compacting',
        'ingredients': [item('mcremix:coal_coke'), item('mcremix:coal_coke'),
                        {'fluid': 'mcremix:coal_tar', 'amount': 100}],
        'results': [item('mcremix:unbaked_carbon_electrode')]})
    firing('carbon_electrode', item('mcremix:unbaked_carbon_electrode'), 'mcremix:carbon_electrode', 'hot', 300)
    # copper terminals on a Bakelite head, on a steel plate
    recipe('electrolyser', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['CBC', 'SSS'],
        'key': {'C': {'tag': 'forge:ingots/copper'}, 'B': item('mcremix:bakelite'),
                'S': {'tag': 'forge:plates/steel'}},
        'result': item('mcremix:electrolyser')})
    # bauxite liquor left to cool, in a basin nothing is working or in a vat, settles out aluminium hydroxide and
    # gives back some of the caustic soda that dissolved it
    recipe('settling/aluminium_hydroxide', {
        'type': 'mcremix:settling', 'processingTime': 200,
        'ingredients': [{'fluid': 'mcremix:bauxite_liquor', 'amount': 250}],
        'results': [item('mcremix:aluminium_hydroxide'), {'fluid': 'mcremix:caustic_soda', 'amount': 100}]})
    # salt water split by a current: chlorine at the anode, hydrogen at the cathode, caustic soda left in the water
    recipe('electrolysis/salt_water', {
        'type': 'mcremix:electrolysis', 'charge': 100,
        'ingredients': [{'fluid': 'mcremix:salt_water', 'amount': 250}],
        'results': [{'fluid': 'mcremix:chlorine', 'amount': 125}, {'fluid': 'mcremix:hydrogen', 'amount': 125},
                    {'fluid': 'mcremix:caustic_soda', 'amount': 125}]})
    # alumina split into molten aluminium, an ingot's worth; the current through it keeps it molten
    recipe('electrolysis/alumina', {
        'type': 'mcremix:electrolysis', 'charge': 500, 'ingredients': [item('mcremix:alumina')],
        'results': [{'fluid': 'mcremix:molten_aluminium', 'amount': 90}]})


def electrolysis_lang():
    return {
        'block.mcremix.electrolyser': 'Electrolyser',
        'block.mcremix.electrolyser.tooltip.summary': (
            'Splits what is in a _basin_ or a _vat_ with a heavy current, carried into it by two _Carbon Electrodes_. '
            'A _Power Grid_ device: wire its _+_ and _-_ terminals across a source.'),
        'block.mcremix.electrolyser.tooltip.condition1': 'When placed',
        'block.mcremix.electrolyser.tooltip.behaviour1': (
            'Its electrodes go into the block you place it against: the _side of a basin_ or the _side of a vat_. '
            'A basin lets the _gases_ it makes escape; a vat keeps them for its pipes.'),
        'block.mcremix.electrolyser.tooltip.condition2': 'When used',
        'block.mcremix.electrolyser.tooltip.behaviour2': (
            'With a _Carbon Electrode_ in hand, fits it, the _anode_ (+) first. With an empty hand, takes one out.'),
        'block.mcremix.electrolyser.tooltip.condition3': 'When powered',
        'block.mcremix.electrolyser.tooltip.behaviour3': (
            'Needs about _4 V_ to start. Past that the _current_, and how fast it splits, climb steeply, and so does '
            'its heat. The _anode burns away_ with the power through it, the _cathode_ a tenth as fast.'),
        'item.mcremix.carbon_electrode': 'Carbon Electrode',
        'item.mcremix.carbon_electrode.tooltip.summary': (
            'Coke and tar baked hard. Fitted in an _Electrolyser_, it carries the current into the liquid. It _burns '
            'away_ as it works, faster the _more power_ goes through it.'),
        'item.mcremix.carbon_electrode.left': '%s%% left',
        'item.mcremix.unbaked_carbon_electrode': 'Unbaked Carbon Electrode',
        'item.mcremix.unbaked_carbon_electrode.tooltip.summary': (
            '_Coal coke_ bound with _coal tar_ by a _press_. _Fire_ it _Hot_ in the Furnace to bake it into a '
            '_Carbon Electrode_.'),
        'mcremix.goggles.electrolyser.anode': 'Anode (+): %s%% left',
        'mcremix.goggles.electrolyser.cathode': 'Cathode (-): %s%% left',
        'mcremix.goggles.electrolyser.no_anode': 'No anode (+)',
        'mcremix.goggles.electrolyser.no_cathode': 'No cathode (-)',
        'mcremix.goggles.electrolyser.reading': '%s V, %s A',
        'mcremix.goggles.electrolyser.temperature': 'Temperature: %s °C',
        'mcremix.goggles.electrolyser.no_electrodes': 'Fit two Carbon Electrodes to start',
        'mcremix.goggles.electrolyser.no_vessel': 'Not on the side of a basin or a vat',
        'mcremix.goggles.electrolyser.nothing_to_split': 'Nothing here it can split',
        'mcremix.goggles.electrolyser.no_current': 'No current: wire it across 4 V or more',
        'mcremix.goggles.vat': 'Vat, %s×%s×%s',
        'mcremix.goggles.vat.idle': 'Nothing to settle, ferment, split or mix',
        'mcremix.goggles.vat.splitting': 'Splitting: %s A',
        'mcremix.goggles.vat.no_current': 'Waiting for current from its Electrolysers',
        'mcremix.goggles.vat.settling': 'Settling',
        'mcremix.goggles.vat.waiting': '%s items waiting for an inventory beside or under its scaffolding, or a funnel on it',
    }


# ------------------------------------------------------------------------------------------------ plastics

# polyethylene: milky and waxy, a little see-through. outline, dark (shadow), base, light (the waxy sheen), how opaque
POLYETHYLENE = ((0x4E, 0x58, 0x56), (0x9C, 0xA8, 0xA4), (0xDC, 0xE4, 0xE0), (0xFF, 0xFF, 0xFF), 235)


def outline(img, colour):
    """A dark rim round everything drawn, as items are drawn."""
    shape = {(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3]}
    for y in range(16):
        for x in range(16):
            if (x, y) not in shape and any((x + dx, y + dy) in shape for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), colour + (255,))


def pellets_texture(seed):
    """A little heap of plastic pellets: rounded beads, each lit on its upper left with a waxy sheen and outlined, the
    ones in front lying over those behind."""
    edge, dark, base, light, alpha = POLYETHYLENE
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    # back to front, so the ones in front lie over the ones behind
    for cx, cy in [(5, 5), (10.5, 5.5), (3.5, 9.5), (8, 9), (12.5, 10), (6.5, 12.5), (10.5, 13)]:
        cx, cy = cx + rnd.uniform(-0.3, 0.3), cy + rnd.uniform(-0.3, 0.3)
        rx, ry = rnd.uniform(2.1, 2.5), rnd.uniform(1.7, 2.0)
        bead = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for y in range(16):
            for x in range(16):
                dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
                if dx * dx + dy * dy > 1:
                    continue
                t = 0.5 - dx * 0.35 - dy * 0.4
                col = mix(base, light, min(1.0, (t - 0.6) * 4)) if t > 0.6 else mix(dark, base, min(1.0, t / 0.6 + 0.15))
                bead.putpixel((x, y), col + (alpha,))
        outline(bead, edge)
        img.alpha_composite(bead)
    return img


def bushing_texture(seed):
    """A plastic bushing: a short thick sleeve seen from above and in front, its hole showing the shadowed inside, its
    rim lit with a waxy sheen and its side shading off to the right."""
    edge, dark, base, light, alpha = POLYETHYLENE
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx, top, bottom = 8, 5.5, 10.5
    rx, ry, hx, hy = 5.6, 3.2, 3.0, 1.8

    def within(x, y, cy, ax, ay):
        dx, dy = (x + 0.5 - cx) / ax, (y + 0.5 - cy) / ay
        return dx * dx + dy * dy <= 1
    for y in range(16):
        for x in range(16):
            on_top = within(x, y, top, rx, ry)
            on_side = not on_top and (within(x, y, bottom, rx, ry)
                                      or (abs(x + 0.5 - cx) <= rx and top <= y + 0.5 <= bottom))
            if not (on_top or on_side):
                continue
            n = rnd.uniform(-0.04, 0.04)
            if within(x, y, top, hx, hy):
                # the inside of the sleeve, in shadow, its far wall a little lit
                col = mix(edge, dark, max(0.0, 0.7 - (y + 0.5 - (top - hy)) / (2 * hy) * 0.8 + n))
            elif on_top:
                t = 0.55 - (x + 0.5 - cx) / rx * 0.3 - (y + 0.5 - top) / ry * 0.25 + n
                col = mix(base, light, min(1.0, (t - 0.55) * 3)) if t > 0.55 else mix(dark, base, min(1.0, t / 0.55 + 0.3))
            else:
                # the side, below the rim's edge, shades off to the right and towards the bottom
                t = 0.65 - (x + 0.5 - cx) / rx * 0.4 - (y + 0.5 - top) / 14 + n
                col = mix(dark, base, max(0.0, min(1.0, t)))
                if within(x, y - 1, top, rx, ry):
                    col = mix(col, edge, 0.35)
            img.putpixel((x, y), col + (alpha,))
    outline(img, edge)
    return img


# PVC: hard, opaque, bright white, nothing like polyethylene's waxy grey: edge, dark, base, light
PVC = ((0x5E, 0x60, 0x64), (0xC4, 0xC6, 0xC8), (0xEE, 0xEF, 0xEE), (0xFF, 0xFF, 0xFF))


def pvc_block_texture(seed, fitting=False):
    """Plain white plastic, almost flat: the faintest mottling, and fittings a shade greyer than the pipe."""
    edge, dark, base, light = PVC
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            col = mix(base, dark, 0.22) if fitting else base
            n = rnd.uniform(-0.05, 0.05)
            col = mix(col, light, n * 3) if n > 0 else mix(col, dark, -n * 2)
            img.putpixel((x, y), col + (255,))
    return img


def pvc_pipe_item_texture(seed):
    """A length of white pipe bent through a right angle, a collar at each end, lit from the upper left."""
    edge, dark, base, light = PVC
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    parts = [
        # x0, y0, x1, y1, collar
        (3, 10, 10, 12, False), (9, 4, 11, 12, False),
        (1, 9, 2, 13, True), (8, 2, 12, 3, True),
    ]
    for x0, y0, x1, y1, collar in parts:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                # across the pipe: lit on its upper or left side, shaded on the other
                across = (y - y0) / max(1, y1 - y0) if x1 - x0 > y1 - y0 else (x - x0) / max(1, x1 - x0)
                t = 1 - across + rnd.uniform(-0.06, 0.06)
                col = mix(dark, base, min(1.0, t * 1.8)) if t < 0.55 else mix(base, light, min(1.0, (t - 0.55) * 2))
                if collar:
                    col = mix(col, dark, 0.25)
                img.putpixel((x, y), col + (255,))
    outline(img, edge)
    return img


def gen_plastics():
    """Stage 4's chemistry, from sugar to polyethylene and PVC."""
    save_png(pellets_texture(1300), asset('textures', 'item', 'polyethylene_pellets.png'))
    save_png(bushing_texture(1301), asset('textures', 'item', 'polyethylene_bushing.png'))
    for name in ('polyethylene_pellets', 'polyethylene_bushing'):
        write_json(asset('models', 'item', name + '.json'),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})
    # what aluminium and titanium friction points take: the aluminium casing and bracket, and all that is built on the
    # casing
    write_json(data('mcremix', 'tags', 'items', 'polyethylene.json'), tag(['mcremix:polyethylene_bushing']))
    # pellets pressed together over a heated basin, as Bakelite is
    recipe('compacting/polyethylene_bushing', {
        'type': 'create:compacting', 'heatRequirement': 'heated',
        'ingredients': [item('mcremix:polyethylene_pellets'), item('mcremix:polyethylene_pellets')],
        'results': [item('mcremix:polyethylene_bushing')]})
    # sugar in water, left to stand in a basin nothing is working or in a vat, ferments into ethanol
    recipe('fermenting/ethanol', {
        'type': 'mcremix:fermenting', 'processingTime': 600,
        'ingredients': [item('minecraft:sugar'), item('minecraft:sugar'), {'fluid': 'minecraft:water', 'amount': 250}],
        'results': [{'fluid': 'mcremix:ethanol', 'amount': 250}]})
    # PVC: resin pressed into sheets, sheets made into pipes that lie flat on block faces
    edge, dark, base, light = PVC
    save_png(powder_texture(1302, dark, base, light), asset('textures', 'item', 'pvc_resin.png'))
    save_png(create_item_texture('sheet', PVC_SHADES), asset('textures', 'item', 'pvc_sheet.png'))
    save_png(pvc_pipe_item_texture(1304), asset('textures', 'item', 'pvc_pipe.png'))
    save_png(pvc_block_texture(1305), asset('textures', 'block', 'pvc_pipe.png'))
    save_png(pvc_block_texture(1306, fitting=True), asset('textures', 'block', 'pvc_fitting.png'))
    for name in ('pvc_resin', 'pvc_sheet', 'pvc_pipe'):
        write_json(asset('models', 'item', name + '.json'),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})
    # the pipe and its corner pieces are drawn by PvcPipeModel; the model file only names their textures
    write_json(asset('models', 'block', 'pvc_pipe.json'),
               {'parent': 'minecraft:block/block', 'textures': {'particle': 'mcremix:block/pvc_pipe',
                                                                  'pipe': 'mcremix:block/pvc_pipe',
                                                                  'fitting': 'mcremix:block/pvc_fitting'}})
    for name in ('pvc_pipe', 'pvc_pipe_corner'):
        write_json(asset('blockstates', name + '.json'), {'variants': {'': {'model': 'mcremix:block/pvc_pipe'}}})
    write_json(data('mcremix', 'loot_tables', 'blocks', 'pvc_pipe.json'), loot_self('pvc_pipe'))
    recipe('pressing/pvc_sheet', {'type': 'create:pressing', 'ingredients': [item('mcremix:pvc_resin')],
                                  'results': [item('mcremix:pvc_sheet')]})
    # three sheets in a row roll into eight pipes: thin pipe, made for long runs
    recipe('crafting/pvc_pipe', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['SSS'],
                                 'key': {'S': {'item': 'mcremix:pvc_sheet'}},
                                 'result': {'item': 'mcremix:pvc_pipe', 'count': 8}})
    # hydrogen chloride split back into chlorine and hydrogen: both gases, so only a vat keeps them
    recipe('electrolysis/hydrogen_chloride', {
        'type': 'mcremix:electrolysis', 'charge': 100,
        'ingredients': [{'fluid': 'mcremix:hydrogen_chloride', 'amount': 250}],
        'results': [{'fluid': 'mcremix:chlorine', 'amount': 125}, {'fluid': 'mcremix:hydrogen', 'amount': 125}]})
    # the Steel Furnace: a Furnace cased in steel, as using steel casing on one does in the world
    recipe('crafting/steel_furnace', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                      'ingredients': [item('mcremix:kiln'), item('mcremix:steel_casing')],
                                      'result': item('mcremix:steel_furnace')})
    # ethanol run over hot alumina loses its water and comes off as ethylene; the alumina wears slowly
    recipe('steel_furnace/ethylene', {
        'type': 'mcremix:steel_furnace', 'heat': 'hot', 'processingTime': 100, 'catalyst_lasts': 16000,
        'ingredients': [item('mcremix:alumina'), {'fluid': 'mcremix:ethanol', 'amount': 250}],
        'results': [{'fluid': 'mcremix:ethylene', 'amount': 250}]})
    # ethylene dichloride cracked: vinyl chloride, and hydrogen chloride given off with it
    recipe('steel_furnace/vinyl_chloride', {
        'type': 'mcremix:steel_furnace', 'heat': 'searing', 'processingTime': 100,
        'ingredients': [{'fluid': 'mcremix:ethylene_dichloride', 'amount': 250}],
        'results': [{'fluid': 'mcremix:vinyl_chloride', 'amount': 250},
                    {'fluid': 'mcremix:hydrogen_chloride', 'amount': 250}]})
    # a Spout of hydrochloric acid scrubs weathered parts and blocks back to bare metal; waxed ones are sealed
    for bare, weathered, mod in acid_cleaning():
        obj = {'type': 'create:filling',
               'ingredients': [weathered, {'fluid': 'mcremix:hydrochloric_acid', 'amount': ACID_PER_ITEM}],
               'results': [item(bare)]}
        if mod:
            obj = {'conditions': [{'type': 'forge:mod_loaded', 'modid': mod}], **obj}
        ns, name = bare.split(':')
        recipe(f'filling/acid/{name if ns == "mcremix" else ns + "_" + name}', obj)


# what a Spout of acid uses to clean one item
ACID_PER_ITEM = 100


def acid_cleaning():
    """(bare item, its weathered forms as one ingredient, the mod it needs) for all a Spout of acid cleans."""
    out = []
    # this mod's shafts, cogs, gearboxes and the rest; steel only ever gets to its first stage
    for material in ('copper', 'iron', 'steel'):
        stages = [s for s in STAGES[1:] if not past_last_stage(material, s)]
        for part in PARTS:
            out.append((f'mcremix:{material}_{part}',
                        [item(f'mcremix:{stage_prefix(material, s)}{material}_{part}') for s in stages], None))
    # Create's copper pipes, pumps and tanks carry their patina on the item, and this mod's iron and steel ones their rust
    for part, material in ([(p, 'copper') for p in ('create:fluid_pipe', 'create:mechanical_pump', 'create:fluid_tank')]
                           + [(p, 'iron') for p in fluid_blocks('iron')] + [(p, 'steel') for p in fluid_blocks('steel')]):
        out.append((part, [{'type': 'forge:partial_nbt', 'item': part, 'nbt': {'BlockStateTag': {'patina': s}}}
                           for s in STAGES[1:] if not past_last_stage(material, s)], None))
    # copper blocks, vanilla's and Create's
    for bare, form in (('copper_block', 'copper'), ('cut_copper', 'cut_copper'),
                       ('cut_copper_stairs', 'cut_copper_stairs'), ('cut_copper_slab', 'cut_copper_slab')):
        out.append((f'minecraft:{bare}', [item(f'minecraft:{s}_{form}') for s in STAGES[1:]], None))
    for form in ('copper_shingles', 'copper_shingle_slab', 'copper_shingle_stairs',
                 'copper_tiles', 'copper_tile_slab', 'copper_tile_stairs'):
        out.append((f'create:{form}', [item(f'create:{s}_{form}') for s in STAGES[1:]], None))
    # Immersive Weathering's iron, when it is there
    rusts = ('exposed', 'weathered', 'rusted')
    for form in ('cut_iron', 'cut_iron_slab', 'cut_iron_stairs', 'plate_iron', 'plate_iron_slab', 'plate_iron_stairs'):
        out.append((f'immersive_weathering:{form}', [item(f'immersive_weathering:{s}_{form}') for s in rusts],
                    'immersive_weathering'))
    for form in ('iron_bars', 'iron_door', 'iron_trapdoor'):
        out.append((f'minecraft:{form}', [item(f'immersive_weathering:{s}_{form}') for s in rusts],
                    'immersive_weathering'))
    return out


def plastics_lang():
    return {
        'item.mcremix.polyethylene_pellets': 'Polyethylene Pellets',
        'item.mcremix.polyethylene_bushing': 'Polyethylene Bushing',
        'item.mcremix.polyethylene_bushing.tooltip.summary': 'A waxy plastic sleeve for a shaft to turn in, pressed from _polyethylene pellets_ by a _press_ over a _heated_ basin. _Aluminium_ and _titanium_ supports take one.',
        'mcremix.jei.fermenting': 'Fermenting',
        'mcremix.jei.fermenting.time': 'Left to stand %ss',
        'mcremix.jei.fermenting.basin': 'In a basin with no heat under it and no mixer or press over it, or in a vat',
        'mcremix.jei.fermenting.vat': 'A vat ferments a batch for each bucket it holds, up to 16 at once',
        'mcremix.goggles.vat.fermenting': 'Fermenting',
        'item.mcremix.pvc_resin': 'PVC Resin',
        'item.mcremix.pvc_sheet': 'PVC Sheet',
        'block.mcremix.pvc_pipe': 'PVC Pipe',
        'block.mcremix.pvc_pipe.tooltip.summary': 'A thin white pipe that lies flat on a block face, like red alloy, and runs round _inside and outside corners_. Pumps push _twice as far_ through it. It only carries _Cold_ fluids: anything hotter makes it _sag and burst_.',
        'block.mcremix.pvc_pipe.tooltip.condition1': 'Joining',
        'block.mcremix.pvc_pipe.tooltip.behaviour1': 'Runs on the same face join up, and it _rises to meet_ a pump, tank, basin or pipe beside it. Use a _wrench_ on a joint to cut it, and again to mend it.',
        'block.mcremix.pvc_pipe_corner': 'PVC Pipe Corner',
        'block.mcremix.steel_furnace': 'Steel Furnace',
        'block.mcremix.steel_furnace.tooltip.summary': 'A _Furnace_ cased in steel: it fires what a Furnace fires and works _fluids_ too, but burns only _fuel piped in from underneath_. Use _steel casing_ on a Furnace to make one.',
        'block.mcremix.steel_furnace.tooltip.condition1': 'Fluid jobs',
        'block.mcremix.steel_furnace.tooltip.behaviour1': 'Pipe or pour a fluid it works into its _front_, _sides_ or _top_ and it waits in the _feed tank_; what it gives waits there for a _pipe_ to draw it off. _Ethanol_ over _alumina_ in the chamber gives _ethylene_ at _Hot_; _ethylene dichloride_ cracks into _vinyl chloride_ and _hydrogen chloride_ at _Searing_.',
        'block.mcremix.steel_furnace.tooltip.condition2': 'Fuel',
        'block.mcremix.steel_furnace.tooltip.behaviour2': 'Pump _wood spirit_, _ethanol_, _coal tar_, _hydrogen_ or _ethylene_ in from _underneath_ and _light_ it: the faster the pump, the _hotter_ it burns, up to what that fuel can reach, and anything faster is _lost_. Only _hydrogen_ and _ethylene_ reach _Blazing_. Unlit, gas fuel _escapes_ out of the front and liquid fuel _spills_ out. It takes no solid fuel, no heat from below and no bellows.',
        'mcremix.goggles.steel_furnace.no_fuel': 'No fuel coming in from underneath',
        'mcremix.goggles.steel_furnace.burning': 'Burning %s, %s mB a tick: %s',
        'mcremix.goggles.steel_furnace.hotter': '%s mB a tick would burn %s',
        'mcremix.goggles.steel_furnace.ceiling': '%s burns no hotter',
        'mcremix.goggles.steel_furnace.rich': 'Fed faster than it burns: past %s mB a tick is lost',
        'mcremix.goggles.steel_furnace.venting': '%s escaping out of the front',
        'mcremix.goggles.steel_furnace.spilling': '%s spilling out of the front',
        'mcremix.goggles.steel_furnace.backing_up': '%s backing up: it has nowhere to spill',
        'mcremix.goggles.steel_furnace.pooled': '%s pooled inside, %s mB',
        'mcremix.goggles.furnace.unlit': 'Not lit: use flint and steel or a fire charge',
        'mcremix.furnace.nothing_to_light': 'There is no fuel in it to catch',
        'mcremix.steel_furnace.nothing_to_light': 'Nothing to catch: pipe fuel in from underneath first',
        'mcremix.steel_furnace.piped_fuel_only': 'A Steel Furnace only burns fuel piped in from underneath',
        'mcremix.goggles.bellows.sealed': 'A Steel Furnace is sealed: air does nothing for it',
        'mcremix.jei.steel_furnace_fuel': 'Steel Furnace Fuel',
        'mcremix.jei.steel_furnace_fuel.piped': 'Piped into a lit Steel Furnace from underneath and burnt as it comes; what comes faster than it burns is lost',
        'mcremix.jei.steel_furnace_fuel.rate': '%s mB a tick',
        'mcremix.jei.steel_furnace_fuel.pump': 'A pump turning at %s RPM moves that much',
        'mcremix.goggles.steel_furnace.catalyst': 'Catalyst',
        'mcremix.goggles.steel_furnace.no_catalyst': 'Needs %s in the chamber',
        'mcremix.goggles.steel_furnace.full': 'Its tanks are full',
        'mcremix.goggles.steel_furnace.waiting': 'Needs %s mB to start',
        'mcremix.goggles.steel_furnace.wear': '%s worn %s%%',
        'mcremix.jei.steel_furnace': 'Steel Furnace',
        'mcremix.jei.steel_furnace.catalyst': 'Lies in the chamber; one wears away every %s mB',
    }


# ------------------------------------------------------------------------------------------------ vat mixers

# The vat form of each mixer that fits a vat: its tier, the mixer it is a form of, and the tools that mine it, as they
# mine the mixer. Copper, iron, steel and aluminium mixers fit vats of their own metal (Taran); Create's own, the
# andesite one, works only over basins, and no mixer fits a brass vat (VatMixerBlock.Kind)
VAT_MIXERS = [(m, f'mcremix:{m}_mechanical_mixer', ('pickaxe', 'axe'))
              for m in ('copper', 'iron', 'steel', 'aluminium')]


def vat_mixer(tier):
    return f'{tier}_vat_mixer'


def vat_mixers_mined_with(tool):
    return [f'mcremix:{vat_mixer(tier)}' for tier, _, tools in VAT_MIXERS if tool in tools]


def vat_mixer_tank(tier, stage):
    """The tank side a vat mixer is drawn in (Taran, 0.23.0): its vat's, Create's own for copper, at the mixer's own
    stage of weathering."""
    if tier == 'copper':
        return 'create:block/fluid_tank' if stage == 'unaffected' else f'mcremix:block/{stage}_fluid_tank'
    return f'mcremix:block/{stage_prefix(tier, stage)}{tank_name(tier)}'


def vat_mixer_model():
    """Drawn facing north, the vat beyond it: a plate of its vat's tank standing against it, six pixels thick, the
    mixer's head out of sight inside. The cog on its face, which takes rotation from the cogs beside it as Create's
    mixer does, is drawn by VatMixerRenderer in the mixer's own metal."""
    return {'parent': 'minecraft:block/block', 'elements': [box([0, 0, 0], [16, 16, 6], {
        'north': {'uv': [0, 0, 16, 16], 'texture': '#tank', 'cullface': 'north'},
        'south': {'uv': [0, 0, 16, 16], 'texture': '#tank'},
        'east': {'uv': [10, 0, 16, 16], 'texture': '#tank', 'cullface': 'east'},
        'west': {'uv': [0, 0, 6, 16], 'texture': '#tank', 'cullface': 'west'},
        'up': {'uv': [0, 0, 16, 6], 'texture': '#tank', 'cullface': 'up'},
        'down': {'uv': [0, 10, 16, 16], 'texture': '#tank', 'cullface': 'down'}})]}


def gen_vat_mixers():
    """Each mixer's vat form, which breaks back into its mixer, and what only a vat can mix. A copper, iron or steel
    one weathers as its mixer does, with a model for each stage (waxed ones look as they did); aluminium never does."""
    write_json(asset('models', 'block', 'vat_mixer', 'block.json'), vat_mixer_model())
    for tier, mixer, _ in VAT_MIXERS:
        name = vat_mixer(tier)
        patinas = PATINAS if tier != 'aluminium' else {None: 'unaffected'}
        for stage in set(patinas.values()):
            texture = vat_mixer_tank(tier, stage)
            write_json(asset('models', 'block', stage_prefix(tier, stage) + name + '.json'), {
                'parent': 'mcremix:block/vat_mixer/block', 'textures': {'tank': texture, 'particle': texture}})
        variants = {}
        for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            for patina, stage in patinas.items():
                key = f'facing={facing}' + (f',patina={patina}' if patina else '')
                model = f'mcremix:block/{stage_prefix(tier, stage)}{name}'
                variants[key] = dict({'model': model}, **({'y': y} if y else {}))
        write_json(asset('blockstates', name + '.json'), {'variants': variants})
        loot = loot_self(name)
        loot['pools'][0]['entries'][0]['name'] = mixer
        write_json(data('mcremix', 'loot_tables', 'blocks', name + '.json'), loot)
    # chlorine and ethylene join into ethylene dichloride over ferric chloride, which is used up only slowly
    recipe('vat_mixing/ethylene_dichloride', {
        'type': 'mcremix:vat_mixing',
        'ingredients': [{'fluid': 'mcremix:chlorine', 'amount': 1000}, {'fluid': 'mcremix:ethylene', 'amount': 1000},
                        {'fluid': 'mcremix:ferric_chloride', 'amount': 25}],
        'results': [{'fluid': 'mcremix:ethylene_dichloride', 'amount': 1000}]})
    # hydrogen chloride dissolves in water as hydrochloric acid
    recipe('vat_mixing/hydrochloric_acid', {
        'type': 'mcremix:vat_mixing',
        'ingredients': [{'fluid': 'mcremix:hydrogen_chloride', 'amount': 250},
                        {'fluid': 'minecraft:water', 'amount': 250}],
        'results': [{'fluid': 'mcremix:hydrochloric_acid', 'amount': 500}]})


def vat_mixer_lang():
    # the vat forms are called after their mixers, so they need no names of their own
    return {
        'mcremix.goggles.vat.mixing': 'Mixing',
        'mcremix.goggles.vat_mixer.still': 'Not turning',
        'mcremix.goggles.vat_mixer.too_slow': 'Turning too slowly to stir',
        'mcremix.goggles.vat_mixer.no_vat': 'Not fixed to a vat',
        'mcremix.goggles.vat_mixer.other_material': 'Does not fit a vat of this material',
        'mcremix.goggles.vat_mixer.idle': 'Nothing in the vat to mix',
        'mcremix.goggles.vat_mixer.mixing': 'Mixing',
        'mcremix.vat_mixer.fits': '%s only fits %s',
        'mcremix.vat_mixer.kind.copper': 'copper vats',
        'mcremix.vat_mixer.kind.iron': 'iron vats',
        'mcremix.vat_mixer.kind.steel': 'steel vats',
        'mcremix.vat_mixer.kind.aluminium': 'aluminium vats',
        'mcremix.jei.vat_mixing': 'Vat Mixing',
        'mcremix.jei.vat_mixing.time': '%ss a batch at %s RPM',
        'mcremix.jei.vat_mixing.vat': 'Only in a vat, which holds gases: a basin lets them go',
        'mcremix.jei.vat_mixing.mixer': 'A mixer used on a vat\'s side goes on as its vat form. Copper, iron, steel and aluminium mixers each stir vats of their own metal; Create\'s mixer works only over basins, and no mixer fits a brass vat',
        'mcremix.jei.vat_mixing.batches': 'Each mixer stirs four batches at a time; a big vat wants several',
    }


# ------------------------------------------------------------------------------------------------ spark plug

def spark_plug_shell_texture(seed):
    """The spark plug's iron shell. Rows 0 to 2 are the sides of its hex, two flats to a side, the left lit and the
    right in shadow; rows 4 to 9 its outer face round the insulator, under a lit rim."""
    base, dark, light, _ = PALETTES['iron']
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if y < 3:
                t = (0.62 if x % 6 < 3 else 0.4) + (0.08 if y == 0 else -0.08 if y == 2 else 0)
            elif 4 <= y < 10:
                t = 0.68 if x % 6 in (0, 5) or y in (4, 9) else 0.52
            else:
                t = 0.46
            img.putpixel((x, y), shade((dark, base, light), t + rnd.uniform(-0.04, 0.04)) + (255,))
    return img


def spark_plug_elements(dz=0):
    """Drawn screwed into the block under it, low on that block's side once SparkPlugBlock turns it (the model's z is
    then its height): an iron hex shell, a Bakelite insulator standing out of it, and the copper cap of its + terminal
    in a red collar; its - terminal is a lug on the shell in a blue one. SparkPlugBlock's shape and terminals are the
    same boxes. {@code dz} moves it along z, to stand it in the middle as an item."""
    s, b, t = '#shell', '#bakelite', '#terminals'
    sides = ('north', 'south', 'east', 'west')

    def at(frm, to, faces):
        return box([frm[0], frm[1], frm[2] + dz], [to[0], to[1], to[2] + dz], faces)
    return [
        at([5, 0, 1], [11, 3, 7], dict({'up': {'uv': [0, 4, 6, 10], 'texture': s}},
                                       **{side: {'uv': [0, 0, 6, 3], 'texture': s} for side in sides})),
        at([6, 3, 2], [10, 9, 6], dict({'up': {'uv': [6, 2, 10, 6], 'texture': b}},
                                       **{side: {'uv': [2, 8, 6, 14], 'texture': b} for side in sides})),
        at([6.5, 9, 2.5], [9.5, 10, 5.5], dict({'up': {'uv': [0, 4, 3, 7], 'texture': t}},
                                               **{side: {'uv': [4, 4, 7, 5], 'texture': t} for side in sides})),
        at([7, 10, 3], [9, 11, 5], dict({'up': {'uv': [0, 0, 2, 2], 'texture': t}},
                                        **{side: {'uv': [0, 2, 2, 3], 'texture': t} for side in sides})),
        at([11, 0, 3], [13, 2, 5], dict({'up': {'uv': [0, 8, 2, 10], 'texture': t}},
                                        **{side: {'uv': [4, 8, 6, 10], 'texture': t} for side in sides})),
    ]


def spark_plug_textures():
    return {'shell': 'mcremix:block/spark_plug_shell', 'bakelite': 'mcremix:block/electrolyser_bakelite',
            'terminals': 'mcremix:block/electrolyser_terminals', 'particle': 'mcremix:block/spark_plug_shell'}


def gen_spark_plug():
    """The spark plug: Bakelite, iron and copper, as Taran chose (2026-10-22)."""
    save_png(spark_plug_shell_texture(1310), asset('textures', 'block', 'spark_plug_shell.png'))
    write_json(asset('models', 'block', 'spark_plug.json'),
               {'parent': 'minecraft:block/block', 'textures': spark_plug_textures(), 'elements': spark_plug_elements()})
    write_json(asset('models', 'item', 'spark_plug.json'),
               {'parent': 'minecraft:block/block', 'textures': spark_plug_textures(),
                'elements': spark_plug_elements(dz=4),
                'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 2, 0], 'scale': [0.9, 0.9, 0.9]},
                            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
                            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.8, 0.8, 0.8]}}})
    write_json(asset('blockstates', 'spark_plug.json'), {'variants': {
        f'facing={facing}': dict({'model': 'mcremix:block/spark_plug'}, **rot) for facing, rot in DOWN_TURNS.items()}})
    write_json(data('mcremix', 'loot_tables', 'blocks', 'spark_plug.json'), loot_self('spark_plug'))
    # a copper core in a Bakelite insulator, in an iron shell
    recipe('spark_plug', {
        'type': 'minecraft:crafting_shaped', 'category': 'redstone', 'pattern': ['C', 'B', 'I'],
        'key': {'C': {'tag': 'forge:ingots/copper'}, 'B': item('mcremix:bakelite'), 'I': {'tag': 'forge:ingots/iron'}},
        'result': item('mcremix:spark_plug')})


def spark_plug_lang():
    return {
        'block.mcremix.spark_plug': 'Spark Plug',
        'block.mcremix.spark_plug.tooltip.summary': 'Screws into the _side of a Furnace or Steel Furnace_ and _lights it_ with a spark. It sparks only with about _1,000 V_ across it, so it wants a _transformer_ stepping the voltage up.',
        'block.mcremix.spark_plug.tooltip.condition1': 'When placed',
        'block.mcremix.spark_plug.tooltip.behaviour1': 'Goes on any side of a furnace _but its front_, low by the firebox. Its _+_ is the copper cap, its _-_ the lug on the iron shell.',
        'block.mcremix.spark_plug.tooltip.condition2': 'When powered',
        'block.mcremix.spark_plug.tooltip.behaviour2': 'Sparks the whole time _900 V_ or more are across it, lit furnace or not, drawing only about _10 mA_, and _lights the furnace_ as soon as there is fuel in it to catch. Put a _switch_ or _relay_ in its circuit to decide when. It _never wears out_.',
        'mcremix.goggles.spark_plug.volts': '%s V across it',
        'mcremix.goggles.spark_plug.sparking': 'Sparking, %s mA',
        'mcremix.goggles.spark_plug.too_low': 'Not sparking: it wants %s V or more',
        'mcremix.goggles.spark_plug.furnace.lit': 'Furnace lit',
        'mcremix.goggles.spark_plug.furnace.out': 'Furnace out',
        'mcremix.goggles.spark_plug.furnace.none': 'Not screwed into a furnace',
    }


# where top speed falls on a Pressurizer's dial, in degrees clockwise from straight up: the needle sweeps from -135
# (nothing) to 135, 15 % past top speed (PressurizerBlockEntity.DIAL_END), and the dial is red past top speed
DIAL_TOP = -135 + 270 / 1.15


def pressurizer_dial_texture(seed, look='steel'):
    """The Pressurizer's front: a casing panel of its metal with its pressure dial let into it, an ivory face in a
    brass bezel, marked at nothing and at each quarter of its metal's top speed, and red past top speed. Its needle and
    the boss it turns on are a model of their own (block/pressurizer/needle), turned as the pressure changes."""
    img = casing_texture(look, seed)
    _, dark, light, _ = PALETTES[look]
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            angle = math.degrees(math.atan2(dx, -dy))
            if r < 4.7:
                col = mix((0xC2, 0xBA, 0xA6), (0xE6, 0xE0, 0xCE), 0.6 - (dx + dy) * 0.06)
                if r > 2.6 and DIAL_TOP - 4 <= angle <= 140:
                    col = mix((0x9C, 0x20, 0x18), (0xD0, 0x36, 0x28), 0.6 - (dx + dy) * 0.06)
            elif r < 6.0:
                col = shade(BRASS_FITTING, 0.6 - (dx + dy) * 0.07)
            elif r < 6.7:
                col = mix(mix(dark, (0, 0, 0), 0.55), img.getpixel((x, y))[:3], 0.35)
            else:
                continue
            img.putpixel((x, y), col + (255,))
    for q in range(5):
        a = math.radians(-135 + (DIAL_TOP + 135) * q / 4)
        img.putpixel((round(7.5 + 3.6 * math.sin(a)), round(7.5 - 3.6 * math.cos(a))), (0x2A, 0x26, 0x22, 255))
    return img


def pressurizer_port_texture(seed, look='steel', sooted=False):
    """A flanged port on a casing panel of a Pressurizer's metal, four bolts round a dark opening: the top of one from
    before 0.24.0 with no room for its vessel, where pipes still go, and the end of a vessel's nozzle (its middle)."""
    img = casing_texture(look, seed)
    _, dark, light, _ = PALETTES[look]
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            if r < 2.3:
                col = mix((0x14, 0x11, 0x10), (0x2E, 0x28, 0x24), max(0.0, min(1.0, (r - 0.8) / 1.5)))
            elif r < 4.5:
                col = mix(dark, light, max(0.0, min(1.0, 0.55 - dx * 0.07 - dy * 0.07)))
                if abs(abs(dx) - 2.5) < 0.6 and abs(abs(dy) - 2.5) < 0.6:
                    col = mix(dark, (0, 0, 0), 0.35)
            elif r < 5.1:
                col = mix(mix(dark, (0, 0, 0), 0.4), img.getpixel((x, y))[:3], 0.3)
            else:
                continue
            img.putpixel((x, y), col + (255,))
    return sooted_texture(img, seed) if sooted else img


def sooted_texture(img, seed):
    """img blackened by the fire under it, unevenly, as a Pressurizer's underside is."""
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            col = img.getpixel((x, y))[:3]
            img.putpixel((x, y), mix(col, (0x1C, 0x19, 0x17), 0.45 + rnd.uniform(0, 0.2)) + (255,))
    return img


def pressurizer_vessel_side_texture(seed, look='steel'):
    """The vessel's drum, laid out as its model reads it (row 16 - y for height y): plates of its metal, brushed
    upright, with a riveted seam near its foot (y 1 to 2) and one under its shoulder (y 10 to 11), then the shoulder
    and crown above, lit as they turn to the sky."""
    base, dark, light, speck = PALETTES[look]
    rnd = random.Random(seed)
    polished = look in TINTED
    deep = mix(dark, (0, 0, 0), 0.4)
    streak = [rnd.uniform(-0.1, 0.1) * (1.4 if polished else 1) for _ in range(16)]
    img = Image.new('RGBA', (16, 16))
    for v in range(16):
        y = 15 - v
        for u in range(16):
            t = 0.5 + streak[u] + rnd.uniform(-0.05, 0.05)
            if polished:
                t += 0.6 * math.exp(-((u - 5) / 2.2) ** 2)
            if y >= 12:
                t += 0.15 * (y - 11)
            col = mix(dark, light, max(0.0, min(1.0, t)))
            if y in (1, 10):
                col = mix(col, light, 0.3) if u % 2 else mix(deep, dark, 0.5)
            elif y in (0, 9) or y == 11:
                col = mix(col, deep, 0.35)
            elif y == 12:
                col = mix(col, deep, 0.2)
            if speck and rnd.random() < 0.08:
                col = mix(col, speck, 0.5)
            img.putpixel((u, v), col + (255,))
    return img


def pressurizer_vessel_top_texture(seed, look='steel'):
    """The vessel's top from above, square as it is since 0.26.0: plate lit on its upper left, a darker line round the
    edge of the tank (pixels 2 and 13), its shoulder (3 and 12) and its crown (5 and 10), darker round the valve's
    foot."""
    base, dark, light, speck = PALETTES[look]
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = max(abs(dx), abs(dy))
            t = 0.55 - (dx + dy) * 0.035 + rnd.uniform(-0.05, 0.05)
            if d in (2.5, 4.5, 5.5):
                t -= 0.22
            col = mix(dark, light, max(0.0, min(1.0, t)))
            if d < 1.6:
                col = mix(col, (0, 0, 0), 0.4)
            if speck and rnd.random() < 0.08:
                col = mix(col, speck, 0.5)
            img.putpixel((x, y), col + (255,))
    return img


BRASS_FITTING = ((0x7A, 0x52, 0x22), (0xC8, 0x92, 0x3E), (0xF4, 0xD0, 0x7A))


def pressurizer_valve_texture():
    """The brass of the safety valve on a vessel's crown, and of the boss a dial's needle turns on."""
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade(BRASS_FITTING, 0.75 - (x + y) * 0.025) + (255,))
    return img


def pressurizer_needle_texture():
    """A dial's needle: blackened steel, a little lighter along its length."""
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), mix((0x1A, 0x17, 0x16), (0x3A, 0x34, 0x30), 0.5 - (x - 7.5) * 0.08) + (255,))
    return img


def vessel_elements(side, top, valve, lift=0):
    """The vessel standing on a Pressurizer, narrower than a block and square since 0.26.0 (Taran: no staggered
    corners): a riveted tank 12 pixels across and 12 high, a shoulder and crown stepping in above it, and a brass
    safety valve on top. lift raises it, for the item, which shows the vessel on its Pressurizer: each face keeps the
    UV it has on the block (lifted)."""
    def tank(frm, to, down=False):
        faces = {d: face(side) for d in ('north', 'south', 'east', 'west')}
        faces['up'] = face(top)
        if down:
            faces['down'] = face(top, None if lift else 'down')
        return box(frm, to, faces)
    elements = [
        tank([2, 0, 2], [14, 12, 14], down=True),
        tank([3, 12, 3], [13, 13, 13]),
        tank([5, 13, 5], [11, 14, 11]),
        box([7, 14, 7], [9, 15.5, 9], all_faces(valve, skip=('down',))),
        box([6.5, 15.5, 7.5], [9.5, 16, 8.5], all_faces(valve)),
    ]
    return [lifted(e, lift) for e in elements] if lift else elements


def auto_uv(frm, to, direction):
    """The UV Minecraft gives a face that names none, from where its element lies in the block (BlockElement)."""
    (fx, fy, fz), (tx, ty, tz) = frm, to
    return {'down': [fx, 16 - tz, tx, 16 - fz], 'up': [fx, fz, tx, tz],
            'north': [16 - tx, 16 - ty, 16 - fx, 16 - fy], 'south': [fx, 16 - ty, tx, 16 - fy],
            'west': [fz, 16 - ty, tz, 16 - fy], 'east': [16 - tz, 16 - ty, 16 - fz, 16 - fy]}[direction]


def lifted(element, lift):
    """element raised lift pixels, each face keeping the UV it had where it was. Minecraft works out a face's UV from
    where it lies in its block, so one above the block's top would reach off its sprite into others on the atlas (the
    Pressurizer items' vessels showed scraps of other textures until 0.28.0)."""
    frm, to = element['from'], element['to']
    faces = {d: dict(f, uv=f.get('uv', auto_uv(frm, to, d))) for d, f in element['faces'].items()}
    return box([frm[0], frm[1] + lift, frm[2]], [to[0], to[1] + lift, to[2]], faces)


def vessel_nozzle_elements(side, port):
    """The nozzle a vessel puts out to a pipe joining one of its sides (Taran, 0.26.0: the model reaches the pipes it
    joins), drawn on its north side and turned by its blockstate: 8 pixels square, from the tank out to the edge of the
    block, where Create's pipe clamps its flange over its end. Its sides are the tank's plate between its seams, run
    along its length."""
    run = lambda d: {'texture': side, 'uv': [4, 8, 12, 10], 'rotation': 90 if d in ('east', 'west') else 0}
    return [box([4, 4, 0], [12, 12, 2], dict({d: run(d) for d in ('east', 'west', 'up', 'down')},
                                              north={'texture': port, 'uv': [4, 4, 12, 12]}))]


def pressurizer_body_elements(top='#top'):
    """A Pressurizer's lower block, drawn facing north: framed as Create's chain drive is, with the two sides its shaft
    goes in at (east and west, Taran, 0.26.0) sunk 0.95 pixels inside the frame round the shaft's hole, so a shaft
    there does not flicker against its face. The frame and the sunk face share one texture, laid on by where each part
    is. Its dial is on the front, its back is plain, its top is under the vessel (top names it: a port on one from
    before 0.24.0 with no room for a vessel) and its underside is sooted from the fire it stands on; the frame's inner
    faces, seen in the recesses, are plain."""
    F, B, A, D = '#front', '#back', '#axis', '#bottom'
    at = lambda tex, uv, cull=None: dict(face(tex, cull), uv=uv)
    return [
        box([0, 0, 0], [16, 2, 16], {'north': at(F, [0, 14, 16, 16], 'north'), 'south': at(B, [0, 14, 16, 16], 'south'),
                                     'east': at(A, [0, 14, 16, 16], 'east'), 'west': at(A, [0, 14, 16, 16], 'west'),
                                     'up': at(B, [0, 0, 16, 16]), 'down': at(D, [0, 0, 16, 16], 'down')}),
        box([0, 14, 0], [16, 16, 16], {'north': at(F, [0, 0, 16, 2], 'north'), 'south': at(B, [0, 0, 16, 2], 'south'),
                                       'east': at(A, [0, 0, 16, 2], 'east'), 'west': at(A, [0, 0, 16, 2], 'west'),
                                       'up': at(top, [0, 0, 16, 16], 'up'), 'down': at(B, [0, 0, 16, 16])}),
        box([0, 2, 0], [16, 14, 2], {'north': at(F, [0, 2, 16, 14], 'north'), 'east': at(A, [14, 2, 16, 14], 'east'),
                                     'west': at(A, [0, 2, 2, 14], 'west'), 'south': at(B, [0, 2, 16, 14])}),
        box([0, 2, 14], [16, 14, 16], {'south': at(B, [0, 2, 16, 14], 'south'), 'east': at(A, [0, 2, 2, 14], 'east'),
                                       'west': at(A, [14, 2, 16, 14], 'west'), 'north': at(B, [0, 2, 16, 14])}),
        box([0.95, 2, 2], [15.05, 14, 14], {'east': at(A, [2, 2, 14, 14]), 'west': at(A, [2, 2, 14, 14])}),
    ]


def needle_model():
    """A Pressurizer's dial needle, on a dial facing south, pointing straight up from the middle of the face; the
    renderer turns it to the Pressurizer's front and swings it with the pressure."""
    N, V = '#needle', '#valve'
    thin = lambda tex: {d: face(tex) for d in ('south', 'east', 'west', 'up', 'down')}
    return {
        'textures': {'needle': 'mcremix:block/pressurizer_needle', 'valve': 'mcremix:block/pressurizer_valve',
                     'particle': 'mcremix:block/pressurizer_needle'},
        'elements': [box([7.5, 8, 16.05], [8.5, 10, 16.25], thin(N)),
                     box([7.75, 10, 16.05], [8.25, 12.25, 16.25], thin(N)),
                     box([7.3, 7.3, 16.05], [8.7, 8.7, 16.45], thin(V))]}


def pressurizer_textures(name, look, seed, vessel_seed):
    """A Pressurizer's faces and its vessel's, in one metal: name_front (the dial), name_axis (its two sides, where its
    shaft goes in), name_top (its back and top), name_bottom (sooted), name_side (a port: the top of one with no
    vessel, and the end of a nozzle), name_vessel_side and name_vessel_top. The faces it had before 0.24.0 keep their
    seeds; the vessel's and the port's come from vessel_seed."""
    save_png(casing_texture(look, seed, hole=True), asset('textures', 'block', name + '_axis.png'))
    save_png(pressurizer_dial_texture(seed + 1, look), asset('textures', 'block', name + '_front.png'))
    save_png(casing_texture(look, seed + 2), asset('textures', 'block', name + '_top.png'))
    save_png(sooted_texture(casing_texture(look, seed + 3), seed + 3), asset('textures', 'block', name + '_bottom.png'))
    save_png(pressurizer_port_texture(vessel_seed, look), asset('textures', 'block', name + '_side.png'))
    save_png(pressurizer_vessel_side_texture(vessel_seed + 1, look),
             asset('textures', 'block', name + '_vessel_side.png'))
    save_png(pressurizer_vessel_top_texture(vessel_seed + 2, look), asset('textures', 'block', name + '_vessel_top.png'))


def pressurizer_models(name):
    """A Pressurizer's block models in one metal, drawn facing north (its dial) and turned by its blockstate: one with
    a vessel on it, and name_open for one from before 0.24.0 with no room for a vessel, whose top is a port that still
    takes pipes; its vessel's, and the nozzle its vessel puts out to a pipe."""
    tex = lambda face_name: f'mcremix:block/{name}_{face_name}'
    body = {'front': tex('front'), 'back': tex('top'), 'axis': tex('axis'), 'top': tex('top'), 'port': tex('side'),
            'bottom': tex('bottom'), 'particle': tex('axis')}
    write_json(asset('models', 'block', name + '.json'), {'textures': body, 'elements': pressurizer_body_elements()})
    write_json(asset('models', 'block', name + '_open.json'),
               {'textures': body, 'elements': pressurizer_body_elements('#port')})
    vessel = {'side': tex('vessel_side'), 'top': tex('vessel_top'), 'valve': 'mcremix:block/pressurizer_valve',
              'port': tex('side'), 'particle': tex('vessel_side')}
    write_json(asset('models', 'block', name + '_vessel.json'),
               {'textures': vessel, 'elements': vessel_elements('#side', '#top', '#valve')})
    write_json(asset('models', 'block', name + '_vessel_nozzle.json'),
               {'textures': vessel, 'elements': vessel_nozzle_elements('#side', '#port')})


def vessel_multipart(model, when=None):
    """A vessel's blockstate parts: its tank, and a nozzle out of each side a pipe joins (model_nozzle, turned from
    north). when, if given, is the rest of the condition each part shows on (a titanium one's patina)."""
    cond = lambda extra: dict(when or {}, **extra)
    parts = [dict({'when': when} if when else {}, apply={'model': model})]
    for side, turn in FACING_TURNS:
        apply = {'model': model + '_nozzle'}
        if turn:
            apply['y'] = turn
        parts.append({'when': cond({side: 'true'}), 'apply': apply})
    return parts


def pressurizer_item_model(name):
    """The item shows the Pressurizer with its vessel on it, two blocks tall, shrunk to fit a slot."""
    tex = lambda face_name: f'mcremix:block/{name}_{face_name}'
    write_json(asset('models', 'item', name + '.json'), {
        'parent': 'minecraft:block/block',
        'textures': {'front': tex('front'), 'back': tex('top'), 'axis': tex('axis'), 'top': tex('top'),
                     'bottom': tex('bottom'), 'vessel_side': tex('vessel_side'), 'vessel_top': tex('vessel_top'),
                     'valve': 'mcremix:block/pressurizer_valve', 'particle': tex('axis')},
        'elements': pressurizer_body_elements() + vessel_elements('#vessel_side', '#vessel_top', '#valve', lift=16),
        'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, -3.2, 0], 'scale': [0.4, 0.4, 0.4]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 1, 0], 'scale': [0.18, 0.18, 0.18]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, -2.8, 0], 'scale': [0.35, 0.35, 0.35]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 1.5, 0], 'scale': [0.25, 0.25, 0.25]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, -1.5, 0], 'scale': [0.28, 0.28, 0.28]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, -1.5, 0], 'scale': [0.28, 0.28, 0.28]}}})


FACING_TURNS = (('north', 0), ('east', 90), ('south', 180), ('west', 270))


def gen_pressurizer():
    """The Pressurizer (Taran, 2026-10-22; two blocks since 0.24.0): a steel body with its dial on the front (north,
    turned by facing) and its shaft through its two sides (since 0.26.0), under a square steel pressure vessel narrower
    than a block, which takes pipes and funnels on its four sides and puts a nozzle out to each pipe. Its recipes: ethylene into polyethylene pellets at 512 RPM over heat, vinyl chloride
    into PVC resin at 256, water into pressurised steam at 128 over a Hot fire."""
    pressurizer_textures('pressurizer', 'steel', 1320, 1400)
    save_png(pressurizer_valve_texture(), asset('textures', 'block', 'pressurizer_valve.png'))
    save_png(pressurizer_needle_texture(), asset('textures', 'block', 'pressurizer_needle.png'))
    write_json(asset('models', 'block', 'pressurizer', 'needle.json'), needle_model())
    pressurizer_models('pressurizer')
    pressurizer_item_model('pressurizer')
    variants = {}
    for facing, turn in FACING_TURNS:
        for vessel in ('true', 'false'):
            variant = {'model': 'mcremix:block/pressurizer' + ('' if vessel == 'true' else '_open')}
            if turn:
                variant['y'] = turn
            variants[f'facing={facing},vessel={vessel}'] = variant
    write_json(asset('blockstates', 'pressurizer.json'), {'variants': variants})
    write_json(asset('blockstates', 'pressurizer_vessel.json'),
               {'multipart': vessel_multipart('mcremix:block/pressurizer_vessel')})
    write_json(data('mcremix', 'loot_tables', 'blocks', 'pressurizer.json'), loot_self('pressurizer'))
    # a steel tank to hold the pressure, a steel shaft to drive it, on a steel casing
    recipe('pressurizer', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' S ', 'PTP', ' C '],
        'key': {'S': item('mcremix:steel_shaft'), 'P': {'tag': 'forge:plates/steel'},
                'T': item('mcremix:steel_fluid_tank'), 'C': item('mcremix:steel_casing')},
        'result': item('mcremix:pressurizer')})
    for name, feed, result, heat, speed, time in (
            ('polyethylene_pellets', {'fluid': 'mcremix:ethylene', 'amount': 100}, item('mcremix:polyethylene_pellets'),
             'warm', 512, 60),
            ('pvc_resin', {'fluid': 'mcremix:vinyl_chloride', 'amount': 100}, item('mcremix:pvc_resin'), 'cold', 256, 80),
            ('pressurized_steam', {'fluidTag': 'minecraft:water', 'amount': 20},
             {'fluid': 'mcremix:pressurized_steam', 'amount': 20}, 'hot', 128, 1)):
        recipe(f'pressurizing/{name}', {'type': 'mcremix:pressurizing', 'heat': heat, 'min_speed': speed,
                                        'processingTime': time, 'ingredients': [feed], 'results': [result]})


def tint_patinas():
    """(patina, texture prefix) for each value of a titanium machine's patina: its four temper colours, waxed or not."""
    out = []
    for waxed in ('', 'waxed_'):
        for stage, prefix in TINT_PREFIXES.items():
            out.append(((waxed + stage if stage != 'unaffected' else waxed.rstrip('_') or 'none'), prefix))
    return out


def gen_titanium_pressurizer():
    """The titanium Pressurizer (Taran, stage 4: steel now, titanium later): the steel one's body and vessel in
    titanium, in each temper colour it takes as it heats, kept in its patina as on every titanium machine; its vessel
    shows the colour of the body under it. Its recipe is the steel one's in titanium, round the same steel tank."""
    for n, (stage, prefix) in enumerate(TINT_PREFIXES.items()):
        name = prefix + 'titanium_pressurizer'
        pressurizer_textures(name, prefix + 'titanium', 1330 + 4 * n, 1403 + 3 * n)
        pressurizer_models(name)
    pressurizer_item_model('titanium_pressurizer')
    variants = {}
    for facing, turn in FACING_TURNS:
        for vessel in ('true', 'false'):
            for patina, prefix in tint_patinas():
                variant = {'model': f'mcremix:block/{prefix}titanium_pressurizer' + ('' if vessel == 'true' else '_open')}
                if turn:
                    variant['y'] = turn
                variants[f'facing={facing},patina={patina},vessel={vessel}'] = variant
    write_json(asset('blockstates', 'titanium_pressurizer.json'), {'variants': variants})
    patinas = {}
    for patina, prefix in tint_patinas():
        patinas.setdefault(prefix, []).append(patina)
    write_json(asset('blockstates', 'titanium_pressurizer_vessel.json'), {'multipart': [
        part for prefix, names in patinas.items()
        for part in vessel_multipart(f'mcremix:block/{prefix}titanium_pressurizer_vessel', {'patina': '|'.join(names)})]})
    write_json(data('mcremix', 'loot_tables', 'blocks', 'titanium_pressurizer.json'), loot_self('titanium_pressurizer'))
    recipe('titanium_pressurizer', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' S ', 'PTP', ' C '],
        'key': {'S': item('mcremix:titanium_shaft'), 'P': {'tag': 'forge:plates/titanium'},
                'T': item('mcremix:steel_fluid_tank'), 'C': item('mcremix:titanium_casing')},
        'result': item('mcremix:titanium_pressurizer')})


def pressurizer_lang():
    return {
        'block.mcremix.pressurizer': 'Pressurizer',
        'block.mcremix.pressurizer.tooltip.summary': 'Squeezes the fluid piped into it, its _shaft_ working a pump into the _pressure vessel_ on top. _Speed is pressure_: each job needs a _least speed_, and faster than that is quicker. It needs _room above_ for its vessel.',
        'block.mcremix.pressurizer.tooltip.condition1': 'What it squeezes',
        'block.mcremix.pressurizer.tooltip.behaviour1': '_Ethylene_ into _polyethylene pellets_ at _512 RPM_, with _heat_ under it. _Vinyl chloride_ into _PVC resin_ at _256 RPM_, heated or not. _Water_ into _pressurised steam_ at _128 RPM_, over a _Hot_ fire.',
        'block.mcremix.pressurizer.tooltip.condition2': 'Pressurised steam',
        'block.mcremix.pressurizer.tooltip.behaviour2': 'It is all a _steel boiler_ runs on: every _10 mB a tick_ piped in is a boiler level. It makes _20 mB a tick at 128 RPM_, twice that at 256 and _80 at 512_.',
        'block.mcremix.pressurizer.tooltip.condition3': 'Pipes and funnels',
        'block.mcremix.pressurizer.tooltip.behaviour3': 'Fluid goes in at any of the _four sides_ of its _vessel_, which reaches out to meet each pipe, and what it makes is drawn off the same sides: fluid by _pumps_, items by _funnels_ or an _empty hand_. Its _shaft_ goes in at _either side_ of the lower block, left or right of the dial.',
        'block.mcremix.pressurizer.tooltip.condition4': 'The dial',
        'block.mcremix.pressurizer.tooltip.behaviour4': 'Reads the _pressure_, which is how fast it turns: from nothing to the _top speed_ of its metal, each mark a quarter of it. Past top speed the needle is in the _red_.',
        'block.mcremix.pressurizer_vessel': 'Pressurizer Vessel',
        'block.mcremix.titanium_pressurizer_vessel': 'Titanium Pressurizer Vessel',
        'block.mcremix.titanium_pressurizer': 'Titanium Pressurizer',
        'block.mcremix.titanium_pressurizer.tooltip.summary': 'The Pressurizer in _titanium_: it takes up to _2048 RPM_ where the steel one stops at _512_, so it squeezes up to _four times as fast_. Like all titanium it _heats as it turns_ and _burns_ if nothing cools it: past about _128 RPM_ give it a _Water Cooler_.',
        'block.mcremix.titanium_pressurizer.tooltip.condition1': 'What it squeezes',
        'block.mcremix.titanium_pressurizer.tooltip.behaviour1': 'The same as the steel one: _ethylene_ into _polyethylene pellets_ at _512 RPM_ over _heat_, _vinyl chloride_ into _PVC resin_ at _256 RPM_, _water_ into _pressurised steam_ at _128 RPM_ over a _Hot_ fire. Twice as fast is twice as quick, and steam comes at up to _320 mB a tick_ at 2048.',
        'block.mcremix.titanium_pressurizer.tooltip.condition2': 'Temper colours',
        'block.mcremix.titanium_pressurizer.tooltip.behaviour2': 'Heat turns it _gold_, _purple_ and _blue_ for good; _hydrochloric acid_ strips it back.',
        'mcremix.jei.pressurizing': 'Pressurizing',
        'mcremix.jei.pressurizing.heated': '%s RPM \u00b7 %s \u00b7 %s',
        'mcremix.jei.pressurizing.unheated': '%s RPM \u00b7 %s',
        'mcremix.jei.pressurizing.seconds': '%ss',
        'mcremix.jei.pressurizing.rate': '%s mB a tick',
        'mcremix.jei.pressurizing.speed': 'Its least speed, %s RPM: nothing happens slower, and twice as fast is twice as quick',
        'mcremix.jei.pressurizing.heat': 'The heat under the Pressurizer, at least; any hotter works the same',
        'mcremix.goggles.pressurizer.idle': 'Idle: pipe in what to squeeze',
        'mcremix.goggles.pressurizer.working': 'Squeezing',
        'mcremix.goggles.pressurizer.too_slow': 'Too slow: this needs %s RPM',
        'mcremix.goggles.pressurizer.too_cold': 'Too cold: this needs %s heat under it',
        'mcremix.goggles.pressurizer.waiting': 'Waiting for a full batch',
        'mcremix.goggles.pressurizer.full': 'Full: draw off what it made',
        'mcremix.goggles.pressurizer.feed': 'Holding %s, %s mB',
        'mcremix.goggles.pressurizer.output': 'Made %s, %s mB',
        'mcremix.goggles.pressurizer.made': 'Made %s %s',
        'mcremix.boiler.steam': 'Steam',
        # padded as Create pads Water, so its bar lines up with Size's
        'mcremix.boiler.steam_dots': '... ',
        'mcremix.boiler.steam_input_rate': 'Pressurised steam input rate',
    }


# ------------------------------------------------------------------------------------------------ cooling (0.18.0)

COPPER_STAGES = ('unaffected', 'exposed', 'weathered', 'oxidized')


def copper_look(stage):
    """The palette of copper at a weathering stage: 'copper', 'exposed_copper' and so on."""
    return stage_prefix('copper', stage) + 'copper'


def look_suffix(stage):
    """What a copper look's textures and models are named with: nothing when fresh, '_exposed' and so on."""
    return '' if stage == 'unaffected' else '_' + stage


def speckle(col, palette, rnd):
    """Now and then a dot of the next stage's colour on weathered copper, none on fresh. Its random numbers are its
    own, so the grain under the specks is the same at every stage."""
    if palette[3] and rnd.random() < 0.1:
        return mix(col, palette[3], 0.6)
    return col


def patina_keys(stage):
    """The patina values that look like a stage, as a multipart condition: 'none|waxed' and so on."""
    return '|'.join(patina for patina, look in PATINAS.items() if look == stage)


def water_cooler_top_texture(seed, stage='unaffected'):
    """The top of the Water Cooler's copper jacket (columns and rows 1 to 14): a lid with a bevelled rim, lit on its
    upper left, a steel screw in each corner, and in the middle a recessed plate with the serpentine channel the water
    runs through pressed into it. Its copper at a weathering stage, for the patina the jacket takes."""
    palette = PALETTES[copper_look(stage)]
    rnd, specks = random.Random(seed), random.Random(seed + 7919)
    # the channel: three runs across, joined at alternate ends
    channel = {(x, y) for y in (5, 7, 9) for x in range(5, 11)} | {(10, 6), (5, 8), (10, 10)} \
        | {(x, 10) for x in range(6, 11)}
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            n = rnd.uniform(-0.04, 0.04)
            edge = min(x, y, 15 - x, 15 - y)
            if edge <= 1:
                # the rim: lit along the top and left, shadowed along the bottom and right
                t = 0.82 if (x <= 1 or y <= 1) and x < 14 and y < 14 else 0.3
            elif edge == 2:
                t = 0.6
            elif edge == 3:
                # the plate is let into the lid: a shadow under its top and left lip, light on the far edges
                t = 0.28 if x == 3 or y == 3 else 0.7
            elif (x, y) in channel:
                t = 0.24
            else:
                t = 0.52
            img.putpixel((x, y), speckle(ramp(palette, t + n), palette, specks) + (255,))
    steel = PALETTES['iron']
    for sx, sy in ((2, 2), (13, 2), (2, 13), (13, 13)):
        img.putpixel((sx, sy), ramp(steel, 0.7) + (255,))
    return img


def water_cooler_side_texture(seed, stage='unaffected'):
    """The sides of the Water Cooler's jacket (columns 1 to 14, rows 4 to 11): the lid's lit edge and its seam over
    the body, the lid's screws, and a round pipe fitting in the middle, a dark bore in a lit ring, where water comes
    in or goes out. Its copper at a weathering stage, as the top's."""
    palette = PALETTES[copper_look(stage)]
    rnd, specks = random.Random(seed), random.Random(seed + 7919)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        streak = rnd.uniform(-0.05, 0.05)
        for x in range(16):
            t = {4: 0.85, 5: 0.6, 6: 0.22, 11: 0.3}.get(y, 0.5 + streak) + rnd.uniform(-0.04, 0.04)
            if y == 5 and x in (2, 13):
                t = 0.2  # a screw holding the lid down
            r = math.hypot(x - 7.5, y - 8.5)
            if r < 1.2:
                t = 0.08  # the fitting's bore
            elif r < 2:
                t = 0.88 if x + y < 16 else 0.4
            img.putpixel((x, y), speckle(ramp(palette, t), palette, specks) + (255,))
    return img


def water_cooler_base_texture(seed):
    """The Water Cooler's aluminium base plate, as the Heat Sink's: rows 0 and 1 are its edge, a lit chamfer over a
    grey thermal pad; drawn as its top, only a pixel shows round the jacket, lit on the upper left."""
    palette = PALETTES['aluminium']
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            n = rnd.uniform(-0.03, 0.03)
            if y == 1 and 0 < x < 15:
                col = mix((0x4E, 0x54, 0x5E), (0x6A, 0x70, 0x7A), 0.5 + n * 4)
            else:
                col = ramp(palette, (0.82 if y == 0 or x == 0 else 0.4 if y == 15 or x == 15 else 0.6) + n)
            img.putpixel((x, y), col + (255,))
    return img


def water_cooler_model():
    """Drawn bolted to the block below: a heat sink's base plate, two pixels thick, under a copper water jacket eight
    high. WaterCoolerBlock's shape is the same two boxes."""
    s, t, j = '#base', '#top', '#side'
    base = box([0, 0, 0], [16, 2, 16], dict(
        {'down': {'uv': [0, 0, 16, 16], 'texture': s, 'cullface': 'down'}, 'up': {'uv': [0, 0, 16, 16], 'texture': s}},
        **{side: {'uv': [0, 0, 16, 2], 'texture': s, 'cullface': side} for side in ('north', 'south', 'east', 'west')}))
    jacket = box([1, 2, 1], [15, 10, 15], dict(
        {'up': {'uv': [1, 1, 15, 15], 'texture': t}},
        **{side: {'uv': [1, 4, 15, 12], 'texture': j} for side in ('north', 'south', 'east', 'west')}))
    return {'parent': 'minecraft:block/block',
            'textures': {'base': 'mcremix:block/water_cooler_base', 'top': 'mcremix:block/water_cooler_top',
                         'side': 'mcremix:block/water_cooler_side', 'particle': 'mcremix:block/water_cooler_side'},
            'elements': [base, jacket]}


def radiator_header_texture(seed):
    """The inside and edges of a radiator's front and back plates, brushed aluminium with no rim, so the plates of
    radiators joined into a panel run on as one. Rows 0 and 1 and rows 14 and 15 are the edges: the tank crimped onto
    the plate, a lit lip on the outside and a row of crimped tabs inside it."""
    palette = PALETTES['aluminium']
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if y in (0, 15):
                t = 0.84
            elif y in (1, 14):
                t = 0.62 if x % 4 in (1, 2) else 0.3
            else:
                t = 0.42
            img.putpixel((x, y), ramp(palette, t + rnd.uniform(-0.04, 0.04)) + (255,))
    return img


def radiator_panel_texture(seed, stage='unaffected'):
    """The outside of a radiator's front and back plates: a pressed aluminium tank face with a rib across the middle
    each way, lit on one side and shaded on the other, and the ends of the four copper tubes brazed into it in rings,
    so radiators joined into a panel show an even grid of them. The copper at a weathering stage."""
    palette, copper = PALETTES['aluminium'], PALETTES[copper_look(stage)]
    rnd, specks = random.Random(seed), random.Random(seed + 7919)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            t = 0.55 + rnd.uniform(-0.03, 0.03)
            if x == 7 or y == 7:
                t = 0.8
            elif x == 8 or y == 8:
                t = 0.32
            col = ramp(palette, t)
            # the tube ends at the four corners of the core: a dark bore in a ring of braze, lit on its upper left
            dx, dy = x % 8 - 4, y % 8 - 4
            if -2 <= dx <= 1 and -2 <= dy <= 1 and not (dx in (-2, 1) and dy in (-2, 1)):
                if dx in (-1, 0) and dy in (-1, 0):
                    col = ramp(copper, 0.12 if (dx, dy) == (0, 0) else 0.3)
                else:
                    col = speckle(ramp(copper, 0.85 if dx + dy < -1 else 0.55 if dx + dy < 1 else 0.35), copper,
                                  specks)
            img.putpixel((x, y), col + (255,))
    return img


def radiator_fin_texture(seed):
    """A radiator's thin aluminium fins: rows 1 to 14 are a fin's face, pressed with louvres, slits lit on one lip and
    dark on the other, that turn the air through it; row 0 is its edge, seen side on, rippled where the louvres
    bend."""
    palette = PALETTES['aluminium']
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if y == 0:
                t = (0.86, 0.62, 0.34, 0.62)[x % 4]
            elif 2 <= x <= 13 and (y - 1) % 3 == 1:
                t = 0.78
            elif 2 <= x <= 13 and (y - 1) % 3 == 2:
                t = 0.3
            else:
                t = 0.55
            img.putpixel((x, y), ramp(palette, t + rnd.uniform(-0.03, 0.03)) + (255,))
    return img


def radiator_tube_texture(seed, stage='unaffected'):
    """Copper tube, as Create's fluid pipes are, lit down one side with a bright line where it catches the light:
    any two columns side by side are a tube's two faces. Its copper at a weathering stage."""
    palette = PALETTES[copper_look(stage)]
    rnd, specks = random.Random(seed), random.Random(seed + 7919)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            t = (0.9 if x % 4 == 0 else 0.62 if x % 4 == 1 else 0.38 if x % 4 == 2 else 0.22) + rnd.uniform(-0.04, 0.04)
            img.putpixel((x, y), speckle(ramp(palette, t), palette, specks) + (255,))
    return img


# fins half a pixel off the grid, so the gaps at both ends match; tubes at the four corners of the core
RADIATOR_FINS = [3.5, 5.5, 7.5, 9.5, 11.5]
RADIATOR_TUBES = [(3, 3), (11, 3), (3, 11), (11, 11)]


def radiator_model():
    """Drawn along y: a plate two pixels deep at each end, the panel's front and back where pipes join, five thin
    aluminium fins across it between them, and four copper tubes running through the fins from plate to plate. Air
    blows through the gaps; RadiatorBlock's shape is the plates and a core."""
    h, k, f, t = '#header', '#panel', '#fin', '#tube'
    elements = []
    for y0, y1, end in ((0, 2, 'down'), (14, 16, 'up')):
        faces = {end: {'uv': [0, 0, 16, 16], 'texture': k, 'cullface': end},
                 ('up' if end == 'down' else 'down'): {'uv': [0, 0, 16, 16], 'texture': h}}
        for side in ('north', 'south', 'east', 'west'):
            faces[side] = {'uv': [0, 16 - y1, 16, 16 - y0], 'texture': h, 'cullface': side}
        elements.append(box([0, y0, 0], [16, y1, 16], faces))
    for y in RADIATOR_FINS:
        faces = {'up': {'uv': [1, 1, 15, 15], 'texture': f}, 'down': {'uv': [1, 1, 15, 15], 'texture': f}}
        for side in ('north', 'south', 'east', 'west'):
            faces[side] = {'uv': [1, 0, 15, 1], 'texture': f}
        elements.append(box([1, y, 1], [15, y + 1, 15], faces))
    for x, z in RADIATOR_TUBES:
        elements.append(box([x, 2, z], [x + 2, 14, z + 2],
                            {side: {'uv': [0, 2, 2, 14], 'texture': t} for side in ('north', 'south', 'east', 'west')}))
    return {'parent': 'minecraft:block/block',
            'textures': {'header': 'mcremix:block/radiator_header', 'panel': 'mcremix:block/radiator_panel',
                         'fin': 'mcremix:block/radiator_fin', 'tube': 'mcremix:block/radiator_tube',
                         'particle': 'mcremix:block/radiator_header'},
            'elements': elements}


def gen_cooling():
    """The Water Cooler and the Radiator. The copper of each weathers by its patina, for looks only (Taran, 0.23.0):
    a model for each stage, which waxed ones keep."""
    for stage in COPPER_STAGES:
        sfx = look_suffix(stage)
        save_png(water_cooler_top_texture(1270, stage), asset('textures', 'block', f'water_cooler_top{sfx}.png'))
        save_png(water_cooler_side_texture(1274, stage), asset('textures', 'block', f'water_cooler_side{sfx}.png'))
        if sfx:
            write_json(asset('models', 'block', f'water_cooler{sfx}.json'), {
                'parent': 'mcremix:block/water_cooler',
                'textures': {'top': f'mcremix:block/water_cooler_top{sfx}', 'side': f'mcremix:block/water_cooler_side{sfx}',
                             'particle': f'mcremix:block/water_cooler_side{sfx}'}})
    save_png(water_cooler_base_texture(1275), asset('textures', 'block', 'water_cooler_base.png'))
    write_json(asset('models', 'block', 'water_cooler.json'), water_cooler_model())
    write_json(asset('models', 'item', 'water_cooler.json'), {'parent': 'mcremix:block/water_cooler'})
    write_json(asset('blockstates', 'water_cooler.json'), {'variants': {
        f'facing={facing},patina={patina}': dict({'model': f'mcremix:block/water_cooler{look_suffix(stage)}'}, **rot)
        for facing, rot in DOWN_TURNS.items() for patina, stage in PATINAS.items()}})
    write_json(data('mcremix', 'loot_tables', 'blocks', 'water_cooler.json'), loot_self('water_cooler'))
    # a heat sink with a copper jacket and two pipe fittings on it
    recipe('water_cooler', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [item('mcremix:heat_sink'), item('create:fluid_pipe'), item('create:fluid_pipe'),
                        {'tag': 'forge:plates/copper'}, {'tag': 'forge:plates/copper'}],
        'result': item('mcremix:water_cooler')})

    save_png(radiator_header_texture(1271), asset('textures', 'block', 'radiator_header.png'))
    for stage in COPPER_STAGES:
        sfx = look_suffix(stage)
        save_png(radiator_panel_texture(1273, stage), asset('textures', 'block', f'radiator_panel{sfx}.png'))
        save_png(radiator_tube_texture(1272, stage), asset('textures', 'block', f'radiator_tube{sfx}.png'))
        if sfx:
            write_json(asset('models', 'block', f'radiator{sfx}.json'), {
                'parent': 'mcremix:block/radiator',
                'textures': {'panel': f'mcremix:block/radiator_panel{sfx}', 'tube': f'mcremix:block/radiator_tube{sfx}'}})
    save_png(radiator_fin_texture(1276), asset('textures', 'block', 'radiator_fin.png'))
    write_json(asset('models', 'block', 'radiator.json'), radiator_model())
    write_json(asset('models', 'item', 'radiator.json'), {'parent': 'mcremix:block/radiator'})
    turns = {'y': {}, 'z': {'x': 90}, 'x': {'x': 90, 'y': 90}}
    write_json(asset('blockstates', 'radiator.json'), {'variants': {
        f'axis={axis},patina={patina}': dict({'model': f'mcremix:block/radiator{look_suffix(stage)}'}, **rot)
        for axis, rot in turns.items() for patina, stage in PATINAS.items()}})
    write_json(data('mcremix', 'loot_tables', 'blocks', 'radiator.json'), loot_self('radiator'))
    # thin fins on tubes: three pipes between two columns of sheets make two
    recipe('radiator', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['SPS', 'SPS', 'SPS'],
        'key': {'S': {'tag': 'forge:plates/aluminium'}, 'P': item('create:fluid_pipe')},
        'result': item('mcremix:radiator', 2)})
    # a fan's air blows on through a radiator, so one fan under a panel cools the whole column above it
    write_json(data('create', 'tags', 'blocks', 'fan_transparent.json'), tag(['mcremix:radiator']))
    # ethylene dichloride boiled with caustic soda: the soda takes its chlorine as salt, leaving ethylene glycol, and
    # the salt water goes back to be electrolysed
    recipe('mixing/ethylene_glycol', {
        'type': 'create:mixing', 'heatRequirement': 'heated',
        'ingredients': [{'fluid': 'mcremix:ethylene_dichloride', 'amount': 250},
                        {'fluid': 'mcremix:caustic_soda', 'amount': 250}],
        'results': [{'fluid': 'mcremix:ethylene_glycol', 'amount': 250},
                    {'fluid': 'mcremix:salt_water', 'amount': 250}]})
    # coolant is glycol and water, half and half
    recipe('mixing/coolant', {
        'type': 'create:mixing',
        'ingredients': [{'fluid': 'mcremix:ethylene_glycol', 'amount': 250},
                        {'fluid': 'minecraft:water', 'amount': 250}],
        'results': [{'fluid': 'mcremix:coolant', 'amount': 500}]})


def cooling_lang():
    return {
        'block.mcremix.water_cooler': 'Water Cooler',
        'block.mcremix.water_cooler.tooltip.summary': (
            'A heat sink with a _water jacket_. Bolted onto a _Power Grid_ device or an _aluminium_ or _titanium '
            'support_, it draws the heat into _water_ piped through it, faster than any heat sink, and gives it '
            'back as _warm water_. _Cold water_ cools twice as fast, and _coolant_ three times as fast, coming out '
            '_hot_.'),
        'block.mcremix.water_cooler.tooltip.condition1': 'When placed',
        'block.mcremix.water_cooler.tooltip.behaviour1': (
            'Its base goes against the block you place it on. Pipe water or coolant in on any other face, and pipe '
            'what comes out away, or it stops when it is full.'),
        'block.mcremix.radiator': 'Radiator',
        'block.mcremix.radiator.tooltip.summary': (
            'Thin aluminium fins on copper tubes. It chills any _water_ into _cold water_, and _hot coolant_ back into '
            '_coolant_: slowly on its own, _much faster_ with an encased fan blowing through it. Cold water and '
            'coolant pass straight through.'),
        'block.mcremix.radiator.tooltip.condition1': 'When placed',
        'block.mcremix.radiator.tooltip.behaviour1': (
            'It faces you, or the radiator you place it against. Radiators _side by side_ facing the same way _join_ '
            'into one panel that shares what it holds, with pipes on its front or back anywhere. Every block chills its '
            'share, so a bigger panel chills more, and a _fan under each column_ blowing up cools the whole column.'),
        'mcremix.goggles.water_cooler.device': 'Cooling something at %s °C',
        'mcremix.goggles.water_cooler.cooling': 'Cooling',
        'mcremix.goggles.water_cooler.idle': 'Nothing warm to cool',
        'mcremix.goggles.water_cooler.loose': 'Not bolted to anything that heats up',
        'mcremix.goggles.water_cooler.dry': 'Nothing to cool with',
        'mcremix.goggles.water_cooler.full': 'What it gives back has nowhere to go',
        'mcremix.goggles.radiator.chilling': 'Chilling %s mB a second',
        'mcremix.goggles.radiator.passing': 'Already cold: passing straight through',
        'mcremix.goggles.radiator.idle': 'Nothing to chill',
        'mcremix.goggles.radiator.full': 'Full: what it gives out has nowhere to go',
        'mcremix.goggles.radiator.panel': '%s panel of %s blocks',
        'mcremix.goggles.radiator.fans': 'Fans blowing through it: %sx as fast',
        'mcremix.goggles.radiator.fanned_blocks': 'Fans blowing through %s of its %s blocks',
        'mcremix.goggles.radiator.still': 'Still air: a fan blowing through it chills much faster',
        'mcremix.goggles.coolant': '%s: %s / %s mB',
        'mcremix.jei.info.water_cooler': (
            'Bolt it onto a Power Grid device or an aluminium or titanium support and pipe water, cold water or coolant '
            'through it. It takes heat in step with how far above the air that is: on water 15 W for each degree, on '
            'cold water twice that, on coolant three times. Water comes out warm, coolant hot.'),
        'mcremix.jei.info.radiator': (
            'Chills any water into cold water, and hot coolant back into coolant: half a millibucket a tick for each '
            'block in still air, and much faster with a fan blowing through it. Radiators side by side join into a '
            'panel. Cold water and coolant pass straight through.'),
        'mcremix.jei.info.warm_water': (
            'What a Water Cooler gives back for water. A Radiator chills it into cold water. Poured out, it is only '
            'water.'),
        'mcremix.jei.info.cold_water': (
            'Taken by bucket, pump or hose pulley in any snowy biome or wherever else it snows, or chilled by a '
            'Radiator. In a Water Cooler it '
            'takes heat twice as fast as water. Poured out, it is only water.'),
        'mcremix.jei.info.coolant': (
            'Glycol and water. In a Water Cooler it takes heat three times as fast as water and comes out as hot '
            'coolant, which a Radiator cools back. It never wears out, so keep it going round a loop of coolers and '
            'radiators.'),
        'mcremix.jei.info.hot_coolant': (
            'What a Water Cooler gives back for coolant. A Radiator cools it back into coolant. Poured out, it is only '
            'coolant.'),
    }


def clean():
    """Everything under assets/mcremix and data/ is generated; start from nothing so renamed files do not linger."""
    import shutil
    for d in (ASSETS, DATA):
        if os.path.isdir(d):
            shutil.rmtree(d)


# ------------------------------------------------------------------------------------------------ encased parts

# every casing a shaft or cog can be encased in (EncasingCasing), each with an encased shaft, cog and large cog
ENCASING = ['andesite', 'brass', 'copper', 'iron', 'steel', 'aluminium', 'titanium']
ENCASED_KINDS = ['shaft', 'cogwheel', 'large_cogwheel']


def encasing_looks():
    """Each casing's encased blocks go by its name, and titanium's by each of its tints as well (Tints)."""
    return [look for casing in ENCASING for look in (TINTED if casing == 'titanium' else [casing])]


def encased_blocks():
    return [f'mcremix:{look}_encased_{kind}' for look in encasing_looks() for kind in ENCASED_KINDS]


def encasing_textures(casing):
    """The textures an encased block in this casing takes: the casing itself, the opening a shaft comes out of, the
    inside seen through a cog's slot, and the slotted side (a sheet that joins up, for large cogs). Andesite and brass
    are Create's own; copper is Create's casing with an opening and slot cut in; iron, steel, aluminium and titanium are
    this mod's casings and gearbox faces."""
    if casing in ('andesite', 'brass'):
        return {'casing': f'create:block/{casing}_casing',
                'opening': 'create:block/gearbox' if casing == 'andesite' else 'create:block/brass_gearbox',
                'inside': 'minecraft:block/stripped_' + ('spruce' if casing == 'andesite' else 'dark_oak') + '_log_top',
                'side': f'create:block/{casing}_encased_cogwheel_side'}
    own = {'copper': 'create:block/copper_casing'}
    return {'casing': own.get(casing, f'mcremix:block/{casing}_casing'),
            'opening': f'mcremix:block/{casing}_encased_opening' if casing in own else f'mcremix:block/{casing}_gearbox',
            'inside': 'minecraft:block/stripped_spruce_log_top',
            'side': f'mcremix:block/{casing}_encased_cogwheel_side'}


def encasing_base(casing):
    """The face of a casing Create has no encased blocks in, as a 16 by 16 image."""
    if casing == 'copper':
        rows = create_texture('copper_casing')
        img = Image.new('RGBA', (16, 16))
        for y, row in enumerate(rows):
            for x, px in enumerate(row):
                img.putpixel((x, y), px)
        return img
    return casing_texture(casing, 600 + TEXTURE_STAGES.index(casing))


def encased_cog_side(base, open_left=False, open_right=False):
    """A casing face with the slot a small cog's teeth show through, as on Create's encased cogs: four pixels high
    across the panel, a little shadow on its upper lip, and open through the frame on a side that joins the next cog."""
    img = base.copy()
    for x in range(2, 14):
        img.putpixel((x, 5), mix(img.getpixel((x, 5))[:3], (0, 0, 0), 0.3) + (255,))
        img.putpixel((x, 10), mix(img.getpixel((x, 10))[:3], (0, 0, 0), 0.15) + (255,))
    x0, x1 = (0 if open_left else 2), (16 if open_right else 14)
    for y in range(6, 10):
        for x in range(x0, x1):
            img.putpixel((x, y), (0, 0, 0, 0))
    return img


def encased_cog_side_connected(base):
    """The two by two sheet the slotted sides join up from, laid out as Create's: alone, joined on the left, joined on
    the right, joined on both. Large encased cogs take their sides from it too."""
    sheet = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    for tx, ty in ((0, 0), (1, 0), (0, 1), (1, 1)):
        sheet.paste(encased_cog_side(base, open_left=tx == 1, open_right=ty == 1), (tx * 16, ty * 16))
    return sheet


def gen_encased_parts():
    for casing in encasing_looks():
        if casing in ('andesite', 'brass'):
            continue
        base = encasing_base(casing)
        if casing == 'copper':
            opening = base.copy()
            bushing(opening, casing)
            save_png(opening, asset('textures', 'block', f'{casing}_encased_opening.png'))
        save_png(encased_cog_side(base), asset('textures', 'block', f'{casing}_encased_cogwheel_side.png'))
        save_png(encased_cog_side_connected(base), asset('textures', 'block', f'{casing}_encased_cogwheel_side_connected.png'))

    for casing in encasing_looks():
        t = encasing_textures(casing)
        create_own = casing in ('andesite', 'brass')
        shaft_model = f'create:block/encased_shaft/block_{casing}' if create_own else f'mcremix:block/{casing}_encased_shaft'
        if not create_own:
            write_json(asset('models', 'block', f'{casing}_encased_shaft.json'), {
                'parent': 'create:block/encased_shaft/block',
                'textures': {'casing': t['casing'], 'opening': t['opening']}})
        turns = {'x': {'x': 90, 'y': 90}, 'y': {}, 'z': {'x': 90, 'y': 180}}
        write_json(asset('blockstates', f'{casing}_encased_shaft.json'), {'variants': {
            f'axis={axis}': dict({'model': shaft_model, 'uvlock': True}, **turn) for axis, turn in turns.items()}})
        for kind in ('cogwheel', 'large_cogwheel'):
            name = f'{casing}_encased_{kind}'
            variants = {}
            for top in ('false', 'true'):
                for bottom in ('false', 'true'):
                    suffix = ('_top' if top == 'true' else '') + ('_bottom' if bottom == 'true' else '')
                    model = (f'create:block/{name}{suffix}' if create_own else f'mcremix:block/{name}{suffix}')
                    if not create_own:
                        write_json(asset('models', 'block', f'{name}{suffix}.json'), {
                            'parent': f'create:block/encased_{kind}/block{suffix}',
                            'textures': {'1': t['inside'], '4': t['opening'], 'casing': t['casing'],
                                         'particle': t['casing'],
                                         'side': t['side'] + ('_connected' if kind == 'large_cogwheel' else '')}})
                    for axis, turn in turns.items():
                        variants[f'axis={axis},bottom_shaft={bottom},top_shaft={top}'] = dict({'model': model}, **turn)
            write_json(asset('blockstates', f'{name}.json'), {'variants': variants})


def encased_lang():
    kinds = {'shaft': 'Encased Shaft', 'cogwheel': 'Encased Cogwheel', 'large_cogwheel': 'Encased Large Cogwheel'}
    return {f'block.mcremix.{look}_encased_{kind}': f'{tinted_title(look)} {kinds[kind]}'
            for look in encasing_looks() for kind in ENCASED_KINDS}


# ------------------------------------------------------------------------------------------------ machine tiers

# the arm's textures, whose brass is their frame and dark wood their panel; in the others andesite is the frame and
# spruce the panel
BRASS_MACHINE_TEXTURES = ('brass_block', 'mechanical_arm', 'mechanical_arm_side')
# Create's crafter's, which no tier has any use for now the crafter comes in brass alone
CRAFTER_TEXTURES = ('crafter_thingies', 'crafter_side', 'crafter_side_connected')
# the parts of a sheet that are the machine's body, where that is not all of it: the millstone's turning stone is drawn
# by Create's renderer in Create's own texture, so its item keeps that stone as well, and only the rim and lid change
MACHINE_TEXTURE_REGIONS = {'millstone': ((0, 26, 16, 32), (20, 4, 32, 20))}
# the values of a machine's patina (Patina), and the weathering stage each looks like
PATINAS = {'none': 'unaffected', 'exposed': 'exposed', 'weathered': 'weathered', 'oxidized': 'oxidized',
           'waxed': 'unaffected', 'waxed_exposed': 'exposed', 'waxed_weathered': 'weathered',
           'waxed_oxidized': 'oxidized'}


def machine_stages():
    """Every look a machine tier has (stage_prefix and material): copper, iron and steel weather, titanium tints and
    aluminium does neither."""
    return [stage_prefix(m, s) + m for m in MACHINE_TIERS for s in (STAGES if m != 'aluminium' else STAGES[:1])]


def luma(c):
    return .299 * c[0] + .587 * c[1] + .114 * c[2]


def machine_shade(c, brass, stone=False):
    """What a pixel of Create's machine is: 'frame' (its andesite, or a brass machine's brass), 'panel' (the spruce,
    or dark wood, set in it), or None for what stays as it is: black, glass, iron and stone. With stone, its grey
    stone and iron are frame too."""
    r, g, b, a = c
    if a == 0:
        return None
    if brass:
        h, s, _ = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
        if r >= g >= b and s > .3 and luma(c) > 90 and .06 < h < .17:
            return 'frame'
        return 'panel' if r - b >= 18 and r >= g >= b and luma(c) <= 95 else None
    if r - b >= 22 and r >= g >= b and max(r, g, b) > 40:
        return 'panel'
    return 'frame' if (0 if stone else 2) <= max(r, g, b) - min(r, g, b) <= 16 and 45 <= luma(c) <= 200 else None


def casing_shades(stage):
    """The colours of a tier's casing (casing_texture), darkest first: its frame's, then its panel's."""
    img = casing_texture(stage, 600 + TEXTURE_STAGES.index(stage))
    frame, panel = [], []
    for y in range(16):
        for x in range(16):
            (frame if x < 2 or x > 13 or y < 2 or y > 13 else panel).append(img.getpixel((x, y))[:3])
    return sorted(frame, key=luma), sorted(panel, key=luma)


def machine_texture(name, stage):
    """Create's machine texture `name` in a tier's casing: its frame and panel take the colours of the casing's, so
    its colours are those of the casing beside it."""
    return recoloured(name, dict(zip(('frame', 'panel'), casing_shades(stage))), name in BRASS_MACHINE_TEXTURES,
                      MACHINE_TEXTURE_REGIONS.get(name))


def recoloured(name, shades, brass=False, regions=None, stone=False):
    """Create's texture `name` with each frame and panel pixel (machine_shade) taking the colour at the same place
    along `shades` of its kind, by its brightness among the texture's own, so the drawing and its contrast stay
    Create's. Only the pixels inside `regions`, where given, change."""
    rows = create_texture(name)
    kinds = {(x, y): machine_shade(px, brass, stone) for y, row in enumerate(rows) for x, px in enumerate(row)
             if regions is None or any(x0 <= x < x1 and y0 <= y < y1 for x0, y0, x1, y1 in regions)}
    span = {}
    for kind in shades:
        values = [luma(rows[y][x]) for (x, y), k in kinds.items() if k == kind]
        span[kind] = (min(values + [60]), max(values + [170]))
    img = Image.new('RGBA', (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, px in enumerate(row):
            kind = kinds.get((x, y))
            if kind:
                lo, hi = span[kind]
                colours = shades[kind]
                i = int((luma(px) - lo) / (hi - lo) * (len(colours) - 1) + .5)
                px = colours[max(0, min(len(colours) - 1, i))] + (px[3],)
            img.putpixel((x, y), px)
    return img


def machine_textures(look):
    """Where a tier's machine models find each of their textures (MACHINE_MODELS)."""
    own = {'casing': f'mcremix:block/{look}_casing', 'gearbox': f'mcremix:block/{look}_gearbox',
           'chain_drive': f'mcremix:block/{look}_chain_drive'}
    return lambda name: own.get(name, f'mcremix:block/machine/{look}/{name}')


def gen_machine_tiers():
    """Every machine tier is Create's machine in its casing's colours: its textures recoloured, Create's models with
    them, and Create's blockstates with a model for each weathering stage (waxed ones look as they did), or for each of
    titanium's tints, kept in the same place. The item shows a fresh one. Crafted as Create's, with the tier's casing in
    place of Create's."""
    for look in machine_stages():
        for name in CREATE_MACHINE_TEXTURES:
            if not name.startswith('_') and name not in MOVER_ONLY_TEXTURES + CRAFTER_TEXTURES:
                save_png(machine_texture(name, look), asset('textures', 'block', 'machine', look, name + '.png'))
        # titanium's encased blocks join up into one wall of casing in each tint, as Create's do (ModSpriteShifts)
        if look.endswith('titanium'):
            save_png(connected_casing_texture(look, 600 + TEXTURE_STAGES.index(look)),
                     asset('textures', 'block', f'{look}_casing_connected.png'))
        texture = machine_textures(look)
        for kind, models in MACHINE_MODELS.items():
            for model, textures in models.items():
                if model != 'item':
                    write_json(asset('models', 'block', f'{look}_{kind}', model + '.json'), {
                        'parent': f'create:block/{kind}/{model}',
                        'textures': {var: texture(name) for var, name in textures.items()}})
        # the drill head with each kind of bits, its collar in the tier's colours
        for material in ('stone', 'iron', 'gold', 'diamond'):
            bits = {} if material == 'iron' else {'2': BITS[material], 'particle': BITS[material]}
            write_json(asset('models', 'block', f'{look}_mechanical_drill', bits_name(material) + '.json'), {
                'parent': 'create:block/mechanical_drill/head',
                'textures': dict({'10': texture('andesite_casing_very_short')}, **bits)})
    for kind, models in MACHINE_MODELS.items():
        for tier in MACHINE_TIERS:
            texture = machine_textures(tier)
            write_json(asset('models', 'item', f'{tier}_{kind}.json'), {
                'parent': f'create:block/{kind}/item',
                'textures': {var: texture(name) for var, name in models['item'].items()}})
            variants = {}
            for key, (model, x, y) in machine_states(kind).items():
                turn = dict(({'x': x} if x else {}), **({'y': y} if y else {}))
                if tier == 'aluminium':
                    variants[key] = dict({'model': f'mcremix:block/{tier}_{kind}/{model}'}, **turn)
                    continue
                for patina, stage in PATINAS.items():
                    look = stage_prefix(tier, stage) + tier
                    variants[(key + ',' if key else '') + f'patina={patina}'] = dict(
                        {'model': f'mcremix:block/{look}_{kind}/{model}'}, **turn)
            write_json(asset('blockstates', f'{tier}_{kind}.json'), {'variants': variants})
            pattern, keys, count = MACHINE_RECIPES[kind]
            recipe(f'{tier}_{kind}', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
                                      'key': dict(keys, C=item(TIER_CASINGS[tier])),
                                      'result': item(f'mcremix:{tier}_{kind}', count)})


def machine_lang():
    return {f'block.mcremix.{tier}_{kind}': title_case(f'{tier}_{kind}') for kind in TIERED_MACHINES
            for tier in MACHINE_TIERS}


# ------------------------------------------------------------------------------------------------ wooden movers

# Create's movers in every wood, for drawbridges and gates (Taran, Part 2): its bearing, both pistons with their poles
# and head, gantry carriage and shaft, and rope pulley. Their blockstates are Create's, and their models Create's with
# each wood's textures in place of its own (tools/create_movers.json)
CREATE_MOVERS = json.load(open(os.path.join(ROOT, 'tools', 'create_movers.json')))
# models of theirs that Create's renderers draw, which no blockstate names: the bearing's top and the gantry's cogs
MOVER_PARTIALS = ('bearing/top', 'gantry_carriage/wheels')
# each wood's stripped log, stem or block of bamboo: the middle, darkest and lightest eighths of its texture
STRIPPED_LOGS = {
    'oak': ((181, 146, 87), (151, 121, 68), (193, 159, 100)),
    'spruce': ((117, 90, 54), (99, 83, 46), (132, 97, 56)),
    'birch': ((199, 177, 118), (180, 156, 101), (209, 193, 132)),
    'jungle': ((176, 130, 86), (151, 124, 69), (189, 143, 99)),
    'acacia': ((177, 92, 60), (154, 88, 55), (192, 98, 63)),
    'dark_oak': ((73, 57, 36), (60, 48, 32), (82, 63, 39)),
    'mangrove': ((122, 55, 49), (111, 45, 45), (123, 62, 48)),
    'cherry': ((217, 147, 152), (204, 132, 127), (220, 153, 159)),
    'bamboo': ((198, 178, 77), (145, 127, 59), (228, 206, 108)),
    'crimson': ((137, 57, 91), (123, 48, 78), (154, 64, 100)),
    'warped': ((59, 153, 151), (48, 136, 127), (65, 164, 162)),
}
# Create's textures that each wood's movers recolour: their andesite and stone become the wood's stripped log, which
# takes the andesite casing's place in their recipes, and their spruce its planks, while a powered gantry shaft lights
# up in the stripped log's colours. The rest is the wood's own (its shaft's axis, and its planks on a piston's head and
# poles) or stays Create's: the slime on a bearing's top, the end of a pole, the rope and the magnet
MOVER_TEXTURES = ('andesite_casing', 'gearbox', 'andesite_casing_piston', 'piston_bottom', 'piston_inner',
                  'mechanical_bearing_side', 'windmill_bearing_side', 'pinion', 'pinion_bottom', 'gantry_shaft',
                  'gantry_shaft_powered', 'gantry_shaft_flipped', 'gantry_shaft_powered_flipped', 'rope_pulley',
                  'cogwheel')
# the ones the machine tiers have no use for
MOVER_ONLY_TEXTURES = MOVER_TEXTURES[:2] + MOVER_TEXTURES[5:]
# the movers that turn, and so hold shafts as friction points: not a piston's poles and head
MOVER_FRICTION_POINTS = ('mechanical_bearing', 'mechanical_piston', 'sticky_mechanical_piston', 'gantry_carriage',
                         'gantry_shaft', 'rope_pulley')


def movers(*blocks):
    """Every wood's movers of these kinds (all of them when none are named), as block ids."""
    return [f'mcremix:{wood}_{block}' for block in CREATE_MOVERS['blockstates'] if not blocks or block in blocks
            for wood in WOODS]


def wood_shades(palette, deep):
    """32 colours of a wood, from deep in its shadow up through its dark, base and light."""
    shadow = mix(palette[1], (0, 0, 0), deep)
    return [mix(shadow, palette[1], t * 4) if t < .25 else ramp(palette, (t - .25) / .75)
            for t in (i / 31 for i in range(32))]


def mover_texture(name, wood):
    """Create's mover texture `name` in a wood (MOVER_TEXTURES)."""
    log, planks = wood_shades(STRIPPED_LOGS[wood], .35), wood_shades(PALETTES[wood], .45)
    lit = name.startswith('gantry_shaft_powered')
    return recoloured(name, {'frame': log, 'panel': log if lit else planks}, stone=True)


def mover_textures(wood):
    """What stands in for each of Create's textures in a wood's movers."""
    own = {f'create:block/{name}': f'mcremix:block/machine/{wood}/{name}' for name in MOVER_TEXTURES}
    own.update({'create:block/axis': f'mcremix:block/{wood}_axis',
                'create:block/axis_top': f'mcremix:block/{wood}_axis_top',
                'block/piston_side': f'minecraft:block/{wood}_planks',
                'block/piston_top': f'minecraft:block/{wood}_planks'})
    return lambda texture: own.get(texture, texture)


def create_model_textures(path):
    """The textures one of Create's mover models ends up with, its parents' included ('#' ones name another)."""
    model = CREATE_MOVERS['models'].get(path, {})
    parent = model.get('parent', '')
    inherited = create_model_textures(parent.removeprefix('create:block/')) if parent.startswith('create:') else {}
    return dict(inherited, **model.get('textures', {}))


def gen_wooden_movers():
    """Each wood's movers: Create's textures recoloured, Create's models and blockstates with them, and Create's
    recipes with that wood's parts. A piston's head drops the pole it is made of, as Create's does."""
    def own(model):
        return model.removeprefix('create:block/')
    paths = set(MOVER_PARTIALS) | {own(item['parent']) for item in CREATE_MOVERS['items'].values()}
    for variants in CREATE_MOVERS['blockstates'].values():
        paths |= {own(v['model']) for v in variants.values()}
    for wood in WOODS:
        for name in MOVER_TEXTURES:
            save_png(mover_texture(name, wood), asset('textures', 'block', 'machine', wood, name + '.png'))
        texture = mover_textures(wood)
        for path in sorted(paths):
            textures = {var: texture(t) for var, t in create_model_textures(path).items() if texture(t) != t}
            write_json(asset('models', 'block', f'{wood}_{path}.json'),
                       {'parent': f'create:block/{path}', 'textures': textures})
        for block, variants in CREATE_MOVERS['blockstates'].items():
            name = f'{wood}_{block}'
            write_json(asset('blockstates', name + '.json'), {'variants': {
                key: dict(v, model=f'mcremix:block/{wood}_{own(v["model"])}') for key, v in variants.items()}})
            loot = loot_self(name)
            if block == 'mechanical_piston_head':
                loot['pools'][0]['entries'][0]['name'] = f'mcremix:{wood}_piston_extension_pole'
            write_json(data('mcremix', 'loot_tables', 'blocks', name + '.json'), loot)
        for block, model in CREATE_MOVERS['items'].items():
            write_json(asset('models', 'item', f'{wood}_{block}.json'), dict(
                {'parent': f'mcremix:block/{wood}_{own(model["parent"])}'},
                **({'textures': {var: texture(t) for var, t in model['textures'].items()}} if 'textures' in model
                   else {})))
        mover_recipes(wood)


def mover_recipes(wood):
    """Create's recipes in a wood (Taran): its stripped log in place of the andesite casing, its slab for slabs and for
    the rope pulley's iron sheet, its shaft for the bearing's shaft and its gearbox for the gantry carriage's cog (wood
    has no cogs, as with its gearshift), and its planks around a stick (poles) or redstone (gantry shafts) where Create
    puts andesite alloy between planks. Sticky pistons are its pistons with a slimeball, as Create's are."""
    slab, planks = item(f'minecraft:{wood}_slab'), item(f'minecraft:{wood}_planks')

    def shaped(block, pattern, keys, count=1):
        recipe(f'{wood}_{block}', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
                                   'key': keys, 'result': item(f'mcremix:{wood}_{block}', count)})
    column = ['B', 'C', 'I']
    shaped('mechanical_bearing', column, {'B': slab, 'C': stripped_wood(wood), 'I': item(f'mcremix:{wood}_shaft')})
    shaped('mechanical_piston', column,
           {'B': slab, 'C': stripped_wood(wood), 'I': item(f'mcremix:{wood}_piston_extension_pole')})
    shaped('sticky_mechanical_piston', ['S', 'P'],
           {'S': {'tag': 'forge:slimeballs'}, 'P': item(f'mcremix:{wood}_mechanical_piston')})
    shaped('piston_extension_pole', ['P', 'S', 'P'], {'P': planks, 'S': {'tag': 'forge:rods/wooden'}}, 8)
    shaped('gantry_carriage', column, {'B': slab, 'C': stripped_wood(wood), 'I': item(f'mcremix:{wood}_gearbox')})
    shaped('gantry_shaft', ['P', 'R', 'P'], {'P': planks, 'R': {'tag': 'forge:dusts/redstone'}}, 8)
    shaped('rope_pulley', column, {'B': stripped_wood(wood), 'C': {'tag': 'minecraft:wool'}, 'I': slab})


def mover_lang():
    lang = {f'block.mcremix.{wood}_{block}': title_case(f'{wood}_{block}')
            for block in CREATE_MOVERS['blockstates'] for wood in WOODS}
    lang.update({f'block.mcremix.{wood}_sticky_mechanical_piston': 'Sticky ' + title_case(f'{wood}_mechanical_piston')
                 for wood in WOODS})
    return lang


# ------------------------------------------------------------------------------------------------ titanium



def sand_texture(seed, dark, base, light, glints):
    """A block of sand: fine grains, each a little lighter or darker than the next, and now and then another grain
    among them (glints: (colour, chance) pairs)."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            t = rnd.random()
            col = mix(dark, base, t * 2) if t < 0.5 else mix(base, light, t * 2 - 1)
            roll = rnd.random()
            for glint, chance in glints:
                if roll < chance:
                    col = glint
                    break
                roll -= chance
            img.putpixel((x, y), col + (255,))
    return img


def titanium_pickaxe_blocks():
    return ['mcremix:titanium_block', 'mcremix:titanium_pressurizer', 'mcremix:titanium_pressurizer_vessel'] \
        + titanium_plating_blocks()


def gen_titanium():
    """Stage 5: titanium's own items and blocks, and the black sand its ore comes out of."""
    for shape in ('ingot', 'nugget', 'sheet'):
        save_png(create_item_texture(shape, TITANIUM_SHADES), asset('textures', 'item', f'titanium_{shape}.png'))
    # rutile: red-black crystals, glinting; salt: a heap of clear grains; the sponge: grey and full of holes; burnt
    # titanium: the whitest powder there is
    save_png(lump_texture(1284, (0x36, 0x12, 0x0C), (0xB4, 0x52, 0x2C), accent=(0xF2, 0xB4, 0x74), accent_chance=0.12),
             asset('textures', 'item', 'rutile.png'))
    save_png(powder_texture(1285, (0x96, 0x9E, 0xA8), (0xDA, 0xE0, 0xE6), (0xFC, 0xFE, 0xFF), clumps=0.3),
             asset('textures', 'item', 'salt.png'))
    save_png(lump_texture(1286, (0x3A, 0x38, 0x38), (0xA2, 0x9E, 0x9A), accent=(0x1A, 0x18, 0x18), accent_chance=0.3),
             asset('textures', 'item', 'titanium_sponge.png'))
    save_png(powder_texture(1287, (0xD0, 0xD0, 0xD2), (0xF6, 0xF6, 0xF6), (0xFF, 0xFF, 0xFF)),
             asset('textures', 'item', 'titanium_dioxide.png'))
    for name in ('titanium_ingot', 'titanium_nugget', 'titanium_sheet', 'rutile', 'salt', 'titanium_sponge',
                 'titanium_dioxide'):
        write_json(asset('models', 'item', name + '.json'),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})

    save_png(metal_block_texture('titanium', 1288), asset('textures', 'block', 'titanium_block.png'))
    # black sand: dark iron ore with glints of magnetite and the odd red grain of rutile
    save_png(sand_texture(1289, (0x16, 0x15, 0x18), (0x2A, 0x29, 0x2E), (0x46, 0x44, 0x4A),
                          (((0x8A, 0x8C, 0x96), 0.05), ((0x6E, 0x30, 0x1E), 0.03))),
             asset('textures', 'block', 'black_sand.png'))
    for name in ('titanium_block', 'black_sand'):
        write_json(asset('models', 'block', name + '.json'),
                   {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'mcremix:block/{name}'}})
        write_json(asset('models', 'item', name + '.json'), {'parent': f'mcremix:block/{name}'})
        write_json(asset('blockstates', name + '.json'), {'variants': {'': {'model': f'mcremix:block/{name}'}}})
        write_json(data('mcremix', 'loot_tables', 'blocks', name + '.json'), loot_self(name))
    for kind in ('blocks', 'items'):
        write_json(data('forge', 'tags', kind, 'storage_blocks', 'titanium.json'), tag(['mcremix:titanium_block']))
    recipe('titanium_block', {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['III', 'III', 'III'],
        'key': {'I': {'tag': 'forge:ingots/titanium'}}, 'result': item('mcremix:titanium_block')})
    recipe('titanium_ingot_from_block', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [item('mcremix:titanium_block')],
        'result': item('mcremix:titanium_ingot', 9)})

    # basalt crushed or milled fine is black sand, and Power Grid's electromagnet pulls its iron out, leaving rutile
    for stone in ('basalt', 'polished_basalt', 'smooth_basalt'):
        recipe(f'crushing/black_sand_from_{stone}', {
            'type': 'create:crushing', 'ingredients': [item(f'minecraft:{stone}')], 'processingTime': 250,
            'results': [item('mcremix:black_sand'), {'item': 'mcremix:black_sand', 'chance': 0.25}]})
        recipe(f'milling/black_sand_from_{stone}', {
            'type': 'create:milling', 'ingredients': [item(f'minecraft:{stone}')], 'processingTime': 250,
            'results': [item('mcremix:black_sand')]})
    recipe('magnetization/black_sand', {
        'type': 'powergrid:magnetization', 'ingredients': [item('mcremix:black_sand')],
        'results': [item('mcremix:rutile'), item('minecraft:iron_nugget', 2),
                    {'item': 'minecraft:iron_nugget', 'count': 2, 'chance': 0.5}]})
    # burnt titanium is titanium dioxide, which is what rutile is, and the whitest pigment there is
    write_json(data('mcremix', 'tags', 'items', 'rutile.json'), tag(['mcremix:rutile', 'mcremix:titanium_dioxide']))
    recipe('white_dye_from_titanium_dioxide', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': 'white_dye',
        'ingredients': [item('mcremix:titanium_dioxide')], 'result': item('minecraft:white_dye', 2)})


def titanium_plating_texture(look, seed):
    """Titanium plating: four polished plates, brushed alternately across and down so that each catches the light its
    own way, with a domed rivet where the corners of four meet. A tinted one has the next temper colour creeping in from each
    plate's edges, where the Furnace's heat reached first."""
    palette = PALETTES[look]
    _, dark, light, speck = palette
    rnd = random.Random(seed)
    streaks = [[rnd.uniform(-0.08, 0.08) for _ in range(8)] for _ in range(4)]
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            px, py = x % 8, y % 8
            plate = (y // 8) * 2 + x // 8
            # polished, a sheen runs across each plate
            gleam = 0.28 * math.exp(-((px + py - 6) / 1.8) ** 2)
            col = ramp(palette, 0.55 + streaks[plate][py if plate in (0, 3) else px] + rnd.uniform(-0.04, 0.04) + gleam)
            edge = min(px, py, 7 - px, 7 - py)
            if speck and rnd.random() < (0.5, 0.22, 0.06, 0.0)[edge]:
                col = mix(col, speck, 0.55)
            if px in (0, 7) and py in (0, 7):
                # the rivet over the corners, lit on its upper left: the corner pixels of the four plates round it
                col = ramp(palette, {(7, 7): 0.95, (0, 0): 0.15}.get((px, py), 0.6))
            elif px == 0 or py == 0:
                col = mix(col, light, 0.4)
            elif px == 7 or py == 7:
                col = mix(col, dark, 0.6)
            img.putpixel((x, y), col + (255,))
    return img


def titanium_plating_blocks():
    return [f'mcremix:{prefix}titanium_plating' for prefix in TINT_PREFIXES.values()]


def gen_titanium_plating():
    """Titanium plating for building with, plain and in each temper colour. Each fires in a Furnace to any deeper
    colour: gold at Hot, purple at Searing, blue at Blazing; the Furnace does the deepest its heat reaches."""
    tints = list(TINT_PREFIXES.values())
    for n, prefix in enumerate(tints):
        name = f'{prefix}titanium_plating'
        save_png(titanium_plating_texture(prefix + 'titanium', 1311 + n), asset('textures', 'block', name + '.png'))
        write_json(asset('models', 'block', name + '.json'),
                   {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'mcremix:block/{name}'}})
        write_json(asset('models', 'item', name + '.json'), {'parent': f'mcremix:block/{name}'})
        write_json(asset('blockstates', name + '.json'), {'variants': {'': {'model': f'mcremix:block/{name}'}}})
        write_json(data('mcremix', 'loot_tables', 'blocks', name + '.json'), loot_self(name))
    recipe('titanium_plating', {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['SS', 'SS'],
        'key': {'S': {'tag': 'forge:plates/titanium'}}, 'result': item('mcremix:titanium_plating', 4)})
    heats = [None, 'hot', 'searing', 'blazing']
    for i, prefix in enumerate(tints):
        for j in range(i + 1, len(tints)):
            path = f'{tints[j]}titanium_plating' + (f'_from_{prefix[:-1]}' if prefix else '')
            firing(path, item(f'mcremix:{prefix}titanium_plating'), f'mcremix:{tints[j]}titanium_plating',
                   heats[j], 200)


def titanium_plating_lang():
    summary = ('Titanium sheets riveted over a block, for building with. Fired in a _Furnace_ it takes on titanium\'s '
               '_temper colours_ for good: _gold_ at _Hot_, _purple_ at _Searing_, _blue_ at _Blazing_, never going '
               'back to a lighter one. _Hydrochloric acid_ strips the colour off.')
    lang = {}
    for prefix in TINT_PREFIXES.values():
        key = f'block.mcremix.{prefix}titanium_plating'
        lang[key] = tinted_title(prefix + 'titanium') + ' Plating'
        lang[key + '.tooltip.summary'] = summary
    return lang


def titanium_lang():
    return {
        'item.mcremix.titanium_ingot': 'Titanium Ingot',
        'item.mcremix.titanium_ingot.tooltip.summary': (
            'Light, strong and slow to give up its heat. Its supports turn up to _2048 RPM_ with _no drag_, and its '
            'shafts and cogs take _any load_, but its supports need _twice the cooling_ aluminium\'s do, and _burn_ '
            'past _1000 °C_.'),
        'item.mcremix.titanium_nugget': 'Titanium Nugget',
        'item.mcremix.titanium_sheet': 'Titanium Sheet',
        'block.mcremix.titanium_casing': 'Titanium Casing',
        'block.mcremix.titanium_casing.tooltip.summary': (
            'Crafted from a _titanium sheet_, a stripped log and _polyethylene_, since every titanium support '
            'needs polyethylene. Titanium gearboxes, clutches, gearshifts and chain drives are built on it.'),
        'block.mcremix.titanium_block': 'Block of Titanium',
        'block.mcremix.black_sand': 'Black Sand',
        'block.mcremix.black_sand.tooltip.summary': (
            '_Basalt_ crushed fine: dark grains of iron ore with a little _rutile_ among them. Power Grid\'s '
            '_electromagnet_ pulls the iron out and leaves the rutile.'),
        'item.mcremix.rutile': 'Rutile',
        'item.mcremix.rutile.tooltip.summary': (
            "Titanium's ore, red-black crystals of _titanium dioxide_. Pressed with _coal coke_ and _coal tar_ into "
            '_briquettes_, which _chlorine_ run over in a _Steel Furnace_ at _Blazing_ heat turns into _titanium '
            'tetrachloride_.'),
        'item.mcremix.salt': 'Salt',
        'item.mcremix.salt.tooltip.summary': (
            'Boiled out of _salt water_ in a _heated basin_ with nothing working it. Melted in a _crucible_, an '
            '_Electrolyser_ splits it into _sodium_ and _chlorine_.'),
        'item.mcremix.titanium_sponge': 'Titanium Sponge',
        'item.mcremix.titanium_sponge.tooltip.summary': (
            'Titanium as sodium leaves it, porous and full of salt. An _Arc Furnace_ melts it down into _ingots_.'),
        'item.mcremix.titanium_dioxide': 'Titanium Dioxide',
        'item.mcremix.titanium_dioxide.tooltip.summary': (
            'The white powder burnt titanium crumbles into. It does for _rutile_ again, and makes _white dye_.'),
        'mcremix.goggles.temperature.burns': 'Temperature: %s °C (burns at %s °C)',
        'mcremix.goggles.tint.exposed': 'Tinted gold by heat',
        'mcremix.goggles.tint.weathered': 'Tinted purple by heat',
        'mcremix.goggles.tint.oxidized': 'Tinted blue by heat',
        'mcremix.tint.exposed': 'Gold-tinted',
        'mcremix.tint.weathered': 'Purple-tinted',
        'mcremix.tint.oxidized': 'Blue-tinted',
    }


# ------------------------------------------------------------------------------------------------ titanium's chain

# the refractory brick an Arc Furnace's hearth is lined with: dark, base, light, and the mortar between
REFRACTORY = ((0x6E, 0x5C, 0x4C), (0xA6, 0x92, 0x7A), (0xCC, 0xBC, 0xA4))
REFRACTORY_MORTAR = (0x58, 0x4C, 0x42)


def arc_furnace_shell_texture(seed, stage='unaffected'):
    """The outside of the Arc Furnace's hearth: thick copper, hammered into shallow dents, each lit on its upper left.
    Rows 9 to 15 are its side: the lit lip of the wall, a water pipe running round it to carry the heat away (lit
    over its shadow), the seams between the wall's panels, the floor plate's riveted edge and its dark foot. The copper
    at a weathering stage."""
    palette = PALETTES[copper_look(stage)]
    rnd, specks = random.Random(seed), random.Random(seed + 7919)
    dents = [(rnd.uniform(0, 16), rnd.uniform(0, 9), rnd.uniform(1.5, 3)) for _ in range(6)]
    side = {9: 0.86, 10: 0.52, 11: 0.82, 12: 0.26, 13: 0.5, 14: 0.44, 15: 0.18}
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            n = rnd.uniform(-0.04, 0.04)
            if y in side:
                t = side[y]
                if y in (10, 13, 14) and x in (4, 11):
                    t = 0.24  # a seam between the wall's panels, which the pipe runs on over
                elif y == 14 and x in (2, 7, 13):
                    t = 0.9  # a rivet in the floor plate, lit
            else:
                t = 0.5
                for dx0, dy0, r in dents:
                    dx, dy = (x - dx0 + 8) % 16 - 8, y - dy0
                    d = math.hypot(dx, dy)
                    if d < r:
                        t += 0.18 * (dx + dy) / (2 * r) * (1 - d / r)
            img.putpixel((x, y), speckle(ramp(palette, t + n), palette, specks) + (255,))
    return img


def arc_furnace_lining_texture(seed):
    """The inside of the hearth, its walls and floor: refractory brick in courses two pixels high, every other one set
    half a brick over, scorched black towards the middle where the arc burns."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    tones = [rnd.uniform(-0.12, 0.12) for _ in range(24)]
    for y in range(16):
        for x in range(16):
            course = y // 3
            if y % 3 == 2:
                col = REFRACTORY_MORTAR
            else:
                brick = (x + (3 if course % 2 else 0)) // 6
                if (x + (3 if course % 2 else 0)) % 6 == 5:
                    col = REFRACTORY_MORTAR
                else:
                    t = 0.5 + tones[(course * 4 + brick) % 24] + (0.14 if y % 3 == 0 else -0.06)
                    col = shade(REFRACTORY, t + rnd.uniform(-0.05, 0.05))
            # soot, heaviest in the middle of the hearth
            soot = max(0.0, 1 - math.hypot(x - 7.5, y - 7.5) / 7.5)
            col = mix(col, (0x1E, 0x1A, 0x18), min(0.85, soot * 1.1 + rnd.uniform(-0.08, 0.08)))
            img.putpixel((x, y), col + (255,))
    return img


def arc_furnace_rim_texture(seed, stage='unaffected'):
    """The top of the hearth's walls (the ring two pixels in from the edge): the copper shell on the outside, lit on
    the upper left, and the refractory brick lining inside it. The copper at a weathering stage."""
    palette = PALETTES[copper_look(stage)]
    rnd, specks = random.Random(seed), random.Random(seed + 7919)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            n = rnd.uniform(-0.04, 0.04)
            edge = min(x, y, 15 - x, 15 - y)
            if edge <= 1:
                col = speckle(ramp(palette, (0.86 if x <= 1 or y <= 1 else 0.44) + n), palette, specks)
            elif edge == 2:
                col = shade(REFRACTORY, (0.62 if x == 2 or y == 2 else 0.4) + n)
            else:
                col = speckle(ramp(palette, 0.5 + n), palette, specks)
            img.putpixel((x, y), col + (255,))
    return img


def arc_furnace_post_texture(seed, stage='unaffected'):
    """The Arc Furnace's steel posts: columns 0 and 1, rows 0 to 3, are a post's side, lit down its left; a copper clamp
    round the top, where the beam is bolted on, and a bolt head near its foot. The clamp's copper at a weathering
    stage."""
    steel, copper = PALETTES['steel'], PALETTES[copper_look(stage)]
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            n = rnd.uniform(-0.04, 0.04)
            lit = x % 2 == 0
            if y % 4 == 0:
                col = ramp(copper, (0.8 if lit else 0.45) + n)
            elif y % 4 == 2 and lit:
                col = ramp(steel, 0.95 + n)
            else:
                col = ramp(steel, (0.62 if lit else 0.32) + n)
            img.putpixel((x, y), col + (255,))
    return img


def arc_melt_texture(seed):
    """Titanium melting under the arc: white-hot in the middle, going yellow and orange where it meets the copper."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 8 + rnd.uniform(-0.08, 0.08)
            col = mix((0xFF, 0xFF, 0xF4), (0xFF, 0xD8, 0x6A), min(1.0, d * 1.4))
            if d > 0.7:
                col = mix(col, (0xF0, 0x80, 0x28), min(1.0, (d - 0.7) * 3))
            img.putpixel((x, y), col + (255,))
    return img


def arc_furnace_model():
    """Drawn facing north: a copper hearth lined with refractory brick, a Bakelite beam over it on two steel posts,
    and on the beam two terminals, copper studs in copper collars. ArcFurnaceBlock's shape and terminals are the same
    boxes."""
    sh, ln, rm, p, b, t = '#shell', '#lining', '#rim', '#post', '#bakelite', '#terminals'
    sides = ('north', 'south', 'east', 'west')
    hearth = lambda frm, to, **faces: box(frm, to, {d: {'texture': tex} for d, tex in faces.items()})
    elements = [
        hearth([1, 0, 1], [15, 2, 15], up=ln, down=sh, north=sh, south=sh, east=sh, west=sh),
        # the walls, each lined on the side facing in
        hearth([1, 2, 1], [15, 7, 3], north=sh, south=ln, east=sh, west=sh, up=rm),
        hearth([1, 2, 13], [15, 7, 15], north=ln, south=sh, east=sh, west=sh, up=rm),
        hearth([1, 2, 3], [3, 7, 13], east=ln, west=sh, up=rm),
        hearth([13, 2, 3], [15, 7, 13], east=sh, west=ln, up=rm),
        box([1, 7, 7], [3, 11, 9], {d: {'uv': [0, 0, 2, 4], 'texture': p} for d in sides}),
        box([13, 7, 7], [15, 11, 9], {d: {'uv': [0, 0, 2, 4], 'texture': p} for d in sides}),
        box([1, 11, 6], [15, 14, 10], {
            'up': {'uv': [1, 0, 15, 4], 'texture': b}, 'down': {'uv': [1, 0, 15, 4], 'texture': b},
            'north': {'uv': [1, 8, 15, 11], 'texture': b}, 'south': {'uv': [1, 8, 15, 11], 'texture': b},
            'east': {'uv': [4, 8, 8, 11], 'texture': b}, 'west': {'uv': [4, 8, 8, 11], 'texture': b}}),
    ]
    for x in (3, 9):
        elements.append(box([x, 14, 6], [x + 4, 15, 10], dict(
            {'up': {'uv': [0, 12, 4, 16], 'texture': t}},
            **{side: {'uv': [4, 12, 8, 13], 'texture': t} for side in sides})))
        elements.append(box([x + 1, 15, 7], [x + 3, 16, 9], dict(
            {'up': {'uv': [0, 0, 2, 2], 'texture': t}},
            **{side: {'uv': [0, 2, 2, 3], 'texture': t} for side in sides})))
    return {'parent': 'minecraft:block/block',
            'textures': {'shell': 'mcremix:block/arc_furnace_shell', 'lining': 'mcremix:block/arc_furnace_lining',
                         'rim': 'mcremix:block/arc_furnace_rim', 'post': 'mcremix:block/arc_furnace_post',
                         'bakelite': 'mcremix:block/electrolyser_bakelite',
                         'terminals': 'mcremix:block/electrolyser_terminals',
                         'particle': 'mcremix:block/arc_furnace_shell'},
            'elements': elements}


def arc_electrode_model(x):
    """A Carbon Electrode hanging from under one of the Arc Furnace's terminals down into its hearth."""
    r = '#rod'
    return {'textures': {'rod': 'mcremix:block/carbon_electrode', 'particle': 'mcremix:block/carbon_electrode'},
            'elements': [box([x, 3, 7], [x + 2, 11, 9], dict(
                {'down': {'uv': [x, 0, x + 2, 2], 'texture': r}},
                **{side: {'uv': [x, 8, x + 2, 16], 'texture': r} for side in ('north', 'south', 'east', 'west')}))]}


def arc_hearth_contents_model(name, texture, height):
    """What lies in the hearth: the white-hot melt under the arc, or the sponge still to melt."""
    tex = '#contents'
    return {'textures': {'contents': texture, 'particle': texture},
            'elements': [box([3, 2, 3], [13, 2 + height, 13],
                             {d: {'texture': tex} for d in ('up', 'north', 'south', 'east', 'west')})]}


def gen_titanium_chain():
    """Stage 5's chain: salt boiled from salt water, melted and split into sodium and chlorine; rutile pressed into
    briquettes with coke, which chlorine turns into titanium tetrachloride in a Steel Furnace; sodium stirred into it in
    a vat, leaving titanium sponge and salt; and the Arc Furnace, which melts the sponge into ingots."""
    save_png(lump_texture(1296, (0x14, 0x12, 0x12), (0x5A, 0x50, 0x4C), accent=(0xB4, 0x52, 0x2C), accent_chance=0.18),
             asset('textures', 'item', 'rutile_briquette.png'))
    write_json(asset('models', 'item', 'rutile_briquette.json'),
               {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mcremix:item/rutile_briquette'}})

    # its copper weathers by its patina, for looks only (Taran, 0.23.0): a model for each stage, which waxed ones keep
    for stage in COPPER_STAGES:
        sfx = look_suffix(stage)
        save_png(arc_furnace_shell_texture(1297, stage), asset('textures', 'block', f'arc_furnace_shell{sfx}.png'))
        save_png(arc_furnace_rim_texture(1360, stage), asset('textures', 'block', f'arc_furnace_rim{sfx}.png'))
        save_png(arc_furnace_post_texture(1361, stage), asset('textures', 'block', f'arc_furnace_post{sfx}.png'))
        if sfx:
            base, dark, light = PALETTES[copper_look(stage)][:3]
            save_png(terminals_texture(1292, (dark, base, light)),
                     asset('textures', 'block', f'arc_furnace_terminals{sfx}.png'))
            write_json(asset('models', 'block', f'arc_furnace{sfx}.json'), {
                'parent': 'mcremix:block/arc_furnace',
                'textures': {'shell': f'mcremix:block/arc_furnace_shell{sfx}', 'rim': f'mcremix:block/arc_furnace_rim{sfx}',
                             'post': f'mcremix:block/arc_furnace_post{sfx}',
                             'terminals': f'mcremix:block/arc_furnace_terminals{sfx}',
                             'particle': f'mcremix:block/arc_furnace_shell{sfx}'}})
    save_png(arc_furnace_lining_texture(1299), asset('textures', 'block', 'arc_furnace_lining.png'))
    save_png(arc_melt_texture(1298), asset('textures', 'block', 'arc_furnace_melt.png'))
    write_json(asset('models', 'block', 'arc_furnace.json'), arc_furnace_model())
    for side, x in (('left', 4), ('right', 10)):
        write_json(asset('models', 'block', f'arc_furnace_{side}.json'), arc_electrode_model(x))
    write_json(asset('models', 'block', 'arc_furnace_melt.json'),
               arc_hearth_contents_model('melt', 'mcremix:block/arc_furnace_melt', 1))
    write_json(asset('models', 'block', 'arc_furnace_charge.json'),
               arc_hearth_contents_model('charge', 'mcremix:item/titanium_sponge', 2))
    write_json(asset('models', 'item', 'arc_furnace.json'), {'parent': 'mcremix:block/arc_furnace'})
    parts = []
    for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        rot = {'y': y} if y else {}
        for stage in COPPER_STAGES:
            parts.append({'when': {'facing': facing, 'patina': patina_keys(stage)},
                          'apply': dict({'model': f'mcremix:block/arc_furnace{look_suffix(stage)}'}, **rot)})
        for side in ('left', 'right'):
            parts.append({'when': {'facing': facing, side: 'true'},
                          'apply': dict({'model': f'mcremix:block/arc_furnace_{side}'}, **rot)})
        parts.append({'when': {'facing': facing, 'lit': 'true'},
                      'apply': dict({'model': 'mcremix:block/arc_furnace_melt'}, **rot)})
        parts.append({'when': {'facing': facing, 'loaded': 'true', 'lit': 'false'},
                      'apply': dict({'model': 'mcremix:block/arc_furnace_charge'}, **rot)})
    write_json(asset('blockstates', 'arc_furnace.json'), {'multipart': parts})
    write_json(data('mcremix', 'loot_tables', 'blocks', 'arc_furnace.json'), loot_self('arc_furnace'))
    # copper terminals on a Bakelite beam, on steel posts over a copper hearth
    recipe('arc_furnace', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['CBC', 'S S', 'PKP'],
        'key': {'C': {'tag': 'forge:ingots/copper'}, 'B': item('mcremix:bakelite'),
                'S': {'tag': 'forge:plates/steel'}, 'P': {'tag': 'forge:plates/copper'},
                'K': {'tag': 'forge:storage_blocks/copper'}},
        'result': item('mcremix:arc_furnace')})

    # salt water boiled down in a heated basin nothing works leaves its salt
    recipe('boiling/salt', {
        'type': 'mcremix:boiling', 'heatRequirement': 'heated', 'processingTime': 200,
        'ingredients': [{'fluid': 'mcremix:salt_water', 'amount': 500}], 'results': [item('mcremix:salt')]})
    # the crucible melts salt, Hot
    recipe('melting/salt', {'type': 'mcremix:melting', 'ingredient': item('mcremix:salt'),
                            'result': {'fluid': 'mcremix:molten_salt', 'amount': 100}})
    # molten salt split by a current: sodium at the cathode, chlorine at the anode
    recipe('electrolysis/molten_salt', {
        'type': 'mcremix:electrolysis', 'charge': 100,
        'ingredients': [{'fluid': 'mcremix:molten_salt', 'amount': 100}],
        'results': [{'fluid': 'mcremix:molten_sodium', 'amount': 50}, {'fluid': 'mcremix:chlorine', 'amount': 50}]})
    # rutile and coke bound with tar by a press, as the Carbon Electrode's coke is
    recipe('compacting/rutile_briquette', {
        'type': 'create:compacting',
        'ingredients': [{'tag': 'mcremix:rutile'}, item('mcremix:coal_coke'), {'fluid': 'mcremix:coal_tar', 'amount': 50}],
        'results': [item('mcremix:rutile_briquette')]})
    # chlorine over a briquette at Blazing: the coke takes the rutile's oxygen and the chlorine its titanium; one
    # briquette for each batch
    recipe('steel_furnace/titanium_tetrachloride', {
        'type': 'mcremix:steel_furnace', 'heat': 'blazing', 'processingTime': 100, 'catalyst_lasts': 200,
        'ingredients': [item('mcremix:rutile_briquette'), {'fluid': 'mcremix:chlorine', 'amount': 200}],
        'results': [{'fluid': 'mcremix:titanium_tetrachloride', 'amount': 100}]})
    # the Kroll process: sodium takes the chlorine back as salt, leaving titanium as a sponge; the salt goes round again
    recipe('vat_mixing/titanium_sponge', {
        'type': 'mcremix:vat_mixing',
        'ingredients': [{'fluid': 'mcremix:titanium_tetrachloride', 'amount': 100},
                        {'fluid': 'mcremix:molten_sodium', 'amount': 200}],
        'results': [item('mcremix:titanium_sponge'), item('mcremix:salt', 4)]})
    # the sponge melted down in the water-cooled hearth, and frozen into an ingot
    recipe('arc_furnace/titanium_ingot', {
        'type': 'mcremix:arc_furnace', 'energy': 16000,
        'ingredients': [item('mcremix:titanium_sponge')], 'results': [item('mcremix:titanium_ingot')]})


def gen_titanium_ponder():
    """Titanium's heat: a creative motor turning a titanium gearbox through titanium shafts, oak planks two blocks in
    front of the gearbox (north, toward the camera) for its burning to light, a water cooler on the gearbox, and a row
    of titanium plating in each colour along the back. The Arc Furnace: one facing north on a hopper that feeds a chest
    to its west, a water cooler bolted to its east side and a creative voltage source in the south-west corner."""
    b = {}
    plate(b)
    b[(0, 1, 2)] = ('create:creative_motor', {'facing': 'east'})
    for x in (1, 3, 4):
        b[(x, 1, 2)] = ('mcremix:titanium_shaft', {'axis': 'x'})
    b[(2, 1, 2)] = ('mcremix:titanium_gearbox', {'axis': 'y'})
    b[(2, 2, 2)] = ('mcremix:water_cooler', {'facing': 'down'})
    b[(2, 1, 0)] = ('minecraft:oak_planks', {})
    for x, prefix in enumerate(TINT_PREFIXES.values(), 1):
        b[(x, 1, 4)] = (f'mcremix:{prefix}titanium_plating', {})
    save_structure('titanium/heat', (5, 3, 5), b)

    b = {}
    plate(b)
    b[(2, 2, 2)] = ('mcremix:arc_furnace', {'facing': 'north', 'left': 'false', 'right': 'false', 'loaded': 'false',
                                            'lit': 'false'})
    b[(2, 1, 2)] = ('minecraft:hopper', {'enabled': 'true', 'facing': 'west'})
    b[(1, 1, 2)] = ('minecraft:chest', {'facing': 'north', 'type': 'single', 'waterlogged': 'false'})
    b[(3, 2, 2)] = ('mcremix:water_cooler', {'facing': 'west'})
    b[(0, 1, 4)] = ('powergrid:creative_voltage_source', {'facing': 'east'})
    save_structure('arc_furnace/intro', (5, 4, 5), b)


def titanium_chain_lang():
    return {
        'item.mcremix.rutile_briquette': 'Rutile Briquette',
        'item.mcremix.rutile_briquette.tooltip.summary': (
            '_Rutile_ and _coal coke_ bound with _coal tar_ by a press. _Chlorine_ run over it in a _Steel Furnace_ at '
            '_Blazing_ heat comes off as _titanium tetrachloride_, the coke taking the rutile\'s oxygen; one wears away '
            'for each batch.'),
        'block.mcremix.arc_furnace': 'Arc Furnace',
        'block.mcremix.arc_furnace.tooltip.summary': (
            'A _Power Grid_ device. It strikes an _arc_ from one _Carbon Electrode_ down through what lies in its copper '
            'hearth and back up the other, melting _titanium sponge_ into _ingots_. Molten titanium dissolves any '
            'other crucible, so the hearth is copper, kept cool by _water_.'),
        'block.mcremix.arc_furnace.tooltip.condition1': 'When powered',
        'block.mcremix.arc_furnace.tooltip.behaviour1': (
            'The arc is a heavy load of _a quarter ohm_, with no + or -: _20 V_ drive _80 A_, _1.6 kW_. It melts only '
            'from _400 W_, each sponge taking _16 kJ_, and its hearth sheds only _150 W_ by itself, so it _overheats_ '
            'unless a _water cooler_ takes the heat: one on water holds it to about _2.5 kW_.'),
        'block.mcremix.arc_furnace.tooltip.condition2': 'When used',
        'block.mcremix.arc_furnace.tooltip.behaviour2': (
            'A _Carbon Electrode_ goes into an empty holder and _sponge_ into the hearth; an empty hand takes out the '
            '_ingots_, then the sponge, then an electrode. Funnels and arms put them in; hoppers take the ingots out. '
            'Both electrodes burn away as it works.'),
        'mcremix.goggles.arc_furnace.electrode': 'Electrode: %s%% left',
        'mcremix.goggles.arc_furnace.no_electrode': 'No electrode',
        'mcremix.goggles.arc_furnace.reading': '%s V, %s A, %s W',
        'mcremix.goggles.arc_furnace.temperature': 'Hearth: %s °C',
        'mcremix.goggles.arc_furnace.melting': 'Melting %s × %s: %s%%',
        'mcremix.goggles.arc_furnace.made': 'Made: %s × %s',
        'mcremix.goggles.arc_furnace.no_electrodes': 'Needs two Carbon Electrodes',
        'mcremix.goggles.arc_furnace.nothing_to_melt': 'Nothing in the hearth to melt',
        'mcremix.goggles.arc_furnace.full': 'Full: take out what it has made',
        'mcremix.goggles.arc_furnace.no_current': 'No current',
        'mcremix.goggles.arc_furnace.too_weak': 'Too weak an arc to melt anything: it takes %s W',
        'mcremix.jei.arc_furnace': 'Arc Furnace',
        'mcremix.jei.arc_furnace.energy': '%s kJ: %ss at %s kW',
        'mcremix.jei.arc_furnace.energy.note': 'Each takes that much energy, so the more power through the arc, the faster it melts',
        'mcremix.jei.arc_furnace.weak': 'Under %s W the arc only warms the hearth',
        'mcremix.jei.boiling': 'Boiling',
        'mcremix.jei.boiling.basin': 'In a basin over a fed Blaze Burner or a Furnace burning Searing or hotter, with no mixer or press over it',
    }


# ------------------------------------------------------------------------------------------------ structural formulas

# Taran's list (0.25.0): compounds show their structural formula on their tooltip, every atom coloured by its element:
# this mod's chemicals and their buckets, and Create's and Minecraft's compounds. Ionic solids show their ions, polymers
# their repeat unit. Each drawing is laid out here in bond lengths, x east and y down, and written to
# formulas/<name>.json; the client draws it (FormulaTooltipRenderer) and a GameTest checks its atoms and charges against
# the formula written under it.

class Formula:
    """A drawing: atoms placed in bond lengths, the bonds between them, brackets round groups, and loose signs."""

    def __init__(self, formula, items=(), fluids=()):
        self.formula = formula
        self.items = list(items)
        self.fluids = list(fluids)
        self.atoms = []
        self.bonds = []
        self.brackets = []
        self.signs = []

    def atom(self, label, x, y, charge=None, h=0, hide=False, end=False, count=None, share=None):
        """
        An atom or group written as `label` at (x, y), centred on its first element (its last if `end`). A skeletal
        carbon is hidden and its `h` hydrogens go unwritten; `count` is written before it, and `share` is the part of it
        the drawing holds, half for an oxygen it shares with the next silicon on.
        """
        a = {'l': label, 'x': round(x, 3), 'y': round(y, 3)}
        if charge:
            a['q'] = charge
        if h:
            a['h'] = h
        if hide:
            a['hide'] = True
        if end:
            a['a'] = 'end'
        if count:
            a['n'] = count
        if share is not None:
            a['w'] = share
        self.atoms.append(a)
        return len(self.atoms) - 1

    def point(self, x, y):
        """Where a bond runs on out of the drawing, to the next unit of a chain or a network."""
        return self.atom('', x, y)

    def pos(self, i):
        return self.atoms[i]['x'], self.atoms[i]['y']

    def off(self, i, angle, r=1.0):
        """The point r bond lengths from atom i, at an angle in degrees counter-clockwise from east."""
        x, y = self.pos(i)
        return x + r * math.cos(math.radians(angle)), y - r * math.sin(math.radians(angle))

    def bond(self, a, b, order=1, toward=None):
        """
        A bond: order 0 is a faint line between neighbouring ions. A double bond is two lines either side of the
        bond's line, or the bond's line and a shorter one on the side toward a ring's centre.
        """
        bond = [a, b, order]
        if toward is not None:
            (ax, ay), (bx, by) = self.pos(a), self.pos(b)
            nx, ny = -(by - ay), bx - ax
            mx, my = (ax + bx) / 2, (ay + by) / 2
            bond.append(1 if (toward[0] - mx) * nx + (toward[1] - my) * ny > 0 else -1)
        self.bonds.append(bond)

    def branch(self, i, label, angle, r=1.0, order=1, **kw):
        """An atom placed at an angle from atom i and bonded to it."""
        x, y = self.off(i, angle, r)
        j = self.atom(label, x, y, **kw)
        self.bond(i, j, order)
        return j

    def stub(self, i, angle, r=0.8):
        """A bond from atom i running on out of the drawing."""
        x, y = self.off(i, angle, r)
        self.bond(i, self.point(x, y))

    def bracket(self, atoms=None, sub=None, sup=None, count=None, x0=None, x1=None):
        """
        Brackets round some atoms (all of them by default), as high as they reach: a polymer's crossing its bonds out
        at x0 and x1 with an n under, an ion's with its charge over, and a count before for several of them.
        """
        if atoms is None:
            atoms = [i for i, a in enumerate(self.atoms) if a['l']]
        b = {'atoms': atoms}
        if sub:
            b['sub'] = sub
        if sup:
            b['sup'] = sup
        if count:
            b['n'] = count
        if x0 is not None:
            b['x0'] = round(x0, 3)
        if x1 is not None:
            b['x1'] = round(x1, 3)
        self.brackets.append(b)

    def sign(self, text, x, y):
        self.signs.append({'s': text, 'x': round(x, 3), 'y': round(y, 3)})

    def json(self):
        out = {'formula': self.formula}
        if self.items:
            out['items'] = self.items
        if self.fluids:
            out['fluids'] = self.fluids
        out['atoms'] = self.atoms
        for key in ('bonds', 'brackets', 'signs'):
            if getattr(self, key):
                out[key] = getattr(self, key)
        return out


def water_molecule(f, x, y):
    """Water with its oxygen at (x, y) and its hydrogens below, 104.5 degrees apart."""
    o = f.atom('O', x, y)
    for angle in (270 - 52.25, 270 + 52.25):
        f.branch(o, 'H', angle)


def glycol_molecule(f, x, y):
    """Ethylene glycol written out in full, its carbons from (x, y) east."""
    c1 = f.atom('C', x, y)
    c2 = f.branch(c1, 'C', 0, 1.2)
    for c, out in ((c1, 180), (c2, 0)):
        for angle in (90, 270):
            f.branch(c, 'H', angle)
        o = f.branch(c, 'O', out, 1.1)
        f.branch(o, 'H', out)


def create_palette(stone):
    """The blocks Create cuts from a stone: its pillar, layered, cut and polished cut, bricks and small bricks."""
    ids = [stone + '_pillar', 'layered_' + stone]
    for base in ('cut_' + stone, 'polished_cut_' + stone):
        ids += [base, base + '_slab', base + '_stairs', base + '_wall']
    for bricks in ('cut_' + stone + '_brick', 'small_' + stone + '_brick'):
        ids += [bricks + 's', bricks + '_slab', bricks + '_stairs', bricks + '_wall']
    return ids


def gen_formulas():
    formulas = {}

    def formula(name, text, items=(), fluids=()):
        formulas[name] = Formula(text, items, fluids)
        return formulas[name]

    def mc(*ids):
        return ['minecraft:' + i for i in ids]

    def cr(*ids):
        return ['create:' + i for i in ids]

    def mr(*ids):
        return ['mcremix:' + i for i in ids]

    # water, as liquid, ice and snow, and the mod's steam, warm water and cold water
    f = formula('water', 'H2O', mc('ice', 'packed_ice', 'blue_ice', 'snow', 'snow_block', 'snowball',
                                   'powder_snow_bucket'),
                mc('water') + mr('pressurized_steam', 'warm_water', 'cold_water'))
    water_molecule(f, 0, 0)

    f = formula('hydrogen', 'H2', fluids=mr('hydrogen'))
    f.branch(f.atom('H', 0, 0), 'H', 0)

    f = formula('chlorine', 'Cl2', fluids=mr('chlorine'))
    f.branch(f.atom('Cl', 0, 0), 'Cl', 0, 1.5)

    f = formula('hydrogen_chloride', 'HCl', fluids=mr('hydrogen_chloride'))
    f.branch(f.atom('H', 0, 0), 'Cl', 0, 1.3)

    # in water, hydrogen chloride hands its hydrogen over: hydronium and chloride ions
    f = formula('hydrochloric_acid', 'HCl in H2O', fluids=mr('hydrochloric_acid'))
    o = f.atom('O', 0, 0, charge='+')
    for angle in (90, 210, 330):
        f.branch(o, 'H', angle)
    f.atom('Cl', 2.4, 0, charge='-')

    f = formula('ethylene', 'C2H4', fluids=mr('ethylene'))
    c1 = f.atom('C', 0, 0)
    c2 = f.branch(c1, 'C', 0, 1.2, order=2)
    for angle in (150, 210):
        f.branch(c1, 'H', angle)
    for angle in (30, 330):
        f.branch(c2, 'H', angle)

    f = formula('vinyl_chloride', 'C2H3Cl', fluids=mr('vinyl_chloride'))
    c1 = f.atom('C', 0, 0)
    c2 = f.branch(c1, 'C', 0, 1.2, order=2)
    for angle in (150, 210):
        f.branch(c1, 'H', angle)
    f.branch(c2, 'H', 30)
    f.branch(c2, 'Cl', 330, 1.3)

    f = formula('ethylene_dichloride', 'C2H4Cl2', fluids=mr('ethylene_dichloride'))
    c1 = f.atom('C', 0, 0)
    c2 = f.branch(c1, 'C', 0, 1.2)
    f.branch(c1, 'Cl', 180, 1.3)
    f.branch(c2, 'Cl', 0, 1.3)
    for c in (c1, c2):
        for angle in (90, 270):
            f.branch(c, 'H', angle)

    # wood spirit is methanol
    f = formula('methanol', 'CH3OH', fluids=mr('wood_spirit'))
    c = f.atom('C', 0, 0)
    for angle in (90, 180, 270):
        f.branch(c, 'H', angle)
    f.branch(f.branch(c, 'O', 0, 1.1), 'H', 0)

    f = formula('ethanol', 'C2H5OH', fluids=mr('ethanol'))
    c1 = f.atom('C', 0, 0)
    for angle in (90, 180, 270):
        f.branch(c1, 'H', angle)
    c2 = f.branch(c1, 'C', 0, 1.2)
    for angle in (90, 270):
        f.branch(c2, 'H', angle)
    f.branch(f.branch(c2, 'O', 0, 1.1), 'H', 0)

    f = formula('ethylene_glycol', 'C2H6O2', fluids=mr('ethylene_glycol'))
    glycol_molecule(f, 0, 0)

    f = formula('coolant', 'C2H6O2 + H2O', fluids=mr('coolant', 'hot_coolant'))
    glycol_molecule(f, 0, 0)
    f.sign('+', 4.1, 0)
    water_molecule(f, 5.6, -0.3)

    f = formula('salt_water', 'NaCl in H2O', fluids=mr('salt_water'))
    f.atom('Na', 0, 0, charge='+')
    f.atom('Cl', 1.6, 0, charge='-')
    f.sign('+', 2.7, 0)
    water_molecule(f, 4.2, -0.3)

    f = formula('molten_salt', 'NaCl', fluids=mr('molten_salt'))
    f.atom('Na', 0, 0, charge='+')
    f.atom('Cl', 1.6, 0, charge='-')

    # salt as a crystal: a corner of its lattice
    f = formula('salt', 'NaCl', mr('salt'))
    grid = {}
    for row in range(2):
        for col in range(3):
            na = (row + col) % 2 == 0
            grid[row, col] = f.atom('Na' if na else 'Cl', col * 1.7, row * 1.5, charge='+' if na else '-')
    for (row, col), i in grid.items():
        for nxt in ((row, col + 1), (row + 1, col)):
            if nxt in grid:
                f.bond(i, grid[nxt], 0)

    # caustic soda is sodium hydroxide
    f = formula('sodium_hydroxide', 'NaOH', fluids=mr('caustic_soda'))
    f.atom('Na', 0, 0, charge='+')
    f.atom('OH', 1.6, 0, charge='-')

    # bauxite liquor: aluminium taken into caustic soda as aluminate ions
    f = formula('sodium_aluminate', 'NaAl(OH)4', fluids=mr('bauxite_liquor'))
    f.atom('Na', 0, 0, charge='+')
    al = f.atom('Al', 3.3, 0)
    group = [al]
    for angle, label in ((90, 'OH'), (0, 'OH'), (270, 'OH'), (180, 'HO')):
        group.append(f.branch(al, label, angle, 1.1, end=label == 'HO'))
    f.bracket(group, sup='-')

    f = formula('ferric_chloride', 'FeCl3', fluids=mr('ferric_chloride'))
    fe = f.atom('Fe', 0, 0, charge='3+')
    for angle in (90, 210, 330):
        f.atom('Cl', *f.off(fe, angle, 1.7), charge='-')

    f = formula('titanium_tetrachloride', 'TiCl4', fluids=mr('titanium_tetrachloride'))
    ti = f.atom('Ti', 0, 0)
    for angle in (0, 90, 180, 270):
        f.branch(ti, 'Cl', angle, 1.3)

    f = formula('titanium_dioxide', 'TiO2', mr('titanium_dioxide', 'rutile'))
    ti = f.atom('Ti', 0, 0, charge='4+')
    for x in (-1.7, 1.8):
        f.bond(ti, f.atom('O', x, 0, charge='2-'), 0)

    f = formula('alumina', 'Al2O3', mr('alumina'))
    al = [f.atom('Al', 0.85 + 1.7 * i, 0, charge='3+') for i in range(2)]
    o = [f.atom('O', 1.7 * i, 1.4, charge='2-') for i in range(3)]
    for i, a in enumerate(al):
        f.bond(a, o[i], 0)
        f.bond(a, o[i + 1], 0)

    f = formula('aluminium_hydroxide', 'Al(OH)3', mr('aluminium_hydroxide'))
    al = f.atom('Al', 0, 0, charge='3+')
    for angle in (90, 210, 330):
        f.atom('OH', *f.off(al, angle, 1.8), charge='-')

    # calcite, dripstone and limestone are calcium carbonate
    f = formula('calcium_carbonate', 'CaCO3',
                mc('calcite', 'dripstone_block', 'pointed_dripstone')
                + cr('limestone', *create_palette('limestone'), *create_palette('calcite')))
    f.atom('Ca', 0, 0, charge='2+')
    c = f.atom('C', 2.8, 0.2)
    f.branch(c, 'O', 90, order=2)
    for angle in (210, 330):
        f.branch(c, 'O', angle, charge='-')

    # quartz, amethyst, flint and rose quartz are silica: each silicon holds four oxygens, each shared with the next
    f = formula('silica', 'SiO2',
                mc('quartz', 'quartz_block', 'chiseled_quartz_block', 'quartz_pillar', 'quartz_bricks', 'smooth_quartz',
                   'quartz_slab', 'quartz_stairs', 'smooth_quartz_slab', 'smooth_quartz_stairs', 'amethyst_shard',
                   'amethyst_block', 'flint')
                + cr('rose_quartz', 'polished_rose_quartz', 'rose_quartz_block', 'rose_quartz_tiles',
                     'small_rose_quartz_tiles'))
    si = f.atom('Si', 0, 0)
    for angle in (0, 90, 180, 270):
        f.stub(f.branch(si, 'O', angle, share=0.5), angle, 0.7)

    # clay is kaolinite: a sheet of silicate, aluminium ions and hydroxide
    f = formula('kaolinite', 'Al2Si2O5(OH)4', mc('clay_ball', 'clay'))
    si = [f.atom('Si', 0, 0), f.atom('Si', 2.2, 0)]
    bridge = f.atom('O', 1.1, 0)
    for s, out in zip(si, (180, 0)):
        f.bond(s, bridge)
        f.branch(s, 'O', 90, charge='-')
        for angle in (out, 270):
            f.stub(f.branch(s, 'O', angle, share=0.5), angle, 0.7)
    f.atom('Al', 5.4, -0.55, charge='3+', count='2')
    f.atom('OH', 5.4, 0.75, charge='-', count='4')

    # bone meal is hydroxyapatite: calcium, phosphate and hydroxide ions
    f = formula('hydroxyapatite', 'Ca5(PO4)3OH', mc('bone_meal'))
    f.atom('Ca', 0, 0, charge='2+', count='5')
    p = f.atom('P', 3.3, 0)
    group = [p, f.branch(p, 'O', 90, order=2)]
    for angle in (0, 180, 270):
        group.append(f.branch(p, 'O', angle, charge='-'))
    f.bracket(group, count='3')
    f.atom('OH', 6.0, 0, charge='-')

    # sugar is sucrose: glucose's six-membered ring joined through an oxygen to fructose's five-membered one
    f = formula('sucrose', 'C12H22O11', mc('sugar'))
    glucose = {}
    for name, angle in (('O', 30), ('C1', 330), ('C2', 270), ('C3', 210), ('C4', 150), ('C5', 90)):
        x, y = math.cos(math.radians(angle)), -math.sin(math.radians(angle))
        glucose[name] = f.atom('O', x, y) if name == 'O' else f.atom('C', x, y, h=1, hide=True)
    ring = [glucose[n] for n in ('O', 'C1', 'C2', 'C3', 'C4', 'C5')]
    for a, b in zip(ring, ring[1:] + ring[:1]):
        f.bond(a, b)
    f.branch(glucose['C2'], 'OH', 270)
    f.branch(glucose['C3'], 'HO', 210, end=True)
    f.branch(glucose['C4'], 'HO', 150, end=True)
    f.branch(glucose['C5'], 'CH2OH', 90)
    c2 = f.branch(f.branch(glucose['C1'], 'O', 0), 'C', 0, hide=True)
    r = 0.5 / math.sin(math.radians(36))
    cx, cy = f.pos(c2)[0] + r, f.pos(c2)[1]
    fructose = {}
    for name, angle in (('O', 108), ('C5', 36), ('C4', -36), ('C3', -108)):
        x, y = cx + r * math.cos(math.radians(angle)), cy - r * math.sin(math.radians(angle))
        fructose[name] = f.atom('O', x, y) if name == 'O' else f.atom('C', x, y, h=1, hide=True)
    ring = [c2] + [fructose[n] for n in ('O', 'C5', 'C4', 'C3')]
    for a, b in zip(ring, ring[1:] + ring[:1]):
        f.bond(a, b)
    f.branch(c2, 'HOH2C', 245, end=True)
    f.branch(fructose['C3'], 'OH', 270)
    f.branch(fructose['C4'], 'OH', -30)
    f.branch(fructose['C5'], 'CH2OH', 30)

    f = formula('polyethylene', '(C2H4)n', mr('polyethylene_pellets'))
    c1 = f.atom('C', 0, 0)
    c2 = f.branch(c1, 'C', 0, 1.2)
    for c in (c1, c2):
        for angle in (90, 270):
            f.branch(c, 'H', angle)
    f.stub(c1, 180, 1.0)
    f.stub(c2, 0, 1.0)
    f.bracket(sub='n', x0=-0.6, x1=1.8)

    f = formula('pvc', '(C2H3Cl)n', mr('pvc_resin', 'pvc_sheet'))
    c1 = f.atom('C', 0, 0)
    c2 = f.branch(c1, 'C', 0, 1.2)
    for angle in (90, 270):
        f.branch(c1, 'H', angle)
    f.branch(c2, 'H', 90)
    f.branch(c2, 'Cl', 270, 1.1)
    f.stub(c1, 180, 1.0)
    f.stub(c2, 0, 1.0)
    f.bracket(sub='n', x0=-0.6, x1=1.8)

    # Bakelite: phenol rings joined by methylene bridges, a ring and a bridge to each unit
    f = formula('bakelite', '(C7H6O)n', mr('bakelite'))
    ring = [f.atom('C', math.cos(math.radians(a)), -math.sin(math.radians(a)), h=h, hide=True)
            for a, h in ((90, 0), (30, 0), (-30, 1), (-90, 1), (-150, 1), (150, 0))]
    for i in range(6):
        f.bond(ring[i], ring[(i + 1) % 6], *((2, (0, 0)) if i % 2 else (1,)))
    f.branch(ring[0], 'OH', 90)
    bridge = f.branch(ring[1], 'C', 30, h=2, hide=True)
    f.stub(bridge, -30, 0.8)
    f.stub(ring[5], 150, 0.8)
    f.bracket(sub='n', x0=-1.25, x1=2.1)

    # materials of one element (Taran, 0.26.0): the symbol alone, in its colour, on the metal's ingots, nuggets, sheets
    # and blocks, on its melt and that melt's bucket; and carbon on diamond and on a baked carbon electrode's graphite
    elements = {
        'Fe': ('iron', mc('iron_ingot', 'iron_nugget', 'iron_block') + cr('iron_sheet'), mr('molten_iron')),
        'Cu': ('copper', mc('copper_ingot', 'copper_block') + cr('copper_nugget', 'copper_sheet'),
               mr('molten_copper')),
        'Au': ('gold', mc('gold_ingot', 'gold_nugget', 'gold_block') + cr('golden_sheet'), mr('molten_gold')),
        'Zn': ('zinc', cr('zinc_ingot', 'zinc_nugget', 'zinc_block'), mr('molten_zinc')),
        'Al': ('aluminium', mr('aluminium_ingot', 'aluminium_nugget', 'aluminium_sheet', 'aluminium_block'),
               mr('molten_aluminium')),
        'Ti': ('titanium', mr('titanium_ingot', 'titanium_nugget', 'titanium_sheet', 'titanium_block',
                              'titanium_sponge'), []),
        'Na': ('sodium', [], mr('molten_sodium')),
        'C': ('carbon', mc('diamond', 'diamond_block') + mr('carbon_electrode'), []),
    }
    for symbol, (name, items, fluids) in elements.items():
        formula(name, symbol, items, fluids).atom(symbol, 0, 0)

    for name, f in formulas.items():
        write_json(asset('formulas', name + '.json'), f.json())
    gen_formula_font()


# The small digits and signs written in subscripts, charges and counts: 3 by 5 pixels, so they stay crisp at every GUI
# scale where Minecraft's own font scaled down would not.
SMALL_GLYPHS = {
    '0': ['###', '#.#', '#.#', '#.#', '###'],
    '1': ['.#.', '##.', '.#.', '.#.', '###'],
    '2': ['###', '..#', '###', '#..', '###'],
    '3': ['###', '..#', '.##', '..#', '###'],
    '4': ['#.#', '#.#', '###', '..#', '..#'],
    '5': ['###', '#..', '###', '..#', '###'],
    '6': ['###', '#..', '###', '#.#', '###'],
    '7': ['###', '..#', '..#', '.#.', '.#.'],
    '8': ['###', '#.#', '###', '#.#', '###'],
    '9': ['###', '#.#', '###', '..#', '###'],
    '+': ['...', '.#.', '###', '.#.', '...'],
    '-': ['...', '...', '###', '...', '...'],
    'n': ['...', '##.', '#.#', '#.#', '#.#'],
}


def gen_formula_font():
    chars = ''.join(SMALL_GLYPHS)
    img = Image.new('RGBA', (4 * len(chars), 5), (0, 0, 0, 0))
    for i, ch in enumerate(chars):
        for y, row in enumerate(SMALL_GLYPHS[ch]):
            for x, px in enumerate(row):
                if px == '#':
                    img.putpixel((4 * i + x, y), (255, 255, 255, 255))
    save_png(img, asset('textures', 'font', 'formula_small.png'))
    write_json(asset('font', 'formula_small.json'), {'providers': [
        {'type': 'bitmap', 'file': 'mcremix:font/formula_small.png', 'height': 5, 'ascent': 5, 'chars': [chars]}]})


# ------------------------------------------------------------------------------------------------ ponder pause button
def gen_ponder_pause():
    """The pause button on Ponder's scenes (0.28.0): a pause sign while the scene plays and a play sign while it is
    paused, side by side, drawn the way Ponder draws its own buttons' icons: light grey, over a darker copy one pixel
    lower that shows under it."""
    glyph, shade = (226, 226, 226, 255), (142, 142, 142, 255)
    pause = {(x, y) for y in range(3, 12) for x in (*range(4, 7), *range(9, 12))}
    play = {(x, y) for y in range(3, 12) for x in range(6, 11 - abs(y - 7))}
    img = Image.new('RGBA', (32, 16), (0, 0, 0, 0))
    for u, pixels in ((0, pause), (16, play)):
        for x, y in pixels:
            img.putpixel((u + x, y + 1), shade)
        for x, y in pixels:
            img.putpixel((u + x, y), glyph)
    save_png(img, asset('textures', 'gui', 'ponder_pause.png'))


if __name__ == '__main__':
    clean()
    gen_textures()
    gen_fluid_textures()
    gen_models_and_states()
    gen_fluid_models()
    gen_steam_engines()
    gen_boiler_gauges()
    gen_electric_pumps()
    gen_lang()
    gen_data()
    gen_ponder()
    gen_drill_ponder()
    gen_saw_ponder()
    gen_machine_ponder()
    gen_boiler_ponder()
    gen_steam_engine_ponder()
    gen_basin_ponder()
    gen_crucible_ponder()
    gen_tar_ponder()
    gen_heat_sink_ponder()
    gen_cooling_ponder()
    gen_electrolysis_ponder()
    gen_stage4_ponder()
    gen_heat_sink()
    gen_chemicals()
    gen_red_mud_bricks()
    gen_electrolysis()
    gen_encased_parts()
    gen_plastics()
    gen_vat_mixers()
    gen_spark_plug()
    gen_pressurizer()
    gen_titanium_pressurizer()
    gen_machine_tiers()
    gen_wooden_movers()
    gen_formulas()
    gen_cooling()
    gen_titanium()
    gen_titanium_plating()
    gen_titanium_chain()
    gen_titanium_ponder()
    gen_ponder_pause()
    print('generated', len(VARIANTS), 'kinetic variants')
