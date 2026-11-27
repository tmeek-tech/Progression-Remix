"""
Rotted wood (0.43.0, Taran's ask of 2026-11-27): the timber of ruined villages after years of rain getting in. Every
overworld wood comes rotted as logs, wood, stripped logs and stripped wood, planks, stairs, slabs, fences, fence gates,
doors and trapdoors, the lists read from RottedWood.java so the Java stays the one list. gen_assets.py calls run()
after gen_ores, passing itself; this file adds to the lang and tag files written before it and owns the files of its
own blocks.

Each texture is drawn from vanilla's for the sound wood, so a ruin's rotted timber still reads as the wood it was:
darker and duller, the warmth gone out of it toward grey-brown and mottled where it stayed wet; the seams between boards
opened up, the boards checked into little cubes across the grain and split along it (brown rot), a soft dark spot or
two and a few tufts of pale mycelium; on a log, bark fallen away in patches with the soft wood showing and a small
bracket fungus; on a sawn end, the heart gone soft and dark with cracks running out of it. Doors and trapdoors have
boards missing (Taran's card) and a foot rotted away where water stood, and their iron has rusted. The blockstates and
models are vanilla's own, pointed at the rotted textures.

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

    def __init__(self, img, seed):
        self.img = img.copy()
        self.px = self.img.load()
        self.W, self.H = self.img.size
        self.rng = random.Random(seed)
        self.seed = seed

    def ok(self, x, y):
        return 0 <= x < self.W and 0 <= y < self.H and self.px[x, y][3] > 0

    def lab(self, x, y):
        return C.oklab(self.px[x, y][:3])

    def put_lab(self, x, y, lab):
        self.px[x, y] = C.from_oklab(lab) + (self.px[x, y][3],)

    def scale(self, x, y, by, chroma=1.0):
        """Darkens (or lightens) a pixel, and its colour by chroma."""
        if self.ok(x, y):
            L, a, b = self.lab(x, y)
            self.put_lab(x, y, (L * by, a * chroma, b * chroma))

    def paint(self, x, y, lab):
        if self.ok(x, y):
            self.put_lab(x, y, lab)

    def hole(self, x, y):
        if 0 <= x < self.W and 0 <= y < self.H:
            self.px[x, y] = (0, 0, 0, 0)


def dull(t, darken=0.84, lift=0.02, chroma=0.5, mottle=0.08):
    """Rotted wood's colour: darker and duller than sound wood, going grey-brown, mottled where it stayed wet."""
    n1, n2 = noise_fn(t.seed * 7 + 1, 16, 4), noise_fn(t.seed * 7 + 2, 16, 8)
    for y in range(t.H):
        for x in range(t.W):
            if t.px[x, y][3] == 0:
                continue
            L, a, b = t.lab(x, y)
            n = n1(x, y) * 0.55 + n2(x, y) * 0.45
            k = chroma * (0.8 + 0.4 * n)
            L2 = L * darken + lift + (n - 0.5) * mottle
            if L2 > 0.7:  # pale wood (birch, its bark) greys over most
                L2 = 0.7 + (L2 - 0.7) * 0.45
            # rot and weather take the warmth out of wood first
            t.put_lab(x, y, (L2, a * k - 0.002, b * k + 0.002))


def mean_L(t):
    s = [t.lab(x, y)[0] for y in range(t.H) for x in range(t.W) if t.px[x, y][3] > 0]
    return sum(s) / len(s)


def crack_colour(t):
    return mean_L(t) * 0.45, 0.008, 0.012


def fungus(t, n, near=None):
    """Pale grey-white mycelium in small tufts, near the given spots or anywhere."""
    for _ in range(n):
        x, y = t.rng.choice(near) if near else (t.rng.randrange(t.W), t.rng.randrange(t.H))
        L = 0.74 + t.rng.random() * 0.06
        t.paint(x, y, (L, 0.003, 0.016))
        dx, dy = t.rng.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
        if t.rng.random() < 0.7:
            t.paint(x + dx, y + dy, (L - 0.07, 0.003, 0.018))


def rot_planks(img, seed):
    """Planks: the joints opened up, the boards checked across the grain and split along it, soft spots, mycelium."""
    t = Tex(img, seed)
    dull(t)
    # the seams between boards are the darkest rows
    rowL = [sum(t.lab(x, y)[0] for x in range(16)) / 16 for y in range(16)]
    seams = sorted(range(16), key=lambda y: rowL[y])[:4]
    dark = crack_colour(t)
    seam_spots = []
    for s in seams:
        # a joint opening up: a run of the seam darker, the edge of the board above it crumbling
        for _ in range(t.rng.randrange(1, 3)):
            x0 = t.rng.randrange(16)
            for k in range(t.rng.randrange(2, 5)):
                x = (x0 + k) % 16
                t.paint(x, s, dark)
                if t.rng.random() < 0.6:
                    t.scale(x, (s - 1) % 16, 0.72, 0.8)
                seam_spots.append((x, (s - 1) % 16))
    boards = [((s + 1) % 16, (s + 3) % 16) for s in seams]
    # brown rot checks a board into little cubes: short cracks across the grain, a split along it
    for (b0, _) in t.rng.sample(boards, 2):
        x0 = t.rng.randrange(16)
        for i in range(t.rng.randrange(2, 4)):
            x = (x0 + i * 2) % 16
            for y in range(b0, b0 + 3):
                if t.rng.random() < 0.75:
                    t.scale(x, y % 16, 0.66, 0.85)
        y = (b0 + 1) % 16
        x1 = (x0 + t.rng.randrange(-2, 3)) % 16
        for k in range(t.rng.randrange(3, 6)):
            t.scale((x1 + k) % 16, y, 0.74, 0.85)
    # a soft spot gone dark with wet
    for _ in range(2):
        cx, cy = t.rng.randrange(16), t.rng.randrange(16)
        for dx in range(-1, 2):
            for dy in range(0, 2):
                if t.rng.random() < 0.8:
                    t.scale((cx + dx) % 16, (cy + dy) % 16, 0.86, 1.1)
    fungus(t, 2, seam_spots or None)
    return t.img


def rot_side(img, seed, inner=None):
    """
    A log's side, the grain running up it: split along the grain and checked across it. With inner (the stripped
    log's side) it is bark, fallen away in patches with the soft wood under it showing, and a small bracket fungus.
    """
    t = Tex(img, seed)
    dull(t, darken=0.86 if inner else 0.84)
    dark = crack_colour(t)
    splits = []
    for _ in range(3):
        # never two splits side by side at one height: on a lone block, such as a plains lamp post's head, a pair
        # reads as a face's eyes
        while True:
            x, y = t.rng.randrange(16), t.rng.randrange(16)
            if all(min((x - sx) % 16, (sx - x) % 16) <= 1 or min((y - sy) % 16, (sy - y) % 16) >= 6
                   for sx, sy in splits):
                break
        splits.append((x, y))
        for k in range(t.rng.randrange(3, 7)):
            t.paint(x, (y + k) % 16, dark if k % 3 else (dark[0] * 1.3, dark[1], dark[2]))
    for _ in range(2):
        x0, y0 = t.rng.randrange(16), t.rng.randrange(16)
        for i in range(3):
            for k in range(2):
                if t.rng.random() < 0.8:
                    t.scale((x0 + k) % 16, (y0 + i * 2) % 16, 0.68, 0.85)
    if inner is not None:
        ip = inner.load()
        for _ in range(2):
            cx, cy = t.rng.randrange(16), t.rng.randrange(16)
            w, h = t.rng.randrange(2, 4), t.rng.randrange(3, 6)
            for dy in range(-h // 2, h - h // 2):
                for dx in range(-w // 2, w - w // 2):
                    if abs(dx) + abs(dy) > (w + h) // 2 + 1 and t.rng.random() < 0.6:
                        continue
                    x, y = (cx + dx) % 16, (cy + dy) % 16
                    L, a, b = C.oklab(ip[x, y][:3])
                    t.put_lab(x, y, (L * 0.82 + 0.02, a * 0.5, b * 0.5))
            # the bark's broken edge above the patch
            for dx in range(-w // 2 - 1, w - w // 2 + 1):
                t.scale((cx + dx) % 16, (cy - h // 2 - 1) % 16, 0.7)
        # the bracket fungus: a pale shelf, its shadow under it
        x, y = t.rng.randrange(16), t.rng.randrange(3, 14)
        for k in range(3):
            t.paint((x + k) % 16, y, (0.70 - 0.05 * abs(k - 1), 0.01, 0.04))
        t.paint((x + 1) % 16, y - 1, (0.66, 0.01, 0.04))
        for k in range(3):
            t.scale((x + k) % 16, (y + 1) % 16, 0.6)
    fungus(t, 2)
    return t.img


def rot_end(img, seed):
    """A log's sawn end: the heart gone soft and dark, cracks running out from it, a ring come apart."""
    t = Tex(img, seed)
    dull(t, mottle=0.05)
    dark = crack_colour(t)
    cx, cy = 7.5 + t.rng.uniform(-0.8, 0.8), 7.5 + t.rng.uniform(-0.8, 0.8)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if r < 2.2:
                t.scale(x, y, 0.55 + 0.1 * r, 0.9)
            elif r < 3.2:
                t.scale(x, y, 0.85)
    for _ in range(3):
        ang = t.rng.uniform(0, 2 * math.pi)
        r, r1 = 2, t.rng.uniform(4, 6.5)
        while r < r1:
            t.paint(round(cx + math.cos(ang) * r), round(cy + math.sin(ang) * r), dark)
            r += 0.7
            ang += t.rng.uniform(-0.15, 0.15)
    rr, a0 = t.rng.uniform(4.2, 5.2), t.rng.uniform(0, 2 * math.pi)
    for i in range(10):
        ang = a0 + i * 0.12
        t.scale(round(cx + math.cos(ang) * rr), round(cy + math.sin(ang) * rr), 0.65)
    fungus(t, 1)
    return t.img


def board_cracks(t, n):
    """Short cracks down the boards of a door."""
    for _ in range(n):
        x, y = t.rng.randrange(t.W), t.rng.randrange(t.H)
        for k in range(t.rng.randrange(2, 5)):
            t.scale(x, (y + k) % t.H, 0.62, 0.85)


def rust_metal(t, src):
    """A door's iron (hinges, bands, a latch) gone to rust: the grey pixels of the sound door's texture."""
    sp = src.load()
    for y in range(t.H):
        for x in range(t.W):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            L, A, B = C.oklab((r, g, b))
            if math.hypot(A, B) < 0.018 and 0.35 < L < 0.86:
                k = 0.6 + 0.4 * t.rng.random()
                t.put_lab(x, y, (L * 0.72, 0.045 * k + 0.01, 0.055 * k + 0.01))


def chew(t, x0, x1, y_edge, step, depth_max):
    """Eats an edge away unevenly, from row y_edge inward (step -1 eats up from the foot), its new edge crumbled dark."""
    d = t.rng.randrange(1, depth_max + 1)
    for x in range(x0, x1):
        d = max(0, min(depth_max, d + t.rng.choice((-1, 0, 0, 1))))
        for k in range(d):
            t.hole(x, y_edge + k * step)
        t.scale(x, y_edge + d * step, 0.72)


def gap(t, x0, w, y0, h):
    """A board gone: a ragged hole, its broken ends uneven, its rim dark."""
    for x in range(x0, x0 + w):
        top, bottom = y0 + t.rng.randrange(0, 3), y0 + h - t.rng.randrange(0, 3)
        for y in range(top, bottom):
            t.hole(x, y)
        t.scale(x, top - 1, 0.7)
        t.scale(x, bottom, 0.7)
    for y in range(y0, y0 + h):
        t.scale(x0 - 1, y, 0.8)
        t.scale(x0 + w, y, 0.8)


def rot_door(img, seed, half):
    """A door's half: a board missing, and on the lower half the foot rotted away, where water stood."""
    t = Tex(img, seed)
    dull(t)
    rust_metal(t, img)
    board_cracks(t, 2)
    if half == 'bottom':
        x0 = t.rng.randrange(1, 5)
        chew(t, x0, x0 + t.rng.randrange(8, 12), 15, -1, 4)
        gap(t, t.rng.randrange(3, 10), 3, t.rng.randrange(2, 5), t.rng.randrange(5, 8))
    else:
        gap(t, t.rng.randrange(3, 10), 3, t.rng.randrange(3, 8), t.rng.randrange(5, 8))
    fungus(t, 1)
    return t.img


def rot_trapdoor(img, seed):
    t = Tex(img, seed)
    dull(t)
    rust_metal(t, img)
    board_cracks(t, 2)
    x0 = t.rng.randrange(0, 7)
    chew(t, x0, x0 + t.rng.randrange(5, 9), 15, -1, 3)
    gap(t, t.rng.randrange(2, 11), 3, t.rng.randrange(1, 6), t.rng.randrange(5, 8))
    fungus(t, 1)
    return t.img


def rot_door_item(img, seed):
    """The door's item, drawn small: a notch out of its foot and a crack."""
    t = Tex(img, seed)
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
    board_cracks(t, 1)
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
