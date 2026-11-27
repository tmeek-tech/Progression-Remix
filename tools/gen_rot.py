"""
Rotted wood (0.43.0, Taran's ask of 2026-11-27): the timber of ruined villages after years of rain getting in. Every
overworld wood comes rotted as logs, wood, stripped logs and stripped wood, planks, stairs, slabs, fences, fence gates,
doors and trapdoors, the lists read from RottedWood.java so the Java stays the one list. gen_assets.py calls run()
after gen_ores, passing itself; this file adds to the lang and tag files written before it and owns the files of its
own blocks.

Each texture is drawn from vanilla's for the sound wood, so a ruin's rotted timber still reads as the wood it was:
darker and duller, the warmth gone out of it toward grey-brown; a damp patch or two fading softly into the wood where
the seams held water, and a short split or two along the grain; on a log, a strip of bark peeled away with the wood
under it showing; on a sawn end, the heart gone soft with two checks running out of it. Since 0.43.1 (Taran: the first
look was messy, its black lines too strong) every mark is darker than the wood round it by a share of that wood's own
lightness, about as dark as vanilla's seams between boards, and the specks, fungus and cubed checking are gone. Doors
and trapdoors have boards missing (Taran's card) and a foot rotted away where water stood, and their iron has rusted.
The blockstates and models are vanilla's own, pointed at the rotted textures.

A rotted block leaves a stick about a third of the time (Taran's card), and itself with Silk Touch, as glass does. It is
in none of the item tags recipes and furnaces read, so it makes no planks or tools; it is in the block tags that make
fences join, villagers and zombies treat doors as wooden, and mushrooms grow on it in any light, as on podzol.
"""
import math
import os
import random
import re

import gen_colours as C
import gen_decor as D

G = None  # gen_assets, set by run()

JAVA = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'src', 'main', 'java', 'com',
                    'mcremix', 'village', 'RottedWood.java')
# the chance a rotted block leaves a stick, without Silk Touch (Taran's card: a stick now and then)
STICK = 0.33
# the kinds of whole blocks of wood, which mushrooms grow on (RottedWood.Kind#whole)
WHOLE = ('log', 'wood', 'stripped_log', 'stripped_wood', 'planks')


def read_lists():
    """RottedWood.java's woods, and its kinds with the pattern of each one's vanilla id."""
    src = open(JAVA).read()
    woods = [m.group(1).lower() for m in re.finditer(r'^\t\t([A-Z_]+)\(BlockSetType\.', src, re.M)]
    kinds = [(m.group(1).lower(), m.group(2)) for m in re.finditer(r'^\t\t([A-Z_]+)\("([%\w]+)"\)', src, re.M)]
    assert woods and len(kinds) == 11, (woods, kinds)
    return woods, kinds


WOODS, KINDS = read_lists()


def run(gen):
    global G
    G = gen
    textures()
    models()
    loot()
    tags()
    lang()


def blocks(kind=None):
    """The rotted blocks' ids, of one kind or all of them."""
    return ['rotted_' + pattern % wood for k, pattern in KINDS if kind in (None, k) for wood in WOODS]


# ------------------------------------------------------------------------------------------------ drawing
#
# Rot is drawn quietly (0.43.1, Taran: the first textures looked messy, their black lines too strong). Every mark is
# darker than the wood round it by a share of that wood's own lightness, never a fixed near-black: a crack is about as
# dark as vanilla's seams between boards, a damp patch fades smoothly into the wood. There are few marks, and each sits
# where the wood's own shape puts it: damp in the joints, splits along the grain, the heart of a sawn end.

def vanilla(name, folder='block'):
    return C.jar_png(C.MC, f'assets/minecraft/textures/{folder}/{name}.png')


def noise_fn(seed, size, cell):
    """Smooth value noise over a size-pixel square, tiling, with features about cell pixels across."""
    rng = random.Random(seed)
    w = max(1, size // cell)
    grid = [[rng.random() for _ in range(w)] for _ in range(w)]

    def at(x, y):
        gx, gy = x / cell, y / cell
        x0, y0 = math.floor(gx), math.floor(gy)
        fx, fy = gx - x0, gy - y0
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        a, b = grid[y0 % w][x0 % w], grid[y0 % w][(x0 + 1) % w]
        c, d = grid[(y0 + 1) % w][x0 % w], grid[(y0 + 1) % w][(x0 + 1) % w]
        return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy
    return at


class Tex:
    """A texture being rotted: its pixels, read and written in OKLab, and the random its marks are placed by."""

    def __init__(self, img, seed, tiles=True):
        self.img = img.copy()
        self.px = self.img.load()
        self.W, self.H = self.img.size
        self.rng = random.Random(seed)
        self.seed = seed
        # a block's side tiles with its neighbours', so marks wrap round its edges; a door's or an item's do not
        self.tiles = tiles

    def at(self, x, y):
        """The pixel a mark at (x, y) lands on, wrapped if the texture tiles, or None off a texture that does not."""
        if self.tiles:
            return x % self.W, y % self.H
        return (x, y) if 0 <= x < self.W and 0 <= y < self.H else None

    def ok(self, x, y):
        return 0 <= x < self.W and 0 <= y < self.H and self.px[x, y][3] > 0

    def lab(self, x, y):
        return C.oklab(self.px[x, y][:3])

    def put_lab(self, x, y, lab):
        self.px[x, y] = C.from_oklab(lab) + (self.px[x, y][3],)

    def scale(self, x, y, by, chroma=1.0):
        """Darkens (or lightens) a pixel by a share of its own lightness, and its colour by chroma."""
        p = self.at(x, y)
        if p and self.ok(*p):
            L, a, b = self.lab(*p)
            self.put_lab(*p, (L * by, a * chroma, b * chroma))

    def hole(self, x, y):
        if 0 <= x < self.W and 0 <= y < self.H:
            self.px[x, y] = (0, 0, 0, 0)


def dull(t, darken=0.84, lift=0.02, chroma=0.5, mottle=0.05):
    """Rotted wood's colour: darker and duller than sound wood, going grey-brown, mottled softly where it stayed wet."""
    n1, n2 = noise_fn(t.seed * 7 + 1, 16, 4), noise_fn(t.seed * 7 + 2, 16, 8)
    for y in range(t.H):
        for x in range(t.W):
            if t.px[x, y][3] == 0:
                continue
            L, a, b = t.lab(x, y)
            n = n1(x, y) * 0.3 + n2(x, y) * 0.7
            k = chroma * (0.85 + 0.3 * n)
            L2 = L * darken + lift + (n - 0.5) * mottle
            if L2 > 0.7:  # pale wood (birch, its bark) greys over most
                L2 = 0.7 + (L2 - 0.7) * 0.45
            # rot and weather take the warmth out of wood first
            t.put_lab(x, y, (L2, a * k - 0.002, b * k + 0.002))


def mean_L(t):
    s = [t.lab(x, y)[0] for y in range(t.H) for x in range(t.W) if t.px[x, y][3] > 0]
    return sum(s) / len(s)


def stain(t, cx, cy, rx, ry, depth=0.15):
    """
    A damp patch round (cx, cy), rx by ry pixels: darkest in its middle, by depth of the wood's own lightness, fading
    smoothly to nothing at its edge, its colour gone greyer and a touch green.
    """
    for y in range(t.H):
        for x in range(t.W):
            if not t.ok(x, y):
                continue
            dx, dy = abs(x - cx), abs(y - cy)
            if t.tiles:
                dx, dy = min(dx, t.W - dx), min(dy, t.H - dy)
            d2 = (dx / rx) ** 2 + (dy / ry) ** 2
            if d2 >= 1:
                continue
            f = (1 - d2) ** 2
            L, a, b = t.lab(x, y)
            t.put_lab(x, y, (L * (1 - depth * f), a * (1 - 0.3 * f) - 0.005 * f, b * (1 - 0.25 * f)))


def split(t, x, y, n, dx, dy, by=0.74):
    """A crack n pixels long from (x, y), a step of (dx, dy) at a time: about as dark as a seam, its ends fainter."""
    for k in range(n):
        end = n > 2 and k in (0, n - 1)
        t.scale(x + k * dx, y + k * dy, (by + 1) / 2 if end else by, 0.85)


def apart(a, b, size=16):
    """How far apart two rows or columns are, round a tiling texture."""
    return min((a - b) % size, (b - a) % size)


def rot_planks(img, seed):
    """Planks: water stood in a joint or two, the joint opened a little; a board split along its grain."""
    t = Tex(img, seed)
    dull(t)
    # the joints between boards are the darkest rows
    rowL = [sum(t.lab(x, y)[0] for x in range(16)) / 16 for y in range(16)]
    seams = sorted(range(16), key=lambda y: rowL[y])[:4]
    for s in t.rng.sample(seams, 2):
        cx = t.rng.randrange(16)
        stain(t, cx, s + 0.5, t.rng.uniform(4, 5.5), t.rng.uniform(2.4, 3.2), 0.2)
        for k in range(-2, 3):
            t.scale(cx + k, s, 0.88 if abs(k) < 2 else 0.94)
    # a board split along its grain, in its middle row, well clear of the damp
    for _ in range(t.rng.choice((1, 2))):
        s = t.rng.choice(seams)
        split(t, t.rng.randrange(16), s + 2, t.rng.randrange(3, 6), 1, 0)
    return t.img


def rot_side(img, seed, inner=None):
    """
    A log's side, the grain running up it. Stripped (no inner), it is split along the grain, a damp streak down it.
    With inner (the stripped log's side), it is bark, fallen away in one patch with the soft wood under it showing.
    """
    t = Tex(img, seed)
    dull(t, darken=0.86 if inner else 0.84)
    if inner is None:
        splits = []
        for _ in range(2):
            # never two splits side by side at one height: on a lone block, such as a plains lamp post's head, a
            # pair reads as a face's eyes
            while True:
                x, y = t.rng.randrange(16), t.rng.randrange(16)
                if all(apart(x, sx) <= 1 or apart(y, sy) >= 7 for sx, sy in splits):
                    break
            splits.append((x, y))
            split(t, x, y, t.rng.randrange(3, 6), 0, 1)
        stain(t, t.rng.randrange(16), t.rng.randrange(16), t.rng.uniform(2.2, 3), t.rng.uniform(4.5, 6), 0.13)
        return t.img
    ip = inner.load()
    bark = mean_L(t)
    # bark peels away along the grain: a strip two pixels wide, its ends one, its sides wandering a little
    x, y0, h = t.rng.randrange(16), t.rng.randrange(16), t.rng.randrange(5, 8)
    patch = set()
    for k in range(h):
        if 0 < k < h - 1 and t.rng.random() < 0.3:
            x += t.rng.choice((-1, 1))
        width = 1 if k in (0, h - 1) else 2
        patch.update(((x + i) % 16, (y0 + k) % 16) for i in range(width))
    # the soft wood under it, dulled, never much paler than the bark round it, so it reads as a strip gone and not
    # a mark on it
    for px, py in patch:
        L, a, b = C.oklab(ip[px, py][:3])
        t.put_lab(px, py, (min(L * 0.78 + 0.02, bark + 0.14), a * 0.45, b * 0.45))
    # the bark's broken edge along each side, a shade darker
    for px, py in patch:
        for side in (-1, 1):
            q = ((px + side) % 16, py)
            if q not in patch:
                t.scale(*q, 0.88)
    stain(t, t.rng.randrange(16), t.rng.randrange(16), t.rng.uniform(2.2, 3), t.rng.uniform(4.5, 6), 0.12)
    return t.img


def rot_end(img, seed):
    """A log's sawn end: the heart gone soft and a little dark, two checks running out from it."""
    t = Tex(img, seed)
    dull(t, mottle=0.03)
    cx, cy = 7.5 + t.rng.uniform(-0.8, 0.8), 7.5 + t.rng.uniform(-0.8, 0.8)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if r < 3.5:
                t.scale(x, y, 0.8 + 0.2 * (r / 3.5) ** 2, 0.9)
    done = set()
    a0 = t.rng.uniform(0, 2 * math.pi)
    for ang in (a0, a0 + t.rng.uniform(2.2, 4.1)):
        r, r1 = 1.6, t.rng.uniform(4, 5.5)
        while r < r1:
            p = (round(cx + math.cos(ang) * r), round(cy + math.sin(ang) * r))
            if p not in done:
                done.add(p)
                t.scale(*p, 0.76 if r < r1 - 1 else 0.87, 0.9)
            r += 0.7
            ang += t.rng.uniform(-0.12, 0.12)
    return t.img


def rust_metal(t, src):
    """A door's iron (hinges, bands, a latch) gone to a dull rust: the grey pixels of the sound door's texture."""
    sp = src.load()
    for y in range(t.H):
        for x in range(t.W):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            L, A, B = C.oklab((r, g, b))
            if math.hypot(A, B) < 0.018 and 0.35 < L < 0.86:
                k = 0.85 + 0.15 * t.rng.random()
                t.put_lab(x, y, (L * 0.78, 0.03 * k + 0.008, 0.04 * k + 0.01))


def chew(t, x0, x1, y_edge, step, depth_max):
    """Eats an edge away unevenly, from row y_edge inward (step -1 eats up from the foot), its new edge a shade dark."""
    d = t.rng.randrange(1, depth_max + 1)
    for x in range(x0, x1):
        d = max(0, min(depth_max, d + t.rng.choice((-1, 0, 0, 1))))
        for k in range(d):
            t.hole(x, y_edge + k * step)
        t.scale(x, y_edge + d * step, 0.82)


def gap(t, x0, w, y0, h):
    """A board gone: a hole, its broken ends a little uneven, the wood round it a shade dark."""
    for x in range(x0, x0 + w):
        top, bottom = y0 + t.rng.randrange(0, 2), y0 + h - t.rng.randrange(0, 2)
        for y in range(top, bottom):
            t.hole(x, y)
        t.scale(x, top - 1, 0.82)
        t.scale(x, bottom, 0.82)
    for y in range(y0, y0 + h):
        t.scale(x0 - 1, y, 0.9)
        t.scale(x0 + w, y, 0.9)


def rot_door(img, seed, half):
    """A door's half: a board missing; on the lower half the foot rotted away and damp where water stood."""
    t = Tex(img, seed, tiles=False)
    dull(t)
    rust_metal(t, img)
    if half == 'bottom':
        stain(t, t.rng.uniform(4, 12), 15, t.rng.uniform(4.5, 6), t.rng.uniform(4, 5.5), 0.14)
        x0 = t.rng.randrange(1, 5)
        chew(t, x0, x0 + t.rng.randrange(8, 12), 15, -1, 3)
        gap(t, t.rng.randrange(3, 10), 3, t.rng.randrange(2, 5), t.rng.randrange(5, 8))
    else:
        gap(t, t.rng.randrange(3, 10), 3, t.rng.randrange(3, 8), t.rng.randrange(5, 8))
    return t.img


def rot_trapdoor(img, seed):
    """A trapdoor: a board missing, an edge rotted away, damp along it."""
    t = Tex(img, seed, tiles=False)
    dull(t)
    rust_metal(t, img)
    x0 = t.rng.randrange(0, 7)
    w = t.rng.randrange(5, 9)
    stain(t, x0 + w / 2, 15, t.rng.uniform(4, 5.5), t.rng.uniform(3, 4.5), 0.14)
    chew(t, x0, x0 + w, 15, -1, 3)
    gap(t, t.rng.randrange(2, 11), 3, t.rng.randrange(1, 6), t.rng.randrange(5, 8))
    return t.img


def rot_door_item(img, seed):
    """The door's item, drawn small: a notch out of its foot."""
    t = Tex(img, seed, tiles=False)
    dull(t)
    ys = [y for y in range(16) if any(t.px[x, y][3] for x in range(16))]
    xs = [x for x in range(16) if any(t.px[x, y][3] for y in range(16))]
    bottom = max(ys)
    x0 = t.rng.randrange(min(xs) + 1, max(xs) - 2)
    for x in range(x0, x0 + 2):
        t.hole(x, bottom)
        if t.rng.random() < 0.6:
            t.hole(x, bottom - 1)
    rust_metal(t, img)
    return t.img


# ------------------------------------------------------------------------------------------------ files

def textures():
    def save(img, name):
        G.save_png(img, G.asset('textures', 'block', f'rotted_{name}.png'))

    for i, w in enumerate(WOODS):
        strip = vanilla(f'stripped_{w}_log')
        save(rot_planks(vanilla(f'{w}_planks'), 1000 + i), f'{w}_planks')
        save(rot_side(vanilla(f'{w}_log'), 2000 + i, inner=strip), f'{w}_log')
        save(rot_side(strip, 3000 + i), f'stripped_{w}_log')
        save(rot_end(vanilla(f'{w}_log_top'), 4000 + i), f'{w}_log_top')
        save(rot_end(vanilla(f'stripped_{w}_log_top'), 4100 + i), f'stripped_{w}_log_top')
        save(rot_door(vanilla(f'{w}_door_top'), 5000 + i, 'top'), f'{w}_door_top')
        save(rot_door(vanilla(f'{w}_door_bottom'), 6000 + i, 'bottom'), f'{w}_door_bottom')
        save(rot_trapdoor(vanilla(f'{w}_trapdoor'), 7000 + i), f'{w}_trapdoor')
        G.save_png(rot_door_item(vanilla(f'{w}_door', 'item'), 8000 + i),
                   G.asset('textures', 'item', f'rotted_{w}_door.png'))


def own_textures(w):
    """The textures drawn rotted for a wood, by their vanilla names."""
    return {f'{w}_planks', f'{w}_log', f'{w}_log_top', f'stripped_{w}_log', f'stripped_{w}_log_top', f'{w}_door_top',
            f'{w}_door_bottom', f'{w}_trapdoor'}


def rotted_ref(ref):
    """minecraft:block/oak_slab -> mcremix:block/rotted_oak_slab, for a model or a texture."""
    folder, name = ref.split(':', 1)[-1].split('/', 1)
    return f'mcremix:{folder}/rotted_{name}'


def copy_model(name, own, done):
    """Vanilla's block model of a sound block as the rotted block's, drawing the rotted textures."""
    if name in done:
        return
    done.add(name)
    obj = C.jar_json(C.MC, f'assets/minecraft/models/block/{name}.json')
    for key, tex in obj.get('textures', {}).items():
        if tex.startswith('#'):
            continue
        if tex.split(':', 1)[-1].split('/', 1)[1] not in own:
            raise ValueError(f'{name} draws {tex}, which is not drawn rotted')
        obj['textures'][key] = rotted_ref(tex)
    D.model('rotted_' + name, obj)


def applies(states):
    """The model references of a blockstate file, variants or multipart, to be changed in place."""
    for v in states.get('variants', {}).values():
        yield from (v if isinstance(v, list) else [v])
    for part in states.get('multipart', []):
        a = part['apply']
        yield from (a if isinstance(a, list) else [a])


def models():
    for w in WOODS:
        own, done = own_textures(w), set()
        for _, pattern in KINDS:
            block = pattern % w
            states = C.jar_json(C.MC, f'assets/minecraft/blockstates/{block}.json')
            for apply in applies(states):
                copy_model(apply['model'].split('/', 1)[1], own, done)
                apply['model'] = rotted_ref(apply['model'])
            D.states('rotted_' + block, states)
            item = C.jar_json(C.MC, f'assets/minecraft/models/item/{block}.json')
            if item['parent'].split(':', 1)[-1].startswith('block/'):
                copy_model(item['parent'].split('/', 1)[1], own, done)
                item['parent'] = rotted_ref(item['parent'])
            for key, tex in item.get('textures', {}).items():
                item['textures'][key] = rotted_ref(tex)
            D.item_model('rotted_' + block, item)


def loot():
    """A stick about a third of the time, the block itself with Silk Touch (both halves of a double slab)."""
    silk = {'condition': 'minecraft:match_tool',
            'predicate': {'enchantments': [{'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}
    for kind, pattern in KINDS:
        for w in WOODS:
            name = 'rotted_' + pattern % w
            block = 'mcremix:' + name
            itself = {'type': 'minecraft:item', 'name': block, 'conditions': [silk]}
            if kind == 'slab':
                itself['functions'] = [{'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [
                    {'condition': 'minecraft:block_state_property', 'block': block, 'properties': {'type': 'double'}}]}]
            stick = {'type': 'minecraft:item', 'name': 'minecraft:stick',
                     'conditions': [{'condition': 'minecraft:random_chance', 'chance': STICK}]}
            conditions = [{'condition': 'minecraft:survives_explosion'}]
            if kind == 'door':  # a door drops once, from its lower half
                conditions.insert(0, {'condition': 'minecraft:block_state_property', 'block': block,
                                      'properties': {'half': 'lower'}})
            G.write_json(G.data('mcremix', 'loot_tables', 'blocks', name + '.json'), {
                'type': 'minecraft:block',
                'pools': [{'rolls': 1, 'bonus_rolls': 0, 'conditions': conditions,
                           'entries': [{'type': 'minecraft:alternatives', 'children': [itself, stick]}]}],
                'random_sequence': f'mcremix:blocks/{name}'})


def tags():
    m = lambda names: [f'mcremix:{n}' for n in names]
    D.add_tag('minecraft', 'blocks', 'mineable/axe', m(blocks()))
    # blocks only: fences join sound wooden fences, villagers open the doors and zombies break them in as wooden ones.
    # The item tags are left out, as recipes and furnaces read them
    D.add_tag('minecraft', 'blocks', 'wooden_fences', m(blocks('fence')))
    D.add_tag('minecraft', 'blocks', 'fence_gates', m(blocks('fence_gate')))
    D.add_tag('minecraft', 'blocks', 'wooden_doors', m(blocks('door')))
    D.add_tag('minecraft', 'blocks', 'wooden_trapdoors', m(blocks('trapdoor')))
    # mushrooms grow on whole rotted wood in any light, as on podzol
    D.add_tag('minecraft', 'blocks', 'mushroom_grow_block', m([n for k in WHOLE for n in blocks(k)]))
    for kind in ('blocks', 'items'):
        D.add_tag('mcremix', kind, 'rotted_wood', m(blocks()))


TAIL = ' Leaves a _stick_ now and then; _Silk Touch_ keeps it whole.'
SUMMARIES = {
    'log': 'Timber from a _ruined village_, gone soft with years of rain. _Mushrooms_ grow on it in any light.' + TAIL,
    'planks': ('Floorboards from a _ruined village_, gone soft with years of rain. Fine to walk on, but _jump_ or '
               '_drop_ onto them and they may give way.' + TAIL),
    'fence': 'Timber from a _ruined village_, gone soft with years of rain.' + TAIL,
    'door': 'From a _ruined village_: boards missing, iron rusted, and it still shuts.' + TAIL,
}
SUMMARIES.update({'wood': SUMMARIES['log'], 'stripped_log': SUMMARIES['log'], 'stripped_wood': SUMMARIES['log'],
                  'stairs': SUMMARIES['planks'], 'slab': SUMMARIES['planks'], 'fence_gate': SUMMARIES['fence'],
                  'trapdoor': SUMMARIES['door']})


def lang():
    names = C.jar_json(C.MC, 'assets/minecraft/lang/en_us.json')
    text = {}
    for kind, pattern in KINDS:
        for w in WOODS:
            name = 'rotted_' + pattern % w
            text[f'block.mcremix.{name}'] = 'Rotted ' + names[f'block.minecraft.{pattern % w}']
            text[f'block.mcremix.{name}.tooltip.summary'] = SUMMARIES[kind]
    D.merge_json(G.asset('lang', 'en_us.json'), lambda obj: obj.update(text))
