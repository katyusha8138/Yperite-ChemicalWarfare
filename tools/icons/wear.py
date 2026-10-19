"""Icons of the First World War equipment: helmets, gas masks, uniforms and body armour."""
from pixel import Part, draw, ramp, tones

# ---------------------------------------------------------------------- colours (dark to light)

FELDGRAU = ramp("1d231c", "2c3429", "3f4a3b", "56624f", "6f7c66", "8c977f")
BRODIE = ramp("23251a", "363925", "4d5131", "66693e", "81834e", "9f9f63")
HORIZON = ramp("222b38", "334052", "4a5a6f", "647689", "8193a5", "a3b2c1")
AUSTRIAN = ramp("25221b", "38342a", "4f4a3a", "68614d", "837b63", "a1987d")
BRASS = ramp("4a3a18", "6e5628", "93743a", "b8944f", "d6b86c", "efd795")
BRONZE = ramp("3d2c14", "5a4220", "7a5b2e", "9b763e", "b99352")
NICKEL = ramp("33373a", "4c5155", "686e73", "878d92", "a9aeb2", "cdd1d3")
LEATHER = ramp("1f150d", "322216", "472f1f", "5f412b", "785539", "936b4a")
LEATHER_DARK = ramp("160f0a", "251a12", "37271b", "4b3626", "614733", "7b5c43")
RUBBER = ramp("121415", "1c1f21", "282c2e", "363c3e", "475052", "5d6668")
GLASS = ramp("0c181a", "172c30", "25444a", "3c6a72", "8fc4c9")
KHAKI_CLOTH = ramp("2b261b", "403928", "564d37", "6e6347", "887b5a", "a3956f")
WEBBING = ramp("433d2a", "554e36", "686044", "7c7353", "918865", "a89f7b")
FRENCH_CLOTH = ramp("303436", "464b4d", "5d6364", "777d7d", "939897", "b0b3b0")
METAL_GREY = ramp("202527", "30373a", "454d50", "5d6669", "788184", "979fa1")
OLIVE_PAINT = ramp("252a1d", "343b28", "475034", "5c6642", "747e54", "8e976a")
AUSTRIAN_FABRIC = ramp("29281f", "3a382d", "4d4a3c", "625e4c", "79745f", "938d76")

BRITISH_KHAKI = ramp("2a2517", "3d3522", "524830", "695e3f", "817550", "9c8f66")
FRENCH_BLUE = ramp("27303c", "394555", "4f5d70", "68788c", "8595a8", "a5b3c3")
GERMAN_FG = ramp("232821", "333a30", "475042", "5d6757", "77816f", "939b89")
GERMAN_STONE = ramp("262627", "363738", "4a4b4c", "606162", "78797a", "939495")
GERMAN_COLLAR = ramp("141c18", "1e2a24", "2a3a32", "374b40", "465c50")
AUSTRIAN_FG = ramp("2a2920", "3b392e", "504c3d", "67624f", "807a64", "9c957d")
BOOT = ramp("1b120b", "2b1d12", "3d2a1a", "523923", "6a4b2f", "84603d")
SOLE = ramp("0f0b08", "1a130d", "251b13")
RED = ramp("4a1010", "6e1818", "922222", "b33030")
DARK_BLUE = ramp("161c30", "222b48", "303c62", "41507e")
DAYFIELD_KHAKI = ramp("362f20", "4b432e", "625840", "7b6f52", "958867", "b0a27f")


def P(name, colours, **kw):
    return Part(name, colours, **kw)


# ---------------------------------------------------------------------- helmets

def stahlhelm():
    """Side view, looking left: domed crown, short visor, deep skirt over the ears, flared neck guard, a lug."""
    legend = tones(FELDGRAU)
    return draw([
        "................",
        "................",
        "......4444......",
        "....44554433....",
        "...4455443332...",
        "..345544333322..",
        "..344433333322..",
        "..333334433221..",
        ".54444444444443.",
        "....33333333222.",
        "....32222222221.",
        "....22222222211.",
        ".....1111111111.",
        "............111.",
        "................",
        "................",
    ], legend)


def brodie():
    """Side view: a shallow bowl on a wide flat brim, the chin strap hanging below."""
    legend = tones(BRODIE)
    legend["b"] = P("strap", LEATHER)
    legend["B"] = (P("buckle", BRASS), 1)
    return draw([
        "................",
        "................",
        "................",
        "......4554......",
        "....44554433....",
        "...4544443332...",
        "...4444433332...",
        "...3333333322...",
        ".55555555555544.",
        ".12222222222221.",
        "....b.......b...",
        "....b.......b...",
        ".....b.....b....",
        "......bbBbb.....",
        "................",
        "................",
    ], legend)


def adrian():
    """Side view: a rounded bowl with the crest along its top, a long peak in front and a short one behind,
    and the grenade badge."""
    legend = tones(HORIZON)
    legend["g"] = P("badge", BRONZE)
    return draw([
        "................",
        "................",
        "................",
        "......4444......",
        "....45555443....",
        "...4554444333...",
        "..455444443332..",
        "..4g4444433332..",
        "..gg4444333322..",
        "..443333333322..",
        ".35555555555543.",
        ".43222222222221.",
        ".3............1.",
        "................",
        "................",
        "................",
    ], legend)


def berndorfer():
    """Side view: like the German helmet but rounder, with a bell-shaped skirt all round and a large lug."""
    legend = tones(AUSTRIAN)
    legend["l"] = (P("lug", AUSTRIAN), 1)
    return draw([
        "................",
        "................",
        "......4444......",
        "....44554433....",
        "...4455443332...",
        "..345544333322..",
        "..3444333333221.",
        "..33333ll333221.",
        "..44444ll444443.",
        "..433333333332..",
        "..332222222221..",
        ".33222222222211.",
        ".2111111111111..",
        "................",
        "................",
        "................",
    ], legend)


# ---------------------------------------------------------------------- gas masks (front view)

def _mask(colours, rim, rows, extra=None):
    legend = tones(colours)
    legend["E"] = P("rim", rim)
    legend["g"] = P("glass", GLASS, base=1)
    legend["G"] = (P("glass", GLASS, base=1), 3)
    if extra:
        legend.update(extra)
    return draw(rows, legend)


def gas_mask():
    """The mod's own mask: black rubber, nickel eyepieces, an olive drum filter at the chin."""
    return _mask(RUBBER, NICKEL, [
        "................",
        "................",
        ".....344443.....",
        "....34455443....",
        "...3445554433...",
        "..3EEE444EEE3...",
        "..EGgE343EGgE2..",
        "..EggE333EggE2..",
        "..3EEE332EEE22..",
        "...333nnn33322..",
        "...32cccccc22...",
        "....ccCcccc2....",
        "....cCcccccc....",
        ".....cccccc.....",
        "................",
        "................",
    ], {"n": P("ring", NICKEL), "c": P("drum", OLIVE_PAINT), "C": (P("drum", OLIVE_PAINT), 2)})


def small_box_respirator():
    """British SBR: khaki face piece, brass eyepieces and mouthpiece, the hose running down to the box."""
    return _mask(KHAKI_CLOTH, BRASS, [
        "................",
        "................",
        ".....344443.....",
        "....34455443....",
        "...3445554433...",
        "..3EEE444EEE3...",
        "..EGgE343EGgE2..",
        "..EggE333EggE2..",
        "..3EEE332EEE22..",
        "...3333bb33322..",
        "....3332b3222...",
        ".....22hh222....",
        ".......hh.......",
        "........hh......",
        ".........hh.....",
        "................",
    ], {"b": P("mouthpiece", BRASS), "h": P("hose", RUBBER)})


def m2_mask():
    """French M2: one piece of grey cloth, two round eye windows, a rounded snout over nose and mouth."""
    return _mask(FRENCH_CLOTH, FRENCH_CLOTH, [
        "................",
        "................",
        ".....344443.....",
        "....34455443....",
        "...3445554433...",
        "..344444444332..",
        "..3gG34443gG32..",
        "..3gg34443gg32..",
        "..3443444434322.",
        "..334455554332..",
        "...3445555432...",
        "....34444432....",
        ".....222222.....",
        "................",
        "................",
        "................",
    ])


def lederschutzmaske():
    """German leather mask: dark oiled leather, steel-rimmed eyepieces, the filter drum on the mouth."""
    return _mask(LEATHER_DARK, NICKEL, [
        "................",
        "................",
        ".....344443.....",
        "....34455443....",
        "...3445554433...",
        "..3EEE444EEE3...",
        "..EGgE343EGgE2..",
        "..EggE333EggE2..",
        "..3EEE332EEE22..",
        "...3ddddddd22...",
        "...dDDdddddd2...",
        "...dDdddddddd...",
        "...ddddddddd....",
        "....ddddddd.....",
        "................",
        "................",
    ], {"d": P("drum", METAL_GREY), "D": (P("drum", METAL_GREY), 2)})


def austrian_gas_mask():
    """Austro-Hungarian mask: rubberised grey-brown fabric, brass eyepieces, a long drum at the mouth."""
    return _mask(AUSTRIAN_FABRIC, BRASS, [
        "................",
        "................",
        ".....344443.....",
        "....34455443....",
        "...3445554433...",
        "..3EEE444EEE3...",
        "..EGgE343EGgE2..",
        "..EggE333EggE2..",
        "..3EEE332EEE22..",
        "...333bbbb3322..",
        "....3dDdddd22...",
        "....dDddddd2....",
        "....ddddddd.....",
        ".....ddddd......",
        "................",
        "................",
    ], {"b": P("ring", BRASS), "d": P("drum", METAL_GREY), "D": (P("drum", METAL_GREY), 2)})


# ---------------------------------------------------------------------- uniforms (front view)

def _row(*segments):
    cells = ["."] * 16
    for x, text in segments:
        for i, ch in enumerate(text):
            cells[x + i] = ch
    return "".join(cells)


def _blank():
    return ["." * 16 for _ in range(16)]


def tunic_icon(cloth, *, collar=None, buttons=None, belt=LEATHER, buckle=BRASS, breast_pockets=False,
               collar_patch=None):
    """A tunic laid flat: shoulders and collar, sleeves down the sides, a row of buttons (or a fly front),
    pocket flaps, the belt with its buckle and the skirt below it."""
    legend = tones(cloth)
    legend["c"] = P("collar", collar or cloth, base=3 if collar is None else 2)
    legend["x"] = (P("patch", collar_patch or cloth), 1)
    legend["b"] = (P("button", buttons or cloth, bevel=False), 4 if buttons else 1)
    legend["B"] = P("belt", belt or cloth)
    legend["K"] = (P("buckle", buckle, bevel=False), 4)
    rows = _blank()
    collar_row = "cccc" if collar_patch is None else "xccx"
    rows[2] = _row((3, "445" + collar_row + "443"))
    rows[3] = _row((1, "4454444433333221"[:14]))
    rows[4] = _row((1, "45444444433332"))
    body = ["44544332", "44444332", "44544332", "44444332", "44444322"]
    for k, line in enumerate(body):
        y = 5 + k
        rows[y] = _row((1, "45"), (4, line), (13, "21"))
    # buttons down the front, or a sewn fly
    for y in range(3, 9):
        ch = "b" if buttons is not None and y % 2 == 1 else "2"
        rows[y] = rows[y][:7] + ch + rows[y][8:]
    # pocket flaps: a light row with a shadow under it
    def flap(y, x0, x1):
        line = list(rows[y])
        under = list(rows[y + 1])
        for x in range(x0, x1 + 1):
            line[x] = "5" if x < 8 else "4"
            under[x] = "2" if x < 8 else "1"
        rows[y] = "".join(line)
        rows[y + 1] = "".join(under)
    if breast_pockets:
        flap(4, 5, 6)
        flap(4, 9, 10)
    flap(7, 5, 6)
    flap(7, 9, 10)
    belt_row = "BBBKKBBB" if belt is not None else "33333222"
    rows[10] = _row((1, "c3"), (4, belt_row), (13, "1c"))
    rows[11] = _row((4, "44443322"))
    rows[12] = _row((3, "3333322221"))
    return draw(rows, legend)


def trousers_icon(cloth, piping=False):
    """Trousers laid flat: the waistband, then two legs."""
    legend = tones(cloth)
    legend["r"] = (P("piping", RED, bevel=False), 2)
    rows = _blank()
    rows[2] = _row((3, "3333333332"))
    rows[3] = _row((3, "4554443332"))
    rows[4] = _row((3, "4544443332"))
    rows[5] = _row((3, "4544.43332"))
    for y in range(6, 13):
        rows[y] = _row((3, "4543"), (9, "4332"))
    rows[13] = _row((3, "2222"), (9, "2111"))
    if piping:
        for y in range(3, 14):
            rows[y] = "r".join([rows[y][:3], rows[y][4:12]]) + "r" + rows[y][13:]
    return draw(rows, legend)


def puttees_icon(wrap):
    """A pair of lower legs wound in puttees, over laced ankle boots, toes to the left."""
    legend = tones(wrap)
    legend["t"] = (P("tape", wrap), 1)
    for i, d in enumerate("abcdef"):
        legend[d] = (P("boot", BOOT, base=0, bevel=False), i)
    legend["s"] = (P("sole", SOLE, base=0, bevel=False), 1)
    rows = _blank()
    rows[1] = _row((3, "tttt"), (9, "tttt"))

    def wound(x, y):
        # a lit left side, a shaded right side, and the diagonal edges of the turns
        tone = (4, 4, 3, 2)[x] - (1 if (x - y) % 3 == 0 else 0)
        return str(max(1, tone))

    for y in range(2, 10):
        left = "".join(wound(x, y) for x in range(4))
        right = "".join(wound(x, y + 1) for x in range(4))
        rows[y] = _row((3, left), (9, right))
    rows[10] = _row((2, "deddc"), (8, "deddc"))
    rows[11] = _row((1, "eedccb"), (7, "eedccb"))
    rows[12] = _row((1, "dcccbba"), (7, "dcccbba"))
    rows[13] = _row((1, "sssssss"), (7, "sssssss"))
    return draw(rows, legend)


def british_tunic():
    return tunic_icon(BRITISH_KHAKI, buttons=BRASS, belt=WEBBING, breast_pockets=True)


def french_tunic():
    return tunic_icon(FRENCH_BLUE, buttons=BRASS, belt=LEATHER, collar_patch=DARK_BLUE)


def german_tunic():
    return tunic_icon(GERMAN_FG, collar=GERMAN_COLLAR, belt=RUBBER, buckle=NICKEL)


def austrian_tunic():
    return tunic_icon(AUSTRIAN_FG, belt=LEATHER, buckle=NICKEL, breast_pockets=True, collar_patch=RED)


def british_trousers():
    return trousers_icon(BRITISH_KHAKI)


def french_trousers():
    return trousers_icon(FRENCH_BLUE)


def german_trousers():
    return trousers_icon(GERMAN_STONE, piping=True)


def austrian_trousers():
    return trousers_icon(AUSTRIAN_FG)


def british_puttees():
    return puttees_icon(BRITISH_KHAKI)


def french_puttees():
    return puttees_icon(FRENCH_BLUE)


def german_puttees():
    return puttees_icon(GERMAN_FG)


def austrian_puttees():
    return puttees_icon(AUSTRIAN_FG)


# ---------------------------------------------------------------------- body armour

def sappenpanzer():
    """The breastplate and the three plates hung below it, riveted, in field grey."""
    legend = tones(FELDGRAU)
    legend["r"] = (P("rivet", FELDGRAU, bevel=False), 5)
    legend["k"] = P("strap", LEATHER)
    return draw([
        "................",
        "...kk......kk...",
        "...kk......kk...",
        "..445555554443..",
        ".4r45555544r432.",
        ".44544444443332.",
        ".44444444433322.",
        "..444443333322..",
        "...1111111111...",
        "..4r4444443r32..",
        "..1111111111111.",
        "...4r444443r2...",
        "...11111111111..",
        "....4r44443r....",
        "....11111111....",
        "................",
    ], legend)


def dayfield():
    """Khaki cloth over steel plates, quilted across the middle, straps over the shoulders and brass buckles."""
    legend = tones(DAYFIELD_KHAKI)
    legend["w"] = P("web", WEBBING)
    legend["K"] = (P("buckle", BRASS, bevel=False), 4)
    return draw([
        "................",
        "...ww......ww...",
        "...ww......ww...",
        "..455554444433..",
        "..454444444332..",
        ".K44444444433K..",
        "..444444443332..",
        "..444444433332..",
        "..222222222211..",
        "..444444333332..",
        ".K44444433332K..",
        "..444443333322..",
        "..444333333222..",
        "...2222222111...",
        "................",
        "................",
    ], legend)
