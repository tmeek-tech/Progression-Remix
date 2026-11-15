"""
The coal tar dyes' eight colours (design/new-colours.md): mauve, lilac, indigo, periwinkle, malachite green, mint,
maroon and rose, on everything vanilla and Create dye. Run by gen_assets.py, last.

Vanilla's and Create's purple files are the templates: every blockstate, model, loot table, recipe, tag and name that
vanilla or Create has for purple is written again for each new colour (vanilla's in our namespace, Create's in its own,
as Create registers its dyed blocks for every colour there is). Textures are vanilla's and Create's own, recoloured from
the nearest vanilla colour in OKLab so they keep their shading, and only where the sixteen differ from one another, so
a bed's frame and pillow, a candle's wick and a postbox's metal stay as they are. The glazed terracotta patterns, after
Victorian encaustic tiles, and the four coal tar dyes' crystals are drawn here.

Reads the Minecraft 1.20.1 client jar, Forge's universal jar and Create's jar from Gradle's caches, so run
./gradlew compileJava once before it.
"""
import glob
import io
import json
import math
import os
import random
import re
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src', 'main', 'resources')

# in DyeColor's order after vanilla's sixteen (NewColour.java): name, display name, colour (textureDiffuseColor)
COLOURS = [
    ('mauve', 'Mauve', 0x9B5FA0),
    ('lilac', 'Lilac', 0xC9A6D4),
    ('indigo', 'Indigo', 0x2B3F7E),
    ('periwinkle', 'Periwinkle', 0x8E9AE0),
    ('malachite_green', 'Malachite Green', 0x1E9E6A),
    ('mint', 'Mint', 0x8FDDB4),
    ('maroon', 'Maroon', 0x7E2033),
    ('rose', 'Rose', 0xC98394),
]
NAMES = [c[0] for c in COLOURS]
DISPLAY = {c[0]: c[1] for c in COLOURS}
NEW = {c[0]: c[2] for c in COLOURS}
# the four dyes, and the pale shade each mixes with white into (Taran: names of their own)
BASE = ['mauve', 'indigo', 'malachite_green', 'maroon']
PALE = {'lilac': 'mauve', 'periwinkle': 'indigo', 'mint': 'malachite_green', 'rose': 'maroon'}
# which vanilla colour's textures each is recoloured from
SOURCE = {'mauve': 'magenta', 'lilac': 'magenta', 'indigo': 'blue', 'periwinkle': 'light_blue',
          'malachite_green': 'cyan', 'mint': 'lime', 'maroon': 'red', 'rose': 'pink'}
# the four dyes have no recipe here: the distillation build's stage 5 makes them from aniline
# (design/distillation.md), and until then they are only in creative (Taran, 2026-11-15)

VANILLA = {
    'white': 0xF9FFFE, 'orange': 0xF9801D, 'magenta': 0xC74EBD, 'light_blue': 0x3AB3DA, 'yellow': 0xFED83D,
    'lime': 0x80C71F, 'pink': 0xF38BAA, 'gray': 0x474F52, 'light_gray': 0x9D9D97, 'cyan': 0x169C9C,
    'purple': 0x8932B8, 'blue': 0x3C44AA, 'brown': 0x835432, 'green': 0x5E7C16, 'red': 0xB02E26, 'black': 0x1D1D21,
}
VANILLA_NAMES = list(VANILLA)
ALL = VANILLA_NAMES + NAMES

# vanilla's dyed blocks and items; each new colour gets its own in our namespace
KINDS = ['wool', 'carpet', 'concrete', 'concrete_powder', 'terracotta', 'glazed_terracotta', 'stained_glass',
         'stained_glass_pane', 'candle', 'candle_cake', 'bed', 'banner', 'wall_banner', 'shulker_box', 'dye']
# vanilla's files for these that are not about a purple thing of ours
SKIP_RECIPES = ('purple_dye.json', 'magenta_dye_from_purple_and_pink.json')
# translucent and cutout blocks: vanilla's own are listed in code, so ours say it in their models
RENDER_TYPES = {'stained_glass': 'minecraft:translucent', 'stained_glass_pane': 'minecraft:translucent',
                'candle': 'minecraft:cutout', 'candle_cake': 'minecraft:cutout'}


def gradle_jar(*patterns):
    home = os.path.expanduser('~/.gradle/caches')
    for p in patterns:
        found = sorted(glob.glob(os.path.join(home, p), recursive=True))
        if found:
            return zipfile.ZipFile(found[0])
    raise SystemExit('gen_colours: no jar in the Gradle cache matches ' + patterns[0] + '; run ./gradlew compileJava')


MC = gradle_jar('neoformruntime/artifacts/minecraft_1.20.1_client.jar', '**/client-1.20.1*.jar')
FORGE = gradle_jar('modules-2/files-2.1/net.minecraftforge/forge/1.20.1-47.4.6/**/forge-1.20.1-47.4.6-universal.jar')
CREATE = gradle_jar('modules-2/files-2.1/com.simibubi.create/create-1.20.1/6.0.8*/**/create-1.20.1-6.0.8*-slim.jar')


def jar_names(jar, prefix):
    return [n for n in jar.namelist() if n.startswith(prefix) and not n.endswith('/')]


def jar_json(jar, name):
    return json.loads(jar.read(name))


def jar_png(jar, name):
    return Image.open(io.BytesIO(jar.read(name))).convert('RGBA')


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def save_png(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)


def res(*parts):
    return os.path.join(RES, *parts)


def merge_tag(path, values):
    """Adds to a tag file written earlier, or starts one; never replaces vanilla's or another mod's."""
    tag = {'replace': False, 'values': []}
    if os.path.exists(path):
        tag = json.load(open(path))
    have = {v if isinstance(v, str) else v['id'] for v in tag['values']}
    for v in values:
        key = v if isinstance(v, str) else v['id']
        if key not in have:
            tag['values'].append(v)
            have.add(key)
    write_json(path, tag)


# ------------------------------------------------------------------------------------------------ colour

def s2l(c):
    c /= 255
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def l2s(c):
    c = 12.92 * c if c <= 0.0031308 else 1.055 * (max(c, 0) ** (1 / 2.4)) - 0.055
    return max(0, min(255, round(c * 255)))


def oklab(rgb):
    r, g, b = (s2l(v) for v in rgb[:3])
    l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
    m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
    s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
    l, m, s = (math.copysign(abs(v) ** (1 / 3), v) for v in (l, m, s))
    return (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s)


def from_oklab(lab):
    L, a, b = lab
    l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
    return (l2s(4.0767416621 * l - 3.3077085119 * m + 0.2309699292 * s),
            l2s(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
            l2s(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s))


def hexrgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def shade(c, dl, k=1.0):
    L, a, b = oklab(c)
    return from_oklab((L + dl, a * k, b * k))


def dyed_mask(variants):
    """The pixels that differ between vanilla's (or Create's) colours of one texture: the dyed part of it."""
    w, h = variants[0].size
    data = [list(v.get_flattened_data()) for v in variants if v.size == (w, h)]
    mask = []
    for i in range(w * h):
        px = [d[i] for d in data]
        if any(p[3] != px[0][3] for p in px):
            mask.append(True)
            continue
        spread = max(max(p[k] for p in px) - min(p[k] for p in px) for k in range(3))
        mask.append(px[0][3] > 0 and spread > 10)
    return mask


def recolour(img, src, colour, mask=None):
    """Moves the dyed pixels of vanilla's texture in src's colour to the new colour, in OKLab: the texture's own
    offset from its dye colour (wool is lighter than its dye, terracotta duller) is carried over, and its shading
    kept, its chroma scaled to the new colour's."""
    data = list(img.get_flattened_data())
    pts = [i for i, p in enumerate(data) if p[3] > 0 and (mask is None or mask[i])]
    if not pts:
        return img.copy()
    labs = {i: oklab(data[i]) for i in pts}
    sm = tuple(sum(v[k] for v in labs.values()) / len(labs) for k in range(3))
    sd = oklab(hexrgb(VANILLA[src]))
    td = oklab(hexrgb(NEW[colour]))
    tm = (td[0] + (sm[0] - sd[0]), td[1] + (sm[1] - sd[1]) * 0.5, td[2] + (sm[2] - sd[2]) * 0.5)
    k = min(math.hypot(tm[1], tm[2]) / (math.hypot(sm[1], sm[2]) or 1e-6), 1.5)
    out = img.copy()
    w = img.size[0]
    pix = out.load()
    for i in pts:
        o = labs[i]
        lab = (tm[0] + (o[0] - sm[0]), tm[1] + (o[1] - sm[1]) * k, tm[2] + (o[2] - sm[2]) * k)
        pix[i % w, i // w] = from_oklab(lab) + (data[i][3],)
    return out


def recolour_from(jar, template, colour):
    """template: a texture's path in the jar with {c} for the colour."""
    src = SOURCE[colour]
    # white is left out: its bed has a whiter pillow than the rest, which would read as dyed
    variants = [jar_png(jar, template.format(c=v)) for v in VANILLA_NAMES
                if v != 'white' and template.format(c=v) in JAR_INDEX[jar]]
    mask = dyed_mask(variants) if len(variants) > 1 else None
    return recolour(jar_png(jar, template.format(c=src)), src, colour, mask)


JAR_INDEX = {MC: set(MC.namelist()), CREATE: set(CREATE.namelist()), FORGE: set(FORGE.namelist())}


# ------------------------------------------------------------------------------------------------ drawn textures

# the four coal tar dyes are crystals, not vanilla's petals and powders: a little heap of shards with a lustre
# (aniline dyes crystallise with a metallic sheen). o outline, d dark, m mid, l light, h highlight
CRYSTALS = [
    '................',
    '................',
    '.......o........',
    '......olo.......',
    '......ohmo......',
    '.....ohlmmo..o..',
    '..o..olmmdo.olo.',
    '.olo.olmmdooohmo',
    'ohlmooolmddolmmo',
    'ohmmdolhmmdolmdo',
    'olmmdolmmmddlmdo',
    '.ommdoommdddmddo',
    '.oodddmmmdddddoo',
    '..oddddddddddoo.',
    '...oooooooooo...',
    '................',
]


def crystal_dye(colour):
    base = hexrgb(NEW[colour])
    pal = {'o': shade(base, -0.30, 1.1), 'd': shade(base, -0.12), 'm': base, 'l': shade(base, 0.10),
           'h': shade(base, 0.24, 0.6)}
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(CRYSTALS):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch] + (255,))
    return img


def glazed_figure(colour):
    """Glazed terracotta after Victorian encaustic tiles (Taran). Each design is a 32x32 figure, symmetric under
    quarter turns about its centre; the tile is its top left quarter, so four tiles turned as vanilla's turn (the
    pinwheel) make the whole figure, and four figures' corners meet in a smaller one, as on an encaustic floor."""
    f = GLAZED[colour]
    base = hexrgb(NEW[colour])
    cream = (233, 220, 190)
    if oklab(base)[0] > 0.75:  # the pale shades: a deeper inlay and a darker ground figure
        pal = {0: base, 1: shade(cream, 0.02), 2: shade(base, -0.28, 1.4), 3: shade(base, -0.10, 1.2)}
    else:
        pal = {0: base, 1: cream, 2: shade(base, -0.16, 1.1), 3: shade(base, 0.08)}
    rnd = random.Random(colour)
    img = Image.new('RGBA', (32, 32))
    ss = 4
    for py in range(32):
        for px in range(32):
            votes = {}
            for sy in range(ss):
                for sx in range(ss):
                    reg = f(px + (sx + 0.5) / ss, py + (sy + 0.5) / ss)
                    votes[reg] = votes.get(reg, 0) + 1
            reg = max(votes, key=votes.get)
            img.putpixel((px, py), shade(pal[reg], rnd.choice((-0.025, 0, 0, 0.02))) + (255,))
    return img


def _fold(x, y):
    a, b = abs(x - 16), abs(y - 16)
    return (a, b) if a >= b else (b, a)


def _corner(x, y):
    a, b = 16 - abs(x - 16), 16 - abs(y - 16)
    return (a, b) if a >= b else (b, a)


# each design gives a point of the figure a region: 0 the ground, 1 cream inlay, 2 dark inlay, 3 a second ground

def _quatrefoil(x, y):  # mauve: a quatrefoil in a frame
    a, b = _fold(x, y)
    r = math.hypot(a, b)
    lobe = math.hypot(a - 6.5, b)
    ca, cb = _corner(x, y)
    cr = math.hypot(ca, cb)
    if r < 2.2:
        return 2
    if lobe < 4.6:
        return 1 if lobe < 3.4 else 2
    if r < 7.5 and abs(a - b) < 1.2:
        return 2
    if cr < 4.5:
        return 1 if cr < 3.2 else 2
    if ca + cb < 8.5 and abs(ca - cb) < 1.1:
        return 1
    if max(abs(x - 16), abs(y - 16)) > 14.6:
        return 2
    return 0


def _fleur(x, y):  # lilac: four fleurs-de-lis about a lozenge
    dx, dy = x - 16, y - 16
    k = round(math.atan2(dy, dx) / (math.pi / 2))
    t = -k * math.pi / 2
    u = dx * math.cos(t) - dy * math.sin(t)
    v = dx * math.sin(t) + dy * math.cos(t)
    if abs(dx) + abs(dy) < 3.6:
        return 2 if abs(dx) + abs(dy) < 2.2 else 1
    if 4 < u < 13 and abs(v) < 1.6 * math.sin(math.pi * (u - 4) / 9) + 0.3:
        return 1
    for s in (-1, 1):
        d = math.hypot(u - 8.5, v - s * 3.6)
        if 1.7 < d < 3.0 and (v * s > 3.0 or u > 8.5):
            return 1
    if 6.2 < u < 7.4 and abs(v) < 3.2:
        return 2
    ca, cb = _corner(x, y)
    if ca + cb < 3.5:
        return 2
    if abs(ca - cb) < 0.9 and ca + cb < 9:
        return 3
    return 0


def _star(x, y):  # indigo: an eight-pointed star of two squares, in a border
    dx, dy = x - 16, y - 16
    s1 = max(abs(dx), abs(dy))
    s2 = max(abs(dx + dy), abs(dx - dy)) / math.sqrt(2)
    if max(s1, s2) < 3.4:
        return 2
    if min(s1, s2) < 7.6:
        return 1 if min(s1, s2) < 6.6 else 2
    ca, cb = _corner(x, y)
    if ca + cb < 5:
        return 1 if ca + cb < 3 else 2
    if max(abs(dx), abs(dy)) > 14.6:
        return 1
    return 0


def _rings(x, y):  # periwinkle: four interlaced rings
    dx, dy = x - 16, y - 16
    on = any(5.6 < math.hypot(dx - cx, dy - cy) < 7.0 for cx, cy in ((7, 0), (-7, 0), (0, 7), (0, -7)))
    r = math.hypot(dx, dy)
    if r < 2.5:
        return 2
    ca, cb = _corner(x, y)
    if math.hypot(ca, cb) < 2.6:
        return 2
    if on:
        return 1
    if r < 6 and min(abs(dx), abs(dy)) < 1.0:
        return 3
    return 0


_KEY = ['1111111', '1000001', '1011101', '1010101', '1010001', '1011111', '1000000', '1111111']


def _meander(x, y):  # malachite green: a Greek key round a plain square
    px, py = int(x), int(y)
    tx, ty = min(px, 31 - px), min(py, 31 - py)
    a, b = (tx, ty) if tx <= ty else (ty, tx)
    if a == 0:
        return 2
    if 1 <= a <= 7:
        return 1 if _KEY[b % 8][a - 1] == '1' else 0
    if a == 8:
        return 2
    d = max(abs(x - 16), abs(y - 16))
    if d < 2.5:
        return 1
    if abs(abs(x - 16) - abs(y - 16)) < 0.8 and d < 7:
        return 3
    return 0


def _rosette(x, y):  # mint: a four-petalled flower, leaves on the diagonals
    dx, dy = x - 16, y - 16
    r = math.hypot(dx, dy)
    ang = math.atan2(dy, dx)
    petal = abs(math.cos(2 * ang))
    if r < 2.4:
        return 2
    if r < 3.4 + 7.5 * petal ** 3:
        return 1 if r < 2.6 + 7.0 * petal ** 3 else 2
    if 4 < r < 2 + 10 * abs(math.sin(2 * ang)) ** 6:
        return 3
    ca, cb = _corner(x, y)
    cr = math.hypot(ca, cb)
    if cr < 5:
        return 1 if cr < 3.2 or cr > 4.2 else 0
    return 0


def _lozenge(x, y):  # maroon: nested lozenges, a chequer where four meet
    dx, dy = abs(x - 16), abs(y - 16)
    d = dx + dy
    if d < 3:
        return 1
    if d < 5:
        return 2
    if d < 9:
        return 1 if d < 8 else 0
    if 10 < d < 12:
        return 2
    ca, cb = _corner(x, y)
    if ca < 4 and cb < 4:
        return 1 if (int(ca) // 2 + int(cb) // 2) % 2 == 0 else 2
    if 15 <= d < 16.5:
        return 3
    return 0


def _roundel(x, y):  # rose: an eight-petalled roundel in a ring
    dx, dy = x - 16, y - 16
    r = math.hypot(dx, dy)
    pet = abs(math.cos(4 * math.atan2(dy, dx)))
    if r < 2.6:
        return 2
    if r < 3.2 + 5.2 * pet ** 2:
        return 1
    if 10.2 < r < 12:
        return 2
    if 9 < r < 10.2:
        return 1
    ca, cb = _corner(x, y)
    if math.hypot(ca, cb) < 3.6:
        return 1
    return 0


GLAZED = {'mauve': _quatrefoil, 'lilac': _fleur, 'indigo': _star, 'periwinkle': _rings,
          'malachite_green': _meander, 'mint': _rosette, 'maroon': _lozenge, 'rose': _roundel}


# ------------------------------------------------------------------------------------------------ vanilla

def ours(text, colour):
    """A vanilla purple file, made the new colour's: purple things become ours, everything else stays vanilla's."""
    text = re.sub(r'minecraft:(block/|item/)?purple_', lambda m: f'mcremix:{m.group(1) or ""}{colour}_', text)
    text = text.replace('forge:dyes/purple', f'forge:dyes/{colour}')
    return text


def kind_of(name):
    """'purple_stained_glass_pane_noside' -> 'stained_glass_pane'."""
    rest = name[len('purple_'):]
    return max((k for k in KINDS if rest.startswith(k)), key=len, default=None)


def gen_vanilla_assets():
    for name in jar_names(MC, 'assets/minecraft/blockstates/purple_'):
        for c in NAMES:
            write_json(res('assets', 'mcremix', 'blockstates', os.path.basename(name).replace('purple', c, 1)),
                       json.loads(ours(MC.read(name).decode(), c)))
    textures = set()
    for folder in ('block', 'item'):
        for name in jar_names(MC, f'assets/minecraft/models/{folder}/purple_'):
            base = os.path.basename(name)
            for c in NAMES:
                model = json.loads(ours(MC.read(name).decode(), c))
                kind = kind_of(base[:-5])
                if folder == 'block' and kind in RENDER_TYPES:
                    model['render_type'] = RENDER_TYPES[kind]
                for t in model.get('textures', {}).values():
                    if t.startswith('mcremix:'):
                        textures.add(t[len('mcremix:'):].replace(c, '{c}', 1))
                write_json(res('assets', 'mcremix', 'models', folder, base.replace('purple', c, 1)), model)
    for t in sorted(textures):
        for c in NAMES:
            out = res('assets', 'mcremix', 'textures', t.format(c=c) + '.png')
            if t == 'block/{c}_glazed_terracotta':
                save_png(glazed_figure(c).crop((0, 0, 16, 16)), out)
            elif t == 'item/{c}_dye':
                if c in BASE:
                    save_png(crystal_dye(c), out)
                else:  # the pale shades are mixed dyes, so they look like vanilla's mixed dyes (pink, lime...)
                    save_png(recolour(jar_png(MC, 'assets/minecraft/textures/item/purple_dye.png'), 'purple', c), out)
            else:
                save_png(recolour_from(MC, f'assets/minecraft/textures/{t}.png', c), out)
    for c in NAMES:
        # beds' and shulker boxes' textures are looked up in vanilla's namespace by colour name (Sheets)
        save_png(recolour_from(MC, 'assets/minecraft/textures/entity/bed/{c}.png', c),
                 res('assets', 'minecraft', 'textures', 'entity', 'bed', c + '.png'))
        save_png(recolour_from(MC, 'assets/minecraft/textures/entity/shulker/shulker_{c}.png', c),
                 res('assets', 'minecraft', 'textures', 'entity', 'shulker', f'shulker_{c}.png'))
        # a llama's carpet (LlamaDecorLayerMixin)
        save_png(recolour_from(MC, 'assets/minecraft/textures/entity/llama/decor/{c}.png', c),
                 res('assets', 'mcremix', 'textures', 'entity', 'llama', 'decor', c + '.png'))


def wool_like(kind, colour):
    return f'mcremix:{colour}_{kind}' if colour in NAMES else f'minecraft:{colour}_{kind}'


def dye_item(colour):
    return wool_like('dye', colour)


def gen_vanilla_data():
    for name in jar_names(MC, 'data/minecraft/loot_tables/blocks/purple_'):
        for c in NAMES:
            write_json(res('data', 'mcremix', 'loot_tables', 'blocks', os.path.basename(name).replace('purple', c, 1)),
                       json.loads(ours(MC.read(name).decode(), c)))
    for c in NAMES:
        write_json(res('data', 'mcremix', 'loot_tables', 'entities', 'sheep', c + '.json'),
                   json.loads(ours(MC.read('data/minecraft/loot_tables/entities/sheep/purple.json').decode(), c)))
    for name in jar_names(MC, 'data/minecraft/recipes/'):
        base = os.path.basename(name)
        if 'purple' not in base or base in SKIP_RECIPES or base.startswith('dye_purple_'):
            continue
        for c in NAMES:
            write_json(res('data', 'mcremix', 'recipes', base.replace('purple', c, 1)),
                       json.loads(ours(MC.read(name).decode(), c)))
    # dyeing wool, carpets and beds: each colour's recipe takes any other colour's, so vanilla's are written again
    # (in vanilla's namespace, over its own) to take ours as well
    for kind, category, group in (('wool', 'building', 'wool'), ('carpet', 'decorations', 'carpet'),
                                  ('bed', 'decorations', 'bed')):
        for c in ALL:
            recipe = {'type': 'minecraft:crafting_shapeless', 'category': category, 'group': group,
                      'ingredients': [{'item': dye_item(c)},
                                      [{'item': wool_like(kind, o)} for o in ALL if o != c]],
                      'result': {'item': wool_like(kind, c)}}
            ns = 'mcremix' if c in NAMES else 'minecraft'
            write_json(res('data', ns, 'recipes', f'dye_{c}_{kind}.json'), recipe)
    for pale, base in PALE.items():
        write_json(res('data', 'mcremix', 'recipes', f'{pale}_dye.json'), {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': f'{pale}_dye',
            'ingredients': [{'item': dye_item(base)}, {'item': 'minecraft:white_dye'}],
            'result': {'item': dye_item(pale), 'count': 2}})
    # tags: wherever vanilla or Forge lists a purple thing, ours go too
    for jar, ns_prefix in ((MC, 'data/minecraft/tags/'), (FORGE, 'data/forge/tags/')):
        for name in jar_names(jar, ns_prefix):
            text = jar.read(name).decode()
            ns = ns_prefix.split('/')[1]
            rel = name[len(f'data/{ns}/tags/'):]
            if 'purple' in rel:  # a tag of its own per colour (forge:dyes/purple)
                for c in NAMES:
                    vals = [ours(v, c) for v in jar_json(jar, name)['values']]
                    merge_tag(res('data', ns, 'tags', rel.replace('purple', c)), vals)
                continue
            if 'purple' not in text:
                continue
            vals = []
            for v in jar_json(jar, name)['values']:
                if isinstance(v, str) and 'purple' in v:
                    vals += [ours(v, c) for c in NAMES]
            if vals:
                merge_tag(res('data', ns, 'tags', rel), vals)


def vanilla_lang():
    en = jar_json(MC, 'assets/minecraft/lang/en_us.json')
    lang = {}
    for key, value in en.items():
        for c in NAMES:
            m = re.match(r'(block|item)\.minecraft\.purple_(.*)$', key)
            if m and kind_of('purple_' + m.group(2)):
                lang[f'{m.group(1)}.mcremix.{c}_{m.group(2)}'] = value.replace('Purple', DISPLAY[c])
            elif key.endswith('.purple'):  # banner patterns, firework stars, shields, colour names
                lang[key[:-len('purple')] + c] = value.replace('Purple', DISPLAY[c])
    return lang


# ------------------------------------------------------------------------------------------------ Create

def create_ours(text, colour):
    """A Create purple file, made the new colour's: vanilla's purple things become ours, Create's stay Create's."""
    return re.sub(r'purple', colour, ours(text, colour))


def gen_create():
    """Create registers its seats, sails, valve handles, toolboxes, postboxes, table cloths and nixie tubes for every
    colour DyeColor has, so it has eight more of each: their files go in Create's namespace, from its purple ones."""
    for name in CREATE.namelist():
        if 'purple' not in os.path.basename(name) and '/purple.' not in name and '/purple/' not in name:
            continue
        if name.startswith('assets/create/textures/'):
            for c in NAMES:
                out = res(*name.replace('purple', c).split('/'))
                if name.endswith('.png'):
                    save_png(recolour_from(CREATE, name.replace('purple', '{c}'), c), out)
                else:
                    os.makedirs(os.path.dirname(out), exist_ok=True)
                    open(out, 'wb').write(CREATE.read(name))
        elif name.endswith('.json') and (name.startswith('assets/create/') or
                                         name.startswith('data/create/loot_tables/') or
                                         name.startswith('data/create/recipes/crafting/')):
            if 'advancement' in name or 'compat' in name:
                continue
            for c in NAMES:
                write_json(res(*name.replace('purple', c).split('/')),
                           json.loads(create_ours(CREATE.read(name).decode(), c)))
    for name in CREATE.namelist():
        if '/tags/' not in name or not name.endswith('.json'):
            continue
        data = jar_json(CREATE, name)
        vals = [v.replace('purple', c) for v in data['values'] if isinstance(v, str) and v.startswith('create:purple_')
                for c in NAMES]
        if vals:
            merge_tag(res(*name.split('/')), vals)


def create_lang():
    en = jar_json(CREATE, 'assets/create/lang/en_us.json')
    lang = {}
    for key, value in en.items():
        if key.startswith('block.create.purple_') or key.startswith('item.create.purple_'):
            for c in NAMES:
                lang[key.replace('purple', c)] = value.replace('Purple', DISPLAY[c])
    return lang


# ------------------------------------------------------------------------------------------------

def gen_colours():
    gen_vanilla_assets()
    gen_vanilla_data()
    gen_create()
    path = res('assets', 'mcremix', 'lang', 'en_us.json')
    lang = json.load(open(path)) if os.path.exists(path) else {}
    lang.update(vanilla_lang())
    lang.update(create_lang())
    write_json(path, lang)


if __name__ == '__main__':
    gen_colours()
