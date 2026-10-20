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
RUSSIAN_KHAKI_PAINT = ramp("22251a", "333826", "474e33", "5d6542", "767f55", "92996c")
RUSSIAN_KHAKI = ramp("2a2a1a", "3c3c26", "525235", "6a6946", "84825a", "a09d73")
GREY_RUBBER = ramp("1a1d1c", "282c2b", "383d3b", "4a504d", "5f6562", "797e7a")
TIN = ramp("2e3236", "464b50", "62686d", "80868b", "a2a7ab", "c6cacd")
BLACK_LEATHER = ramp("0c0c0c", "171616", "232120", "302d2b", "403c39", "55504c")
SCARLET = ramp("4a0f0f", "6e1616", "922020", "b32d2a", "cc4440")
GOLD = ramp("5a4410", "7e6018", "a37f26", "c79f3a", "e0bc55", "f2d885")
PATENT = ramp("0c0c0d", "161618", "222326", "303236", "45484d", "60646a")
BLACK_FELT = ramp("141416", "1e1e22", "2a2a2f", "36363c", "44444b", "55555d")
ORANGE = ramp("6a2d08", "8e3d0c", "b05212", "cc6a1c", "e0852c")
WHITE = ramp("8f8f8a", "b3b3ad", "d4d4ce", "ecece6")
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


def adrian_russian():
    """The Adrian as the Russians had it: khaki paint and a brass double-headed eagle on the front."""
    legend = tones(RUSSIAN_KHAKI_PAINT)
    legend["g"] = P("eagle", BRASS)
    legend["G"] = (P("eagle", BRASS), 4)
    return draw([
        "................",
        "................",
        "................",
        "......4444......",
        "....45555443....",
        "...4554444333...",
        "..G55444443332..",
        "..gg4444433332..",
        "..Gg4444333322..",
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


# ---------------------------------------------------------------------- caps and headgear (side view, looking left)

def _cap_legend(cloth, peak=PATENT):
    legend = tones(cloth)
    legend["p"] = P("peak", peak)
    legend["P"] = (P("peak", peak), 2)
    return legend


def british_cap():
    """The Service Dress cap: a wide flat khaki crown, the band with the brass badge and the brown chin strap,
    a brown leather peak."""
    legend = _cap_legend(BRITISH_KHAKI, LEATHER)
    legend["g"] = P("badge", BRASS)
    legend["c"] = (P("strap", LEATHER), -1)
    return draw([
        "................",
        "................",
        "................",
        "................",
        "...4444444444...",
        "..455555555443..",
        "..444444444333..",
        "...3333333332...",
        "...gg333333322..",
        "...cccccc33222..",
        "..pPpp22222111..",
        ".ppp............",
        "................",
        "................",
        "................",
        "................",
    ], legend)


def french_kepi():
    """The kepi in horizon blue: upright in front, sloping in behind to a small flat top, a flat black peak and
    the black chin strap with its brass buttons."""
    legend = _cap_legend(FRENCH_BLUE)
    legend["k"] = P("strap", PATENT)
    legend["b"] = (P("button", BRASS, bevel=False), 4)
    return draw([
        "................",
        "................",
        "................",
        "....5555543.....",
        "....44444433....",
        "....444444333...",
        "....3444443322..",
        "....33333333322.",
        "....33333333222.",
        "....bkkkkkkb222.",
        "....22222222211.",
        "..Ppppp.........",
        "................",
        "................",
        "................",
        "................",
    ], legend)


def _wide_cap(cloth, band, cockade_top, cockade_band, piping=SCARLET):
    legend = _cap_legend(cloth)
    legend["r"] = P("piping", piping)
    legend["R"] = P("band", band)
    legend["o"] = cockade_top
    legend["x"] = cockade_band
    return legend


def german_cap():
    """The German peaked cap: a wide field-grey crown piped in red, the red band, the two cockades, a glossy peak."""
    legend = _wide_cap(GERMAN_FG, SCARLET, (P("cockade", SCARLET, bevel=False), 3), (P("state", WHITE, bevel=False), 2))
    return draw([
        "................",
        "................",
        "................",
        "...4555555554...",
        "..rrrrrrrrrrrr..",
        "..444444444333..",
        "...3o333333322..",
        "....33333332....",
        "...RxRRRRRRRR...",
        "...RRRRRRRRRR...",
        ".ppPp22222111...",
        "ppp.............",
        "................",
        "................",
        "................",
        "................",
    ], legend)


def russian_cap():
    """The furazhka: a broad flat khaki crown piped in red, a darker band with the orange cockade, a black peak."""
    legend = _wide_cap(RUSSIAN_KHAKI, RUSSIAN_KHAKI, (P("cloth", RUSSIAN_KHAKI, bevel=False), 3),
                       (P("cockade", ORANGE, bevel=False), 3))
    legend["R"] = (P("band", RUSSIAN_KHAKI, base=1), 0)
    return draw([
        "................",
        "................",
        "................",
        "...4555555554...",
        "..rrrrrrrrrrrr..",
        "..444444444333..",
        "...33333333322..",
        "....33333332....",
        "...RxRRRRRRRR...",
        "...rrrrrrrrrr...",
        ".ppPp22222111...",
        "ppp.............",
        "................",
        "................",
        "................",
        "................",
    ], legend)


def austrian_cap():
    """The Austro-Hungarian officer's cap: upright and flat-topped, the gilt badge high in front, the neck flap
    buttoned in front, a short cloth peak."""
    legend = _cap_legend(AUSTRIAN_FG, AUSTRIAN_FG)
    legend["g"] = (P("badge", GOLD, bevel=False), 4)
    legend["b"] = (P("button", GOLD, bevel=False), 3)
    return draw([
        "................",
        "................",
        "................",
        "....44444444....",
        "...4555555443...",
        "...g444444433...",
        "...4444444433...",
        "...3333333322...",
        "...2b22222221...",
        "...2222222221...",
        "..pppp..........",
        "................",
        "................",
        "................",
        "................",
        "................",
    ], legend)


def _pickel(rows, shell, peaks, spike, extra=None):
    legend = tones(shell)
    legend["p"] = P("peak", peaks)
    legend["g"] = P("spike", spike)
    legend["G"] = (P("spike", spike), 2)
    legend["s"] = (P("scales", BRASS, bevel=False), 2)
    if extra:
        legend.update(extra)
    return draw(rows, legend)


PICKEL_ROWS = [
    ".......G........",
    ".......g........",
    "......gGg.......",
    "....ggggggg.....",
    "...4455444433...",
    "..455444444333..",
    "..e44444443333..",
    "..ee4444433332..",
    "..ee3333333322..",
    "..3333333333221.",
    ".pp.s......1111.",
    "pp...s......111.",
    ".....s..........",
    "................",
    "................",
    "................",
]


def pickelhaube():
    """The Pickelhaube: a black lacquered dome with its brass spike, the brass eagle in front, the front peak and
    the deeper neck guard, brass chin scales."""
    return _pickel(PICKEL_ROWS, PATENT, PATENT, BRASS, {"e": (P("eagle", BRASS, bevel=False), 3)})


def pickelhaube_covered():
    """The Pickelhaube in its field-grey cover, with the regiment's number in red on the front and the spike
    standing out of it."""
    rows = [r.replace("s", "l") for r in PICKEL_ROWS]
    return _pickel(rows, GERMAN_FG, GERMAN_FG, METAL_GREY, {"e": (P("number", SCARLET, bevel=False), 3),
                                                             "l": (P("strap", LEATHER), 0)})


def austrian_shako():
    """The shako: a tall black body narrowing to the top, the gold braid and cockade with its loop, the brass
    double eagle on the front, a black peak."""
    legend = tones(BLACK_FELT)
    legend["t"] = (P("top", PATENT), 3)
    legend["y"] = P("braid", GOLD)
    legend["k"] = (P("cockade", GOLD, bevel=False), 4)
    legend["l"] = (P("loop", GOLD, bevel=False), 3)
    legend["g"] = (P("eagle", BRASS, bevel=False), 3)
    legend["p"] = P("peak", PATENT)
    return draw([
        "................",
        "................",
        "....tttttttt....",
        "....yyyyyyyy....",
        "....k4444433....",
        "....l4444433....",
        "...gg44444433...",
        "...gg44444333...",
        "...g444443333...",
        "...4444433332...",
        "...3333333222...",
        ".pppp...........",
        "................",
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


def zelinsky_kummant_mask():
    """Russian Zelinsky-Kummant: a grey rubber helmet over the whole head, small tin-rimmed eyepieces and the
    flat tin box of charcoal hanging from the mouth."""
    return _mask(GREY_RUBBER, TIN, [
        "................",
        ".....444444.....",
        "....45555443....",
        "...4555544433...",
        "...4544444433...",
        "...4EE444EE33...",
        "...EGgE4EGgE3...",
        "...4EE433EE32...",
        "...44433333322..",
        "...343bbb33322..",
        "....3tTTTTt22...",
        "....tTttttTt....",
        "....tTttttTt....",
        "....tttttttt....",
        ".....111111.....",
        "................",
    ], {"b": P("mouthpiece", TIN), "t": P("box", TIN), "T": (P("box", TIN), 4)})


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
               collar_patch=None, pullover=False):
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
    # buttons down the front, or a sewn fly; a pullover has a short placket at the neck and none below it
    for y in range(3, 9):
        if pullover and y > 6:
            continue
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
    if not pullover:
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


def breeches_icon(cloth):
    """Breeches laid flat: full over the thighs, narrowing to the knee."""
    legend = tones(cloth)
    rows = _blank()
    rows[2] = _row((3, "3333333332"))
    rows[3] = _row((3, "4554443332"))
    rows[4] = _row((2, "445444433322"))
    rows[5] = _row((1, "445443"), (9, "443321"))
    rows[6] = _row((1, "455443"), (9, "443321"))
    rows[7] = _row((2, "45443"), (9, "44332"))
    rows[8] = _row((3, "4543"), (9, "4332"))
    for y in range(9, 13):
        rows[y] = _row((3, "4543"), (9, "4332"))
    rows[13] = _row((3, "2222"), (9, "2111"))
    return draw(rows, legend)


def jackboots_icon(leather):
    """A pair of tall leather boots, toes to the left, shining down the front."""
    legend = tones(leather)
    legend["s"] = (P("sole", SOLE, base=0, bevel=False), 1)
    rows = _blank()
    rows[1] = _row((3, "3333"), (9, "3333"))
    rows[2] = _row((3, "2222"), (9, "2222"))
    for y in range(3, 10):
        rows[y] = _row((3, "5432"), (9, "5432"))
    rows[10] = _row((2, "45432"), (8, "45432"))
    rows[11] = _row((1, "455432"), (7, "455432"))
    rows[12] = _row((1, "4443221"), (7, "4443221"))
    rows[13] = _row((1, "sssssss"), (7, "sssssss"))
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


def russian_tunic():
    return tunic_icon(RUSSIAN_KHAKI, buttons=BRASS, belt=LEATHER, breast_pockets=True, pullover=True)


def british_trousers():
    return trousers_icon(BRITISH_KHAKI)


def french_trousers():
    return trousers_icon(FRENCH_BLUE)


def german_trousers():
    return trousers_icon(GERMAN_STONE, piping=True)


def austrian_trousers():
    return trousers_icon(AUSTRIAN_FG)


def russian_trousers():
    return breeches_icon(RUSSIAN_KHAKI)


def russian_boots():
    return jackboots_icon(BLACK_LEATHER)


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
