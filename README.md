# Yperite - Chemical Warfare

Minecraft に毒ガスを中心とした化学兵器を追加する mod です。
ガス本体、散布手段、症状、防護手段を備え、[Superb Warfare](https://github.com/Mercurows/SuperbWarfare) と連携できます(連携は任意)。

- 対応: Minecraft 1.20.1 / Forge(NeoForge 1.21.1 は移植予定)
- ライセンス: GPL-3.0-only(`LICENSE`)
- 設計と計画: [`docs/DESIGN.md`](docs/DESIGN.md)

## 現在の状態

開発初期です。ガス濃度場(拡散・風・比重・遮蔽・地面残留)とデバッグコマンドまでが動きます。

## 開発

Java 17 が必要です。

```sh
./gradlew build        # ビルドとテスト
./gradlew test         # シミュレーション核の JUnit テストのみ
./gradlew runServer    # 開発用サーバー
./gradlew runClient    # 開発用クライアント
```

### デバッグコマンド(OP 権限)

```
/yperite gas burst <type> <mass> <radius> [pos]   ガスを放出
/yperite gas clear [type]                          ガスを消す
/yperite gas info                                  セル数と総量を表示
/yperite gas probe                                 自分の位置の濃度を表示
```

## 素材について

テクスチャ・モデル・効果音は仮のものです。差し替えやすいよう `assets/yperite/` 以下にまとめます。
