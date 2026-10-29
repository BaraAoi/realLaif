"""Генератор пиксельных текстур мода (python3 tools/gen_textures.py)."""
import random
from PIL import Image

R = 'src/main/resources/assets/reallife/textures'
random.seed(42)


def img(w=16, h=16, c=(0, 0, 0, 0)):
    return Image.new('RGBA', (w, h), c)


def noise(im, base, var=12, box=None):
    x0, y0, x1, y1 = box or (0, 0, im.width, im.height)
    for x in range(x0, x1):
        for y in range(y0, y1):
            d = random.randint(-var, var)
            im.putpixel((x, y), tuple(max(0, min(255, v + d)) for v in base[:3]) + (255,))


def rect(im, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            im.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def outline(im, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        im.putpixel((x, y0), c + (255,)); im.putpixel((x, y1), c + (255,))
    for y in range(y0, y1 + 1):
        im.putpixel((x0, y), c + (255,)); im.putpixel((x1, y), c + (255,))


def pix(im, pts, c):
    for p in pts:
        im.putpixel(p, c + (255,))


GOLD = (230, 190, 50)
DGOLD = (170, 130, 20)

# ---------------- блоки
side = img(); noise(side, (140, 140, 145), 8)
outline(side, 0, 0, 15, 15, (100, 100, 105)); rect(side, 0, 0, 15, 1, GOLD); rect(side, 0, 1, 15, 1, DGOLD)
side.save(f'{R}/block/bank_counter_side.png')

top = img(); noise(top, (225, 222, 215), 6)
for i in range(16):
    if random.random() < 0.6: top.putpixel((i, (i * 7) % 16), (180, 180, 185, 255))
outline(top, 0, 0, 15, 15, DGOLD)
top.save(f'{R}/block/bank_counter_top.png')

front = side.copy()
rect(front, 3, 3, 12, 8, (150, 200, 230)); outline(front, 2, 2, 13, 9, (60, 60, 65))
pix(front, [(5, 4), (6, 4), (5, 5)], (220, 240, 255))
# символ рубля
rub = [(6, 10), (6, 11), (6, 12), (6, 13), (6, 14), (7, 10), (8, 10), (9, 11), (8, 12), (7, 12), (5, 13), (7, 13), (8, 13)]
pix(front, rub, GOLD)
front.save(f'{R}/block/bank_counter_front.png')


def planks(base=(120, 85, 50)):
    im = img(); noise(im, base, 10)
    for y in (3, 7, 11, 15):
        for x in range(16): im.putpixel((x, y), tuple(int(v * 0.65) for v in base) + (255,))
    for (x, y) in [(5, 0), (5, 1), (5, 2), (12, 4), (12, 5), (12, 6), (3, 8), (3, 9), (3, 10), (9, 12), (9, 13), (9, 14)]:
        im.putpixel((x, y), tuple(int(v * 0.65) for v in base) + (255,))
    return im


ms = planks((105, 75, 45)); ms.save(f'{R}/block/mfo_counter_side.png')
mt = planks((95, 70, 45))
for _ in range(6): mt.putpixel((random.randint(1, 14), random.randint(1, 14)), (60, 50, 40, 255))
mt.save(f'{R}/block/mfo_counter_top.png')
mf = planks((105, 75, 45))
rect(mf, 2, 2, 13, 13, (30, 30, 30)); outline(mf, 2, 2, 13, 13, (70, 70, 70))
GREEN = (60, 220, 80)
pct = [(5, 4), (6, 4), (5, 5), (6, 5), (10, 4), (10, 5), (9, 6), (9, 7), (8, 8), (7, 9), (6, 10), (6, 11), (9, 10), (10, 10), (9, 11), (10, 11)]
pix(mf, pct, GREEN)
mf.save(f'{R}/block/mfo_counter_front.png')

jw = img(); noise(jw, (55, 58, 64), 6)
for x in range(16): jw.putpixel((x, 7), (35, 37, 40, 255)); jw.putpixel((x, 15), (35, 37, 40, 255))
for y in range(0, 8): jw.putpixel((7, y), (35, 37, 40, 255))
for y in range(8, 16): jw.putpixel((15, y), (35, 37, 40, 255))
for p in [(1, 1), (5, 1), (9, 1), (13, 1), (1, 9), (5, 9), (9, 9), (13, 9), (1, 5), (13, 5)]:
    jw.putpixel(p, (120, 125, 130, 255))
jw.save(f'{R}/block/jail_wall.png')


# ---------------- предметы
def note(color, dark, marks, name):
    im = img()
    rect(im, 1, 4, 14, 11, color)
    outline(im, 1, 4, 14, 11, dark)
    rect(im, 3, 6, 5, 9, tuple(min(255, v + 40) for v in color))
    pix(im, marks, dark)
    pix(im, [(12, 5), (13, 10)], dark)
    im.save(f'{R}/item/{name}.png')


note((205, 150, 90), (120, 70, 30), [(8, 6), (8, 7), (8, 8), (8, 9), (10, 6), (11, 6), (10, 9), (11, 9), (10, 7), (10, 8), (11, 7), (11, 8)], 'banknote_10')
note((120, 190, 110), (40, 100, 40), [(7, 7), (7, 8), (9, 7), (9, 8), (11, 7), (11, 8), (8, 6), (10, 6), (12, 6)], 'banknote_100')
note((110, 170, 220), (30, 80, 140), [(7, 7), (8, 7), (9, 7), (10, 7), (11, 7), (12, 7), (7, 8), (12, 8)], 'banknote_1000')
note((230, 120, 80), (140, 40, 20), [(7, 6), (8, 6), (9, 6), (7, 7), (7, 8), (8, 8), (9, 8), (9, 9), (7, 9), (8, 9), (11, 7), (12, 8)], 'banknote_5000')


def paper():
    im = img()
    rect(im, 3, 1, 12, 14, (240, 236, 220)); outline(im, 3, 1, 12, 14, (170, 160, 140))
    for y in (3, 5, 7):
        for x in range(5, 11): im.putpixel((x, y), (120, 120, 130, 255))
    return im


bp = paper(); rect(bp, 8, 10, 11, 13, GOLD); outline(bp, 8, 10, 11, 13, DGOLD); bp.save(f'{R}/item/bank_permit.png')
mp = paper()
for _ in range(10): mp.putpixel((random.randint(4, 11), random.randint(2, 13)), (200, 190, 160, 255))
pix(mp, [(8, 10), (11, 10), (10, 11), (9, 12), (8, 13), (11, 13)], (40, 160, 60)); mp.save(f'{R}/item/mfo_permit.png')
ho = paper()
rect(ho, 6, 10, 10, 13, (200, 170, 120)); pix(ho, [(8, 8), (7, 9), (8, 9), (9, 9), (6, 10), (10, 10)], (190, 40, 40))
pix(ho, [(5, 10), (11, 10)], (190, 40, 40)); rect(ho, 8, 12, 8, 13, (100, 60, 30)); ho.save(f'{R}/item/house_order.png')

pk = img()
for i in range(10): pk.putpixel((3 + i, 12 - i), (110, 75, 40, 255)); pk.putpixel((4 + i, 12 - i), (80, 55, 30, 255))
head = [(6, 2), (7, 2), (8, 2), (9, 2), (10, 2), (11, 3), (12, 3), (13, 4), (13, 5), (14, 6), (14, 7), (5, 3), (4, 3), (3, 4), (11, 4), (12, 4), (12, 5), (13, 6), (10, 3), (9, 3), (8, 3)]
pix(pk, head, (170, 170, 175))
pix(pk, [(9, 3), (12, 5), (5, 3), (13, 6)], (170, 100, 50))
pk.save(f'{R}/item/debtor_pickaxe.png')


# ---------------- скины NPC (64x64, формат игрока)
def face_box(im, x, y, w, h, d, colors):
    """Раскладка куба: top, bottom, right, front, left, back."""
    top, bottom, right, front, left, back = colors
    rect(im, x + d, y, x + d + w - 1, y + d - 1, top)
    rect(im, x + d + w, y, x + d + 2 * w - 1, y + d - 1, bottom)
    rect(im, x, y + d, x + d - 1, y + d + h - 1, right)
    rect(im, x + d, y + d, x + d + w - 1, y + d + h - 1, front)
    rect(im, x + d + w, y + d, x + 2 * d + w - 1, y + d + h - 1, left)
    rect(im, x + 2 * d + w, y + d, x + 2 * d + 2 * w - 1, y + d + h - 1, back)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def skin(name, skin_c, hair, shirt, pants, shoes, hat=None, extra=None, eyes=(40, 40, 60), beard=None, sleeves_full=True):
    im = img(64, 64)
    # голова
    face_box(im, 0, 0, 8, 8, 8, [hair, skin_c, skin_c, skin_c, skin_c, hair])
    rect(im, 8, 8, 15, 9, hair)          # чёлка
    rect(im, 0, 8, 7, 10, hair)          # волосы сбоку
    rect(im, 16, 8, 23, 10, hair)
    rect(im, 24, 8, 31, 13, hair)        # затылок
    pix(im, [(10, 12), (13, 12)], eyes)
    pix(im, [(9, 12), (14, 12)], (240, 240, 240))
    rect(im, 11, 14, 12, 14, shade(skin_c, 0.7))
    if beard:
        rect(im, 9, 14, 14, 15, beard); rect(im, 11, 14, 12, 14, shade(skin_c, 0.6))
    # тело
    face_box(im, 16, 16, 8, 12, 4, [shirt, shirt, shade(shirt, 0.85), shirt, shade(shirt, 0.85), shade(shirt, 0.8)])
    # правая рука
    arm = shirt if sleeves_full else skin_c
    face_box(im, 40, 16, 4, 12, 4, [shirt, skin_c, shade(arm, 0.85), arm, shade(arm, 0.85), shade(arm, 0.8)])
    rect(im, 40, 20, 55, 23, shirt)
    rect(im, 40, 29, 55, 31, skin_c)
    # левая рука
    face_box(im, 32, 48, 4, 12, 4, [shirt, skin_c, shade(arm, 0.85), arm, shade(arm, 0.85), shade(arm, 0.8)])
    rect(im, 32, 52, 47, 55, shirt)
    rect(im, 32, 61, 47, 63, skin_c)
    # ноги
    face_box(im, 0, 16, 4, 12, 4, [pants, shoes, shade(pants, 0.85), pants, shade(pants, 0.85), shade(pants, 0.8)])
    rect(im, 0, 29, 15, 31, shoes)
    face_box(im, 16, 48, 4, 12, 4, [pants, shoes, shade(pants, 0.85), pants, shade(pants, 0.85), shade(pants, 0.8)])
    rect(im, 16, 61, 31, 63, shoes)
    if hat:
        # второй слой головы (шапка/фуражка)
        face_box(im, 32, 0, 8, 8, 8, [hat, (0, 0, 0, 0), hat, hat, hat, hat])
        for x in range(32, 64):
            for y in range(11, 16):
                im.putpixel((x, y), (0, 0, 0, 0))
        rect(im, 40, 0, 47, 7, hat)
    if extra:
        extra(im)
    im.save(f'{R}/entity/{name}.png')


SKIN = (225, 180, 145)


def zhora_extra(im):
    # белая рубашка и галстук
    rect(im, 23, 20, 24, 31, (180, 30, 40))
    rect(im, 20, 20, 22, 21, (245, 245, 245)); rect(im, 25, 20, 27, 21, (245, 245, 245))
    pix(im, [(9, 15), (14, 15)], shade(SKIN, 0.8))
    rect(im, 11, 15, 12, 15, (170, 80, 80))  # улыбка


skin('zhora', SKIN, (110, 70, 40), (45, 60, 110), (40, 40, 55), (25, 20, 20), extra=zhora_extra)


def alex_extra(im):
    rect(im, 20, 20, 27, 31, (25, 25, 25))
    rect(im, 23, 20, 24, 23, (200, 200, 200))
    pix(im, [(22, 24), (25, 24)], (180, 160, 40))
    rect(im, 9, 11, 14, 11, (20, 20, 20))  # нахмуренные брови


skin('alexander', (215, 170, 135), (30, 25, 20), (35, 35, 38), (30, 30, 35), (15, 15, 15), extra=alex_extra, beard=(40, 30, 25))


def police_extra(im):
    rect(im, 22, 22, 23, 23, (230, 200, 60))  # значок
    rect(im, 20, 27, 27, 27, (20, 20, 25))    # ремень
    rect(im, 40, 0, 47, 7, (25, 35, 80))
    rect(im, 40, 8, 47, 8, (20, 20, 20))      # козырёк
    rect(im, 42, 9, 45, 9, (220, 190, 40))    # кокарда


skin('police', SKIN, (60, 45, 30), (40, 60, 120), (30, 40, 80), (15, 15, 15), hat=(25, 35, 80), extra=police_extra)


def vovan_extra(im):
    # спортивный костюм с тремя полосками
    for x in (16, 19):
        pass
    for y in range(20, 32):
        for x in (41, 43, 45):
            pass
    for y in range(20, 32):
        im.putpixel((44, y), (240, 240, 240, 255)); im.putpixel((46, y), (240, 240, 240, 255))
        im.putpixel((36, y + 32), (240, 240, 240, 255)); im.putpixel((38, y + 32), (240, 240, 240, 255))
        im.putpixel((4, y), (240, 240, 240, 255)); im.putpixel((6, y), (240, 240, 240, 255))
        im.putpixel((20, y + 32), (240, 240, 240, 255)); im.putpixel((22, y + 32), (240, 240, 240, 255))
    rect(im, 23, 20, 24, 31, (200, 200, 200))  # молния


skin('vovan', SKIN, (90, 70, 50), (25, 25, 30), (25, 25, 30), (240, 240, 240), extra=vovan_extra, hat=(20, 20, 25))


def bratok_extra(im):
    rect(im, 20, 20, 27, 31, (60, 40, 30))
    rect(im, 22, 20, 25, 21, (240, 200, 40)); pix(im, [(21, 21), (26, 21), (23, 22), (24, 22)], (240, 200, 40))  # цепь
    rect(im, 9, 12, 14, 12, (20, 20, 20))  # тёмные очки
    rect(im, 0, 0, 31, 7, SKIN); rect(im, 8, 8, 15, 9, SKIN); rect(im, 0, 8, 7, 10, SKIN); rect(im, 16, 8, 23, 10, SKIN); rect(im, 24, 8, 31, 13, SKIN)
    rect(im, 9, 12, 14, 12, (20, 20, 20))
    pix(im, [(10, 12), (13, 12)], (60, 60, 70))


skin('bratok', (220, 175, 140), (40, 30, 25), (20, 20, 20), (30, 30, 30), (10, 10, 10), extra=bratok_extra)
print('ok')
