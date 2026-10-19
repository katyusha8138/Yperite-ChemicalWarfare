"""Icons of the chemical weapons and the protective and medical gear."""
from pixel import Part, draw, ramp, tones
from wear import BRASS, LEATHER, OLIVE_PAINT, _blank, _row, tunic_icon

# One band colour per gas, as on the grenades from the start: tear gas cream, chlorine green, blood agent red,
# yperite ochre, nerve agent purple, Blue Cross blue.
BANDS = {
    "tear_gas": ramp("6e6a52", "9c977a", "c9c3a1", "e8e3c4", "faf6e2"),
    "chlorine": ramp("3d5216", "5b7a1f", "7fa62b", "a3cc3c", "c8e86a"),
    "blood_gas": ramp("4d1414", "7a1f1f", "a62b2b", "cc4040", "e86a6a"),
    "yperite": ramp("5c4310", "8a6418", "b88a26", "d9aa3c", "f0c862"),
    "nerve_gas": ramp("2e2152", "4a367a", "6a52a6", "8d74cc", "b39ee8"),
    "blue_cross": ramp("15304d", "214a75", "2f65a0", "4a86c4", "78acdf"),
}

STEEL_GREY = ramp("1e2224", "2e3437", "434b4f", "5c6569", "788286", "98a1a4")
COPPER = ramp("4a2312", "6e351c", "934a28", "b56338", "d6844f")
SHELL_GREY = ramp("22262a", "33393f", "4a5258", "636c73", "7f888f", "a0a8ae")
CREAM = ramp("6b6650", "958f72", "bdb694", "ddd6b4", "f2ecd0")
RUBBER_OD = ramp("1e2414", "2c341d", "3e4828", "525e35", "6a7744", "848f57")
WHITE = ramp("5a5e60", "7d8284", "a3a8aa", "c6cacb", "e2e5e5", "f7f9f9")
WATER = ramp("1b3d5c", "28598a", "3a7bb8", "5c9fd6", "9ccbf0")
RED = ramp("4d1010", "7a1a1a", "a82626", "d03a3a", "ec6a6a")
DIAL = ramp("4a4636", "7a7458", "b0a984", "ddd6b2", "f5f0d8")
ORANGE = ramp("5c2a0a", "8a4210", "b8601a", "e0842e", "f5a85a")
TIN = ramp("2a2d2f", "3f4447", "5a6064", "777e82", "979ea1", "bcc2c4")


def P(name, colours, **kw):
    return Part(name, colours, **kw)


# ---------------------------------------------------------------------- hand grenades

def grenade(gas):
    """An egg-shaped gas grenade in olive paint, its gas band round the middle, the fuse and lever on top."""
    legend = tones(OLIVE_PAINT)
    for i, d in enumerate("abcde"):
        legend[d] = (P("band", BANDS[gas], base=0, bevel=False), i)
    legend["m"] = P("fuse", STEEL_GREY)
    legend["o"] = (P("ring", STEEL_GREY, base=0, bevel=False), 4)
    return draw([
        "................",
        "......mm..oo....",
        ".....mmmmo..o...",
        ".....mmmmmmoo...",
        "......mmmmmm....",
        ".....445443.....",
        "....45544333....",
        "...4554443332...",
        "...dddccccbba...",
        "...ddccccbbba...",
        "...4444333322...",
        "...4443333222...",
        "....33332221....",
        ".....222111.....",
        "................",
        "................",
    ], legend)


# ---------------------------------------------------------------------- Superb Warfare rounds

def _band(gas):
    return {d: (P("band", BANDS[gas], base=0, bevel=False), i) for i, d in enumerate("abcde")}


def grenade_40mm(gas):
    """A 40 mm grenade cartridge: the rounded olive projectile with its gas band, the copper band, the brass case."""
    legend = tones(OLIVE_PAINT)
    legend.update(_band(gas))
    for i, d in enumerate("vwxyz"):
        legend[d] = (P("case", BRASS, base=0, bevel=False), i + 1)
    legend["r"] = (P("copper", COPPER, base=0, bevel=False), 3)
    return draw([
        "................",
        "................",
        "......4554......",
        ".....455443.....",
        ".....454433.....",
        ".....dccbba.....",
        ".....444332.....",
        ".....rrrrrr.....",
        ".....zyyxwv.....",
        ".....zyyxwv.....",
        ".....zyyxwv.....",
        ".....zyyxwv.....",
        "....zyyyxxwv....",
        "....xxxwwwvv....",
        "................",
        "................",
    ], legend)


def mortar_shell(gas):
    """A finned mortar bomb: the teardrop body in grey with its gas band, the tail and fins below."""
    legend = tones(SHELL_GREY)
    legend.update(_band(gas))
    legend["f"] = P("fin", STEEL_GREY)
    return draw([
        "................",
        ".......44.......",
        "......4554......",
        "......4544......",
        ".....455443.....",
        ".....dccbba.....",
        ".....455433.....",
        ".....454433.....",
        ".....454332.....",
        "......4432......",
        ".......43.......",
        ".......43.......",
        "......f43f......",
        ".....ff43ff.....",
        ".....f....f.....",
        "................",
    ], legend)


def large_shell(gas):
    """An artillery shell: pointed nose, grey body with its gas band, copper driving band, flat base."""
    legend = tones(SHELL_GREY)
    legend.update(_band(gas))
    legend["r"] = (P("copper", COPPER, base=0, bevel=False), 3)
    legend["R"] = (P("copper", COPPER, base=0, bevel=False), 1)
    legend["n"] = P("fuze", BRASS)
    return draw([
        "................",
        ".......nn.......",
        "......nnnn......",
        "......4553......",
        ".....455433.....",
        ".....454433.....",
        ".....dccbba.....",
        ".....dccbba.....",
        ".....454332.....",
        ".....454332.....",
        ".....444332.....",
        ".....rrrrRR.....",
        ".....444322.....",
        ".....222111.....",
        "................",
        "................",
    ], legend)


# ---------------------------------------------------------------------- filters

def _drum(body, label, rows):
    legend = tones(body)
    for i, d in enumerate("abcde"):
        legend[d] = (P("label", label, base=0, bevel=False), i)
    legend["m"] = P("thread", STEEL_GREY)
    return draw(rows, legend)


DRUM = [
    "................",
    "......mmmm......",
    "......mmmm......",
    "....44555543....",
    "...4555555543...",
    "...4444443332...",
    "...4544443332...",
    "...dddcccbbaa...",
    "...dddcccbbaa...",
    "...4544443332...",
    "...4544443332...",
    "...4444433322...",
    "....33332222....",
    "................",
    "................",
    "................",
]


def gas_filter_general():
    """A filter drum in olive paint with a cream label: for most gases."""
    return _drum(OLIVE_PAINT, CREAM, DRUM)


def gas_filter_special():
    """A dark filter drum with a red label: for blood and nerve agents."""
    return _drum(STEEL_GREY, RED, DRUM)


# ---------------------------------------------------------------------- the protective outfit (rubberised olive)

def protective_hood():
    """A rubber hood with a wide face opening, seen from the front."""
    legend = tones(RUBBER_OD)
    return draw([
        "................",
        "......4444......",
        "....44555443....",
        "...4555444433...",
        "..455444444332..",
        "..454000000332..",
        "..44000000003...",
        "..44000000003...",
        "..43000000002...",
        "..44300000032...",
        "..443300003322..",
        "..4443333333221.",
        ".44444433333221.",
        ".44443333332221.",
        "..222222222111..",
        "................",
    ], legend)


def protective_jacket():
    """The rubberised jacket laid flat: no buttons and no belt, a sealed front."""
    return tunic_icon(RUBBER_OD, belt=None)


def protective_trousers():
    legend = tones(RUBBER_OD)
    rows = _blank()
    rows[2] = _row((3, "3333333332"))
    rows[3] = _row((3, "4554443332"))
    rows[4] = _row((3, "4544443332"))
    rows[5] = _row((3, "4544.43332"))
    for y in range(6, 13):
        rows[y] = _row((3, "4543"), (9, "4332"))
    rows[13] = _row((3, "2222"), (9, "2111"))
    return draw(rows, legend)


def protective_boots():
    """Rubber overboots, a pair, toes to the left."""
    legend = tones(RUBBER_OD)
    rows = _blank()
    for y in range(3, 10):
        rows[y] = _row((3, "4543"), (9, "4332"))
    rows[2] = _row((3, "5554"), (9, "5443"))
    rows[10] = _row((2, "44543"), (8, "44332"))
    rows[11] = _row((1, "455443"), (7, "443322"))
    rows[12] = _row((1, "4444332"), (7, "4333221"))
    rows[13] = _row((1, "1111111"), (7, "1111111"))
    return draw(rows, legend)


def sealed_fabric():
    """A folded piece of rubberised cloth, the fold catching the light."""
    legend = tones(RUBBER_OD)
    return draw([
        "................",
        "................",
        "................",
        "..444444444443..",
        "..455555555543..",
        "..444444444433..",
        "..333333333322..",
        "..555555555554..",
        "..444444444433..",
        "..444444444333..",
        "..443444443333..",
        "..444444433332..",
        "..333333333222..",
        "................",
        "................",
        "................",
    ], legend)


# ---------------------------------------------------------------------- detection

def gas_detector():
    """A hand detector: a grey case with a carrying handle, a round dial and a red warning lamp."""
    legend = tones(STEEL_GREY)
    for i, d in enumerate("abcde"):
        legend[d] = (P("dial", DIAL, base=0, bevel=False), i)
    legend["n"] = (P("needle", RED, base=0, bevel=False), 1)
    legend["L"] = (P("lamp", RED, base=0, bevel=False), 3)
    legend["l"] = (P("lamp", RED, base=0, bevel=False), 4)
    legend["h"] = P("handle", LEATHER)
    return draw([
        "................",
        ".....hhhhhh.....",
        ".....h....h.....",
        "...4555555543...",
        "...4544444332...",
        "...45.dddd.32...",
        "...4.dddddc.2...",
        "...4.ddndcc.2...",
        "...4.dnccbb.2...",
        "...45.cbbb.32...",
        "...4544444332...",
        "...44lL444332...",
        "...4444433322...",
        "...2222222111...",
        "................",
        "................",
    ], legend)


def wind_vane():
    """A pocket wind vane: an arrow with a tail fin turning on a brass pin, on a short handle."""
    legend = tones(BRASS)
    legend["r"] = P("vane", RED)
    legend["w"] = P("tail", WHITE)
    legend["h"] = P("handle", LEATHER)
    return draw([
        "................",
        "................",
        "...........ww...",
        "..r........www..",
        ".rr44444444www..",
        "..r........ww...",
        "......44........",
        "......43........",
        "......43........",
        "......hh........",
        "......hh........",
        "......hh........",
        "......hh........",
        "......hh........",
        "................",
        "................",
    ], legend)


# ---------------------------------------------------------------------- decontamination and treatment

def decon_solution():
    """A brown glass bottle of decontaminating solution with a cream label and a stopper."""
    legend = tones(ramp("1f1209", "33200f", "4d3216", "6b471f", "8f6230", "b38449"))
    for i, d in enumerate("abcde"):
        legend[d] = (P("label", CREAM, base=0, bevel=False), i)
    legend["s"] = P("stopper", LEATHER)
    legend["g"] = (P("shine", WHITE, base=0, bevel=False), 3)
    return draw([
        "................",
        "......ssss......",
        "......ssss......",
        "......4553......",
        "......4543......",
        ".....455433.....",
        "....45g44332....",
        "....4g443332....",
        "....dccbbaa1....",
        "....dccbbaa1....",
        "....dccbbaa1....",
        "....44443332....",
        "....44433322....",
        ".....222211.....",
        "................",
        "................",
    ], legend)


def decon_powder():
    """A round tin of bleaching powder: grey tin, a label band and a press-on lid."""
    legend = tones(TIN)
    for i, d in enumerate("abcde"):
        legend[d] = (P("label", ORANGE, base=0, bevel=False), i)
    legend["w"] = (P("powder", WHITE, base=0, bevel=False), 4)
    return draw([
        "................",
        "................",
        "....44444443....",
        "...4555555543...",
        "...4444444333...",
        "...3333333222...",
        "...4544444332...",
        "...dddcccbbaa...",
        "...dwwdcwcbaa...",
        "...dddcccbbaa...",
        "...4544444332...",
        "...4444433322...",
        "....33332222....",
        "................",
        "................",
        "................",
    ], legend)


def medicine_general():
    """A tin box of tablets with a red cross."""
    legend = tones(WHITE)
    legend["r"] = (P("cross", RED, base=0, bevel=False), 3)
    legend["R"] = (P("cross", RED, base=0, bevel=False), 2)
    return draw([
        "................",
        "................",
        "................",
        "...4444444443...",
        "...4555555543...",
        "...3333333332...",
        "...4444rr4432...",
        "...444rrrR432...",
        "...444rrrR432...",
        "...4444RR4332...",
        "...4444443332...",
        "...4444433322...",
        "...2222222111...",
        "................",
        "................",
        "................",
    ], legend)


def antidote_nerve():
    """An atropine auto-injector: a green tube with a yellow band and the needle end."""
    legend = tones(ramp("1c3018", "2a4823", "3d6633", "548a46", "70ab5c", "93c97e"))
    for i, d in enumerate("abcde"):
        legend[d] = (P("band", BANDS["yperite"], base=0, bevel=False), i)
    legend["m"] = P("cap", STEEL_GREY)
    return draw([
        "................",
        "..........m.....",
        ".........mmm....",
        "........3mmm....",
        ".......4533.....",
        "......45433.....",
        ".....dcbba......",
        "....dcbba.......",
        "...45433........",
        "..45433.........",
        "..4433..........",
        "..233...........",
        "................",
        "................",
        "................",
        "................",
    ], legend)


def ointment_blister():
    """A squeezed tube of ointment against blister agents, with its cap and a yellow band."""
    legend = tones(WHITE)
    for i, d in enumerate("abcde"):
        legend[d] = (P("band", BANDS["yperite"], base=0, bevel=False), i)
    legend["m"] = P("cap", STEEL_GREY)
    return draw([
        "................",
        "................",
        "................",
        "..........mm....",
        ".........mmmm...",
        "........44mm....",
        ".......4553.....",
        "......45543.....",
        ".....dccba......",
        "....dccba.......",
        "...45543........",
        "..455433........",
        "..44433.........",
        "..2222..........",
        "................",
        "................",
    ], legend)
