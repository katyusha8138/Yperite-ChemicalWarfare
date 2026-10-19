# Yperite - Chemical Warfare

Minecraft に毒ガスを中心とした化学兵器を追加する mod です。
ガス本体、散布手段、症状、防護手段を備え、[Superb Warfare](https://github.com/Mercurows/SuperbWarfare) と連携できます(連携は任意)。

- 対応: Minecraft 1.20.1 / Forge(NeoForge 1.21.1 は移植予定)
- 必須の前提 mod: [GeckoLib](https://modrinth.com/mod/geckolib) 4.4.6 以降(装備の 3D モデルの描画に使います)
- ライセンス: GPL-3.0-only(`LICENSE`)
- 設計と計画: [`docs/DESIGN.md`](docs/DESIGN.md)

## 現在の状態

開発初期です。ガス濃度場(拡散・風・屋外の判定・比重・遮蔽・地面残留)に加えて、
曝露と症状(吸入と皮膚、遅れて発症、段階的な悪化、系統ごとの後遺症)が動きます。
防護(ガスマスクとフィルター、防護服、検知器・警報器、風向計、除染、治療薬)も動きます。
散布手段として、ガス手榴弾(6 種)とガスボンベも使えます(入手はクリエイティブタブとコマンドのみで、クラフトレシピはまだありません)。
Superb Warfare が入っていれば、化学弾(40mm 擲弾、迫撃砲弾、砲兵の大口径砲弾)も使えます(下の「任意の連携」)。

第一次世界大戦の装備もあります(クリエイティブタブ):

- 鉄帽 4 種: 英ブロディ Mk I、仏アドリアン M15、独シュタールヘルム M1916、墺ベルンドルファー型
- ガスマスク 4 種: 英 小箱型呼吸器(SBR)、仏 M2、独 革製防毒面、墺洪軍マスク(ほかに現代型のガスマスク)
- 各国の軍服: 英・仏・独・墺洪の上衣・ズボン・巻きゲートルと編上靴
- 塹壕用の胴鎧: 独 ザッペンパンツァー、英 デイフィールド防弾胴衣

鉄帽と胴鎧には Superb Warfare の弾丸耐性が付きます(SBW が入っているときだけ効きます)。
装備はすべて GeckoLib の 3D モデルで描かれ、重ねて着ても互いにめり込みません(鉄帽とマスク、軍服と胴鎧とマスクのかばん)。

組み込みガス(系統):
`tear_gas`(催涙剤)、`chlorine`(窒息剤)、`blood_gas`(血液剤)、`yperite`(びらん剤)、
`nerve_gas`(神経剤)、`blue_cross`(無力化剤)。

## 開発

Java 17 が必要です。

```sh
./gradlew build               # ビルドと単体テスト
./gradlew test                # シミュレーション核の単体テストのみ
./gradlew runGameTestServer   # ワールド内テスト(実ブロック・扉・コマンドなど)
./gradlew -PnoCurios runGameTestServer   # Curios なしで同じ試験(任意の依存の確認)
./gradlew -PwithSbw runGameTestServer    # Superb Warfare を読み込んだ試験(連携の確認。Kotlin for Forge も自動で入る)
./gradlew runServer           # 開発用サーバー
./gradlew runClient           # 開発用クライアント
./gradlew -PsmokeClient runClient   # 見た目の確認(仮想ディスプレイ上で場面を撮影、docs/DESIGN.md §7.9)
```

### デバッグコマンド(OP 権限)

```
/yperite gas burst <type> <mass> <radius> [pos]   ガスを放出
/yperite gas contaminate <type> <amount> <radius> [pos]   地面に残留を置く
/yperite gas clear [type]                          ガスを消す
/yperite gas info                                  セル数と総量を表示
/yperite gas probe                                 自分の位置の濃度を表示
/yperite wind                                      風向と強さを表示
/yperite wind set <方角> <強さ>                    風を固定(方角は吹いてくる向き 0〜360、強さは 0〜1)
/yperite wind auto                                 風を自動に戻す
/yperite exposure [player]                         曝露の状態を表示
/yperite exposure add <系統> <量> [player]         曝露量を加える(潜伏として入る)
/yperite exposure clear [player]                   曝露と後遺症を消す
```

実際に試すには、自分の近くでガスを出します(例: 窒息剤。数秒でむせ始めます)。

```
/yperite gas burst yperite:chlorine 300 3
```

ガスは風に流れる煙の塊として見え、雲の中に入ると視界がガスの色に霞みます。地面に残ったガス(イペリットなど)は低い靄として見えます。
離れて待てば回復します。`/yperite gas clear` でガスを消せます。

防護具を試すには、クリエイティブタブ「Yperite - 化学戦」にすべて入っています。
ガスマスクは、フィルターとクラフトして装填します(装填済みのマスクもタブにあります)。
ガス検知器を手に持つと、周囲のガスの系統と濃さが表示されます。

散布手段もタブに入っています。手榴弾は右クリックで投げると、着地して約 3 秒後にガスが出ます。
ガスボンベは置いて、素手で右クリックするかレッドストーン信号で弁が開きます(中身は説明に出ます)。
コマンドでも出せます。

```
/give @s yperite:chlorine_grenade
/give @s yperite:gas_cylinder{BlockEntityTag:{Gas:"yperite:nerve_gas",Amount:2000f}}
```

## 任意の連携

- [Superb Warfare](https://modrinth.com/mod/superb-warfare)(0.8.9.2 で確認): 入っていれば、6 種のガスを詰めた 40mm 擲弾(M79 などで撃つ)、
  迫撃砲弾(迫撃砲で撃つ)、大口径砲弾(Mk42、Mle1934、FH-77BW、PLZ-05 で撃つ)が増えます。着弾した場所に、小さな爆発とともにガスが出ます。
  クリエイティブタブ「Yperite - 化学戦」から出せます(クラフトレシピはまだありません)。SBW のファイルは書き換えません。

- [Curios](https://www.curseforge.com/minecraft/mc-mods/curios): 入っていれば、ガスマスクを「顔」スロットにも着けられます(鉄帽や防護フードと併用可)。
  胴鎧は「胴」スロットに入れると、軍服や防護上衣の上に重ねて着られます(防御・弾丸耐性・重さは胸当て枠と同じ)。

- Superb Warfare の装備の値: 鉄帽(弾丸耐性 5〜8%)と胴鎧(ザッペンパンツァー 30%・歩く速さ -10%、デイフィールド 18%・-4%)。
  弾丸耐性は SBW の属性 `superbwarfare:bullet_resistance` で、傷むほど下がります(SBW と同じ扱い)。

## 素材について

モデルとテクスチャは `tools/` のスクリプトで作っています(Python 3、numpy、Pillow が必要)。

```sh
python3 tools/build_models.py   # 装備の GeckoLib モデルと貼り付け画像(重ね着のめり込みも検査)
python3 tools/build_icons.py    # アイテムのアイコン(16×16 のドット絵)
python3 tools/build_blocks.py   # ガスボンベと警報器のブロックモデル
python3 tools/build_particles.py   # ガスの雲と地面の靄の粒子
```

作り方と方針は [`docs/DESIGN.md`](docs/DESIGN.md) の「見た目」の節にあります。効果音はバニラのものを使っています。
