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

# ================= версия 2: новые блоки, предметы, скины =================
def metal(base, var=5):
    im = img(); noise(im, base, var); return im


# холодильник
fs = metal((225, 228, 232), 4); outline(fs, 0, 0, 15, 15, (175, 178, 185)); fs.save(f'{R}/block/fridge_side.png')
ft = metal((215, 218, 222), 4); outline(ft, 0, 0, 15, 15, (175, 178, 185)); ft.save(f'{R}/block/fridge_top.png')
ff = metal((235, 237, 240), 3); outline(ff, 0, 0, 15, 15, (170, 172, 180))
rect(ff, 1, 5, 14, 5, (165, 168, 175))              # щель между дверцами
rect(ff, 12, 1, 12, 3, (120, 125, 135)); rect(ff, 12, 7, 12, 12, (120, 125, 135))  # ручки
pix(ff, [(3, 2), (4, 2)], (90, 160, 220))           # индикатор
ff.save(f'{R}/block/fridge_front.png')

# банкомат
asd = metal((70, 75, 85), 5); outline(asd, 0, 0, 15, 15, (45, 48, 55)); rect(asd, 0, 0, 15, 1, GOLD); asd.save(f'{R}/block/atm_side.png')
atp = metal((60, 64, 72), 4); outline(atp, 0, 0, 15, 15, (45, 48, 55)); atp.save(f'{R}/block/atm_top.png')
af = metal((75, 80, 92), 4); outline(af, 0, 0, 15, 15, (45, 48, 55)); rect(af, 0, 0, 15, 1, GOLD)
rect(af, 3, 3, 12, 7, (30, 90, 60)); outline(af, 2, 2, 13, 8, (35, 35, 40))      # экран
pix(af, [(4, 4), (5, 4), (6, 4), (8, 4), (9, 4), (4, 6), (5, 6), (6, 6), (7, 6)], (120, 240, 160))
for yy in (10, 12):
    for xx in (3, 5, 7):
        pix(af, [(xx, yy)], (200, 200, 205))       # клавиши
rect(af, 10, 10, 13, 10, (20, 20, 20))               # картоприёмник
rect(af, 9, 13, 13, 13, (15, 15, 15))                # купюроприёмник
af.save(f'{R}/block/atm_front.png')

# торговые автоматы
vs = metal((160, 30, 35), 6); outline(vs, 0, 0, 15, 15, (110, 20, 25)); vs.save(f'{R}/block/vending_side.png')
vt = metal((140, 25, 30), 5); outline(vt, 0, 0, 15, 15, (110, 20, 25)); vt.save(f'{R}/block/vending_top.png')


def vending(name, colors):
    im = metal((165, 32, 38), 5); outline(im, 0, 0, 15, 15, (110, 20, 25))
    rect(im, 1, 1, 10, 13, (190, 220, 235))          # витрина
    for row in range(3):
        rect(im, 1, 4 + row * 4, 10, 4 + row * 4, (120, 120, 130))
        for k in range(3):
            c = colors[(row * 3 + k) % len(colors)]
            rect(im, 2 + k * 3, 2 + row * 4, 3 + k * 3, 3 + row * 4, c)
    rect(im, 12, 2, 14, 4, (40, 40, 45)); pix(im, [(13, 3)], (120, 240, 160))   # табло
    for yy in (6, 8, 10):
        pix(im, [(12, yy), (14, yy)], (220, 220, 220))
    rect(im, 2, 14, 9, 14, (20, 20, 20))              # лоток
    im.save(f'{R}/block/{name}.png')


vending('vending_food_front', [(220, 160, 60), (200, 60, 50), (240, 220, 120), (120, 70, 40), (90, 170, 60)])
vending('vending_resources_front', [(130, 130, 130), (150, 110, 70), (220, 220, 230), (60, 60, 60), (200, 120, 60)])

# печатный станок
ps = metal((90, 95, 100), 6); outline(ps, 0, 0, 15, 15, (55, 58, 62)); rect(ps, 2, 6, 13, 9, (60, 62, 66)); ps.save(f'{R}/block/money_printer_side.png')
pt = metal((80, 85, 90), 5); outline(pt, 0, 0, 15, 15, (55, 58, 62)); rect(pt, 3, 3, 12, 12, (130, 175, 120)); pt.save(f'{R}/block/money_printer_top.png')
pf = metal((95, 100, 105), 5); outline(pf, 0, 0, 15, 15, (55, 58, 62))
rect(pf, 2, 2, 13, 4, (40, 40, 45)); pix(pf, [(3, 3), (5, 3)], (240, 60, 60)); pix(pf, [(7, 3)], (60, 240, 90))
rect(pf, 2, 8, 13, 11, (30, 30, 32)); rect(pf, 3, 9, 12, 10, (120, 170, 110))  # купюры выходят
pix(pf, [(6, 9), (7, 9), (8, 10)], (70, 120, 70))
rect(pf, 1, 13, 14, 14, (60, 62, 66))
pf.save(f'{R}/block/money_printer_front.png')

# грязные деньги — мятая пачка, перетянутая резинкой
dm = img()
rect(dm, 2, 4, 13, 11, (95, 125, 80)); outline(dm, 2, 4, 13, 11, (55, 75, 45))
rect(dm, 3, 3, 14, 10, (110, 140, 90)); outline(dm, 3, 3, 14, 10, (60, 85, 50))
rect(dm, 7, 3, 8, 10, (190, 60, 50))
pix(dm, [(4, 5), (10, 8), (12, 4), (5, 9)], (70, 90, 55))
pix(dm, [(11, 6), (12, 6), (11, 7)], (40, 55, 35))
dm.save(f'{R}/item/dirty_money.png')


def seller_extra(im):
    rect(im, 20, 20, 27, 31, (245, 245, 245))     # фартук
    rect(im, 20, 20, 27, 20, (30, 120, 60))
    rect(im, 22, 23, 25, 24, (30, 120, 60))
    rect(im, 11, 15, 12, 15, (170, 80, 80))


skin('seller', SKIN, (150, 90, 40), (30, 120, 60), (50, 50, 60), (30, 25, 20), extra=seller_extra, hat=(30, 120, 60))


def inspector_extra(im):
    rect(im, 20, 20, 27, 31, (200, 200, 205))
    rect(im, 23, 20, 24, 27, (30, 50, 90))        # галстук
    rect(im, 20, 22, 21, 23, (240, 240, 120))     # бейдж
    rect(im, 40, 0, 47, 7, (240, 200, 30))        # каска
    rect(im, 9, 11, 14, 11, (60, 60, 60))
    rect(im, 9, 12, 10, 12, (30, 30, 30)); rect(im, 13, 12, 14, 12, (30, 30, 30))  # очки


skin('inspector', SKIN, (90, 90, 95), (90, 95, 110), (50, 55, 70), (20, 20, 20), extra=inspector_extra, hat=(240, 200, 30))
# ================= версия 3: казино, приёмник, ООО, кассир, амбал =================
# приёмник ямы — деревянный короб с воронкой
rs = planks((110, 80, 50)); outline(rs, 0, 0, 15, 15, (60, 45, 25)); rs.save(f'{R}/block/pit_receiver_side.png')
rt = planks((110, 80, 50)); rect(rt, 3, 3, 12, 12, (40, 40, 42)); outline(rt, 2, 2, 13, 13, (90, 90, 95))
rect(rt, 6, 6, 9, 9, (15, 15, 15)); rt.save(f'{R}/block/pit_receiver_top.png')
rf = planks((110, 80, 50)); outline(rf, 0, 0, 15, 15, (60, 45, 25))
rect(rf, 3, 3, 12, 6, (230, 225, 210)); pix(rf, [(5, 4), (6, 4), (7, 4), (9, 4), (10, 4), (5, 5), (9, 5)], (150, 30, 30))   # табличка «₽»
rect(rf, 4, 9, 11, 12, (25, 25, 25)); rf.save(f'{R}/block/pit_receiver_front.png')

# ООО «Отмыв денег» — стиральная машина
ls = metal((230, 232, 236), 4); outline(ls, 0, 0, 15, 15, (170, 172, 180)); ls.save(f'{R}/block/launder_machine_side.png')
lt = metal((220, 222, 228), 4); outline(lt, 0, 0, 15, 15, (170, 172, 180)); rect(lt, 2, 2, 13, 4, (90, 160, 220)); lt.save(f'{R}/block/launder_machine_top.png')
lf = metal((238, 240, 244), 3); outline(lf, 0, 0, 15, 15, (170, 172, 180))
rect(lf, 1, 1, 14, 3, (70, 75, 85)); pix(lf, [(3, 2), (5, 2)], (60, 240, 90)); pix(lf, [(12, 2)], (240, 200, 60))
for x in range(16):
    for y in range(16):
        d = ((x - 7.5) ** 2 + (y - 9.5) ** 2) ** 0.5
        if d <= 5.2:
            lf.putpixel((x, y), (60, 65, 75, 255) if d > 4.2 else ((120, 170, 110, 255) if (x + y) % 3 else (90, 140, 210, 255)))
lf.save(f'{R}/block/launder_machine_front.png')

# казино — общий корпус
CRED = (130, 20, 30)
cs = metal(CRED, 6); outline(cs, 0, 0, 15, 15, DGOLD); rect(cs, 0, 0, 15, 0, GOLD); cs.save(f'{R}/block/casino_side.png')
ct = metal((30, 100, 50), 6); outline(ct, 0, 0, 15, 15, DGOLD); ct.save(f'{R}/block/casino_top.png')

# орёл/решка — стол с монетой
cf = metal(CRED, 6); outline(cf, 0, 0, 15, 15, DGOLD)
rect(cf, 1, 1, 14, 5, (30, 100, 50))
for x in range(16):
    for y in range(16):
        d = ((x - 7.5) ** 2 + (y - 10) ** 2) ** 0.5
        if d <= 4: cf.putpixel((x, y), (GOLD if d <= 3 else DGOLD) + (255,))
pix(cf, [(7, 9), (8, 9), (7, 10), (8, 11)], DGOLD)
cf.save(f'{R}/block/coin_table_front.png')
cto = metal((30, 100, 50), 6); outline(cto, 0, 0, 15, 15, DGOLD)
for x in range(16):
    for y in range(16):
        if ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 <= 3: cto.putpixel((x, y), GOLD + (255,))
cto.save(f'{R}/block/coin_table_top.png')

# однорукий бандит
sf = metal(CRED, 5); outline(sf, 0, 0, 15, 15, DGOLD); rect(sf, 0, 0, 15, 1, GOLD)
rect(sf, 1, 4, 12, 9, (245, 245, 240)); outline(sf, 1, 4, 12, 9, (40, 40, 40))
rect(sf, 4, 4, 4, 9, (40, 40, 40)); rect(sf, 8, 4, 8, 9, (40, 40, 40))
for i, c in enumerate([(220, 30, 30), (220, 30, 30), (220, 30, 30)]):
    pix(sf, [(2 + i * 4, 6), (3 + i * 4, 6), (3 + i * 4, 7), (2 + i * 4, 8)], c)    # 7 7 7
rect(sf, 14, 3, 14, 8, (180, 180, 185)); rect(sf, 13, 2, 15, 3, (220, 30, 30))   # рычаг
rect(sf, 3, 12, 10, 13, (20, 20, 20)); pix(sf, [(2, 2), (5, 2), (8, 2), (11, 2)], (255, 230, 120))
sf.save(f'{R}/block/slot_machine_front.png')

# Мама Анархия — фиолетовый автомат с черепом и короной
af3 = metal((70, 20, 90), 6); outline(af3, 0, 0, 15, 15, (200, 60, 200))
rect(af3, 2, 2, 13, 11, (20, 10, 25)); outline(af3, 2, 2, 13, 11, (200, 60, 200))
pix(af3, [(6, 3), (8, 3), (10, 3), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4)], GOLD)            # корона
rect(af3, 6, 5, 10, 8, (235, 235, 235)); pix(af3, [(7, 6), (9, 6)], (20, 10, 25)); rect(af3, 7, 9, 9, 9, (235, 235, 235))
pix(af3, [(8, 9)], (20, 10, 25))
rect(af3, 3, 13, 12, 14, (230, 40, 40))
af3.save(f'{R}/block/anarchy_machine_front.png')

# рулетка
rfr = metal(CRED, 6); outline(rfr, 0, 0, 15, 15, DGOLD); rect(rfr, 1, 1, 14, 6, (30, 100, 50))
for k in range(7):
    pix(rfr, [(1 + k * 2, 3)], (220, 30, 30) if k % 2 else (20, 20, 20))
rfr.save(f'{R}/block/roulette_table_front.png')
rto = metal((30, 100, 50), 6); outline(rto, 0, 0, 15, 15, DGOLD)
import math
for x in range(16):
    for y in range(16):
        dx, dy = x - 7.5, y - 7.5
        d = (dx * dx + dy * dy) ** 0.5
        if d <= 7:
            if d > 5.5: c = (110, 70, 35)
            elif d > 2:
                seg = int((math.atan2(dy, dx) + math.pi) / (2 * math.pi) * 12)
                c = (30, 140, 60) if seg == 0 else ((200, 30, 30) if seg % 2 else (25, 25, 25))
            elif d > 1: c = GOLD
            else: c = (240, 240, 240)
            rto.putpixel((x, y), c + (255,))
rto.save(f'{R}/block/roulette_table_top.png')

# клевер абсолютной удачи
cl = img()
LEAF, DLEAF = (60, 190, 70), (30, 120, 40)
for cx, cy in [(5, 4), (10, 4), (5, 9), (10, 9)]:
    for x in range(cx - 2, cx + 3):
        for y in range(cy - 2, cy + 3):
            if abs(x - cx) + abs(y - cy) <= 3: cl.putpixel((x, y), LEAF + (255,))
    pix(cl, [(cx, cy)], DLEAF)
rect(cl, 7, 6, 8, 7, DLEAF)
pix(cl, [(8, 11), (8, 12), (9, 13), (9, 14), (10, 15)], DLEAF)
pix(cl, [(4, 3), (9, 3)], (170, 240, 170))
cl.save(f'{R}/item/clover.png')

# лицензия казино
cp = paper()
rect(cp, 5, 7, 10, 12, (130, 20, 30)); outline(cp, 5, 7, 10, 12, DGOLD)
pix(cp, [(7, 9), (8, 9), (7, 10), (8, 10)], GOLD)
cp.save(f'{R}/item/casino_permit.png')


def cashier_extra(im):
    rect(im, 20, 20, 27, 31, (245, 245, 245))     # блузка
    rect(im, 20, 20, 27, 21, (200, 40, 50))       # шейный платок
    rect(im, 25, 23, 26, 24, (230, 190, 50))      # бейдж банка
    rect(im, 11, 15, 12, 15, (190, 70, 80))
    rect(im, 0, 8, 7, 14, (120, 60, 30)); rect(im, 16, 8, 23, 14, (120, 60, 30))  # длинные волосы
    rect(im, 24, 8, 31, 15, (120, 60, 30))


skin('cashier', SKIN, (120, 60, 30), (35, 45, 90), (35, 45, 90), (20, 15, 15), extra=cashier_extra)


def ambal_extra(im):
    rect(im, 9, 11, 14, 12, (10, 10, 10))           # тёмные очки
    rect(im, 20, 20, 27, 31, (245, 245, 245)); rect(im, 23, 20, 24, 31, (10, 10, 10))
    rect(im, 8, 8, 15, 8, shade(SKIN, 0.85))        # лысина
    rect(im, 40, 20, 41, 23, (20, 20, 20))
    pix(im, [(16, 12), (16, 13)], (60, 60, 60))     # наушник охраны


skin('ambal', shade(SKIN, 0.95), shade(SKIN, 0.85), (15, 15, 18), (15, 15, 18), (5, 5, 5), extra=ambal_extra)

# ---------------- v4: сундук цеха и случайные гости
import os
os.makedirs(f'{R}/entity/visitor', exist_ok=True)

bc_side = planks((70, 50, 35)); outline(bc_side, 0, 0, 15, 15, (40, 30, 20))
for y in (5, 10): rect(bc_side, 0, y, 15, y, (90, 90, 95))
bc_side.save(f'{R}/block/business_chest_side.png')
bc_top = planks((80, 58, 40)); outline(bc_top, 0, 0, 15, 15, (40, 30, 20))
rect(bc_top, 6, 6, 9, 9, (90, 90, 95))
bc_top.save(f'{R}/block/business_chest_top.png')
bc_front = bc_side.copy()
rect(bc_front, 6, 6, 9, 10, (60, 60, 65)); rect(bc_front, 7, 7, 8, 8, (200, 170, 40))
pix(bc_front, [(2, 2), (3, 2), (2, 3), (12, 12), (13, 12), (13, 13)], (60, 140, 60))   # торчат купюры
bc_front.save(f'{R}/block/business_chest_front.png')


def vis(name, *args, **kw):
    skin('visitor/' + name, *args, **kw)


def tie(c):
    def f(im):
        rect(im, 20, 20, 27, 31, (240, 240, 240)); rect(im, 23, 20, 24, 30, c)
    return f


def glasses(im):
    rect(im, 9, 11, 14, 12, (20, 20, 20))


def combo(*fs):
    def f(im):
        for g in fs: g(im)
    return f


def stripes(im):
    for y in range(20, 32, 2): rect(im, 20, y, 27, y, (240, 240, 240))
    for y in range(20, 32, 2): rect(im, 44, y, 47, y, (240, 240, 240))


def apron(c):
    def f(im):
        rect(im, 21, 22, 26, 31, c)
    return f


def badge(c):
    def f(im):
        rect(im, 25, 22, 26, 23, c)
    return f


def mustache(im):
    rect(im, 10, 14, 13, 14, (60, 40, 25))


def stubble(im):
    for p in [(9, 14), (11, 15), (13, 14), (14, 15), (10, 15), (12, 14)]: im.putpixel(p, (120, 95, 80, 255))


def scarf(c):
    def f(im):
        rect(im, 0, 8, 7, 15, c); rect(im, 16, 8, 23, 15, c); rect(im, 24, 8, 31, 15, c); rect(im, 8, 8, 15, 9, c)
        rect(im, 40, 0, 47, 7, c)
    return f


def gray_hair(im):
    rect(im, 8, 8, 15, 8, (200, 200, 200)); rect(im, 0, 8, 7, 9, (200, 200, 200)); rect(im, 16, 8, 23, 9, (200, 200, 200))


def white_beard(im):
    rect(im, 9, 13, 14, 15, (230, 230, 230)); rect(im, 11, 14, 12, 14, shade(SKIN, 0.6))


def helmet_stripe(im):
    rect(im, 32, 13, 63, 13, (230, 230, 60)) if False else None
    rect(im, 20, 28, 27, 28, (230, 230, 60)); rect(im, 44, 28, 47, 28, (230, 230, 60))


def yellow_box(im):
    rect(im, 32, 36, 39, 47, (250, 200, 0)) if False else None
    rect(im, 28, 20, 31, 31, (250, 200, 0))


vis('tax', SKIN, (60, 50, 40), (50, 55, 70), (45, 48, 60), (20, 20, 20), extra=combo(tie((30, 40, 120)), glasses, badge((230, 190, 50))))
vis('tax_police', SKIN, (60, 45, 30), (40, 60, 120), (30, 40, 80), (15, 15, 15), hat=(25, 35, 80), extra=badge((230, 190, 50)))
vis('bailiff', SKIN, (40, 35, 30), (20, 30, 60), (20, 30, 60), (10, 10, 10), hat=(20, 30, 60), extra=combo(badge((200, 200, 210)), stubble))
vis('plumber', SKIN, (100, 70, 40), (40, 90, 170), (40, 90, 170), (60, 40, 25), hat=(200, 60, 40), extra=combo(mustache, apron((30, 70, 140))))
vis('electric', SKIN, (120, 80, 50), (240, 140, 20), (50, 50, 60), (40, 30, 20), hat=(250, 210, 30), extra=helmet_stripe)
vis('gypsy', (200, 150, 110), (20, 15, 15), (180, 30, 90), (120, 20, 60), (60, 30, 20), extra=combo(scarf((200, 160, 30)), badge((250, 210, 60))))
vis('gopnik', (220, 180, 150), (70, 50, 35), (25, 25, 30), (25, 25, 30), (240, 240, 240), hat=(30, 30, 35), extra=combo(stripes, stubble))
vis('gopnik_friend', (215, 175, 145), (90, 60, 40), (30, 30, 70), (30, 30, 70), (240, 240, 240), extra=combo(stripes, stubble))
vis('dealer', (205, 165, 135), (30, 25, 20), (60, 40, 30), (40, 30, 25), (20, 15, 10), hat=(40, 40, 40), extra=combo(glasses, stubble))
vis('horse_thief', (200, 160, 120), (50, 35, 25), (110, 80, 50), (70, 55, 40), (50, 35, 20), hat=(80, 60, 40), extra=combo(mustache, scarf((150, 30, 30))))
vis('courier', SKIN, (90, 60, 35), (250, 200, 0), (40, 40, 45), (20, 20, 20), hat=(250, 200, 0), extra=yellow_box)
vis('realtor', SKIN, (160, 110, 60), (230, 230, 235), (30, 30, 40), (90, 50, 30), extra=tie((200, 30, 40)))
vis('granny', (230, 190, 165), (200, 200, 200), (140, 60, 90), (90, 50, 70), (50, 35, 30), extra=combo(scarf((220, 80, 60)), apron((240, 240, 240))))
vis('fire_inspector', SKIN, (60, 45, 30), (150, 30, 30), (40, 40, 45), (15, 15, 15), hat=(150, 30, 30), extra=badge((230, 190, 50)))
vis('fireman', SKIN, (60, 45, 30), (60, 50, 40), (60, 50, 40), (15, 15, 15), hat=(220, 40, 30), extra=helmet_stripe)
vis('neighbor', (225, 170, 140), (120, 90, 60), (40, 110, 60), (60, 60, 120), (60, 40, 20), extra=combo(stubble, mustache))
vis('promoter', SKIN, (230, 200, 60), (40, 200, 70), (40, 40, 45), (240, 240, 240), hat=(40, 200, 70), extra=badge((255, 255, 255)))
vis('grandpa', (220, 180, 150), (190, 190, 190), (90, 80, 60), (70, 60, 50), (40, 30, 20), hat=(70, 70, 75), extra=combo(gray_hair, white_beard))
vis('philosopher', SKIN, (25, 20, 15), (20, 20, 22), (20, 20, 22), (10, 10, 10), extra=combo(glasses, tie((90, 20, 20))), beard=(40, 30, 25))
vis('bank_scammer', SKIN, (60, 50, 40), (30, 30, 35), (30, 30, 35), (10, 10, 10), extra=combo(tie((30, 120, 200)), glasses))
vis('postman', SKIN, (110, 75, 45), (30, 60, 140), (30, 60, 140), (30, 25, 20), hat=(30, 60, 140), extra=badge((250, 250, 250)))
vis('sociologist', SKIN, (150, 100, 60), (230, 230, 230), (100, 100, 110), (60, 40, 30), extra=combo(glasses, badge((40, 120, 220))))


# ---------------- Round 5: подозрительный тип, ОБЭП, работники, крипта, телефон, касса, 500₽
def hood(c):
    def f(im):
        rect(im, 0, 8, 7, 15, c); rect(im, 16, 8, 23, 15, c); rect(im, 24, 8, 31, 15, c); rect(im, 8, 8, 15, 10, c)
        rect(im, 32, 0, 63, 15, (0, 0, 0, 0)); rect(im, 40, 0, 47, 7, c)
    return f


def obep_vest(im):
    rect(im, 20, 20, 27, 31, (30, 30, 35))
    rect(im, 21, 24, 26, 25, (240, 240, 240))
    rect(im, 32, 20, 39, 31, (30, 30, 35))
    rect(im, 33, 24, 38, 25, (240, 240, 240))


vis('snoop', (210, 170, 140), (50, 40, 30), (70, 70, 75), (40, 40, 50), (20, 20, 20), extra=combo(hood((55, 55, 60)), glasses, stubble))
vis('obep', SKIN, (40, 30, 25), (50, 55, 60), (35, 40, 50), (10, 10, 10), hat=(25, 25, 30), extra=combo(obep_vest, badge((230, 190, 50))))


def worker(name, *args, **kw):
    skin('worker/' + name, *args, **kw)


worker('shawarma', (200, 150, 110), (20, 15, 10), (240, 240, 240), (60, 60, 70), (40, 30, 20), hat=(240, 240, 240), extra=combo(apron((200, 60, 40)), mustache))
worker('minimarket', (230, 190, 165), (170, 90, 40), (60, 140, 200), (50, 50, 70), (60, 40, 30), extra=combo(apron((40, 160, 80)), badge((250, 250, 250))))
worker('car_wash', SKIN, (40, 30, 20), (30, 80, 170), (30, 80, 170), (230, 200, 30), hat=(30, 80, 170), extra=helmet_stripe)
worker('quarry', SKIN, (90, 70, 50), (230, 120, 20), (60, 60, 70), (60, 40, 20), hat=(250, 210, 30), extra=combo(helmet_stripe, stubble))
worker('pawnshop', SKIN, (30, 25, 20), (60, 30, 30), (30, 30, 35), (10, 10, 10), extra=combo(tie((200, 170, 40)), glasses, mustache))
worker('gas_station', SKIN, (110, 75, 45), (240, 200, 20), (40, 40, 50), (20, 20, 20), hat=(230, 40, 30), extra=badge((230, 40, 30)))
worker('electronics', SKIN, (150, 100, 60), (230, 100, 20), (40, 40, 50), (20, 20, 20), extra=combo(badge((250, 250, 250)), tie((40, 40, 45))))

note((200, 120, 200), (110, 40, 110), [(7, 6), (8, 6), (7, 7), (8, 8), (7, 9), (8, 9), (10, 7), (10, 8), (11, 6), (11, 9), (12, 7), (12, 8)], 'banknote_500')

PCB = (30, 110, 60)


def chip(base=(30, 30, 35)):
    im = img()
    return im


mb = img(); rect(mb, 1, 1, 14, 14, PCB); outline(mb, 1, 1, 14, 14, (20, 70, 40))
rect(mb, 4, 4, 7, 7, (190, 190, 195)); rect(mb, 9, 3, 12, 4, (40, 40, 45)); rect(mb, 9, 6, 12, 7, (40, 40, 45))
rect(mb, 3, 10, 12, 11, (30, 30, 35)); pix(mb, [(2, 2), (13, 13), (13, 2), (2, 13)], GOLD)
mb.save(f'{R}/item/motherboard.png')

cpu = img(); rect(cpu, 3, 3, 12, 12, (160, 160, 165)); outline(cpu, 3, 3, 12, 12, (90, 90, 95)); rect(cpu, 5, 5, 10, 10, (200, 200, 205))
for i in range(4, 12, 2):
    pix(cpu, [(i, 2), (i, 13), (2, i), (13, i)], GOLD)
pix(cpu, [(6, 7), (7, 7), (8, 7), (9, 7)], (60, 60, 70)); cpu.save(f'{R}/item/cpu.png')

ram = img(); rect(ram, 1, 5, 14, 10, PCB); outline(ram, 1, 5, 14, 10, (20, 70, 40))
for x in (2, 5, 8, 11): rect(ram, x, 6, x + 1, 8, (30, 30, 35))
for x in range(2, 14, 2): ram.putpixel((x, 10), GOLD + (255,))
ram.save(f'{R}/item/ram.png')

psu = img(); rect(psu, 2, 3, 13, 12, (50, 50, 55)); outline(psu, 2, 3, 13, 12, (25, 25, 28))
rect(psu, 4, 5, 9, 10, (80, 80, 85)); outline(psu, 4, 5, 9, 10, (30, 30, 35)); pix(psu, [(6, 7), (7, 8), (6, 8), (7, 7)], (120, 120, 125))
pix(psu, [(11, 5), (11, 7)], (230, 200, 30)); pix(psu, [(11, 9)], (220, 40, 30)); psu.save(f'{R}/item/psu.png')

cool = img(); rect(cool, 2, 2, 13, 13, (60, 60, 65)); outline(cool, 2, 2, 13, 13, (30, 30, 35))
for (x, y) in [(7, 4), (8, 4), (10, 6), (10, 7), (8, 10), (7, 10), (4, 8), (4, 7), (6, 5), (9, 5), (9, 9), (6, 9)]:
    cool.putpixel((x, y), (120, 190, 230, 255))
rect(cool, 7, 7, 8, 8, (180, 180, 185)); cool.save(f'{R}/item/cooler.png')

gpu = img(); rect(gpu, 1, 4, 14, 11, (35, 35, 40)); outline(gpu, 1, 4, 14, 11, (15, 15, 18))
for cx in (4, 10):
    rect(gpu, cx - 2, 6, cx + 1, 9, (70, 70, 80)); pix(gpu, [(cx - 1, 7), (cx, 8)], (160, 160, 170))
rect(gpu, 2, 12, 12, 12, GOLD); rect(gpu, 1, 3, 14, 3, (120, 200, 90)); gpu.save(f'{R}/item/gpu.png')

ph = img(); rect(ph, 4, 1, 11, 14, (25, 25, 30)); outline(ph, 4, 1, 11, 14, (70, 70, 80))
rect(ph, 5, 3, 10, 11, (60, 130, 210)); pix(ph, [(6, 4), (7, 4), (8, 4)], (240, 200, 60)); rect(ph, 6, 6, 9, 6, (230, 230, 240))
rect(ph, 6, 8, 8, 8, (230, 230, 240)); pix(ph, [(7, 13), (8, 13)], (120, 120, 130)); ph.save(f'{R}/item/phone.png')

bz = paper(); rect(bz, 7, 9, 11, 13, (60, 160, 70)); outline(bz, 7, 9, 11, 13, (30, 100, 40)); pix(bz, [(9, 10), (9, 11), (9, 12)], (240, 240, 200))
bz.save(f'{R}/item/biz_permit.png')

# майнинг-ферма
rs = metal((45, 45, 50), 5); outline(rs, 0, 0, 15, 15, (20, 20, 22))
for y in range(3, 13, 3): rect(rs, 2, y, 13, y, (25, 25, 28))
rs.save(f'{R}/block/crypto_rig_side.png')
rt = metal((40, 40, 45), 4); outline(rt, 0, 0, 15, 15, (20, 20, 22))
for x in range(3, 13, 2): rect(rt, x, 3, x, 12, (25, 25, 28))
rt.save(f'{R}/block/crypto_rig_top.png')
rfz = metal((40, 40, 45), 4); outline(rfz, 0, 0, 15, 15, (20, 20, 22))
for i, cy in enumerate((3, 8)):
    rect(rfz, 2, cy, 13, cy + 3, (25, 25, 30)); pix(rfz, [(4, cy + 1), (7, cy + 1), (10, cy + 1)], (80, 220, 120) if i == 0 else (220, 80, 200))
rect(rfz, 2, 13, 13, 13, (80, 220, 120)); rfz.save(f'{R}/block/crypto_rig_front.png')

# касса заведения
ks = planks((150, 110, 70)); outline(ks, 0, 0, 15, 15, (90, 65, 40)); ks.save(f'{R}/block/biz_counter_side.png')
kt = planks((170, 130, 85)); outline(kt, 0, 0, 15, 15, (90, 65, 40)); rect(kt, 9, 3, 13, 7, (40, 40, 45)); kt.save(f'{R}/block/biz_counter_top.png')
kf = planks((150, 110, 70)); outline(kf, 0, 0, 15, 15, (90, 65, 40)); rect(kf, 3, 3, 12, 8, (40, 40, 45)); rect(kf, 4, 4, 11, 7, (90, 200, 120))
pix(kf, [(5, 5), (6, 5), (8, 6), (9, 6)], (20, 60, 30)); rect(kf, 3, 11, 12, 12, (60, 160, 70)); kf.save(f'{R}/block/biz_counter_front.png')


# ---------------- Round 6: оборудование заведений и сырьё
WOOD = (150, 110, 70)
GOODS = [(200, 60, 50), (60, 140, 200), (240, 200, 60), (80, 170, 80), (220, 120, 40), (170, 80, 170), (230, 230, 230)]


def goods_rows(im, rows, x0, x1, level, step=3):
    """Заполнить полки товаром: level 0..3 — сколько рядов занято."""
    rnd = random.Random(7)
    for ri, (ytop, ybot) in enumerate(rows):
        if ri >= level:
            continue
        x = x0
        while x <= x1 - 1:
            c = GOODS[rnd.randrange(len(GOODS))]
            h = rnd.randint(1, ybot - ytop)
            rect(im, x, ybot - h + 1, min(x + 1, x1), ybot, c)
            x += step


# торговая полка (дерево)
ss = planks(WOOD); outline(ss, 0, 0, 15, 15, (90, 65, 40)); ss.save(f'{R}/block/shop_shelf_side.png')
st_ = planks((170, 130, 85)); outline(st_, 0, 0, 15, 15, (90, 65, 40)); st_.save(f'{R}/block/shop_shelf_top.png')
for lv in range(4):
    im = img(); noise(im, (70, 50, 30), 4)
    outline(im, 0, 0, 15, 15, (110, 80, 50))
    for y in (5, 10, 15):
        rect(im, 1, y, 14, y, (160, 120, 75))
    rect(im, 0, 0, 0, 15, (130, 95, 60)); rect(im, 15, 0, 15, 15, (130, 95, 60))
    goods_rows(im, [(1, 4), (6, 9), (11, 14)], 2, 13, lv)
    im.save(f'{R}/block/shop_shelf_front_{lv}.png')

# складской стеллаж (металл)
rs_ = metal((110, 115, 120), 5); outline(rs_, 0, 0, 15, 15, (60, 65, 70))
for y in (5, 10): rect(rs_, 1, y, 14, y, (200, 120, 40))
rs_.save(f'{R}/block/storage_rack_side.png')
rt_ = metal((100, 105, 110), 4); outline(rt_, 0, 0, 15, 15, (60, 65, 70)); rt_.save(f'{R}/block/storage_rack_top.png')
for lv in range(4):
    im = img(); noise(im, (35, 38, 42), 3)
    rect(im, 0, 0, 1, 15, (60, 90, 160)); rect(im, 14, 0, 15, 15, (60, 90, 160))
    for y in (0, 7, 14, 15):
        rect(im, 0, y, 15, y, (220, 130, 40))
    rnd = random.Random(11)
    boxes = [(2, 1, 6, 6), (8, 2, 13, 6), (2, 8, 7, 13), (9, 9, 13, 13)]
    for i, (a, b, c, d) in enumerate(boxes):
        if i < [0, 1, 3, 4][lv]:
            rect(im, a, b, c, d, (190, 150, 95)); outline(im, a, b, c, d, (130, 95, 55)); rect(im, a, b + (d - b) // 2, c, b + (d - b) // 2, (215, 190, 120))
    im.save(f'{R}/block/storage_rack_front_{lv}.png')

# витрина (стекло + товар)
ds = metal((180, 185, 190), 4); outline(ds, 0, 0, 15, 15, (120, 125, 130)); rect(ds, 1, 2, 14, 13, (170, 210, 225)); ds.save(f'{R}/block/display_case_side.png')
dt = metal((175, 180, 185), 4); outline(dt, 0, 0, 15, 15, (120, 125, 130)); rect(dt, 2, 2, 13, 13, (190, 225, 235)); dt.save(f'{R}/block/display_case_top.png')
for lv in range(4):
    im = img(); noise(im, (180, 185, 190), 3); outline(im, 0, 0, 15, 15, (120, 125, 130))
    rect(im, 1, 1, 14, 12, (175, 215, 230))
    rect(im, 1, 7, 14, 7, (230, 240, 245))
    pix(im, [(2, 2), (3, 2), (2, 3)], (240, 250, 255))
    goods_rows(im, [(2, 6), (8, 11)], 2, 13, min(2, lv) if lv < 3 else 2, step=3 if lv < 3 else 2)
    rect(im, 1, 13, 14, 14, (60, 60, 65)); pix(im, [(7, 13), (8, 13)], GOLD)
    im.save(f'{R}/block/display_case_front_{lv}.png')

# сейф
sfs = metal((70, 75, 80), 5); outline(sfs, 0, 0, 15, 15, (40, 42, 45)); sfs.save(f'{R}/block/safe_side.png')
sft = metal((65, 70, 75), 4); outline(sft, 0, 0, 15, 15, (40, 42, 45)); sft.save(f'{R}/block/safe_top.png')
sff = metal((80, 85, 90), 4); outline(sff, 0, 0, 15, 15, (40, 42, 45)); outline(sff, 2, 2, 13, 13, (50, 52, 56))
rect(sff, 6, 5, 9, 8, (150, 150, 155)); outline(sff, 6, 5, 9, 8, (40, 40, 45)); pix(sff, [(7, 6), (8, 7)], (220, 220, 225))
rect(sff, 11, 9, 12, 11, (200, 170, 60)); pix(sff, [(4, 11), (5, 11)], (200, 40, 40))
sff.save(f'{R}/block/safe_front.png')

# гриль для шаурмы
gs = metal((190, 192, 195), 5); outline(gs, 0, 0, 15, 15, (120, 122, 126)); gs.save(f'{R}/block/shawarma_grill_side.png')
gt = metal((60, 60, 62), 4); outline(gt, 0, 0, 15, 15, (120, 122, 126)); rect(gt, 6, 6, 9, 9, (150, 90, 40)); gt.save(f'{R}/block/shawarma_grill_top.png')
gf = metal((190, 192, 195), 4); outline(gf, 0, 0, 15, 15, (120, 122, 126))
rect(gf, 2, 1, 4, 13, (230, 90, 30)); rect(gf, 11, 1, 13, 13, (230, 90, 30))           # горелки
for y in range(1, 14): pix(gf, [(3, y)], (255, 180, 60)); pix(gf, [(12, y)], (255, 180, 60))
rect(gf, 6, 1, 9, 13, (170, 105, 50)); rect(gf, 7, 0, 8, 15, (90, 90, 95))             # мясо на вертеле
for y in (2, 5, 8, 11): rect(gf, 6, y, 9, y, (130, 70, 30))
rect(gf, 0, 14, 15, 15, (90, 90, 95))
gf.save(f'{R}/block/shawarma_grill_front.png')

# мойка высокого давления
ws = metal((40, 110, 190), 5); outline(ws, 0, 0, 15, 15, (25, 70, 130)); ws.save(f'{R}/block/pressure_washer_side.png')
wt = metal((35, 100, 175), 4); outline(wt, 0, 0, 15, 15, (25, 70, 130)); wt.save(f'{R}/block/pressure_washer_top.png')
wf = metal((45, 120, 200), 4); outline(wf, 0, 0, 15, 15, (25, 70, 130))
rect(wf, 2, 2, 13, 5, (230, 230, 235)); pix(wf, [(4, 3), (5, 3), (6, 3), (8, 4), (9, 4)], (40, 110, 190))
for i in range(6): pix(wf, [(3 + i, 7 + i % 3)], (30, 30, 35))
rect(wf, 9, 7, 10, 13, (30, 30, 35)); rect(wf, 11, 12, 13, 13, (240, 200, 40)); pix(wf, [(13, 8), (12, 9), (13, 10)], (150, 210, 255))
wf.save(f'{R}/block/pressure_washer_front.png')

# камнедробилка
cs = metal((220, 170, 40), 6); outline(cs, 0, 0, 15, 15, (140, 105, 20))
for i in range(0, 16, 4): pix(cs, [(i, 14), (i + 1, 15)], (30, 30, 30))
cs.save(f'{R}/block/stone_crusher_side.png')
ct = metal((90, 90, 95), 5); outline(ct, 0, 0, 15, 15, (140, 105, 20)); rect(ct, 3, 3, 12, 12, (40, 40, 42))
for (x, y) in [(5, 5), (8, 6), (10, 9), (6, 10), (9, 4)]: rect(ct, x, y, x + 1, y + 1, (130, 130, 135))
ct.save(f'{R}/block/stone_crusher_top.png')
cf = metal((225, 175, 45), 5); outline(cf, 0, 0, 15, 15, (140, 105, 20))
rect(cf, 2, 2, 13, 9, (50, 50, 52))
for x in range(3, 13, 2): pix(cf, [(x, 3), (x + 1, 4), (x, 5), (x + 1, 6), (x, 7), (x + 1, 8)], (150, 150, 155))
rect(cf, 2, 11, 13, 13, (60, 60, 62)); pix(cf, [(4, 12), (7, 12), (10, 12)], (140, 140, 140))
cf.save(f'{R}/block/stone_crusher_front.png')

# топливная колонка
fs_ = metal((230, 230, 232), 4); outline(fs_, 0, 0, 15, 15, (150, 150, 155)); rect(fs_, 0, 0, 15, 2, (220, 40, 30)); fs_.save(f'{R}/block/fuel_pump_side.png')
ft_ = metal((210, 40, 30), 4); outline(ft_, 0, 0, 15, 15, (150, 25, 20)); ft_.save(f'{R}/block/fuel_pump_top.png')
ff_ = metal((235, 235, 237), 4); outline(ff_, 0, 0, 15, 15, (150, 150, 155)); rect(ff_, 0, 0, 15, 2, (220, 40, 30))
rect(ff_, 3, 4, 12, 7, (25, 25, 30)); pix(ff_, [(4, 5), (5, 5), (6, 5), (8, 6), (9, 6), (10, 6)], (240, 160, 40))
rect(ff_, 3, 9, 6, 13, (40, 40, 45)); rect(ff_, 4, 10, 5, 11, (220, 200, 40))
for y in range(9, 14): pix(ff_, [(10, y)], (30, 30, 30))
pix(ff_, [(11, 13), (12, 13), (12, 12)], (30, 30, 30))
ff_.save(f'{R}/block/fuel_pump_front.png')


# сырьё
def crate(base, mark, name, kind='box'):
    im = img()
    if kind == 'barrel':
        rect(im, 4, 2, 11, 14, base); outline(im, 4, 2, 11, 14, tuple(int(v * 0.6) for v in base))
        for y in (5, 11): rect(im, 4, y, 11, y, tuple(int(v * 0.6) for v in base))
        rect(im, 6, 7, 9, 9, mark)
    elif kind == 'can':
        rect(im, 4, 4, 11, 14, base); outline(im, 4, 4, 11, 14, tuple(int(v * 0.6) for v in base))
        rect(im, 8, 1, 10, 3, (40, 40, 45)); rect(im, 5, 2, 7, 3, base); rect(im, 6, 8, 9, 11, mark)
    else:
        rect(im, 2, 4, 13, 13, base); outline(im, 2, 4, 13, 13, tuple(int(v * 0.6) for v in base))
        rect(im, 2, 7, 13, 7, tuple(int(v * 0.75) for v in base)); rect(im, 6, 9, 9, 11, mark)
    im.save(f'{R}/item/{name}.png')


crate((170, 120, 70), (230, 90, 40), 'supply_shawarma')
crate((200, 160, 100), (60, 140, 200), 'supply_minimarket')
crate((40, 120, 210), (240, 240, 240), 'supply_car_wash', 'can')
crate((40, 40, 45), (230, 190, 40), 'supply_quarry', 'barrel')
crate((60, 30, 30), (230, 190, 50), 'supply_pawnshop')
crate((200, 40, 30), (240, 200, 40), 'supply_gas_station', 'barrel')
crate((235, 235, 240), (40, 40, 45), 'supply_electronics')

# ---------------- v1.7: гости для кочевников и покупатели
def vest(c):
    def f(im):
        rect(im, 20, 20, 27, 31, c); rect(im, 22, 20, 25, 31, (240, 240, 240))
    return f


def backpack(c):
    def f(im):
        rect(im, 32, 20, 39, 31, c); rect(im, 28, 20, 31, 31, c)
    return f


def headlamp(im):
    rect(im, 8, 8, 15, 8, (60, 60, 60)); rect(im, 11, 8, 12, 9, (255, 250, 180))


def dirt(im):
    for p in [(21, 24), (25, 27), (23, 30), (44, 26), (9, 12), (14, 13), (10, 20), (5, 22)]: im.putpixel(p, (90, 70, 45, 255))


vis('uchastkovy', SKIN, (60, 45, 30), (60, 70, 90), (45, 50, 65), (15, 15, 15), hat=(40, 50, 80), extra=combo(badge((230, 190, 50)), mustache))
vis('black_digger', (200, 160, 130), (40, 30, 20), (60, 55, 45), (45, 40, 35), (30, 25, 20), hat=(50, 50, 50), extra=combo(headlamp, stubble, dirt))
vis('lost_caver', SKIN, (150, 100, 50), (230, 120, 30), (80, 80, 90), (50, 35, 25), hat=(240, 200, 30), extra=combo(headlamp, dirt, stubble))
vis('eco_inspector', SKIN, (110, 80, 50), (50, 110, 60), (40, 70, 45), (25, 20, 15), hat=(40, 90, 50), extra=combo(badge((230, 230, 230)), glasses))
vis('hitchhiker', (225, 180, 150), (180, 120, 60), (220, 80, 60), (60, 80, 140), (90, 60, 40), hat=(220, 220, 60), extra=backpack((60, 120, 60)))
vis('peddler', (200, 150, 110), (20, 15, 10), (140, 90, 40), (50, 45, 40), (40, 30, 20), hat=(60, 40, 30), extra=combo(mustache, apron((200, 160, 60))))
vis('hobo', (190, 150, 120), (90, 80, 70), (90, 85, 70), (60, 55, 50), (40, 35, 30), hat=(70, 60, 50), extra=combo(stubble, dirt), beard=(110, 100, 90))
vis('treasure_hunter', (215, 175, 145), (180, 180, 180), (110, 90, 50), (70, 60, 40), (50, 35, 20), hat=(120, 90, 50), extra=combo(white_beard, backpack((90, 70, 40))))
vis('dps', SKIN, (60, 45, 30), (40, 60, 110), (30, 40, 80), (15, 15, 15), hat=(30, 45, 90), extra=combo(vest((210, 230, 40)), badge((230, 190, 50))))

os.makedirs(f'{R}/entity/customer', exist_ok=True)
skin('customer/c0', SKIN, (90, 60, 35), (70, 120, 190), (50, 50, 60), (30, 30, 30))
skin('customer/c1', (230, 190, 165), (200, 160, 60), (200, 70, 90), (40, 40, 60), (60, 30, 30), extra=scarf((200, 70, 90)))
skin('customer/c2', (200, 150, 110), (30, 25, 20), (60, 60, 65), (40, 50, 90), (20, 20, 20), extra=stubble)
skin('customer/c3', (225, 180, 150), (190, 190, 190), (120, 90, 70), (70, 60, 50), (40, 30, 20), hat=(90, 90, 95), extra=gray_hair)
skin('customer/c4', SKIN, (120, 60, 30), (90, 160, 90), (60, 60, 70), (240, 240, 240), extra=backpack((40, 40, 120)))
skin('customer/c5', (210, 170, 140), (60, 40, 25), (240, 200, 60), (40, 40, 45), (30, 25, 20), extra=glasses)

print('ok')
