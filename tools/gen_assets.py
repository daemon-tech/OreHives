"""Generates all JSON resources and hand-coded pixel textures. Run: python3 tools/gen_assets.py"""
import json, os, random
from PIL import Image
R = "src/main/resources"; NS = "orehives"
ORES = {"iron": (216,175,147), "copper": (224,115,77), "gold": (250,215,60), "diamond": (90,230,220),
        "emerald": (40,200,90), "redstone": (220,30,30), "lapis": (40,80,200)}
def w(path, obj):
    p = os.path.join(R, path); os.makedirs(os.path.dirname(p), exist_ok=True)
    open(p, "w").write(json.dumps(obj, indent=2) + "\n")
def tex(name, img):
    p = f"{R}/assets/{NS}/textures/{name}.png"; os.makedirs(os.path.dirname(p), exist_ok=True); img.save(p)
def shade(c, f): return tuple(max(0, min(255, int(v*f))) for v in c) + (255,)

HEX = ["....XXXX........", "...XoooOX.......", "..XoooooOX......", ".XoooooooOX.....",
       ".XoooooooOX.....", ".XoooooooOX.....", "..XoooooOX......", "...XOOOOX......."]
def comb(c):
    img = Image.new("RGBA", (16,16), (0,0,0,0)); px = img.load()
    for ox, oy in ((1,1),(6,5),(0,8)):
        for y,row in enumerate(HEX):
            for x,ch in enumerate(row):
                X,Y = x+ox, y+oy
                if X>15 or Y>15 or ch == ".": continue
                px[X,Y] = shade(c, {"X":0.45,"o":1.0,"O":0.75}[ch])
    random.seed(sum(c))
    for _ in range(6):
        x,y = random.randrange(16), random.randrange(16)
        if px[x,y][3]: px[x,y] = shade(c, 1.25)
    return img
def block(base, noise=0.12, seed=1):
    random.seed(seed); img = Image.new("RGBA",(16,16)); px = img.load()
    for y in range(16):
        for x in range(16): px[x,y] = shade(base, 1 + random.uniform(-noise, noise))
    return img
def frame(img, c):
    px = img.load()
    for i in range(16): px[i,0]=px[i,15]=px[0,i]=px[15,i]=shade(c,0.6)
    return img

for o,c in ORES.items(): tex(f"item/{o}_comb", comb(c))
# ore honey bottle
b = Image.new("RGBA",(16,16),(0,0,0,0)); px=b.load()
for y in range(5,15):
    for x in range(4,12): px[x,y] = (200,140,40,255) if y>7 else (220,230,235,180)
for y in range(2,5):
    for x in range(6,10): px[x,y] = (220,230,235,200)
for x in range(6,10): px[x,1] = (140,90,50,255)
for (x,y),o in zip(((5,9),(8,11),(10,13)), ("iron","gold","diamond")): px[x,y]=shade(ORES[o],1.1)
tex("item/ore_honey_bottle", b)
# hive: stone-brick-ish with comb slots
wood=(150,110,70); stone=(120,120,125)
top = frame(block(stone, seed=3), stone); tex("block/ore_beehive_top", top)
for fill in range(3):
    img = frame(block(wood, seed=4), wood); px=img.load()
    for y in (5,10):
        for x in range(1,15): px[x,y]=shade(wood,0.6)
    for x,y in ((4,7),(7,7),(10,7),(5,12),(9,12)):
        col = (60,40,25) if fill==0 else (250,200,60) if fill==1 or (x+y)%2 else (90,230,220)
        for dx in range(2):
            for dy in range(2): px[x+dx,y+dy]=shade(col,1.0)
    tex(f"block/ore_beehive_side_{fill}", img)
metal=(160,165,170)
tex("block/centrifuge_side", frame(block(metal, 0.06, 5), metal))
for on in (0,1):
    img = frame(block(metal,0.06,6), metal); px=img.load()
    for y in range(4,12):
        for x in range(4,12):
            r=((x-7.5)**2+(y-7.5)**2)**.5
            if r<4: px[x,y] = shade((250,190,40) if on else (90,90,95), 1 - r/10)
    tex(f"block/centrifuge_top{'_on' if on else ''}", img)
# icon
ic = Image.new("RGBA",(16,16)); ic.paste(comb(ORES["gold"])); ic.resize((128,128), Image.NEAREST).save(f"{R}/assets/{NS}/icon.png")

# models / blockstates
A=f"assets/{NS}"
for o in ORES: w(f"{A}/models/item/{o}_comb.json", {"parent":"minecraft:item/generated","textures":{"layer0":f"{NS}:item/{o}_comb"}})
w(f"{A}/models/item/ore_honey_bottle.json", {"parent":"minecraft:item/generated","textures":{"layer0":f"{NS}:item/ore_honey_bottle"}})
for f in range(3):
    w(f"{A}/models/block/ore_beehive_{f}.json", {"parent":"minecraft:block/cube_column","textures":{"end":f"{NS}:block/ore_beehive_top","side":f"{NS}:block/ore_beehive_side_{f}"}})
w(f"{A}/blockstates/ore_beehive.json", {"variants":{f"fill={f}":{"model":f"{NS}:block/ore_beehive_{f}"} for f in range(3)}})
w(f"{A}/models/item/ore_beehive.json", {"parent":f"{NS}:block/ore_beehive_1"})
for on in (False,True):
    w(f"{A}/models/block/centrifuge{'_on' if on else ''}.json", {"parent":"minecraft:block/cube_column","textures":{"end":f"{NS}:block/centrifuge_top{'_on' if on else ''}","side":f"{NS}:block/centrifuge_side"}})
w(f"{A}/blockstates/centrifuge.json", {"variants":{"running=false":{"model":f"{NS}:block/centrifuge"},"running=true":{"model":f"{NS}:block/centrifuge_on"}}})
w(f"{A}/models/item/centrifuge.json", {"parent":f"{NS}:block/centrifuge"})
lang = {"block.orehives.ore_beehive":"Ore Beehive","block.orehives.centrifuge":"Honey Centrifuge",
        "item.orehives.ore_honey_bottle":"Ore Honey Bottle","itemGroup.orehives.main":"Ore Hives",
        "message.orehives.hive_status":"Combs: %s %s"}
for o in ORES: lang[f"item.orehives.{o}_comb"] = f"{o.capitalize()} Comb"
for pre,en in (("potion","Potion"),("splash_potion","Splash Potion"),("lingering_potion","Lingering Potion")):
    lang[f"item.minecraft.{pre}.effect.haste"] = f"{en} of Haste"
lang["item.minecraft.tipped_arrow.effect.haste"] = "Arrow of Haste"
w(f"{A}/lang/en_us.json", lang)
de = dict(lang); de.update({"block.orehives.ore_beehive":"Erz-Bienenstock","block.orehives.centrifuge":"Honigschleuder",
        "item.orehives.ore_honey_bottle":"Erzhonigflasche","message.orehives.hive_status":"Waben: %s %s"})
for o,n in {"iron":"Eisen","copper":"Kupfer","gold":"Gold","diamond":"Diamant","emerald":"Smaragd","redstone":"Redstone","lapis":"Lapis"}.items(): de[f"item.orehives.{o}_comb"]=f"{n}wabe"
for pre,dn in (("potion","Trank"),("splash_potion","Wurftrank"),("lingering_potion","Verweiltrank")):
    de[f"item.minecraft.{pre}.effect.haste"] = f"{dn} der Eile"
de["item.minecraft.tipped_arrow.effect.haste"] = "Pfeil der Eile"
w(f"{A}/lang/de_de.json", de)

# data: tags (OrePlants entries optional -> soft dependency)
D=f"data/{NS}"
OP = {"iron":"oreplant:iron_crop_block","gold":"oreplant:gold_crop_block","diamond":"oreplant:diamond_crop_block"}
for o in ORES:
    vals = [{"id":OP[o],"required":False}] if o in OP else []
    w(f"{D}/tags/block/ore_flowers/{o}.json", {"replace":False,"values":vals})
w(f"{D}/tags/block/ore_flowers.json", {"replace":False,"values":["#minecraft:flowers"]})
w("data/minecraft/tags/block/flowers.json", {"replace":False,"values":[{"id":v,"required":False} for v in OP.values()]})
w(f"{D}/tags/item/ore_combs.json", {"replace":False,"values":[f"{NS}:{o}_comb" for o in ORES]})
# loot: centrifuge outputs (balance: see README)
def entry(item, lo, hi, chance=None):
    e = {"type":"minecraft:item","name":item,"functions":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":lo,"max":hi}}]}
    pool = {"rolls":1,"entries":[e]}
    if chance: pool["conditions"]=[{"condition":"minecraft:random_chance","chance":chance}]
    return pool
outs = {"iron":[entry("minecraft:iron_nugget",2,4)], "gold":[entry("minecraft:gold_nugget",2,4)],
        "copper":[entry("minecraft:raw_copper",1,1,0.5)], "diamond":[entry("minecraft:diamond",1,1,0.05)],
        "emerald":[entry("minecraft:emerald",1,1,0.08)], "redstone":[entry("minecraft:redstone",2,4)],
        "lapis":[entry("minecraft:lapis_lazuli",2,4)]}
for o,pools in outs.items():
    pools = pools + [entry("minecraft:honeycomb",1,1,0.3), entry(f"{NS}:ore_honey_bottle",1,1,0.05)]
    w(f"{D}/loot_table/centrifuge/{o}_comb.json", {"type":"minecraft:chest","pools":pools})
for b in ("ore_beehive","centrifuge"):
    w(f"{D}/loot_table/blocks/{b}.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":f"{NS}:{b}"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
w("data/minecraft/tags/block/mineable/axe.json", {"values":[f"{NS}:ore_beehive"]})
w("data/minecraft/tags/block/mineable/pickaxe.json", {"values":[f"{NS}:centrifuge"]})
w(f"{D}/recipe/ore_beehive.json", {"type":"minecraft:crafting_shaped","category":"misc","pattern":["PPP","HIH","PPP"],
   "key":{"P":{"tag":"minecraft:planks"},"H":{"item":"minecraft:honeycomb"},"I":{"item":"minecraft:iron_ingot"}},"result":{"id":f"{NS}:ore_beehive","count":1}})
w(f"{D}/recipe/centrifuge.json", {"type":"minecraft:crafting_shaped","category":"misc","pattern":["III","IHI","SSS"],
   "key":{"I":{"item":"minecraft:iron_ingot"},"H":{"item":"minecraft:honeycomb"},"S":{"item":"minecraft:smooth_stone"}},"result":{"id":f"{NS}:centrifuge","count":1}})
w("fabric.mod.json", {"schemaVersion":1,"id":NS,"version":"${version}","name":"Ore Hives",
   "description":"Bees pollinating ore flowers (OrePlants crops or flowers on ore) fill Ore Beehives with ore combs; spin them in a Honey Centrifuge.",
   "authors":["Sam (daemon-tech)"],"contact":{"sources":"https://github.com/daemon-tech"},"license":"MIT","icon":f"assets/{NS}/icon.png","environment":"*",
   "entrypoints":{"main":["net.daemon.orehives.OreHives"]},
   "depends":{"fabricloader":">=0.16.0","minecraft":"~1.21.1","java":">=21","fabric-api":"*"},
   "suggests":{"oreplant":"*"}})
print("ok")

# separate dev-only gametest mod (not in release jar)
os.makedirs("src/gametest/resources", exist_ok=True)
open("src/gametest/resources/fabric.mod.json","w").write(json.dumps({"schemaVersion":1,"id":"orehives-gametest","version":"1.0.0",
  "name":"Ore Hives GameTests","environment":"*","entrypoints":{"fabric-gametest":["net.daemon.orehives.test.OreHivesGameTest"]},
  "depends":{"orehives":"*"}}, indent=2)+"\n")
