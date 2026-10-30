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

print('ok')
