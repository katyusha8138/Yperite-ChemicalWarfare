# Yperite - Chemical Warfare

Minecraft に毒ガスを中心とした化学兵器を追加する mod です。
ガス本体、散布手段、症状、防護手段を備え、[Superb Warfare](https://github.com/Mercurows/SuperbWarfare) と連携できます(連携は任意)。

- 対応: Minecraft 1.20.1 / Forge(NeoForge 1.21.1 は移植予定)
- ライセンス: GPL-3.0-only(`LICENSE`)
- 設計と計画: [`docs/DESIGN.md`](docs/DESIGN.md)

## 現在の状態

開発初期です。ガス濃度場(拡散・風・屋外の判定・比重・遮蔽・地面残留)に加えて、
曝露と症状(吸入と皮膚、遅れて発症、段階的な悪化、系統ごとの後遺症)が動きます。
防護(ガスマスクとフィルター、防護服、検知器・警報器、風向計、除染、治療薬)も動きます。
散布手段と Superb Warfare 連携はこれからです(計画は設計書を参照)。

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
./gradlew runServer           # 開発用サーバー
./gradlew runClient           # 開発用クライアント
./gradlew -PsmokeClient runClient   # 見た目の確認(仮想ディスプレイ上で場面を撮影、docs/DESIGN.md §7.9)
```

### デバッグコマンド(OP 権限)

```
/yperite gas burst <type> <mass> <radius> [pos]   ガスを放出
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

離れて待てば回復します。`/yperite gas clear` でガスを消せます。

防護具を試すには、クリエイティブタブ「Yperite - 化学戦」にすべて入っています。
ガスマスクは、フィルターとクラフトして装填します(装填済みのマスクもタブにあります)。
ガス検知器を手に持つと、周囲のガスの系統と濃さが表示されます。

## 任意の連携

- [Curios](https://www.curseforge.com/minecraft/mc-mods/curios): 入っていれば、ガスマスクを「顔」スロットにも着けられます(ヘルメットと併用可。顔枠でも見た目に描かれます)。

## 素材について

テクスチャ・モデルは仮のものです(`tools/gen_placeholder_assets.py` で生成)。
`assets/yperite/` 以下の同じ名前の PNG を差し替えれば入れ替わります。効果音はバニラのものを使っています。
